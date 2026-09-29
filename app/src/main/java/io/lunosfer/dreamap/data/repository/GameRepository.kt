package io.lunosfer.dreamap.data.repository

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.data.model.GameProgress
import io.lunosfer.dreamap.data.model.LeaderboardEntry
import io.lunosfer.dreamap.data.model.OnboardingResult
import io.lunosfer.dreamap.data.model.PublicProgress
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicLong

/** XP/rutbe/rozet degisimlerinden dogan, ekranda kutlanacak olaylar. */
sealed class GameEvent {
    abstract val id: Long

    data class XpGain(val amount: Int, val reason: String?, override val id: Long = nextId()) : GameEvent()
    data class RankUp(val rank: Int, override val id: Long = nextId()) : GameEvent()
    data class BadgeEarned(val code: String, override val id: Long = nextId()) : GameEvent()

    private companion object {
        val counter = AtomicLong()
        fun nextId() = counter.incrementAndGet()
    }
}

/**
 * Oyunlastirma durumu (tekil, UserWallet deseni).
 *
 * Puanlar tamamen sunucuda (DB trigger'lari) veriliyor; istemci sadece
 * get_my_progress() okuyor. Realtime bagimliligi olmadigi icin tazeleme:
 * giris/uygulamaya donus + her basarili yazma API cagrisindan sonra
 * (GameRefreshInterceptor) + dogrudan PostgREST ile yazilan ruya sonrasi.
 *
 * "Gorulen" XP/rutbe/rozetler kullanici bazinda diske yaziliyor: uygulama
 * kapaliyken kazanilanlar da (baskasi begendi vb.) bir sonraki acilista
 * kutlanabilsin diye fark bellekteki degil diskteki son degere gore aliniyor.
 */
object GameRepository {

    private const val TAG = "GameRepository"
    private const val PREFS = "lunosfer_game"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    // OkHttp is parcaciklarindan da cagriliyor.
    @Volatile private var pendingRefresh: Job? = null
    @Volatile private var lastRefreshAt = 0L

    private val _progress = MutableStateFlow<GameProgress?>(null)
    val progress: StateFlow<GameProgress?> = _progress.asStateFlow()

    // SharedFlow degil: abone yokken (arka plan) yayinlanan olay kaybolmasin.
    private val _events = MutableStateFlow<List<GameEvent>>(emptyList())
    val events: StateFlow<List<GameEvent>> = _events.asStateFlow()

    fun consume(event: GameEvent) {
        _events.update { list -> list.filterNot { it.id == event.id } }
    }

    /** Art arda gelen yazma islemlerini tek bir sorguda toplar. */
    fun requestRefresh(delayMs: Long = 800) {
        pendingRefresh?.cancel()
        pendingRefresh = scope.launch {
            delay(delayMs)
            refresh()
        }
    }

    suspend fun refresh(): GameProgress? = mutex.withLock {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return@withLock null
        val fresh = runCatching {
            json.decodeFromString<GameProgress>(supabaseClient.postgrest.rpc("get_my_progress") {}.data)
        }.onFailure { Log.w(TAG, "get_my_progress failed", it) }.getOrNull()
            ?: return@withLock _progress.value

        // Istek surerken oturum degistiyse (cikis / baska hesap) eski sonucu yazma.
        if (supabaseClient.auth.currentUserOrNull()?.id != userId) return@withLock null

        _progress.value = fresh
        lastRefreshAt = System.currentTimeMillis()
        if (!fresh.isGuest) diffAgainstSeen(userId, fresh)
        fresh
    }

    /** Uygulamaya donuste: az once tazelendiyse tekrar sorgulama. */
    fun refreshIfStale(maxAgeMs: Long = 30_000) {
        if (System.currentTimeMillis() - lastRefreshAt > maxAgeMs) requestRefresh(0)
    }

    private fun diffAgainstSeen(userId: String, p: GameProgress) {
        val prefs = DreamapApp.instance.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val xpKey = "xp_$userId"
        val rankKey = "rank_$userId"
        val badgesKey = "badges_$userId"
        val earned = p.earnedBadgeCodes

        // Bu cihazda bu hesabin ilk yuklemesi: gecmis (backfill) icin olay patlatma.
        if (!prefs.contains(xpKey)) {
            prefs.edit().putInt(xpKey, p.xp).putInt(rankKey, p.rank).putStringSet(badgesKey, earned).apply()
            return
        }

        val seenXp = prefs.getInt(xpKey, 0)
        val seenRank = prefs.getInt(rankKey, 0)
        val seenBadges = prefs.getStringSet(badgesKey, emptySet()).orEmpty()

        val newEvents = mutableListOf<GameEvent>()
        if (p.xp > seenXp) {
            val gained = p.xp - seenXp
            val single = p.recent.firstOrNull()?.takeIf { it.delta == gained }
            newEvents += GameEvent.XpGain(gained, single?.reason)
        }
        (earned - seenBadges).forEach { newEvents += GameEvent.BadgeEarned(it) }
        if (p.rank > seenRank) newEvents += GameEvent.RankUp(p.rank)

        prefs.edit().putInt(xpKey, p.xp).putInt(rankKey, p.rank).putStringSet(badgesKey, earned).apply()
        if (newEvents.isNotEmpty()) _events.update { it + newEvents }
    }

    suspend fun finishOnboarding(completed: Boolean): OnboardingResult {
        val raw = supabaseClient.postgrest.rpc(
            "finish_onboarding",
            buildJsonObject { put("p_completed", completed) }
        ) {}.data
        val result = json.decodeFromString<OnboardingResult>(raw)
        refresh()
        return result
    }

    /**
     * Dış platform paylaşımını (WhatsApp, Instagram...) XP'ye sayar. Sunucu
     * içerik + kanal + gün başına bir kez ve günde en fazla 5 kez ödüllendirir;
     * hata paylaşımı asla engellemez. Kendi kapsamında çalışır: paylaşım
     * sayfası dış uygulama açılırken kapansa da istek iptal olmaz.
     */
    fun recordShare(type: String, id: String, channel: String) {
        if (supabaseClient.auth.currentUserOrNull() == null) return
        scope.launch {
            runCatching {
                supabaseClient.postgrest.rpc(
                    "record_share",
                    buildJsonObject {
                        put("p_type", type)
                        put("p_id", id)
                        put("p_channel", channel)
                    }
                ) {}
                requestRefresh()
            }.onFailure { Log.w(TAG, "record_share failed: ${it.message}") }
        }
    }

    suspend fun leaderboard(period: String): List<LeaderboardEntry> {
        val raw = supabaseClient.postgrest.rpc(
            "get_leaderboard",
            buildJsonObject {
                put("p_period", period)
                put("p_limit", 20)
            }
        ) {}.data
        return json.decodeFromString(raw)
    }

    /** Gorunurluk izin vermiyorsa (gizli profil vb.) null. */
    suspend fun publicProgress(userId: String): PublicProgress? {
        val raw = supabaseClient.postgrest.rpc(
            "get_public_progress",
            buildJsonObject { put("p_user", userId) }
        ) {}.data.trim()
        if (raw.isEmpty() || raw == "null") return null
        return json.decodeFromString<PublicProgress>(raw)
    }

    fun clear() {
        pendingRefresh?.cancel()
        lastRefreshAt = 0L
        _progress.value = null
        _events.value = emptyList()
    }
}

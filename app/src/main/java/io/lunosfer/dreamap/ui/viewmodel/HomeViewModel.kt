package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.FeedItem
import io.lunosfer.dreamap.data.repository.DreamRepository
import io.lunosfer.dreamap.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Home ekranindaki "gunluk seri" (streak) bilgisini tutan basit veri sinifi.
 * streakDays: kullanicinin art arda ruya kaydettigi gun sayisi (bugun dahil).
 * streakStartDate: serinin basladigi tarih (kullanicidostu format, ornek: "12 Ocak").
 * hasDreamToday: bugun zaten bir ruya kaydedilmis mi.
 */
data class StreakInfo(
    val streakDays: Int = 0,
    val streakStartDate: String? = null,
    val hasDreamToday: Boolean = false
)

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository(),
    private val dreamRepository: DreamRepository = DreamRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<FeedItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<FeedItem>>> = _state.asStateFlow()

    private val _streak = MutableStateFlow(StreakInfo())
    val streak: StateFlow<StreakInfo> = _streak.asStateFlow()

    // Ana akış kartındaki kalp ikonuna dokunarak beğenme durumu — sadece bu oturum
    // için lokal tutulur, çünkü /api/home-feed liste yanıtı is_liked döndürmüyor
    // (bu alan sadece tekil rüya detay endpoint'inde var, bkz. DreamDetail.isLiked).
    // DreamRepository.likeDream() sunucudaki "Already liked" hatasını sessizce
    // başarı sayarak yuttuğu için ilk dokunuşta da doğru sonuca yakınsıyor.
    private val _likedDreamIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedDreamIds: StateFlow<Set<Long>> = _likedDreamIds.asStateFlow()

    private val _likeCountOverrides = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val likeCountOverrides: StateFlow<Map<Long, Int>> = _likeCountOverrides.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    fun toggleDreamLike(dream: Dream, userId: String?) {
        if (userId.isNullOrBlank()) {
            _actionError.value = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.dream_detail_error_login_required_like)
            return
        }
        val dreamId = dream.id
        val wasLiked = _likedDreamIds.value.contains(dreamId)
        val baseCount = _likeCountOverrides.value[dreamId] ?: (dream.likesCount ?: 0)
        val newLiked = !wasLiked
        val newCount = if (newLiked) baseCount + 1 else maxOf(0, baseCount - 1)

        // Optimistic güncelleme
        _likedDreamIds.value = if (newLiked) _likedDreamIds.value + dreamId else _likedDreamIds.value - dreamId
        _likeCountOverrides.value = _likeCountOverrides.value + (dreamId to newCount)

        viewModelScope.launch {
            val result = if (newLiked) {
                dreamRepository.likeDream(dreamId, userId)
            } else {
                dreamRepository.unlikeDream(dreamId, userId)
            }

            result.onSuccess { response ->
                val confirmedLiked = response.liked || response.isLiked
                _likedDreamIds.value = if (confirmedLiked) _likedDreamIds.value + dreamId else _likedDreamIds.value - dreamId
                val confirmedCount = response.likesCount ?: response.count
                if (confirmedCount != null) {
                    _likeCountOverrides.value = _likeCountOverrides.value + (dreamId to confirmedCount)
                }
            }.onFailure { err ->
                val msg = err.message ?: ""
                if (msg.contains("Already liked", ignoreCase = true)) {
                    // Sessizce yut, optimistic state zaten doğru
                    return@onFailure
                }
                // Hata durumunda rollback
                _likedDreamIds.value = if (wasLiked) _likedDreamIds.value + dreamId else _likedDreamIds.value - dreamId
                _likeCountOverrides.value = _likeCountOverrides.value + (dreamId to baseCount)
                _actionError.value = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.dream_detail_error_like_failed)
            }
        }
    }

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.loadFirstPage()
                .onSuccess {
                    _state.value = UiState.Success(it)
                    _streak.value = computeStreak(it)
                }
                .onFailure { _state.value = UiState.Error(it.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_unknown)) }
        }
    }

    /**
     * Kullanicinin kendi ruyalarindan (FeedItem.DreamItem) art arda kac gun
     * ruya kaydettigini hesaplar. Feed listesi zaten createdAt'e gore
     * azalan sirada geldigi icin (bkz. HomeRepository), gunleri gruplayip
     * bugunden geriye dogru kesintisiz zinciri sayiyoruz.
     */
    private fun computeStreak(items: List<FeedItem>): StreakInfo {
        val dreamDates = items
            .filterIsInstance<FeedItem.DreamItem>()
            .mapNotNull { parseDayKey(it.dream.createdAt) }
            .distinct()
            .sortedDescending()

        if (dreamDates.isEmpty()) return StreakInfo()

        val todayKey = dayKeyFor(Calendar.getInstance())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = dayKeyFor(yesterdayCal)

        val hasToday = dreamDates.first() == todayKey
        // Seri, bugunku veya dunku kayittan baslamiyorsa kirilmis demektir.
        val startingKey = when (dreamDates.first()) {
            todayKey -> todayKey
            yesterdayKey -> yesterdayKey
            else -> return StreakInfo(streakDays = 0, hasDreamToday = false)
        }

        var streak = 1
        var cursor = Calendar.getInstance().apply {
            timeInMillis = startingKey
        }
        for (i in 1 until dreamDates.size) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
            val expectedKey = dayKeyFor(cursor)
            if (dreamDates[i] == expectedKey) {
                streak++
            } else {
                break
            }
        }

        val streakStartCal = Calendar.getInstance().apply {
            timeInMillis = startingKey
            add(Calendar.DAY_OF_YEAR, -(streak - 1))
        }
        val displayFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
        val startDateDisplay = displayFormat.format(streakStartCal.time)

        return StreakInfo(
            streakDays = streak,
            streakStartDate = startDateDisplay,
            hasDreamToday = hasToday
        )
    }

    /** ISO tarih string'ini gun bazinda karsilastirilabilir bir zaman damgasina cevirir (saat/dakika/saniye sifirlanir). */
    private fun parseDayKey(isoDate: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(isoDate.take(19)) ?: return null
            val cal = Calendar.getInstance()
            cal.time = date
            dayKeyFor(cal)
        } catch (e: Exception) {
            null
        }
    }

    private fun dayKeyFor(cal: Calendar): Long {
        val clone = cal.clone() as Calendar
        clone.set(Calendar.HOUR_OF_DAY, 0)
        clone.set(Calendar.MINUTE, 0)
        clone.set(Calendar.SECOND, 0)
        clone.set(Calendar.MILLISECOND, 0)
        return clone.timeInMillis
    }
}

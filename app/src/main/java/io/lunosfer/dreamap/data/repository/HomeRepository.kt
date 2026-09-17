package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.lunosfer.dreamap.data.model.FeedItem
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * pages/api/home-feed.js'nin type=dreams ve type=visions çağrılarını PARALEL
 * yapıp created_at'e göre tek bir kronolojik listede birleştirir — web
 * tarafındaki handler'ın yaptığı [...dreams, ...visions].sort() mantığının
 * istemci tarafı karşılığı (bkz. HomeFeed.kt açıklaması: neden ayrı iki
 * tip-güvenli çağrı tercih edildi). İki çağrı birbirinden bağımsız olduğu
 * için coroutineScope + async ile paralel çalıştırılıyor, ardışık değil.
 *
 * Sayfalama: API her iki tür için AYRI imleç döndürüyor (nextDreamsBefore /
 * nextVisionsBefore). Uygulama bunları hiç kullanmıyordu; akış ilk sayfadan
 * sonra (6 rüya + 6 vizyon) kalıcı olarak bitiyordu — kullanıcı daha eski
 * hiçbir gönderiye ulaşamıyordu.
 */
class HomeRepository {
    private val api = NetworkModule.api

    data class HomePage(
        val items: List<FeedItem>,
        val nextDreamsBefore: String?,
        val nextVisionsBefore: String?,
        val hasMoreDreams: Boolean,
        val hasMoreVisions: Boolean
    )

    suspend fun loadFirstPage(): Result<HomePage> = loadPage(null, null)

    suspend fun loadPage(
        dreamsBefore: String?,
        visionsBefore: String?,
        includeDreams: Boolean = true,
        includeVisions: Boolean = true
    ): Result<HomePage> = runCatching {
        coroutineScope {
            val dreamsDeferred = if (includeDreams) async { api.getHomeDreams(type = "dreams", dreamsBefore = dreamsBefore) } else null
            val visionsDeferred = if (includeVisions) async { api.getHomeVisions(type = "visions", visionsBefore = visionsBefore) } else null

            val dreamsResponse = dreamsDeferred?.await()
            val visionsResponse = visionsDeferred?.await()

            val items = buildList {
                dreamsResponse?.items?.forEach { add(FeedItem.DreamItem(it)) }
                visionsResponse?.items?.forEach { add(FeedItem.VisionItem(it)) }
            }.sortedByDescending { it.createdAt }

            HomePage(
                items = items,
                nextDreamsBefore = dreamsResponse?.nextDreamsBefore,
                nextVisionsBefore = visionsResponse?.nextVisionsBefore,
                hasMoreDreams = dreamsResponse?.hasMore ?: false,
                hasMoreVisions = visionsResponse?.hasMore ?: false
            )
        }
    }

    @Serializable
    private data class DreamDateRow(@SerialName("created_at") val createdAt: String)

    @Serializable
    private data class GoalIdRow(val id: String)

    /**
     * Günlük seri, KULLANICININ KENDİ rüya kayıtlarından hesaplanmalı.
     * Önceden ana akıştaki (arkadaşların gönderileri de dahil) ilk 6 rüyadan
     * hesaplanıyordu: başkası rüya paylaştığında kullanıcının serisi artıyor,
     * kendi rüyaları ilk sayfaya sığmadığında seri sıfırlanıyordu.
     */
    suspend fun loadOwnDreamDates(limit: Long = 120): Result<List<String>> = runCatching {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return@runCatching emptyList()
        supabaseClient.postgrest["dreams"]
            .select(columns = Columns.list("created_at")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<DreamDateRow>()
            .map { it.createdAt }
    }

    /** Karşılama başlığındaki "aktif vizyon" sayısı — kullanıcının KENDİ aktif vizyonları. */
    suspend fun loadOwnActiveVisionCount(): Result<Int> = runCatching {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return@runCatching 0
        supabaseClient.postgrest["goals"]
            .select(columns = Columns.list("id")) {
                filter {
                    eq("user_id", userId)
                    eq("status", "active")
                }
                limit(200)
            }
            .decodeList<GoalIdRow>()
            .size
    }
}

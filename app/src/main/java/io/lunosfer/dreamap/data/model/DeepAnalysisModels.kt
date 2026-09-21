package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * "Derin Analiz" — kullanicinin son ruyalari + vizyonlari + arketip
 * egilimi birlikte okunarak Opus 5 ile uretilen tek rapor.
 *
 * dreams.premium_deep_analysis (tek bir ruyanin derin analizi) ile ayni
 * sey DEGIL; o DreamDetailScreen'de, bu ayri bir ekranda.
 */
@Serializable
data class DeepAnalysisRequest(
    /** Bos birakilirsa sunucu son 12 ruyayi kendisi secer. */
    @SerialName("dreamIds") val dreamIds: List<Long>? = null,
    /**
     * Yalnizca yedek: sunucu once ruyalarin YAZILDIGI dili (dominant
     * original_language) kullanir, arayuz dilini degil.
     */
    val lang: String = "en"
)

@Serializable
data class FearMapItem(
    val symbol: String,
    /** 0-100: bu korkunun materyali ne kadar surukledigine dair guven. */
    val weight: Int = 0,
    val evidence: String? = null
)

@Serializable
data class DeepAnalysis(
    val id: String,
    @SerialName("created_at") val createdAt: String? = null,
    val lang: String? = null,
    val status: String? = null,
    @SerialName("personality_analysis") val personalityAnalysis: String? = null,
    @SerialName("fear_map") val fearMap: List<FearMapItem>? = null,
    @SerialName("confrontation_solution") val confrontationSolution: String? = null,
    @SerialName("card_headline") val cardHeadline: String? = null,
    @SerialName("card_affirmation") val cardAffirmation: String? = null,
    @SerialName("card_image_url") val cardImageUrl: String? = null
)

@Serializable
data class DeepAnalysisResponse(
    val ok: Boolean = false,
    val analysis: DeepAnalysis? = null,
    val aurasLeft: Int? = null,
    val isPremium: Boolean = false,
    val error: String? = null,
    /** insufficient_auras ile birlikte gelir. */
    val cost: Int? = null,
    val auras: Int? = null,
    /** not_enough_dreams ile birlikte gelir. */
    val minimum: Int? = null,
    /** rate_limited ile birlikte gelir. */
    val limit: Int? = null,
    val retryAfterMinutes: Int? = null,
    /** Uretim basarisiz oldu ve Aura iade edildi mi. */
    val refunded: Boolean = false
)

@Serializable
data class DeepAnalysisListResponse(
    val ok: Boolean = false,
    val analyses: List<DeepAnalysis> = emptyList(),
    val error: String? = null
)

package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeDreamRequest(
    val dreamId: Long,
    val content: String,
    val lang: String
)

/** analyze-dream yaniti; karti guncellemek icin yalnizca analiz alanlari okunur. */
@Serializable
data class AnalyzeDreamResponse(val dream: AnalyzedDreamFields? = null)

@Serializable
data class AnalyzedDreamFields(
    @kotlinx.serialization.SerialName("ai_title") val aiTitle: String? = null,
    @kotlinx.serialization.SerialName("ai_archetypes") val aiArchetypes: List<String>? = null,
    @kotlinx.serialization.SerialName("ai_jungian_analysis") val aiJungianAnalysis: AiJungianAnalysis? = null,
    @kotlinx.serialization.SerialName("ai_sentiment") val aiSentiment: String? = null
)

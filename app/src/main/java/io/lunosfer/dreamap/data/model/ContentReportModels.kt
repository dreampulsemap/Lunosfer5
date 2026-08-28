package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

// Vizyonlar için zaten GoalReportReason + reportGoal vardı (bkz.
// GoalReportModels.kt). Bu dosya aynı 6 sabit sebebi (spam/inappropriate/
// harassment/misinformation/hate_speech/other) rüya, mesaj ve kullanıcı
// şikayetleri için tekrar kullanır — UI tarafında da GoalReportReason
// enum'ı ve genelleştirilmiş VisionReportSheet doğrudan yeniden
// kullanılıyor (bkz. VisionInteractionComponents.kt, DreamDetailScreen.kt,
// PublicProfileScreen.kt, ThreadScreen.kt).

@Serializable
data class ReportDreamRequest(
    val dreamId: Long,
    val reason: String,
    val note: String? = null
)

@Serializable
data class ReportMessageRequest(
    val messageId: String,
    val reason: String,
    val note: String? = null
)

@Serializable
data class ReportUserRequest(
    val userId: String,
    val reason: String,
    val note: String? = null
)

@Serializable
data class ContentReportResponse(
    val success: Boolean = false,
    val already_reported: Boolean = false,
    val error: String? = null
)

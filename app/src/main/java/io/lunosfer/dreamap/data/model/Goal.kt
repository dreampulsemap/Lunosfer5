package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * goals tablosu satırı ("Vizyon" = Vision Board hedefi). Alanlar
 * pages/api/goals/create.js (insert payload) ve pages/api/goals/list.js
 * (select + join'ler) ile eşleşir. GoalCard.jsx'te render edilen alanların
 * hepsi burada: title, cover_image_url, status, completion_percentage,
 * believers_count, has_reacted, has_saved, slide_count, owner.
 */
@Serializable
data class Goal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("cover_image_source") val coverImageSource: String? = null,
    val status: String = "active",
    val visibility: String = "public",
    @SerialName("completion_percentage") val completionPercentage: Double? = 0.0,
    @SerialName("believers_count") val believersCount: Int? = 0,
    @SerialName("has_reacted") val hasReacted: Boolean? = false,
    @SerialName("has_saved") val hasSaved: Boolean? = false,
    @SerialName("slide_count") val slideCount: Int? = 0,
    // goal_comments'e yazılınca handle_goal_comment_change trigger'ı bu
    // kolonu otomatik günceller (bkz. pages/api/goals/comment.js) — Android
    // tarafında yorum ikonunun altındaki sayıyı, yorum paneli hiç açılmadan
    // (ağ isteği atmadan) doğru göstermek için kullanılıyor.
    @SerialName("comments_count") val commentsCount: Int? = 0,
    // Reels editöründen export edilip Storage'a yüklenen videonun public URL'i
    // (pages/api/goals/save-vision-video.js tarafından yazılır).
    @SerialName("vision_video_url") val visionVideoUrl: String? = null,
    @SerialName("target_date") val targetDate: String? = null,
    // Backend addition (pages/api/goals/generate-future-message.js) — AI line
    // written as if from the user's future self who already reached this
    // goal. Null until that endpoint has been called for this goal at least
    // once; VisionMessageCard's mapper falls back to description/title
    // until then.
    @SerialName("ai_future_message") val aiFutureMessage: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("micro_goals") val microGoals: List<MicroGoal>? = null,
    val owner: UserProfile? = null,
    @SerialName("feed_type") val feedType: String? = "vision"
)

@Serializable
data class MicroGoal(
    val id: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("order_index") val orderIndex: Int? = null
)

@Serializable
data class GoalsListResponse(
    val goals: List<Goal>,
    val page: Int,
    val hasMore: Boolean
)

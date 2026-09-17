package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiaryRing(
    val userId: String = "",
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val entryCount: Int = 0,
    val latestEntryAt: String? = null,
    val hasUnseen: Boolean = false,
    val streakDays: Int? = null,
    val isSelf: Boolean = false
) {
    val nameOrFallback: String get() = displayName?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.common_user_fallback)
}

@Serializable
data class DiaryFeedResponse(
    val rings: List<DiaryRing> = emptyList()
)

@Serializable
data class DiaryEntry(
    val id: String = "",
    @SerialName("media_type") val mediaType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    val caption: String? = null,
    @SerialName("goal_id") val goalId: String? = null,
    @SerialName("goal_title") val goalTitle: String? = null,
    val visibility: String = "private",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("is_liked") val isLiked: Boolean = false
)

@Serializable
data class DiaryLikeRequest(
    @SerialName("diary_entry_id") val diaryEntryId: String
)

@Serializable
data class DiaryLikeResponse(
    val success: Boolean = false,
    val liked: Boolean = false,
    val count: Int = 0,
    val error: String? = null
)

@Serializable
data class DiaryComment(
    val id: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_profiles") val userProfile: UserProfile? = null
)

@Serializable
data class DiaryCommentsResponse(
    val comments: List<DiaryComment> = emptyList()
)

@Serializable
data class CreateDiaryCommentRequest(
    @SerialName("diary_entry_id") val diaryEntryId: String,
    val content: String
)

@Serializable
data class CreateDiaryCommentResponse(
    val success: Boolean = false,
    val comment: DiaryComment? = null,
    val error: String? = null
)

@Serializable
data class DeleteDiaryCommentRequest(
    @SerialName("comment_id") val commentId: String
)

@Serializable
data class DiaryListResponse(
    val owner: UserProfile? = null,
    val entries: List<DiaryEntry> = emptyList(),
    val isSelf: Boolean = false
)

@Serializable
data class CreateDiaryInput(
    val mediaType: String,
    val mediaUrl: String? = null,
    val posterUrl: String? = null,
    val caption: String? = null,
    val visibility: String = "private",
    val goalId: String? = null
)

@Serializable
data class CreateDiaryResponse(
    val entry: DiaryEntry? = null,
    val error: String? = null
)

@Serializable
data class MarkDiarySeenInput(
    val ownerId: String
)

@Serializable
data class DeleteDiaryInput(
    val entryId: String
)

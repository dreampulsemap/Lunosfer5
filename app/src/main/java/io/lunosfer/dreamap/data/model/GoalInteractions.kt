package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Vision / Goal Creation ---

@Serializable
data class RoadmapItemInput(
    val title: String
)

@Serializable
data class CreateGoalRequest(
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("cover_image_source") val coverImageSource: String? = null,
    @SerialName("target_date") val targetDate: String? = null,
    val visibility: String? = null,
    val roadmap: List<RoadmapItemInput>? = null
)

@Serializable
data class CreateGoalResponse(
    val goal: Goal? = null,
    val microGoals: List<MicroGoal>? = null,
    val error: String? = null
)

// --- Update Status ---

@Serializable
data class UpdateGoalStatusRequest(
    val goalId: String,
    val status: String, // "completed" | "abandoned"
    val story: String? = null
)

@Serializable
data class UpdateGoalStatusResponse(
    val goal: Goal? = null,
    val error: String? = null
)

// --- Delete Goal ---

@Serializable
data class DeleteGoalRequest(
    val goalId: String
)

// --- Save / Bookmark Goal ---

@Serializable
data class SaveGoalRequest(
    val goalId: String
)

@Serializable
data class SaveGoalResponse(
    val saved: Boolean = false,
    val error: String? = null
)

// --- Klonla ("Kendi Vizyonlarıma Ekle") ---
// Başkasına ait (visibility=public) bir vizyonu, aynı içerikle (başlık,
// açıklama, kapak + varsa vizyon videosu/slaytları) kullanıcının kendi
// vizyonlarına yeni bir goals satırı olarak kopyalar. goals.source_goal_id
// (bkz. Supabase migration) sayesinde aynı vizyon ikinci kez eklenmeye
// çalışılırsa backend hata yerine already_cloned:true döner — pages/api/
// goals/report.js'teki already_reported ile birebir aynı desen.
@Serializable
data class CloneGoalRequest(
    val goalId: String
)

@Serializable
data class CloneGoalResponse(
    val success: Boolean = false,
    @SerialName("already_cloned") val alreadyCloned: Boolean = false,
    val goal: Goal? = null,
    val error: String? = null
)

// --- Give Mana ---

@Serializable
data class GiveManaRequest(
    val goalId: String,
    val amount: Int = 1
)

@Serializable
data class GiveManaResponse(
    @SerialName("manaBalance") val manaBalance: Int? = null,
    val error: String? = null
)

// --- Goal Comments ---

@Serializable
data class GoalComment(
    val id: String,
    @SerialName("goal_id") val goalId: String? = null,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_profiles") val userProfile: UserProfile? = null
)

@Serializable
data class GoalCommentsResponse(
    val comments: List<GoalComment> = emptyList()
)

@Serializable
data class CreateGoalCommentRequest(
    val goalId: String,
    val content: String
)

@Serializable
data class CreateGoalCommentResponse(
    val comment: GoalComment? = null,
    val error: String? = null
)

@Serializable
data class DeleteGoalCommentRequest(
    val commentId: String
)

package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** "Ortak Vizyon" — bir vizyona (goal) davet edilen/katılan işbirlikçi. */
@Serializable
data class GoalCollaborator(
    val id: Long,
    @SerialName("goal_id") val goalId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("invited_by") val invitedBy: String,
    val status: String = "pending", // "pending" | "accepted" | "declined"
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_profiles") val userProfile: UserProfile? = null,
    val goals: Goal? = null
)

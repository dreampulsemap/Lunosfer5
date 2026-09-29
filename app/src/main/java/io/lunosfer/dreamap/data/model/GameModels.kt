package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * public.get_my_progress() yaniti. XP miktarlari, limitler, rutbe esikleri,
 * rozetler ve gunluk gorevler SADECE sunucuda tanimli (bkz. dreamap-frontend
 * supabase/migrations/016_gamification_xp_ranks.sql); istemci bunlari buradan okur.
 */
@Serializable
data class GameProgress(
    val xp: Int = 0,
    val rank: Int = 0,
    @SerialName("max_rank") val maxRank: Int = 9,
    @SerialName("rank_min_xp") val rankMinXp: Int = 0,
    @SerialName("next_rank_xp") val nextRankXp: Int? = null,
    val thresholds: List<Int> = emptyList(),
    @SerialName("onboarding_status") val onboardingStatus: String = "none",
    @SerialName("is_guest") val isGuest: Boolean = true,
    @SerialName("today_xp") val todayXp: Int = 0,
    @SerialName("weekly_xp") val weeklyXp: Int = 0,
    val quests: List<DailyQuest> = emptyList(),
    @SerialName("quests_bonus_xp") val questsBonusXp: Int = 0,
    @SerialName("quests_bonus_claimed") val questsBonusClaimed: Boolean = false,
    val badges: List<BadgeState> = emptyList(),
    val rules: List<XpRule> = emptyList(),
    val recent: List<XpEvent> = emptyList()
) {
    /** Mevcut rutbe icindeki ilerleme (0..1); en ust rutbede 1. */
    val rankProgress: Float
        get() {
            val next = nextRankXp ?: return 1f
            val span = (next - rankMinXp).coerceAtLeast(1)
            return ((xp - rankMinXp).toFloat() / span).coerceIn(0f, 1f)
        }

    val earnedBadgeCodes: Set<String> get() = badges.filter { it.earnedAt != null }.map { it.code }.toSet()
}

@Serializable
data class DailyQuest(
    val code: String,
    val target: Int = 1,
    val xp: Int = 0,
    val progress: Int = 0
) {
    val done: Boolean get() = progress >= target
}

@Serializable
data class BadgeState(
    val code: String,
    val xp: Int = 0,
    @SerialName("earned_at") val earnedAt: String? = null
)

@Serializable
data class XpRule(
    val reason: String,
    val xp: Int,
    @SerialName("daily_cap") val dailyCap: Int? = null
)

@Serializable
data class XpEvent(
    val reason: String,
    val delta: Int,
    @SerialName("created_at") val createdAt: String? = null,
    val badge: String? = null
)

@Serializable
data class LeaderboardEntry(
    val pos: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String? = null,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val xp: Int = 0,
    val rank: Int = 0,
    @SerialName("is_me") val isMe: Boolean? = false
)

@Serializable
data class PublicProgress(
    val xp: Int = 0,
    val rank: Int = 0,
    val badges: List<String> = emptyList()
)

@Serializable
data class OnboardingResult(
    val awarded: Int = 0,
    val status: String? = null
)

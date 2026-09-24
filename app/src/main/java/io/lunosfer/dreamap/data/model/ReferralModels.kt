package io.lunosfer.dreamap.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DIKKAT: bu model onceden backend'in HIC gondermedigi alan adlarini
// bekliyordu (totalReferrals/totalManaEarned/claimableMana, ve claim
// istegi inviterCode yerine referralCode/code gonderiyordu) — sonuc:
// istatistikler her zaman 0/0 gorunuyordu VE bir arkadas kodu girmek
// backend'de HER ZAMAN "inviterCode_required" (400) ile patliyordu.
// Ozellik cift yonde de fiilen calismiyordu. Gercek backend semasi
// (pages/api/referrals/stats.js, pages/api/referrals/claim.js — web
// tarafi ReferralWidget.jsx zaten bu alanlari dogru kullaniyordu) ile
// eslesecek sekilde duzeltildi.
@Serializable
data class ReferralStatsResponse(
    @SerialName("referralCode") val referralCode: String? = null,
    @SerialName("totalInvited") val totalInvited: Int = 0,
    @SerialName("totalCreditsEarned") val totalCreditsEarned: Int = 0,
    val referrals: List<ReferralHistoryItem> = emptyList(),
    val error: String? = null
) {
    val displayCode: String get() = referralCode ?: "—"
}

@Serializable
data class ReferralHistoryItem(
    val id: String? = null,
    @SerialName("invited_user_id") val invitedUserId: String? = null,
    @SerialName("reward_amount") val rewardAmount: Int = 0,
    @SerialName("reward_granted") val rewardGranted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ClaimReferralRequest(
    @SerialName("inviterCode") val inviterCode: String
)

@Serializable
data class ClaimReferralResponse(
    val referral: ReferralHistoryItem? = null,
    val error: String? = null
) {
    val creditsAwarded: Int get() = referral?.rewardAmount ?: 0
}

package io.lunosfer.dreamap.data.model

import kotlinx.serialization.Serializable

/**
 * pages/api/dreams/collective-stats.js — GET, no body. Response is already
 * flat camelCase server-side (see that route's toApiShape()), so no
 * @SerialName overrides are needed here, unlike SummaryData's snake_case
 * fallbacks.
 *
 * `available` is false when the last-24h sample was too small to report
 * without being statistically meaningless (and potentially identifying) —
 * topArchetype/percentage are null in that case. Always check `available`
 * before showing the card.
 */
@Serializable
data class CollectiveStatsResponse(
    val ok: Boolean? = true,
    val available: Boolean = false,
    val topArchetype: String? = null,
    val percentage: Int? = null,
    val sampleSize: Int? = null,
    val windowStart: String? = null,
    val windowEnd: String? = null,
    val error: String? = null
)

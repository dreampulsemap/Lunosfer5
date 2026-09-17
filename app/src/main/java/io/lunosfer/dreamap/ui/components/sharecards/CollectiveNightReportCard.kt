package io.lunosfer.dreamap.ui.components.sharecards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.CollectiveStatsResponse

/**
 * "Kolektif Gece Raporu" — now backed by a real endpoint
 * (pages/api/dreams/collective-stats.js, wired via
 * LunosferApi.getCollectiveStats() / CollectiveStatsRepository).
 *
 * Returns null when `available` is false — the endpoint deliberately
 * withholds the stat when the last-24h sample is too small to be
 * meaningful (or safely anonymous). Skip rendering this card entirely
 * in that case rather than showing a 0%/blank version.
 */
fun CollectiveStatsResponse.toCardData(dateLabel: String): CollectiveNightReportCardData? {
    if (!available) return null
    val archetype = topArchetype ?: return null
    val pct = percentage ?: return null
    return CollectiveNightReportCardData(
        percentage = pct,
        symbolName = archetype,
        dateLabel = dateLabel
    )
}

data class CollectiveNightReportCardData(
    val percentage: Int,
    val symbolName: String,
    val dateLabel: String
)

@Composable
fun CollectiveNightReportCard(
    data: CollectiveNightReportCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .background(CardPalette.cardBackground)
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .background(Brush.radialGradient(listOf(GlowGold.copy(alpha = 0.20f), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CardEyebrow(stringResource(R.string.sharecard_collective_eyebrow))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "%${data.percentage}",
                    color = GlowGold,
                    fontFamily = TitleFont,
                    fontSize = 64.sp
                )
                Text(
                    text = stringResource(R.string.sharecard_collective_body, data.symbolName),
                    color = CardPalette.textPrimary,
                    fontFamily = LabelFont,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(data.dateLabel, color = CardPalette.textSecondary, fontFamily = LabelFont, fontSize = 11.sp)
                CardWatermark()
            }
        }
    }
}

package io.lunosfer.dreamap.ui.components.sharecards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.SummaryData

data class WeeklySynthesisCardData(
    val periodLabel: String,
    val dreamCount: Int,
    val dominantArchetypesLabel: String,
    val dominantSentimentLabel: String,
    val summaryLine: String
)

/**
 * "Dönüşüm Üçgeni Sentez Kartı" — the best-backed of the three vision/günce
 * concepts: SummaryData already has everything this card needs (dreamCount,
 * dominantArchetypes, dominantSentiment, summaryText), no stand-in fields
 * required. periodLabel is left as a caller-supplied string since
 * SummaryData.type is a raw "weekly"/"monthly" key, not a display label —
 * format it via stringResource wherever you already localize period names.
 */
fun SummaryData.toSynthesisCardData(periodLabel: String): WeeklySynthesisCardData =
    WeeklySynthesisCardData(
        periodLabel = periodLabel,
        dreamCount = dreamCount ?: 0,
        dominantArchetypesLabel = dominantArchetypes?.take(2)?.joinToString(" · ").orEmpty(),
        dominantSentimentLabel = dominantSentiment.orEmpty(),
        summaryLine = text
    )

@Composable
fun WeeklySynthesisCard(
    data: WeeklySynthesisCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .background(CardPalette.synthesisBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CardEyebrow(data.periodLabel)

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = data.summaryLine,
                    color = CardPalette.textPrimary,
                    fontFamily = TitleFont,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    maxLines = 4
                )
                HorizontalDivider(color = GlowGold.copy(alpha = 0.3f), thickness = 1.dp)
                CardStatRow(stringResource(R.string.sharecard_synthesis_dream_count), data.dreamCount.toString(), accent = GlowIndigo)
                if (data.dominantArchetypesLabel.isNotBlank()) {
                    CardStatRow(stringResource(R.string.sharecard_synthesis_archetypes), data.dominantArchetypesLabel, accent = GlowGold)
                }
                if (data.dominantSentimentLabel.isNotBlank()) {
                    CardStatRow(stringResource(R.string.sharecard_synthesis_sentiment), data.dominantSentimentLabel, accent = CardPalette.textPrimary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardWatermark()
            }
        }
    }
}

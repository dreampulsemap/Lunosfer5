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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.AiJungianAnalysis
import io.lunosfer.dreamap.data.model.DreamDetail

data class DreamArchetypeCardData(
    val archetypeName: String,
    val symbolLabel: String?,
    val sentimentLabel: String?,
    val dateLabel: String,
    val imageUrl: String?
)

/**
 * Maps the real API model to the card's data contract.
 *
 * Was previously using DreamDetail.tags as a guess for "key symbol" — turned
 * out wrong (confirmed against the backend: tags are the user's own free-text
 * labels, set in update-dream.js, never AI-generated). Fixed: analyze-dream.js
 * now generates a dedicated `symbol` field in the same multi-lang shape as
 * title/summary/motiv. Falls back to tags only for dreams analyzed before
 * this field existed, so old content doesn't just go blank.
 *
 * archetypes is a List<String> with no ranking/primary flag, so this takes
 * the first entry.
 */
fun DreamDetail.toArchetypeCardData(locale: String = "tr"): DreamArchetypeCardData {
    val analysis: AiJungianAnalysis? = aiJungianAnalysis
    return DreamArchetypeCardData(
        archetypeName = analysis?.archetypes?.firstOrNull()
            ?: (analysis?.title).localized(locale)
            ?: content.take(40),
        symbolLabel = (analysis?.symbol).localized(locale) ?: tags?.firstOrNull(),
        sentimentLabel = analysis?.sentiment ?: userSelectedSentiment,
        dateLabel = dreamDate,
        imageUrl = displayImageUrl
    )
}

@Composable
fun DreamArchetypeCard(
    data: DreamArchetypeCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .background(CardPalette.cardBackground)
    ) {
        if (data.imageUrl != null) {
            AsyncImage(
                model = data.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        } else {
            // No dream image yet — signature glow carries the card instead.
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = 70.dp)
                    .background(Brush.radialGradient(listOf(GlowViolet.copy(alpha = 0.35f), Color.Transparent)))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CardEyebrow(stringResource(R.string.sharecard_dream_archetype_eyebrow))

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = data.archetypeName,
                    color = CardPalette.textPrimary,
                    fontFamily = TitleFont,
                    fontSize = 36.sp,
                    lineHeight = 42.sp
                )
                HorizontalDivider(color = GlowGold.copy(alpha = 0.3f), thickness = 1.dp)
                data.sentimentLabel?.let {
                    CardStatRow(stringResource(R.string.sharecard_dream_sentiment_label), it, accent = CardPalette.textPrimary)
                }
                data.symbolLabel?.let {
                    CardStatRow(stringResource(R.string.sharecard_dream_symbol_label), it, accent = GlowGold)
                }
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

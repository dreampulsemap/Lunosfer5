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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Goal

data class VisionMessageCardData(
    val message: String,
    val visionTitle: String,
    val dateLabel: String
)

/**
 * Backend now generates this (pages/api/goals/generate-future-message.js,
 * called separately from goal creation — same pattern as dreams: create
 * fast, enrich with AI as a follow-up call). Goal.aiFutureMessage is null
 * until that endpoint has run for a given goal at least once, so this
 * still falls back to description/title for anything not yet processed.
 */
fun Goal.toMessageCardData(dateLabel: String): VisionMessageCardData =
    VisionMessageCardData(
        message = aiFutureMessage?.takeIf { it.isNotBlank() }
            ?: description?.takeIf { it.isNotBlank() }
            ?: title,
        visionTitle = title,
        dateLabel = dateLabel
    )

@Composable
fun VisionMessageCard(
    data: VisionMessageCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .background(CardPalette.cardBackground)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-50).dp)
                .background(Brush.radialGradient(listOf(GlowGold.copy(alpha = 0.35f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            CardEyebrow(stringResource(R.string.sharecard_vision_eyebrow))

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = data.message,
                    color = CardPalette.textPrimary,
                    fontFamily = TitleFont,
                    fontStyle = FontStyle.Italic,
                    fontSize = 25.sp,
                    lineHeight = 34.sp
                )
            }

            Column {
                HorizontalDivider(color = GlowGold.copy(alpha = 0.3f), thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        data.visionTitle.uppercase(),
                        color = CardPalette.textSecondary,
                        fontFamily = LabelFont,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    CardWatermark()
                }
            }
        }
    }
}

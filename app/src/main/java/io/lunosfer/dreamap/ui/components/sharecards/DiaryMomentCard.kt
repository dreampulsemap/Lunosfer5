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
import io.lunosfer.dreamap.data.model.DiaryEntry

data class DiaryMomentCardData(
    val captionLine: String,
    val dateLabel: String,
    val linkedGoalTitle: String?
)

/**
 * Renamed from the brainstorm's "Günce & Zihin Spektrumu Kartı" on purpose:
 * DiaryEntry has no mood/focus-percentage field (that card's "%85 Zihin
 * Odaklılığı" stat had nothing to back it), so rather than fabricate a
 * number this leans into the caption itself as the featured line — same
 * spirit as the poetic dream card, applied to the diary. Entries with no
 * caption fall back to the linked goal's title so the card is never blank.
 */
fun DiaryEntry.toMomentCardData(dateLabel: String): DiaryMomentCardData =
    DiaryMomentCardData(
        captionLine = caption?.takeIf { it.isNotBlank() } ?: goalTitle.orEmpty(),
        dateLabel = dateLabel,
        linkedGoalTitle = goalTitle
    )

@Composable
fun DiaryMomentCard(
    data: DiaryMomentCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .background(CardPalette.cardBackground)
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .background(Brush.radialGradient(listOf(GlowCyan.copy(alpha = 0.28f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            CardEyebrow(stringResource(R.string.sharecard_diary_eyebrow))

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = data.captionLine,
                    color = CardPalette.textPrimary,
                    fontFamily = TitleFont,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    lineHeight = 32.sp
                )
            }

            Column {
                HorizontalDivider(color = GlowCyan.copy(alpha = 0.3f), thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        data.linkedGoalTitle?.uppercase() ?: data.dateLabel,
                        color = CardPalette.textSecondary,
                        fontFamily = LabelFont,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    CardWatermark()
                }
            }
        }
    }
}

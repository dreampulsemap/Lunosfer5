package io.lunosfer.dreamap.ui.components.sharecards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.data.model.DreamDetail
import io.lunosfer.dreamap.ui.theme.Void950

data class DreamPoeticCardData(
    val line: String,
    val dateLabel: String
)

/**
 * Falls back to summary -> motiv -> the dream's own content — whichever
 * locale-keyed field actually has text for this dream. If none of the AI
 * fields are populated yet (analysis still pending), this reads from the
 * user's own written content instead of showing an empty card.
 */
fun DreamDetail.toPoeticCardData(locale: String = "tr"): DreamPoeticCardData {
    val analysis = aiJungianAnalysis
    val line = analysis?.summary.localized(locale)
        ?: analysis?.motiv.localized(locale)
        ?: content.take(140)
    return DreamPoeticCardData(line = line, dateLabel = dreamDate)
}

/**
 * "Şiirsel Tipografi Kartı" — deliberately the quietest card in the set.
 * Near-black, one line of italic serif type, nothing else competing for
 * attention. The restraint IS the design here, unlike the other cards'
 * glow accent.
 */
@Composable
fun DreamPoeticCard(
    data: DreamPoeticCardData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .background(Void950)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = data.line,
                    color = CardPalette.textPrimary,
                    fontFamily = TitleFont,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
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

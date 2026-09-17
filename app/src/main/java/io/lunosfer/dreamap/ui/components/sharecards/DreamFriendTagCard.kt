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

/**
 * "Rüyamda Seni Gördüm" — deliberately takes plain strings rather than a
 * UserProfile/friend-search model, since that model lives wherever the
 * tag-a-friend picker UI ends up (AddFriendScreen-adjacent, not written
 * here). Wire it up by passing friend.nameOrFallback from whatever
 * UserProfile the picker returns.
 */
data class DreamFriendTagCardData(
    val symbolOrArchetype: String,
    val friendDisplayName: String,
    val dateLabel: String
)

@Composable
fun DreamFriendTagCard(
    data: DreamFriendTagCardData,
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
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = 60.dp)
                .background(Brush.radialGradient(listOf(GlowCyan.copy(alpha = 0.30f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .background(Brush.radialGradient(listOf(GlowGold.copy(alpha = 0.22f), Color.Transparent)))
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CardEyebrow(stringResource(R.string.sharecard_friend_tag_eyebrow, data.friendDisplayName))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = data.symbolOrArchetype,
                    color = CardPalette.textPrimary,
                    fontFamily = TitleFont,
                    fontStyle = FontStyle.Italic,
                    fontSize = 32.sp,
                    lineHeight = 40.sp
                )
                HorizontalDivider(color = GlowCyan.copy(alpha = 0.3f), thickness = 1.dp)
                Text(
                    text = stringResource(R.string.sharecard_friend_tag_caption),
                    color = CardPalette.textSecondary,
                    fontFamily = LabelFont,
                    fontSize = 14.sp
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

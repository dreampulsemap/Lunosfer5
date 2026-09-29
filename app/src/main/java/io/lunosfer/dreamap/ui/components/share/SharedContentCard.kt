package io.lunosfer.dreamap.ui.components.share

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.SharedRef
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.Void950

/** DM balonunda paylaşılan rüya / günce / vizyon kartı. */
@Composable
fun SharedContentCard(ref: SharedRef, isOwn: Boolean, onOpen: () -> Unit) {
    val primary = if (isOwn) Void950 else Color.White
    val secondary = if (isOwn) Void950.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.72f)
    Column(Modifier.width(240.dp).clickable(onClick = onOpen)) {
        if (!ref.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ref.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(150.dp)
            )
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                "${sharedEmoji(ref.type)} ${stringResource(sharedTypeRes(ref.type)).uppercase()}",
                color = if (isOwn) Void950.copy(alpha = 0.6f) else AstralGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            if (!ref.title.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(ref.title, color = primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!ref.excerpt.isNullOrBlank() && ref.excerpt != ref.title) {
                Spacer(Modifier.height(2.dp))
                Text(ref.excerpt, color = secondary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (!ref.ownerName.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("— ${ref.ownerName}", color = secondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

fun sharedEmoji(type: String): String = when (type) {
    "dream" -> "🌙"
    "diary" -> "📖"
    else -> "✨"
}

@StringRes
fun sharedTypeRes(type: String): Int = when (type) {
    "dream" -> R.string.share_card_dream
    "diary" -> R.string.share_card_diary
    else -> R.string.share_card_vision
}

package io.lunosfer.dreamap.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text that shrinks its font size (one step at a time) until it fits on a single
 * line within the space it's given, so it never gets clipped/wrapped on narrow
 * screens (e.g. the "LUNOSFER" wordmark in the top bar). Compatible with older
 * Compose BOMs that don't yet ship the built-in autoSize Text API.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = style.fontSize,
    minFontSize: TextUnit = 10.sp,
    stepSize: TextUnit = 1.sp
) {
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text, maxFontSize) { mutableStateOf(false) }

    BasicText(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        style = style.copy(fontSize = fontSize),
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                val next = fontSize.value - stepSize.value
                fontSize = if (next < minFontSize.value) minFontSize else next.sp
            } else {
                readyToDraw = true
            }
        }
    )
}

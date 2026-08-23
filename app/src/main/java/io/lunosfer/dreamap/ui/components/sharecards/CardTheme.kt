package io.lunosfer.dreamap.ui.components.sharecards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.AetherCyan
import io.lunosfer.dreamap.ui.theme.AetherIndigo
import io.lunosfer.dreamap.ui.theme.AetherViolet
import io.lunosfer.dreamap.ui.theme.AstralAmber
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.SansFontFamily
import io.lunosfer.dreamap.ui.theme.SerifFontFamily
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950

/**
 * Design tokens for the shareable cards, built ONLY from what already exists
 * in ui/theme/Color.kt + Type.kt — no invented hex values. The app is
 * dark-mode-only (see Theme.kt), so every card stays on the Void background
 * family rather than introducing a separate light palette; each content
 * type is told apart by which accent leads, not by background lightness.
 *
 * Accent assignment:
 *   Rüya / Arketip   -> AetherViolet  (mystical, inward)
 *   Rüya / Şiirsel    -> AetherIndigo (quiet — deliberately the least glow)
 *   Rüya / Arkadaş    -> AetherCyan   (social/connection)
 *   Gece Raporu       -> AstralGold   (the "big stat" card)
 *   Vizyon            -> AstralGold / AstralAmber (matches VisionGridCard's
 *                         existing use of AstralGold for progress/believers)
 *   Günce             -> AetherCyan   (calm, present-moment)
 *   Haftalık Sentez   -> AetherIndigo -> AstralGold gradient (rüya -> vizyon)
 */
object CardPalette {
    val cardBackground = Brush.verticalGradient(listOf(Void950, Void900, Void950))
    val synthesisBackground = Brush.verticalGradient(listOf(Void950, Color(0xFF241C3D), Void950))

    val textPrimary = Color.White
    val textSecondary = Color(0xFFC8C3DA)
}

// Design-time card size (9:16, Story ratio). On a standard xxhdpi (3x)
// device this rasterizes close to 1080x1920; on other densities the aspect
// ratio still holds. Force an exact pixel size afterwards with
// Bitmap.createScaledBitmap if you need one regardless of device.
val CardWidth: Dp = 360.dp
val CardHeight: Dp = 640.dp

/**
 * Looks up a locale-keyed AI content map (AiJungianAnalysis.title/summary/motiv
 * are Map<langCode, text>) the same way the rest of the app should: requested
 * locale, then English, then Turkish, then whatever's there. Used by the
 * per-card mapper functions in this package.
 */
fun Map<String, String>?.localized(locale: String): String? {
    if (this == null) return null
    return this[locale] ?: this["en"] ?: this["tr"] ?: values.firstOrNull()
}

@Composable
fun CardEyebrow(text: String, tint: Color = CardPalette.textSecondary, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = tint,
        fontFamily = SansFontFamily,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun CardStatRow(
    label: String,
    value: String,
    accent: Color = CardPalette.textPrimary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label.uppercase(),
            color = CardPalette.textSecondary,
            fontFamily = SansFontFamily,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Text(
            value,
            color = accent,
            fontFamily = SansFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Simplified Compose recreation of the app icon's brand mark (see
 * res/drawable/ic_launcher_foreground.xml: two overlapping rings, night
 * dream journal over day vision tracking, meeting point as the "insight"
 * dot) — small enough for an inline watermark, not a pixel copy of the
 * vector. Same AstralGold-family gradient as the real icon.
 */
@Composable
private fun LunosferRingMark(size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val goldGradient = Brush.linearGradient(listOf(Color(0xFFF2DDA8), Color(0xFFD9B166)))
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension * 0.34f
        val strokeWidth = this.size.minDimension * 0.11f
        val cx = this.size.width / 2f
        val topCenter = Offset(cx, this.size.height * 0.40f)
        val bottomCenter = Offset(cx, this.size.height * 0.60f)
        drawCircle(brush = goldGradient, radius = r, center = topCenter, style = Stroke(width = strokeWidth))
        drawCircle(brush = goldGradient, radius = r, center = bottomCenter, style = Stroke(width = strokeWidth))
        drawCircle(brush = goldGradient, radius = strokeWidth * 0.4f, center = Offset(cx, this.size.height / 2f))
    }
}

@Composable
fun CardWatermark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LunosferRingMark()
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            color = AstralGold,
            fontFamily = SansFontFamily,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Re-exported so card files only need to import from this one file for accents.
val GlowViolet = AetherViolet
val GlowIndigo = AetherIndigo
val GlowCyan = AetherCyan
val GlowGold = AstralGold
val GlowAmber = AstralAmber
val TitleFont = SerifFontFamily
val LabelFont = SansFontFamily

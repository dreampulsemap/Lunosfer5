package io.lunosfer.dreamap.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.CompassUiState
import kotlinx.coroutines.delay
import java.util.Calendar

private const val HOLD_DURATION_MS = 2000f

/**
 * Ana sayfadaki "basili tut ve gunun okumasini ac" pusulasi — web'deki
 * components/DailyCompass.jsx ile ayni ritueli uygular: 2 saniye basili
 * tutma, alttan dolan ilerleme halkasi, sonrasinda arketip + okuma karti ve
 * hikayede paylasma. Gun icinde tekrar girildiginde okuma kayitli kaldigi
 * icin (bkz. VisionViewModel) kart dolu gorunur; hic cekilmediyse gece
 * yarisina kalan sure sayar.
 */
@Composable
fun DailyCompassHoldCard(
    state: CompassUiState,
    onDraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canDraw = state is CompassUiState.Idle || state is CompassUiState.Error

    var pressing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pressing, canDraw) {
        if (!pressing) {
            progress = 0f
            return@LaunchedEffect
        }
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = (System.currentTimeMillis() - startTime).toFloat()
            progress = (elapsed / HOLD_DURATION_MS).coerceAtMost(1f)
            if (progress >= 1f) {
                onDraw()
                break
            }
            withFrameNanos { }
        }
    }

    val accent = (state as? CompassUiState.Success)?.color?.let { parseCompassColor(it) } ?: AstralGold

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Void900.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "🧭 " + stringResource(R.string.vision_daily_compass_title),
                color = AstralGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            when (state) {
                is CompassUiState.Success -> CompassReading(
                    archetype = state.archetype,
                    reading = state.reading,
                    accent = accent,
                    onShare = {
                        val body = context.getString(
                            R.string.compass_share_text,
                            state.archetype ?: "",
                            state.reading
                        )
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        context.startActivity(
                            Intent.createChooser(send, context.getString(R.string.compass_share_story))
                        )
                    }
                )

                is CompassUiState.AlreadyUsedToday -> CompassCountdown()

                else -> {
                    val loading = state is CompassUiState.Loading
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Void950)
                            .border(1.dp, AstralGold.copy(alpha = 0.3f), CircleShape)
                            .then(
                                if (canDraw) {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                pressing = true
                                                tryAwaitRelease()
                                                pressing = false
                                            }
                                        )
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(progress)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                AetherCyan.copy(alpha = 0.4f),
                                                AstralGold.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                            )
                        }
                        if (loading) {
                            CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AstralGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = if (loading) stringResource(R.string.compass_decoding)
                        else stringResource(R.string.compass_hold_instruction),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    if (state is CompassUiState.Error) {
                        Text(
                            text = state.message,
                            color = SemanticDanger400,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompassReading(
    archetype: String?,
    reading: String,
    accent: Color,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text("👁️", fontSize = 26.sp)

    if (!archetype.isNullOrBlank()) {
        Text(
            text = archetype.uppercase(),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center
        )
    }

    Text(
        text = "“$reading”",
        color = Color.White,
        fontSize = 15.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center,
        maxLines = if (expanded) Int.MAX_VALUE else 6,
        modifier = Modifier.clickable { expanded = !expanded }
    )

    Button(
        onClick = onShare,
        colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
        shape = RoundedCornerShape(50)
    ) {
        Icon(Icons.Default.Share, contentDescription = null, tint = Void950, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.compass_share_story),
            color = Void950,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun CompassCountdown() {
    var timeLeft by remember { mutableStateOf(formatTimeUntilMidnight()) }
    LaunchedEffect(Unit) {
        while (true) {
            timeLeft = formatTimeUntilMidnight()
            delay(1000)
        }
    }

    Text("⏳", fontSize = 22.sp)
    Text(
        text = stringResource(R.string.compass_realigning),
        color = Color.Gray,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
    )
    Text(
        text = timeLeft,
        color = AstralGold,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
    Text(
        text = stringResource(R.string.compass_refresh_note),
        color = Color.Gray,
        fontSize = 10.sp,
        textAlign = TextAlign.Center
    )
}

private fun formatTimeUntilMidnight(): String {
    val now = Calendar.getInstance()
    val tomorrow = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var diff = (tomorrow.timeInMillis - now.timeInMillis) / 1000
    if (diff < 0) diff = 0
    val h = diff / 3600
    val m = (diff % 3600) / 60
    val s = diff % 60
    return "${h}h ${m}m ${s}s"
}

private fun parseCompassColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(AstralGold)

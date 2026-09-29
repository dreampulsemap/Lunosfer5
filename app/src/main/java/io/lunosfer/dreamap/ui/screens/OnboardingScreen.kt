package io.lunosfer.dreamap.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.components.RankEmblem
import io.lunosfer.dreamap.ui.components.rankNameRes
import io.lunosfer.dreamap.ui.theme.AetherCyan
import io.lunosfer.dreamap.ui.theme.AetherIndigo
import io.lunosfer.dreamap.ui.theme.AetherViolet
import io.lunosfer.dreamap.ui.theme.AstralAmber
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.MoonSilver
import io.lunosfer.dreamap.ui.theme.SemanticSuccess400
import io.lunosfer.dreamap.ui.theme.SerifFontFamily
import io.lunosfer.dreamap.ui.theme.ShadowWorkRose
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Turu kim izliyor: ödül/son ekran metni buna göre değişir. */
enum class OnboardingMode {
    /** Gerçek hesap, ödülü henüz almamış. */
    ACCOUNT,
    /** Gerçek hesap, turu daha önce tamamlamış (tekrar oynuyor). */
    ACCOUNT_REPLAY,
    /** Girişsiz ya da misafir: ödül hesap açınca verilir. */
    VISITOR
}

// Her bölüm +10 -> 5 bölüm = sunucudaki xp_rules('onboarding_completed') = 50.
// ponytail: istemci sabiti; sunucu kuralı değişirse burası da güncellenmeli.
private const val XP_PER_CHAPTER = 10
private const val CHAPTER_COUNT = 5

private data class Chapter(@StringRes val title: Int, @StringRes val body: Int?, @StringRes val action: Int)

private val CHAPTERS = listOf(
    Chapter(R.string.onb_c1_title, R.string.onb_c1_body, R.string.onb_c1_action),
    Chapter(R.string.onb_c2_title, R.string.onb_c2_body, R.string.onb_c2_action),
    Chapter(R.string.onb_c3_title, R.string.onb_c3_body, R.string.onb_c3_action),
    Chapter(R.string.onb_c4_title, R.string.onb_c4_body, R.string.onb_c4_action),
    Chapter(R.string.onb_c5_title, null, R.string.onb_c5_action)
)

/**
 * İlk kez açan / kayıt olan kullanıcıya uygulamayı oyun gibi öğreten tur.
 * Her bölümde küçük bir etkileşim (dokun) var; tamamlanınca "Devam" açılır.
 * "Atla" her an görünür. [onFinish] true = tamamlandı, false = atlandı.
 */
@Composable
fun OnboardingScreen(
    mode: OnboardingMode,
    currentRank: Int,
    currentXp: Int,
    onFinish: (completed: Boolean) -> Unit
) {
    var chapter by rememberSaveable { mutableIntStateOf(0) }
    var doneMask by rememberSaveable { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    val showXp = mode != OnboardingMode.ACCOUNT_REPLAY

    fun isDone(i: Int) = doneMask and (1 shl i) != 0
    fun markDone(i: Int) {
        doneMask = doneMask or (1 shl i)
    }

    val tourXp by animateIntAsState(Integer.bitCount(doneMask) * XP_PER_CHAPTER, animationSpec = tween(700), label = "tourXp")

    BackHandler {
        when {
            finished -> finished = false
            chapter > 0 -> chapter--
            else -> onFinish(false)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Void950)
            // Alttaki ekrana dokunuş geçmesin.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        StarField()
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                // Tablette metin tüm genişliğe yayılmasın.
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChapterProgress(
                    current = if (finished) CHAPTER_COUNT else chapter,
                    doneMask = doneMask,
                    modifier = Modifier.weight(1f)
                )
                if (showXp) {
                    Spacer(Modifier.width(10.dp))
                    XpPill(tourXp)
                }
                if (!finished) {
                    TextButton(onClick = { onFinish(false) }) {
                        Text(stringResource(R.string.onb_skip), color = MoonSilver)
                    }
                }
            }

            if (finished) {
                FinalPanel(
                    mode = mode,
                    currentRank = currentRank,
                    currentXp = currentXp,
                    earnedXp = CHAPTER_COUNT * XP_PER_CHAPTER,
                    onDone = { onFinish(true) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                AnimatedContent(
                    targetState = chapter,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it else it } + fadeOut())
                    },
                    modifier = Modifier.weight(1f),
                    label = "chapter"
                ) { c ->
                    ChapterPage(
                        index = c,
                        done = isDone(c),
                        onDone = { markDone(c) }
                    )
                }

                // Butonun hemen üstünde sabit: kısa/yatay ekranda kaydırmayla kaybolmasın.
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    ActionHint(done = isDone(chapter), text = stringResource(CHAPTERS[chapter].action), showXp = showXp)
                }

                Button(
                    onClick = { if (chapter < CHAPTER_COUNT - 1) chapter++ else finished = true },
                    enabled = isDone(chapter),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AstralGold, disabledContainerColor = Void800)
                ) {
                    Text(
                        stringResource(R.string.onb_next),
                        color = if (isDone(chapter)) Void950 else MoonSilver,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isDone(chapter)) Void950 else MoonSilver,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Sahneler bu yükseklik için tasarlandı; alan azsa bütün sahne orantılı küçülür.
private val SCENE_HEIGHT = 270.dp

@Composable
private fun ChapterPage(index: Int, done: Boolean, onDone: () -> Unit) {
    val chapter = CHAPTERS[index]
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Son bölümde açıklama yerine 4 satırlık kural listesi var: sahneye daha az yer.
        val sceneFraction = if (chapter.body == null) 0.36f else 0.45f
        val sceneHeight = (maxHeight * sceneFraction).coerceIn(150.dp, SCENE_HEIGHT)
        val sceneScale = sceneHeight / SCENE_HEIGHT
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(sceneHeight), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .requiredHeight(SCENE_HEIGHT)
                        .graphicsLayer {
                            scaleX = sceneScale
                            scaleY = sceneScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (index) {
                        0 -> MoonScene(done, onDone)
                        1 -> DreamCardScene(done, onDone)
                        2 -> ManaScene(done, onDone)
                        3 -> HeartScene(done, onDone)
                        else -> ChestScene(done, onDone)
                    }
                }
            }
            ChapterTexts(chapter)
        }
    }
}

@Composable
private fun ChapterTexts(chapter: Chapter) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(chapter.title),
            color = AstralGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SerifFontFamily,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(Modifier.height(10.dp))
        if (chapter.body != null) {
            Text(
                stringResource(chapter.body),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        } else {
            GameRules()
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActionHint(done: Boolean, text: String, showXp: Boolean) {
    val alpha by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "hintAlpha"
    )
    if (done) {
        if (showXp) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SemanticSuccess400.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SemanticSuccess400, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.xp_toast_format, XP_PER_CHAPTER), color = SemanticSuccess400, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Text(
            "✦ $text ✦",
            color = AetherCyan.copy(alpha = alpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GameRules() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GameRuleLine(Icons.Filled.AutoAwesome, R.string.onb_c5_line1)
        GameRuleLine(Icons.AutoMirrored.Filled.TrendingUp, R.string.onb_c5_line2)
        GameRuleLine(Icons.Filled.TaskAlt, R.string.onb_c5_line3)
        GameRuleLine(Icons.Filled.MilitaryTech, R.string.onb_c5_line4)
    }
}

@Composable
private fun GameRuleLine(icon: ImageVector, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(AetherViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(stringResource(text), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 19.sp)
    }
}

// --- Bölüm sahneleri ---------------------------------------------------------

@Composable
private fun MoonScene(done: Boolean, onDone: () -> Unit) {
    val lit by animateFloatAsState(if (done) 1f else 0.12f, animationSpec = tween(1400, easing = FastOutSlowInEasing), label = "moonLit")
    val pulse by rememberInfiniteTransition(label = "moon").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "moonPulse"
    )
    val cd = stringResource(R.string.onb_cd_moon)
    Box(contentAlignment = Alignment.Center) {
        Burst(trigger = done, color = AstralGold, modifier = Modifier.size(260.dp))
        RankEmblem(
            rank = 8,
            lit = lit,
            sparkles = false,
            size = 190.dp,
            modifier = Modifier
                .scale(if (done) 1f else pulse)
                .clip(CircleShape)
                .clickable(enabled = !done, role = Role.Button, onClickLabel = cd) { onDone() }
                .semantics { contentDescription = cd }
        )
    }
}

@Composable
private fun DreamCardScene(done: Boolean, onDone: () -> Unit) {
    val rotation by animateFloatAsState(if (done) 180f else 0f, animationSpec = tween(750, easing = FastOutSlowInEasing), label = "flip")
    val density = LocalDensity.current.density
    val cd = stringResource(R.string.onb_cd_card)
    Box(
        Modifier
            .fillMaxWidth(0.88f)
            .height(230.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(AetherIndigo.copy(alpha = 0.55f), Void900)))
            .border(1.dp, AstralGold.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .clickable(enabled = !done, role = Role.Button, onClickLabel = cd) { onDone() }
            .semantics { contentDescription = cd }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = AstralGold, modifier = Modifier.size(40.dp))
                Text(
                    stringResource(R.string.onb_c2_dream_sample),
                    color = Color.White,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            // Arka yüz: aynalanmış görünmesin diye geri çevriliyor.
            Column(
                Modifier.graphicsLayer { rotationY = 180f },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SymbolLine("🌊", R.string.onb_c2_sym1)
                SymbolLine("🕊️", R.string.onb_c2_sym2)
                SymbolLine("🗝️", R.string.onb_c2_sym3)
            }
        }
    }
}

@Composable
private fun SymbolLine(emoji: String, @StringRes text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(text), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ManaScene(done: Boolean, onDone: () -> Unit) {
    val density = LocalDensity.current
    val flight = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (done) flight.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val believers by animateIntAsState(if (done) 13 else 12, animationSpec = tween(400, delayMillis = 600), label = "believers")
    val glow by animateColorAsState(if (done) AetherCyan else Color.White.copy(alpha = 0.15f), animationSpec = tween(600, delayMillis = 600), label = "glow")
    val cd = stringResource(R.string.onb_cd_drop)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(AetherViolet.copy(alpha = 0.45f), Void900)))
                .border(1.5.dp, glow, RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Visibility, contentDescription = null, tint = AstralGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.onb_c3_vision_sample), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.onb_c3_believers_format, believers), color = AetherCyan, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(36.dp))
        val lift = with(density) { (flight.value * 150).dp.roundToPx() }
        Box(
            Modifier
                .offset { IntOffset(0, -lift) }
                .graphicsLayer {
                    alpha = 1f - flight.value
                    scaleX = 1f - flight.value * 0.5f
                    scaleY = 1f - flight.value * 0.5f
                }
                .size(76.dp)
                .clip(CircleShape)
                .background(AetherCyan.copy(alpha = 0.18f))
                .border(1.5.dp, AetherCyan, CircleShape)
                .clickable(enabled = !done, role = Role.Button, onClickLabel = cd) { onDone() }
                .semantics { contentDescription = cd },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = AetherCyan, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun HeartScene(done: Boolean, onDone: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            scale.animateTo(1.5f, tween(160))
            scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f))
        }
    }
    val likes by animateIntAsState(if (done) 8 else 7, label = "likes")
    val cd = stringResource(R.string.onb_cd_heart)

    Column(
        Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(20.dp))
            .background(Void900)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(AetherViolet.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text("L", color = AstralGold, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text("Luna", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.onb_c4_post_sample), color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Burst(trigger = done, color = ShadowWorkRose, modifier = Modifier.size(90.dp))
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !done, role = Role.Button, onClickLabel = cd) { onDone() }
                        .semantics { contentDescription = cd },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (done) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (done) ShadowWorkRose else Color.White,
                        modifier = Modifier.size(30.dp).scale(scale.value)
                    )
                }
            }
            Text("$likes", color = Color.White, fontWeight = FontWeight.Bold)
        }
        AnimatedVisibility(visible = done, enter = expandVertically() + fadeIn()) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Void800)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.onb_c4_comment_sample), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ChestScene(done: Boolean, onDone: () -> Unit) {
    val wobble by rememberInfiniteTransition(label = "chest").animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
        label = "chestWobble"
    )
    val cd = stringResource(R.string.onb_cd_chest)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RankEmblem(0, size = 44.dp)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MoonSilver, modifier = Modifier.size(16.dp))
            RankEmblem(4, size = 56.dp)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MoonSilver, modifier = Modifier.size(16.dp))
            RankEmblem(9, size = 72.dp)
        }
        Spacer(Modifier.height(20.dp))
        Box(contentAlignment = Alignment.Center) {
            Burst(trigger = done, color = AstralAmber, modifier = Modifier.size(170.dp))
            Box(
                Modifier
                    .size(104.dp)
                    .rotate(if (done) 0f else wobble)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(AstralAmber.copy(alpha = 0.35f), Void900)))
                    .border(1.5.dp, AstralGold, RoundedCornerShape(24.dp))
                    .clickable(enabled = !done, role = Role.Button, onClickLabel = cd) { onDone() }
                    .semantics { contentDescription = cd },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (done) Icons.Filled.Celebration else Icons.Filled.Redeem,
                    contentDescription = null,
                    tint = AstralGold,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
private fun FinalPanel(
    mode: OnboardingMode,
    currentRank: Int,
    currentXp: Int,
    earnedXp: Int,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            Burst(trigger = true, color = AstralGold, modifier = Modifier.size(240.dp))
            RankEmblem(currentRank, size = 150.dp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.onb_done_title),
            color = AstralGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SerifFontFamily,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        val body = when (mode) {
            OnboardingMode.ACCOUNT -> stringResource(R.string.onb_done_body_format, earnedXp)
            OnboardingMode.ACCOUNT_REPLAY -> stringResource(R.string.onb_done_replay_body)
            OnboardingMode.VISITOR -> stringResource(R.string.onb_done_guest_body_format, earnedXp)
        }
        Text(body, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 23.sp)
        if (mode == OnboardingMode.ACCOUNT && currentXp > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.onb_done_existing_format, stringResource(rankNameRes(currentRank))),
                color = AetherCyan,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
        ) {
            Text(
                stringResource(if (mode == OnboardingMode.VISITOR) R.string.quests_cta_signup else R.string.onb_done_cta),
                color = Void950,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

// --- Küçük parçalar -----------------------------------------------------------

@Composable
private fun ChapterProgress(current: Int, doneMask: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(CHAPTER_COUNT) { i ->
            val done = doneMask and (1 shl i) != 0
            val color = when {
                done -> AstralGold
                i == current -> AstralGold.copy(alpha = 0.45f)
                else -> Color.White.copy(alpha = 0.12f)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun XpPill(xp: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(AstralGold.copy(alpha = 0.15f))
            .border(1.dp, AstralGold.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AstralGold, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.xp_toast_format, xp), color = AstralGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** [trigger] true olunca bir kez dışa doğru saçılan parıltılar. */
@Composable
private fun Burst(trigger: Boolean, color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1100, easing = LinearEasing))
        }
    }
    if (!trigger) return
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val maxR = size.minDimension / 2f
        repeat(16) { i ->
            val angle = Math.toRadians(i * 22.5)
            val dist = maxR * (0.35f + 0.65f * t)
            val p = Offset(center.x + dist * cos(angle).toFloat(), center.y + dist * sin(angle).toFloat())
            drawCircle(color.copy(alpha = (1f - t).coerceIn(0f, 1f)), radius = maxR * 0.035f * (1.2f - t), center = p)
        }
    }
}

@Composable
private fun StarField() {
    val stars = remember {
        val rnd = Random(42)
        List(70) { Triple(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.2f) }
    }
    val t by rememberInfiniteTransition(label = "stars").animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "twinkle"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1024), Void950)))
        stars.forEachIndexed { i, (x, y, s) ->
            val a = 0.2f + 0.6f * abs(sin(t + i * 0.7f))
            drawCircle(Color.White.copy(alpha = a), radius = s * density, center = Offset(x * size.width, y * size.height))
        }
    }
}

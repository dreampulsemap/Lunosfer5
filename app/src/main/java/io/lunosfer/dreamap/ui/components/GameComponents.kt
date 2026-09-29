package io.lunosfer.dreamap.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DailyQuest
import io.lunosfer.dreamap.data.model.GameProgress
import io.lunosfer.dreamap.data.model.PublicProgress
import io.lunosfer.dreamap.data.repository.GameEvent
import io.lunosfer.dreamap.data.repository.GameRepository
import io.lunosfer.dreamap.ui.theme.AetherCyan
import io.lunosfer.dreamap.ui.theme.AetherViolet
import io.lunosfer.dreamap.ui.theme.AstralAmber
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.MoonSilver
import io.lunosfer.dreamap.ui.theme.SemanticSuccess400
import io.lunosfer.dreamap.ui.theme.SemanticSuccess500
import io.lunosfer.dreamap.ui.theme.SerifFontFamily
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.util.GuestPrompt
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Rütbe görünümü: isimler/renkler istemcide, eşikler sunucuda (get_my_progress).
// ---------------------------------------------------------------------------

private val RANK_NAMES = listOf(
    R.string.rank_name_0, R.string.rank_name_1, R.string.rank_name_2, R.string.rank_name_3, R.string.rank_name_4,
    R.string.rank_name_5, R.string.rank_name_6, R.string.rank_name_7, R.string.rank_name_8, R.string.rank_name_9
)

@StringRes
fun rankNameRes(rank: Int): Int = RANK_NAMES[rank.coerceIn(0, RANK_NAMES.lastIndex)]

fun rankColor(rank: Int): Color = when (rank.coerceIn(0, 9)) {
    0 -> Color(0xFF94A3B8)
    1 -> Color(0xFF7DD3FC)
    2 -> Color(0xFF38BDF8)
    3 -> Color(0xFF818CF8)
    4 -> Color(0xFFA855F7)
    5 -> Color(0xFFE879F9)
    6 -> Color(0xFFF472B6)
    7 -> Color(0xFFF59E0B)
    8 -> Color(0xFFE6C687)
    else -> Color(0xFFFFE9A8)
}

/**
 * Rütbe amblemi: rütbe yükseldikçe ay evresi dolar (0 = ince hilal,
 * 9 = parlayan dolunay). [lit] verilirse evre ondan alınır (tanıtım turu).
 */
@Composable
fun RankEmblem(rank: Int, modifier: Modifier = Modifier, size: Dp = 48.dp, lit: Float? = null, sparkles: Boolean = true) {
    val color = rankColor(rank)
    val litFraction = (lit ?: (0.12f + 0.88f * (rank.coerceIn(0, 9) / 9f))).coerceIn(0f, 1f)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), center = c, radius = r),
            radius = r,
            center = c
        )
        drawCircle(color = color.copy(alpha = 0.9f), radius = r * 0.9f, center = c, style = Stroke(width = r * 0.07f))

        val moonR = r * 0.6f
        clipPath(Path().apply { addOval(Rect(c, moonR)) }) {
            drawCircle(Void800, radius = moonR, center = c)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, color),
                    center = Offset(c.x - moonR * 0.35f, c.y - moonR * 0.35f),
                    radius = moonR * 1.7f
                ),
                radius = moonR,
                center = c
            )
            // Gölge sola kaydıkça aydınlık kısım büyür; litFraction = 1 -> tamamen dışarıda
            // (2.1 > 1 + 1.02: dolunayda sol kenarda ince koyu şerit kalmasın).
            drawCircle(Void900, radius = moonR * 1.02f, center = Offset(c.x - moonR * 2.1f * litFraction, c.y))
        }

        if (sparkles && rank >= 7) {
            repeat(rank - 5) { i ->
                val angle = Math.toRadians(-60.0 + i * 40.0)
                val p = Offset(c.x + r * 0.9f * cos(angle).toFloat(), c.y + r * 0.9f * sin(angle).toFloat())
                drawCircle(Color.White, radius = r * 0.06f, center = p)
            }
        }
    }
}

@Composable
fun XpBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), animationSpec = tween(900), label = "xpBar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)))
            )
        }
    }
}

@Composable
fun NextRankText(p: GameProgress, modifier: Modifier = Modifier) {
    val next = p.nextRankXp
    Text(
        text = if (next == null) {
            stringResource(R.string.journey_max_rank)
        } else {
            stringResource(R.string.journey_to_next_format, (next - p.xp).coerceAtLeast(0), stringResource(rankNameRes(p.rank + 1)))
        },
        color = MoonSilver.copy(alpha = 0.85f),
        fontSize = 11.sp,
        modifier = modifier
    )
}

/** Üst bardaki profil düğmesi: etrafında rütbe renginde XP halkası + seviye rozeti. */
@Composable
fun RankRingAvatar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progress by GameRepository.progress.collectAsState()
    val p = progress?.takeIf { !it.isGuest }
    val rank = p?.rank ?: 0
    val color = rankColor(rank)
    val frac by animateFloatAsState(p?.rankProgress ?: 0f, animationSpec = tween(900), label = "ring")
    val cd = if (p != null) {
        stringResource(R.string.cd_rank_avatar_format, stringResource(rankNameRes(rank)))
    } else {
        stringResource(R.string.cd_profile)
    }

    Box(
        modifier
            .size(36.dp)
            .clickable(onClickLabel = cd, onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.5.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(color.copy(alpha = 0.22f), 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            if (p != null) {
                drawArc(color, -90f, 360f * frac, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Void800),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
        }
        if (p != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Void950, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${rank + 1}", color = Void950, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Rozetler, XP nedenleri, görevler
// ---------------------------------------------------------------------------

data class BadgeMeta(@StringRes val name: Int, @StringRes val desc: Int, val icon: ImageVector)

/** Bilinmeyen (sunucuya sonradan eklenmiş) rozet kodu -> null, sessizce gizlenir. */
fun badgeMeta(code: String): BadgeMeta? = when (code) {
    "first_dream" -> BadgeMeta(R.string.badge_first_dream, R.string.badge_first_dream_desc, Icons.Filled.Bedtime)
    "streak_3" -> BadgeMeta(R.string.badge_streak_3, R.string.badge_streak_3_desc, Icons.Filled.LocalFireDepartment)
    "first_vision" -> BadgeMeta(R.string.badge_first_vision, R.string.badge_first_vision_desc, Icons.Filled.Visibility)
    "first_diary" -> BadgeMeta(R.string.badge_first_diary, R.string.badge_first_diary_desc, Icons.Filled.AutoStories)
    "dreams_10" -> BadgeMeta(R.string.badge_dreams_10, R.string.badge_dreams_10_desc, Icons.Filled.EditNote)
    "streak_7" -> BadgeMeta(R.string.badge_streak_7, R.string.badge_streak_7_desc, Icons.Filled.Whatshot)
    "mana_giver_10" -> BadgeMeta(R.string.badge_mana_giver_10, R.string.badge_mana_giver_10_desc, Icons.Filled.WaterDrop)
    "friends_5" -> BadgeMeta(R.string.badge_friends_5, R.string.badge_friends_5_desc, Icons.Filled.Groups)
    "comments_25" -> BadgeMeta(R.string.badge_comments_25, R.string.badge_comments_25_desc, Icons.Filled.Forum)
    "quest_master" -> BadgeMeta(R.string.badge_quest_master, R.string.badge_quest_master_desc, Icons.Filled.TaskAlt)
    "ambassador" -> BadgeMeta(R.string.badge_ambassador, R.string.badge_ambassador_desc, Icons.Filled.Campaign)
    "likes_received_50" -> BadgeMeta(R.string.badge_likes_received_50, R.string.badge_likes_received_50_desc, Icons.Filled.Favorite)
    "dreams_50" -> BadgeMeta(R.string.badge_dreams_50, R.string.badge_dreams_50_desc, Icons.Filled.Inventory2)
    "streak_30" -> BadgeMeta(R.string.badge_streak_30, R.string.badge_streak_30_desc, Icons.Filled.Brightness3)
    else -> null
}

@Composable
fun BadgeIcon(code: String, earned: Boolean, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val meta = badgeMeta(code) ?: return
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (earned) Brush.linearGradient(listOf(AstralGold.copy(alpha = 0.35f), AetherViolet.copy(alpha = 0.35f)))
                else SolidColor(Color.White.copy(alpha = 0.05f))
            )
            .border(1.dp, if (earned) AstralGold else Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            meta.icon,
            contentDescription = null,
            tint = if (earned) AstralGold else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(size * 0.5f)
        )
        if (!earned) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
            )
        }
    }
}

@StringRes
fun xpReasonRes(reason: String?): Int = when (reason) {
    "dream_posted" -> R.string.xp_reason_dream_posted
    "diary_posted" -> R.string.xp_reason_diary_posted
    "vision_created" -> R.string.xp_reason_vision_created
    "comment_posted" -> R.string.xp_reason_comment_posted
    "comment_received" -> R.string.xp_reason_comment_received
    "like_given" -> R.string.xp_reason_like_given
    "like_received" -> R.string.xp_reason_like_received
    "mana_sent" -> R.string.xp_reason_mana_sent
    "mana_given" -> R.string.xp_reason_mana_given
    "friend_made" -> R.string.xp_reason_friend_made
    "referral_inviter" -> R.string.xp_reason_referral_inviter
    "referral_joined" -> R.string.xp_reason_referral_joined
    "compass_checkin" -> R.string.xp_reason_compass_checkin
    "seed_completed" -> R.string.xp_reason_seed_completed
    "onboarding_completed" -> R.string.xp_reason_onboarding_completed
    "profile_completed" -> R.string.xp_reason_profile_completed
    "daily_quests_bonus" -> R.string.xp_reason_daily_quests_bonus
    "badge_earned" -> R.string.xp_reason_badge_earned
    "content_shared" -> R.string.xp_reason_content_shared
    else -> R.string.xp_reason_other
}

fun xpReasonIcon(reason: String?): ImageVector = when (reason) {
    "dream_posted" -> Icons.Filled.Bedtime
    "diary_posted" -> Icons.Filled.AutoStories
    "vision_created" -> Icons.Filled.Visibility
    "comment_posted", "comment_received" -> Icons.Filled.ChatBubble
    "like_given", "like_received" -> Icons.Filled.Favorite
    "mana_sent", "mana_given" -> Icons.Filled.WaterDrop
    "friend_made" -> Icons.Filled.PersonAdd
    "referral_inviter", "referral_joined" -> Icons.Filled.Campaign
    "compass_checkin" -> Icons.Filled.Explore
    "seed_completed" -> Icons.Filled.Spa
    "onboarding_completed" -> Icons.Filled.Flag
    "profile_completed" -> Icons.Filled.AccountCircle
    "daily_quests_bonus" -> Icons.Filled.Redeem
    "badge_earned" -> Icons.Filled.MilitaryTech
    "content_shared" -> Icons.Filled.Share
    else -> Icons.Filled.AutoAwesome
}

@StringRes
private fun questLabelRes(code: String): Int? = when (code) {
    "post_dream" -> R.string.quest_post_dream
    "post_diary" -> R.string.quest_post_diary
    "comment_2" -> R.string.quest_comment_2
    "like_3" -> R.string.quest_like_3
    "mana_1" -> R.string.quest_mana_1
    "compass" -> R.string.quest_compass
    else -> null
}

private fun questIcon(code: String): ImageVector = when (code) {
    "post_dream" -> Icons.Filled.Bedtime
    "post_diary" -> Icons.Filled.AutoStories
    "comment_2" -> Icons.Filled.ChatBubble
    "like_3" -> Icons.Filled.Favorite
    "mana_1" -> Icons.Filled.WaterDrop
    "compass" -> Icons.Filled.Explore
    else -> Icons.Filled.TaskAlt
}

private fun msUntilUtcMidnight(): Long = 86_400_000L - (System.currentTimeMillis() % 86_400_000L)

@Composable
private fun QuestResetCountdown() {
    val msLeft by produceState(msUntilUtcMidnight()) {
        while (true) {
            delay(30_000)
            value = msUntilUtcMidnight()
        }
    }
    val hours = (msLeft / 3_600_000L).toInt()
    val minutes = ((msLeft / 60_000L) % 60L).toInt()
    Text(stringResource(R.string.quests_resets_format, hours, minutes), color = MoonSilver, fontSize = 11.sp)
}

@Composable
private fun QuestRow(q: DailyQuest) {
    val label = questLabelRes(q.code) ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (q.done) SemanticSuccess500.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (q.done) Icons.Filled.Check else questIcon(q.code),
                contentDescription = null,
                tint = if (q.done) SemanticSuccess400 else AstralGold,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(label),
            color = if (q.done) MoonSilver else Color.White,
            fontSize = 13.sp,
            textDecoration = if (q.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        Text("${q.progress.coerceAtMost(q.target)}/${q.target}", color = MoonSilver, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        // xp eylem basinadir (xp_rules); gorevin toplam odulu = xp * target.
        Text(stringResource(R.string.xp_toast_format, q.xp * q.target), color = AstralGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Günün 3 görevi + sandık. [onClick] null ise kart tıklanmaz (Yolculuğum
 * ekranının kendisi). İlerleme yüklenene kadar hiç çizilmez (titreme yok).
 */
@Composable
fun DailyQuestsCard(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val progress by GameRepository.progress.collectAsState()
    val p = progress ?: return
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(AetherViolet.copy(alpha = 0.16f), Void900)))
            .border(1.dp, AetherViolet.copy(alpha = 0.35f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.quests_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (!p.isGuest) QuestResetCountdown()
        }

        if (p.isGuest) {
            Text(stringResource(R.string.quests_guest), color = MoonSilver, fontSize = 13.sp)
            TextButton(onClick = { GuestPrompt.show() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(stringResource(R.string.quests_cta_signup), color = AstralGold, fontWeight = FontWeight.Bold)
            }
        } else {
            p.quests.forEach { QuestRow(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Redeem,
                    contentDescription = null,
                    tint = if (p.questsBonusClaimed) AstralGold else MoonSilver,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (p.questsBonusClaimed) stringResource(R.string.quests_chest_claimed)
                    else stringResource(R.string.quests_chest_format, p.questsBonusXp),
                    color = if (p.questsBonusClaimed) AstralGold else MoonSilver,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Profil ekranındaki özet: amblem + rütbe + XP çubuğu + kazanılan rozetler. */
@Composable
fun JourneySummaryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val progress by GameRepository.progress.collectAsState()
    val p = progress?.takeIf { !it.isGuest } ?: return
    val color = rankColor(p.rank)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Void900)
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankEmblem(p.rank, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(rankNameRes(p.rank)),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = SerifFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(stringResource(R.string.journey_level_format, p.rank + 1, p.xp), color = MoonSilver, fontSize = 12.sp)
                XpBar(p.rankProgress, color)
                NextRankText(p)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MoonSilver)
        }
        val earned = p.badges.filter { it.earnedAt != null && badgeMeta(it.code) != null }
        if (earned.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                earned.take(6).forEach { BadgeIcon(it.code, earned = true, size = 30.dp) }
                if (earned.size > 6) Text("+${earned.size - 6}", color = MoonSilver, fontSize = 12.sp)
            }
        }
    }
}

/** Başkasının profilinde rütbe hapı + rozetler; görünürlük izin vermezse hiçbir şey çizmez. */
@Composable
fun PublicRankRow(userId: String) {
    val loaded by produceState<PublicProgress?>(null, userId) {
        value = runCatching { GameRepository.publicProgress(userId) }.getOrNull()
    }
    val p = loaded ?: return
    val color = rankColor(p.rank)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.12f))
                .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(start = 4.dp, end = 12.dp, top = 3.dp, bottom = 3.dp)
        ) {
            RankEmblem(p.rank, size = 24.dp)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(rankNameRes(p.rank)), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(" · ${p.xp} XP", color = MoonSilver, fontSize = 12.sp)
        }
        val badges = p.badges.filter { badgeMeta(it) != null }
        if (badges.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                badges.takeLast(8).forEach { BadgeIcon(it, earned = true, size = 26.dp) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kutlamalar: +XP bildirimi, rozet ve rütbe atlama pencereleri.
// ---------------------------------------------------------------------------

/** MainScreen'de bir kez çizilir; GameRepository olay kuyruğunu sırayla gösterir. */
@Composable
fun GameEventHost() {
    val events by GameRepository.events.collectAsState()
    val head = events.firstOrNull() ?: return
    key(head.id) {
        when (head) {
            is GameEvent.XpGain -> XpToast(head) { GameRepository.consume(head) }
            is GameEvent.BadgeEarned -> BadgeEarnedDialog(head.code) { GameRepository.consume(head) }
            is GameEvent.RankUp -> RankUpDialog(head.rank) { GameRepository.consume(head) }
        }
    }
}

@Composable
private fun XpToast(event: GameEvent.XpGain, onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(2_300)
        visible = false
        delay(400)
        onDone()
    }
    val offsetY = with(LocalDensity.current) { 76.dp.roundToPx() }
    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, offsetY), properties = PopupProperties(focusable = false)) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(AstralAmber, AstralGold)))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Void950, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.xp_toast_format, event.amount), color = Void950, fontWeight = FontWeight.Bold)
                event.reason?.let {
                    Text(" · " + stringResource(xpReasonRes(it)), color = Void950.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CelebrationRays(color: Color, modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "rays").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "raysRotation"
    )
    Canvas(modifier.graphicsLayer { rotationZ = rotation }) {
        val c = center
        val r = size.minDimension / 2f
        repeat(12) { i ->
            val a = Math.toRadians(i * 30.0)
            val path = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + r * cos(a - 0.09).toFloat(), c.y + r * sin(a - 0.09).toFloat())
                lineTo(c.x + r * cos(a + 0.09).toFloat(), c.y + r * sin(a + 0.09).toFloat())
                close()
            }
            drawPath(path, color.copy(alpha = 0.18f))
        }
    }
}

@Composable
private fun RankUpDialog(rank: Int, onDismiss: () -> Unit) {
    val color = rankColor(rank)
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Void900)
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(R.string.rank_up_title),
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily,
                textAlign = TextAlign.Center
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                CelebrationRays(color, Modifier.fillMaxSize())
                RankEmblem(rank, size = 120.dp, modifier = Modifier.scale(scale.value))
            }
            Text(
                stringResource(R.string.rank_up_body_format, stringResource(rankNameRes(rank))),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = color)) {
                Text(stringResource(R.string.rank_up_cta), color = Void950, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BadgeEarnedDialog(code: String, onDismiss: () -> Unit) {
    val meta = badgeMeta(code)
    if (meta == null) {
        LaunchedEffect(code) { onDismiss() }
        return
    }
    val xp = GameRepository.progress.collectAsState().value?.badges?.firstOrNull { it.code == code }?.xp
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 320f)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Void900)
                .border(1.dp, AstralGold.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.badge_earned_title),
                color = AstralGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                CelebrationRays(AstralGold, Modifier.fillMaxSize())
                BadgeIcon(code, earned = true, size = 88.dp, modifier = Modifier.scale(scale.value))
            }
            Text(stringResource(meta.name), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(meta.desc), color = MoonSilver, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (xp != null) {
                Text(stringResource(R.string.xp_toast_format, xp), color = AetherCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = AstralGold)) {
                Text(stringResource(R.string.badge_earned_cta), color = Void950, fontWeight = FontWeight.Bold)
            }
        }
    }
}

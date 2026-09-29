package io.lunosfer.dreamap.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.BadgeState
import io.lunosfer.dreamap.data.model.GameProgress
import io.lunosfer.dreamap.data.model.LeaderboardEntry
import io.lunosfer.dreamap.data.model.XpEvent
import io.lunosfer.dreamap.data.model.XpRule
import io.lunosfer.dreamap.data.repository.GameRepository
import io.lunosfer.dreamap.ui.components.BadgeIcon
import io.lunosfer.dreamap.ui.components.DailyQuestsCard
import io.lunosfer.dreamap.ui.components.NextRankText
import io.lunosfer.dreamap.ui.components.RankEmblem
import io.lunosfer.dreamap.ui.components.XpBar
import io.lunosfer.dreamap.ui.components.badgeMeta
import io.lunosfer.dreamap.ui.components.rankColor
import io.lunosfer.dreamap.ui.components.rankNameRes
import io.lunosfer.dreamap.ui.components.xpReasonIcon
import io.lunosfer.dreamap.ui.components.xpReasonRes
import io.lunosfer.dreamap.ui.theme.AetherViolet
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.MoonSilver
import io.lunosfer.dreamap.ui.theme.SerifFontFamily
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.ui.viewmodel.JourneyViewModel
import io.lunosfer.dreamap.ui.viewmodel.UiState
import io.lunosfer.dreamap.util.GuestPrompt
import io.lunosfer.dreamap.util.OnboardingController
import io.lunosfer.dreamap.util.RelativeTime

/** "Yolculuğum": rütbe, günlük görevler, rozetler, sıralama, XP kuralları. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: JourneyViewModel = viewModel()
) {
    val progress by GameRepository.progress.collectAsState()
    val board by viewModel.board.collectAsState()
    val period by viewModel.period.collectAsState()
    var selectedBadge by remember { mutableStateOf<BadgeState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.journey_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { padding ->
        val p = progress
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AstralGold)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { if (p.isGuest) GuestJourneyCard() else RankHero(p) }
            item { DailyQuestsCard(onClick = null) }

            if (!p.isGuest) {
                val known = p.badges.filter { badgeMeta(it.code) != null }
                item {
                    SectionTitle(
                        R.string.journey_badges_title,
                        trailing = stringResource(R.string.journey_badges_count_format, known.count { it.earnedAt != null }, known.size)
                    )
                }
                items(known.chunked(4)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { badge -> BadgeTile(badge, Modifier.weight(1f)) { selectedBadge = badge } }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            item { SectionTitle(R.string.journey_lb_title) }
            item {
                PeriodToggle(period = period, onSelect = viewModel::setPeriod)
            }
            when (val b = board) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(28.dp))
                    }
                }
                is UiState.Error -> item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.journey_lb_error), color = MoonSilver, fontSize = 13.sp)
                        TextButton(onClick = viewModel::loadBoard) { Text(stringResource(R.string.retry), color = AstralGold) }
                    }
                }
                is UiState.Success -> {
                    if (b.data.isEmpty()) {
                        item { Text(stringResource(R.string.journey_lb_empty), color = MoonSilver, fontSize = 13.sp) }
                    } else {
                        items(b.data, key = { "lb_${it.userId}" }) { entry ->
                            LeaderboardRow(entry) { onUserClick(entry.userId) }
                        }
                    }
                }
            }

            if (p.rules.isNotEmpty()) {
                item { SectionTitle(R.string.journey_how_title) }
                items(p.rules.sortedByDescending { it.xp }, key = { "rule_${it.reason}" }) { RuleRow(it) }
            }

            if (p.thresholds.isNotEmpty()) {
                item { SectionTitle(R.string.journey_ranks_title) }
                items(p.thresholds.indices.toList(), key = { "rank_$it" }) { i ->
                    RankLadderRow(rank = i, threshold = p.thresholds[i], currentRank = if (p.isGuest) -1 else p.rank)
                }
            }

            if (!p.isGuest) {
                item { SectionTitle(R.string.journey_recent_title) }
                if (p.recent.isEmpty()) {
                    item { Text(stringResource(R.string.journey_recent_empty), color = MoonSilver, fontSize = 13.sp) }
                } else {
                    items(p.recent) { RecentRow(it) }
                }
            }

            item { ReplayTourButton(p) }
        }
    }

    selectedBadge?.let { badge ->
        val meta = badgeMeta(badge.code)
        if (meta != null) {
            AlertDialog(
                onDismissRequest = { selectedBadge = null },
                containerColor = Void900,
                icon = { BadgeIcon(badge.code, earned = badge.earnedAt != null, size = 64.dp) },
                title = { Text(stringResource(meta.name), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(meta.desc), color = MoonSilver, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(if (badge.earnedAt != null) R.string.journey_badge_earned else R.string.journey_badge_locked) +
                                " · " + stringResource(R.string.xp_toast_format, badge.xp),
                            color = if (badge.earnedAt != null) AstralGold else MoonSilver,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedBadge = null }) { Text(stringResource(R.string.badge_earned_cta), color = AstralGold) }
                }
            )
        }
    }
}

@Composable
private fun RankHero(p: GameProgress) {
    val color = rankColor(p.rank)
    val pulse by rememberInfiniteTransition(label = "hero").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "heroPulse"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Void900)
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RankEmblem(p.rank, size = 128.dp, modifier = Modifier.scale(pulse))
        Text(
            stringResource(rankNameRes(p.rank)),
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SerifFontFamily,
            textAlign = TextAlign.Center
        )
        Text(stringResource(R.string.journey_level_format, p.rank + 1, p.xp), color = Color.White, fontSize = 14.sp)
        XpBar(p.rankProgress, color, height = 10.dp)
        NextRankText(p)
        Text(
            stringResource(R.string.journey_today_week_format, p.todayXp, p.weeklyXp),
            color = AstralGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GuestJourneyCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Void900)
            .border(1.dp, AstralGold.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RankEmblem(0, size = 96.dp)
        Text(
            stringResource(R.string.journey_guest_title),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SerifFontFamily,
            textAlign = TextAlign.Center
        )
        Text(stringResource(R.string.journey_guest_body), color = MoonSilver, fontSize = 13.sp, textAlign = TextAlign.Center)
        Button(onClick = { GuestPrompt.show() }, colors = ButtonDefaults.buttonColors(containerColor = AstralGold)) {
            Text(stringResource(R.string.quests_cta_signup), color = Void950, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(title),
            color = AstralGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) Text(trailing, color = MoonSilver, fontSize = 12.sp)
    }
}

@Composable
private fun BadgeTile(badge: BadgeState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val meta = badgeMeta(badge.code) ?: return
    val earned = badge.earnedAt != null
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BadgeIcon(badge.code, earned = earned, size = 48.dp)
        Text(
            stringResource(meta.name),
            color = if (earned) Color.White else MoonSilver.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PeriodToggle(period: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Void900)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            JourneyViewModel.PERIOD_WEEK to R.string.journey_lb_week,
            JourneyViewModel.PERIOD_ALL to R.string.journey_lb_all
        ).forEach { (value, label) ->
            val selected = period == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) AetherViolet.copy(alpha = 0.35f) else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(label),
                    color = if (selected) Color.White else MoonSilver,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, onClick: () -> Unit) {
    val me = entry.isMe == true
    val medal = when (entry.pos) {
        1 -> Color(0xFFFFD166)
        2 -> Color(0xFFCBD5E1)
        3 -> Color(0xFFD9A066)
        else -> null
    }
    val name = entry.displayName?.takeIf { it.isNotBlank() } ?: entry.username ?: "—"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (me) AetherViolet.copy(alpha = 0.18f) else Void900)
            .border(1.dp, if (me) AetherViolet.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${entry.pos}",
            color = medal ?: MoonSilver,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.width(30.dp)
        )
        if (!entry.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = entry.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape)
            )
        } else {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Void800),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), color = AstralGold, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (me) "$name · ${stringResource(R.string.journey_lb_you)}" else name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(stringResource(rankNameRes(entry.rank)), color = rankColor(entry.rank), fontSize = 11.sp, maxLines = 1)
        }
        RankEmblem(entry.rank, size = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text("${entry.xp} XP", color = AstralGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun RuleRow(rule: XpRule) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Void900), contentAlignment = Alignment.Center) {
            Icon(xpReasonIcon(rule.reason), contentDescription = null, tint = AstralGold, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(xpReasonRes(rule.reason)), color = Color.White, fontSize = 13.sp)
            val sub = when {
                rule.reason == "mana_given" -> stringResource(R.string.journey_rule_per_mana)
                rule.dailyCap != null -> stringResource(R.string.journey_rule_cap_format, rule.dailyCap)
                else -> null
            }
            if (sub != null) Text(sub, color = MoonSilver, fontSize = 11.sp)
        }
        Text(stringResource(R.string.xp_toast_format, rule.xp), color = AstralGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun RankLadderRow(rank: Int, threshold: Int, currentRank: Int) {
    val reached = currentRank >= rank
    val current = currentRank == rank
    val color = rankColor(rank)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (current) color.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (current) color.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankEmblem(rank, size = 36.dp, modifier = if (reached) Modifier else Modifier.scale(0.9f))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(rankNameRes(rank)),
                color = if (reached) color else MoonSilver.copy(alpha = 0.6f),
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            Text(stringResource(R.string.journey_rank_threshold_format, threshold), color = MoonSilver, fontSize = 11.sp)
        }
        if (current) {
            Text(stringResource(R.string.journey_rank_current), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentRow(event: XpEvent) {
    val badgeName = event.badge?.let { badgeMeta(it) }?.name
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(xpReasonIcon(event.reason), contentDescription = null, tint = MoonSilver, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (badgeName != null) stringResource(badgeName) else stringResource(xpReasonRes(event.reason)),
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(RelativeTime.format(event.createdAt), color = MoonSilver, fontSize = 11.sp)
        }
        Text(
            (if (event.delta >= 0) "+" else "") + "${event.delta} XP",
            color = if (event.delta >= 0) AstralGold else MoonSilver,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ReplayTourButton(p: GameProgress) {
    val reward = p.rules.firstOrNull { it.reason == "onboarding_completed" }?.xp
    val label = if (!p.isGuest && p.onboardingStatus != "completed" && reward != null) {
        stringResource(R.string.journey_replay_tour_bonus, reward)
    } else {
        stringResource(R.string.journey_replay_tour)
    }
    OutlinedButton(
        onClick = { OnboardingController.show() },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
        border = androidx.compose.foundation.BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
    ) {
        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

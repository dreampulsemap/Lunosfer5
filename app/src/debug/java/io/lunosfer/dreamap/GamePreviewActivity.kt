package io.lunosfer.dreamap

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.lunosfer.dreamap.data.model.BadgeState
import io.lunosfer.dreamap.data.model.DailyQuest
import io.lunosfer.dreamap.data.model.GameProgress
import io.lunosfer.dreamap.data.model.XpEvent
import io.lunosfer.dreamap.data.model.XpRule
import io.lunosfer.dreamap.data.repository.GameEvent
import io.lunosfer.dreamap.data.repository.GameRepository
import io.lunosfer.dreamap.ui.components.DailyQuestsCard
import io.lunosfer.dreamap.ui.components.GameEventHost
import io.lunosfer.dreamap.ui.components.JourneySummaryCard
import io.lunosfer.dreamap.ui.components.RankEmblem
import io.lunosfer.dreamap.ui.components.RankRingAvatar
import io.lunosfer.dreamap.ui.screens.JourneyScreen
import io.lunosfer.dreamap.ui.theme.MyApplicationTheme
import io.lunosfer.dreamap.ui.theme.Void950
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * GECICI QA araci (yalnizca debug): giris gerektiren oyunlastirma ekranlarini
 * sahte ilerleme verisiyle gosterir.
 *   adb shell am start -n io.lunosfer.dreamap/.GamePreviewActivity --es screen journey|cards|rankup|badge|guest
 */
class GamePreviewActivity : AppCompatActivity() {

    @Suppress("UNCHECKED_CAST")
    private fun <T> field(name: String): MutableStateFlow<T> {
        val f = GameRepository::class.java.getDeclaredField(name)
        f.isAccessible = true
        return f.get(null) as MutableStateFlow<T>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "journey"
        val guest = screen == "guest"

        field<GameProgress?>("_progress").value = GameProgress(
            xp = 420, rank = 2, rankMinXp = 300, nextRankXp = 700,
            thresholds = listOf(0, 100, 300, 700, 1500, 3000, 5500, 9000, 14000, 21000),
            onboardingStatus = "skipped", isGuest = guest, todayXp = 35, weeklyXp = 160,
            quests = listOf(
                DailyQuest("post_dream", 1, 20, 1),
                DailyQuest("like_3", 3, 1, 2),
                DailyQuest("compass", 1, 5, 0)
            ),
            questsBonusXp = 30, questsBonusClaimed = false,
            badges = listOf(
                BadgeState("first_dream", 25, "2026-09-20T10:00:00Z"), BadgeState("streak_3", 20, "2026-09-22T10:00:00Z"),
                BadgeState("first_vision", 25, null), BadgeState("first_diary", 15, "2026-09-23T10:00:00Z"),
                BadgeState("dreams_10", 50, null), BadgeState("streak_7", 70, null), BadgeState("mana_giver_10", 40, null),
                BadgeState("friends_5", 40, null), BadgeState("comments_25", 40, null), BadgeState("quest_master", 50, null),
                BadgeState("ambassador", 50, null), BadgeState("likes_received_50", 60, null), BadgeState("dreams_50", 150, null),
                BadgeState("streak_30", 300, null)
            ),
            rules = listOf(
                XpRule("dream_posted", 20, 3), XpRule("comment_posted", 5, 10), XpRule("mana_given", 1, null),
                XpRule("onboarding_completed", 50, null), XpRule("daily_quests_bonus", 30, 1)
            ),
            recent = listOf(
                XpEvent("dream_posted", 20, "2026-09-29T09:00:00Z"),
                XpEvent("badge_earned", 25, "2026-09-29T09:00:00Z", badge = "first_dream"),
                XpEvent("like_received", 2, "2026-09-28T19:00:00Z")
            )
        )
        when (screen) {
            "rankup" -> field<List<GameEvent>>("_events").value = listOf(GameEvent.RankUp(3))
            "badge" -> field<List<GameEvent>>("_events").value = listOf(GameEvent.BadgeEarned("first_dream"))
            "xp" -> field<List<GameEvent>>("_events").value = listOf(GameEvent.XpGain(5, "comment_posted"))
        }

        setContent {
            MyApplicationTheme {
                when (screen) {
                    "journey", "guest" -> JourneyScreen(onBack = { finish() }, onUserClick = {})
                    else -> Column(
                        Modifier
                            .fillMaxSize()
                            .background(Void950)
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .widthIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RankRingAvatar(onClick = {})
                            (0..9).forEach { RankEmblem(it, size = 40.dp) }
                        }
                        DailyQuestsCard(onClick = {})
                        JourneySummaryCard(onClick = {})
                        GameEventHost()
                    }
                }
            }
        }
    }
}

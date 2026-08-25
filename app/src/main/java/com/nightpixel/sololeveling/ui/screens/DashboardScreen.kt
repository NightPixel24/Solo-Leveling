package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.Boss
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.gamification.MAX_STAT_LEVEL
import com.nightpixel.sololeveling.data.gamification.QuestItem
import com.nightpixel.sololeveling.data.gamification.computeDailyQuests
import com.nightpixel.sololeveling.data.gamification.computeRank
import com.nightpixel.sololeveling.data.gamification.computeWeeklyQuests
import com.nightpixel.sololeveling.data.gamification.xpForLevel
import com.nightpixel.sololeveling.ui.components.RadarChart
import com.nightpixel.sololeveling.ui.components.RankBadge
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemVioletBright
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import java.time.DayOfWeek
import java.time.LocalDate

/** The Status Window (spec Section 6) - the Section 5.1 radar chart, the Section 5.3 Rank badge,
 * per-stat level/XP rows, and (as of Phase 12) the spec Section 5.4 Today's/Weekly Quests and
 * Section 5.5 Boss Fights, all computed live rather than persisted (see `data/gamification/
 * Quests.kt` and `Boss.kt`'s doc comments for why). Mood heatmap preview, quick-add buttons, Life
 * Goals summary, and the Analytics tab are still Phase 15 ("Dashboard/Analytics screen tying
 * everything together"). Settings, including Export/Import, is reached from here rather than the
 * bottom nav (spec Section 8). Life Goals is reached by tapping the Rank badge (spec Section 8).
 * The gavel icon opens the spec Section 5.6 Punishment Pool, which - like Goals/Settings - has no
 * assigned bottom-nav slot either. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onSettingsClick: () -> Unit, onGoalsClick: () -> Unit, onPunishmentsClick: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val statDao = remember { app.database.statDao() }
    val goalDao = remember { app.database.goalDao() }
    val habitDao = remember { app.database.habitDao() }
    val gymDao = remember { app.database.gymDao() }
    val waterDao = remember { app.database.waterDao() }
    val moodDao = remember { app.database.moodDao() }
    val taskDao = remember { app.database.taskDao() }
    val bossDao = remember { app.database.bossDao() }

    val stats by statDao.observeStats().collectAsState(initial = emptyList())
    val byTag = remember(stats) { stats.associateBy { it.tag } }
    val orderedStats = remember(byTag) { StatTag.entries.map { byTag[it] ?: Stat(tag = it) } }

    val goals by goalDao.observeAll().collectAsState(initial = emptyList())
    val rank = remember(goals) { computeRank(goals) }

    val today = remember { LocalDate.now() }
    val todayStr = remember(today) { today.toString() }
    val weekDays = remember(today) {
        val monday = today.with(DayOfWeek.MONDAY)
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    val habitsWithLogs by habitDao.observeHabitsWithLogs().collectAsState(initial = emptyList())
    val exercisesWithSessions by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val todayWaterLog by waterDao.observeLog(todayStr).collectAsState(initial = null)
    val allWaterLogs by waterDao.observeAllLogs().collectAsState(initial = emptyList())
    val moodEntries by moodDao.observeEntries().collectAsState(initial = emptyList())
    val allTasks by taskDao.observeAllTasks().collectAsState(initial = emptyList())
    val bosses by bossDao.observeBosses().collectAsState(initial = emptyList())

    val dailyQuests = remember(habitsWithLogs, exercisesWithSessions, todayWaterLog, moodEntries, allTasks, today) {
        computeDailyQuests(
            today = today,
            habitsWithLogs = habitsWithLogs,
            exercisesWithSessions = exercisesWithSessions,
            waterLog = todayWaterLog,
            moodLoggedToday = moodEntries.any { it.date == todayStr },
            allTasks = allTasks
        )
    }
    val weeklyQuests = remember(habitsWithLogs, exercisesWithSessions, allWaterLogs, today, weekDays) {
        computeWeeklyQuests(
            today = today,
            weekDays = weekDays,
            habitsWithLogs = habitsWithLogs,
            exercisesWithSessions = exercisesWithSessions,
            waterLogsByDate = allWaterLogs.associateBy { it.date }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onPunishmentsClick) {
                        Icon(Icons.Filled.Gavel, contentDescription = "Punishment Pool")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RankBadge(rank = rank, onClick = onGoalsClick)
                    Column {
                        Text("Rank", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your life trajectory - tap to view Life Goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                RadarChart(
                    values = orderedStats.map { it.tag.name to it.level / MAX_STAT_LEVEL.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(orderedStats, key = { it.tag }) { stat ->
                StatRow(stat)
            }
            if (dailyQuests.isNotEmpty()) {
                item { SectionHeader("Today's Quests") }
                item { QuestList(dailyQuests) }
            }
            item { SectionHeader("This Week's Quests") }
            item { WeeklyQuestCard(weeklyQuests.items, weeklyQuests.goodWeek) }
            val activeBosses = bosses.filter { !it.defeated }
            if (activeBosses.isNotEmpty()) {
                item { SectionHeader("Active Boss Fights") }
                items(activeBosses, key = { it.id }) { boss ->
                    val bestWeight = exercisesWithSessions
                        .find { it.exercise.id == boss.exerciseId }
                        ?.sessions?.mapNotNull { it.actualWeight }?.maxOrNull() ?: 0.0
                    DashboardBossRow(boss, bestWeight)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun QuestList(quests: List<QuestItem>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quests.forEach { quest -> QuestRow(quest) }
        }
    }
}

@Composable
private fun QuestRow(quest: QuestItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (quest.done) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (quest.done) SystemGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            quest.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (quest.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WeeklyQuestCard(items: List<QuestItem>, goodWeek: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { quest -> QuestRow(quest) }
            if (goodWeek) {
                Text("Good week!", color = SystemGreen, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DashboardBossRow(boss: Boss, currentBest: Double) {
    val progress = if (boss.targetWeight > 0) (currentBest / boss.targetWeight).toFloat().coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(boss.name, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = SystemRed)
            Text(
                "${cleanNumber(currentBest)}kg / ${cleanNumber(boss.targetWeight)}kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun cleanNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
private fun StatRow(stat: Stat) {
    val color = statColor(stat.tag)
    val needed = xpForLevel(stat.level)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.tag.name, style = MaterialTheme.typography.titleMedium, color = color)
            Text(
                if (stat.level >= MAX_STAT_LEVEL) "Lv. $MAX_STAT_LEVEL (max)" else "Lv. ${stat.level}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (stat.level < MAX_STAT_LEVEL) {
            LinearProgressIndicator(
                progress = { (stat.currentXp.toFloat() / needed).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
            Text(
                "${stat.currentXp} / $needed XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun statColor(tag: StatTag) = when (tag) {
    StatTag.STR -> SystemRed
    StatTag.VIT -> SystemGreen
    StatTag.DISCIPLINE -> SystemBlue
    StatTag.INT -> SystemVioletBright
    StatTag.AGILITY -> SystemYellow
}

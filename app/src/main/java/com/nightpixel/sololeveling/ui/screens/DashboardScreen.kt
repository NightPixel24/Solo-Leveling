package com.nightpixel.sololeveling.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.Boss
import com.nightpixel.sololeveling.data.entity.ExerciseType
import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GoalStatus
import com.nightpixel.sololeveling.data.entity.GoalTier
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.data.entity.XpLog
import com.nightpixel.sololeveling.data.gamification.HabitCompletionStat
import com.nightpixel.sololeveling.data.gamification.MAX_STAT_LEVEL
import com.nightpixel.sololeveling.data.gamification.MoodDistribution
import com.nightpixel.sololeveling.data.gamification.QuestItem
import com.nightpixel.sololeveling.data.gamification.RankTier
import com.nightpixel.sololeveling.data.gamification.WeekGoodness
import com.nightpixel.sololeveling.data.gamification.WeeklyQuestResult
import com.nightpixel.sololeveling.data.gamification.WeeklyVolume
import com.nightpixel.sololeveling.data.gamification.computeDailyQuests
import com.nightpixel.sololeveling.data.gamification.computeRank
import com.nightpixel.sololeveling.data.gamification.computeWeeklyQuests
import com.nightpixel.sololeveling.data.gamification.goodWeekHistory
import com.nightpixel.sololeveling.data.gamification.gymVolumeByWeek
import com.nightpixel.sololeveling.data.gamification.habitCompletionRates
import com.nightpixel.sololeveling.data.gamification.moodDistributionForMonth
import com.nightpixel.sololeveling.data.gamification.statXpTrends
import com.nightpixel.sololeveling.data.gamification.xpForLevel
import com.nightpixel.sololeveling.ui.components.LineChart
import com.nightpixel.sololeveling.ui.components.LineSeries
import com.nightpixel.sololeveling.ui.components.MonthHeatmap
import com.nightpixel.sololeveling.ui.components.RadarChart
import com.nightpixel.sololeveling.ui.components.RankBadge
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemVioletBright
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** The Status Window (spec Section 6) plus (Phase 15) quick-add buttons, a mood month preview, a
 * Life Goals summary, and a full Analytics tab - the "tying everything together" phase spec
 * Section 10 names last. Everything here is computed live from data other phases already persist
 * (XpLog/HabitLog/GymSession/MoodEntry/Goal), the same "derive, don't store a second copy"
 * approach Quests/Rank/Rewards already established - see `data/gamification/Analytics.kt`'s doc
 * comment. Settings, including Export/Import, is reached from here rather than the bottom nav
 * (spec Section 8). Life Goals is reached by tapping the Rank badge or the goals summary (spec
 * Section 8). The gavel icon opens the spec Section 5.6 Punishment Pool, which - like Goals/
 * Settings - has no assigned bottom-nav slot either. */
private enum class DashboardTab(val label: String) { HOME("Home"), ANALYTICS("Analytics") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onPunishmentsClick: () -> Unit,
    onMoodClick: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val statDao = remember { app.database.statDao() }
    val goalDao = remember { app.database.goalDao() }
    val habitDao = remember { app.database.habitDao() }
    val gymDao = remember { app.database.gymDao() }
    val waterDao = remember { app.database.waterDao() }
    val moodDao = remember { app.database.moodDao() }
    val foodDao = remember { app.database.foodDao() }
    val taskDao = remember { app.database.taskDao() }
    val bossDao = remember { app.database.bossDao() }
    val rewardDao = remember { app.database.rewardDao() }
    val xpEngine = remember { app.xpEngine }
    val goldEngine = remember { app.goldEngine }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val stats by statDao.observeStats().collectAsState(initial = emptyList())
    val byTag = remember(stats) { stats.associateBy { it.tag } }
    val orderedStats = remember(byTag) { StatTag.entries.map { byTag[it] ?: Stat(tag = it) } }

    val goals by goalDao.observeAll().collectAsState(initial = emptyList())
    val rank = remember(goals) { computeRank(goals) }

    val today = remember { LocalDate.now() }
    val todayStr = remember(today) { today.toString() }
    val currentMonth = remember(today) { YearMonth.from(today) }
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
    val goldBalance by rewardDao.observeBalance().collectAsState(initial = null)
    val xpLogs by statDao.observeXpLogs().collectAsState(initial = emptyList())
    val waterLogsByDate = remember(allWaterLogs) { allWaterLogs.associateBy { it.date } }

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
            waterLogsByDate = waterLogsByDate
        )
    }

    var tab by remember { mutableStateOf(DashboardTab.HOME) }
    var showHabitPicker by remember { mutableStateOf(false) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingPhotoFile
        pendingPhotoFile = null
        if (success && file != null) capturedPhotoFile = file else file?.delete()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Dashboard") },
                    actions = {
                        // A small HUD-style corner badge rather than a full-size row on the home
                        // content itself (which read as a prominent, out-of-place "cash" callout
                        // right next to the Rank badge every time the screen loaded).
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.MonetizationOn,
                                contentDescription = "Gold",
                                tint = SystemYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "${goldBalance?.balance ?: 0}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onPunishmentsClick) {
                            Icon(Icons.Filled.Gavel, contentDescription = "Punishment Pool")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
                TabRow(selectedTabIndex = tab.ordinal) {
                    DashboardTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(Modifier.fillMaxWidth().padding(innerPadding)) {
            when (tab) {
                DashboardTab.HOME -> DashboardHome(
                    rank = rank,
                    onGoalsClick = onGoalsClick,
                    orderedStats = orderedStats,
                    dailyQuests = dailyQuests,
                    weeklyQuests = weeklyQuests,
                    bosses = bosses,
                    exercisesWithSessions = exercisesWithSessions,
                    goals = goals,
                    allTasks = allTasks,
                    currentMonth = currentMonth,
                    moodEntriesByDate = remember(moodEntries) { moodEntries.associateBy { it.date } },
                    onMoodClick = onMoodClick,
                    onGoalsSummaryClick = onGoalsClick,
                    onQuickAddHabit = { showHabitPicker = true },
                    onQuickAddWater = {
                        scope.launch {
                            val (logged, goal) = logWaterBottle(waterDao, xpEngine, todayStr, todayWaterLog)
                            val message = if (logged >= goal && (todayWaterLog?.bottlesLogged ?: 0) >= goal) {
                                "Daily water goal already reached ($logged/$goal cups)"
                            } else {
                                "Logged 1 cup ($logged/$goal)"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    onQuickAddFood = {
                        val file = createPhotoFile(context)
                        pendingPhotoFile = file
                        cameraLauncher.launch(photoFileUri(context, file))
                    }
                )
                DashboardTab.ANALYTICS -> DashboardAnalytics(
                    today = today,
                    currentMonth = currentMonth,
                    xpLogs = xpLogs,
                    habitsWithLogs = habitsWithLogs,
                    exercisesWithSessions = exercisesWithSessions,
                    moodEntries = moodEntries,
                    waterLogsByDate = waterLogsByDate
                )
            }
        }
    }

    if (showHabitPicker) {
        val undoneDaily = habitsWithLogs.filter { hwl ->
            hwl.habit.frequency == HabitFrequency.DAILY &&
                hwl.logs.none { it.date == todayStr && it.done }
        }
        AlertDialog(
            onDismissRequest = { showHabitPicker = false },
            title = { Text("Log a Habit") },
            text = {
                if (undoneDaily.isEmpty()) {
                    Text("All daily habits are already done today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        undoneDaily.forEach { hwl ->
                            Surface(
                                onClick = {
                                    toggleToday(hwl, today, habitDao, xpEngine, goldEngine, scope)
                                    showHabitPicker = false
                                },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    hwl.habit.title,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showHabitPicker = false }) { Text("Close") }
            }
        )
    }

    capturedPhotoFile?.let { file ->
        ConfirmFoodDialog(
            photoUri = photoFileUri(context, file),
            onDismiss = {
                file.delete()
                capturedPhotoFile = null
            },
            onSave = { description ->
                scope.launch {
                    foodDao.insertEntry(
                        FoodLogEntry(
                            date = LocalDate.now().toString(),
                            photoUri = photoFileUri(context, file).toString(),
                            description = description
                        )
                    )
                    xpEngine.grant(StatTag.VIT, 5, "Food logged")
                }
                capturedPhotoFile = null
            }
        )
    }
}

@Composable
private fun DashboardHome(
    rank: RankTier,
    onGoalsClick: () -> Unit,
    orderedStats: List<Stat>,
    dailyQuests: List<QuestItem>,
    weeklyQuests: WeeklyQuestResult,
    bosses: List<Boss>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    goals: List<Goal>,
    allTasks: List<Task>,
    currentMonth: YearMonth,
    moodEntriesByDate: Map<String, MoodEntry>,
    onMoodClick: () -> Unit,
    onGoalsSummaryClick: () -> Unit,
    onQuickAddHabit: () -> Unit,
    onQuickAddWater: () -> Unit,
    onQuickAddFood: () -> Unit
) {
    val tierGoals = remember(goals) {
        GoalTier.entries.mapNotNull { tier ->
            goals.filter { it.tier == tier && it.status == GoalStatus.ACTIVE }
                .minByOrNull { it.createdAt }
                ?.let { tier to it }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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
            val radarValues = remember(orderedStats) {
                orderedStats.map { it.tag.name to it.level / MAX_STAT_LEVEL.toFloat() }
            }
            RadarChart(values = radarValues, modifier = Modifier.fillMaxWidth())
        }
        items(orderedStats, key = { it.tag }) { stat ->
            StatRow(stat)
        }
        item { SectionHeader("Quick Add") }
        item { QuickAddRow(onQuickAddHabit, onQuickAddWater, onQuickAddFood) }
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
        item { SectionHeader("Mood This Month") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    MonthHeatmap(
                        month = currentMonth,
                        entriesByDate = moodEntriesByDate,
                        onDayClick = { onMoodClick() }
                    )
                }
            }
        }
        if (tierGoals.isNotEmpty()) {
            item { SectionHeader("Life Goals") }
            items(tierGoals, key = { it.first }) { (tier, goal) ->
                GoalSummaryRow(tier.label, goal, allTasks, onGoalsSummaryClick)
            }
        }
    }
}

@Composable
private fun QuickAddRow(onHabit: () -> Unit, onWater: () -> Unit, onFood: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onHabit, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Habit", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(onClick = onWater, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.LocalDrink, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Water", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(onClick = onFood, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" Food", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun GoalSummaryRow(
    tierLabel: String,
    goal: Goal,
    allTasks: List<Task>,
    onClick: () -> Unit
) {
    val linkedIds = remember(goal.linkedTaskIds) {
        goal.linkedTaskIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }
    val linkedTasks = remember(linkedIds, allTasks) { allTasks.filter { it.id in linkedIds } }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tierLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(goal.title, style = MaterialTheme.typography.titleMedium)
            if (linkedTasks.isNotEmpty()) {
                val done = linkedTasks.count { it.isDone }
                LinearProgressIndicator(
                    progress = { done.toFloat() / linkedTasks.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "$done/${linkedTasks.size} linked tasks done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardAnalytics(
    today: LocalDate,
    currentMonth: YearMonth,
    xpLogs: List<XpLog>,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    moodEntries: List<MoodEntry>,
    waterLogsByDate: Map<String, WaterLog>
) {
    val trends = remember(xpLogs, today) { statXpTrends(xpLogs, today) }
    val habitStats = remember(habitsWithLogs, today) { habitCompletionRates(habitsWithLogs, today) }
    val volume = remember(exercisesWithSessions, today) { gymVolumeByWeek(exercisesWithSessions, today) }
    val moodDist = remember(moodEntries, currentMonth) { moodDistributionForMonth(moodEntries, currentMonth) }
    val goodWeeks = remember(habitsWithLogs, exercisesWithSessions, waterLogsByDate, today) {
        goodWeekHistory(today, 8, habitsWithLogs, exercisesWithSessions, waterLogsByDate)
    }
    val prExercises = remember(exercisesWithSessions) {
        exercisesWithSessions.filter { it.exercise.type == ExerciseType.STRENGTH }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { SectionHeader("Stat Trends (30 days)") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LineChart(
                        series = StatTag.entries.map { tag ->
                            LineSeries(tag.name, statColor(tag), trends[tag].orEmpty().map { it.cumulativeXp.toFloat() })
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTag.entries.forEach { tag -> LegendDot(tag.name, statColor(tag)) }
                    }
                }
            }
        }

        item { SectionHeader("Habit Completion") }
        if (habitStats.isEmpty()) {
            item {
                Text(
                    "No habits yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        habitStats.forEach { stat -> HabitCompletionRow(stat) }
                    }
                }
            }
        }

        item { SectionHeader("Gym Volume (8 weeks)") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VolumeBarChart(volume)
                    if (prExercises.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Current PRs", style = MaterialTheme.typography.labelLarge)
                            prExercises.forEach { ews ->
                                val best = ews.sessions.mapNotNull { it.actualWeight }.maxOrNull()
                                Text(
                                    "${ews.exercise.name}: ${best?.let { cleanNumber(it) + "kg" } ?: "no PR yet"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Mood This Month") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoodDistributionBars(moodDist)
                }
            }
        }

        item { SectionHeader("Good Weeks") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoodWeekRow(goodWeeks)
                    Text(
                        "${goodWeeks.count { it.good }}/${goodWeeks.size} good weeks recently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitCompletionRow(stat: HabitCompletionStat) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stat.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${(stat.percent * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(progress = { stat.percent }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VolumeBarChart(volume: List<WeeklyVolume>) {
    val maxVolume = (volume.maxOfOrNull { it.volume } ?: 0.0).coerceAtLeast(1.0)
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        volume.forEach { wv ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val fraction = (wv.volume / maxVolume).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                        .background(SystemRed, shape = RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun MoodDistributionBars(dist: MoodDistribution) {
    val total = dist.total.coerceAtLeast(1)
    MoodBarRow("Good", dist.good, total, SystemGreen)
    MoodBarRow("OK", dist.ok, total, SystemYellow)
    MoodBarRow("Bad", dist.bad, total, SystemRed)
}

@Composable
private fun MoodBarRow(label: String, count: Int, total: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
        LinearProgressIndicator(
            progress = { count.toFloat() / total },
            modifier = Modifier.weight(1f),
            color = color
        )
        Text("$count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoodWeekRow(weeks: List<WeekGoodness>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weeks.forEach { week ->
            Icon(
                if (week.good) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (week.good) SystemGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun QuestList(quests: List<QuestItem>) {
    Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
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
    Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
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
    Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
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

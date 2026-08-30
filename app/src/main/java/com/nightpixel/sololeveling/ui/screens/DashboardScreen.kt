package com.nightpixel.sololeveling.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.BodyStatEntry
import com.nightpixel.sololeveling.data.entity.BodyStatType
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import com.nightpixel.sololeveling.data.entity.ExerciseType
import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GoalStatus
import com.nightpixel.sololeveling.data.entity.GoalTier
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.PlayerProfile
import com.nightpixel.sololeveling.data.entity.RoutineItem
import com.nightpixel.sololeveling.data.entity.ScheduledWorkout
import com.nightpixel.sololeveling.data.entity.SplitDay
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.data.entity.XpLog
import com.nightpixel.sololeveling.data.gamification.BloodPressureTrend
import com.nightpixel.sololeveling.data.gamification.FoodHealthDistribution
import com.nightpixel.sololeveling.data.gamification.HabitCompletionStat
import com.nightpixel.sololeveling.data.gamification.HealthPeriod
import com.nightpixel.sololeveling.data.gamification.MAX_STAT_LEVEL
import com.nightpixel.sololeveling.data.gamification.MoodDistribution
import com.nightpixel.sololeveling.data.gamification.NextUpItem
import com.nightpixel.sololeveling.data.gamification.RankTier
import com.nightpixel.sololeveling.data.gamification.Title
import com.nightpixel.sololeveling.data.gamification.WeekGoodness
import com.nightpixel.sololeveling.data.gamification.WeeklyVolume
import com.nightpixel.sololeveling.data.gamification.bloodPressureTrend
import com.nightpixel.sololeveling.data.gamification.bloodPressureValues
import com.nightpixel.sololeveling.data.gamification.bloodSugarTrend
import com.nightpixel.sololeveling.data.gamification.bloodSugarValues
import com.nightpixel.sololeveling.data.gamification.computeRank
import com.nightpixel.sololeveling.data.gamification.filterByPeriod
import com.nightpixel.sololeveling.data.gamification.foodHealthDistributionForMonth
import com.nightpixel.sololeveling.data.gamification.goodWeekHistory
import com.nightpixel.sololeveling.data.gamification.gymVolumeByWeek
import com.nightpixel.sololeveling.data.gamification.habitCompletionRates
import com.nightpixel.sololeveling.data.gamification.moodDistributionForMonth
import com.nightpixel.sololeveling.data.gamification.nextCalendarEvent
import com.nightpixel.sololeveling.data.gamification.nextHabit
import com.nightpixel.sololeveling.data.gamification.nextRoutineItem
import com.nightpixel.sololeveling.data.gamification.statXpTrends
import com.nightpixel.sololeveling.data.gamification.weightTrend
import com.nightpixel.sololeveling.data.gamification.weightValues
import com.nightpixel.sololeveling.data.gamification.titleById
import com.nightpixel.sololeveling.data.gamification.unlockedTitles
import com.nightpixel.sololeveling.data.gamification.workoutCalendarForMonth
import com.nightpixel.sololeveling.data.gamification.xpForLevel
import com.nightpixel.sololeveling.ui.components.GlowCard
import com.nightpixel.sololeveling.ui.components.GradientDivider
import com.nightpixel.sololeveling.ui.components.GradientProgressBar
import com.nightpixel.sololeveling.ui.components.LineChart
import com.nightpixel.sololeveling.ui.components.LineSeries
import com.nightpixel.sololeveling.ui.components.MonthHeatmap
import com.nightpixel.sololeveling.ui.components.RadarChart
import com.nightpixel.sololeveling.ui.components.RankBadge
import com.nightpixel.sololeveling.ui.components.StatChip
import com.nightpixel.sololeveling.ui.components.WorkoutCalendarLegend
import com.nightpixel.sololeveling.ui.components.WorkoutMonthCalendar
import com.nightpixel.sololeveling.ui.components.statTagColor
import com.nightpixel.sololeveling.ui.components.statTagFullName
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The Status Window (spec Section 6) plus (Phase 15) quick-add buttons, a mood month preview, a
 * Life Goals summary, and a full Analytics tab - the "tying everything together" phase spec
 * Section 10 names last. Everything here is computed live from data other phases already persist
 * (XpLog/HabitLog/GymSession/MoodEntry/Goal), the same "derive, don't store a second copy"
 * approach Quests/Rank/Rewards already established - see `data/gamification/Analytics.kt`'s doc
 * comment. Settings, including Export/Import, is reached from here rather than the bottom nav
 * (spec Section 8). Life Goals is reached via the flag icon in the top bar (moved off the Rank
 * badge, user feedback 2026-08-26) or the goals summary; the Rank badge itself now opens the stat
 * radar chart, which otherwise stays out of the everyday Home scroll. The gavel icon opens the
 * spec Section 5.6 Punishment Pool, which - like Goals/Settings - has no assigned bottom-nav slot
 * either. */
private enum class DashboardTab(val label: String) { HOME("Home"), HEALTH("Health"), ANALYTICS("Analytics") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSettingsClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onPunishmentsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onMoodClick: () -> Unit,
    onGymClick: () -> Unit
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
    val taskListDao = remember { app.database.taskListDao() }
    val playerProfileDao = remember { app.database.playerProfileDao() }
    val splitDayDao = remember { app.database.splitDayDao() }
    val scheduledWorkoutDao = remember { app.database.scheduledWorkoutDao() }
    val routineDao = remember { app.database.routineDao() }
    val calendarDao = remember { app.database.calendarDao() }
    val healthDao = remember { app.database.healthDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val stats by statDao.observeStats().collectAsState(initial = emptyList())
    val byTag = remember(stats) { stats.associateBy { it.tag } }
    val orderedStats = remember(byTag) { StatTag.entries.map { byTag[it] ?: Stat(tag = it) } }

    val goals by goalDao.observeAll().collectAsState(initial = emptyList())
    val rank = remember(goals) { computeRank(goals) }

    val profile by playerProfileDao.observe().collectAsState(initial = null)
    val equippedTitle = remember(profile, rank, orderedStats) {
        titleById(profile?.equippedTitleId, rank, orderedStats)
    }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showTitlePicker by remember { mutableStateOf(false) }
    var showRadarChart by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val todayStr = remember(today) { today.toString() }
    val currentMonth = remember(today) { YearMonth.from(today) }
    // A one-shot "now" for the whole composition, same "computed once, not live-ticking" approach
    // `today` above already established - "at a glance" doesn't need to redraw as the clock ticks,
    // only when the underlying data changes (a habit gets checked off, an event gets added, etc.).
    val nowMinute = remember { LocalTime.now().let { it.hour * 60 + it.minute } }
    val weekDays = remember(today) {
        val monday = today.with(DayOfWeek.MONDAY)
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    val habitsWithLogs by habitDao.observeHabitsWithLogs().collectAsState(initial = emptyList())
    val exercisesWithSessions by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val todayWaterLog by waterDao.observeLog(todayStr).collectAsState(initial = null)
    val allWaterLogs by waterDao.observeAllLogs().collectAsState(initial = emptyList())
    val moodEntries by moodDao.observeEntries().collectAsState(initial = emptyList())
    val foodEntries by foodDao.observeEntries().collectAsState(initial = emptyList())
    val allTasks by taskDao.observeAllTasks().collectAsState(initial = emptyList())
    val taskLists by taskListDao.observeLists().collectAsState(initial = emptyList())
    val xpLogs by statDao.observeXpLogs().collectAsState(initial = emptyList())
    val waterLogsByDate = remember(allWaterLogs) { allWaterLogs.associateBy { it.date } }
    val splitDays by splitDayDao.observeSplitDays().collectAsState(initial = emptyList())
    val scheduledWorkouts by scheduledWorkoutDao.observeAll().collectAsState(initial = emptyList())
    val routineItems by routineDao.observeItems().collectAsState(initial = emptyList())
    val calendarEvents by calendarDao.observeEvents().collectAsState(initial = emptyList())
    val bodyStatEntries by healthDao.observeEntries().collectAsState(initial = emptyList())
    val workoutsByDate = remember(exercisesWithSessions, splitDays, currentMonth) {
        workoutCalendarForMonth(currentMonth, exercisesWithSessions, splitDays)
    }

    // "At a Glance" (user feedback, 2026-08-30: replaces Today's/This Week's Quests - "start
    // fresh... at a glance window to see what's upcoming next") - three independent "next" signals
    // rather than one merged list, matching how the user described them separately.
    val habitTitleById = remember(habitsWithLogs) { habitsWithLogs.associate { it.habit.id to it.habit.title } }
    val nextRoutine = remember(routineItems, habitTitleById, nowMinute) {
        nextRoutineItem(routineItems, habitTitleById, nowMinute)
    }
    val nextHabitUp = remember(habitsWithLogs, today, nowMinute) {
        nextHabit(habitsWithLogs, today, nowMinute)
    }
    val nextEvent = remember(calendarEvents) { nextCalendarEvent(calendarEvents, Instant.now()) }
    val todaysWorkout = remember(scheduledWorkouts, splitDays, today) {
        scheduledWorkouts.find { it.dayOfWeek == today.dayOfWeek.value }
            ?.let { sw -> splitDays.find { it.id == sw.splitDayId } }
    }
    val todaysWorkoutDone = remember(exercisesWithSessions, todaysWorkout, todayStr) {
        todaysWorkout != null && exercisesWithSessions.any { ews ->
            ews.exercise.splitDayId == todaysWorkout.id && ews.sessions.any { it.date == todayStr }
        }
    }
    val waterGoalHitToday = todayWaterLog?.let { it.bottlesLogged >= it.goalBottles } ?: false
    val foodLoggedToday = remember(foodEntries, todayStr) { foodEntries.any { it.date == todayStr } }
    val moodLoggedToday = remember(moodEntries, todayStr) { moodEntries.any { it.date == todayStr } }
    val dailyHabits = remember(habitsWithLogs) { habitsWithLogs.filter { it.habit.frequency == HabitFrequency.DAILY } }
    val dailyHabitsDone = remember(dailyHabits, todayStr) {
        dailyHabits.count { hwl -> hwl.logs.any { it.date == todayStr && it.done } }
    }

    var tab by remember { mutableStateOf(DashboardTab.HOME) }
    var showHabitPicker by remember { mutableStateOf(false) }
    var showTaskQuickAdd by remember { mutableStateOf(false) }
    var showFoodDialog by remember { mutableStateOf(false) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingPhotoFile
        pendingPhotoFile = null
        if (success && file != null) {
            capturedPhotoFile?.let { if (it != file) it.delete() }
            capturedPhotoFile = file
        } else {
            file?.delete()
        }
    }
    fun launchFoodCamera() {
        val file = createPhotoFile(context)
        pendingPhotoFile = file
        cameraLauncher.launch(photoFileUri(context, file))
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Dashboard") },
                    actions = {
                        // Life Goals moved here from the Rank badge (user feedback, 2026-08-26) -
                        // grouped with the other screens that only live behind a top-bar icon.
                        IconButton(onClick = onGoalsClick) {
                            Icon(Icons.Filled.Flag, contentDescription = "Life Goals")
                        }
                        IconButton(onClick = onCalendarClick) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendar")
                        }
                        IconButton(onClick = onRewardsClick) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = "Rewards")
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
                GradientDivider()
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(Modifier.fillMaxWidth().padding(innerPadding)) {
            when (tab) {
                DashboardTab.HOME -> DashboardHome(
                    rank = rank,
                    onRankClick = { showRadarChart = true },
                    playerName = profile?.name ?: "Hunter",
                    equippedTitle = equippedTitle,
                    onNameClick = { showRenameDialog = true },
                    onTitleClick = { showTitlePicker = true },
                    orderedStats = orderedStats,
                    nextRoutine = nextRoutine,
                    nextHabitUp = nextHabitUp,
                    nextEvent = nextEvent,
                    todaysWorkout = todaysWorkout,
                    todaysWorkoutDone = todaysWorkoutDone,
                    waterGoalHitToday = waterGoalHitToday,
                    todayWaterLog = todayWaterLog,
                    foodLoggedToday = foodLoggedToday,
                    moodLoggedToday = moodLoggedToday,
                    dailyHabitsDone = dailyHabitsDone,
                    dailyHabitsTotal = dailyHabits.size,
                    goals = goals,
                    allTasks = allTasks,
                    onGymClick = onGymClick,
                    onGoalsSummaryClick = onGoalsClick,
                    onQuickAddTask = { showTaskQuickAdd = true },
                    onQuickAddWater = {
                        scope.launch {
                            val (logged, goal) = logWaterBottle(waterDao, foodDao, xpEngine, todayStr, todayWaterLog)
                            val message = if (logged >= goal && (todayWaterLog?.bottlesLogged ?: 0) >= goal) {
                                "Daily water goal already reached ($logged/$goal cups)"
                            } else {
                                "Logged 1 cup ($logged/$goal)"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    onQuickAddFood = { showFoodDialog = true },
                    onTickWaterGoal = { scope.launch { markWaterGoalHit(waterDao, foodDao, xpEngine, todayStr, todayWaterLog) } },
                    onTickFood = { showFoodDialog = true },
                    onTickMood = onMoodClick,
                    onTickHabits = { showHabitPicker = true }
                )
                DashboardTab.HEALTH -> DashboardHealth(
                    entries = bodyStatEntries,
                    onAdd = { entry -> scope.launch { healthDao.insert(entry) } },
                    onDelete = { entry -> scope.launch { healthDao.delete(entry) } }
                )
                DashboardTab.ANALYTICS -> DashboardAnalytics(
                    today = today,
                    currentMonth = currentMonth,
                    xpLogs = xpLogs,
                    habitsWithLogs = habitsWithLogs,
                    exercisesWithSessions = exercisesWithSessions,
                    moodEntries = moodEntries,
                    foodEntries = foodEntries,
                    waterLogsByDate = waterLogsByDate,
                    bodyStatEntries = bodyStatEntries,
                    splitDays = splitDays,
                    workoutsByDate = workoutsByDate,
                    onGymClick = onGymClick,
                    onMoodClick = onMoodClick
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
                                    toggleToday(hwl, today, habitDao, foodDao, xpEngine, scope)
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

    // Both Quick Add's Food button and the At a Glance checklist's "Food" row open this same
    // dialog directly (photo optional inside, via `onTakePhoto`) rather than launching the camera
    // first - matches FoodScreen's own single-entry-point flow (third feedback pass, 2026-08-26:
    // "I dont like how theres an edit button and camera button seperate from each other"), which
    // the Dashboard's old camera-first shortcut had drifted from.
    if (showFoodDialog || capturedPhotoFile != null) {
        val file = capturedPhotoFile
        ConfirmFoodDialog(
            photoUri = file?.let { photoFileUri(context, it) },
            onTakePhoto = { launchFoodCamera() },
            onDismiss = {
                file?.delete()
                capturedPhotoFile = null
                showFoodDialog = false
            },
            onSave = { description, rating ->
                val photoUriString = file?.let { photoFileUri(context, it).toString() }
                scope.launch { logFood(foodDao, xpEngine, photoUriString, description, rating) }
                capturedPhotoFile = null
                showFoodDialog = false
            }
        )
    }

    if (showTaskQuickAdd) {
        TaskQuickAddDialog(
            taskLists = taskLists,
            onDismiss = { showTaskQuickAdd = false },
            onConfirm = { title, listId ->
                scope.launch { taskDao.insertTask(Task(title = title, listId = listId)) }
                showTaskQuickAdd = false
            }
        )
    }

    if (showRenameDialog) {
        RenameProfileDialog(
            initialName = profile?.name ?: "Hunter",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                scope.launch {
                    playerProfileDao.upsert((profile ?: PlayerProfile()).copy(name = newName))
                }
                showRenameDialog = false
            }
        )
    }

    if (showTitlePicker) {
        TitlePickerDialog(
            unlocked = unlockedTitles(rank, orderedStats),
            equippedId = equippedTitle.id,
            onDismiss = { showTitlePicker = false },
            onEquip = { titleId ->
                scope.launch {
                    playerProfileDao.upsert((profile ?: PlayerProfile()).copy(equippedTitleId = titleId))
                }
                showTitlePicker = false
            }
        )
    }

    if (showRadarChart) {
        RadarChartDialog(orderedStats = orderedStats, onDismiss = { showRadarChart = false })
    }
}

/** The Home tab, rewritten per user feedback (2026-08-30: "let's start fresh") - Today's/This
 * Week's Quests and the Workout Calendar preview are gone (the latter moved to Analytics), the old
 * Habit quick-add is replaced by a Task quick-add, and a new "At a Glance" section (next-up signals
 * plus a today checklist) now sits above Quick Add, with the Rank/stat block moved below it. */
@Composable
private fun DashboardHome(
    rank: RankTier,
    onRankClick: () -> Unit,
    playerName: String,
    equippedTitle: Title,
    onNameClick: () -> Unit,
    onTitleClick: () -> Unit,
    orderedStats: List<Stat>,
    nextRoutine: NextUpItem?,
    nextHabitUp: NextUpItem?,
    nextEvent: CalendarEventCache?,
    todaysWorkout: SplitDay?,
    todaysWorkoutDone: Boolean,
    waterGoalHitToday: Boolean,
    todayWaterLog: WaterLog?,
    foodLoggedToday: Boolean,
    moodLoggedToday: Boolean,
    dailyHabitsDone: Int,
    dailyHabitsTotal: Int,
    goals: List<Goal>,
    allTasks: List<Task>,
    onGymClick: () -> Unit,
    onGoalsSummaryClick: () -> Unit,
    onQuickAddTask: () -> Unit,
    onQuickAddWater: () -> Unit,
    onQuickAddFood: () -> Unit,
    onTickWaterGoal: () -> Unit,
    onTickFood: () -> Unit,
    onTickMood: () -> Unit,
    onTickHabits: () -> Unit
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
        item { SectionHeader("At a Glance") }
        item {
            NextUpCard(nextRoutine = nextRoutine, nextHabitUp = nextHabitUp, nextEvent = nextEvent)
        }
        item {
            TodayChecklistCard(
                todaysWorkout = todaysWorkout,
                todaysWorkoutDone = todaysWorkoutDone,
                waterGoalHitToday = waterGoalHitToday,
                todayWaterLog = todayWaterLog,
                foodLoggedToday = foodLoggedToday,
                moodLoggedToday = moodLoggedToday,
                dailyHabitsDone = dailyHabitsDone,
                dailyHabitsTotal = dailyHabitsTotal,
                onGymClick = onGymClick,
                onTickWaterGoal = onTickWaterGoal,
                onTickFood = onTickFood,
                onTickMood = onTickMood,
                onTickHabits = onTickHabits
            )
        }
        item { SectionHeader("Quick Add") }
        item { QuickAddRow(onQuickAddTask, onQuickAddWater, onQuickAddFood) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankBadge(rank = rank, onClick = onRankClick)
                Column {
                    Text(
                        playerName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable(onClick = onNameClick)
                    )
                    Text(
                        equippedTitle.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onTitleClick)
                    )
                }
            }
        }
        items(orderedStats, key = { it.tag }) { stat ->
            StatRow(stat)
        }
        if (tierGoals.isNotEmpty()) {
            item { SectionHeader("Life Goals") }
            items(tierGoals, key = { it.first }) { (tier, goal) ->
                GoalSummaryRow(tier.label, goal, allTasks, onGoalsSummaryClick)
            }
        }
    }
}

/** Up to three independent "what's next" rows (user feedback, 2026-08-30) - Routine schedule,
 * habits, and calendar each only show up when something with an actual time still lies ahead
 * today (see [nextRoutineItem]/[nextHabit]/[nextCalendarEvent]'s doc comments). All three empty is
 * a real, expected state (nothing time-specific left today), not an error - shown as one quiet line
 * instead of an empty card. */
@Composable
private fun NextUpCard(nextRoutine: NextUpItem?, nextHabitUp: NextUpItem?, nextEvent: CalendarEventCache?) {
    GlowCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Next Up", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (nextRoutine == null && nextHabitUp == null && nextEvent == null) {
                Text(
                    "Nothing else scheduled today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                nextRoutine?.let { NextUpRow(Icons.Filled.Schedule, it.label, formatMinutes(it.minuteOfDay)) }
                nextHabitUp?.let { NextUpRow(Icons.Filled.Repeat, it.label, formatMinutes(it.minuteOfDay)) }
                nextEvent?.let {
                    val time = Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalTime()
                    NextUpRow(Icons.Filled.Event, it.title, time.format(DateTimeFormatter.ofPattern("h:mm a")))
                }
            }
        }
    }
}

@Composable
private fun NextUpRow(icon: ImageVector, label: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "What's coming up in the day" (user feedback, 2026-08-30) - a broader, not-immediate view than
 * [NextUpCard]: today's gym split, and whether water/food/mood/habits are logged yet. Water/Food/
 * Mood/Habits are real checkboxes a tap can act on directly (see each `onTick*` callback's own
 * wiring in `DashboardScreen`); Gym shows status but taps through to the Gym screen instead, since
 * "done" there means logging real sets/reps/weight, not something a single tap can represent. */
@Composable
private fun TodayChecklistCard(
    todaysWorkout: SplitDay?,
    todaysWorkoutDone: Boolean,
    waterGoalHitToday: Boolean,
    todayWaterLog: WaterLog?,
    foodLoggedToday: Boolean,
    moodLoggedToday: Boolean,
    dailyHabitsDone: Int,
    dailyHabitsTotal: Int,
    onGymClick: () -> Unit,
    onTickWaterGoal: () -> Unit,
    onTickFood: () -> Unit,
    onTickMood: () -> Unit,
    onTickHabits: () -> Unit
) {
    GlowCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Coming Up Today",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            ChecklistRow(
                label = todaysWorkout?.let { "Workout: ${it.name}" } ?: "No workout scheduled today",
                checked = todaysWorkoutDone,
                onClick = onGymClick
            )
            val goalBottles = todayWaterLog?.goalBottles ?: 8
            val loggedBottles = todayWaterLog?.bottlesLogged ?: 0
            ChecklistRow(
                label = "Hit water goal ($loggedBottles/$goalBottles cups)",
                checked = waterGoalHitToday,
                onClick = { if (!waterGoalHitToday) onTickWaterGoal() }
            )
            ChecklistRow(label = "Log food today", checked = foodLoggedToday, onClick = { if (!foodLoggedToday) onTickFood() })
            ChecklistRow(label = "Log today's mood", checked = moodLoggedToday, onClick = { if (!moodLoggedToday) onTickMood() })
            ChecklistRow(
                label = "Daily habits ($dailyHabitsDone/$dailyHabitsTotal done)",
                checked = dailyHabitsTotal > 0 && dailyHabitsDone >= dailyHabitsTotal,
                onClick = onTickHabits
            )
        }
    }
}

@Composable
private fun ChecklistRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuickAddRow(onTask: () -> Unit, onWater: () -> Unit, onFood: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onTask, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Task", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        OutlinedButton(onClick = onWater, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.LocalDrink, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Water", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
        OutlinedButton(onClick = onFood, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Food", style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskQuickAddDialog(
    taskLists: List<TaskList>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, listId: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    // Defaults to the permanent "Daily" list (TaskList.DEFAULT_ID) - user feedback, 2026-08-30:
    // "by default, it should add to the daily task section, but it should also have a drop down to
    // show me the task list... if I wanna add it to a different task list."
    var selectedListId by remember(taskLists) { mutableStateOf(TaskList.DEFAULT_ID) }
    var expanded by remember { mutableStateOf(false) }
    val selectedListName = taskLists.find { it.id == selectedListId }?.name ?: "Daily"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("List: $selectedListName", modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        taskLists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = { selectedListId = list.id; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim(), selectedListId) },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

/** The Dashboard's Health tab (user feedback, 2026-08-30: "I want to record my body stats. So
 * weight, blood sugar mmol/L, and blood pressure"). A quick-add row picks which [BodyStatType] to
 * log (each opens the same [AddBodyStatDialog], parameterized by type); tapping a type's own
 * section heading below drills into [HealthDetailView] for that one type (user feedback,
 * 2026-08-26: "have an option to click the heading... swaps out the log a reading screen section
 * for a scrollable version of the readings with a chart at the top"). Trend charts for the
 * Analytics tab's own "Health Trends" section are separate (last-30-readings, all types at once);
 * this detail view is per-type and period-filterable instead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardHealth(
    entries: List<BodyStatEntry>,
    onAdd: (BodyStatEntry) -> Unit,
    onDelete: (BodyStatEntry) -> Unit
) {
    var addDialogType by remember { mutableStateOf<BodyStatType?>(null) }
    var detailType by remember { mutableStateOf<BodyStatType?>(null) }
    val byType = remember(entries) { entries.groupBy { it.type } }

    detailType?.let { type ->
        HealthDetailView(
            type = type,
            entries = byType[type].orEmpty(),
            onBack = { detailType = null },
            onDelete = onDelete
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { SectionHeader("Log a Reading") }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // contentPadding tightened from ButtonDefaults' default 24.dp horizontal - three
                // equal-weight buttons on one row leaves little room, and "Weight" (the longest
                // label) was wrapping to 2 lines at the default padding once the icon-to-text gap
                // below switched from a cramped leading-space hack to a real Spacer.
                val readingButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                OutlinedButton(
                    onClick = { addDialogType = BodyStatType.WEIGHT },
                    modifier = Modifier.weight(1f),
                    contentPadding = readingButtonPadding
                ) {
                    // Icon(Icons.Filled.Scale, ...) previously here - that glyph's own artwork
                    // leaves dead space on its right edge, which swallowed the leading-space
                    // hack below and read as touching the text (user feedback, 2026-08-30: "weight
                    // is still off aligned" - a first icon swap alone didn't fix it). Straighten
                    // (a ruler) is symmetric within its bounding box like Bloodtype/Favorite are,
                    // and every button now uses an explicit Spacer instead of a leading space
                    // character so the icon-to-text gap no longer depends on a glyph's own padding.
                    Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Weight", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { addDialogType = BodyStatType.BLOOD_SUGAR },
                    modifier = Modifier.weight(1f),
                    contentPadding = readingButtonPadding
                ) {
                    Icon(Icons.Filled.Bloodtype, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sugar", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { addDialogType = BodyStatType.BLOOD_PRESSURE },
                    modifier = Modifier.weight(1f),
                    contentPadding = readingButtonPadding
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("BP", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
        BodyStatType.entries.forEach { type ->
            val typeEntries = byType[type].orEmpty()
            item { ClickableSectionHeader(type.label, onClick = { detailType = type }) }
            if (typeEntries.isEmpty()) {
                item {
                    Text(
                        "No entries yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(typeEntries, key = { it.id }) { entry -> BodyStatRow(entry, onDelete = { onDelete(entry) }) }
            }
        }
    }

    addDialogType?.let { type ->
        AddBodyStatDialog(
            type = type,
            onDismiss = { addDialogType = null },
            onSave = { entry -> onAdd(entry); addDialogType = null }
        )
    }
}

@Composable
private fun ClickableSectionHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "View $title history",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The per-type drill-in view a tapped Health section heading opens: a period selector (spec-free,
 * user-requested "weekly, monthly, 6 months, by year and all time"), a chart of that period's
 * readings, and the full scrollable list for the period below - replacing the whole Health tab body
 * while open (back arrow returns to it). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthDetailView(
    type: BodyStatType,
    entries: List<BodyStatEntry>,
    onBack: () -> Unit,
    onDelete: (BodyStatEntry) -> Unit
) {
    var period by remember { mutableStateOf(HealthPeriod.MONTH) }
    val periodEntries = remember(entries, period) { filterByPeriod(entries, period) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(type.label, style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HealthPeriod.entries.forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (type == BodyStatType.BLOOD_PRESSURE) {
                        val bp = remember(periodEntries) { bloodPressureValues(periodEntries) }
                        if (bp.systolic.size < 2) {
                            Text(
                                "Not enough readings in this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LineChart(
                                series = listOf(
                                    LineSeries("Systolic", SystemRed, bp.systolic),
                                    LineSeries("Diastolic", SystemYellow, bp.diastolic)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LegendDot("Systolic", SystemRed)
                                LegendDot("Diastolic", SystemYellow)
                            }
                        }
                    } else {
                        val values = remember(periodEntries, type) {
                            if (type == BodyStatType.WEIGHT) weightValues(periodEntries) else bloodSugarValues(periodEntries)
                        }
                        if (values.size < 2) {
                            Text(
                                "Not enough readings in this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LineChart(
                                series = listOf(LineSeries(type.label, SystemBlue, values)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        if (periodEntries.isEmpty()) {
            item {
                Text(
                    "No entries in this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(periodEntries.sortedByDescending { it.timestamp }, key = { it.id }) { entry ->
                BodyStatRow(entry, onDelete = { onDelete(entry) })
            }
        }
    }
}

private val bodyStatTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")

/** No always-visible delete button (user feedback, 2026-08-30) - long-press reveals it instead,
 * matching a common list-row pattern; a normal tap while revealed hides it again without deleting. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BodyStatRow(entry: BodyStatEntry, onDelete: () -> Unit) {
    var showDelete by remember(entry.id) { mutableStateOf(false) }
    val dateTime = remember(entry.timestamp) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.timestamp), ZoneId.systemDefault())
    }
    val valueText = when (entry.type) {
        BodyStatType.WEIGHT -> entry.value?.let { "${cleanNumber(it)} kg" } ?: "-"
        BodyStatType.BLOOD_SUGAR -> entry.value?.let { "${cleanNumber(it)} mmol/L" } ?: "-"
        BodyStatType.BLOOD_PRESSURE -> if (entry.systolic != null && entry.diastolic != null) {
            "${entry.systolic}/${entry.diastolic} mmHg"
        } else "-"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (showDelete) showDelete = false },
                    onLongClick = { showDelete = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(valueText, style = MaterialTheme.typography.titleMedium)
                Text(
                    dateTime.format(bodyStatTimeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBodyStatDialog(
    type: BodyStatType,
    onDismiss: () -> Unit,
    onSave: (BodyStatEntry) -> Unit
) {
    var dateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var valueText by remember { mutableStateOf("") }
    var systolicText by remember { mutableStateOf("") }
    var diastolicText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val valid = when (type) {
        BodyStatType.BLOOD_PRESSURE -> systolicText.toIntOrNull() != null && diastolicText.toIntOrNull() != null
        else -> valueText.toDoubleOrNull() != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${type.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (type == BodyStatType.BLOOD_PRESSURE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = systolicText,
                            onValueChange = { systolicText = it.filter { c -> c.isDigit() } },
                            label = { Text("Systolic") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = diastolicText,
                            onValueChange = { diastolicText = it.filter { c -> c.isDigit() } },
                            label = { Text("Diastolic") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = { valueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (type == BodyStatType.WEIGHT) "Weight (kg)" else "Blood sugar (mmol/L)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                    }
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a")))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val entry = if (type == BodyStatType.BLOOD_PRESSURE) {
                        BodyStatEntry(
                            type = type,
                            timestamp = timestamp,
                            systolic = systolicText.toIntOrNull(),
                            diastolic = diastolicText.toIntOrNull()
                        )
                    } else {
                        BodyStatEntry(type = type, timestamp = timestamp, value = valueText.toDoubleOrNull())
                    }
                    onSave(entry)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val initialMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        dateTime = LocalDateTime.of(newDate, dateTime.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    dateTime = LocalDateTime.of(dateTime.toLocalDate(), LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
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
    foodEntries: List<FoodLogEntry>,
    waterLogsByDate: Map<String, WaterLog>,
    bodyStatEntries: List<BodyStatEntry>,
    splitDays: List<SplitDay>,
    workoutsByDate: Map<LocalDate, SplitDay>,
    onGymClick: () -> Unit,
    onMoodClick: () -> Unit
) {
    val trends = remember(xpLogs, today) { statXpTrends(xpLogs, today) }
    val habitStats = remember(habitsWithLogs, today) { habitCompletionRates(habitsWithLogs, today) }
    val volume = remember(exercisesWithSessions, today) { gymVolumeByWeek(exercisesWithSessions, today) }
    val moodEntriesByDate = remember(moodEntries) { moodEntries.associateBy { it.date } }
    val moodDist = remember(moodEntries, currentMonth) { moodDistributionForMonth(moodEntries, currentMonth) }
    val foodDist = remember(foodEntries, currentMonth) { foodHealthDistributionForMonth(foodEntries, currentMonth) }
    val goodWeeks = remember(habitsWithLogs, exercisesWithSessions, waterLogsByDate, today) {
        goodWeekHistory(today, 8, habitsWithLogs, exercisesWithSessions, waterLogsByDate)
    }
    val prExercises = remember(exercisesWithSessions) {
        exercisesWithSessions.filter { it.exercise.type == ExerciseType.STRENGTH }
    }
    val weightSeries = remember(bodyStatEntries) { weightTrend(bodyStatEntries) }
    val sugarSeries = remember(bodyStatEntries) { bloodSugarTrend(bodyStatEntries) }
    val bpSeries = remember(bodyStatEntries) { bloodPressureTrend(bodyStatEntries) }

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
                            LineSeries(tag.name, statTagColor(tag), trends[tag].orEmpty().map { it.cumulativeXp.toFloat() })
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTag.entries.forEach { tag -> LegendDot(tag.name, statTagColor(tag)) }
                    }
                }
            }
        }

        // Moved here from the Home tab (user feedback, 2026-08-30: "move out the workout
        // calendar... putting that under the analytics section") - Home now leads with the "At a
        // Glance" checklist instead.
        item { SectionHeader("Workout Calendar") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onGymClick),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutMonthCalendar(month = currentMonth, workoutsByDate = workoutsByDate, onDayClick = { onGymClick() })
                    if (splitDays.isNotEmpty()) WorkoutCalendarLegend(splitDays)
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

        // Moved here from the Home tab (user feedback, 2026-08-30: "move mood calendar on the
        // home screen to the analytics tab") - sits above the distribution bars this section
        // already had, both under the one "Mood This Month" header rather than two.
        item { SectionHeader("Mood This Month") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MonthHeatmap(
                        month = currentMonth,
                        entriesByDate = moodEntriesByDate,
                        onDayClick = { onMoodClick() }
                    )
                    MoodDistributionBars(moodDist)
                }
            }
        }

        item { SectionHeader("Food Health This Month") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodHealthBars(foodDist)
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

        item { SectionHeader("Health Trends") }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HealthTrendChart("Weight (kg)", weightSeries, SystemBlue)
                    HealthTrendChart("Blood Sugar (mmol/L)", sugarSeries, SystemGreen)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Blood Pressure (mmHg)", style = MaterialTheme.typography.labelLarge)
                        if (bpSeries.systolic.size < 2) {
                            Text(
                                "Not enough readings yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LineChart(
                                series = listOf(
                                    LineSeries("Systolic", SystemRed, bpSeries.systolic),
                                    LineSeries("Diastolic", SystemYellow, bpSeries.diastolic)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LegendDot("Systolic", SystemRed)
                                LegendDot("Diastolic", SystemYellow)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthTrendChart(label: String, values: List<Float>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (values.size < 2) {
            Text(
                "Not enough readings yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LineChart(series = listOf(LineSeries(label, color, values)), modifier = Modifier.fillMaxWidth())
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
private fun FoodHealthBars(dist: FoodHealthDistribution) {
    val total = dist.total.coerceAtLeast(1)
    FoodBarRow("Healthy", dist.healthy, total, SystemGreen)
    FoodBarRow("OK", dist.ok, total, SystemYellow)
    FoodBarRow("Unhealthy", dist.unhealthy, total, SystemRed)
}

@Composable
private fun FoodBarRow(label: String, count: Int, total: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp))
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

private fun cleanNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
private fun StatRow(stat: Stat) {
    val color = statTagColor(stat.tag)
    val needed = xpForLevel(stat.level)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(statTagFullName(stat.tag), style = MaterialTheme.typography.titleMedium, color = color)
            Text(
                if (stat.level >= MAX_STAT_LEVEL) "Lv. $MAX_STAT_LEVEL (max)" else "Lv. ${stat.level}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (stat.level < MAX_STAT_LEVEL) {
            GradientProgressBar(
                progress = stat.currentXp.toFloat() / needed,
                baseColor = color,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "${stat.currentXp} / $needed XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/** The radar chart used to sit permanently in the Home scroll; moved behind tapping the Rank
 * badge instead (user feedback, 2026-08-26: "hide the spiderchart normally") - it's a detail view
 * now, not a fixture of the everyday scroll. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadarChartDialog(orderedStats: List<Stat>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stat Radar") },
        text = {
            val radarValues = remember(orderedStats) {
                orderedStats.map { it.tag.name to it.level / MAX_STAT_LEVEL.toFloat() }
            }
            RadarChart(values = radarValues, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameProfileDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TitlePickerDialog(
    unlocked: List<Title>,
    equippedId: String,
    onDismiss: () -> Unit,
    onEquip: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Title") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                unlocked.forEach { title ->
                    val selected = title.id == equippedId
                    Surface(
                        onClick = { onEquip(title.id) },
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title.displayName, style = MaterialTheme.typography.bodyLarge)
                            if (selected) {
                                Icon(Icons.Filled.Check, contentDescription = "Equipped", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

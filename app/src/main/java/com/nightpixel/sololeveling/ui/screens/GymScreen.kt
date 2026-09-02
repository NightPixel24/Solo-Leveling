package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.Exercise
import com.nightpixel.sololeveling.data.entity.ExerciseType
import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.GymSession
import com.nightpixel.sololeveling.data.entity.RestDayLog
import com.nightpixel.sololeveling.data.entity.ScheduledWorkout
import com.nightpixel.sololeveling.data.entity.SplitDay
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.gamification.workoutCalendarForMonth
import com.nightpixel.sololeveling.ui.components.StatChip
import com.nightpixel.sololeveling.ui.components.SubtleIconButton
import com.nightpixel.sololeveling.ui.components.WorkoutCalendarLegend
import com.nightpixel.sololeveling.ui.components.WorkoutMonthCalendar
import com.nightpixel.sololeveling.ui.components.parseHexColor
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class GymTab(val label: String) { ROUTINE("Routine"), WORKOUTS("Workouts"), CALENDAR("Calendar") }

/** Exercises are grouped under user-defined [SplitDay]s ("Back Day", "Chest and Shoulders", ...) -
 * called "Workouts" in the UI (renamed from "Split Day"/"Routine", user feedback, 2026-08-30) -
 * rather than fixed weekdays (user feedback, 2026-08-26: missing a gym day and working out the
 * next day used to push every later exercise onto the wrong weekday header - a workout with no
 * calendar day baked into it can't have that problem). The new Routine tab is a separate, purely
 * optional weekly *plan* (which [SplitDay] you intend for each weekday, [ScheduledWorkout]) - the
 * Calendar tab stays the derived record of what was actually done on which date, built live from
 * GymSession data (see `data/gamification/WorkoutCalendar.kt`) and never reads the plan, so
 * missing a planned day can't corrupt anything (same reasoning that killed fixed weekdays in the
 * first place). Tapping a Calendar day opens [DayDetailDialog] to see/backfill that specific date.
 * Boss Fights (spec Section 5.5) was removed per user feedback (2026-08-26) - see
 * `MIGRATION_15_16`'s doc comment for the data-layer side of the removal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val gymDao = remember { app.database.gymDao() }
    val splitDayDao = remember { app.database.splitDayDao() }
    val scheduledWorkoutDao = remember { app.database.scheduledWorkoutDao() }
    val restDayLogDao = remember { app.database.restDayLogDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val exercises by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val splitDays by splitDayDao.observeSplitDays().collectAsState(initial = emptyList())
    val scheduledWorkouts by scheduledWorkoutDao.observeAll().collectAsState(initial = emptyList())
    val restDayLogs by restDayLogDao.observeAll().collectAsState(initial = emptyList())

    var tab by remember { mutableStateOf(GymTab.ROUTINE) }
    // Workouts-tab-only edit toggle (user feedback, 2026-09-02: "move the delete button to the top
    // next to the plus icon. When clicked it shows all the delete icons") - same pencil/check
    // pattern the Routine tab's own edit toggle already uses: delete affordances on each workout
    // header and exercise row stay hidden until this is on.
    var workoutsEditMode by remember { mutableStateOf(false) }
    // Routine-tab-only edit toggle (user feedback, 2026-08-31: "add an edit button for gym
    // routine as well so i can edit what workouts are on each day and delete them") - same
    // pattern the Routine (Schedule/Habits) screen's own Edit button just established: swaps a
    // long-press-to-reveal delete affordance for an always-visible one while active. Tapping a
    // day row to open the assign/reassign picker already worked with no mode gate, in either
    // mode - unaffected, that's the "edit what workout is on each day" half of the ask.
    var routineEditMode by remember { mutableStateOf(false) }
    var showAddDayDialog by remember { mutableStateOf(false) }
    var editDayTarget by remember { mutableStateOf<SplitDay?>(null) }
    var deleteDayTarget by remember { mutableStateOf<SplitDay?>(null) }
    var addExerciseTargetDay by remember { mutableStateOf<SplitDay?>(null) }
    var logTarget by remember { mutableStateOf<Pair<Exercise, LocalDate>?>(null) }
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }
    var showClearScheduleConfirm by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Gym")
                            StatChip(StatTag.STR)
                        }
                    },
                    actions = {
                        // Only the Workouts tab has a top-bar add target - Routine assigns via a
                        // per-day picker (there's no single "add" action for a 7-row schedule) and
                        // Calendar adds by tapping a day instead.
                        if (tab == GymTab.WORKOUTS) {
                            IconButton(onClick = { workoutsEditMode = !workoutsEditMode }) {
                                Icon(
                                    if (workoutsEditMode) Icons.Filled.Check else Icons.Outlined.Edit,
                                    contentDescription = if (workoutsEditMode) "Done editing" else "Edit workouts"
                                )
                            }
                            IconButton(onClick = { showAddDayDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add workout")
                            }
                        }
                        if (tab == GymTab.ROUTINE) {
                            IconButton(onClick = { routineEditMode = !routineEditMode }) {
                                Icon(
                                    if (routineEditMode) Icons.Filled.Check else Icons.Outlined.Edit,
                                    contentDescription = if (routineEditMode) "Done editing" else "Edit schedule"
                                )
                            }
                            if (scheduledWorkouts.isNotEmpty()) {
                                IconButton(onClick = { showClearScheduleConfirm = true }) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear entire schedule")
                                }
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = tab.ordinal) {
                    GymTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                    }
                }
            }
        }
    ) { innerPadding ->
        when (tab) {
            GymTab.WORKOUTS -> WorkoutsTab(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                exercises = exercises,
                splitDays = splitDays,
                today = today,
                editMode = workoutsEditMode,
                restLoggedTodaySplitDayId = restDayLogs.find { it.date == today.toString() }?.splitDayId,
                onEditDay = { editDayTarget = it },
                onDeleteDay = { deleteDayTarget = it },
                onAddExerciseToDay = { addExerciseTargetDay = it },
                onToggleExercise = { exercise, day, done ->
                    if (done) {
                        scope.launch { gymDao.deleteSession(exercise.id, day.toString()) }
                    } else {
                        logTarget = exercise to day
                    }
                },
                onDeleteExercise = { exercise -> scope.launch { gymDao.deleteExercise(exercise) } },
                onToggleRestDay = { day, checked ->
                    scope.launch {
                        if (checked) restDayLogDao.upsert(RestDayLog(date = today.toString(), splitDayId = day.id))
                        else restDayLogDao.deleteByDate(today.toString())
                    }
                }
            )
            GymTab.ROUTINE -> ScheduleTab(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                splitDays = splitDays,
                scheduledWorkouts = scheduledWorkouts,
                editMode = routineEditMode,
                today = today,
                onAssign = { dayOfWeek, splitDayId ->
                    scope.launch { scheduledWorkoutDao.upsert(ScheduledWorkout(dayOfWeek, splitDayId)) }
                },
                onClearDay = { dayOfWeek -> scope.launch { scheduledWorkoutDao.clearDay(dayOfWeek) } }
            )
            GymTab.CALENDAR -> CalendarTab(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                exercises = exercises,
                splitDays = splitDays,
                restDayLogs = restDayLogs,
                onDayClick = { date -> dayDetailDate = date }
            )
        }
    }

    if (showAddDayDialog) {
        SplitDayDialog(
            onDismiss = { showAddDayDialog = false },
            onConfirm = { name, colorHex, isRest ->
                scope.launch {
                    splitDayDao.insert(
                        SplitDay(name = name, colorHex = colorHex, orderIndex = splitDays.size, isRest = isRest)
                    )
                }
                showAddDayDialog = false
            }
        )
    }

    editDayTarget?.let { day ->
        SplitDayDialog(
            initial = day,
            // The rest/workout toggle can't be flipped once exercises are attached - that's
            // delete-and-recreate territory, not an edit (same reasoning RoutineItemEditorDialog
            // uses for its own locked mode).
            restToggleLocked = exercises.any { it.exercise.splitDayId == day.id },
            onDismiss = { editDayTarget = null },
            onConfirm = { name, colorHex, isRest ->
                scope.launch { splitDayDao.update(day.copy(name = name, colorHex = colorHex, isRest = isRest)) }
                editDayTarget = null
            }
        )
    }

    deleteDayTarget?.let { day ->
        val exerciseCount = exercises.count { it.exercise.splitDayId == day.id }
        AlertDialog(
            onDismissRequest = { deleteDayTarget = null },
            title = { Text("Delete '${day.name}'?") },
            text = {
                Text(
                    if (exerciseCount > 0) {
                        "This also deletes its $exerciseCount exercise${if (exerciseCount == 1) "" else "s"} and their logged history."
                    } else {
                        "This workout has no exercises yet."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { splitDayDao.delete(day) }
                    deleteDayTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteDayTarget = null }) { Text("Cancel") }
            }
        )
    }

    addExerciseTargetDay?.let { day ->
        AddExerciseDialog(
            splitDayName = day.name,
            onDismiss = { addExerciseTargetDay = null },
            onConfirm = { exercise ->
                scope.launch { gymDao.insertExercise(exercise.copy(splitDayId = day.id)) }
                addExerciseTargetDay = null
            }
        )
    }

    logTarget?.let { (exercise, date) ->
        LogSessionDialog(
            exercise = exercise,
            onDismiss = { logTarget = null },
            onConfirm = { session ->
                val previousSessions = exercises.find { it.exercise.id == exercise.id }?.sessions.orEmpty()
                val isPr = isPersonalRecord(exercise, session, previousSessions)
                // Both exercise types feed STR now (AGILITY was dropped) - kept as its own local
                // rather than inlined below since the PR-vs-normal amount below still branches on it.
                val statTag = StatTag.STR
                scope.launch {
                    gymDao.upsertSession(session.copy(exerciseId = exercise.id, date = date.toString()))
                    val xpAmount = if (isPr) 40 else 15
                    val source = if (isPr) "Gym PR: ${exercise.name}" else "Gym: ${exercise.name}"
                    xpEngine.grant(statTag, xpAmount, source)
                }
                logTarget = null
            }
        )
    }

    if (showClearScheduleConfirm) {
        AlertDialog(
            onDismissRequest = { showClearScheduleConfirm = false },
            title = { Text("Clear entire schedule?") },
            text = { Text("This removes every day's assigned workout. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { scheduledWorkoutDao.clear() }
                    showClearScheduleConfirm = false
                }) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearScheduleConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Tapping a Calendar day - shows what was actually logged that date, and lets the user
    // backfill/reschedule by picking a different workout to log against that same date (user
    // feedback, 2026-08-30: "I can also click into the day, and I can manually add a different
    // workout if I want to"). Reuses `logTarget` (already date-generic, not hardcoded to "today")
    // for the actual per-exercise logging, so this gets the exact same PR-check/XP-grant flow.
    dayDetailDate?.let { date ->
        DayDetailDialog(
            date = date,
            exercises = exercises,
            splitDays = splitDays,
            scheduledWorkouts = scheduledWorkouts,
            onLogExercise = { exercise -> logTarget = exercise to date },
            onDeleteSession = { exercise -> scope.launch { gymDao.deleteSession(exercise.id, date.toString()) } },
            onDismiss = { dayDetailDate = null }
        )
    }
}

@Composable
private fun WorkoutsTab(
    modifier: Modifier,
    exercises: List<ExerciseWithSessions>,
    splitDays: List<SplitDay>,
    today: LocalDate,
    editMode: Boolean,
    restLoggedTodaySplitDayId: Long?,
    onEditDay: (SplitDay) -> Unit,
    onDeleteDay: (SplitDay) -> Unit,
    onAddExerciseToDay: (SplitDay) -> Unit,
    onToggleExercise: (Exercise, LocalDate, done: Boolean) -> Unit,
    onDeleteExercise: (Exercise) -> Unit,
    onToggleRestDay: (SplitDay, checked: Boolean) -> Unit
) {
    if (splitDays.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "No workouts yet - tap + to add your first one",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Collapsed by default (user feedback, 2026-09-02: "right now they are all expanded hard to
    // see") - absent from the map reads as collapsed. In-memory only, resets on navigating away,
    // matching this app's other ephemeral per-screen UI state.
    val expandedByDay = remember { mutableStateMapOf<Long, Boolean>() }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        splitDays.forEach { day ->
            // A rest day (user feedback, 2026-09-02) is a SplitDay with no exercises - a flat
            // tickable row instead of a collapsible exercise list. Ticking it records today into
            // rest_day_logs, which colors today on the Calendar with this rest day's color.
            if (day.isRest) {
                item(key = "rest-${day.id}") {
                    RestDayRow(
                        day = day,
                        checkedToday = restLoggedTodaySplitDayId == day.id,
                        editMode = editMode,
                        onToggle = { checked -> onToggleRestDay(day, checked) },
                        onEdit = { onEditDay(day) },
                        onDelete = { onDeleteDay(day) }
                    )
                }
                return@forEach
            }
            val dayExercises = exercises.filter { it.exercise.splitDayId == day.id }
            val expanded = expandedByDay[day.id] ?: false
            item(key = "header-${day.id}") {
                SplitDayHeader(
                    day = day,
                    exerciseCount = dayExercises.size,
                    expanded = expanded,
                    editMode = editMode,
                    onToggleExpand = { expandedByDay[day.id] = !expanded },
                    onAddExercise = { onAddExerciseToDay(day) },
                    onEdit = { onEditDay(day) },
                    onDelete = { onDeleteDay(day) }
                )
            }
            if (expanded) {
                if (dayExercises.isEmpty()) {
                    item(key = "empty-${day.id}") {
                        Text(
                            "No exercises yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                } else {
                    items(dayExercises, key = { it.exercise.id }) { exerciseWithSessions ->
                        val exercise = exerciseWithSessions.exercise
                        val doneToday = exerciseWithSessions.sessions.any { it.date == today.toString() }
                        ExerciseRow(
                            exercise = exercise,
                            done = doneToday,
                            editMode = editMode,
                            onToggle = { onToggleExercise(exercise, today, doneToday) },
                            onDelete = { onDeleteExercise(exercise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitDayHeader(
    day: SplitDay,
    exerciseCount: Int,
    expanded: Boolean,
    editMode: Boolean,
    onToggleExpand: () -> Unit,
    onAddExercise: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse ${day.name}" else "Expand ${day.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier.size(12.dp).background(parseHexColor(day.colorHex), CircleShape)
            )
            Text(day.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "($exerciseCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = onAddExercise) {
                Icon(Icons.Filled.Add, contentDescription = "Add exercise to ${day.name}")
            }
            if (editMode) {
                SubtleIconButton(Icons.Outlined.Edit, "Edit ${day.name}", onClick = onEdit)
                SubtleIconButton(Icons.Outlined.Delete, "Delete ${day.name}", onClick = onDelete)
            }
        }
    }
}

/** A rest day's row on the Workouts tab (user feedback, 2026-09-02) - a flat, non-collapsible
 * row (a rest [SplitDay] has no exercises) with a checkbox that logs/unlogs today into
 * `rest_day_logs`. Same colored dot + name as a workout header, plus a "REST" tag; edit/delete
 * icons only in edit mode, and no "+ add exercise". */
@Composable
private fun RestDayRow(
    day: SplitDay,
    checkedToday: Boolean,
    editMode: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checkedToday, onCheckedChange = { onToggle(!checkedToday) })
            Box(modifier = Modifier.size(12.dp).background(parseHexColor(day.colorHex), CircleShape))
            Text(
                day.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            Surface(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "REST",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (editMode) {
                SubtleIconButton(Icons.Outlined.Edit, "Edit ${day.name}", onClick = onEdit)
                SubtleIconButton(Icons.Outlined.Delete, "Delete ${day.name}", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun CalendarTab(
    modifier: Modifier,
    exercises: List<ExerciseWithSessions>,
    splitDays: List<SplitDay>,
    restDayLogs: List<RestDayLog>,
    onDayClick: (LocalDate) -> Unit
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val workouts = remember(exercises, splitDays, restDayLogs, displayedMonth) {
        workoutCalendarForMonth(displayedMonth, exercises, splitDays, restDayLogs)
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WorkoutMonthCalendar(month = displayedMonth, workoutsByDate = workouts, onDayClick = onDayClick)
                if (splitDays.isNotEmpty()) WorkoutCalendarLegend(splitDays)
            }
        }
    }
}

/** The Gym screen's third tab - a plain weekly plan mapping each weekday to one [SplitDay] via
 * [ScheduledWorkout] (user feedback, 2026-08-30: "basically the gym schedule"). Used to also carry
 * a live "Today" checklist at the top, but that's gone (user feedback, 2026-08-31: "remove the
 * today section from the routine tab, i know what day it is, this is just to show a weekly
 * schedule") - this tab is now purely the plan, with no derived "what's due right now" view; that
 * live checklist already exists elsewhere (the Dashboard's "Coming Up Today" section). Tapping a
 * weekday row always opens a picker to assign/reassign it, regardless of [editMode] - that already
 * covers "edit what workout is on each day" with no mode gate needed. The row's clear icon used to
 * sit behind a long-press instead of a permanent icon; now it's gated by the shared top-bar Edit
 * toggle instead (user feedback, 2026-08-31: "add an edit button for gym routine as well... so i
 * can... delete them"), same always-visible-in-edit-mode pattern the Routine (Schedule/Habits)
 * screen's own Edit button established. */
@Composable
private fun ScheduleTab(
    modifier: Modifier,
    splitDays: List<SplitDay>,
    scheduledWorkouts: List<ScheduledWorkout>,
    editMode: Boolean,
    today: LocalDate,
    onAssign: (dayOfWeek: Int, splitDayId: Long) -> Unit,
    onClearDay: (dayOfWeek: Int) -> Unit
) {
    if (splitDays.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Add a workout on the Workouts tab first, then schedule it here",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        return
    }

    val splitDayById = remember(splitDays) { splitDays.associateBy { it.id } }
    val scheduledByDay = remember(scheduledWorkouts) { scheduledWorkouts.associateBy { it.dayOfWeek } }
    var pickerForDay by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(DayOfWeek.entries.toList(), key = { "day-${it.value}" }) { dow ->
            val workout = scheduledByDay[dow.value]?.let { splitDayById[it.splitDayId] }
            ScheduledDayRow(
                label = dow.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                workout = workout,
                editMode = editMode,
                isToday = dow.value == today.dayOfWeek.value,
                onClick = { pickerForDay = dow.value },
                onClearDay = { onClearDay(dow.value) }
            )
        }
    }

    pickerForDay?.let { dayOfWeek ->
        WorkoutPickerDialog(
            title = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.getDefault()),
            splitDays = splitDays,
            selectedId = scheduledByDay[dayOfWeek]?.splitDayId,
            showClear = scheduledByDay[dayOfWeek] != null,
            onSelect = { splitDayId -> onAssign(dayOfWeek, splitDayId); pickerForDay = null },
            onClear = { onClearDay(dayOfWeek); pickerForDay = null },
            onDismiss = { pickerForDay = null }
        )
    }
}

@Composable
private fun ScheduledDayRow(
    label: String,
    workout: SplitDay?,
    editMode: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    onClearDay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isToday) {
                    Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        CardDefaults.shape
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isToday) {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (workout != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(parseHexColor(workout.colorHex), CircleShape))
                    Text(workout.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (editMode) {
                        SubtleIconButton(Icons.Outlined.Delete, "Clear $label", onClick = onClearDay)
                    }
                }
            } else {
                Text("Rest - tap to schedule", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Shared by the Routine tab's per-day picker and [DayDetailDialog]'s "log a different workout"
 * flow - a plain tappable list of [SplitDay]s, same picker-row pattern already used elsewhere in
 * this app (e.g. the habit picker in `RoutineItemEditorDialog`). */
@Composable
private fun WorkoutPickerDialog(
    title: String,
    splitDays: List<SplitDay>,
    selectedId: Long?,
    showClear: Boolean,
    onSelect: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                splitDays.forEach { day ->
                    Surface(
                        onClick = { onSelect(day.id) },
                        color = if (day.id == selectedId) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(parseHexColor(day.colorHex), CircleShape))
                            Text(day.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                if (showClear) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/** Opened from a tapped Calendar day - shows what was actually logged that date (derived from
 * GymSession, same as the calendar's own coloring) and lets the user pick a workout to log against
 * that same date, backfilling a missed day or logging a different workout than what actually
 * happened without disturbing any other day (user feedback, 2026-08-30). Per-exercise logging
 * reuses [LogSessionDialog] via `onLogExercise`, so PR checks and XP grants stay identical to
 * logging from the Workouts tab - only the date passed through differs. */
@Composable
private fun DayDetailDialog(
    date: LocalDate,
    exercises: List<ExerciseWithSessions>,
    splitDays: List<SplitDay>,
    scheduledWorkouts: List<ScheduledWorkout>,
    onLogExercise: (Exercise) -> Unit,
    onDeleteSession: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val dateStr = remember(date) { date.toString() }
    // Rest days aren't logged from here - they're ticked from the Workouts tab (user feedback,
    // 2026-09-02) - so this picker only offers real workouts.
    val workoutDays = remember(splitDays) { splitDays.filter { !it.isRest } }
    val loggedToday = remember(exercises, dateStr) {
        exercises.mapNotNull { ews -> ews.sessions.find { it.date == dateStr }?.let { ews.exercise to it } }
    }
    // Defaults to whatever's scheduled for this date's weekday (Routine tab), if anything - just
    // a starting point, the user can pick any other workout instead via "Choose a different one".
    var selectedSplitDayId by remember(date) {
        mutableStateOf(scheduledWorkouts.find { it.dayOfWeek == date.dayOfWeek.value }?.splitDayId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Logged", style = MaterialTheme.typography.titleSmall)
                if (loggedToday.isEmpty()) {
                    Text(
                        "Nothing logged this day yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    loggedToday.forEach { (exercise, session) ->
                        LoggedSessionRow(exercise = exercise, session = session, onDelete = { onDeleteSession(exercise) })
                    }
                }

                Text("Log a workout for this day", style = MaterialTheme.typography.titleSmall)
                val selectedDay = workoutDays.find { it.id == selectedSplitDayId }
                if (selectedDay == null) {
                    if (workoutDays.isEmpty()) {
                        Text(
                            "No workouts yet - add one on the Workouts tab first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        workoutDays.forEach { day ->
                            Surface(
                                onClick = { selectedSplitDayId = day.id },
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(modifier = Modifier.size(10.dp).background(parseHexColor(day.colorHex), CircleShape))
                                    Text(day.name)
                                }
                            }
                        }
                    }
                } else {
                    val dayExercises = exercises.filter { it.exercise.splitDayId == selectedDay.id }.map { it.exercise }
                    if (dayExercises.isEmpty()) {
                        Text(
                            "'${selectedDay.name}' has no exercises yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        dayExercises.forEach { exercise ->
                            val alreadyLogged = loggedToday.any { it.first.id == exercise.id }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(exercise.name)
                                if (alreadyLogged) {
                                    Text("Logged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    TextButton(onClick = { onLogExercise(exercise) }) { Text("Log") }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { selectedSplitDayId = null }) { Text("Choose a different workout") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun LoggedSessionRow(exercise: Exercise, session: GymSession, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                when (exercise.type) {
                    ExerciseType.STRENGTH -> listOfNotNull(
                        session.actualSets?.let { s -> session.actualReps?.let { r -> "${s}x$r" } },
                        session.actualWeight?.let { "${cleanNumber(it)}kg" }
                    ).joinToString(" @ ").ifEmpty { "Logged" }
                    ExerciseType.CARDIO_SPORT -> session.actualDuration?.let { "$it min" } ?: "Logged"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SubtleIconButton(Icons.Outlined.Delete, "Delete logged ${exercise.name}", onClick = onDelete)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitDayDialog(
    initial: SplitDay? = null,
    restToggleLocked: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, isRest: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var colorHex by remember {
        mutableStateOf(initial?.colorHex ?: SplitDay.COLOR_PALETTE.first())
    }
    var isRest by remember { mutableStateOf(initial?.isRest ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Workout" else "Edit Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isRest) "Name (e.g. Active Recovery)" else "Name (e.g. Back Day)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // A rest day (user feedback, 2026-09-02) is still a workout in the list - named,
                // colored - but has no exercises and is ticked per date onto the calendar instead
                // of logging sets/reps. Locked once exercises exist (that's delete-and-recreate).
                FilterChip(
                    selected = isRest,
                    onClick = { if (!restToggleLocked) isRest = !isRest },
                    enabled = !restToggleLocked,
                    label = { Text("Rest day (no exercises)") }
                )
                Text("Color:", style = MaterialTheme.typography.labelLarge)
                ColorSwatchPicker(selected = colorHex, onSelect = { colorHex = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), colorHex, isRest) },
                enabled = name.isNotBlank()
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchPicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SplitDay.COLOR_PALETTE.forEach { hex ->
            val color = parseHexColor(hex)
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color.White, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

/** A session is a PR if it beats every previous session logged for the same exercise - Strength
 * compares weight, Cardio/Sport compares duration (spec Section 5.2's "PR vs normal session"
 * XP split). */
private fun isPersonalRecord(exercise: Exercise, session: GymSession, previousSessions: List<GymSession>): Boolean =
    when (exercise.type) {
        ExerciseType.STRENGTH -> {
            val weight = session.actualWeight
            weight != null && previousSessions.all { (it.actualWeight ?: 0.0) < weight }
        }
        ExerciseType.CARDIO_SPORT -> {
            val duration = session.actualDuration
            duration != null && previousSessions.all { (it.actualDuration ?: 0) < duration }
        }
    }

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    done: Boolean,
    editMode: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeChip(exercise.type)
                    Text(
                        targetSummary(exercise),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (editMode) {
                SubtleIconButton(Icons.Outlined.Delete, "Delete exercise", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun TypeChip(type: ExerciseType) {
    val color = if (type == ExerciseType.STRENGTH) SystemRed else SystemYellow
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            // Both types feed STR now (AGILITY was dropped) - label the type itself
            // (Strength vs Cardio) rather than a stat abbreviation that no longer distinguishes them.
            if (type == ExerciseType.STRENGTH) "STRENGTH" else "CARDIO",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun targetSummary(exercise: Exercise): String = when (exercise.type) {
    ExerciseType.STRENGTH -> {
        val setsReps = if (exercise.targetSets != null && exercise.targetReps != null) {
            "${exercise.targetSets}x${exercise.targetReps}"
        } else null
        val weight = exercise.targetWeight?.let { "${cleanNumber(it)}kg" }
        listOfNotNull(setsReps, weight).joinToString(" @ ").ifEmpty { "Strength" }
    }
    ExerciseType.CARDIO_SPORT -> exercise.targetDuration?.let { "$it min" } ?: "Cardio/Sport"
}

private fun cleanNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    splitDayName: String,
    onDismiss: () -> Unit,
    onConfirm: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ExerciseType.STRENGTH) }
    var targetSets by remember { mutableStateOf(3) }
    var targetReps by remember { mutableStateOf(10) }
    var targetWeight by remember { mutableStateOf("") }
    var targetDuration by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Exercise - $splitDayName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == ExerciseType.STRENGTH,
                        onClick = { type = ExerciseType.STRENGTH },
                        label = { Text("Strength") }
                    )
                    FilterChip(
                        selected = type == ExerciseType.CARDIO_SPORT,
                        onClick = { type = ExerciseType.CARDIO_SPORT },
                        label = { Text("Cardio/Sport") }
                    )
                }
                if (type == ExerciseType.STRENGTH) {
                    Stepper(label = "Sets", value = targetSets, onChange = { targetSets = it })
                    Stepper(label = "Reps", value = targetReps, onChange = { targetReps = it })
                    OutlinedTextField(
                        value = targetWeight,
                        onValueChange = { targetWeight = it },
                        label = { Text("Target weight (kg, optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Stepper(label = "Duration (min)", value = targetDuration, onChange = { targetDuration = it }, step = 5, min = 5)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            Exercise(
                                name = name.trim(),
                                splitDayId = 0,
                                type = type,
                                targetSets = if (type == ExerciseType.STRENGTH) targetSets else null,
                                targetReps = if (type == ExerciseType.STRENGTH) targetReps else null,
                                targetWeight = if (type == ExerciseType.STRENGTH) targetWeight.toDoubleOrNull() else null,
                                targetDuration = if (type == ExerciseType.CARDIO_SPORT) targetDuration else null
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    step: Int = 1,
    min: Int = 1,
    max: Int = 999
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value - step >= min) onChange(value - step) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
        }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { if (value + step <= max) onChange(value + step) }) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSessionDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirm: (GymSession) -> Unit
) {
    var sets by remember { mutableStateOf((exercise.targetSets ?: 3).toString()) }
    var reps by remember { mutableStateOf((exercise.targetReps ?: 10).toString()) }
    var weight by remember { mutableStateOf(exercise.targetWeight?.let { cleanNumber(it) } ?: "") }
    var duration by remember { mutableStateOf((exercise.targetDuration ?: 30).toString()) }
    var intensity by remember { mutableStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${exercise.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (exercise.type == ExerciseType.STRENGTH) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sets") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Stepper(label = "Intensity (1-5)", value = intensity, onChange = { intensity = it }, min = 1, max = 5)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    GymSession(
                        exerciseId = exercise.id,
                        date = "",
                        actualSets = if (exercise.type == ExerciseType.STRENGTH) sets.toIntOrNull() else null,
                        actualReps = if (exercise.type == ExerciseType.STRENGTH) reps.toIntOrNull() else null,
                        actualWeight = if (exercise.type == ExerciseType.STRENGTH) weight.toDoubleOrNull() else null,
                        actualDuration = if (exercise.type == ExerciseType.CARDIO_SPORT) duration.toIntOrNull() else null,
                        intensity = if (exercise.type == ExerciseType.CARDIO_SPORT) intensity else null
                    )
                )
            }) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

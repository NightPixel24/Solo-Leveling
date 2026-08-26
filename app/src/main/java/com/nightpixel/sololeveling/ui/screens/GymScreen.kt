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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
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
import com.nightpixel.sololeveling.data.entity.SplitDay
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.gamification.workoutCalendarForMonth
import com.nightpixel.sololeveling.ui.components.StatChip
import com.nightpixel.sololeveling.ui.components.WorkoutCalendarLegend
import com.nightpixel.sololeveling.ui.components.WorkoutMonthCalendar
import com.nightpixel.sololeveling.ui.components.parseHexColor
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private enum class GymTab(val label: String) { ROUTINE("Routine"), CALENDAR("Calendar") }

/** Exercises are grouped under user-defined [SplitDay]s ("Day 1", "Day 2 - Chest and Shoulders",
 * ...) rather than fixed weekdays (user feedback, 2026-08-26: missing a gym day and working out
 * the next day used to push every later exercise onto the wrong weekday header - a split with no
 * calendar day baked into the routine itself can't have that problem). The Calendar tab is the
 * derived record of which split day was actually done on which date, built live from GymSession
 * data (see `data/gamification/WorkoutCalendar.kt`) rather than a second persisted log.
 * Boss Fights (spec Section 5.5) was removed per user feedback (2026-08-26) - see
 * `MIGRATION_15_16`'s doc comment for the data-layer side of the removal. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val gymDao = remember { app.database.gymDao() }
    val splitDayDao = remember { app.database.splitDayDao() }
    val xpEngine = remember { app.xpEngine }
    val goldEngine = remember { app.goldEngine }
    val scope = rememberCoroutineScope()

    val exercises by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val splitDays by splitDayDao.observeSplitDays().collectAsState(initial = emptyList())

    var tab by remember { mutableStateOf(GymTab.ROUTINE) }
    var showAddDayDialog by remember { mutableStateOf(false) }
    var editDayTarget by remember { mutableStateOf<SplitDay?>(null) }
    var deleteDayTarget by remember { mutableStateOf<SplitDay?>(null) }
    var addExerciseTargetDay by remember { mutableStateOf<SplitDay?>(null) }
    var logTarget by remember { mutableStateOf<Pair<Exercise, LocalDate>?>(null) }

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
                        IconButton(onClick = { showAddDayDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add split day")
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
            GymTab.ROUTINE -> RoutineTab(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                exercises = exercises,
                splitDays = splitDays,
                today = today,
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
                onDeleteExercise = { exercise -> scope.launch { gymDao.deleteExercise(exercise) } }
            )
            GymTab.CALENDAR -> CalendarTab(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                exercises = exercises,
                splitDays = splitDays
            )
        }
    }

    if (showAddDayDialog) {
        SplitDayDialog(
            onDismiss = { showAddDayDialog = false },
            onConfirm = { name, colorHex ->
                scope.launch { splitDayDao.insert(SplitDay(name = name, colorHex = colorHex, orderIndex = splitDays.size)) }
                showAddDayDialog = false
            }
        )
    }

    editDayTarget?.let { day ->
        SplitDayDialog(
            initial = day,
            onDismiss = { editDayTarget = null },
            onConfirm = { name, colorHex ->
                scope.launch { splitDayDao.update(day.copy(name = name, colorHex = colorHex)) }
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
                        "This split day has no exercises yet."
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
                    // Spec Section 5.7 - "habits and gym completions grant Gold in addition to
                    // stat XP, e.g. 1 Gold per 10 XP" - derived from the XP just granted above.
                    goldEngine.grantFromXp(xpAmount, source)
                }
                logTarget = null
            }
        )
    }
}

@Composable
private fun RoutineTab(
    modifier: Modifier,
    exercises: List<ExerciseWithSessions>,
    splitDays: List<SplitDay>,
    today: LocalDate,
    onEditDay: (SplitDay) -> Unit,
    onDeleteDay: (SplitDay) -> Unit,
    onAddExerciseToDay: (SplitDay) -> Unit,
    onToggleExercise: (Exercise, LocalDate, done: Boolean) -> Unit,
    onDeleteExercise: (Exercise) -> Unit
) {
    if (splitDays.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "No workout split yet - tap + to add your first day",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        splitDays.forEach { day ->
            val dayExercises = exercises.filter { it.exercise.splitDayId == day.id }
            item(key = "header-${day.id}") {
                SplitDayHeader(
                    day = day,
                    onAddExercise = { onAddExerciseToDay(day) },
                    onEdit = { onEditDay(day) },
                    onDelete = { onDeleteDay(day) }
                )
            }
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
                        onToggle = { onToggleExercise(exercise, today, doneToday) },
                        onDelete = { onDeleteExercise(exercise) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitDayHeader(
    day: SplitDay,
    onAddExercise: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(12.dp).background(parseHexColor(day.colorHex), CircleShape)
            )
            Text(day.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = onAddExercise) {
                Icon(Icons.Filled.Add, contentDescription = "Add exercise to ${day.name}")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${day.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${day.name}")
            }
        }
    }
}

@Composable
private fun CalendarTab(
    modifier: Modifier,
    exercises: List<ExerciseWithSessions>,
    splitDays: List<SplitDay>
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    val workouts = remember(exercises, splitDays, displayedMonth) {
        workoutCalendarForMonth(displayedMonth, exercises, splitDays)
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
                WorkoutMonthCalendar(month = displayedMonth, workoutsByDate = workouts)
                if (splitDays.isNotEmpty()) WorkoutCalendarLegend(splitDays)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitDayDialog(
    initial: SplitDay? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var colorHex by remember {
        mutableStateOf(initial?.colorHex ?: SplitDay.COLOR_PALETTE.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Split Day" else "Edit Split Day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Day 1 - Back and Bi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color:", style = MaterialTheme.typography.labelLarge)
                ColorSwatchPicker(selected = colorHex, onSelect = { colorHex = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), colorHex) },
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
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete exercise")
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

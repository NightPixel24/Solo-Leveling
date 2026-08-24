package com.nightpixel.sololeveling.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.Boss
import com.nightpixel.sololeveling.data.entity.Exercise
import com.nightpixel.sololeveling.data.entity.ExerciseType
import com.nightpixel.sololeveling.data.entity.GymSession
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val gymDao = remember { app.database.gymDao() }
    val bossDao = remember { app.database.bossDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val exercises by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val bosses by bossDao.observeBosses().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddBossDialog by remember { mutableStateOf(false) }
    var logTarget by remember { mutableStateOf<Pair<Exercise, LocalDate>?>(null) }

    val today = remember { LocalDate.now() }
    val weekStart = remember(today) { today.with(DayOfWeek.MONDAY) }
    val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No exercises yet - tap + to build your routine",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val strengthExercises = exercises.filter { it.exercise.type == ExerciseType.STRENGTH }
                if (strengthExercises.isNotEmpty() || bosses.isNotEmpty()) {
                    item(key = "boss-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader("Boss Fights")
                            val canAddBoss = strengthExercises.any { se ->
                                bosses.none { it.exerciseId == se.exercise.id && !it.defeated }
                            }
                            IconButton(onClick = { showAddBossDialog = true }, enabled = canAddBoss) {
                                Icon(Icons.Filled.Add, contentDescription = "Add boss")
                            }
                        }
                    }
                    items(bosses, key = { "boss-${it.id}" }) { boss ->
                        val exerciseWithSessions = strengthExercises.find { it.exercise.id == boss.exerciseId }
                        val bestWeight = exerciseWithSessions?.sessions
                            ?.mapNotNull { it.actualWeight }?.maxOrNull() ?: 0.0
                        BossRow(
                            boss = boss,
                            exerciseName = exerciseWithSessions?.exercise?.name ?: "Unknown exercise",
                            currentBest = bestWeight,
                            onDelete = { scope.launch { bossDao.deleteBoss(boss) } }
                        )
                    }
                }
                weekDays.forEach { day ->
                    val dayExercises = exercises.filter { it.exercise.dayOfWeek == day.dayOfWeek.value }
                    if (dayExercises.isNotEmpty()) {
                        item(key = "header-${day.dayOfWeek}") {
                            SectionHeader(day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                        }
                        items(dayExercises, key = { "${it.exercise.id}-${day}" }) { exerciseWithSessions ->
                            val exercise = exerciseWithSessions.exercise
                            val doneThatDay = exerciseWithSessions.sessions.any { it.date == day.toString() }
                            ExerciseRow(
                                exercise = exercise,
                                done = doneThatDay,
                                onToggle = {
                                    if (doneThatDay) {
                                        scope.launch { gymDao.deleteSession(exercise.id, day.toString()) }
                                    } else {
                                        logTarget = exercise to day
                                    }
                                },
                                onDelete = { scope.launch { gymDao.deleteExercise(exercise) } }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { exercise ->
                scope.launch { gymDao.insertExercise(exercise) }
                showAddDialog = false
            }
        )
    }

    if (showAddBossDialog) {
        val available = exercises
            .filter { it.exercise.type == ExerciseType.STRENGTH }
            .map { it.exercise }
            .filter { ex -> bosses.none { it.exerciseId == ex.id && !it.defeated } }
        AddBossDialog(
            availableExercises = available,
            onDismiss = { showAddBossDialog = false },
            onConfirm = { boss ->
                // A boss can be created against an exercise that already has a logged PR meeting
                // or beating the target (e.g. setting a lower target than your current best just
                // to log the win) - detect that up front rather than only on the next session log,
                // since otherwise it'd sit at 0 HP forever without ever flipping to defeated.
                val currentBest = exercises.find { it.exercise.id == boss.exerciseId }
                    ?.sessions?.mapNotNull { it.actualWeight }?.maxOrNull() ?: 0.0
                val alreadyDefeated = currentBest >= boss.targetWeight
                scope.launch {
                    bossDao.insertBoss(
                        if (alreadyDefeated) boss.copy(defeated = true, defeatedAt = System.currentTimeMillis())
                        else boss
                    )
                    if (alreadyDefeated) {
                        xpEngine.grant(StatTag.STR, 50, "Boss defeated: ${boss.name}")
                    }
                }
                showAddBossDialog = false
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
                val statTag = if (exercise.type == ExerciseType.STRENGTH) StatTag.STR else StatTag.AGILITY
                val newlyDefeatedBoss = session.actualWeight?.let { weight ->
                    bosses.find { it.exerciseId == exercise.id && !it.defeated && weight >= it.targetWeight }
                }
                scope.launch {
                    gymDao.upsertSession(session.copy(exerciseId = exercise.id, date = date.toString()))
                    xpEngine.grant(
                        statTag,
                        if (isPr) 40 else 15,
                        if (isPr) "Gym PR: ${exercise.name}" else "Gym: ${exercise.name}"
                    )
                    newlyDefeatedBoss?.let { boss ->
                        bossDao.updateBoss(boss.copy(defeated = true, defeatedAt = System.currentTimeMillis()))
                        // Spec Section 5.5 - defeating a boss grants "a bonus reward"; no amount
                        // given, so this is this app's own tuned value, same as other examples.
                        xpEngine.grant(StatTag.STR, 50, "Boss defeated: ${boss.name}")
                    }
                }
                logTarget = null
            }
        )
    }
}

/** A session is a PR if it beats every previous session logged for the same exercise - Strength
 * compares weight, Cardio/Sport compares duration (spec Section 5.2's "PR vs normal session"
 * XP split). Full Boss-Fight-style PR tracking with a running target/HP is Phase 12 scope; this
 * is just the simpler "is this the best I've logged" check needed to pick an XP amount now. */
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
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
private fun BossRow(boss: Boss, exerciseName: String, currentBest: Double, onDelete: () -> Unit) {
    val hpRemaining = (boss.targetWeight - currentBest).coerceAtLeast(0.0)
    val progress = if (boss.targetWeight > 0) (currentBest / boss.targetWeight).toFloat().coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(boss.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        exerciseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (boss.defeated) {
                    Text(
                        "Defeated!",
                        color = SystemGreen,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                // Deleting a defeated boss doesn't undo its already-granted XP reward - the
                // "permanent trophy" is that reward, not the card itself, so removing the card
                // to declutter is always allowed.
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete boss")
                }
            }
            if (!boss.defeated) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(
                    "${cleanNumber(currentBest)}kg / ${cleanNumber(boss.targetWeight)}kg" +
                        " - ${cleanNumber(hpRemaining)}kg to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddBossDialog(
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (Boss) -> Unit
) {
    var selectedExercise by remember { mutableStateOf(availableExercises.firstOrNull()) }
    var name by remember { mutableStateOf(selectedExercise?.let { "${it.name} Boss" } ?: "") }
    var targetWeight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Boss") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Exercise:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableExercises.forEach { ex ->
                        FilterChip(
                            selected = selectedExercise?.id == ex.id,
                            onClick = {
                                selectedExercise = ex
                                name = "${ex.name} Boss"
                            },
                            label = { Text(ex.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Boss name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    label = { Text("Target weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val exercise = selectedExercise
                    val weight = targetWeight.toDoubleOrNull()
                    if (exercise != null && weight != null && weight > 0 && name.isNotBlank()) {
                        onConfirm(Boss(exerciseId = exercise.id, name = name.trim(), targetWeight = weight))
                    }
                },
                enabled = selectedExercise != null && (targetWeight.toDoubleOrNull() ?: 0.0) > 0 && name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
            if (type == ExerciseType.STRENGTH) "STR" else "AGILITY",
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var type by remember { mutableStateOf(ExerciseType.STRENGTH) }
    var targetSets by remember { mutableStateOf(3) }
    var targetReps by remember { mutableStateOf(10) }
    var targetWeight by remember { mutableStateOf("") }
    var targetDuration by remember { mutableStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Day:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = dayOfWeek == day,
                            onClick = { dayOfWeek = day },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                        )
                    }
                }
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
                                dayOfWeek = dayOfWeek.value,
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

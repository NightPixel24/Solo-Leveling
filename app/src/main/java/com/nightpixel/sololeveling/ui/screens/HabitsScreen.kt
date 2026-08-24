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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.dao.HabitDao
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitLog
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemVioletBright
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val habitDao = remember { app.database.habitDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val habits by habitDao.observeHabitsWithLogs().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habits") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add habit")
                    }
                }
            )
        }
    ) { innerPadding ->
        val daily = habits.filter { it.habit.frequency == HabitFrequency.DAILY }
        val weekly = habits.filter { it.habit.frequency == HabitFrequency.WEEKLY }

        if (habits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No habits yet - tap + to add one",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (daily.isNotEmpty()) {
                    item { SectionHeader("Daily") }
                    items(daily, key = { it.habit.id }) { habitWithLogs ->
                        HabitRow(
                            habitWithLogs = habitWithLogs,
                            today = today,
                            onToggleToday = { toggleToday(habitWithLogs, today, habitDao, xpEngine, scope) },
                            onDelete = { scope.launch { habitDao.deleteHabit(habitWithLogs.habit) } }
                        )
                    }
                }
                if (weekly.isNotEmpty()) {
                    item { SectionHeader("Weekly") }
                    items(weekly, key = { it.habit.id }) { habitWithLogs ->
                        HabitRow(
                            habitWithLogs = habitWithLogs,
                            today = today,
                            onToggleToday = { toggleToday(habitWithLogs, today, habitDao, xpEngine, scope) },
                            onDelete = { scope.launch { habitDao.deleteHabit(habitWithLogs.habit) } }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { habit ->
                scope.launch { habitDao.insertHabit(habit) }
                showAddDialog = false
            }
        )
    }
}

private fun toggleToday(
    habitWithLogs: HabitWithLogs,
    today: LocalDate,
    habitDao: HabitDao,
    xpEngine: XpEngine,
    scope: CoroutineScope
) {
    val habit = habitWithLogs.habit
    val todayStr = today.toString()
    val doneToday = habitWithLogs.logs.any { it.date == todayStr && it.done }
    scope.launch {
        if (doneToday) {
            habitDao.deleteLog(habit.id, todayStr)
        } else {
            habitDao.upsertLog(HabitLog(habitId = habit.id, date = todayStr))
            xpEngine.grant(habit.statTag, 10, "Habit: ${habit.title}")
        }
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
private fun HabitRow(
    habitWithLogs: HabitWithLogs,
    today: LocalDate,
    onToggleToday: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = habitWithLogs.habit
    val doneDates = remember(habitWithLogs.logs) {
        habitWithLogs.logs.filter { it.done }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
    }
    val doneToday = today in doneDates

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = doneToday, onCheckedChange = { onToggleToday() })
            Column(Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(habit.statTag)
                    if (habit.frequency == HabitFrequency.WEEKLY) {
                        val weekCount = doneDates.count { weekStart(it) == weekStart(today) }
                        Text(
                            "$weekCount/${habit.targetPerWeek} this week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val streak = if (habit.frequency == HabitFrequency.DAILY) {
                        dailyStreak(doneDates, today)
                    } else {
                        weeklyStreak(doneDates, habit.targetPerWeek, today)
                    }
                    if (streak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = SystemYellow,
                                modifier = Modifier.width(16.dp)
                            )
                            Text(
                                "$streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete habit")
            }
        }
    }
}

@Composable
private fun StatChip(tag: StatTag) {
    val color = when (tag) {
        StatTag.STR -> SystemRed
        StatTag.VIT -> SystemGreen
        StatTag.DISCIPLINE -> SystemBlue
        StatTag.INT -> SystemVioletBright
        StatTag.AGILITY -> SystemYellow
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            tag.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (Habit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(HabitFrequency.DAILY) }
    var targetPerWeek by remember { mutableStateOf(3) }
    var statTag by remember { mutableStateOf(StatTag.DISCIPLINE) }
    var reminderTime by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitFrequency.entries.forEach { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                if (frequency == HabitFrequency.WEEKLY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Target per week:", modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (targetPerWeek > 1) targetPerWeek-- }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease target")
                        }
                        Text("$targetPerWeek", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { if (targetPerWeek < 7) targetPerWeek++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase target")
                        }
                    }
                }
                Text("Feeds stat:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatTag.entries.forEach { tag ->
                        FilterChip(
                            selected = statTag == tag,
                            onClick = { statTag = tag },
                            label = { Text(tag.name) }
                        )
                    }
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(reminderTime?.let { formatMinutes(it) } ?: "Set reminder (optional)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            Habit(
                                title = title.trim(),
                                frequency = frequency,
                                targetPerWeek = targetPerWeek,
                                statTag = statTag,
                                reminderTime = reminderTime
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showTimePicker) {
        val initial = reminderTime ?: (8 * 60)
        val timePickerState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = false
        )
        Dialog(onDismissRequest = { showTimePicker = false }, properties = DialogProperties()) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { reminderTime = null; showTimePicker = false }) {
                            Text("Clear")
                        }
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Button(onClick = {
                            reminderTime = timePickerState.hour * 60 + timePickerState.minute
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

private fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

/** Spec Section 5.4 - "once per week, a missed daily habit doesn't break its streak." No separate
 * freeze-usage table: a gap is forgiven the moment it's encountered as long as the last forgiven
 * gap (if any) was 7+ days back, which is equivalent to "one freeze per rolling week" without
 * needing to persist anything - the streak is still computed purely from HabitLog. A forgiven day
 * doesn't add to the streak count (the habit wasn't actually done that day), it just doesn't break
 * the chain of days before it. */
private fun dailyStreak(doneDates: Set<LocalDate>, today: LocalDate): Int {
    var streak = 0
    var day = if (today in doneDates) today else today.minusDays(1)
    var lastFreezeDate: LocalDate? = null
    while (true) {
        if (day in doneDates) {
            streak++
            day = day.minusDays(1)
        } else {
            val freezeAvailable = lastFreezeDate == null ||
                ChronoUnit.DAYS.between(day, lastFreezeDate) >= 7
            if (!freezeAvailable) break
            lastFreezeDate = day
            day = day.minusDays(1)
        }
    }
    return streak
}

private fun weeklyStreak(doneDates: Set<LocalDate>, target: Int, today: LocalDate): Int {
    val counts = doneDates.groupingBy { weekStart(it) }.eachCount()
    var streak = 0
    var week = weekStart(today)
    if ((counts[week] ?: 0) < target) week = week.minusWeeks(1)
    while ((counts[week] ?: 0) >= target) {
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}

private fun formatMinutes(minutes: Int): String {
    val time = LocalTime.of(minutes / 60, minutes % 60)
    return time.format(DateTimeFormatter.ofPattern("h:mm a"))
}

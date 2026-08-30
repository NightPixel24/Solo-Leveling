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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.dao.HabitDao
import com.nightpixel.sololeveling.data.entity.DayPart
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.RoutineItem
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.ui.components.StatChip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class RoutineTab(val label: String) { SCHEDULE("Schedule"), HABITS("Habits") }

/** The renamed Habits bottom-nav slot (user feedback, 2026-08-30: "make this a routine tab...
 * I also want it to open on a schedule screen first"). Schedule is the default sub-tab - a
 * day-planner grouped into [DayPart] blocks where the user can drop in either a free-text plan
 * item or an existing habit (see [RoutineItem]'s doc comment for why both share one table); Habits
 * is the original habit-list screen, now embedded here instead of owning its own bottom-nav slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val routineDao = remember { app.database.routineDao() }
    val habitDao = remember { app.database.habitDao() }
    val foodDao = remember { app.database.foodDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val items by routineDao.observeItems().collectAsState(initial = emptyList())
    val habitsWithLogs by habitDao.observeHabitsWithLogs().collectAsState(initial = emptyList())
    val habitsById = remember(habitsWithLogs) { habitsWithLogs.associateBy { it.habit.id } }
    val today = remember { LocalDate.now() }

    var tab by remember { mutableStateOf(RoutineTab.SCHEDULE) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Routine") },
                    actions = {
                        IconButton(
                            onClick = {
                                if (tab == RoutineTab.SCHEDULE) showAddItemDialog = true else showAddHabitDialog = true
                            }
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = if (tab == RoutineTab.SCHEDULE) "Add routine item" else "Add habit"
                            )
                        }
                    }
                )
                TabRow(selectedTabIndex = tab.ordinal) {
                    RoutineTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                    }
                }
            }
        }
    ) { innerPadding ->
        when (tab) {
            RoutineTab.SCHEDULE -> ScheduleTab(
                modifier = Modifier.padding(innerPadding),
                items = items,
                habitsById = habitsById,
                today = today,
                habitDao = habitDao,
                foodDao = foodDao,
                xpEngine = xpEngine,
                scope = scope,
                onDelete = { item -> scope.launch { routineDao.delete(item) } }
            )
            RoutineTab.HABITS -> HabitsTab(
                modifier = Modifier.padding(innerPadding),
                showAddDialog = showAddHabitDialog,
                onDismissAddDialog = { showAddHabitDialog = false }
            )
        }
    }

    if (showAddItemDialog) {
        val scheduledHabitIds = remember(items) { items.mapNotNull { it.habitId }.toSet() }
        AddRoutineItemDialog(
            habits = habitsWithLogs.map { it.habit }.filter { it.id !in scheduledHabitIds },
            onDismiss = { showAddItemDialog = false },
            onConfirm = { item ->
                scope.launch { routineDao.insert(item) }
                showAddItemDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleTab(
    modifier: Modifier,
    items: List<RoutineItem>,
    habitsById: Map<Long, HabitWithLogs>,
    today: LocalDate,
    habitDao: HabitDao,
    foodDao: FoodDao,
    xpEngine: XpEngine,
    scope: CoroutineScope,
    onDelete: (RoutineItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No routine items yet - tap + to plan your day",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val byPart = remember(items) { items.groupBy { it.dayPart } }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DayPart.entries.forEach { part ->
            val partItems = byPart[part].orEmpty()
            if (partItems.isNotEmpty()) {
                item { SectionHeader(part.label) }
                items(partItems, key = { it.id }) { item ->
                    RoutineItemRow(
                        item = item,
                        habitWithLogs = item.habitId?.let { habitsById[it] },
                        today = today,
                        onToggleHabit = { hwl ->
                            toggleToday(hwl, today, habitDao, foodDao, xpEngine, scope)
                        },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineItemRow(
    item: RoutineItem,
    habitWithLogs: HabitWithLogs?,
    today: LocalDate,
    onToggleHabit: (HabitWithLogs) -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (habitWithLogs != null) {
                val doneToday = habitWithLogs.logs.any { it.date == today.toString() && it.done }
                Checkbox(checked = doneToday, onCheckedChange = { onToggleHabit(habitWithLogs) })
                Column(Modifier.weight(1f)) {
                    Text(habitWithLogs.habit.title, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatChip(habitWithLogs.habit.statTag)
                        item.reminderTime?.let { ReminderTimeLabel(it) }
                    }
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    item.reminderTime?.let { ReminderTimeLabel(it) }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove from schedule")
            }
        }
    }
}

@Composable
private fun ReminderTimeLabel(reminderTime: Int) {
    Text(
        formatMinutes(reminderTime),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private enum class RoutineItemMode { CUSTOM, HABIT }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddRoutineItemDialog(
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onConfirm: (RoutineItem) -> Unit
) {
    var dayPart by remember { mutableStateOf(DayPart.MORNING) }
    var mode by remember { mutableStateOf(RoutineItemMode.CUSTOM) }
    var title by remember { mutableStateOf("") }
    var selectedHabitId by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("When:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DayPart.entries.forEach { part ->
                        FilterChip(
                            selected = dayPart == part,
                            onClick = { dayPart = part },
                            label = { Text(part.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == RoutineItemMode.CUSTOM,
                        onClick = { mode = RoutineItemMode.CUSTOM },
                        label = { Text("Custom item") }
                    )
                    FilterChip(
                        selected = mode == RoutineItemMode.HABIT,
                        onClick = { mode = RoutineItemMode.HABIT },
                        label = { Text("Existing habit") }
                    )
                }
                if (mode == RoutineItemMode.CUSTOM) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What's happening") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (habits.isEmpty()) {
                    Text(
                        "No unscheduled habits left to add.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        habits.forEach { habit ->
                            val selected = selectedHabitId == habit.id
                            Surface(
                                onClick = { selectedHabitId = habit.id },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    habit.title,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                // Optional, unlike the day-part slot itself - most items are just "sometime in
                // the morning," but a time-specific one (user's own example: "at nine PM, take my
                // tablets") gets a real notification at that exact time via RoutineReminderWorker.
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(reminderTime?.let { formatMinutes(it) } ?: "Set a specific time (optional)")
                }
            }
        },
        confirmButton = {
            val enabled = if (mode == RoutineItemMode.CUSTOM) title.isNotBlank() else selectedHabitId != null
            TextButton(
                onClick = {
                    val item = if (mode == RoutineItemMode.CUSTOM) {
                        RoutineItem(dayPart = dayPart, title = title.trim(), reminderTime = reminderTime)
                    } else {
                        RoutineItem(dayPart = dayPart, habitId = selectedHabitId, reminderTime = reminderTime)
                    }
                    onConfirm(item)
                },
                enabled = enabled
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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

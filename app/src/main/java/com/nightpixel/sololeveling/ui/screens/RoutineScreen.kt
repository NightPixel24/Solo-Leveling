package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.dao.HabitDao
import com.nightpixel.sololeveling.data.entity.DayPart
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.RoutineItem
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.ui.components.StatChip
import com.nightpixel.sololeveling.ui.components.SubtleIcon
import com.nightpixel.sololeveling.ui.components.SubtleIconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private enum class RoutineTab(val label: String) { SCHEDULE("Schedule"), HABITS("Habits") }

/** The renamed Habits bottom-nav slot (user feedback, 2026-08-30: "make this a routine tab...
 * I also want it to open on a schedule screen first"). Schedule is the default sub-tab - a
 * day-planner grouped into [DayPart] blocks where the user can drop in either a free-text plan
 * item or an existing habit (see [RoutineItem]'s doc comment for why both share one table); Habits
 * is the original habit-list screen, now embedded here instead of owning its own bottom-nav slot.
 * A top-bar Edit toggle (user feedback, 2026-08-31: "next to schedule have an edit button") is
 * shared across both sub-tabs but reads differently depending which is active - on Schedule it
 * reveals a drag handle + always-visible delete icon per item (replacing the old long-press-reveal
 * pattern) and makes tapping a row open its editor; on Habits it does the same minus the drag
 * handle (habits aren't reordered, just deleted/edited). One shared boolean rather than a
 * per-tab flag - the button's own label/behavior already reads as "edit mode for whatever you're
 * looking at," and there's no scenario where a user would want it on for one tab but not the other
 * mid-session. */
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
    var editMode by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<RoutineItem?>(null) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Routine") },
                    actions = {
                        IconButton(onClick = { editMode = !editMode }) {
                            Icon(
                                if (editMode) Icons.Filled.Check else Icons.Outlined.Edit,
                                contentDescription = if (editMode) "Done editing" else "Edit"
                            )
                        }
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
                editMode = editMode,
                habitDao = habitDao,
                foodDao = foodDao,
                xpEngine = xpEngine,
                scope = scope,
                onDelete = { item -> scope.launch { routineDao.delete(item) } },
                onToggleCustom = { item, done ->
                    scope.launch { routineDao.setCompletedDate(item.id, if (done) today.toString() else null) }
                },
                onEditItem = { item -> editingItem = item },
                onReorder = { newOrder ->
                    scope.launch {
                        newOrder.forEachIndexed { index, item ->
                            if (item.position != index) routineDao.update(item.copy(position = index))
                        }
                    }
                }
            )
            RoutineTab.HABITS -> HabitsTab(
                modifier = Modifier.padding(innerPadding),
                editMode = editMode,
                showAddDialog = showAddHabitDialog,
                onDismissAddDialog = { showAddHabitDialog = false },
                editingHabit = editingHabit,
                onEditHabit = { habit -> editingHabit = habit },
                onDismissEditDialog = { editingHabit = null }
            )
        }
    }

    if (showAddItemDialog || editingItem != null) {
        val scheduledHabitIds = remember(items) { items.mapNotNull { it.habitId }.toSet() }
        RoutineItemEditorDialog(
            habits = habitsWithLogs.map { it.habit }.filter { it.id !in scheduledHabitIds },
            habitsById = habitsById,
            existing = editingItem,
            onDismiss = { showAddItemDialog = false; editingItem = null },
            onConfirm = { item ->
                // Decide insert-vs-update from the item's own id, not the `editingItem` state var
                // - a real crash caught in on-device testing (SQLiteConstraintException: UNIQUE
                // constraint failed: routine_items.id): `editingItem = null` right below runs
                // synchronously, but `scope.launch`'s body only reads `editingItem` once the
                // coroutine actually gets dispatched - by then it had already been cleared, so an
                // edit's confirm always fell through to `insert`, trying to insert a second row
                // with the same (already-existing) id. `item.id` itself isn't racy - it's plain
                // data captured by value in this lambda's closure, unaffected by the state reset.
                val isEdit = item.id != 0L
                scope.launch {
                    if (isEdit) {
                        routineDao.update(item)
                    } else {
                        val position = items.count { it.dayPart == item.dayPart }
                        routineDao.insert(item.copy(position = position))
                    }
                }
                showAddItemDialog = false
                editingItem = null
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
    editMode: Boolean,
    habitDao: HabitDao,
    foodDao: FoodDao,
    xpEngine: XpEngine,
    scope: CoroutineScope,
    onDelete: (RoutineItem) -> Unit,
    onToggleCustom: (RoutineItem, Boolean) -> Unit,
    onEditItem: (RoutineItem) -> Unit,
    onReorder: (List<RoutineItem>) -> Unit
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
                item(key = "header_${part.name}") { SectionHeader(part.label) }
                // A plain (non-lazy) section per day part rather than folding partItems into the
                // outer LazyColumn's own items() - drag-to-reorder needs every row in the group
                // actually measured/composed at once to compute swap thresholds from real row
                // heights, and a day part's item count is small enough that losing virtualization
                // here doesn't matter.
                item(key = "section_${part.name}") {
                    ReorderableDayPartSection(
                        items = partItems,
                        habitsById = habitsById,
                        today = today,
                        editMode = editMode,
                        onToggleHabit = { hwl -> toggleToday(hwl, today, habitDao, foodDao, xpEngine, scope) },
                        onToggleCustom = onToggleCustom,
                        onDelete = onDelete,
                        onEditItem = onEditItem,
                        onReorder = onReorder
                    )
                }
            }
        }
    }
}

/** Drag-to-reorder within one day part group (user feedback, 2026-08-31: "have a drag bar on the
 * left side so I can move the scheduled items around instead of being locked into place"). Hand-
 * rolled rather than a third-party reorder library, matching this codebase's existing preference
 * for small from-scratch Compose pieces (RadarChart, LineChart) over new dependencies for a single
 * use. [order] mirrors [items] but is dragged/swapped locally frame-by-frame - real DB positions
 * are only written once via [onReorder] on drag end, so mid-drag frames don't spam the DB. */
@Composable
private fun ReorderableDayPartSection(
    items: List<RoutineItem>,
    habitsById: Map<Long, HabitWithLogs>,
    today: LocalDate,
    editMode: Boolean,
    onToggleHabit: (HabitWithLogs) -> Unit,
    onToggleCustom: (RoutineItem, Boolean) -> Unit,
    onDelete: (RoutineItem) -> Unit,
    onEditItem: (RoutineItem) -> Unit,
    onReorder: (List<RoutineItem>) -> Unit
) {
    // Resets whenever `items` itself changes identity - an add/delete/edit elsewhere, or the Flow
    // echoing back a just-committed reorder. Not reset by our own local swaps below, since those
    // only reassign this state var, never the `items` parameter.
    var order by remember(items) { mutableStateOf(items) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        order.forEach { item ->
            // Keyed by item.id (user feedback, 2026-08-31, drag bug found in on-device testing):
            // a plain forEach with no key() matches composable slots by call POSITION, not by
            // which item occupies them - so mid-swap, the slot that held item A gets handed item
            // B's data, which restarts anything keyed off item.id (including this row's own
            // pointerInput drag coroutine) and silently kills the in-flight gesture before
            // onDragEnd ever fires, so the reorder never actually reaches the DB even though the
            // on-screen order looks right.
            key(item.id) {
                val isDragging = editMode && item.id == draggingId
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                        .onSizeChanged { itemHeights[item.id] = it.height }
                ) {
                    RoutineItemRow(
                        item = item,
                        habitWithLogs = item.habitId?.let { habitsById[it] },
                        today = today,
                        editMode = editMode,
                        onToggleHabit = onToggleHabit,
                        onToggleCustom = { done -> onToggleCustom(item, done) },
                        onDelete = { onDelete(item) },
                        onEditClick = { onEditItem(item) },
                        dragHandleModifier = Modifier.pointerInput(item.id) {
                            detectVerticalDragGestures(
                                onDragStart = { draggingId = item.id; dragOffset = 0f },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = 0f
                                    if (order != items) onReorder(order)
                                },
                                onDragCancel = { draggingId = null; dragOffset = 0f },
                                onVerticalDrag = { change, delta ->
                                    change.consume()
                                    dragOffset += delta
                                    val currentIndex = order.indexOfFirst { it.id == item.id }
                                    if (currentIndex == -1) return@detectVerticalDragGestures
                                    if (dragOffset > 0 && currentIndex < order.size - 1) {
                                        val nextHeight = itemHeights[order[currentIndex + 1].id]
                                        if (nextHeight != null && dragOffset > nextHeight / 2f) {
                                            order = order.toMutableList().apply {
                                                add(currentIndex, removeAt(currentIndex + 1))
                                            }
                                            dragOffset -= nextHeight
                                        }
                                    } else if (dragOffset < 0 && currentIndex > 0) {
                                        val prevHeight = itemHeights[order[currentIndex - 1].id]
                                        if (prevHeight != null && -dragOffset > prevHeight / 2f) {
                                            order = order.toMutableList().apply {
                                                add(currentIndex, removeAt(currentIndex - 1))
                                            }
                                            dragOffset += prevHeight
                                        }
                                    }
                                }
                            )
                        }
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
    editMode: Boolean,
    onToggleHabit: (HabitWithLogs) -> Unit,
    onToggleCustom: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit,
    dragHandleModifier: Modifier
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (editMode) {
                Box(
                    modifier = dragHandleModifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SubtleIcon(Icons.Filled.DragIndicator, "Drag to reorder")
                }
            }
            val contentModifier = Modifier
                .weight(1f)
                .then(if (editMode) Modifier.clickable(onClick = onEditClick) else Modifier)
            if (habitWithLogs != null) {
                val doneToday = habitWithLogs.logs.any { it.date == today.toString() && it.done }
                Checkbox(checked = doneToday, onCheckedChange = { onToggleHabit(habitWithLogs) })
                Column(contentModifier) {
                    Text(habitWithLogs.habit.title, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatChip(habitWithLogs.habit.statTag)
                        item.reminderTime?.let { ReminderTimeLabel(it) }
                    }
                }
            } else {
                // A free-text item has no HabitLog to borrow, so its own checkbox reads/writes
                // RoutineItem.completedDate directly (user feedback, 2026-08-31: "you didnt make
                // the items checkboxes" - previously only habit-linked items were checkable).
                val doneToday = item.completedDate == today.toString()
                Checkbox(checked = doneToday, onCheckedChange = { checked -> onToggleCustom(checked) })
                Column(contentModifier) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    item.reminderTime?.let { ReminderTimeLabel(it) }
                }
            }
            if (editMode) {
                SubtleIconButton(Icons.Outlined.Delete, "Remove from schedule", onClick = onDelete)
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

/** Add-or-edit for one Schedule item (edit dialog generalized from the add-only original, same
 * "TaskEditorDialog" precedent this codebase already used for Tasks - user feedback, 2026-08-31:
 * "when i click on an item i can edit the fields and name"). [existing] null means Add; non-null
 * means Edit, in which case the CUSTOM/HABIT mode toggle is hidden and locked to whatever shape
 * the item already is - switching a slotted habit into a free-text item (or back) isn't a coherent
 * "edit," it's really delete-and-recreate, which the row's own trash icon + the Add flow already
 * cover. A habit-linked item's [habits] list has already excluded it (it's "already scheduled"),
 * so its title for display is looked up from [habitsById] instead. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoutineItemEditorDialog(
    habits: List<Habit>,
    habitsById: Map<Long, HabitWithLogs>,
    existing: RoutineItem?,
    onDismiss: () -> Unit,
    onConfirm: (RoutineItem) -> Unit
) {
    val editingHabitLinked = existing?.habitId != null
    var dayPart by remember { mutableStateOf(existing?.dayPart ?: DayPart.MORNING) }
    var mode by remember { mutableStateOf(if (editingHabitLinked) RoutineItemMode.HABIT else RoutineItemMode.CUSTOM) }
    var title by remember { mutableStateOf(if (existing != null && !editingHabitLinked) existing.title else "") }
    var selectedHabitId by remember { mutableStateOf(existing?.habitId) }
    var reminderTime by remember { mutableStateOf(existing?.reminderTime) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add to Schedule" else "Edit Schedule Item") },
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
                if (existing == null) {
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
                }
                if (mode == RoutineItemMode.CUSTOM) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What's happening") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (existing != null) {
                    Text(
                        "Habit: ${habitsById[selectedHabitId]?.habit?.title ?: ""}",
                        style = MaterialTheme.typography.bodyLarge
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
                    val item = when {
                        existing != null && mode == RoutineItemMode.CUSTOM ->
                            existing.copy(dayPart = dayPart, title = title.trim(), reminderTime = reminderTime)
                        existing != null ->
                            existing.copy(dayPart = dayPart, reminderTime = reminderTime)
                        mode == RoutineItemMode.CUSTOM ->
                            RoutineItem(dayPart = dayPart, title = title.trim(), reminderTime = reminderTime)
                        else ->
                            RoutineItem(dayPart = dayPart, habitId = selectedHabitId, reminderTime = reminderTime)
                    }
                    onConfirm(item)
                },
                enabled = enabled
            ) { Text(if (existing == null) "Add" else "Save") }
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

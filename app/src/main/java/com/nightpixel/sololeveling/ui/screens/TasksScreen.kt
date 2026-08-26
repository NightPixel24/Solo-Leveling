package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.dao.TaskDao
import com.nightpixel.sololeveling.data.entity.Priority
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import com.nightpixel.sololeveling.data.entity.TaskWithSubtasks
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val taskDao = remember { app.database.taskDao() }
    val taskListDao = remember { app.database.taskListDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val lists by taskListDao.observeLists().collectAsState(initial = emptyList())
    val pagerState = rememberPagerState(pageCount = { lists.size })
    var pendingFocusListId by remember { mutableStateOf<Long?>(null) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // A newly added list isn't in `lists` yet on the frame it's created, so
    // park the target id and jump the pager to it once the Flow catches up.
    LaunchedEffect(lists, pendingFocusListId) {
        val targetId = pendingFocusListId ?: return@LaunchedEffect
        val index = lists.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            pagerState.scrollToPage(index)
            pendingFocusListId = null
        }
    }

    val currentList = lists.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { showAddListDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add list")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentList != null) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add task")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TaskListTabRow(
                lists = lists,
                selectedIndex = pagerState.currentPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                onRename = { list, newName -> scope.launch { taskListDao.updateList(list.copy(name = newName)) } },
                onDelete = { list -> scope.launch { taskListDao.deleteListCascading(list.id) } },
                canDelete = lists.size > 1
            )

            if (lists.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No lists yet - tap + to add one",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) { page ->
                    TaskListContent(
                        listId = lists[page].id,
                        taskDao = taskDao,
                        xpEngine = xpEngine,
                        scope = scope,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showAddListDialog) {
        ListNameDialog(
            initialName = "",
            title = "New List",
            onDismiss = { showAddListDialog = false },
            onConfirm = { name ->
                scope.launch { pendingFocusListId = taskListDao.insertList(TaskList(name = name, position = lists.size)) }
                showAddListDialog = false
            }
        )
    }

    if (showAddTaskDialog && currentList != null) {
        TaskEditorDialog(
            dialogTitle = "New Task",
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, dueDate, priority, notes ->
                scope.launch {
                    taskDao.insertTask(
                        Task(listId = currentList.id, title = title, dueDate = dueDate, priority = priority, notes = notes)
                    )
                }
                showAddTaskDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListTabRow(
    lists: List<TaskList>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onRename: (TaskList, String) -> Unit,
    onDelete: (TaskList) -> Unit,
    canDelete: Boolean
) {
    var menuTarget by remember { mutableStateOf<TaskList?>(null) }
    var renameTarget by remember { mutableStateOf<TaskList?>(null) }
    val tabRowState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) tabRowState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = tabRowState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(lists, key = { _, list -> list.id }) { index, list ->
            val selected = index == selectedIndex
            Box {
                Surface(
                    modifier = Modifier.combinedClickable(
                        onClick = { onSelect(index) },
                        onLongClick = { menuTarget = list }
                    ),
                    color = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Text(
                        list.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuTarget?.id == list.id,
                    onDismissRequest = { menuTarget = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuTarget = null; renameTarget = list }
                    )
                    DropdownMenuItem(
                        text = { Text(if (canDelete) "Delete list" else "Can't delete last list") },
                        enabled = canDelete,
                        onClick = { menuTarget = null; onDelete(list) }
                    )
                }
            }
        }
    }

    renameTarget?.let { list ->
        ListNameDialog(
            initialName = list.name,
            title = "Rename List",
            onDismiss = { renameTarget = null },
            onConfirm = { newName -> onRename(list, newName); renameTarget = null }
        )
    }
}

@Composable
private fun TaskListContent(
    listId: Long,
    taskDao: TaskDao,
    xpEngine: XpEngine,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val tasks by taskDao.observeTasksForList(listId).collectAsState(initial = emptyList())
    var editingTask by remember { mutableStateOf<Task?>(null) }

    if (tasks.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "No tasks yet - tap + to add one",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.task.id }) { taskWithSubtasks ->
                TaskCard(
                    taskWithSubtasks = taskWithSubtasks,
                    onEdit = { editingTask = taskWithSubtasks.task },
                    onToggleDone = {
                        val task = taskWithSubtasks.task
                        val nowDone = !task.isDone
                        scope.launch {
                            taskDao.updateTask(task.copy(isDone = nowDone))
                            if (nowDone) {
                                xpEngine.grant(StatTag.DISCIPLINE, 5, "Task: ${task.title}")
                                // Spec Section 5.4 - "your Task list, reframed" as Side Quests;
                                // completing one grants a small bonus on top of normal task XP.
                                // No spec-given amount, so this is this app's own tuned value.
                                xpEngine.grant(StatTag.DISCIPLINE, 3, "Side Quest bonus: ${task.title}")
                            }
                        }
                    },
                    onDelete = { scope.launch { taskDao.deleteTask(taskWithSubtasks.task) } },
                    onToggleSubtask = { subtask ->
                        val nowDone = !subtask.isDone
                        scope.launch {
                            taskDao.updateSubtask(subtask.copy(isDone = nowDone))
                            if (nowDone) xpEngine.grant(StatTag.DISCIPLINE, 2, "Subtask: ${subtask.title}")
                        }
                    },
                    onDeleteSubtask = { subtask -> scope.launch { taskDao.deleteSubtask(subtask) } },
                    onAddSubtask = { title ->
                        scope.launch {
                            taskDao.insertSubtask(
                                Subtask(
                                    taskId = taskWithSubtasks.task.id,
                                    title = title,
                                    position = taskWithSubtasks.subtasks.size
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    editingTask?.let { task ->
        TaskEditorDialog(
            dialogTitle = "Edit Task",
            initial = task,
            onDismiss = { editingTask = null },
            onConfirm = { newTitle, dueDate, priority, notes ->
                scope.launch {
                    taskDao.updateTask(task.copy(title = newTitle, dueDate = dueDate, priority = priority, notes = notes))
                }
                editingTask = null
            }
        )
    }
}

@Composable
private fun ListNameDialog(
    initialName: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
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
private fun TaskCard(
    taskWithSubtasks: TaskWithSubtasks,
    onEdit: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubtask: (Subtask) -> Unit,
    onDeleteSubtask: (Subtask) -> Unit,
    onAddSubtask: (String) -> Unit
) {
    val task = taskWithSubtasks.task
    val subtasks = taskWithSubtasks.subtasks
    var expanded by remember(task.id) { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityChip(task.priority)
                        task.dueDate?.let {
                            Text(
                                formatDate(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (subtasks.isNotEmpty()) {
                            Text(
                                "${subtasks.count { it.isDone }}/${subtasks.size} subtasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (task.notes.isNotBlank()) {
                        Text(
                            task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit task")
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Hide subtasks" else "Show subtasks"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                }
            }

            if (expanded) {
                Column(Modifier.padding(start = 40.dp)) {
                    subtasks.sortedBy { it.position }.forEach { subtask ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = subtask.isDone,
                                onCheckedChange = { onToggleSubtask(subtask) }
                            )
                            Text(
                                subtask.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null
                            )
                            IconButton(onClick = { onDeleteSubtask(subtask) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Delete subtask")
                            }
                        }
                    }
                    AddSubtaskRow(onAdd = onAddSubtask)
                }
            }
        }
    }
}

/** Collapsed to a low-emphasis text row by default rather than an always-visible outlined text
 * field + button - a full input control sitting in every expanded task card, whether or not the
 * user actually wants to add a subtask right then, was too visually loud next to the task list
 * above it. Tapping it swaps in the real input, same as before. */
@Composable
private fun AddSubtaskRow(onAdd: (String) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    if (adding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Subtask title", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    onAdd(text.trim())
                    text = ""
                }
                adding = false
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Confirm add subtask")
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { adding = true }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Add subtask",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityChip(priority: Priority) {
    val color = when (priority) {
        Priority.LOW -> SystemGreen
        Priority.MEDIUM -> SystemYellow
        Priority.HIGH -> SystemRed
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            priority.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** Used both to create a new task and (pre-filled from an existing one) to edit it - the two
 * flows collect the exact same fields, so there's no reason to keep separate dialogs in sync. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    dialogTitle: String,
    initial: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: Long?, priority: Priority, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    var dueDate by remember { mutableStateOf(initial?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
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
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) }
                        )
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(dueDate?.let { formatDate(it) } ?: "Set due date (optional)")
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim(), dueDate, priority, notes.trim()) },
                enabled = title.isNotBlank()
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

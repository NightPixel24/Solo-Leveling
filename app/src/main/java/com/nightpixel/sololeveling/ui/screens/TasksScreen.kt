package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
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
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import com.nightpixel.sololeveling.data.entity.TaskWithSubtasks
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val COLUMN_WIDTH = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val taskDao = remember { app.database.taskDao() }
    val taskListDao = remember { app.database.taskListDao() }
    val scope = rememberCoroutineScope()

    val lists by taskListDao.observeLists().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tasks") }) }
    ) { innerPadding ->
        LazyRow(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lists, key = { it.id }) { list ->
                TaskListColumn(
                    list = list,
                    taskDao = taskDao,
                    scope = scope,
                    canDelete = lists.size > 1,
                    onRename = { newName -> scope.launch { taskListDao.updateList(list.copy(name = newName)) } },
                    onDelete = { scope.launch { taskListDao.deleteListCascading(list.id) } }
                )
            }
            item {
                AddListColumn(
                    onAdd = { name ->
                        scope.launch { taskListDao.insertList(TaskList(name = name, position = lists.size)) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListColumn(
    list: TaskList,
    taskDao: TaskDao,
    scope: CoroutineScope,
    canDelete: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val tasks by taskDao.observeTasksForList(list.id).collectAsState(initial = emptyList())
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.width(COLUMN_WIDTH).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${tasks.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "List options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (canDelete) "Delete list" else "Can't delete last list") },
                            enabled = canDelete,
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                items(tasks, key = { it.task.id }) { taskWithSubtasks ->
                    TaskCard(
                        taskWithSubtasks = taskWithSubtasks,
                        onToggleDone = {
                            scope.launch {
                                taskDao.updateTask(taskWithSubtasks.task.copy(isDone = !taskWithSubtasks.task.isDone))
                            }
                        },
                        onDelete = { scope.launch { taskDao.deleteTask(taskWithSubtasks.task) } },
                        onToggleSubtask = { subtask ->
                            scope.launch { taskDao.updateSubtask(subtask.copy(isDone = !subtask.isDone)) }
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
                item {
                    TextButton(onClick = { showAddTaskDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add task")
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, dueDate, priority, notes ->
                scope.launch {
                    taskDao.insertTask(
                        Task(listId = list.id, title = title, dueDate = dueDate, priority = priority, notes = notes)
                    )
                }
                showAddTaskDialog = false
            }
        )
    }

    if (showRenameDialog) {
        ListNameDialog(
            initialName = list.name,
            title = "Rename List",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName -> onRename(newName); showRenameDialog = false }
        )
    }
}

@Composable
private fun AddListColumn(onAdd: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.width(140.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(Modifier.fillMaxSize().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
            TextButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add list")
            }
        }
    }

    if (showDialog) {
        ListNameDialog(
            initialName = "",
            title = "New List",
            onDismiss = { showDialog = false },
            onConfirm = { name -> onAdd(name); showDialog = false }
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

@Composable
private fun AddSubtaskRow(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Add subtask", style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {
            if (text.isNotBlank()) {
                onAdd(text.trim())
                text = ""
            }
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Add subtask")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: Long?, priority: Priority, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
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
            ) { Text("Add") }
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

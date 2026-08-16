package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import com.nightpixel.sololeveling.data.entity.Priority
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskWithSubtasks
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
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
    val scope = rememberCoroutineScope()

    val tasks by taskDao.observeTasksWithSubtasks().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tasks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No tasks yet - tap + to add one",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, dueDate, priority, notes ->
                scope.launch { taskDao.insertTask(Task(title = title, dueDate = dueDate, priority = priority, notes = notes)) }
                showAddDialog = false
            }
        )
    }
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })
                Column(Modifier.weight(1f)) {
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
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                }
            }

            Column(Modifier.padding(start = 40.dp)) {
                taskWithSubtasks.subtasks.sortedBy { it.position }.forEach { subtask ->
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

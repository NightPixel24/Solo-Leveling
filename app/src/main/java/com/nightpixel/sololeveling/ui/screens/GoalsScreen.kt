package com.nightpixel.sololeveling.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GoalStatus
import com.nightpixel.sololeveling.data.entity.GoalTier
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Spec Section 4.7 - the long-game tier tracker that will drive the Rank engine
 * (Phase 11) once each tier's first completed goal advances it. Reached from the
 * Dashboard's flag icon for now; spec calls for tapping a Rank badge there instead,
 * but that badge doesn't exist until the gamification core (Phase 10/15) is built. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val goalDao = remember { app.database.goalDao() }
    val taskDao = remember { app.database.taskDao() }
    val scope = rememberCoroutineScope()

    val goals by goalDao.observeAll().collectAsState(initial = emptyList())
    var allTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    val grouped = remember(goals) { goals.groupBy { it.tier } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Life Goals") },
                actions = {
                    IconButton(onClick = {
                        editingGoal = null
                        scope.launch { allTasks = taskDao.getAllTasksOnce() }
                        showDialog = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add goal")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No goals yet - tap + to define your first",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoalTier.entries.forEach { tier ->
                    val tierGoals = grouped[tier].orEmpty()
                    if (tierGoals.isNotEmpty()) {
                        item(key = "header-${tier.name}") { SectionHeader(tier.label) }
                        items(tierGoals, key = { it.id }) { goal ->
                            GoalCard(
                                goal = goal,
                                allTasks = allTasks,
                                onClick = {
                                    editingGoal = goal
                                    scope.launch { allTasks = taskDao.getAllTasksOnce() }
                                    showDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        GoalDialog(
            existing = editingGoal,
            allTasks = allTasks,
            onDismiss = { showDialog = false },
            onSave = { goal ->
                scope.launch {
                    if (editingGoal == null) goalDao.insertGoal(goal) else goalDao.updateGoal(goal)
                }
                showDialog = false
            },
            onDelete = editingGoal?.let { existing ->
                {
                    scope.launch { goalDao.deleteGoal(existing) }
                    showDialog = false
                }
            }
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, allTasks: List<Task>, onClick: () -> Unit) {
    val linkedIds = remember(goal.linkedTaskIds) {
        goal.linkedTaskIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }
    val linkedTasks = remember(linkedIds, allTasks) { allTasks.filter { it.id in linkedIds } }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(goal.status)
            }
            if (goal.description.isNotBlank()) {
                Text(
                    goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            goal.targetDate?.let {
                Text(
                    "Target: ${formatGoalDate(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (linkedTasks.isNotEmpty()) {
                val done = linkedTasks.count { it.isDone }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "$done/${linkedTasks.size} linked tasks done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { done.toFloat() / linkedTasks.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: GoalStatus) {
    val color = when (status) {
        GoalStatus.ACTIVE -> SystemBlue
        GoalStatus.COMPLETED -> SystemGreen
        GoalStatus.FAILED -> SystemRed
    }
    Text(
        statusLabel(status),
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

private fun statusLabel(status: GoalStatus): String = when (status) {
    GoalStatus.ACTIVE -> "Active"
    GoalStatus.COMPLETED -> "Completed"
    GoalStatus.FAILED -> "Failed"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GoalDialog(
    existing: Goal?,
    allTasks: List<Task>,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var tier by remember { mutableStateOf(existing?.tier ?: GoalTier.ONE_MONTH) }
    var status by remember { mutableStateOf(existing?.status ?: GoalStatus.ACTIVE) }
    var targetDate by remember { mutableStateOf(existing?.targetDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var linkedIds by remember {
        mutableStateOf(
            existing?.linkedTaskIds.orEmpty().split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Goal" else "Edit Goal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalTier.entries.forEach { t ->
                        FilterChip(selected = tier == t, onClick = { tier = t }, label = { Text(t.label) })
                    }
                }
                if (existing != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalStatus.entries.forEach { s ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(statusLabel(s)) })
                        }
                    }
                }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(targetDate?.let { formatGoalDate(it) } ?: "Set target date (optional)")
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                if (allTasks.isNotEmpty()) {
                    Text("Link tasks as milestones", style = MaterialTheme.typography.labelLarge)
                    Column(modifier = Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                        allTasks.forEach { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = task.id in linkedIds,
                                    onCheckedChange = { checked ->
                                        linkedIds = if (checked) linkedIds + task.id else linkedIds - task.id
                                    }
                                )
                                Text(
                                    task.title,
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            Goal(
                                id = existing?.id ?: 0,
                                tier = tier,
                                title = title.trim(),
                                description = description.trim(),
                                targetDate = targetDate,
                                status = status,
                                linkedTaskIds = linkedIds.joinToString(","),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    targetDate = datePickerState.selectedDateMillis
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

private fun formatGoalDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

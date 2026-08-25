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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.PunishmentAssignment
import com.nightpixel.sololeveling.data.entity.PunishmentPoolItem
import com.nightpixel.sololeveling.data.entity.PunishmentSeverity
import com.nightpixel.sololeveling.data.gamification.detectMissedItems
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Spec Section 5.6 - a self-defined pool of Minor/Major punishments, randomly assigned as
 * "Debts" when a daily habit/gym day or weekly target is missed. Reached from a new Dashboard
 * icon (spec doesn't assign this a bottom-nav slot, matching how Life Goals/Settings also live
 * off the bottom nav). The missed-item scan (`detectMissedItems`) runs once when this screen
 * opens rather than on every app launch or via a background job (that's Phase 16) - it only looks
 * at the most recently completed day/week, so opening this screen after a longer gap won't
 * backfill every miss in between, same "keep it bounded" reasoning Quests (Phase 12) uses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunishmentScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val punishmentDao = remember { app.database.punishmentDao() }
    val habitDao = remember { app.database.habitDao() }
    val gymDao = remember { app.database.gymDao() }
    val scope = rememberCoroutineScope()

    val poolItems by punishmentDao.observePoolItems().collectAsState(initial = emptyList())
    val activeAssignments by punishmentDao.observeActiveAssignments().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val habitsWithLogs = habitDao.observeHabitsWithLogs().first()
        val exercisesWithSessions = gymDao.observeExercisesWithSessions().first()
        val missed = detectMissedItems(LocalDate.now(), habitsWithLogs, exercisesWithSessions)
        missed.forEach { miss ->
            val item = punishmentDao.getRandomItem(miss.severity) ?: return@forEach
            punishmentDao.insertAssignment(
                PunishmentAssignment(
                    itemId = item.id,
                    sourceRef = miss.sourceRef,
                    dateAssigned = miss.date.toString()
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Punishment Pool") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add punishment")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (poolItems.isEmpty() && activeAssignments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No punishments defined yet - tap + to add one",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeAssignments.isNotEmpty()) {
                    item(key = "debts-header") { SectionHeader("Active Debts") }
                    items(activeAssignments, key = { "debt-${it.id}" }) { assignment ->
                        val item = poolItems.find { it.id == assignment.itemId }
                        DebtRow(
                            assignment = assignment,
                            item = item,
                            onResolve = {
                                scope.launch {
                                    punishmentDao.updateAssignment(
                                        assignment.copy(resolved = true, resolvedAt = System.currentTimeMillis())
                                    )
                                }
                            }
                        )
                    }
                }
                item(key = "pool-header") { SectionHeader("Pool") }
                items(poolItems, key = { "item-${it.id}" }) { item ->
                    PoolItemRow(item = item, onDelete = { scope.launch { punishmentDao.deletePoolItem(item) } })
                }
            }
        }
    }

    if (showAddDialog) {
        AddPunishmentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                scope.launch { punishmentDao.insertPoolItem(item) }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DebtRow(assignment: PunishmentAssignment, item: PunishmentPoolItem?, onResolve: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item?.description ?: "(deleted punishment)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item?.let { SeverityChip(it.severity) }
                    Text(
                        "Assigned ${assignment.dateAssigned}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onResolve) { Text("Clear") }
        }
    }
}

@Composable
private fun PoolItemRow(item: PunishmentPoolItem, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.titleMedium)
                SeverityChip(item.severity)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete punishment")
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: PunishmentSeverity) {
    val color = if (severity == PunishmentSeverity.MAJOR) SystemRed else SystemYellow
    Text(severity.name, style = MaterialTheme.typography.labelSmall, color = color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPunishmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (PunishmentPoolItem) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(PunishmentSeverity.MINOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Punishment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = severity == PunishmentSeverity.MINOR,
                        onClick = { severity = PunishmentSeverity.MINOR },
                        label = { Text("Minor") }
                    )
                    FilterChip(
                        selected = severity == PunishmentSeverity.MAJOR,
                        onClick = { severity = PunishmentSeverity.MAJOR },
                        label = { Text("Major") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (description.isNotBlank()) {
                        onConfirm(PunishmentPoolItem(description = description.trim(), severity = severity))
                    }
                },
                enabled = description.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

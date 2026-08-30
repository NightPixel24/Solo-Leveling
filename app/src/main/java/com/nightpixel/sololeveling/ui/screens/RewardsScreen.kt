package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.PunishmentSeverity
import com.nightpixel.sololeveling.data.entity.RewardInventoryItem
import com.nightpixel.sololeveling.data.entity.RewardPoolItem
import com.nightpixel.sololeveling.data.gamification.claimedMajorThisMonth
import com.nightpixel.sololeveling.data.gamification.claimedMinorThisWeek
import com.nightpixel.sololeveling.data.gamification.countGoodWeeksInLastN
import com.nightpixel.sololeveling.data.gamification.wasLastWeekGood
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The currency-free Reward Economy (user feedback, 2026-08-30: "get rid of currency all together
 * keep the rewards section. There should be minor and major rewards..."). Mirrors the Punishment
 * Pool's own Minor/Major split (`PunishmentSeverity` reused directly, same [SeverityChip]-style
 * visual language) for the opposite direction - a payoff instead of a debt. Two live-computed
 * eligibility windows (see `data/gamification/Rewards.kt`): a good week unlocks one Minor claim
 * the following week; 3 good weeks in the trailing 4 unlocks one Major claim this month. Claiming
 * moves a pool item into your [RewardInventoryItem] inventory; "Use" there marks it redeemed in
 * real life - the same "pending vs. history" dual state `PunishmentAssignment.resolved` already
 * plays for Debts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val rewardDao = remember { app.database.rewardDao() }
    val habitDao = remember { app.database.habitDao() }
    val gymDao = remember { app.database.gymDao() }
    val waterDao = remember { app.database.waterDao() }
    val scope = rememberCoroutineScope()

    val poolItems by rewardDao.observePoolItems().collectAsState(initial = emptyList())
    val inventory by rewardDao.observeInventory().collectAsState(initial = emptyList())

    val habitsWithLogs by habitDao.observeHabitsWithLogs().collectAsState(initial = emptyList())
    val exercisesWithSessions by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val allWaterLogs by waterDao.observeAllLogs().collectAsState(initial = emptyList())
    val waterLogsByDate = remember(allWaterLogs) { allWaterLogs.associateBy { it.date } }

    val today = remember { LocalDate.now() }

    val lastWeekGood = remember(habitsWithLogs, exercisesWithSessions, waterLogsByDate, today) {
        wasLastWeekGood(today, habitsWithLogs, exercisesWithSessions, waterLogsByDate)
    }
    val minorAlreadyClaimed = remember(inventory, today) { claimedMinorThisWeek(today, inventory) }
    val minorEligible = lastWeekGood && !minorAlreadyClaimed

    val goodWeeksInLast4 = remember(habitsWithLogs, exercisesWithSessions, waterLogsByDate, today) {
        countGoodWeeksInLastN(today, 4, habitsWithLogs, exercisesWithSessions, waterLogsByDate)
    }
    val majorUnlocked = goodWeeksInLast4 >= 3
    val majorAlreadyClaimed = remember(inventory, today) { claimedMajorThisMonth(today, inventory) }
    val majorEligible = majorUnlocked && !majorAlreadyClaimed

    var showAddDialog by remember { mutableStateOf(false) }
    var claimPickerSeverity by remember { mutableStateOf<PunishmentSeverity?>(null) }

    fun claim(item: RewardPoolItem) {
        scope.launch {
            rewardDao.insertInventoryItem(RewardInventoryItem(title = item.title, severity = item.severity))
        }
        claimPickerSeverity = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add reward")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "week-header") { SectionHeader("This Week") }
            item(key = "week-card") {
                EligibilityCard(
                    severity = PunishmentSeverity.MINOR,
                    eligible = minorEligible,
                    reason = when {
                        minorAlreadyClaimed -> "Already claimed this week"
                        !lastWeekGood -> "Last week wasn't a good week"
                        else -> null
                    },
                    onClaim = { claimPickerSeverity = PunishmentSeverity.MINOR }
                )
            }

            item(key = "month-header") { SectionHeader("This Month") }
            item(key = "month-card") {
                EligibilityCard(
                    severity = PunishmentSeverity.MAJOR,
                    eligible = majorEligible,
                    reason = when {
                        majorAlreadyClaimed -> "Already claimed this month"
                        !majorUnlocked -> "$goodWeeksInLast4/4 good weeks recently - need 3 to unlock"
                        else -> null
                    },
                    onClaim = { claimPickerSeverity = PunishmentSeverity.MAJOR }
                )
            }

            val unusedInventory = inventory.filter { it.usedAt == null }
            if (unusedInventory.isNotEmpty()) {
                item(key = "inv-header") { SectionHeader("Inventory") }
                items(unusedInventory, key = { "inv-${it.id}" }) { item ->
                    InventoryRow(
                        item = item,
                        onUse = {
                            scope.launch {
                                rewardDao.updateInventoryItem(item.copy(usedAt = System.currentTimeMillis()))
                            }
                        }
                    )
                }
            }

            val minorPool = poolItems.filter { it.severity == PunishmentSeverity.MINOR }
            if (minorPool.isNotEmpty()) {
                item(key = "minor-pool-header") { SectionHeader("Minor Pool") }
                items(minorPool, key = { "minor-${it.id}" }) { item ->
                    PoolItemRow(item, onDelete = { scope.launch { rewardDao.deletePoolItem(item) } })
                }
            }
            val majorPool = poolItems.filter { it.severity == PunishmentSeverity.MAJOR }
            if (majorPool.isNotEmpty()) {
                item(key = "major-pool-header") { SectionHeader("Major Pool") }
                items(majorPool, key = { "major-${it.id}" }) { item ->
                    PoolItemRow(item, onDelete = { scope.launch { rewardDao.deletePoolItem(item) } })
                }
            }

            val history = inventory.filter { it.usedAt != null }.sortedByDescending { it.usedAt }
            if (history.isNotEmpty()) {
                item(key = "history-header") { SectionHeader("History") }
                items(history, key = { "hist-${it.id}" }) { item -> HistoryRow(item) }
            }
        }
    }

    if (showAddDialog) {
        AddRewardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                scope.launch { rewardDao.insertPoolItem(item) }
                showAddDialog = false
            }
        )
    }

    claimPickerSeverity?.let { severity ->
        ClaimPickerDialog(
            severity = severity,
            poolItems = poolItems.filter { it.severity == severity },
            onDismiss = { claimPickerSeverity = null },
            onPick = ::claim
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SeverityChip(severity: PunishmentSeverity) {
    val color = if (severity == PunishmentSeverity.MAJOR) SystemRed else SystemYellow
    Text(severity.name, style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun EligibilityCard(
    severity: PunishmentSeverity,
    eligible: Boolean,
    reason: String?,
    onClaim: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (severity == PunishmentSeverity.MAJOR) "Major Reward" else "Minor Reward",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    reason ?: "Ready to claim!",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (eligible) SystemGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (eligible) {
                Button(onClick = onClaim) { Text("Claim") }
            }
        }
    }
}

@Composable
private fun InventoryRow(item: RewardInventoryItem, onUse: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SeverityChip(item.severity)
                    Text(
                        "Claimed ${formatDate(item.claimedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onUse) { Text("Use") }
        }
    }
}

@Composable
private fun PoolItemRow(item: RewardPoolItem, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                SeverityChip(item.severity)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete reward")
            }
        }
    }
}

@Composable
private fun HistoryRow(item: RewardInventoryItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SeverityChip(item.severity)
                Text(
                    "Used ${item.usedAt?.let { formatDate(it) } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaimPickerDialog(
    severity: PunishmentSeverity,
    poolItems: List<RewardPoolItem>,
    onDismiss: () -> Unit,
    onPick: (RewardPoolItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Claim a ${if (severity == PunishmentSeverity.MAJOR) "Major" else "Minor"} Reward") },
        text = {
            if (poolItems.isEmpty()) {
                Text(
                    "Add a reward to this pool first",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    poolItems.forEach { item ->
                        Surface(
                            onClick = { onPick(item) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                item.title,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (RewardPoolItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(PunishmentSeverity.MINOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Reward") },
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
                    if (title.isNotBlank()) {
                        onConfirm(RewardPoolItem(title = title.trim(), severity = severity))
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

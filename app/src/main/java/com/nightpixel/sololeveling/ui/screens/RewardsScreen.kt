package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.nightpixel.sololeveling.data.entity.RewardPool
import com.nightpixel.sololeveling.data.entity.RewardPoolItem
import com.nightpixel.sololeveling.data.entity.RewardTarget
import com.nightpixel.sololeveling.data.gamification.countGoodWeeksInLastN
import com.nightpixel.sololeveling.data.gamification.goldEarnedSince
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemVioletBright
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Spec Section 5.7 - the Reward Economy. Gold is granted elsewhere (habit/gym completion sites -
 * see `GoldEngine`); this screen is where it gets spent. Each week/month you pick one reward from
 * that pool as your target (`RewardTarget`); once Gold *earned* since the period started (see
 * `goldEarnedSince`) covers its cost, "Claim" unlocks and spends it. The Monthly pool stays locked
 * until 3 of the last 4 weeks were "good weeks" (spec Section 5.4/5.7, `countGoodWeeksInLastN`) -
 * reusing the exact "good week" definition Weekly Quests already established on the Dashboard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val rewardDao = remember { app.database.rewardDao() }
    val habitDao = remember { app.database.habitDao() }
    val gymDao = remember { app.database.gymDao() }
    val waterDao = remember { app.database.waterDao() }
    val goldEngine = remember { app.goldEngine }
    val scope = rememberCoroutineScope()

    val balance by rewardDao.observeBalance().collectAsState(initial = null)
    val poolItems by rewardDao.observePoolItems().collectAsState(initial = emptyList())
    val targets by rewardDao.observeTargets().collectAsState(initial = emptyList())
    val transactions by rewardDao.observeTransactions().collectAsState(initial = emptyList())

    val habitsWithLogs by habitDao.observeHabitsWithLogs().collectAsState(initial = emptyList())
    val exercisesWithSessions by gymDao.observeExercisesWithSessions().collectAsState(initial = emptyList())
    val allWaterLogs by waterDao.observeAllLogs().collectAsState(initial = emptyList())

    val today = remember { LocalDate.now() }
    val weekStart = remember(today) { today.with(DayOfWeek.MONDAY) }
    val monthStart = remember(today) { today.withDayOfMonth(1) }

    val goodWeeks = remember(habitsWithLogs, exercisesWithSessions, allWaterLogs, today) {
        countGoodWeeksInLastN(today, 4, habitsWithLogs, exercisesWithSessions, allWaterLogs.associateBy { it.date })
    }
    val monthlyUnlocked = goodWeeks >= 3

    val weeklyEarned = remember(weekStart, transactions) { goldEarnedSince(weekStart, transactions) }
    val monthlyEarned = remember(monthStart, transactions) { goldEarnedSince(monthStart, transactions) }

    val weeklyTarget = targets.find { it.pool == RewardPool.WEEKLY && it.periodStart == weekStart.toString() }
    val monthlyTarget = targets.find { it.pool == RewardPool.MONTHLY && it.periodStart == monthStart.toString() }

    var showAddDialog by remember { mutableStateOf(false) }

    fun pick(pool: RewardPool, periodStart: LocalDate, item: RewardPoolItem) {
        scope.launch {
            rewardDao.upsertTarget(RewardTarget(pool = pool, periodStart = periodStart.toString(), itemId = item.id))
        }
    }

    fun claim(target: RewardTarget, item: RewardPoolItem) {
        scope.launch {
            goldEngine.spend(item.cost, "Reward: ${item.title}")
            rewardDao.updateTarget(target.copy(claimed = true, claimedAt = System.currentTimeMillis()))
        }
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
            item { GoldBalanceCard(balance?.balance ?: 0) }

            item { SectionHeader("Weekly Target") }
            item {
                TargetCard(
                    target = weeklyTarget,
                    item = poolItems.find { it.id == weeklyTarget?.itemId },
                    earned = weeklyEarned,
                    poolItems = poolItems.filter { it.pool == RewardPool.WEEKLY },
                    locked = false,
                    lockedReason = null,
                    onPick = { pick(RewardPool.WEEKLY, weekStart, it) },
                    onClaim = ::claim
                )
            }

            item { SectionHeader("Monthly Target") }
            item {
                TargetCard(
                    target = monthlyTarget,
                    item = poolItems.find { it.id == monthlyTarget?.itemId },
                    earned = monthlyEarned,
                    poolItems = poolItems.filter { it.pool == RewardPool.MONTHLY },
                    locked = !monthlyUnlocked,
                    lockedReason = "$goodWeeks/4 good weeks recently - need 3 to unlock Monthly rewards",
                    onPick = { pick(RewardPool.MONTHLY, monthStart, it) },
                    onClaim = ::claim
                )
            }

            val weeklyPool = poolItems.filter { it.pool == RewardPool.WEEKLY }
            if (weeklyPool.isNotEmpty()) {
                item { SectionHeader("Weekly Pool") }
                items(weeklyPool, key = { "wpool-${it.id}" }) { item ->
                    PoolItemRow(item, onDelete = { scope.launch { rewardDao.deletePoolItem(item) } })
                }
            }
            val monthlyPool = poolItems.filter { it.pool == RewardPool.MONTHLY }
            if (monthlyPool.isNotEmpty()) {
                item { SectionHeader("Monthly Pool") }
                items(monthlyPool, key = { "mpool-${it.id}" }) { item ->
                    PoolItemRow(item, onDelete = { scope.launch { rewardDao.deletePoolItem(item) } })
                }
            }

            val history = targets.filter { it.claimed }.sortedByDescending { it.claimedAt }
            if (history.isNotEmpty()) {
                item { SectionHeader("History") }
                items(history, key = { "hist-${it.id}" }) { target ->
                    HistoryRow(target, poolItems.find { it.id == target.itemId })
                }
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
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GoldBalanceCard(balance: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = SystemYellow)
            Text("$balance Gold", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetCard(
    target: RewardTarget?,
    item: RewardPoolItem?,
    earned: Int,
    poolItems: List<RewardPoolItem>,
    locked: Boolean,
    lockedReason: String?,
    onPick: (RewardPoolItem) -> Unit,
    onClaim: (RewardTarget, RewardPoolItem) -> Unit
) {
    var showPicker by remember(target?.id, locked) { mutableStateOf(target == null && !locked) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (locked) {
                Text(
                    lockedReason ?: "Locked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (target != null && item != null && !showPicker) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${item.cost} Gold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (target.claimed) {
                        Text("Claimed!", color = SystemGreen, style = MaterialTheme.typography.labelLarge)
                    } else {
                        TextButton(onClick = { showPicker = true }) { Text("Change") }
                    }
                }
                if (!target.claimed) {
                    val progress = if (item.cost > 0) (earned / item.cost.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemVioletBright
                    )
                    Text(
                        "$earned / ${item.cost} Gold earned this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (earned >= item.cost) {
                        Button(onClick = { onClaim(target, item) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Claim")
                        }
                    }
                }
            } else if (target != null && item == null) {
                Text("(deleted reward)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!locked && showPicker) {
                if (poolItems.isEmpty()) {
                    Text(
                        "Add a reward to this pool first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Pick a target:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        poolItems.forEach { candidate ->
                            FilterChip(
                                selected = target?.itemId == candidate.id,
                                onClick = {
                                    onPick(candidate)
                                    showPicker = false
                                },
                                label = { Text("${candidate.title} (${candidate.cost}g)") }
                            )
                        }
                    }
                }
            }
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
                Text(
                    "${item.cost} Gold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete reward")
            }
        }
    }
}

@Composable
private fun HistoryRow(target: RewardTarget, item: RewardPoolItem?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item?.title ?: "(deleted reward)", style = MaterialTheme.typography.titleMedium)
            Text(
                poolLabel(target.pool) + (target.claimedAt?.let { " - claimed ${formatDate(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun poolLabel(pool: RewardPool): String =
    pool.name.lowercase().replaceFirstChar { it.uppercase() }

private fun formatDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (RewardPoolItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var pool by remember { mutableStateOf(RewardPool.WEEKLY) }

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
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Gold cost") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pool == RewardPool.WEEKLY,
                        onClick = { pool = RewardPool.WEEKLY },
                        label = { Text("Weekly") }
                    )
                    FilterChip(
                        selected = pool == RewardPool.MONTHLY,
                        onClick = { pool = RewardPool.MONTHLY },
                        label = { Text("Monthly") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val costValue = cost.toIntOrNull()
                    if (title.isNotBlank() && costValue != null && costValue > 0) {
                        onConfirm(RewardPoolItem(title = title.trim(), cost = costValue, pool = pool))
                    }
                },
                enabled = title.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

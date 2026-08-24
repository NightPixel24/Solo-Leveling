package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val waterDao = remember { app.database.waterDao() }
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now().toString() }
    val log by waterDao.observeLog(today).collectAsState(initial = null)
    var showGoalDialog by remember { mutableStateOf(false) }

    val bottlesLogged = log?.bottlesLogged ?: 0
    val goalBottles = log?.goalBottles ?: 8

    // A day's row doesn't exist until first touched - seed it (using whatever goal was last
    // set, so the target doesn't silently reset to the default every new day) rather than
    // showing an empty/undefined state before the user interacts with anything.
    LaunchedEffect(log) {
        if (log == null) {
            val defaultGoal = waterDao.getLatestGoal() ?: 8
            waterDao.upsertLog(WaterLog(date = today, bottlesLogged = 0, goalBottles = defaultGoal))
        }
    }

    fun updateBottles(newBottles: Int, newGoal: Int = goalBottles) {
        scope.launch {
            waterDao.upsertLog(
                WaterLog(date = today, bottlesLogged = newBottles.coerceIn(0, newGoal), goalBottles = newGoal)
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's Water", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { showGoalDialog = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Set goal")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "$bottlesLogged / $goalBottles bottles ($goalBottles L goal)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { if (goalBottles > 0) (bottlesLogged.toFloat() / goalBottles).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (i in 0 until goalBottles) {
                val filled = i < bottlesLogged
                IconButton(onClick = { updateBottles(if (filled) i else i + 1) }) {
                    Icon(
                        Icons.Filled.LocalDrink,
                        contentDescription = "Bottle ${i + 1}",
                        tint = if (filled) SystemBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }

    if (showGoalDialog) {
        SetGoalDialog(
            initialGoal = goalBottles,
            onDismiss = { showGoalDialog = false },
            onConfirm = { newGoal ->
                updateBottles(bottlesLogged, newGoal)
                showGoalDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetGoalDialog(
    initialGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var goal by remember { mutableStateOf(initialGoal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Water Goal") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Bottles (1L each):", modifier = Modifier.weight(1f))
                IconButton(onClick = { if (goal > 1) goal-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease goal")
                }
                Text("$goal", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { if (goal < 20) goal++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase goal")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(goal) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

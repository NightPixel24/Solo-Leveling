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
import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.dao.WaterDao
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.data.gamification.applyVitalityMultiplier
import com.nightpixel.sololeveling.ui.components.StatChip
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val waterDao = remember { app.database.waterDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now().toString() }
    val log by waterDao.observeLog(today).collectAsState(initial = null)
    var showGoalDialog by remember { mutableStateOf(false) }

    val bottlesLogged = log?.bottlesLogged ?: 0
    val goalBottles = log?.goalBottles ?: 8

    // A day's row doesn't exist until first touched - seed it (using whatever goal was last
    // set, so the target doesn't silently reset to the default every new day) rather than
    // showing an empty/undefined state before the user interacts with anything. Keyed on Unit
    // (runs once per screen visit) and checks the DB directly via getLogOnce rather than trusting
    // `log` - collectAsState's synthetic `initial = null` is indistinguishable from "no row yet"
    // on the very first frame, so keying this off `log` raced the Flow's real first emission and
    // could overwrite an already-logged day back to 0 cups if this composed again before that
    // emission arrived (e.g. reopening the tab, or right after the Dashboard's quick-add).
    LaunchedEffect(Unit) {
        if (waterDao.getLogOnce(today) == null) {
            val defaultGoal = waterDao.getLatestGoal() ?: 8
            waterDao.upsertLog(WaterLog(date = today, bottlesLogged = 0, goalBottles = defaultGoal))
        }
    }

    fun updateBottles(newBottles: Int, newGoal: Int = goalBottles) {
        val clamped = newBottles.coerceIn(0, newGoal)
        val alreadyGranted = log?.xpGranted ?: false
        val justHitGoal = clamped >= newGoal && !alreadyGranted
        scope.launch {
            waterDao.upsertLog(
                WaterLog(
                    date = today,
                    bottlesLogged = clamped,
                    goalBottles = newGoal,
                    xpGranted = alreadyGranted || justHitGoal
                )
            )
            if (justHitGoal) {
                xpEngine.grant(StatTag.VIT, applyVitalityMultiplier(app.database.foodDao(), 10), "Water goal hit")
            }
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Today's Water", style = MaterialTheme.typography.titleLarge)
                StatChip(StatTag.VIT)
            }
            IconButton(onClick = { showGoalDialog = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "Set goal")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // User feedback, 2026-08-30: spell out the per-cup ml amount instead of only the
            // goal-setting dialog knowing it, and show the goal itself in liters (not ml) - a
            // round-numbered "2.0 L" reads more like a real hydration target than "2000 ml".
            Text(
                "$bottlesLogged / $goalBottles cups (250 ml each) - ${litersText(goalBottles)} L goal",
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
                        contentDescription = "Cup ${i + 1}",
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

/** "2.0" for 8 cups, "1.75" for 7 - always at least one decimal place so a round number still
 * reads as a liquid volume ("2.0 L"), not an integer count. */
private fun litersText(cups: Int): String {
    val liters = cups * 250 / 1000.0
    return if (liters == liters.toLong().toDouble()) {
        "%.1f".format(liters)
    } else {
        liters.toString().trimEnd('0').trimEnd('.')
    }
}

/** Not private - reused by the Dashboard's water quick-add (spec Section 6): a plain +1 to
 * today's cup count, sharing the same goal-default and one-time `xpGranted` bookkeeping
 * [WaterScreen]'s own cup taps use, rather than re-deriving that logic a second time. Returns the
 * new count/goal so the caller (Dashboard has no visible water counter of its own) can show
 * feedback - without this, tapping the quick-add button had no visible effect at all. */
suspend fun logWaterBottle(
    waterDao: WaterDao,
    foodDao: FoodDao,
    xpEngine: XpEngine,
    date: String,
    currentLog: WaterLog?
): Pair<Int, Int> {
    val goal = currentLog?.goalBottles ?: waterDao.getLatestGoal() ?: 8
    val newBottles = ((currentLog?.bottlesLogged ?: 0) + 1).coerceAtMost(goal)
    val alreadyGranted = currentLog?.xpGranted ?: false
    val justHitGoal = newBottles >= goal && !alreadyGranted
    waterDao.upsertLog(
        WaterLog(date = date, bottlesLogged = newBottles, goalBottles = goal, xpGranted = alreadyGranted || justHitGoal)
    )
    if (justHitGoal) xpEngine.grant(StatTag.VIT, applyVitalityMultiplier(foodDao, 10), "Water goal hit")
    return newBottles to goal
}

/** The Dashboard's "At a Glance" checklist (user feedback, 2026-08-30) - ticking off "hit water
 * goal" there jumps straight to the goal in one tap rather than requiring `goalBottles` individual
 * taps, since a checkbox reads as "mark this done," not "add one more cup." Shares the exact same
 * goal-default/one-time-`xpGranted` bookkeeping [logWaterBottle] and the Water tab's own cup taps
 * already use. A no-op if the goal's already met (so re-tapping an already-checked box is safe). */
suspend fun markWaterGoalHit(
    waterDao: WaterDao,
    foodDao: FoodDao,
    xpEngine: XpEngine,
    date: String,
    currentLog: WaterLog?
) {
    val goal = currentLog?.goalBottles ?: waterDao.getLatestGoal() ?: 8
    if ((currentLog?.bottlesLogged ?: 0) >= goal) return
    val alreadyGranted = currentLog?.xpGranted ?: false
    waterDao.upsertLog(WaterLog(date = date, bottlesLogged = goal, goalBottles = goal, xpGranted = true))
    if (!alreadyGranted) xpEngine.grant(StatTag.VIT, applyVitalityMultiplier(foodDao, 10), "Water goal hit")
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
                Text("Cups (250 ml each):", modifier = Modifier.weight(1f))
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

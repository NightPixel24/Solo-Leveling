package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.gamification.MAX_STAT_LEVEL
import com.nightpixel.sololeveling.data.gamification.xpForLevel
import com.nightpixel.sololeveling.ui.components.RadarChart
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemVioletBright
import com.nightpixel.sololeveling.ui.theme.SystemYellow

/** The Status Window (spec Section 6) - just the Section 5.1 radar chart + per-stat level/XP
 * list for now; Today's Quests, boss fights, and the rest of Section 6's widgets are Phase 15
 * ("Dashboard/Analytics screen tying everything together"), not this phase's scope. Settings,
 * including Export/Import, is reached from here rather than the bottom nav (spec Section 8). The
 * flag icon is a stand-in for the spec's Rank badge (tapping it also opens Life Goals) until the
 * Rank engine (Phase 11) actually builds that badge. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onSettingsClick: () -> Unit, onGoalsClick: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val statDao = remember { app.database.statDao() }

    val stats by statDao.observeStats().collectAsState(initial = emptyList())
    val byTag = remember(stats) { stats.associateBy { it.tag } }
    val orderedStats = remember(byTag) { StatTag.entries.map { byTag[it] ?: Stat(tag = it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onGoalsClick) {
                        Icon(Icons.Filled.Flag, contentDescription = "Life Goals")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                RadarChart(
                    values = orderedStats.map { it.tag.name to it.level / MAX_STAT_LEVEL.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(orderedStats, key = { it.tag }) { stat ->
                StatRow(stat)
            }
        }
    }
}

@Composable
private fun StatRow(stat: Stat) {
    val color = statColor(stat.tag)
    val needed = xpForLevel(stat.level)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.tag.name, style = MaterialTheme.typography.titleMedium, color = color)
            Text(
                if (stat.level >= MAX_STAT_LEVEL) "Lv. $MAX_STAT_LEVEL (max)" else "Lv. ${stat.level}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (stat.level < MAX_STAT_LEVEL) {
            LinearProgressIndicator(
                progress = { (stat.currentXp.toFloat() / needed).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = color
            )
            Text(
                "${stat.currentXp} / $needed XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun statColor(tag: StatTag) = when (tag) {
    StatTag.STR -> SystemRed
    StatTag.VIT -> SystemGreen
    StatTag.DISCIPLINE -> SystemBlue
    StatTag.INT -> SystemVioletBright
    StatTag.AGILITY -> SystemYellow
}

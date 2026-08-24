package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.MoodColor
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class LifeTab(val label: String) { MOOD("Mood"), FOOD("Food"), WATER("Water") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeScreen() {
    var tab by remember { mutableStateOf(LifeTab.MOOD) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Life") })
                TabRow(selectedTabIndex = tab.ordinal) {
                    LifeTab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                LifeTab.MOOD -> MoodScreen()
                LifeTab.FOOD -> FoodScreen()
                LifeTab.WATER -> WaterScreen()
            }
        }
    }
}

@Composable
private fun MoodScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val moodDao = remember { app.database.moodDao() }
    val scope = rememberCoroutineScope()

    val entries by moodDao.observeEntries().collectAsState(initial = emptyList())
    val entriesByDate = remember(entries) { entries.associateBy { it.date } }

    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    val today = remember { LocalDate.now() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TodayCard(entry = entriesByDate[today.toString()], onClick = { editingDate = today })
        MonthNavigator(
            month = displayedMonth,
            onPrevious = { displayedMonth = displayedMonth.minusMonths(1) },
            onNext = { displayedMonth = displayedMonth.plusMonths(1) }
        )
        MonthHeatmap(
            month = displayedMonth,
            entriesByDate = entriesByDate,
            onDayClick = { date -> editingDate = date }
        )
        Legend()
    }

    editingDate?.let { date ->
        EditMoodDialog(
            date = date,
            existing = entriesByDate[date.toString()],
            onDismiss = { editingDate = null },
            onSave = { color, note ->
                scope.launch { moodDao.upsertEntry(MoodEntry(date = date.toString(), color = color, note = note)) }
                editingDate = null
            },
            onClear = {
                scope.launch { moodDao.deleteEntry(date.toString()) }
                editingDate = null
            }
        )
    }
}

@Composable
private fun TodayCard(entry: MoodEntry?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoodDot(color = entry?.color, size = 32.dp)
            Column(Modifier.weight(1f)) {
                Text("Today", style = MaterialTheme.typography.titleMedium)
                Text(
                    entry?.let { moodLabel(it.color) } ?: "Tap to rate your day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthNavigator(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthHeatmap(
    month: YearMonth,
    entriesByDate: Map<String, MoodEntry>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = remember(month) {
        buildList {
            repeat(leadingBlanks) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
            // Pad the trailing blanks too, not just leading - otherwise the last (partial)
            // week's Row has fewer than 7 weighted children, so each weight(1f) cell claims
            // a bigger share of the row width than the full weeks above it.
            while (size % 7 != 0) add(null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val entry = entriesByDate[date.toString()]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (entry != null) {
                                            Modifier.background(moodColorValue(entry.color))
                                        } else {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                                        }
                                    )
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = entry?.let { moodTextColor(it.color) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MoodColor.entries.forEach { color ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MoodDot(color = color, size = 12.dp)
                Text(
                    moodLabel(color),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MoodDot(color: MoodColor?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (color != null) {
                    Modifier.background(moodColorValue(color))
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                }
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMoodDialog(
    date: LocalDate,
    existing: MoodEntry?,
    onDismiss: () -> Unit,
    onSave: (MoodColor, String) -> Unit,
    onClear: () -> Unit
) {
    var color by remember { mutableStateOf(existing?.color ?: MoodColor.OK) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.format(dateFormatter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MoodColor.entries.forEach { c ->
                        MoodChoice(color = c, selected = color == c, onClick = { color = c })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(color, note.trim()) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun MoodChoice(color: MoodColor, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(moodColorValue(color))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
        )
        Text(moodLabel(color), style = MaterialTheme.typography.labelSmall)
    }
}

private fun moodColorValue(color: MoodColor): Color = when (color) {
    MoodColor.GOOD -> SystemGreen
    MoodColor.OK -> SystemYellow
    MoodColor.BAD -> Color(0xFF2A2A2A)
}

private fun moodTextColor(color: MoodColor): Color = when (color) {
    MoodColor.GOOD, MoodColor.OK -> Color(0xFF1A1A1A)
    MoodColor.BAD -> Color.White
}

private fun moodLabel(color: MoodColor): String = when (color) {
    MoodColor.GOOD -> "Good"
    MoodColor.OK -> "OK"
    MoodColor.BAD -> "Bad"
}

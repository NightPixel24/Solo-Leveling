package com.nightpixel.sololeveling.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.data.entity.MoodColor
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Shared with `LifeScreen`'s Mood tab (the original home of this grid, Phase 7) and the
 * Dashboard's month preview (Phase 15, spec Section 6) - extracted so both render the exact same
 * heatmap rather than keeping two copies of the leading/trailing-blank-padding fix in sync. */
@Composable
fun MonthHeatmap(
    month: YearMonth,
    entriesByDate: Map<String, MoodEntry>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
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
                            val isToday = date == today
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
                                    .then(
                                        if (isToday) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                        } else {
                                            Modifier
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

fun moodColorValue(color: MoodColor): Color = when (color) {
    MoodColor.GOOD -> SystemGreen
    MoodColor.OK -> SystemYellow
    MoodColor.BAD -> SystemRed
}

fun moodTextColor(color: MoodColor): Color = when (color) {
    MoodColor.GOOD, MoodColor.OK -> Color(0xFF1A1A1A)
    MoodColor.BAD -> Color.White
}

fun moodLabel(color: MoodColor): String = when (color) {
    MoodColor.GOOD -> "Good"
    MoodColor.OK -> "OK"
    MoodColor.BAD -> "Bad"
}

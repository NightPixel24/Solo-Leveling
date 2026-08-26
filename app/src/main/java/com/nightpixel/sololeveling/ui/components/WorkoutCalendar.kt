package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.nightpixel.sololeveling.data.entity.SplitDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Same leading/trailing-blank-padding shape as [MonthHeatmap] (Phase 7's fixed bug), but each
 * day is colored by which [SplitDay] was logged that date instead of a fixed 3-tier rating -
 * built as its own component rather than generalizing MonthHeatmap's mood-specific coloring,
 * since the two color models (fixed enum vs. arbitrary user-picked hex) don't share logic beyond
 * the grid layout itself. */
@Composable
fun WorkoutMonthCalendar(
    month: YearMonth,
    workoutsByDate: Map<LocalDate, SplitDay>,
    onDayClick: (LocalDate) -> Unit = {}
) {
    val today = remember { LocalDate.now() }
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = remember(month) {
        buildList {
            repeat(leadingBlanks) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
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
                            val splitDay = workoutsByDate[date]
                            val isToday = date == today
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .then(
                                        if (splitDay != null) {
                                            Modifier.background(parseHexColor(splitDay.colorHex))
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
                                    color = if (splitDay != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutCalendarLegend(splitDays: List<SplitDay>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        splitDays.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).background(parseHexColor(day.colorHex), CircleShape))
                Text(day.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Plain hex-string parsing (`#RRGGBB`/`#AARRGGBB`) rather than `android.graphics.Color.
 * parseColor` - keeps SplitDay's stored color a framework-independent String (also what gets
 * round-tripped through the JSON backup) without pulling in an Android framework color parser
 * just for this. */
fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val colorLong = cleaned.toLong(16)
    return if (cleaned.length == 6) Color(0xFF000000 or colorLong) else Color(colorLong)
}

package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.ui.theme.SystemBlue
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemVioletBright
import com.nightpixel.sololeveling.ui.theme.SystemYellow

/** Extracted from `HabitsScreen`'s per-habit stat tag pill (Phase 4) so the same colored chip -
 * not a plain text label - can mark which stat a screen's actions feed (user feedback,
 * 2026-08-26: "you have a color and [stat] under the habit, can you just put that there instead"
 * for Gym/Tasks/Water/Food's "Feeds X" text). */
fun statTagColor(tag: StatTag): Color = when (tag) {
    StatTag.STR -> SystemRed
    StatTag.VIT -> SystemGreen
    StatTag.DISCIPLINE -> SystemBlue
    StatTag.INT -> SystemVioletBright
    StatTag.SPIRITUALITY -> SystemYellow
}

/** Full word for the Dashboard's per-stat level bars (user feedback, 2026-08-26: "In the app have
 * the stats be abbreviated like STR is Strength but in the dashboard level bars make the full
 * word shown"). Everywhere else (this chip, the radar chart) keeps the short `StatTag.name`. */
fun statTagFullName(tag: StatTag): String = when (tag) {
    StatTag.STR -> "Strength"
    StatTag.VIT -> "Vitality"
    StatTag.DISCIPLINE -> "Discipline"
    StatTag.INT -> "Intelligence"
    StatTag.SPIRITUALITY -> "Spirituality"
}

@Composable
fun StatChip(tag: StatTag) {
    val color = statTagColor(tag)
    Surface(
        color = color.copy(alpha = 0.2f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            tag.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

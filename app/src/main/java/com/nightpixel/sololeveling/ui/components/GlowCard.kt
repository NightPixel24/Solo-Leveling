package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.ui.theme.accentGlassGradient
import com.nightpixel.sololeveling.ui.theme.accentGradient

/** A flat elevation-0 `Card` with a faint gradient tint and a thin gradient border instead of a
 * solid surface color - the app's "glass panel" replacement for the plain
 * `Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))`
 * pattern used throughout (user feedback, 2026-08-30: "make it look cooler, more modern,
 * futuristic"). Not yet swapped in everywhere - applied to the Dashboard first as the flagship
 * screen; other screens can adopt it the same way over time. */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    borderBrush: Brush = accentGradient(),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(accentGlassGradient())
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, borderBrush, shape),
        content = content
    )
}

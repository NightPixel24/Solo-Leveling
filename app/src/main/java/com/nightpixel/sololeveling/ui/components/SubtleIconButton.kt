package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Low-emphasis icon button for the affordances a screen's Edit toggle reveals - per-row delete /
 * edit icons and drag handles (user feedback, 2026-09-02: those icons "look a bit dated and are
 * too pronounced... should be visible but not the center of attention"). Keeps the full default
 * 48dp `IconButton` touch target for accessibility, but draws the glyph smaller (18dp) and in the
 * muted `onSurfaceVariant` tint rather than full-contrast `onSurface`. Pair with `Icons.Outlined.*`
 * variants at call sites for the lighter, less "solid" look. */
@Composable
fun SubtleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Just the glyph (no `IconButton` wrapper) at the same subtle size/tint - for spots that already
 * supply their own gesture modifier, like a drag handle `Box`. */
@Composable
fun SubtleIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Icon(icon, contentDescription = contentDescription, tint = tint, modifier = modifier.size(18.dp))
}

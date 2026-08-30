package com.nightpixel.sololeveling.ui.theme

import androidx.compose.ui.graphics.Brush

/** The app's one shared "system UI chrome" gradient (user feedback, 2026-08-30: "make it look
 * cooler, more modern, futuristic") - blue into cyan, deliberately distinct from every StatTag
 * color so it never reads as "this belongs to a specific stat." Used for card border glows, the
 * Rank badge's ring, and progress-bar fills - declared once here rather than re-built per call
 * site, the same reasoning `statTagColor` centralizes stat colors in one place instead of two. */
fun accentGradient(): Brush = Brush.linearGradient(listOf(SystemBlue, SystemCyan))

fun accentGradientVertical(): Brush = Brush.verticalGradient(listOf(SystemBlue, SystemCyan))

/** A very low-alpha version of the same gradient, for glass-panel card backgrounds - a faint tint
 * rather than a flat surface color, without competing with the content drawn on top of it. */
fun accentGlassGradient(): Brush = Brush.linearGradient(
    listOf(SystemBlue.copy(alpha = 0.10f), SystemCyan.copy(alpha = 0.05f))
)

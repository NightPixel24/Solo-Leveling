package com.nightpixel.sololeveling.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Always dark: this is a fixed System-window aesthetic, not a light/dark user toggle.
private val SoloLevelingColorScheme = darkColorScheme(
    primary = SystemBlue,
    onPrimary = SystemTextPrimary,
    secondary = SystemViolet,
    onSecondary = SystemTextPrimary,
    // Cyan reads as its own "system chrome" accent (see SystemCyan's doc comment) - wiring it into
    // the color scheme's tertiary slot means the handful of Material3 components that reach for
    // tertiary on their own (e.g. some indicator/track colors) pick it up for free.
    tertiary = SystemCyan,
    onTertiary = SystemBackground,
    background = SystemBackground,
    onBackground = SystemTextPrimary,
    surface = SystemSurface,
    onSurface = SystemTextPrimary,
    surfaceVariant = SystemSurfaceElevated,
    onSurfaceVariant = SystemTextSecondary,
    // Used by any Modifier.border(... MaterialTheme.colorScheme.outline ...) that doesn't want a
    // full gradient - a dim, cool-toned line rather than Material's default warm gray.
    outline = SystemBlue.copy(alpha = 0.35f),
    error = SystemRed,
    onError = SystemTextPrimary
)

// Slightly larger, more rounded than Material3's defaults (4/12/16dp) - reads more like a
// holographic panel than a stock Android card (user feedback, 2026-08-30: "make it look cooler,
// more modern, futuristic"). Every Card/Button/Dialog in the app already relies on these defaults
// rather than passing an explicit shape, so this alone reshapes the whole app for free.
private val SoloLevelingShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SoloLevelingTheme(
    // Reserved for a future manual override; the app itself has no light mode.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoloLevelingColorScheme,
        typography = SystemTypography,
        shapes = SoloLevelingShapes,
        content = content
    )
}

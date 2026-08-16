package com.nightpixel.sololeveling.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Always dark: this is a fixed System-window aesthetic, not a light/dark user toggle.
private val SoloLevelingColorScheme = darkColorScheme(
    primary = SystemBlue,
    onPrimary = SystemTextPrimary,
    secondary = SystemViolet,
    onSecondary = SystemTextPrimary,
    background = SystemBackground,
    onBackground = SystemTextPrimary,
    surface = SystemSurface,
    onSurface = SystemTextPrimary,
    surfaceVariant = SystemSurfaceElevated,
    onSurfaceVariant = SystemTextSecondary,
    error = SystemRed,
    onError = SystemTextPrimary
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
        content = content
    )
}

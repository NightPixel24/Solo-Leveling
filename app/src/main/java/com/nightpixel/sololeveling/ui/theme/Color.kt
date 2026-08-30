package com.nightpixel.sololeveling.ui.theme

import androidx.compose.ui.graphics.Color

// System-window palette: near-black background, electric blue + violet accents.
// Deepened slightly (was 0A0A12/13131F/1B1B2C) for more contrast between background and the
// "glass panel" cards sitting on it - user feedback, 2026-08-30: "make it look cooler, more
// modern, futuristic".
val SystemBackground = Color(0xFF07070D)
val SystemSurface = Color(0xFF121220)
val SystemSurfaceElevated = Color(0xFF1C1C30)

val SystemBlue = Color(0xFF3D7BFF)
val SystemBlueBright = Color(0xFF6FA0FF)
val SystemViolet = Color(0xFF8B5CF6)
val SystemVioletBright = Color(0xFFA78BFA)

// A fresh accent, deliberately not used by any StatTag color (red/green/blue/violet/yellow are
// all already spoken for) - the one color reserved for "system chrome" itself: card border glows,
// the Rank badge's ring, progress-bar highlights. Paired with SystemBlue as a blue->cyan gradient
// (see Gradients.kt) for the app's holographic-HUD accent.
val SystemCyan = Color(0xFF22D3EE)

val SystemTextPrimary = Color(0xFFECECF5)
val SystemTextSecondary = Color(0xFF9797B0)

val SystemGreen = Color(0xFF3DDC84)
val SystemYellow = Color(0xFFF5C242)
val SystemRed = Color(0xFFE5484D)

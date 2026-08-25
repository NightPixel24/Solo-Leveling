package com.nightpixel.sololeveling.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/** Screens reachable from the bottom nav (spec Section 8). Settings and the
 * Rank/Goals detail screen are reached from the Dashboard, not this bar. */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Dashboard("dashboard", "Home", Icons.Filled.Shield),
    Calendar("calendar", "Calendar", Icons.Filled.CalendarMonth),
    Tasks("tasks", "Tasks", Icons.Filled.CheckCircle),
    Habits("habits", "Habits", Icons.Filled.Repeat),
    Gym("gym", "Gym", Icons.Filled.FitnessCenter),
    Life("life", "Life", Icons.Filled.Restaurant),
    Rewards("rewards", "Rewards", Icons.Filled.CardGiftcard)
}

object Routes {
    const val SETTINGS = "settings"
    const val GOALS = "goals"
    const val PUNISHMENTS = "punishments"
}

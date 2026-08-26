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

/** Screens reachable from the bottom nav (spec Section 8, since refined by user feedback down to
 * 5 items with Home centered - Calendar and Rewards moved to Dashboard top-bar icons instead,
 * the same "reached from Dashboard, not this bar" treatment Settings/Goals/Punishments already
 * had). Declaration order is display order, so Dashboard sits third (visually centered) rather
 * than first. */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Tasks("tasks", "Tasks", Icons.Filled.CheckCircle),
    Habits("habits", "Habits", Icons.Filled.Repeat),
    Dashboard("dashboard", "Home", Icons.Filled.Shield),
    Gym("gym", "Gym", Icons.Filled.FitnessCenter),
    Life("life", "Life", Icons.Filled.Restaurant)
}

object Routes {
    const val SETTINGS = "settings"
    const val GOALS = "goals"
    const val PUNISHMENTS = "punishments"
    const val CALENDAR = "calendar"
    const val REWARDS = "rewards"
}

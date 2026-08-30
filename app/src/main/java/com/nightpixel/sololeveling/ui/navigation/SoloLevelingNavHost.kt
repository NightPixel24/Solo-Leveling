package com.nightpixel.sololeveling.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nightpixel.sololeveling.ui.components.SoloLevelingBottomNavBar
import com.nightpixel.sololeveling.ui.components.navigateToBottomNav
import com.nightpixel.sololeveling.ui.screens.CalendarScreen
import com.nightpixel.sololeveling.ui.screens.DashboardScreen
import com.nightpixel.sololeveling.ui.screens.GoalsScreen
import com.nightpixel.sololeveling.ui.screens.GymScreen
import com.nightpixel.sololeveling.ui.screens.LifeScreen
import com.nightpixel.sololeveling.ui.screens.PunishmentScreen
import com.nightpixel.sololeveling.ui.screens.RewardsScreen
import com.nightpixel.sololeveling.ui.screens.RoutineScreen
import com.nightpixel.sololeveling.ui.screens.SettingsScreen
import com.nightpixel.sololeveling.ui.screens.TasksScreen

@Composable
fun SoloLevelingApp(navController: NavHostController = rememberNavController()) {
    Scaffold(
        bottomBar = { SoloLevelingBottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavDestination.Dashboard.route) {
                DashboardScreen(
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onGoalsClick = { navController.navigate(Routes.GOALS) },
                    onPunishmentsClick = { navController.navigate(Routes.PUNISHMENTS) },
                    onCalendarClick = { navController.navigate(Routes.CALENDAR) },
                    onRewardsClick = { navController.navigate(Routes.REWARDS) },
                    onMoodClick = { navController.navigateToBottomNav(BottomNavDestination.Life) },
                    onGymClick = { navController.navigateToBottomNav(BottomNavDestination.Gym) }
                )
            }
            composable(BottomNavDestination.Tasks.route) { TasksScreen() }
            composable(BottomNavDestination.Routine.route) { RoutineScreen() }
            composable(BottomNavDestination.Gym.route) { GymScreen() }
            composable(BottomNavDestination.Life.route) { LifeScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.GOALS) { GoalsScreen() }
            composable(Routes.PUNISHMENTS) { PunishmentScreen() }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.REWARDS) { RewardsScreen() }
        }
    }
}

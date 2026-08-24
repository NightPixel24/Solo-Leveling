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
import com.nightpixel.sololeveling.ui.screens.CalendarScreen
import com.nightpixel.sololeveling.ui.screens.DashboardScreen
import com.nightpixel.sololeveling.ui.screens.GoalsScreen
import com.nightpixel.sololeveling.ui.screens.GymScreen
import com.nightpixel.sololeveling.ui.screens.HabitsScreen
import com.nightpixel.sololeveling.ui.screens.LifeScreen
import com.nightpixel.sololeveling.ui.screens.RewardsScreen
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
                    onGoalsClick = { navController.navigate(Routes.GOALS) }
                )
            }
            composable(BottomNavDestination.Calendar.route) { CalendarScreen() }
            composable(BottomNavDestination.Tasks.route) { TasksScreen() }
            composable(BottomNavDestination.Habits.route) { HabitsScreen() }
            composable(BottomNavDestination.Gym.route) { GymScreen() }
            composable(BottomNavDestination.Life.route) { LifeScreen() }
            composable(BottomNavDestination.Rewards.route) { RewardsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.GOALS) { GoalsScreen() }
        }
    }
}

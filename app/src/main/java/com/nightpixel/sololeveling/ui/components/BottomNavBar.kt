package com.nightpixel.sololeveling.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nightpixel.sololeveling.ui.navigation.BottomNavDestination

@Composable
fun SoloLevelingBottomNavBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar {
        BottomNavDestination.entries.forEach { destination ->
            val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    // popBackStack first: navigate()'s popUpTo+launchSingleTop+restoreState combo
                    // is a documented no-op when the target is the graph's start destination and
                    // it's already sitting (not on top) in the back stack - e.g. returning to
                    // Dashboard from Goals/Settings, which are pushed outside this bottom-nav
                    // graph. Popping directly back to an existing entry always works; only fall
                    // back to navigate() when the destination isn't already in the back stack.
                    val alreadyInBackStack = navController.popBackStack(destination.route, inclusive = false)
                    if (!alreadyInBackStack) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = {
                    BasicText(
                        text = destination.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = LocalContentColor.current,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize
                        )
                    )
                }
            )
        }
    }
}

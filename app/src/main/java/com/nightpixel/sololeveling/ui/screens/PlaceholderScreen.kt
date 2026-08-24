package com.nightpixel.sololeveling.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Shared placeholder for modules not yet built (spec Section 10 build order). */
@Composable
fun PlaceholderScreen(title: String) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$title\n(coming soon)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LifeScreen() = PlaceholderScreen("Life (Mood / Food / Water)")

@Composable
fun RewardsScreen() = PlaceholderScreen("Rewards")

@Composable
fun GoalsScreen() = PlaceholderScreen("Life Goals & Rank")

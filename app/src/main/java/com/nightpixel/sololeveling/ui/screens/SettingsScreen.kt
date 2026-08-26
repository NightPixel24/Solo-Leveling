package com.nightpixel.sololeveling.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.ui.theme.SystemRed
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWipeConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { app.backupManager.exportTo(context, uri) }
                .onSuccess { snackbarHostState.showSnackbar("Backup exported") }
                .onFailure { snackbarHostState.showSnackbar("Export failed: ${it.message}") }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { app.backupManager.importFrom(context, uri) }
                .onSuccess { snackbarHostState.showSnackbar("Backup imported") }
                .onFailure { snackbarHostState.showSnackbar("Import failed: ${it.message}") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Backup", style = MaterialTheme.typography.titleLarge)
            Text(
                "Export a full JSON backup of your data, or restore from a previous export. " +
                    "Do this before installing any update that changes core logic.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { exportLauncher.launch(defaultBackupFileName()) }) {
                Text("Export Backup")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                Text("Import Backup")
            }

            HorizontalDivider()

            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            Text(
                "Habit, water, mood, gym-day, and review reminders (spec Section 7) are " +
                    "scheduled automatically. Mute or tune individual reminder types from the " +
                    "system notification settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }) {
                Text("Notification Settings")
            }

            HorizontalDivider()

            Text("Danger Zone", style = MaterialTheme.typography.titleLarge, color = SystemRed)
            Text(
                "Wipes every table back to a fresh-install state (default task list, 5 stats at " +
                    "level 1, zero Gold) and deletes logged food photos. For repeatedly starting " +
                    "clean during testing - export a backup first if this data matters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showWipeConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemRed)
            ) {
                Text("Clear All App Data")
            }

            HorizontalDivider()

            Text("About", style = MaterialTheme.typography.titleLarge)
            Text(
                "Solo Leveling v${appVersionName(context)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Clear all app data?") },
            text = { Text("This permanently deletes every task, habit, gym log, stat, goal, and everything else on this device. This can't be undone unless you've exported a backup.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirm = false
                        scope.launch {
                            runCatching { app.backupManager.wipeAll(context) }
                                .onSuccess { snackbarHostState.showSnackbar("All app data cleared") }
                                .onFailure { snackbarHostState.showSnackbar("Clear failed: ${it.message}") }
                        }
                    }
                ) { Text("Clear Everything", color = SystemRed) }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun defaultBackupFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").format(LocalDateTime.now())
    return "solo-leveling-backup-$stamp.json"
}

private fun appVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

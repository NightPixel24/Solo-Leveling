package com.nightpixel.sololeveling.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.StatTag
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun FoodScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val foodDao = remember { app.database.foodDao() }
    val xpEngine = remember { app.xpEngine }
    val scope = rememberCoroutineScope()

    val entries by foodDao.observeEntries().collectAsState(initial = emptyList())
    val grouped = remember(entries) { entries.groupBy { it.date } }
    val sortedDates = remember(grouped) { grouped.keys.sortedDescending() }

    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingPhotoFile
        pendingPhotoFile = null
        if (success && file != null) {
            capturedPhotoFile = file
        } else {
            file?.delete()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val file = createPhotoFile(context)
                pendingPhotoFile = file
                cameraLauncher.launch(photoFileUri(context, file))
            }) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Log food")
            }
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No food logged yet - tap the camera to add one",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sortedDates.forEach { date ->
                    item(key = "header-$date") {
                        Text(
                            formatHeaderDate(date),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(grouped.getValue(date), key = { it.id }) { entry ->
                        FoodRow(entry = entry, onDelete = { scope.launch { foodDao.deleteEntry(entry) } })
                    }
                }
            }
        }
    }

    capturedPhotoFile?.let { file ->
        ConfirmFoodDialog(
            photoUri = photoFileUri(context, file),
            onDismiss = {
                file.delete()
                capturedPhotoFile = null
            },
            onSave = { description ->
                scope.launch {
                    foodDao.insertEntry(
                        FoodLogEntry(
                            date = LocalDate.now().toString(),
                            photoUri = photoFileUri(context, file).toString(),
                            description = description
                        )
                    )
                    xpEngine.grant(StatTag.VIT, 5, "Food logged")
                }
                capturedPhotoFile = null
            }
        )
    }
}

@Composable
private fun FoodRow(entry: FoodLogEntry, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = entry.photoUri,
                contentDescription = entry.description,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f)) {
                Text(entry.description.ifBlank { "(no description)" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
                        .toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmFoodDialog(
    photoUri: Uri,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What was it?") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(description.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Discard") }
        }
    )
}

private fun formatHeaderDate(date: String): String {
    val localDate = LocalDate.parse(date)
    val today = LocalDate.now()
    return when (localDate) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

private fun createPhotoFile(context: Context): File {
    val dir = File(context.filesDir, "food_photos").apply { mkdirs() }
    return File(dir, "food_${System.currentTimeMillis()}.jpg")
}

private fun photoFileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

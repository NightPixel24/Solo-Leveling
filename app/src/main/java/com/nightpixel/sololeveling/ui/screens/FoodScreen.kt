package com.nightpixel.sololeveling.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.FoodRating
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.data.gamification.applyVitalityMultiplier
import com.nightpixel.sololeveling.ui.components.StatChip
import com.nightpixel.sololeveling.ui.theme.SystemGreen
import com.nightpixel.sololeveling.ui.theme.SystemRed
import com.nightpixel.sololeveling.ui.theme.SystemYellow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
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
    var showManualEntry by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text("Food") },
                actions = {
                    IconButton(onClick = { showManualEntry = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Log without a photo")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val file = createPhotoFile(context)
                pendingPhotoFile = file
                cameraLauncher.launch(photoFileUri(context, file))
            }) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "Log food with a photo")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StatChip(StatTag.VIT)
            }
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No food logged yet - tap the camera to add one",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
    }

    capturedPhotoFile?.let { file ->
        ConfirmFoodDialog(
            photoUri = photoFileUri(context, file),
            onDismiss = {
                file.delete()
                capturedPhotoFile = null
            },
            onSave = { description, rating ->
                scope.launch {
                    logFood(foodDao, xpEngine, photoFileUri(context, file).toString(), description, rating)
                }
                capturedPhotoFile = null
            }
        )
    }

    if (showManualEntry) {
        ConfirmFoodDialog(
            photoUri = null,
            onDismiss = { showManualEntry = false },
            onSave = { description, rating ->
                scope.launch { logFood(foodDao, xpEngine, null, description, rating) }
                showManualEntry = false
            }
        )
    }
}

/** Not private - reused by the Dashboard's food quick-add (spec Section 6) so both places share
 * the exact same save-and-grant-XP logic. Applies the low-vitality debuff (user feedback,
 * 2026-08-26) to the flat +5 VIT food-logged grant, same as every other VIT source. */
suspend fun logFood(
    foodDao: FoodDao,
    xpEngine: XpEngine,
    photoUri: String?,
    description: String,
    rating: FoodRating
) {
    foodDao.insertEntry(
        FoodLogEntry(
            date = LocalDate.now().toString(),
            photoUri = photoUri,
            description = description,
            rating = rating
        )
    )
    xpEngine.grant(StatTag.VIT, applyVitalityMultiplier(foodDao, 5), "Food logged")
}

@Composable
private fun FoodRow(entry: FoodLogEntry, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (entry.photoUri != null) {
                AsyncImage(
                    model = entry.photoUri,
                    contentDescription = entry.description,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.description.ifBlank { "(no description)" }, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FoodRatingChipLabel(entry.rating)
                    Text(
                        Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
                            .toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

fun foodRatingColor(rating: FoodRating): Color = when (rating) {
    FoodRating.HEALTHY -> SystemGreen
    FoodRating.OK -> SystemYellow
    FoodRating.UNHEALTHY -> SystemRed
}

fun foodRatingLabel(rating: FoodRating): String = when (rating) {
    FoodRating.HEALTHY -> "Healthy"
    FoodRating.OK -> "OK"
    FoodRating.UNHEALTHY -> "Unhealthy"
}

@Composable
private fun FoodRatingChipLabel(rating: FoodRating) {
    Text(
        foodRatingLabel(rating),
        style = MaterialTheme.typography.labelSmall,
        color = foodRatingColor(rating)
    )
}

/** Not private - reused by the Dashboard's food quick-add (spec Section 6), so both places share
 * the exact same capture/type-confirm-save flow instead of a second copy of it. `photoUri` is
 * null for the manual "log without a photo" path (user feedback, 2026-08-26). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmFoodDialog(
    photoUri: Uri?,
    onDismiss: () -> Unit,
    onSave: (description: String, rating: FoodRating) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(FoodRating.OK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Food") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What was it?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodRating.entries.forEach { r ->
                        FilterChip(
                            selected = rating == r,
                            onClick = { rating = r },
                            label = { Text(foodRatingLabel(r)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(description.trim(), rating) }) { Text("Save") }
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

fun createPhotoFile(context: Context): File {
    val dir = File(context.filesDir, "food_photos").apply { mkdirs() }
    return File(dir, "food_${System.currentTimeMillis()}.jpg")
}

fun photoFileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

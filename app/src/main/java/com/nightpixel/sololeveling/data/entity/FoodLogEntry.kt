package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Simple chronological photo log (spec Section 4.6) - no calorie estimation for v1.
 * `photoUri` is a content:// URI from this app's own FileProvider, pointing at a JPEG under
 * this app's internal files dir (see FileProviderPaths / food_photos/). */
@Serializable
@Entity(tableName = "food_log_entries")
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoUri: String,
    val description: String
)

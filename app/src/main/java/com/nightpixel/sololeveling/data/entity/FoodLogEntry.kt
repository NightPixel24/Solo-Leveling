package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Healthy/OK/Unhealthy self-rating (user feedback, 2026-08-26) - same three-tier shape as
 * [MoodColor] so the Analytics tab can reuse the same distribution-bar treatment. Existing rows
 * from before this field existed default to OK on migration (a neutral read, not a judgment). */
enum class FoodRating { HEALTHY, OK, UNHEALTHY }

/** Simple chronological log (spec Section 4.6) - no calorie estimation for v1. `photoUri` is a
 * content:// URI from this app's own FileProvider pointing at a JPEG under this app's internal
 * files dir (see FileProviderPaths / food_photos/) - nullable since a user-requested "log it
 * without a photo" path (2026-08-26) means not every entry has one. */
@Serializable
@Entity(tableName = "food_log_entries")
data class FoodLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    val description: String,
    val rating: FoodRating = FoodRating.OK
)

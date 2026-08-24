package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class MoodColor { GOOD, OK, BAD }

/** One row per calendar day (spec Section 4.5) - `date` (ISO "yyyy-MM-dd", same convention as
 * HabitLog/GymSession) is the primary key since there's only ever one rating per day. */
@Serializable
@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey val date: String,
    val color: MoodColor,
    val note: String = ""
)

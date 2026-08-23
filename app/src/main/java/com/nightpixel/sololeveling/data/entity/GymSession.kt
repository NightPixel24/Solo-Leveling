package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One row per (exercise, calendar day) it was logged on - same date-string
 * convention as HabitLog. Only the fields relevant to the exercise's type
 * are ever populated (actualSets/Reps/Weight for Strength, actualDuration/
 * intensity for Cardio/Sport). */
@Serializable
@Entity(
    tableName = "gym_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId", "date"], unique = true)]
)
data class GymSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val date: String,
    val actualSets: Int? = null,
    val actualReps: Int? = null,
    val actualWeight: Double? = null,
    val actualDuration: Int? = null,
    val intensity: Int? = null
)

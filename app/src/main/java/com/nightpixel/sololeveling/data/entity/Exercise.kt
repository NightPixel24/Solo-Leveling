package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Both types feed STR (spec Section 5.1 originally split Strength->STR, Cardio/Sport->AGILITY,
 * but AGILITY was later dropped per user feedback - both exercise types read as "the same gym
 * grind" to the user) - showing up on either type still feeds DISCIPLINE, handled once XP exists
 * (Phase 10). */
enum class ExerciseType { STRENGTH, CARDIO_SPORT }

/** Pinned to a user-defined [SplitDay] ("Day 1", "Day 2", ...) rather than a fixed weekday (user
 * feedback, 2026-08-26) - missing a day no longer pushes the rest of the split onto the wrong
 * calendar day, since there's no calendar day baked into the routine at all. Cascades on its
 * split day's deletion, same as GymSession already cascades on this entity's deletion. */
@Serializable
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = SplitDay::class,
            parentColumns = ["id"],
            childColumns = ["splitDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("splitDayId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val splitDayId: Long,
    val type: ExerciseType = ExerciseType.STRENGTH,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeight: Double? = null,
    val targetDuration: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

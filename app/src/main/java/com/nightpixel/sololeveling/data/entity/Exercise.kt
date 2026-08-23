package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Strength feeds STR, Cardio/Sport feeds AGILITY (spec Section 5.1) - showing
 * up on either type still feeds DISCIPLINE, handled once XP exists (Phase 10). */
enum class ExerciseType { STRENGTH, CARDIO_SPORT }

@Serializable
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 1=Monday..7=Sunday (java.time.DayOfWeek.value) - stored as a plain Int
     * rather than the DayOfWeek type itself so this needs no custom Room
     * TypeConverter or kotlinx.serialization serializer. */
    val dayOfWeek: Int = 1,
    val type: ExerciseType = ExerciseType.STRENGTH,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeight: Double? = null,
    val targetDuration: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

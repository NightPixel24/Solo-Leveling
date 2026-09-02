package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A user-defined workout-split day ("Day 1", "Day 2 - Chest and Shoulders", ...) rather than a
 * fixed weekday (user feedback, 2026-08-26: missing a day and working out the next day used to
 * push every subsequent exercise onto the wrong weekday header). `colorHex` drives both this
 * day's chip and the workout calendar's per-day color coding. `orderIndex` is the user's chosen
 * split order (Day 1, Day 2, ...), independent of creation order. `isRest` (user feedback,
 * 2026-09-02) marks this as a rest day rather than a workout: it still has a name and color and
 * lists on the Workouts tab like any other, but has no [Exercise]s attached and is ticked off per
 * date into [RestDayLog] instead of logging [GymSession]s. */
@Serializable
@Entity(tableName = "split_days")
data class SplitDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val orderIndex: Int,
    val isRest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Rotated across days during migration/creation so a fresh split doesn't start
         * monochrome; the user can always recolor a day individually afterward. */
        val COLOR_PALETTE = listOf(
            "#E5484D", "#F5C242", "#3DDC84", "#3D7BFF",
            "#8B5CF6", "#FF8C42", "#42C2FF", "#FF6EC7"
        )
    }
}

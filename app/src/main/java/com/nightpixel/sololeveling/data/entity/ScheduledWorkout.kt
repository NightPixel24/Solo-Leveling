package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One row per weekday (1=Monday..7=Sunday, `java.time.DayOfWeek.value`) mapping it to the
 * [SplitDay] ("Workout" in the UI, since the Gym screen's tab renamed from Routine - user
 * feedback, 2026-08-30) planned for that day of the week - the Gym screen's new Routine tab. This
 * is deliberately just a recurring weekly *plan* with no date of its own, unlike the old fixed-
 * weekday `Exercise.dayOfWeek` this codebase already removed once (MIGRATION_14_15): the Calendar
 * tab's actual history stays independently derived from GymSession data and never reads this
 * table, so missing a planned day here can't corrupt anything else. `dayOfWeek` as the primary key
 * means "assign a workout to Monday" is a plain upsert (REPLACE) and "clear Monday" is a delete by
 * dayOfWeek - at most one planned workout per weekday. FK+cascade to SplitDay so deleting a
 * workout automatically un-schedules any day using it. */
@Serializable
@Entity(
    tableName = "scheduled_workouts",
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
data class ScheduledWorkout(
    @PrimaryKey val dayOfWeek: Int,
    val splitDayId: Long
)

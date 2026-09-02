package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A checkable free-text line under a rest-day [SplitDay] (user feedback, 2026-09-02: a rest day
 * "should still have a drop down header similar to the other workouts design, its just that when
 * adding things under it dont show the exercise modal show a text box"; then "the checkbox should
 * only be for added items, not in the header... when you add an item as per normal it should have
 * the checkbox there"). These are the rest day's *definition* - what that kind of rest involves
 * ("light stretching", "20 min walk") - the same role [Exercise]s play under a normal workout,
 * just plain text with no sets/reps/targets. `completedDate` (ISO date string, null = not done)
 * holds this item's own daily checkoff exactly like [RoutineItem.completedDate] - checking it sets
 * today's date, and comparing that against "today" on read makes it reset each day with no cron
 * job. Ticking any of a rest day's items for a date is what records that rest day onto the
 * calendar (see [RestDayLog], maintained by GymScreen on each toggle). Cascades on the rest
 * [SplitDay]'s deletion. */
@Serializable
@Entity(
    tableName = "rest_day_notes",
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
data class RestDayNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitDayId: Long,
    val text: String,
    val completedDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

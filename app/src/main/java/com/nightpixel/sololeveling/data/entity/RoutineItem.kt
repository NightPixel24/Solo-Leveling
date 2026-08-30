package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** The four blocks a day gets carved into on the Routine tab's Schedule sub-tab (user feedback,
 * 2026-08-30: "I can map out my day the morning, day, afternoon and night"). Declared in
 * chronological order so grouping by this enum's natural ordinal sorts correctly with no separate
 * sort key needed. */
enum class DayPart(val label: String) {
    MORNING("Morning"), DAY("Day"), AFTERNOON("Afternoon"), NIGHT("Night")
}

/** A single slot on the Routine tab's day-planner schedule (user feedback, 2026-08-30: "I can add
 * routine items under those sections and I can also slot my habits into my schedule so that they
 * fit into my day rather than doing them at random times"). Two shapes share this one table rather
 * than a sealed hierarchy Room can't map directly: a free-text plan item (`habitId` null, `title`
 * holds the text) or a slotted-in existing [Habit] (`habitId` set - `title` is unused and left
 * blank, since the habit's own title is looked up live so a rename elsewhere shows up here too,
 * same "derive, don't store a second copy" reasoning as this codebase's other screens). No
 * completion state of its own: a slotted habit's checkbox on the Schedule tab reads/writes the
 * exact same [HabitLog] the Habits tab already tracks, and a free-text item is just a plan, not a
 * second thing to check off. `reminderTime` (minutes since midnight, same shape as
 * [Habit.reminderTime]) is optional - when set, [com.nightpixel.sololeveling.notifications.
 * RoutineReminderWorker] fires a notification at that specific time (user feedback, 2026-08-30:
 * "if there are any time specific things in my schedule, I should also be notified... at nine PM
 * it might say take my tablets"), independent of any per-habit reminder a slotted habit might
 * separately have - the two are different concepts (a habit's own "time to do this" nudge vs. this
 * item's place in the day plan) and can both legitimately fire. */
@Serializable
@Entity(
    tableName = "routine_items",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId")]
)
data class RoutineItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayPart: DayPart,
    val title: String = "",
    val habitId: Long? = null,
    val reminderTime: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

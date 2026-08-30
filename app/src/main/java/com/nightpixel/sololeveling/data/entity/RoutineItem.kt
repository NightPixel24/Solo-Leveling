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
 * second thing to check off. */
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
    val createdAt: Long = System.currentTimeMillis()
)

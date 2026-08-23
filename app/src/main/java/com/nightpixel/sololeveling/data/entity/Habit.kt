package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class HabitFrequency { DAILY, WEEKLY }

/** Which Stat (spec Section 5.1) this habit feeds once the gamification core
 * (Phase 10) exists to actually grant XP against it. */
enum class StatTag { STR, VIT, DISCIPLINE, INT, AGILITY }

@Serializable
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    /** Only meaningful when frequency == WEEKLY, e.g. "3x/week". */
    val targetPerWeek: Int = 3,
    val statTag: StatTag = StatTag.DISCIPLINE,
    /** Minutes since midnight, local time; null = no reminder set. Notification
     * scheduling itself is Phase 16 - this just captures the chosen time. */
    val reminderTime: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.nightpixel.sololeveling.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class HabitWithLogs(
    @Embedded val habit: Habit,
    @Relation(parentColumn = "id", entityColumn = "habitId")
    val logs: List<HabitLog>
)

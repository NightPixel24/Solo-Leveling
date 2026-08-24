package com.nightpixel.sololeveling.data.backup

import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import com.nightpixel.sololeveling.data.entity.Exercise
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GymSession
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitLog
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import com.nightpixel.sololeveling.data.entity.WaterLog
import kotlinx.serialization.Serializable

/**
 * Full JSON export/import shape (spec Section 3). One field per table -
 * extend this every time a new module adds a table, and default any new
 * field so an older backup still loads after the schema evolves.
 */
@Serializable
data class BackupData(
    val schemaVersion: Int,
    val exportedAt: String,
    val appMeta: AppMeta? = null,
    val taskLists: List<TaskList> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val gymSessions: List<GymSession> = emptyList(),
    val calendarEvents: List<CalendarEventCache> = emptyList(),
    val moodEntries: List<MoodEntry> = emptyList(),
    val foodLogEntries: List<FoodLogEntry> = emptyList(),
    val waterLogs: List<WaterLog> = emptyList(),
    val goals: List<Goal> = emptyList()
)

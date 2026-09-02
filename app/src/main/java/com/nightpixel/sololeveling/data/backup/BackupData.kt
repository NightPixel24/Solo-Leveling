package com.nightpixel.sololeveling.data.backup

import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.BodyStatEntry
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import com.nightpixel.sololeveling.data.entity.Exercise
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GymSession
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitLog
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.PlayerProfile
import com.nightpixel.sololeveling.data.entity.PunishmentAssignment
import com.nightpixel.sololeveling.data.entity.PunishmentPoolItem
import com.nightpixel.sololeveling.data.entity.RestDayLog
import com.nightpixel.sololeveling.data.entity.RestDayNote
import com.nightpixel.sololeveling.data.entity.RewardInventoryItem
import com.nightpixel.sololeveling.data.entity.RewardPoolItem
import com.nightpixel.sololeveling.data.entity.RoutineItem
import com.nightpixel.sololeveling.data.entity.ScheduledWorkout
import com.nightpixel.sololeveling.data.entity.SplitDay
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.data.entity.XpLog
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
    val splitDays: List<SplitDay> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val gymSessions: List<GymSession> = emptyList(),
    val calendarEvents: List<CalendarEventCache> = emptyList(),
    val moodEntries: List<MoodEntry> = emptyList(),
    val foodLogEntries: List<FoodLogEntry> = emptyList(),
    val waterLogs: List<WaterLog> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val stats: List<Stat> = emptyList(),
    val xpLogs: List<XpLog> = emptyList(),
    val punishmentPoolItems: List<PunishmentPoolItem> = emptyList(),
    val punishmentAssignments: List<PunishmentAssignment> = emptyList(),
    val rewardPoolItems: List<RewardPoolItem> = emptyList(),
    val rewardInventory: List<RewardInventoryItem> = emptyList(),
    val playerProfile: PlayerProfile? = null,
    val routineItems: List<RoutineItem> = emptyList(),
    val bodyStatEntries: List<BodyStatEntry> = emptyList(),
    val scheduledWorkouts: List<ScheduledWorkout> = emptyList(),
    val restDayLogs: List<RestDayLog> = emptyList(),
    val restDayNotes: List<RestDayNote> = emptyList()
)

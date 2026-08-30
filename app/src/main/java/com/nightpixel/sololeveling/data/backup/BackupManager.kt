package com.nightpixel.sololeveling.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.nightpixel.sololeveling.data.AppDatabase
import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.TaskList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

class BackupManager(private val database: AppDatabase) {

    suspend fun exportTo(context: Context, uri: Uri) {
        val backup = BackupData(
            schemaVersion = AppDatabase.CURRENT_SCHEMA_VERSION,
            exportedAt = Instant.now().toString(),
            appMeta = database.appMetaDao().observe().firstOrNull(),
            taskLists = database.taskListDao().getAllListsOnce(),
            tasks = database.taskDao().getAllTasksOnce(),
            subtasks = database.taskDao().getAllSubtasksOnce(),
            habits = database.habitDao().getAllHabitsOnce(),
            habitLogs = database.habitDao().getAllLogsOnce(),
            splitDays = database.splitDayDao().getAllOnce(),
            exercises = database.gymDao().getAllExercisesOnce(),
            gymSessions = database.gymDao().getAllSessionsOnce(),
            calendarEvents = database.calendarDao().getAllEventsOnce(),
            moodEntries = database.moodDao().getAllOnce(),
            foodLogEntries = database.foodDao().getAllOnce(),
            waterLogs = database.waterDao().getAllOnce(),
            goals = database.goalDao().getAllOnce(),
            stats = database.statDao().getAllStatsOnce(),
            xpLogs = database.statDao().getAllXpLogsOnce(),
            punishmentPoolItems = database.punishmentDao().getAllItemsOnce(),
            punishmentAssignments = database.punishmentDao().getAllAssignmentsOnce(),
            rewardPoolItems = database.rewardDao().getAllPoolItemsOnce(),
            rewardInventory = database.rewardDao().getAllInventoryOnce(),
            playerProfile = database.playerProfileDao().getOnce(),
            routineItems = database.routineDao().getAllOnce(),
            bodyStatEntries = database.healthDao().getAllOnce(),
            scheduledWorkouts = database.scheduledWorkoutDao().getAllOnce()
        )
        val json = backupJson.encodeToString(BackupData.serializer(), backup)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Could not open $uri for writing")
    }

    suspend fun importFrom(context: Context, uri: Uri) {
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Could not open $uri for reading")

        restore(backupJson.decodeFromString(BackupData.serializer(), json))
    }

    /** Wipes every table back to the same empty/seeded state a fresh install starts from -
     * reuses [restore]'s "missing fields get seeded" behavior (default task list, 5 stats at
     * level 1) by just handing it an all-empty [BackupData], rather than duplicating that seed
     * logic a second time. Also deletes food photo files from internal
     * storage, since those live on disk outside Room and would otherwise be orphaned. Meant for
     * repeatedly starting fresh during testing - export first if the data matters. */
    suspend fun wipeAll(context: Context) {
        restore(BackupData(schemaVersion = AppDatabase.CURRENT_SCHEMA_VERSION, exportedAt = Instant.now().toString()))
        File(context.filesDir, "food_photos").listFiles()?.forEach { it.delete() }
    }

    private suspend fun restore(backup: BackupData) {
        database.withTransaction {
            backup.appMeta?.let { database.appMetaDao().upsert(it) }
                ?: database.appMetaDao().upsert(AppMeta(schemaVersion = backup.schemaVersion))

            database.taskListDao().clearLists()
            database.taskListDao().insertLists(
                backup.taskLists.ifEmpty {
                    listOf(TaskList(id = TaskList.DEFAULT_ID, name = "Daily", position = -1, isProtected = true))
                }
            )
            database.taskDao().replaceAll(backup.tasks, backup.subtasks)
            database.habitDao().replaceAll(backup.habits, backup.habitLogs)
            // Habits must already exist before this - routine_items.habitId FK-references them.
            database.routineDao().replaceAll(backup.routineItems)
            // Split days first - exercises FK-reference them, so they must already exist before
            // gymDao's own replaceAll inserts exercises pointing at their ids.
            database.splitDayDao().replaceAll(backup.splitDays)
            database.gymDao().replaceAll(backup.exercises, backup.gymSessions)
            database.calendarDao().replaceAll(backup.calendarEvents)
            database.moodDao().replaceAll(backup.moodEntries)
            database.foodDao().replaceAll(backup.foodLogEntries)
            database.waterDao().replaceAll(backup.waterLogs)
            database.goalDao().replaceAll(backup.goals)
            database.statDao().replaceAll(
                backup.stats.ifEmpty { StatTag.entries.map { Stat(tag = it) } },
                backup.xpLogs
            )
            database.punishmentDao().replaceAll(backup.punishmentPoolItems, backup.punishmentAssignments)
            database.rewardDao().replaceAll(backup.rewardPoolItems, backup.rewardInventory)
            database.playerProfileDao().replaceAll(backup.playerProfile)
            database.healthDao().replaceAll(backup.bodyStatEntries)
        }
    }
}

package com.nightpixel.sololeveling.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.nightpixel.sololeveling.data.AppDatabase
import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.TaskList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
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
            exercises = database.gymDao().getAllExercisesOnce(),
            gymSessions = database.gymDao().getAllSessionsOnce(),
            calendarEvents = database.calendarDao().getAllEventsOnce(),
            moodEntries = database.moodDao().getAllOnce()
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

        val backup = backupJson.decodeFromString(BackupData.serializer(), json)
        database.withTransaction {
            backup.appMeta?.let { database.appMetaDao().upsert(it) }
                ?: database.appMetaDao().upsert(AppMeta(schemaVersion = backup.schemaVersion))

            database.taskListDao().clearLists()
            database.taskListDao().insertLists(
                backup.taskLists.ifEmpty { listOf(TaskList(id = TaskList.DEFAULT_ID, name = "Tasks")) }
            )
            database.taskDao().replaceAll(backup.tasks, backup.subtasks)
            database.habitDao().replaceAll(backup.habits, backup.habitLogs)
            database.gymDao().replaceAll(backup.exercises, backup.gymSessions)
            database.calendarDao().replaceAll(backup.calendarEvents)
            database.moodDao().replaceAll(backup.moodEntries)
        }
    }
}

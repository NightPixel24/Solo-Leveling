package com.nightpixel.sololeveling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nightpixel.sololeveling.data.dao.AppMetaDao
import com.nightpixel.sololeveling.data.dao.TaskDao
import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task

/**
 * Every schema change here must ship with an explicit Room Migration
 * (spec Section 3) rather than falling back to destructive rebuild -
 * this is a single-device local DB with no server backup.
 */
@Database(
    entities = [AppMeta::class, Task::class, Subtask::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        private const val DB_NAME = "solo_leveling.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        dueDate INTEGER,
                        priority TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        isDone INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS subtasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        isDone INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_taskId ON subtasks(taskId)")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}

package com.nightpixel.sololeveling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nightpixel.sololeveling.data.dao.AppMetaDao
import com.nightpixel.sololeveling.data.dao.BossDao
import com.nightpixel.sololeveling.data.dao.CalendarDao
import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.dao.GoalDao
import com.nightpixel.sololeveling.data.dao.GymDao
import com.nightpixel.sololeveling.data.dao.HabitDao
import com.nightpixel.sololeveling.data.dao.MoodDao
import com.nightpixel.sololeveling.data.dao.StatDao
import com.nightpixel.sololeveling.data.dao.TaskDao
import com.nightpixel.sololeveling.data.dao.TaskListDao
import com.nightpixel.sololeveling.data.dao.WaterDao
import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.Boss
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import com.nightpixel.sololeveling.data.entity.Exercise
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.Goal
import com.nightpixel.sololeveling.data.entity.GymSession
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitLog
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.data.entity.XpLog

/**
 * Every schema change here must ship with an explicit Room Migration
 * (spec Section 3) rather than falling back to destructive rebuild -
 * this is a single-device local DB with no server backup.
 */
@Database(
    entities = [
        AppMeta::class, Task::class, Subtask::class, TaskList::class,
        Habit::class, HabitLog::class, Exercise::class, GymSession::class,
        CalendarEventCache::class, MoodEntry::class, FoodLogEntry::class, WaterLog::class,
        Goal::class, Stat::class, XpLog::class, Boss::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun habitDao(): HabitDao
    abstract fun gymDao(): GymDao
    abstract fun calendarDao(): CalendarDao
    abstract fun moodDao(): MoodDao
    abstract fun foodDao(): FoodDao
    abstract fun waterDao(): WaterDao
    abstract fun goalDao(): GoalDao
    abstract fun statDao(): StatDao
    abstract fun bossDao(): BossDao

    companion object {
        const val CURRENT_SCHEMA_VERSION = 11
        private const val DB_NAME = "solo_leveling.db"

        private fun seedDefaultListSql(): String =
            "INSERT INTO task_lists (id, name, position, createdAt) " +
                "VALUES (${TaskList.DEFAULT_ID}, 'Tasks', 0, ${System.currentTimeMillis()})"

        private fun seedStatSql(tag: StatTag): String =
            "INSERT INTO stats (tag, level, currentXp) VALUES ('${tag.name}', 1, 0)"

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(seedDefaultListSql())
                db.execSQL("ALTER TABLE tasks ADD COLUMN listId INTEGER NOT NULL DEFAULT ${TaskList.DEFAULT_ID}")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_listId ON tasks(listId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        targetPerWeek INTEGER NOT NULL,
                        statTag TEXT NOT NULL,
                        reminderTime INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        done INTEGER NOT NULL,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_logs_habitId_date ON habit_logs(habitId, date)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercises (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        targetSets INTEGER,
                        targetReps INTEGER,
                        targetWeight REAL,
                        targetDuration INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gym_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        actualSets INTEGER,
                        actualReps INTEGER,
                        actualWeight REAL,
                        actualDuration INTEGER,
                        intensity INTEGER,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_gym_sessions_exerciseId_date ON gym_sessions(exerciseId, date)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_event_cache (
                        googleEventId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        start INTEGER NOT NULL,
                        end INTEGER NOT NULL,
                        syncedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mood_entries (
                        date TEXT NOT NULL PRIMARY KEY,
                        color TEXT NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS food_log_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        photoUri TEXT NOT NULL,
                        description TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS water_logs (
                        date TEXT NOT NULL PRIMARY KEY,
                        bottlesLogged INTEGER NOT NULL,
                        goalBottles INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tier TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        targetDate INTEGER,
                        status TEXT NOT NULL,
                        linkedTaskIds TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stats (
                        tag TEXT NOT NULL PRIMARY KEY,
                        level INTEGER NOT NULL,
                        currentXp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS xp_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        statTag TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                StatTag.entries.forEach { db.execSQL(seedStatSql(it)) }
                db.execSQL("ALTER TABLE water_logs ADD COLUMN xpGranted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bosses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        targetWeight REAL NOT NULL,
                        defeated INTEGER NOT NULL,
                        defeatedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bosses_exerciseId ON bosses(exerciseId)")
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
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Fresh installs skip MIGRATION_2_3 and MIGRATION_9_10, so seed here instead.
                            db.execSQL(seedDefaultListSql())
                            StatTag.entries.forEach { db.execSQL(seedStatSql(it)) }
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}

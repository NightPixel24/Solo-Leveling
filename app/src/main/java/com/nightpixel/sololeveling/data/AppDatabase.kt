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
import com.nightpixel.sololeveling.data.dao.PlayerProfileDao
import com.nightpixel.sololeveling.data.dao.PunishmentDao
import com.nightpixel.sololeveling.data.dao.RewardDao
import com.nightpixel.sololeveling.data.dao.SplitDayDao
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
import com.nightpixel.sololeveling.data.entity.GoldBalance
import com.nightpixel.sololeveling.data.entity.GoldTransaction
import com.nightpixel.sololeveling.data.entity.GymSession
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitLog
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.PlayerProfile
import com.nightpixel.sololeveling.data.entity.PunishmentAssignment
import com.nightpixel.sololeveling.data.entity.PunishmentPoolItem
import com.nightpixel.sololeveling.data.entity.RewardPoolItem
import com.nightpixel.sololeveling.data.entity.RewardTarget
import com.nightpixel.sololeveling.data.entity.SplitDay
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
        Goal::class, Stat::class, XpLog::class, Boss::class,
        PunishmentPoolItem::class, PunishmentAssignment::class,
        GoldBalance::class, GoldTransaction::class, RewardPoolItem::class, RewardTarget::class,
        PlayerProfile::class, SplitDay::class
    ],
    version = 15,
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
    abstract fun punishmentDao(): PunishmentDao
    abstract fun rewardDao(): RewardDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun splitDayDao(): SplitDayDao

    companion object {
        const val CURRENT_SCHEMA_VERSION = 15
        private const val DB_NAME = "solo_leveling.db"

        /** Fresh installs get a single protected "Daily" list (user feedback, 2026-08-26: "make
         * a 'daily' task list be permanently there and the default") - `position = -1` keeps it
         * first no matter how many other lists get added later. Existing installs go through
         * MIGRATION_14_15 instead, which adds a Daily list alongside whatever the user already
         * has rather than renaming their existing list out from under them. */
        private fun seedDefaultListSql(): String =
            "INSERT INTO task_lists (id, name, position, createdAt, isProtected) " +
                "VALUES (${TaskList.DEFAULT_ID}, 'Daily', -1, ${System.currentTimeMillis()}, 1)"

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
                // NOT seedDefaultListSql() - that helper also sets isProtected, a column that
                // doesn't exist on task_lists until MIGRATION_14_15. This is the original v3
                // table shape (id/name/position/createdAt only); the "Daily" protected list gets
                // added later, on top of whatever this seeds, by MIGRATION_14_15 itself.
                db.execSQL(
                    "INSERT INTO task_lists (id, name, position, createdAt) " +
                        "VALUES (${TaskList.DEFAULT_ID}, 'Tasks', 0, ${System.currentTimeMillis()})"
                )
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

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS punishment_pool_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS punishment_assignments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        sourceRef TEXT NOT NULL,
                        dateAssigned TEXT NOT NULL,
                        resolved INTEGER NOT NULL,
                        resolvedAt INTEGER,
                        FOREIGN KEY(itemId) REFERENCES punishment_pool_items(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_punishment_assignments_itemId ON punishment_assignments(itemId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_punishment_assignments_sourceRef ON punishment_assignments(sourceRef)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gold_balance (
                        id INTEGER NOT NULL PRIMARY KEY,
                        balance INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO gold_balance (id, balance) VALUES (0, 0)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gold_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reward_pool_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        cost INTEGER NOT NULL,
                        pool TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reward_targets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        pool TEXT NOT NULL,
                        periodStart TEXT NOT NULL,
                        itemId INTEGER NOT NULL,
                        claimed INTEGER NOT NULL,
                        claimedAt INTEGER,
                        FOREIGN KEY(itemId) REFERENCES reward_pool_items(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reward_targets_itemId ON reward_targets(itemId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_reward_targets_pool_periodStart " +
                        "ON reward_targets(pool, periodStart)"
                )
            }
        }

        /** Bundles three unrelated-but-simultaneous changes from the same round of user feedback
         * (2026-08-26) rather than three separate version bumps for one evening's work:
         * (1) AGILITY was dropped in favor of SPIRITUALITY - since StatTag is stored as its raw
         * `.name` string (see Converters.kt), every existing row referencing the old name needs
         * updating, not just the Kotlin enum; (2) FoodLogEntry gained a `rating` column and its
         * `photoUri` became nullable - SQLite can't ALTER a column's NOT NULL constraint in place,
         * so this rebuilds the table (copy-drop-rename), the standard safe approach for a
         * constraint change; (3) a new player_profile table for the name+title system. */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE stats SET tag = 'SPIRITUALITY' WHERE tag = 'AGILITY'")
                db.execSQL("UPDATE habits SET statTag = 'SPIRITUALITY' WHERE statTag = 'AGILITY'")
                db.execSQL("UPDATE xp_logs SET statTag = 'SPIRITUALITY' WHERE statTag = 'AGILITY'")

                db.execSQL(
                    """
                    CREATE TABLE food_log_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        photoUri TEXT,
                        description TEXT NOT NULL,
                        rating TEXT NOT NULL DEFAULT 'OK'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO food_log_entries_new (id, date, timestamp, photoUri, description, rating) " +
                        "SELECT id, date, timestamp, photoUri, description, 'OK' FROM food_log_entries"
                )
                db.execSQL("DROP TABLE food_log_entries")
                db.execSQL("ALTER TABLE food_log_entries_new RENAME TO food_log_entries")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS player_profile (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        equippedTitleId TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO player_profile (id, name, equippedTitleId) VALUES (0, 'Hunter', NULL)")
            }
        }

        /** Bundles two unrelated-but-simultaneous changes from the same round of user feedback
         * (2026-08-26), same "one evening, one version bump" approach MIGRATION_13_14 used:
         * (1) Tasks gets a permanent, undeletable "Daily" list - inserted alongside whatever
         * lists already exist (never renaming the user's existing data) rather than assuming
         * their current default list is still named "Tasks"/unedited; (2) Gym drops fixed
         * weekday scheduling for a user-defined workout split (Day 1, Day 2, ...) - missing a
         * day used to push every later exercise onto the wrong weekday header, which a split
         * with no calendar day baked in can't do. `exercises` needs a real table rebuild since
         * `dayOfWeek` is being replaced by a new FK column, `splitDayId`, not just widened. */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task_lists ADD COLUMN isProtected INTEGER NOT NULL DEFAULT 0")
                db.query("SELECT COUNT(*) FROM task_lists WHERE name = 'Daily'").use { cursor ->
                    if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                        db.execSQL(
                            "INSERT INTO task_lists (name, position, createdAt, isProtected) " +
                                "VALUES ('Daily', -1, ${System.currentTimeMillis()}, 1)"
                        )
                    }
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS split_days (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Only create a split day for weekdays that actually had an exercise pinned to
                // them - an empty gym routine migrates to an empty split, same "no exercises yet"
                // empty state GymScreen already shows, rather than 7 blank placeholder days.
                val usedDays = mutableListOf<Int>()
                db.query("SELECT DISTINCT dayOfWeek FROM exercises ORDER BY dayOfWeek ASC").use { cursor ->
                    while (cursor.moveToNext()) usedDays.add(cursor.getInt(0))
                }
                val weekdayNames = listOf(
                    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
                )
                usedDays.forEachIndexed { index, day ->
                    val name = weekdayNames.getOrElse(day - 1) { "Day $day" }
                    val color = SplitDay.COLOR_PALETTE[index % SplitDay.COLOR_PALETTE.size]
                    db.execSQL(
                        "INSERT INTO split_days (id, name, colorHex, orderIndex, createdAt) " +
                            "VALUES (${day}, '${name}', '${color}', ${index}, ${System.currentTimeMillis()})"
                    )
                }

                db.execSQL(
                    """
                    CREATE TABLE exercises_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        splitDayId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        targetSets INTEGER,
                        targetReps INTEGER,
                        targetWeight REAL,
                        targetDuration INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(splitDayId) REFERENCES split_days(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO exercises_new (id, name, splitDayId, type, targetSets, targetReps, " +
                        "targetWeight, targetDuration, createdAt) " +
                        "SELECT id, name, dayOfWeek, type, targetSets, targetReps, targetWeight, " +
                        "targetDuration, createdAt FROM exercises"
                )
                db.execSQL("DROP TABLE exercises")
                db.execSQL("ALTER TABLE exercises_new RENAME TO exercises")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_splitDayId ON exercises(splitDayId)")
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
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
                        MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Fresh installs skip MIGRATION_2_3, MIGRATION_9_10, MIGRATION_12_13 and
                            // MIGRATION_13_14's inserts, so seed here instead.
                            db.execSQL(seedDefaultListSql())
                            StatTag.entries.forEach { db.execSQL(seedStatSql(it)) }
                            db.execSQL("INSERT INTO gold_balance (id, balance) VALUES (0, 0)")
                            db.execSQL("INSERT INTO player_profile (id, name, equippedTitleId) VALUES (0, 'Hunter', NULL)")
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}

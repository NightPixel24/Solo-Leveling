package com.nightpixel.sololeveling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nightpixel.sololeveling.data.dao.AppMetaDao
import com.nightpixel.sololeveling.data.dao.CalendarDao
import com.nightpixel.sololeveling.data.dao.FoodDao
import com.nightpixel.sololeveling.data.dao.GoalDao
import com.nightpixel.sololeveling.data.dao.GymDao
import com.nightpixel.sololeveling.data.dao.HabitDao
import com.nightpixel.sololeveling.data.dao.HealthDao
import com.nightpixel.sololeveling.data.dao.MoodDao
import com.nightpixel.sololeveling.data.dao.PlayerProfileDao
import com.nightpixel.sololeveling.data.dao.PunishmentDao
import com.nightpixel.sololeveling.data.dao.RestDayLogDao
import com.nightpixel.sololeveling.data.dao.RestDayNoteDao
import com.nightpixel.sololeveling.data.dao.RewardDao
import com.nightpixel.sololeveling.data.dao.RoutineDao
import com.nightpixel.sololeveling.data.dao.ScheduledWorkoutDao
import com.nightpixel.sololeveling.data.dao.SplitDayDao
import com.nightpixel.sololeveling.data.dao.StatDao
import com.nightpixel.sololeveling.data.dao.TaskDao
import com.nightpixel.sololeveling.data.dao.TaskListDao
import com.nightpixel.sololeveling.data.dao.WaterDao
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
        Goal::class, Stat::class, XpLog::class,
        PunishmentPoolItem::class, PunishmentAssignment::class,
        RewardPoolItem::class, RewardInventoryItem::class,
        PlayerProfile::class, SplitDay::class, RoutineItem::class, BodyStatEntry::class,
        ScheduledWorkout::class, RestDayLog::class, RestDayNote::class
    ],
    version = 25,
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
    abstract fun punishmentDao(): PunishmentDao
    abstract fun rewardDao(): RewardDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun splitDayDao(): SplitDayDao
    abstract fun routineDao(): RoutineDao
    abstract fun healthDao(): HealthDao
    abstract fun scheduledWorkoutDao(): ScheduledWorkoutDao
    abstract fun restDayLogDao(): RestDayLogDao
    abstract fun restDayNoteDao(): RestDayNoteDao

    companion object {
        const val CURRENT_SCHEMA_VERSION = 25
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

        /** Boss Fights (spec Section 5.5) removed per user feedback (2026-08-26: "get rid of boss
         * fights in the gym page") - drops the table outright rather than leaving it as dead
         * schema, same "don't leave unused scaffolding around" approach this codebase already
         * follows for Kotlin code. This does delete any boss data a user already had; acceptable
         * here since the feature itself is gone, so there's nothing left for that data to serve. */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS bosses")
            }
        }

        /** Bundles two unrelated-but-simultaneous new features from the same round of user
         * feedback (2026-08-30), same "one evening, one version bump" approach prior passes used:
         * (1) the Habits tab becomes "Routine" - a Schedule sub-tab lets the user plan free-text
         * or habit-linked items under Morning/Day/Afternoon/Night, backed by the new
         * `routine_items` table (FK+cascade to `habits`, same as `HabitLog`); (2) a new Health tab
         * on the Dashboard records dated/timed weight, blood sugar, and blood pressure readings,
         * backed by `body_stat_entries` - one table for all three types (see [BodyStatEntry]'s doc
         * comment) rather than three near-identical tables. */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routine_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dayPart TEXT NOT NULL,
                        title TEXT NOT NULL,
                        habitId INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_items_habitId ON routine_items(habitId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS body_stat_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        value REAL,
                        systolic INTEGER,
                        diastolic INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        /** Removes currency entirely (user feedback, 2026-08-30: "get rid of currency all together
         * keep the rewards section... the currency system was to complex") - `gold_balance`,
         * `gold_transactions`, and `reward_targets` (the old Gold-cost/pick-a-target flow) are
         * dropped outright, same "don't leave unused scaffolding around" approach the Boss Fights
         * removal (`MIGRATION_15_16`) already used for this codebase's DB layer; their historical
         * rows (a Gold ledger, past claimed-target picks) don't mean anything under the new
         * severity-based system, so there's nothing worth preserving there. `reward_pool_items`
         * does keep its existing rows though - the user's own reward titles are still meaningful,
         * just re-shaped: `cost`/`pool` (WEEKLY/MONTHLY) are replaced by `severity`
         * (MINOR/MAJOR, [PunishmentSeverity] reused from the Punishment Pool's identical split),
         * mapped WEEKLY->MINOR and MONTHLY->MAJOR since that's the closest analogue (Weekly was
         * the smaller/frequent pool, Minor is the smaller/frequent tier). New `reward_inventory`
         * table holds rewards actually claimed, pending "use" (see [RewardInventoryItem]'s doc
         * comment for why it snapshots title/severity rather than an FK to the pool item). */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS gold_balance")
                db.execSQL("DROP TABLE IF EXISTS gold_transactions")
                db.execSQL("DROP TABLE IF EXISTS reward_targets")

                db.execSQL(
                    """
                    CREATE TABLE reward_pool_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO reward_pool_items_new (id, title, severity, createdAt) " +
                        "SELECT id, title, CASE WHEN pool = 'WEEKLY' THEN 'MINOR' ELSE 'MAJOR' END, createdAt " +
                        "FROM reward_pool_items"
                )
                db.execSQL("DROP TABLE reward_pool_items")
                db.execSQL("ALTER TABLE reward_pool_items_new RENAME TO reward_pool_items")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reward_inventory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        claimedAt INTEGER NOT NULL,
                        usedAt INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the Gym screen's new Routine tab (user feedback, 2026-08-30: "add a new tab...
         * calling that routine... it's going to be basically the gym schedule") - a plain weekly
         * plan mapping each weekday to a [SplitDay] ("Workout" in the now-renamed UI), fully
         * decoupled from the Calendar tab's actual logged history (see [ScheduledWorkout]'s doc
         * comment for why). No data migration needed beyond creating the empty table - there's no
         * prior "schedule" concept for existing rows to map from. */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scheduled_workouts (
                        dayOfWeek INTEGER NOT NULL PRIMARY KEY,
                        splitDayId INTEGER NOT NULL,
                        FOREIGN KEY(splitDayId) REFERENCES split_days(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scheduled_workouts_splitDayId ON scheduled_workouts(splitDayId)")
            }
        }

        /** Adds an optional specific reminder time to a Routine schedule item (user feedback,
         * 2026-08-30: "if there are any time specific things in my schedule, I should also be
         * notified for that... at nine PM it might say take my tablets") - see [RoutineItem]'s doc
         * comment. A plain nullable column add, no data to migrate. */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_items ADD COLUMN reminderTime INTEGER")
            }
        }

        /** Free-text Routine schedule items get their own checkbox (user feedback, 2026-08-31:
         * "you didnt make the items checkboxes" - previously only habit-linked items could be
         * checked, per [RoutineItem]'s now-outdated doc comment). `completedDate` holds the ISO
         * date it was last checked; null = not done. A plain nullable column add, no data to
         * migrate - every existing row reads as "not done today" either way. */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_items ADD COLUMN completedDate TEXT")
            }
        }

        /** Adds drag-to-reorder within a Schedule day part (user feedback, 2026-08-31: "have a
         * drag bar on the left side so I can move the scheduled items around instead of being
         * locked into place") - see [RoutineItem]'s doc comment for why a plain default-0 column
         * needs no per-row backfill: `createdAt` already reproduces the pre-existing order as a
         * tiebreak until the user actually drags something. */
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_items ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Drops the Dashboard's title system entirely (user feedback, 2026-08-31: "remove the
         * rank titles... i would rather it just be a text field non clickable that says 'E
         * rank'") - the equipped-title choice ([PlayerProfile.equippedTitleId]) has nothing left
         * to reference now that `data/gamification/Titles.kt` is deleted, so the column goes too
         * rather than leaving unused scaffolding around (same stance the Boss Fights/Gold removals
         * already took). Table rebuild (create-copy-drop-rename) rather than a plain `DROP COLUMN`
         * so this still works on the pre-3.35 SQLite versions this app's minSdk 26 can run on. */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS player_profile_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO player_profile_new (id, name) SELECT id, name FROM player_profile")
                db.execSQL("DROP TABLE player_profile")
                db.execSQL("ALTER TABLE player_profile_new RENAME TO player_profile")
            }
        }

        /** Bundles three unrelated-but-simultaneous changes from one round of feedback
         * (2026-09-02), same "one evening, one version bump" approach earlier passes used:
         * - `tasks.position` (user feedback: "an edit icon at the top... lets me drag them around
         *   in different orders" + HIGH-priority tasks auto-added at the top) - a plain default-0
         *   column add, no per-row backfill: `createdAt DESC` still reproduces the pre-existing
         *   newest-first order as a tiebreak until the user actually drags something.
         * - `split_days.isRest` + `rest_day_logs` (user feedback: rest days are "a workout like the
         *   rest of them" - a [SplitDay] with `isRest = 1`, no exercises, ticked per date into
         *   `rest_day_logs` from the Workouts tab). `rest_day_logs.date` is the PK (one rest entry
         *   per calendar day), FK+cascade to `split_days`; its index mirrors the `@Index` on
         *   [RestDayLog] so runMigrationsAndValidate's schema check passes. */
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE split_days ADD COLUMN isRest INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rest_day_logs (
                        date TEXT NOT NULL PRIMARY KEY,
                        splitDayId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(splitDayId) REFERENCES split_days(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rest_day_logs_splitDayId " +
                        "ON rest_day_logs(splitDayId)"
                )
            }
        }

        /** A rest-day workout gets its own free-text notes (user feedback, 2026-09-02: a rest day
         * "should still have a drop down header similar to the other workouts... when adding things
         * under it dont show the exercise modal show a text box so i can type stuff in") - a plain
         * new `rest_day_notes` table, FK+cascade to `split_days`, its index mirroring the `@Index`
         * on [RestDayNote] so runMigrationsAndValidate's schema check passes. Purely additive, no
         * existing data to migrate. */
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rest_day_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        splitDayId INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(splitDayId) REFERENCES split_days(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_rest_day_notes_splitDayId " +
                        "ON rest_day_notes(splitDayId)"
                )
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
                        MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                        MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                        MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Fresh installs skip MIGRATION_2_3, MIGRATION_9_10, MIGRATION_12_13 and
                            // MIGRATION_13_14's inserts, so seed here instead.
                            db.execSQL(seedDefaultListSql())
                            StatTag.entries.forEach { db.execSQL(seedStatSql(it)) }
                            db.execSQL("INSERT INTO player_profile (id, name) VALUES (0, 'Hunter')")
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}

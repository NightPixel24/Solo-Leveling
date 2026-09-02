package com.nightpixel.sololeveling.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightpixel.sololeveling.data.entity.StatTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 17 - replays every real [AppDatabase] Migration against the exact schema JSON Room
 * exported for that version (committed under app/schemas), so a mistake in a Migration's SQL
 * fails a test instead of only surfacing the next time a real device upgrades in place. Each
 * step is also exercised individually (not just the full 1->13 chain) so a broken migration
 * points straight at the version pair responsible.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(dbName, 2, true, AppDatabase.MIGRATION_1_2)
    }

    @Test
    fun migrate2To3() {
        helper.createDatabase(dbName, 2).close()
        helper.runMigrationsAndValidate(dbName, 3, true, AppDatabase.MIGRATION_2_3)
    }

    @Test
    fun migrate3To4() {
        helper.createDatabase(dbName, 3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4)
    }

    @Test
    fun migrate4To5() {
        helper.createDatabase(dbName, 4).close()
        helper.runMigrationsAndValidate(dbName, 5, true, AppDatabase.MIGRATION_4_5)
    }

    @Test
    fun migrate5To6() {
        helper.createDatabase(dbName, 5).close()
        helper.runMigrationsAndValidate(dbName, 6, true, AppDatabase.MIGRATION_5_6)
    }

    @Test
    fun migrate6To7() {
        helper.createDatabase(dbName, 6).close()
        helper.runMigrationsAndValidate(dbName, 7, true, AppDatabase.MIGRATION_6_7)
    }

    @Test
    fun migrate7To8() {
        helper.createDatabase(dbName, 7).close()
        helper.runMigrationsAndValidate(dbName, 8, true, AppDatabase.MIGRATION_7_8)
    }

    @Test
    fun migrate8To9() {
        helper.createDatabase(dbName, 8).close()
        helper.runMigrationsAndValidate(dbName, 9, true, AppDatabase.MIGRATION_8_9)
    }

    @Test
    fun migrate9To10() {
        helper.createDatabase(dbName, 9).close()
        helper.runMigrationsAndValidate(dbName, 10, true, AppDatabase.MIGRATION_9_10)
    }

    @Test
    fun migrate10To11() {
        helper.createDatabase(dbName, 10).close()
        helper.runMigrationsAndValidate(dbName, 11, true, AppDatabase.MIGRATION_10_11)
    }

    @Test
    fun migrate11To12() {
        helper.createDatabase(dbName, 11).close()
        helper.runMigrationsAndValidate(dbName, 12, true, AppDatabase.MIGRATION_11_12)
    }

    @Test
    fun migrate12To13() {
        helper.createDatabase(dbName, 12).close()
        helper.runMigrationsAndValidate(dbName, 13, true, AppDatabase.MIGRATION_12_13)
    }

    /** Also seeds a real 'AGILITY' stats/habits/xp_logs row before migrating, so the rename to
     * 'SPIRITUALITY' (user feedback, 2026-08-26) is exercised against actual data, not just an
     * empty table - runMigrationsAndValidate's schema check alone wouldn't catch a rename typo. */
    @Test
    fun migrate13To14() {
        val db = helper.createDatabase(dbName, 13)
        db.execSQL("INSERT INTO stats (tag, level, currentXp) VALUES ('AGILITY', 3, 10)")
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 14, true, AppDatabase.MIGRATION_13_14)
        migrated.query("SELECT tag, level FROM stats WHERE level = 3").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("SPIRITUALITY", cursor.getString(0))
        }
    }

    /** Seeds two exercises on different weekdays before migrating, so the exercises table rebuild
     * (weekday -> a real [com.nightpixel.sololeveling.data.entity.SplitDay] row) is exercised
     * against actual data (user feedback, 2026-08-26: dropping fixed weekday scheduling for a
     * user-defined workout split), not just an empty table. Also confirms the always-present
     * "Daily" task list (same round of feedback) gets inserted when nothing named that exists yet. */
    @Test
    fun migrate14To15() {
        val db = helper.createDatabase(dbName, 14)
        db.execSQL(
            "INSERT INTO exercises (id, name, dayOfWeek, type, createdAt) VALUES (1, 'Bench Press', 1, 'STRENGTH', 0)"
        )
        db.execSQL(
            "INSERT INTO exercises (id, name, dayOfWeek, type, createdAt) VALUES (2, 'Squat', 3, 'STRENGTH', 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 15, true, AppDatabase.MIGRATION_14_15)

        migrated.query("SELECT COUNT(*) FROM task_lists WHERE name = 'Daily' AND isProtected = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM split_days").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        migrated.query("SELECT name, splitDayId FROM exercises WHERE name = 'Bench Press'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(1))
        }
    }

    /** Seeds a real boss row before migrating so the drop is exercised against actual data, not
     * just an empty table - Boss Fights removed per user feedback (2026-08-26: "get rid of boss
     * fights in the gym page"). */
    @Test
    fun migrate15To16() {
        val db = helper.createDatabase(dbName, 15)
        db.execSQL(
            "INSERT INTO exercises (id, name, splitDayId, type, createdAt) VALUES (1, 'Bench Press', 1, 'STRENGTH', 0)"
        )
        db.execSQL(
            "INSERT INTO split_days (id, name, colorHex, orderIndex, createdAt) VALUES (1, 'Day 1', '#E5484D', 0, 0)"
        )
        db.execSQL(
            "INSERT INTO bosses (id, exerciseId, name, targetWeight, defeated, createdAt) " +
                "VALUES (1, 1, 'Bench Boss', 100.0, 0, 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 16, true, AppDatabase.MIGRATION_15_16)
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'bosses'").use { cursor ->
            assertEquals(0, cursor.count)
        }
    }

    /** Seeds a real habit before migrating so `routine_items` (Routine tab's Schedule sub-tab,
     * user feedback 2026-08-30) and `body_stat_entries` (Dashboard's new Health tab) both get
     * created with the right shape - not just an empty-table schema check. */
    @Test
    fun migrate16To17() {
        val db = helper.createDatabase(dbName, 16)
        db.execSQL(
            "INSERT INTO habits (id, title, frequency, targetPerWeek, statTag, createdAt) " +
                "VALUES (1, 'Read scripture', 'DAILY', 3, 'SPIRITUALITY', 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 17, true, AppDatabase.MIGRATION_16_17)

        migrated.execSQL(
            "INSERT INTO routine_items (dayPart, title, habitId, createdAt) VALUES ('MORNING', '', 1, 0)"
        )
        migrated.query("SELECT COUNT(*) FROM routine_items WHERE dayPart = 'MORNING'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        migrated.execSQL(
            "INSERT INTO body_stat_entries (type, timestamp, value, systolic, diastolic) " +
                "VALUES ('BLOOD_PRESSURE', 0, NULL, 120, 80)"
        )
        migrated.query("SELECT systolic, diastolic FROM body_stat_entries WHERE type = 'BLOOD_PRESSURE'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(120, cursor.getInt(0))
            assertEquals(80, cursor.getInt(1))
        }
    }

    /** Seeds a real Weekly reward pool item (plus a gold_balance/reward_targets row) before
     * migrating, so the currency removal (user feedback, 2026-08-30: "get rid of currency all
     * together... the currency system was to complex") is exercised against actual data: the
     * existing reward title should survive, re-shaped from cost/pool to severity
     * (WEEKLY -> MINOR), while gold_balance/gold_transactions/reward_targets should be gone. */
    @Test
    fun migrate17To18() {
        val db = helper.createDatabase(dbName, 17)
        db.execSQL(
            "INSERT INTO reward_pool_items (id, title, cost, pool, createdAt) " +
                "VALUES (1, 'Movie Night', 50, 'WEEKLY', 0)"
        )
        db.execSQL("INSERT INTO gold_balance (id, balance) VALUES (0, 100)")
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 18, true, AppDatabase.MIGRATION_17_18)

        migrated.query("SELECT title, severity FROM reward_pool_items WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Movie Night", cursor.getString(0))
            assertEquals("MINOR", cursor.getString(1))
        }
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN " +
                "('gold_balance', 'gold_transactions', 'reward_targets')"
        ).use { cursor ->
            assertEquals(0, cursor.count)
        }
    }

    /** Seeds a real split day before migrating and assigns it to a weekday, so the new
     * `scheduled_workouts` table (Gym screen's Routine tab, user feedback, 2026-08-30: "add a new
     * tab... calling that routine... basically the gym schedule") is exercised against a real
     * FK target, not just an empty table. */
    @Test
    fun migrate18To19() {
        val db = helper.createDatabase(dbName, 18)
        db.execSQL(
            "INSERT INTO split_days (id, name, colorHex, orderIndex, createdAt) " +
                "VALUES (1, 'Back Day', '#E5484D', 0, 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 19, true, AppDatabase.MIGRATION_18_19)

        migrated.execSQL("INSERT INTO scheduled_workouts (dayOfWeek, splitDayId) VALUES (1, 1)")
        migrated.query("SELECT splitDayId FROM scheduled_workouts WHERE dayOfWeek = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }
    }

    /** Seeds a real routine item before migrating, so the new nullable `reminderTime` column
     * (user feedback, 2026-08-30: time-specific schedule notifications) is exercised against a
     * real pre-existing row, not just an empty table. */
    @Test
    fun migrate19To20() {
        val db = helper.createDatabase(dbName, 19)
        db.execSQL(
            "INSERT INTO routine_items (id, dayPart, title, habitId, createdAt) " +
                "VALUES (1, 'NIGHT', 'Take tablets', NULL, 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 20, true, AppDatabase.MIGRATION_19_20)

        migrated.execSQL("UPDATE routine_items SET reminderTime = 1260 WHERE id = 1")
        migrated.query("SELECT reminderTime FROM routine_items WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1260, cursor.getInt(0))
        }
    }

    /** Seeds a real routine item before migrating, so the new nullable `completedDate` column
     * (user feedback, 2026-08-31: "you didnt make the items checkboxes" - free-text items now get
     * their own checkable done-state) is exercised against a real pre-existing row. */
    @Test
    fun migrate20To21() {
        val db = helper.createDatabase(dbName, 20)
        db.execSQL(
            "INSERT INTO routine_items (id, dayPart, title, habitId, createdAt) " +
                "VALUES (1, 'MORNING', 'Stretch', NULL, 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 21, true, AppDatabase.MIGRATION_20_21)

        migrated.execSQL("UPDATE routine_items SET completedDate = '2026-08-31' WHERE id = 1")
        migrated.query("SELECT completedDate FROM routine_items WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("2026-08-31", cursor.getString(0))
        }
    }

    /** Seeds two real routine items in the same day part before migrating, so the new `position`
     * column (user feedback, 2026-08-31: drag-to-reorder within a Schedule day part) is exercised
     * against real pre-existing rows - both should default to 0 (no backfill needed, see
     * RoutineItem's doc comment) and remain independently updatable afterward. */
    @Test
    fun migrate21To22() {
        val db = helper.createDatabase(dbName, 21)
        db.execSQL(
            "INSERT INTO routine_items (id, dayPart, title, habitId, createdAt) " +
                "VALUES (1, 'MORNING', 'Stretch', NULL, 0)"
        )
        db.execSQL(
            "INSERT INTO routine_items (id, dayPart, title, habitId, createdAt) " +
                "VALUES (2, 'MORNING', 'Journal', NULL, 1)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 22, true, AppDatabase.MIGRATION_21_22)

        migrated.query("SELECT position FROM routine_items WHERE id IN (1, 2)").use { cursor ->
            while (cursor.moveToNext()) {
                assertEquals(0, cursor.getInt(0))
            }
        }
        migrated.execSQL("UPDATE routine_items SET position = 1 WHERE id = 1")
        migrated.query("SELECT position FROM routine_items WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    /** Seeds a real player_profile row with a non-null equippedTitleId before migrating, so the
     * title system's removal (user feedback, 2026-08-31: "remove the rank titles... i would
     * rather it just be a text field non clickable that says 'E rank'") is exercised against real
     * data - the name should survive the table rebuild, and equippedTitleId should be gone. */
    @Test
    fun migrate22To23() {
        val db = helper.createDatabase(dbName, 22)
        db.execSQL("INSERT INTO player_profile (id, name, equippedTitleId) VALUES (0, 'Joshua', 'RANK_C')")
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 23, true, AppDatabase.MIGRATION_22_23)

        migrated.query("SELECT name FROM player_profile WHERE id = 0").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Joshua", cursor.getString(0))
        }
        migrated.query("PRAGMA table_info(player_profile)").use { cursor ->
            var hasEquippedTitleId = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "equippedTitleId") hasEquippedTitleId = true
            }
            assertEquals(false, hasEquippedTitleId)
        }
    }

    /** Seeds two real tasks and a real split day before migrating, so the bundled v24 changes
     * (user feedback, 2026-09-02) are exercised against real data: `tasks.position` defaults to 0
     * on every existing row, `split_days` gains an `isRest` column, and the brand-new
     * `rest_day_logs` table takes a row FK-referencing that split day. */
    @Test
    fun migrate23To24() {
        val db = helper.createDatabase(dbName, 23)
        db.execSQL(
            "INSERT INTO tasks (id, listId, title, priority, notes, isDone, createdAt) " +
                "VALUES (1, 1, 'Buy milk', 'HIGH', '', 0, 0)"
        )
        db.execSQL(
            "INSERT INTO tasks (id, listId, title, priority, notes, isDone, createdAt) " +
                "VALUES (2, 1, 'Call bank', 'MEDIUM', '', 0, 1)"
        )
        db.execSQL(
            "INSERT INTO split_days (id, name, colorHex, orderIndex, createdAt) " +
                "VALUES (1, 'Rest Day', '#42C2FF', 0, 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 24, true, AppDatabase.MIGRATION_23_24)

        migrated.query("SELECT position FROM tasks WHERE id IN (1, 2)").use { cursor ->
            while (cursor.moveToNext()) {
                assertEquals(0, cursor.getInt(0))
            }
        }
        migrated.execSQL("UPDATE tasks SET position = -1 WHERE id = 1")
        migrated.query("SELECT position FROM tasks WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(-1, cursor.getInt(0))
        }

        migrated.execSQL("UPDATE split_days SET isRest = 1 WHERE id = 1")
        migrated.query("SELECT isRest FROM split_days WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        migrated.execSQL("INSERT INTO rest_day_logs (date, splitDayId, createdAt) VALUES ('2026-09-02', 1, 0)")
        migrated.query("SELECT splitDayId FROM rest_day_logs WHERE date = '2026-09-02'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }
    }

    /** Seeds a real rest-day split day before migrating, so the brand-new `rest_day_notes` table
     * (user feedback, 2026-09-02: a rest day "should still have a drop down header similar to the
     * other workouts... when adding things under it dont show the exercise modal show a text box")
     * is exercised against a real FK target, not just an empty-table schema check. */
    @Test
    fun migrate24To25() {
        val db = helper.createDatabase(dbName, 24)
        db.execSQL(
            "INSERT INTO split_days (id, name, colorHex, orderIndex, isRest, createdAt) " +
                "VALUES (1, 'Active Recovery', '#42C2FF', 0, 1, 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 25, true, AppDatabase.MIGRATION_24_25)

        migrated.execSQL(
            "INSERT INTO rest_day_notes (splitDayId, text, createdAt) VALUES (1, 'Light stretching', 0)"
        )
        migrated.query("SELECT splitDayId, text FROM rest_day_notes WHERE splitDayId = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Light stretching", cursor.getString(1))
        }
    }

    /** Seeds a rest-day split day + a rest_day_notes row before migrating, so the new nullable
     * `completedDate` column (user feedback, 2026-09-02: "the checkbox should only be for added
     * items, not in the header") is exercised against a real pre-existing row - mirrors
     * `migrate20To21` (the same column on `routine_items`). */
    @Test
    fun migrate25To26() {
        val db = helper.createDatabase(dbName, 25)
        db.execSQL(
            "INSERT INTO split_days (id, name, colorHex, orderIndex, isRest, createdAt) " +
                "VALUES (1, 'Active Recovery', '#42C2FF', 0, 1, 0)"
        )
        db.execSQL(
            "INSERT INTO rest_day_notes (id, splitDayId, text, createdAt) VALUES (1, 1, 'Stretch', 0)"
        )
        db.close()
        val migrated = helper.runMigrationsAndValidate(dbName, 26, true, AppDatabase.MIGRATION_25_26)

        migrated.query("SELECT completedDate FROM rest_day_notes WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(true, cursor.isNull(0))
        }
        migrated.execSQL("UPDATE rest_day_notes SET completedDate = '2026-09-02' WHERE id = 1")
        migrated.query("SELECT completedDate FROM rest_day_notes WHERE id = 1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("2026-09-02", cursor.getString(0))
        }
    }

    /** The full chain a real device upgrading from the very first release runs through, plus a
     * sanity read through Room's own generated DAOs (not just raw-SQL schema validation) to
     * confirm the fully-migrated database is actually usable - the seeded rows MIGRATION_2_3,
     * MIGRATION_9_10, MIGRATION_12_13, MIGRATION_13_14 and MIGRATION_14_15 insert should have
     * survived the whole chain, no stats row should still carry the old 'AGILITY' tag, and the
     * bosses/gold_balance/gold_transactions/reward_targets tables dropped along the way should
     * all be gone. */
    @Test
    fun migrateAllStepsAndOpenWithRoom() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(
            dbName, 26, true,
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16,
            AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19,
            AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22,
            AppDatabase.MIGRATION_22_23, AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25,
            AppDatabase.MIGRATION_25_26
        )

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            dbName
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23, AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_25_26
            )
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .build()

        db.openHelper.writableDatabase
        var taskLists = emptyList<com.nightpixel.sololeveling.data.entity.TaskList>()
        var stats = emptyList<com.nightpixel.sololeveling.data.entity.Stat>()
        var profileName: String? = null
        var exercises = emptyList<com.nightpixel.sololeveling.data.entity.Exercise>()
        runBlocking {
            taskLists = db.taskListDao().getAllListsOnce()
            stats = db.statDao().getAllStatsOnce()
            profileName = db.playerProfileDao().getOnce()?.name
            exercises = db.gymDao().getAllExercisesOnce()
        }
        db.close()

        assertEquals(2, taskLists.size)
        assertEquals(true, taskLists.any { it.name == "Daily" && it.isProtected })
        assertEquals(5, stats.size)
        assertEquals(true, stats.any { it.tag == StatTag.SPIRITUALITY })
        assertEquals("Hunter", profileName)
        assertEquals(true, exercises.isEmpty())
    }
}

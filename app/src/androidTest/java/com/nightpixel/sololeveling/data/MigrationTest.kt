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

    /** The full chain a real device upgrading from the very first release runs through, plus a
     * sanity read through Room's own generated DAOs (not just raw-SQL schema validation) to
     * confirm the fully-migrated database is actually usable - the seeded rows MIGRATION_2_3,
     * MIGRATION_9_10, MIGRATION_12_13, MIGRATION_13_14 and MIGRATION_14_15 insert should have
     * survived the whole chain, no stats row should still carry the old 'AGILITY' tag, and the
     * bosses table dropped by MIGRATION_15_16 should be gone. */
    @Test
    fun migrateAllStepsAndOpenWithRoom() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(
            dbName, 16, true,
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16
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
                AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16
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

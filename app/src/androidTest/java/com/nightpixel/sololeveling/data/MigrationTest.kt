package com.nightpixel.sololeveling.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    /** The full chain a real device upgrading from the very first release runs through, plus a
     * sanity read through Room's own generated DAOs (not just raw-SQL schema validation) to
     * confirm the fully-migrated database is actually usable - the seeded rows both
     * MIGRATION_2_3 and MIGRATION_9_10 insert should have survived the whole chain. */
    @Test
    fun migrateAllStepsAndOpenWithRoom() {
        helper.createDatabase(dbName, 1).close()
        helper.runMigrationsAndValidate(
            dbName, 13, true,
            AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13
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
                AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13
            )
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .build()

        db.openHelper.writableDatabase
        val (taskLists, stats) = runBlocking {
            db.taskListDao().getAllListsOnce() to db.statDao().getAllStatsOnce()
        }
        db.close()

        assertEquals(1, taskLists.size)
        assertEquals(5, stats.size)
    }
}

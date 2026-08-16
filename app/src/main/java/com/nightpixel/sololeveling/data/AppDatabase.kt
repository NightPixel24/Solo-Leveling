package com.nightpixel.sololeveling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nightpixel.sololeveling.data.dao.AppMetaDao
import com.nightpixel.sololeveling.data.entity.AppMeta

/**
 * Every schema change here must ship with an explicit Room Migration
 * (spec Section 3) rather than falling back to destructive rebuild -
 * this is a single-device local DB with no server backup.
 */
@Database(
    entities = [AppMeta::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        private const val DB_NAME = "solo_leveling.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}

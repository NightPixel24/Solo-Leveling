package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.BodyStatEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM body_stat_entries ORDER BY timestamp DESC")
    fun observeEntries(): Flow<List<BodyStatEntry>>

    @Insert
    suspend fun insert(entry: BodyStatEntry): Long

    @Delete
    suspend fun delete(entry: BodyStatEntry)

    @Query("SELECT * FROM body_stat_entries")
    suspend fun getAllOnce(): List<BodyStatEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<BodyStatEntry>)

    @Query("DELETE FROM body_stat_entries")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(entries: List<BodyStatEntry>) {
        clear()
        insertAll(entries)
    }
}

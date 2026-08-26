package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_log_entries ORDER BY timestamp DESC")
    fun observeEntries(): Flow<List<FoodLogEntry>>

    /** Used by [com.nightpixel.sololeveling.data.gamification.vitXpMultiplier] to check recent
     * ratings before granting VIT XP. */
    @Query("SELECT * FROM food_log_entries ORDER BY timestamp DESC LIMIT :count")
    suspend fun getRecentOnce(count: Int): List<FoodLogEntry>

    @Insert
    suspend fun insertEntry(entry: FoodLogEntry): Long

    @Delete
    suspend fun deleteEntry(entry: FoodLogEntry)

    @Query("SELECT * FROM food_log_entries")
    suspend fun getAllOnce(): List<FoodLogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<FoodLogEntry>)

    @Query("DELETE FROM food_log_entries")
    suspend fun clearEntries()

    @Transaction
    suspend fun replaceAll(entries: List<FoodLogEntry>) {
        clearEntries()
        insertEntries(entries)
    }
}

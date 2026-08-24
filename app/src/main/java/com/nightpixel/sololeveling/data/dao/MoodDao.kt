package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY date ASC")
    fun observeEntries(): Flow<List<MoodEntry>>

    /** Insert-or-replace keyed by the `date` primary key, so re-rating a day overwrites. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: MoodEntry)

    @Query("DELETE FROM mood_entries WHERE date = :date")
    suspend fun deleteEntry(date: String)

    @Query("SELECT * FROM mood_entries")
    suspend fun getAllOnce(): List<MoodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<MoodEntry>)

    @Query("DELETE FROM mood_entries")
    suspend fun clearEntries()

    @Transaction
    suspend fun replaceAll(entries: List<MoodEntry>) {
        clearEntries()
        insertEntries(entries)
    }
}

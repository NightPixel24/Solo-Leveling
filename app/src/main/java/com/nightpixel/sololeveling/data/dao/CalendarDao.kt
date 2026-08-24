package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_event_cache ORDER BY start ASC")
    fun observeEvents(): Flow<List<CalendarEventCache>>

    @Query("SELECT * FROM calendar_event_cache")
    suspend fun getAllEventsOnce(): List<CalendarEventCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventCache>)

    @Query("DELETE FROM calendar_event_cache")
    suspend fun clearEvents()

    /** Google Calendar is the source of truth - every sync replaces the whole
     * cache rather than diffing, so a deleted-on-Google event disappears here too. */
    @Transaction
    suspend fun replaceAll(events: List<CalendarEventCache>) {
        clearEvents()
        insertEvents(events)
    }
}

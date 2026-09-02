package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.RestDayLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RestDayLogDao {
    @Query("SELECT * FROM rest_day_logs ORDER BY date DESC")
    fun observeAll(): Flow<List<RestDayLog>>

    /** REPLACE keyed by the `date` primary key - ticking a rest day for a date that already has
     * one (e.g. switching which rest type) just overwrites it. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: RestDayLog)

    @Query("DELETE FROM rest_day_logs WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("SELECT * FROM rest_day_logs")
    suspend fun getAllOnce(): List<RestDayLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<RestDayLog>)

    @Query("DELETE FROM rest_day_logs")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(logs: List<RestDayLog>) {
        clear()
        insertAll(logs)
    }
}

package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE date = :date")
    fun observeLog(date: String): Flow<WaterLog?>

    /** Used by the Dashboard's Weekly Quests (spec Section 5.4) to check "hit water goal N/7
     * days" across the whole week, not just today. */
    @Query("SELECT * FROM water_logs")
    fun observeAllLogs(): Flow<List<WaterLog>>

    /** Used to default a newly-created day's goal to whatever the user last set it to. */
    @Query("SELECT goalBottles FROM water_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLatestGoal(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: WaterLog)

    @Query("SELECT * FROM water_logs")
    suspend fun getAllOnce(): List<WaterLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<WaterLog>)

    @Query("DELETE FROM water_logs")
    suspend fun clearLogs()

    @Transaction
    suspend fun replaceAll(logs: List<WaterLog>) {
        clearLogs()
        insertLogs(logs)
    }
}

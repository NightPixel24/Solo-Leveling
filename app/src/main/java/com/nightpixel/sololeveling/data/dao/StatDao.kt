package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.Stat
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.XpLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StatDao {
    @Query("SELECT * FROM stats")
    fun observeStats(): Flow<List<Stat>>

    @Query("SELECT * FROM stats WHERE tag = :tag")
    suspend fun getStat(tag: StatTag): Stat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: Stat)

    @Insert
    suspend fun insertXpLog(log: XpLog)

    @Query("SELECT * FROM stats")
    suspend fun getAllStatsOnce(): List<Stat>

    @Query("SELECT * FROM xp_logs")
    suspend fun getAllXpLogsOnce(): List<XpLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: List<Stat>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpLogs(logs: List<XpLog>)

    @Query("DELETE FROM stats")
    suspend fun clearStats()

    @Query("DELETE FROM xp_logs")
    suspend fun clearXpLogs()

    @Transaction
    suspend fun replaceAll(stats: List<Stat>, logs: List<XpLog>) {
        clearXpLogs()
        clearStats()
        insertStats(stats)
        insertXpLogs(logs)
    }
}

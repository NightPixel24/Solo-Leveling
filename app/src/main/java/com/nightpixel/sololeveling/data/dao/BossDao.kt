package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.Boss
import kotlinx.coroutines.flow.Flow

@Dao
interface BossDao {
    @Query("SELECT * FROM bosses ORDER BY createdAt DESC")
    fun observeBosses(): Flow<List<Boss>>

    @Insert
    suspend fun insertBoss(boss: Boss): Long

    @Update
    suspend fun updateBoss(boss: Boss)

    @Delete
    suspend fun deleteBoss(boss: Boss)

    @Query("SELECT * FROM bosses")
    suspend fun getAllOnce(): List<Boss>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBosses(bosses: List<Boss>)

    @Query("DELETE FROM bosses")
    suspend fun clearBosses()

    @Transaction
    suspend fun replaceAll(bosses: List<Boss>) {
        clearBosses()
        insertBosses(bosses)
    }
}

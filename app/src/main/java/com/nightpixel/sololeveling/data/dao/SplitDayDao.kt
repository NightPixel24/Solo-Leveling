package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.SplitDay
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDayDao {
    @Query("SELECT * FROM split_days ORDER BY orderIndex ASC, createdAt ASC")
    fun observeSplitDays(): Flow<List<SplitDay>>

    @Insert
    suspend fun insert(day: SplitDay): Long

    @Update
    suspend fun update(day: SplitDay)

    @Delete
    suspend fun delete(day: SplitDay)

    @Query("SELECT * FROM split_days")
    suspend fun getAllOnce(): List<SplitDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<SplitDay>)

    @Query("DELETE FROM split_days")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(days: List<SplitDay>) {
        clear()
        insertAll(days)
    }
}

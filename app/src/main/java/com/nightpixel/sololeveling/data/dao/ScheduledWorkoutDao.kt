package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.ScheduledWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledWorkoutDao {
    @Query("SELECT * FROM scheduled_workouts")
    fun observeAll(): Flow<List<ScheduledWorkout>>

    /** REPLACE keyed by the dayOfWeek primary key - re-assigning a day overwrites whatever was
     * scheduled there before, rather than needing a separate update/insert branch. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scheduled: ScheduledWorkout)

    @Query("DELETE FROM scheduled_workouts WHERE dayOfWeek = :dayOfWeek")
    suspend fun clearDay(dayOfWeek: Int)

    @Query("SELECT * FROM scheduled_workouts")
    suspend fun getAllOnce(): List<ScheduledWorkout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduledWorkout>)

    @Query("DELETE FROM scheduled_workouts")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<ScheduledWorkout>) {
        clear()
        insertAll(items)
    }
}

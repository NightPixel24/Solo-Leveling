package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.Habit
import com.nightpixel.sololeveling.data.entity.HabitLog
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Transaction
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabitsWithLogs(): Flow<List<HabitWithLogs>>

    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    /** Insert-or-replace keyed by the (habitId, date) unique index, so
     * logging the same day twice just overwrites rather than duplicating. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: Long, date: String)

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsOnce(): List<Habit>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogsOnce(): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<Habit>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<HabitLog>)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM habit_logs")
    suspend fun clearLogs()

    @Transaction
    suspend fun replaceAll(habits: List<Habit>, logs: List<HabitLog>) {
        clearLogs()
        clearHabits()
        insertHabits(habits)
        insertLogs(logs)
    }
}

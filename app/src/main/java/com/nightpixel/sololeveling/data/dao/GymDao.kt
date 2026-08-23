package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.Exercise
import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.GymSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Transaction
    @Query("SELECT * FROM exercises ORDER BY dayOfWeek ASC, createdAt ASC")
    fun observeExercisesWithSessions(): Flow<List<ExerciseWithSessions>>

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    /** Insert-or-replace keyed by the (exerciseId, date) unique index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: GymSession)

    @Query("DELETE FROM gym_sessions WHERE exerciseId = :exerciseId AND date = :date")
    suspend fun deleteSession(exerciseId: Long, date: String)

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesOnce(): List<Exercise>

    @Query("SELECT * FROM gym_sessions")
    suspend fun getAllSessionsOnce(): List<GymSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<GymSession>)

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM gym_sessions")
    suspend fun clearSessions()

    @Transaction
    suspend fun replaceAll(exercises: List<Exercise>, sessions: List<GymSession>) {
        clearSessions()
        clearExercises()
        insertExercises(exercises)
        insertSessions(sessions)
    }
}

package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nightpixel.sololeveling.data.entity.RoutineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_items ORDER BY createdAt ASC")
    fun observeItems(): Flow<List<RoutineItem>>

    @Insert
    suspend fun insert(item: RoutineItem): Long

    @Delete
    suspend fun delete(item: RoutineItem)

    @Query("SELECT * FROM routine_items")
    suspend fun getAllOnce(): List<RoutineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RoutineItem>)

    @Query("DELETE FROM routine_items")
    suspend fun clear()

    /** Called after HabitDao's own replaceAll during backup restore - routine_items.habitId
     * references habits(id), so the habits must already exist before this inserts. */
    @Transaction
    suspend fun replaceAll(items: List<RoutineItem>) {
        clear()
        insertAll(items)
    }
}

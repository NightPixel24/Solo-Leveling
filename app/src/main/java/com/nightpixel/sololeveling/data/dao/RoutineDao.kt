package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.RoutineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    // position ASC (see RoutineItem's doc comment) groups correctly once the caller buckets by
    // dayPart, with createdAt as the tiebreak so every pre-existing row (position 0) keeps its
    // old order and a freshly-inserted item (also position 0 until reassigned by a drag) still
    // lands after older siblings.
    @Query("SELECT * FROM routine_items ORDER BY position ASC, createdAt ASC")
    fun observeItems(): Flow<List<RoutineItem>>

    @Insert
    suspend fun insert(item: RoutineItem): Long

    @Update
    suspend fun update(item: RoutineItem)

    @Delete
    suspend fun delete(item: RoutineItem)

    /** Toggling a free-text item's own checkbox (habit-linked items instead read/write HabitLog).
     * Pass null to uncheck, an ISO date string to mark it done for that day. */
    @Query("UPDATE routine_items SET completedDate = :date WHERE id = :id")
    suspend fun setCompletedDate(id: Long, date: String?)

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

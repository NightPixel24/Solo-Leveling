package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY position ASC, createdAt ASC")
    fun observeLists(): Flow<List<TaskList>>

    @Query("SELECT * FROM task_lists")
    suspend fun getAllListsOnce(): List<TaskList>

    @Insert
    suspend fun insertList(list: TaskList): Long

    @Update
    suspend fun updateList(list: TaskList)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(lists: List<TaskList>)

    @Query("DELETE FROM task_lists")
    suspend fun clearLists()

    @Query("DELETE FROM tasks WHERE listId = :listId")
    suspend fun deleteTasksForList(listId: Long)

    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    /** No DB-level foreign key from tasks.listId to task_lists.id (would require
     * rebuilding the tasks table just to add it) - cascade deletion is handled
     * here instead. Subtasks still cascade for real via their FK on tasks.id. */
    @Transaction
    suspend fun deleteListCascading(listId: Long) {
        deleteTasksForList(listId)
        deleteListById(listId)
    }
}

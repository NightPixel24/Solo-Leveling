package com.nightpixel.sololeveling.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Manual drag order (Task.position) is the primary sort now (user feedback, 2026-09-02) -
    // done tasks still sink to the bottom, and createdAt DESC breaks position ties so untouched
    // lists keep their old newest-first order.
    @Transaction
    @Query(
        "SELECT * FROM tasks WHERE listId = :listId " +
            "ORDER BY isDone ASC, position ASC, createdAt DESC"
    )
    fun observeTasksForList(listId: Long): Flow<List<TaskWithSubtasks>>

    @Query("SELECT MIN(position) FROM tasks WHERE listId = :listId")
    suspend fun minPositionForList(listId: Long): Int?

    @Query("SELECT MAX(position) FROM tasks WHERE listId = :listId")
    suspend fun maxPositionForList(listId: Long): Int?

    /** Across all lists - used by the Dashboard's Today's Quests section (spec Section 5.4) to
     * find tasks due today regardless of which list they're filed under. */
    @Query("SELECT * FROM tasks")
    fun observeAllTasks(): Flow<List<Task>>

    @Insert
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Insert
    suspend fun insertSubtask(subtask: Subtask): Long

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<Task>

    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksOnce(): List<Subtask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<Subtask>)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM subtasks")
    suspend fun clearSubtasks()

    @Transaction
    suspend fun replaceAll(tasks: List<Task>, subtasks: List<Subtask>) {
        clearSubtasks()
        clearTasks()
        insertTasks(tasks)
        insertSubtasks(subtasks)
    }
}

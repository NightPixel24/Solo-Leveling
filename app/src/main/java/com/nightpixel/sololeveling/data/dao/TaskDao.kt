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
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isDone ASC, (dueDate IS NULL) ASC, dueDate ASC, createdAt DESC")
    fun observeTasksWithSubtasks(): Flow<List<TaskWithSubtasks>>

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

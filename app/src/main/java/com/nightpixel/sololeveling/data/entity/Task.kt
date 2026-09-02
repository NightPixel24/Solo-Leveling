package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class Priority { LOW, MEDIUM, HIGH }

@Serializable
@Entity(tableName = "tasks", indices = [Index("listId")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long = TaskList.DEFAULT_ID,
    val title: String,
    val dueDate: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val notes: String = "",
    val isDone: Boolean = false,
    /** Manual sort order within a list (user feedback, 2026-09-02: "an edit icon at the top...
     * lets me drag them around in different orders"). Lower sorts first, `createdAt DESC` breaks
     * ties so every pre-existing row (all default 0) keeps its old newest-first order until the
     * user actually drags something. A newly added HIGH-priority task is given a position below
     * the current minimum so it lands at the top automatically (same feedback); any other new
     * task is given one above the current maximum so it lands last. */
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

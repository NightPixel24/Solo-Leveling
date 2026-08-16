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
    val createdAt: Long = System.currentTimeMillis()
)

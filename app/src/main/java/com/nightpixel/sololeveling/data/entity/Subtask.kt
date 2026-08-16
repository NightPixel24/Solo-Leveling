package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
    val position: Int = 0
)

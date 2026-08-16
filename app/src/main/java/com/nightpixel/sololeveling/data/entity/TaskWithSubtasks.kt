package com.nightpixel.sololeveling.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithSubtasks(
    @Embedded val task: Task,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val subtasks: List<Subtask>
)

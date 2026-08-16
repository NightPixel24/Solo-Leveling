package com.nightpixel.sololeveling.data.backup

import com.nightpixel.sololeveling.data.entity.AppMeta
import com.nightpixel.sololeveling.data.entity.Subtask
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.TaskList
import kotlinx.serialization.Serializable

/**
 * Full JSON export/import shape (spec Section 3). One field per table -
 * extend this every time a new module adds a table, and default any new
 * field so an older backup still loads after the schema evolves.
 */
@Serializable
data class BackupData(
    val schemaVersion: Int,
    val exportedAt: String,
    val appMeta: AppMeta? = null,
    val taskLists: List<TaskList> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val subtasks: List<Subtask> = emptyList()
)

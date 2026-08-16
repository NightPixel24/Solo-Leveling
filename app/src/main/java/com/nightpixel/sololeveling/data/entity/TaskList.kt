package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "task_lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Seeded on first run (fresh installs via Callback.onCreate, existing
         * installs via MIGRATION_2_3) so pre-Phase-3.1 tasks land somewhere. */
        const val DEFAULT_ID = 1L
    }
}

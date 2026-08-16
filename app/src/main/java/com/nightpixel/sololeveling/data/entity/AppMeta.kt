package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Single-row table tracking the current schema version, used by the
 * JSON Export/Import format (spec Section 3) to know which shape of
 * backup it's reading and default-fill anything newer fields expect.
 */
@Serializable
@Entity(tableName = "app_meta")
data class AppMeta(
    @PrimaryKey val id: Int = 0,
    val schemaVersion: Int
)

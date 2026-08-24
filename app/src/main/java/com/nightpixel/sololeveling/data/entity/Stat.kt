package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One row per StatTag (spec Section 5.1), seeded at level 1 for both fresh installs and
 * existing installs upgrading through MIGRATION_9_10 - the radar chart and stat list on the
 * Dashboard expect all five to always exist. */
@Serializable
@Entity(tableName = "stats")
data class Stat(
    @PrimaryKey val tag: StatTag,
    val level: Int = 1,
    val currentXp: Int = 0
)

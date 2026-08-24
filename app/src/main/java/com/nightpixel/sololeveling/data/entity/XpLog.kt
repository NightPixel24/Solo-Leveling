package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Audit trail of every XP grant (spec Section 5.1's `XPLog`) - not read back by the app yet,
 * but kept so the tuned grant amounts (spec Section 5.2 calls them "tunable") can be analyzed
 * and adjusted later without having thrown the history away. */
@Serializable
@Entity(tableName = "xp_logs")
data class XpLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val statTag: StatTag,
    val amount: Int,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

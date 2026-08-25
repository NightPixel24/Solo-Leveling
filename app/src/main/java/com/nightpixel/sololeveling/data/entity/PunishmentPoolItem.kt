package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Spec Section 5.6 - "Minor" punishments back missed daily habits/gym days, "Major" ones back
 * missed weekly targets. */
enum class PunishmentSeverity { MINOR, MAJOR }

@Serializable
@Entity(tableName = "punishment_pool_items")
data class PunishmentPoolItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val severity: PunishmentSeverity,
    val createdAt: Long = System.currentTimeMillis()
)

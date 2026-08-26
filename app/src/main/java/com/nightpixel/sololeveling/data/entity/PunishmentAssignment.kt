package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Spec Section 5.6's `PunishmentAssignment` - a "Debt" you clear by marking it resolved.
 * [sourceRef] isn't in the spec's field list but is what makes the missed-day/week scan in
 * `data/gamification/Punishments.kt` idempotent: it's a unique key like "habit-daily:3:2026-01-05"
 * so re-scanning the same miss (e.g. reopening the Punishment Pool screen) never assigns a second
 * debt for it. */
@Serializable
@Entity(
    tableName = "punishment_assignments",
    foreignKeys = [
        ForeignKey(
            entity = PunishmentPoolItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("sourceRef", unique = true)]
)
data class PunishmentAssignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val sourceRef: String,
    val dateAssigned: String,
    val resolved: Boolean = false,
    val resolvedAt: Long? = null
)

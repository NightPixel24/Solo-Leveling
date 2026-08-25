package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A single denormalized row (id fixed at 0, the same "one settings-shaped row" pattern AppMeta
 * already uses) holding the current Gold total, kept in sync with [GoldTransaction] inserts by
 * GoldEngine so reads (Dashboard, Rewards Screen) don't need to SUM the whole ledger every time. */
@Serializable
@Entity(tableName = "gold_balance")
data class GoldBalance(
    @PrimaryKey val id: Int = 0,
    val balance: Int = 0
)

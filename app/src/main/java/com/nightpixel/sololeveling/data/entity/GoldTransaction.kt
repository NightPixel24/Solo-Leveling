package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Spec Section 5.7's Gold ledger - the same denormalized-current-value-plus-audit-trail shape
 * Stat/XpLog already established (GoldBalance/GoldTransaction ~ Stat/XpLog). [amount] is signed:
 * positive from a habit/gym completion, negative when a reward is redeemed - the only place
 * balance moves down. */
@Serializable
@Entity(tableName = "gold_transactions")
data class GoldTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

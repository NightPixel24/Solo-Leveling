package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Real-world rewards you define, split Minor/Major - the same severity split (and
 * [PunishmentSeverity] reuse) `PunishmentPoolItem` already established, just for the opposite
 * direction (a payoff for a good week/month rather than a debt for a missed one). Currency was
 * removed entirely (user feedback, 2026-08-30: "the currency system was to complex") - there's no
 * cost here, just a title and which tier it belongs to; eligibility to claim one is computed live
 * from good-week history (see `data/gamification/Rewards.kt`), not earned/spent. */
@Serializable
@Entity(tableName = "reward_pool_items")
data class RewardPoolItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val severity: PunishmentSeverity,
    val createdAt: Long = System.currentTimeMillis()
)

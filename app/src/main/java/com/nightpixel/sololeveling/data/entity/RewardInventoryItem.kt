package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A reward you've actually claimed, sitting in your inventory until you redeem it in real life
 * (user feedback, 2026-08-30: "have an inventory to store my rewards in and when i 'use' them from
 * my inventory that means i claimed it in real life"). [title]/[severity] are snapshotted from the
 * [RewardPoolItem] at claim time rather than an FK - unlike `PunishmentAssignment`'s FK+cascade to
 * its pool item, a reward already sitting in your inventory shouldn't vanish or go stale just
 * because you later edit or delete that pool item's definition. [usedAt] null means still in
 * inventory (unredeemed); set means "History" - the same current-state/history dual role
 * `PunishmentAssignment.resolved` and the old `RewardTarget.claimed` already played. */
@Serializable
@Entity(tableName = "reward_inventory")
data class RewardInventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val severity: PunishmentSeverity,
    val claimedAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null
)

package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Spec Section 5.7 - real-world rewards you define, each with a Gold cost, split into a Weekly
 * pool (smaller) and a Monthly pool (bigger, gated behind good weeks - see [RewardTarget]). */
enum class RewardPool { WEEKLY, MONTHLY }

@Serializable
@Entity(tableName = "reward_pool_items")
data class RewardPoolItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cost: Int,
    val pool: RewardPool,
    val createdAt: Long = System.currentTimeMillis()
)

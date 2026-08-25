package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Spec Section 5.7 - the one reward picked as this period's target ("each week, pick one reward
 * from the Weekly pool as your target"). [periodStart] is the Monday (Weekly) or first-of-month
 * (Monthly) date the pick applies to, unique per [pool] so picking again for the same period
 * replaces the prior pick rather than creating a second one; past rows (claimed or not) double as
 * the "reward history" the spec asks the Rewards Screen to show, the same dual current-state/
 * history role `PunishmentAssignment.resolved` already plays for Debts. */
@Serializable
@Entity(
    tableName = "reward_targets",
    foreignKeys = [
        ForeignKey(
            entity = RewardPoolItem::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index(value = ["pool", "periodStart"], unique = true)]
)
data class RewardTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pool: RewardPool,
    val periodStart: String,
    val itemId: Long,
    val claimed: Boolean = false,
    val claimedAt: Long? = null
)

package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** The seven horizons spec Section 4.7 defines - also what the Rank engine
 * (Phase 11) advances through as each tier's first goal is completed. */
enum class GoalTier(val label: String) {
    ONE_MONTH("1-Month"),
    THREE_MONTH("3-Month"),
    SIX_MONTH("6-Month"),
    YEARLY("Yearly"),
    FIVE_YEAR("5-Year"),
    TEN_YEAR("10-Year"),
    LIFETIME("Lifetime")
}

enum class GoalStatus { ACTIVE, COMPLETED, FAILED }

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tier: GoalTier,
    val title: String,
    val description: String = "",
    val targetDate: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    /** Comma-separated Task ids linked as milestones (spec 4.7) - no FK, same
     * retrofit-avoidance reasoning as Task.listId. Habit-linking is deferred;
     * Task already exposes a clean isDone signal to compute progress from. */
    val linkedTaskIds: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

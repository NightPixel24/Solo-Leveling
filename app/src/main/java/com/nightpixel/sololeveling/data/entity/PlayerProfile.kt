package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Single-row table (id fixed at 0, same pattern as AppMeta) - the display name and
 * currently-equipped title shown on the Dashboard in place of the old plain "Rank" label (user
 * feedback, 2026-08-26). `equippedTitleId` is one of the ids `data/gamification/Titles.kt`
 * generates - titles themselves aren't a table, just like Rank/Quests they're computed live from
 * existing Rank/Stat data, so only the free-text name and the equip *choice* need persisting. */
@Serializable
@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 0,
    val name: String = "Hunter",
    val equippedTitleId: String? = null
)

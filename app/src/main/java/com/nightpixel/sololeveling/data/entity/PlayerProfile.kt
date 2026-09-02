package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Single-row table (id fixed at 0, same pattern as AppMeta) - just the display name shown on the
 * Dashboard in place of the old plain "Rank" label (user feedback, 2026-08-26). Used to also carry
 * an equipped-title choice (`equippedTitleId`, `data/gamification/Titles.kt`) but the whole title
 * system was removed per later feedback (2026-08-31: "remove the rank titles... hunter is
 * meaningless to me, i would rather it just be a text field non clickable that says 'E rank'") -
 * the Dashboard now shows the live rank tier directly instead of a title. */
@Serializable
@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 0,
    val name: String = "Hunter"
)

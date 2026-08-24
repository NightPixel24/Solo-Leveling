package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One row per day (spec Section 4.6) - `date` is the primary key directly, same convention as
 * MoodEntry, since there's only ever one water tally per day. `goalBottles` is snapshotted per
 * day (not read from a separate settings table) so changing your goal later doesn't rewrite
 * history - 1 bottle = 1L per spec, so this doubles as the liters goal. */
@Serializable
@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey val date: String,
    val bottlesLogged: Int = 0,
    val goalBottles: Int = 8,
    /** Whether the +10 VIT "water goal hit" grant (spec Section 5.2) has already fired for this
     * day - tracked explicitly rather than inferred from bottlesLogged so draining and refilling
     * bottles the same day after hitting the goal doesn't re-grant XP. */
    val xpGranted: Boolean = false
)

package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A rest day the user ticked off for a specific date (user feedback, 2026-09-02: rest days are
 * "a workout like the rest of them" - a [SplitDay] with `isRest = true`, no exercises attached -
 * that you tick from the Workouts tab to record it on the calendar). This is the "was it done on
 * date X" record; a rest [SplitDay] has no [Exercise]s, so unlike a real workout there's no
 * [GymSession] to derive that from. `date` is the primary key (ISO `yyyy-MM-dd`) - one rest entry
 * per calendar day, so ticking a different rest type the same day just replaces it. Cascades on
 * the rest [SplitDay]'s deletion. */
@Serializable
@Entity(
    tableName = "rest_day_logs",
    foreignKeys = [
        ForeignKey(
            entity = SplitDay::class,
            parentColumns = ["id"],
            childColumns = ["splitDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("splitDayId")]
)
data class RestDayLog(
    @PrimaryKey val date: String,
    val splitDayId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

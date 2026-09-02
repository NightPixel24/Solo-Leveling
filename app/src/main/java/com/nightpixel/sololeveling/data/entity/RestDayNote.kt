package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A free-text line under a rest-day [SplitDay] (user feedback, 2026-09-02: a rest day "should
 * still have a drop down header similar to the other workouts design, its just that when adding
 * things under it dont show the exercise modal show a text box so i can type stuff in"). These
 * are the rest day's *definition* - what that kind of rest involves ("light stretching", "20 min
 * walk") - the same role [Exercise]s play under a normal workout, just plain text with no
 * sets/reps/targets and nothing to log per session (a rest day is recorded onto the calendar by
 * ticking the day itself, see [RestDayLog]). Cascades on the rest [SplitDay]'s deletion. */
@Serializable
@Entity(
    tableName = "rest_day_notes",
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
data class RestDayNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitDayId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

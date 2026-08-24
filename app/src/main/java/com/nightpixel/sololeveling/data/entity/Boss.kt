package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Spec Section 5.5 - PR Boss only. HP isn't a stored counter: it's computed live as
 * `targetWeight - best GymSession.actualWeight logged for [exerciseId]`, the same "derive, don't
 * store" approach as Rank (Phase 11), so there's nothing to keep in sync when a session is
 * logged or edited. `defeated`/`defeatedAt` ARE stored though, since the spec calls the win a
 * "permanent trophy" - if that were recomputed live, editing/deleting a later session could make
 * an already-won boss un-defeat itself, which isn't "permanent."
 * Streak Boss (habit/Cardio-Sport streak HP with a Hard/Easy mode toggle) is deferred - the spec
 * leaves too many undefined parameters (period length, what counts as a "successful period" for
 * an arbitrary habit vs. exercise, per-boss mode choice) to implement well without more direction. */
@Serializable
@Entity(
    tableName = "bosses",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class Boss(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val name: String,
    val targetWeight: Double,
    val defeated: Boolean = false,
    val defeatedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.nightpixel.sololeveling.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ExerciseWithSessions(
    @Embedded val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val sessions: List<GymSession>
)

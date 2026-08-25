package com.nightpixel.sololeveling.data

import androidx.room.TypeConverter
import com.nightpixel.sololeveling.data.entity.ExerciseType
import com.nightpixel.sololeveling.data.entity.GoalStatus
import com.nightpixel.sololeveling.data.entity.GoalTier
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.MoodColor
import com.nightpixel.sololeveling.data.entity.Priority
import com.nightpixel.sololeveling.data.entity.PunishmentSeverity
import com.nightpixel.sololeveling.data.entity.StatTag

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter
    fun fromHabitFrequency(frequency: HabitFrequency): String = frequency.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)

    @TypeConverter
    fun fromStatTag(tag: StatTag): String = tag.name

    @TypeConverter
    fun toStatTag(value: String): StatTag = StatTag.valueOf(value)

    @TypeConverter
    fun fromExerciseType(type: ExerciseType): String = type.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)

    @TypeConverter
    fun fromMoodColor(color: MoodColor): String = color.name

    @TypeConverter
    fun toMoodColor(value: String): MoodColor = MoodColor.valueOf(value)

    @TypeConverter
    fun fromGoalTier(tier: GoalTier): String = tier.name

    @TypeConverter
    fun toGoalTier(value: String): GoalTier = GoalTier.valueOf(value)

    @TypeConverter
    fun fromGoalStatus(status: GoalStatus): String = status.name

    @TypeConverter
    fun toGoalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)

    @TypeConverter
    fun fromPunishmentSeverity(severity: PunishmentSeverity): String = severity.name

    @TypeConverter
    fun toPunishmentSeverity(value: String): PunishmentSeverity = PunishmentSeverity.valueOf(value)
}

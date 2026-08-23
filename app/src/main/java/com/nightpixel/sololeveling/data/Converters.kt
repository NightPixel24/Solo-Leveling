package com.nightpixel.sololeveling.data

import androidx.room.TypeConverter
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.Priority
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
}

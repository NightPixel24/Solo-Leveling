package com.nightpixel.sololeveling.data

import androidx.room.TypeConverter
import com.nightpixel.sololeveling.data.entity.Priority

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)
}

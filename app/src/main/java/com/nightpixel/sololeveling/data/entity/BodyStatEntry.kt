package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Which body stat a [BodyStatEntry] records (user feedback, 2026-08-30: "I want to record my
 * body stats. So weight, blood sugar mmol/L, and blood pressure"). */
enum class BodyStatType(val label: String) {
    WEIGHT("Weight"), BLOOD_SUGAR("Blood Sugar"), BLOOD_PRESSURE("Blood Pressure")
}

/** A single dated/timed body-stat reading on the Dashboard's Health tab. One table for all three
 * types rather than three near-identical tables - `value` holds Weight (kg) or Blood Sugar
 * (mmol/L), `systolic`/`diastolic` hold Blood Pressure (mmHg); whichever pair doesn't apply to a
 * row's [type] is left null. [timestamp] is a real epoch-millis date+time (not just a date, unlike
 * `MoodEntry`/`WaterLog`) since the user explicitly wants "the date time of when these were
 * recorded" and can log more than one reading a day. */
@Serializable
@Entity(tableName = "body_stat_entries")
data class BodyStatEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BodyStatType,
    val timestamp: Long,
    val value: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null
)

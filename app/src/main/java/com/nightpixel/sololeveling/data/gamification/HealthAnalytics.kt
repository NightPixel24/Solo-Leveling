package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.BodyStatEntry
import com.nightpixel.sololeveling.data.entity.BodyStatType

/** Feeds the Dashboard Analytics tab's Health Trends charts (user feedback, 2026-08-30: "add a
 * graph and stuff to analytics for this info"). Readings are logged whenever the user takes them,
 * not once a day, so - same simplification `LineChart` already makes for stat XP trends - points
 * are plotted in chronological order by index rather than against a real time axis; the most
 * recent [limit] readings of a type are shown so the chart stays readable as history accumulates. */
private const val HEALTH_TREND_LIMIT = 30

fun weightTrend(entries: List<BodyStatEntry>, limit: Int = HEALTH_TREND_LIMIT): List<Float> =
    entries.filter { it.type == BodyStatType.WEIGHT && it.value != null }
        .sortedBy { it.timestamp }
        .takeLast(limit)
        .map { it.value!!.toFloat() }

fun bloodSugarTrend(entries: List<BodyStatEntry>, limit: Int = HEALTH_TREND_LIMIT): List<Float> =
    entries.filter { it.type == BodyStatType.BLOOD_SUGAR && it.value != null }
        .sortedBy { it.timestamp }
        .takeLast(limit)
        .map { it.value!!.toFloat() }

data class BloodPressureTrend(val systolic: List<Float>, val diastolic: List<Float>)

fun bloodPressureTrend(entries: List<BodyStatEntry>, limit: Int = HEALTH_TREND_LIMIT): BloodPressureTrend {
    val readings = entries
        .filter { it.type == BodyStatType.BLOOD_PRESSURE && it.systolic != null && it.diastolic != null }
        .sortedBy { it.timestamp }
        .takeLast(limit)
    return BloodPressureTrend(
        systolic = readings.map { it.systolic!!.toFloat() },
        diastolic = readings.map { it.diastolic!!.toFloat() }
    )
}

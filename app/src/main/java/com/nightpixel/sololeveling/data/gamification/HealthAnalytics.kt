package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.BodyStatEntry
import com.nightpixel.sololeveling.data.entity.BodyStatType
import java.time.Instant
import java.time.temporal.ChronoUnit

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

/** Time windows for the per-stat detail view a tapped Health section heading opens (user feedback,
 * 2026-08-30: "show weekly, monthly, 6 months, by year and all time"). "Month"/"6 Months"/"Year"
 * are fixed day counts rather than calendar-aware (`minusMonths`/`minusYears`) - simpler, and the
 * few days of slop this introduces near month/year boundaries doesn't matter for a trend view. */
enum class HealthPeriod(val label: String, val days: Long?) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    SIX_MONTHS("6 Months", 182),
    YEAR("Year", 365),
    ALL_TIME("All Time", null)
}

fun filterByPeriod(entries: List<BodyStatEntry>, period: HealthPeriod, now: Instant = Instant.now()): List<BodyStatEntry> {
    val days = period.days ?: return entries
    val cutoff = now.minus(days, ChronoUnit.DAYS).toEpochMilli()
    return entries.filter { it.timestamp >= cutoff }
}

/** Unlimited (period-filtered, not "last 30") variants of the trend functions above, for the
 * Health tab's own per-stat detail view - the Analytics tab's own "last 30 readings" versions
 * above are unaffected. */
fun weightValues(entries: List<BodyStatEntry>): List<Float> =
    entries.filter { it.type == BodyStatType.WEIGHT && it.value != null }
        .sortedBy { it.timestamp }
        .map { it.value!!.toFloat() }

fun bloodSugarValues(entries: List<BodyStatEntry>): List<Float> =
    entries.filter { it.type == BodyStatType.BLOOD_SUGAR && it.value != null }
        .sortedBy { it.timestamp }
        .map { it.value!!.toFloat() }

fun bloodPressureValues(entries: List<BodyStatEntry>): BloodPressureTrend {
    val readings = entries
        .filter { it.type == BodyStatType.BLOOD_PRESSURE && it.systolic != null && it.diastolic != null }
        .sortedBy { it.timestamp }
    return BloodPressureTrend(
        systolic = readings.map { it.systolic!!.toFloat() },
        diastolic = readings.map { it.diastolic!!.toFloat() }
    )
}

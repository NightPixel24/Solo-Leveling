package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.ExerciseType
import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.FoodLogEntry
import com.nightpixel.sololeveling.data.entity.FoodRating
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.MoodColor
import com.nightpixel.sololeveling.data.entity.MoodEntry
import com.nightpixel.sololeveling.data.entity.StatTag
import com.nightpixel.sololeveling.data.entity.WaterLog
import com.nightpixel.sololeveling.data.entity.XpLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Spec Section 6's Analytics tab - all computed live from data other phases already persist
 * (XpLog, HabitLog, GymSession, MoodEntry), the same "derive, don't store a second copy" approach
 * Quests/Rank/Rewards already established, rather than a new pre-aggregated analytics table. */

data class TrendPoint(val date: LocalDate, val cumulativeXp: Int)

/** Cumulative XP *gained within the window* per stat, not the stat's all-time total - a rolling
 * window shows the recent growth trajectory (the thing "trend" actually asks for) without having
 * to reconstruct historical absolute totals by walking XpLog backward from the current Stat row,
 * which would draw the same increasing-line shape for no extra information at a 30-day scale. */
fun statXpTrends(logs: List<XpLog>, today: LocalDate, days: Int = 30): Map<StatTag, List<TrendPoint>> {
    val startDate = today.minusDays((days - 1).toLong())
    val dateRange = (0 until days).map { startDate.plusDays(it.toLong()) }
    val byTag = logs.filter { it.timestamp.toLocalDate() >= startDate }.groupBy { it.statTag }
    return StatTag.entries.associateWith { tag ->
        val byDate = byTag[tag].orEmpty().groupBy { it.timestamp.toLocalDate() }.mapValues { (_, l) -> l.sumOf { it.amount } }
        var running = 0
        dateRange.map { date ->
            running += byDate[date] ?: 0
            TrendPoint(date, running)
        }
    }
}

data class HabitCompletionStat(val habitId: Long, val title: String, val percent: Float)

/** Daily habits: % of the last [days] days done. Weekly habits: % of the last [weeksBack] weeks
 * (including the current, in-progress one) whose target was met - matches how streaks are already
 * evaluated per-frequency elsewhere (`HabitsScreen`'s `dailyStreak`/`weeklyStreak`). */
fun habitCompletionRates(
    habitsWithLogs: List<HabitWithLogs>,
    today: LocalDate,
    days: Int = 30,
    weeksBack: Int = 8
): List<HabitCompletionStat> = habitsWithLogs.map { hwl ->
    val habit = hwl.habit
    val doneDates = hwl.logs.filter { it.done }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
    val percent = when (habit.frequency) {
        HabitFrequency.DAILY -> {
            val doneCount = (0 until days).count { doneDates.contains(today.minusDays(it.toLong())) }
            doneCount.toFloat() / days
        }
        HabitFrequency.WEEKLY -> {
            val thisMonday = today.with(DayOfWeek.MONDAY)
            val counts = doneDates.groupingBy { it.with(DayOfWeek.MONDAY) }.eachCount()
            val metCount = (0 until weeksBack).count { weeksAgo ->
                (counts[thisMonday.minusWeeks(weeksAgo.toLong())] ?: 0) >= habit.targetPerWeek
            }
            metCount.toFloat() / weeksBack
        }
    }
    HabitCompletionStat(habit.id, habit.title, percent.coerceIn(0f, 1f))
}

data class WeeklyVolume(val weekStart: LocalDate, val volume: Double)

/** "Volume" - sets x reps x weight for Strength sessions, minutes for Cardio/Sport - summed per
 * week over the last [weeks] weeks (including the current, in-progress one). */
fun gymVolumeByWeek(exercisesWithSessions: List<ExerciseWithSessions>, today: LocalDate, weeks: Int = 8): List<WeeklyVolume> {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    val weekStarts = (weeks - 1 downTo 0).map { thisMonday.minusWeeks(it.toLong()) }
    val allSessions = exercisesWithSessions.flatMap { ews -> ews.sessions.map { ews.exercise.type to it } }
    return weekStarts.map { weekStart ->
        val weekEnd = weekStart.plusDays(6)
        val volume = allSessions.filter { (_, session) ->
            val date = runCatching { LocalDate.parse(session.date) }.getOrNull()
            date != null && date >= weekStart && date <= weekEnd
        }.sumOf { (type, session) ->
            when (type) {
                ExerciseType.STRENGTH -> {
                    val sets = session.actualSets ?: 0
                    val reps = session.actualReps ?: 0
                    val weight = session.actualWeight ?: 0.0
                    sets * reps * weight
                }
                ExerciseType.CARDIO_SPORT -> (session.actualDuration ?: 0).toDouble()
            }
        }
        WeeklyVolume(weekStart, volume)
    }
}

data class MoodDistribution(val good: Int, val ok: Int, val bad: Int) {
    val total get() = good + ok + bad
}

fun moodDistributionForMonth(entries: List<MoodEntry>, month: YearMonth): MoodDistribution {
    val monthEntries = entries.filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d -> YearMonth.from(d) == month } == true }
    return MoodDistribution(
        good = monthEntries.count { it.color == MoodColor.GOOD },
        ok = monthEntries.count { it.color == MoodColor.OK },
        bad = monthEntries.count { it.color == MoodColor.BAD }
    )
}

data class FoodHealthDistribution(val healthy: Int, val ok: Int, val unhealthy: Int) {
    val total get() = healthy + ok + unhealthy
}

/** "Stats showing how healthy I ate" (user feedback, 2026-08-26) - same shape as
 * [moodDistributionForMonth], now that food entries carry a [FoodRating]. */
fun foodHealthDistributionForMonth(entries: List<FoodLogEntry>, month: YearMonth): FoodHealthDistribution {
    val monthEntries = entries.filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d -> YearMonth.from(d) == month } == true }
    return FoodHealthDistribution(
        healthy = monthEntries.count { it.rating == FoodRating.HEALTHY },
        ok = monthEntries.count { it.rating == FoodRating.OK },
        unhealthy = monthEntries.count { it.rating == FoodRating.UNHEALTHY }
    )
}

data class WeekGoodness(val weekStart: LocalDate, val good: Boolean)

/** "Weekly/monthly good-week history" (spec Section 6) - reuses [computeWeeklyQuests]'s exact
 * "good week" definition (Section 5.4) over the last [weeks] weeks, including the current,
 * in-progress one (shown live, same as the Dashboard's own "This Week's Quests" card). */
fun goodWeekHistory(
    today: LocalDate,
    weeks: Int,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    waterLogsByDate: Map<String, WaterLog>
): List<WeekGoodness> {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    return (weeks - 1 downTo 0).map { weeksAgo ->
        val weekStart = thisMonday.minusWeeks(weeksAgo.toLong())
        val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
        val result = computeWeeklyQuests(today, weekDays, habitsWithLogs, exercisesWithSessions, waterLogsByDate)
        WeekGoodness(weekStart, result.goodWeek)
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

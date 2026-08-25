package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.GoldTransaction
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.WaterLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Spec Section 5.7's weekly/monthly Reward Economy targets. Gold *earned* within a period
 * (positive [GoldTransaction] rows only - spends don't count against progress) measures whether
 * that period's chosen target reward is unlocked, matching "once you've earned enough Gold that
 * week it unlocks." */
fun goldEarnedSince(periodStart: LocalDate, transactions: List<GoldTransaction>): Int =
    transactions.filter { it.amount > 0 && it.timestamp.toLocalDate() >= periodStart }
        .sumOf { it.amount }

/** "If 3 of the last 4 weeks were good weeks, you unlock the ability to pick a Monthly reward"
 * (spec Section 5.7) - reuses [computeWeeklyQuests]'s "good week" definition (Section 5.4) over
 * the [n] completed weeks immediately before the current one. */
fun countGoodWeeksInLastN(
    today: LocalDate,
    n: Int,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    waterLogsByDate: Map<String, WaterLog>
): Int {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    return (1..n).count { weeksAgo ->
        val weekStart = thisMonday.minusWeeks(weeksAgo.toLong())
        val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
        computeWeeklyQuests(today, weekDays, habitsWithLogs, exercisesWithSessions, waterLogsByDate).goodWeek
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

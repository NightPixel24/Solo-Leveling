package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.PunishmentSeverity
import com.nightpixel.sololeveling.data.entity.RewardInventoryItem
import com.nightpixel.sololeveling.data.entity.WaterLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The currency-free Reward Economy (user feedback, 2026-08-30: "get rid of currency all together
 * keep the rewards section... Having a good week means in the following week I can claim 1 minor
 * reward... After 3 good weeks in a month I can claim a major reward"). Both eligibility checks
 * reuse [computeWeeklyQuests]'s exact "good week" definition (Section 5.4) rather than a second
 * one - Minor looks at the single week immediately before this one; Major reuses
 * [countGoodWeeksInLastN]'s existing trailing-4-week window (the same simplification the old
 * Gold-based Monthly-pool unlock already made for "3 good weeks in a month," carried over here
 * since calendar months don't divide evenly into Monday-Sunday weeks). */

/** "If 3 of the last 4 weeks were good weeks..." - reused unchanged from the old Gold-based
 * Monthly-unlock rule for Major reward eligibility. */
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

/** Was the single week immediately before this one (Mon-Sun) a good week? Drives Minor reward
 * eligibility - "a good week means in the following week I can claim 1 minor reward." */
fun wasLastWeekGood(
    today: LocalDate,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    waterLogsByDate: Map<String, WaterLog>
): Boolean {
    val lastWeekStart = today.with(DayOfWeek.MONDAY).minusWeeks(1)
    val lastWeekDays = (0..6).map { lastWeekStart.plusDays(it.toLong()) }
    return computeWeeklyQuests(today, lastWeekDays, habitsWithLogs, exercisesWithSessions, waterLogsByDate).goodWeek
}

/** Caps Minor claims to 1 per calendar week (Mon-Sun) - "at any time" within that week, but only
 * once. */
fun claimedMinorThisWeek(today: LocalDate, inventory: List<RewardInventoryItem>): Boolean {
    val weekStart = today.with(DayOfWeek.MONDAY)
    return inventory.any { it.severity == PunishmentSeverity.MINOR && it.claimedAt.toLocalDate() >= weekStart }
}

/** Caps Major claims to 1 per calendar month. */
fun claimedMajorThisMonth(today: LocalDate, inventory: List<RewardInventoryItem>): Boolean {
    val monthStart = today.withDayOfMonth(1)
    return inventory.any { it.severity == PunishmentSeverity.MAJOR && it.claimedAt.toLocalDate() >= monthStart }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

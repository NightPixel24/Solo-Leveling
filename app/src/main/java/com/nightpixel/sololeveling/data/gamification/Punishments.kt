package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.PunishmentSeverity
import java.time.DayOfWeek
import java.time.LocalDate

data class MissedItem(val sourceRef: String, val severity: PunishmentSeverity, val date: LocalDate)

/** Spec Section 5.6 - missing a daily habit assigns a Minor punishment; missing a whole week's
 * target for a habit/gym assigns a Major one. No background job exists (that's Phase 16), so this
 * only looks at the most recently completed day (yesterday) and week (last Mon-Sun) rather than
 * retroactively backfilling every day the app wasn't opened - the same "keep it bounded" approach
 * Quests (Phase 12) uses for its own auto-generation. Each [MissedItem] carries a stable
 * [MissedItem.sourceRef] so the caller's dedup (a unique DB index, see `PunishmentAssignment`)
 * makes repeat scans of the same miss a no-op.
 * Gym used to also assign a Minor punishment for a missed scheduled weekday; once exercises moved
 * to a user-defined rotating split with no calendar day baked in (user feedback, 2026-08-26),
 * there's no "scheduled day" left to miss day-to-day - only the weekly frequency target (see
 * [GYM_WEEKLY_TARGET]) still makes sense to hold someone to, so the per-day Minor case is dropped
 * and only the weekly Major case remains, now measured against that same frequency target. */
fun detectMissedItems(
    today: LocalDate,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>
): List<MissedItem> {
    val missed = mutableListOf<MissedItem>()

    val yesterday = today.minusDays(1)
    val yesterdayStr = yesterday.toString()

    habitsWithLogs.filter { it.habit.frequency == HabitFrequency.DAILY }.forEach { hwl ->
        val done = hwl.logs.any { it.date == yesterdayStr && it.done }
        if (!done) {
            missed += MissedItem("habit-daily:${hwl.habit.id}:$yesterdayStr", PunishmentSeverity.MINOR, yesterday)
        }
    }

    val lastWeekStart = today.with(DayOfWeek.MONDAY).minusWeeks(1)
    val lastWeekStartStr = lastWeekStart.toString()
    val lastWeekDays = (0..6).map { lastWeekStart.plusDays(it.toLong()) }

    habitsWithLogs.filter { it.habit.frequency == HabitFrequency.WEEKLY }.forEach { hwl ->
        val doneCount = lastWeekDays.count { day -> hwl.logs.any { it.date == day.toString() && it.done } }
        if (doneCount < hwl.habit.targetPerWeek) {
            missed += MissedItem("habit-weekly:${hwl.habit.id}:$lastWeekStartStr", PunishmentSeverity.MAJOR, lastWeekStart)
        }
    }

    if (exercisesWithSessions.isNotEmpty()) {
        val daysWorkedOutLastWeek = lastWeekDays.count { day ->
            exercisesWithSessions.any { ews -> ews.sessions.any { it.date == day.toString() } }
        }
        if (daysWorkedOutLastWeek < GYM_WEEKLY_TARGET) {
            missed += MissedItem("gym-weekly:$lastWeekStartStr", PunishmentSeverity.MAJOR, lastWeekStart)
        }
    }

    return missed
}

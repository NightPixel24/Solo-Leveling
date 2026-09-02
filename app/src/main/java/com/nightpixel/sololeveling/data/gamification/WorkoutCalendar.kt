package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.RestDayLog
import com.nightpixel.sololeveling.data.entity.SplitDay
import java.time.LocalDate
import java.time.YearMonth

/** Which [SplitDay] was done on which calendar date - computed live from GymSession/Exercise
 * data rather than a separately persisted "workout log" table, the same "derive, don't store a
 * second copy" approach Rank/Quests/Rewards already established. A date with sessions logged
 * against exercises from more than one split day (unusual, but not prevented) resolves to
 * whichever split day's session was logged last for that date - a reasonable tie-break, not a
 * scenario the UI needs to represent precisely.
 *
 * Rest days ([restDayLogs], user feedback 2026-09-02) fold in as any other [SplitDay] - a rest
 * day IS a SplitDay (with `isRest = true`), it just has no exercises so it can't come through the
 * GymSession path. An actual logged exercise on a date wins over a rest marker for that date. */
fun workoutCalendarForMonth(
    month: YearMonth,
    exercisesWithSessions: List<ExerciseWithSessions>,
    splitDays: List<SplitDay>,
    restDayLogs: List<RestDayLog> = emptyList()
): Map<LocalDate, SplitDay> {
    val splitDayById = splitDays.associateBy { it.id }
    val result = mutableMapOf<LocalDate, SplitDay>()
    restDayLogs.forEach { log ->
        val splitDay = splitDayById[log.splitDayId] ?: return@forEach
        val date = runCatching { LocalDate.parse(log.date) }.getOrNull() ?: return@forEach
        if (YearMonth.from(date) == month) result[date] = splitDay
    }
    exercisesWithSessions.forEach { ews ->
        val splitDay = splitDayById[ews.exercise.splitDayId] ?: return@forEach
        ews.sessions.forEach { session ->
            val date = runCatching { LocalDate.parse(session.date) }.getOrNull() ?: return@forEach
            if (YearMonth.from(date) == month) result[date] = splitDay
        }
    }
    return result
}

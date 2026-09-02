package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.Priority
import com.nightpixel.sololeveling.data.entity.RoutineItem
import com.nightpixel.sololeveling.data.entity.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The Dashboard Home tab's "At a Glance" section (user feedback, 2026-08-30: replaces Today's/
 * This Week's Quests - "start fresh... at a glance window to see what's upcoming next"). Three
 * independent "what's next" signals - Routine schedule, habits, calendar - rather than one merged
 * list, matching the user's own framing ("the next thing happening in my day... the next habit
 * upcoming... the next calendar event"). All three only consider items with an actual time set -
 * an item with no specific time isn't "next" in any orderable sense, so it's simply skipped here
 * (a plain day-part-only Routine item, or a habit with no reminder, never appears). */
data class NextUpItem(val label: String, val minuteOfDay: Int)

fun nextRoutineItem(items: List<RoutineItem>, habitTitleById: Map<Long, String>, nowMinute: Int): NextUpItem? =
    items
        .mapNotNull { item ->
            val time = item.reminderTime ?: return@mapNotNull null
            if (time < nowMinute) return@mapNotNull null
            val label = item.habitId?.let { habitTitleById[it] } ?: item.title.ifBlank { return@mapNotNull null }
            NextUpItem(label, time)
        }
        .minByOrNull { it.minuteOfDay }

/** [scheduledHabitIds] are habits already slotted into a Routine schedule item - excluded here so
 * a slotted habit doesn't show up twice in "Next Up" (once as its Routine item, once as itself)
 * when both carry a time (user feedback, 2026-09-02: "the habit and routine show up twice for
 * upcoming... it's the same information"). */
fun nextHabit(
    habitsWithLogs: List<HabitWithLogs>,
    today: LocalDate,
    nowMinute: Int,
    scheduledHabitIds: Set<Long> = emptySet()
): NextUpItem? {
    val todayStr = today.toString()
    return habitsWithLogs
        .asSequence()
        .filter { it.habit.frequency == HabitFrequency.DAILY }
        .filter { it.habit.id !in scheduledHabitIds }
        .filter { hwl -> hwl.logs.none { it.date == todayStr && it.done } }
        .mapNotNull { hwl ->
            val time = hwl.habit.reminderTime ?: return@mapNotNull null
            if (time < nowMinute) return@mapNotNull null
            NextUpItem(hwl.habit.title, time)
        }
        .minByOrNull { it.minuteOfDay }
}

/** Incomplete HIGH-priority tasks from the Daily list, shown as their own "Next Up" rows (user
 * feedback, 2026-09-02: "in the next up section on the home screen also show the high priority
 * tasks only from the daily list"). Ordered by the same manual `position` the Tasks screen uses,
 * capped so a long backlog doesn't crowd the card. */
fun highPriorityDailyTasks(tasks: List<Task>, dailyListId: Long, limit: Int = 3): List<String> =
    tasks
        .filter { it.listId == dailyListId && !it.isDone && it.priority == Priority.HIGH }
        .sortedWith(compareBy({ it.position }, { -it.createdAt }))
        .take(limit)
        .map { it.title }

/** Today's next event only - `start` is epoch millis, compared directly against `now` and the
 * start of tomorrow (local time) rather than converting every event to a LocalDate first. */
fun nextCalendarEvent(events: List<CalendarEventCache>, now: Instant): CalendarEventCache? {
    val startOfTomorrow = now.atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant()
    return events
        .filter { it.start >= now.toEpochMilli() && it.start < startOfTomorrow.toEpochMilli() }
        .minByOrNull { it.start }
}

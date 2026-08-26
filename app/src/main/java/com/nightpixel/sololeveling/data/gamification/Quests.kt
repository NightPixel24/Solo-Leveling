package com.nightpixel.sololeveling.data.gamification

import com.nightpixel.sololeveling.data.entity.ExerciseWithSessions
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import com.nightpixel.sololeveling.data.entity.HabitWithLogs
import com.nightpixel.sololeveling.data.entity.Task
import com.nightpixel.sololeveling.data.entity.WaterLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Spec Section 5.4 - Quests are computed live from each source's own data rather than persisted
 * as a separate `Quest` row: a habit/task/session/water-goal/mood-entry already records its own
 * done state, so a stored Quest row would just be a second, potentially-stale copy of the same
 * fact. This also makes "auto-generated every morning" free - the list is simply always today's,
 * with no rollover job to write. */
data class QuestItem(val label: String, val done: Boolean)

/** This app's own tuned weekly workout-frequency target (see [computeWeeklyQuests]'s doc comment). */
const val GYM_WEEKLY_TARGET = 3

/** "Today's Quests" (spec Section 5.4): one entry per daily habit, one for logging any workout
 * today, the water goal, tonight's mood check-in, and tasks due today. Weekly habits are excluded -
 * they don't have a specific "due today," only a weekly target, which belongs to Weekly Quests.
 * Gym used to itemize each exercise scheduled today (matched by a fixed weekday); once exercises
 * moved to a user-defined rotating split with no calendar day baked in (user feedback,
 * 2026-08-26), there's no longer a way to know which exercises are "due today" - only whether a
 * workout happened, so this collapses to a single quest. */
fun computeDailyQuests(
    today: LocalDate,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    waterLog: WaterLog?,
    moodLoggedToday: Boolean,
    allTasks: List<Task>
): List<QuestItem> {
    val todayStr = today.toString()
    val habitQuests = habitsWithLogs
        .filter { it.habit.frequency == HabitFrequency.DAILY }
        .map { hwl -> QuestItem(hwl.habit.title, hwl.logs.any { it.date == todayStr && it.done }) }
    val gymQuests = if (exercisesWithSessions.isNotEmpty()) {
        listOf(QuestItem("Log a workout today", exercisesWithSessions.any { ews -> ews.sessions.any { it.date == todayStr } }))
    } else {
        emptyList()
    }
    val waterQuest = QuestItem(
        "Hit water goal",
        waterLog != null && waterLog.bottlesLogged >= waterLog.goalBottles
    )
    val moodQuest = QuestItem("Rate today's mood", moodLoggedToday)
    val taskQuests = allTasks
        .filter { it.dueDate != null && it.dueDate.toLocalDate() == today }
        .map { QuestItem(it.title, it.isDone) }
    return habitQuests + gymQuests + listOf(waterQuest, moodQuest) + taskQuests
}

data class WeeklyQuestResult(val items: List<QuestItem>, val goodWeek: Boolean)

/** "Weekly Quests" (spec Section 5.4) - the three example targets, evaluated Monday..today for
 * the week in progress. Days before today must be fully satisfied to keep a quest "on track";
 * today itself is never counted as a miss (the day isn't over), so a quest can show as on-track
 * before you've necessarily done today's part yet. A week where all three are on track/met counts
 * as a "good week" once it's actually over - spec Section 5.7 uses that for monthly rewards.
 * Gym's target used to be "complete all scheduled gym days" against a fixed weekday schedule;
 * with a rotating split (user feedback, 2026-08-26) there's no fixed schedule left to check
 * fidelity against, so this becomes a plain workout-frequency target instead - this app's own
 * tuned number (3x/week), same "no spec-given amount" reasoning as other tuned XP values. */
fun computeWeeklyQuests(
    today: LocalDate,
    weekDays: List<LocalDate>,
    habitsWithLogs: List<HabitWithLogs>,
    exercisesWithSessions: List<ExerciseWithSessions>,
    waterLogsByDate: Map<String, WaterLog>
): WeeklyQuestResult {
    val pastDays = weekDays.filter { it.isBefore(today) }

    val daysWorkedOut = weekDays.count { day ->
        exercisesWithSessions.any { ews -> ews.sessions.any { it.date == day.toString() } }
    }
    val gymQuest = QuestItem(
        "Work out $daysWorkedOut/${GYM_WEEKLY_TARGET} days",
        exercisesWithSessions.isEmpty() || daysWorkedOut >= GYM_WEEKLY_TARGET
    )

    val daysHitGoal = weekDays.count { day ->
        waterLogsByDate[day.toString()]?.let { it.bottlesLogged >= it.goalBottles } == true
    }
    val waterQuest = QuestItem("Hit water goal $daysHitGoal/7 days", daysHitGoal >= 6)

    val dailyHabits = habitsWithLogs.filter { it.habit.frequency == HabitFrequency.DAILY }
    val missedAny = dailyHabits.any { hwl ->
        pastDays.any { day -> !hwl.logs.any { it.date == day.toString() && it.done } }
    }
    val habitsQuest = QuestItem("Zero missed daily habits", dailyHabits.isNotEmpty() && !missedAny)

    val items = listOf(gymQuest, waterQuest, habitsQuest)
    return WeeklyQuestResult(items, goodWeek = items.all { it.done })
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

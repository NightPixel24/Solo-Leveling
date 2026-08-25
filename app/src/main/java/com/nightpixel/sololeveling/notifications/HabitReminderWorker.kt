package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.HabitFrequency
import java.time.LocalDate
import java.time.LocalDateTime

/** Spec Section 7 - "reminders for habits due today (time configurable per habit)," using each
 * [com.nightpixel.sololeveling.data.entity.Habit.reminderTime] captured back in Phase 4. Runs
 * every 15 minutes (WorkManager's periodic minimum) and fires for any habit whose reminder time
 * falls in the window just passed, rather than scheduling one exact-time job per habit - that
 * would need cancel/reschedule wiring every time a habit is added, edited, or deleted, whereas a
 * periodic sweep just picks up habit changes on its next tick for free. The 15-minute window means
 * a reminder can land up to ~15 minutes late, an acceptable trade for that simplicity. Weekly
 * habits are excluded - they have no specific "due today," the same reasoning `computeDailyQuests`
 * (Phase 12) already established for Today's Quests. */
class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SoloLevelingApplication
        val now = LocalDateTime.now()
        val nowMinute = now.hour * 60 + now.minute
        val today = LocalDate.now().toString()

        val habits = app.database.habitDao().getAllHabitsOnce()
        val logs = app.database.habitDao().getAllLogsOnce()

        habits.filter { it.frequency == HabitFrequency.DAILY }.forEach { habit ->
            val reminderMinute = habit.reminderTime ?: return@forEach
            val minutesSinceReminder = ((nowMinute - reminderMinute) + 1440) % 1440
            if (minutesSinceReminder > 14) return@forEach

            val doneToday = logs.any { it.habitId == habit.id && it.date == today && it.done }
            if (!doneToday) {
                Notifier.show(
                    applicationContext,
                    NotificationChannels.HABITS,
                    (HABIT_NOTIF_ID_BASE + habit.id).toInt(),
                    "Habit reminder",
                    habit.title
                )
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "habit_reminder_sweep"
        private const val HABIT_NOTIF_ID_BASE = 10_000L
    }
}

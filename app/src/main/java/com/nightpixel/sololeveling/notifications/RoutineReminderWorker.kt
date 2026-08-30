package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nightpixel.sololeveling.SoloLevelingApplication
import java.time.LocalDateTime

/** User feedback, 2026-08-30: "if there are any time specific things in my schedule, I should
 * also be notified for that... at nine PM it might say take my tablets." Same periodic-sweep
 * shape [HabitReminderWorker] already established (every 15 minutes, fire for anything whose
 * reminder time falls in the window just passed) rather than one self-rescheduling exact-time job
 * per item - a routine item can be added/edited/deleted freely with no cancel/reschedule wiring
 * needed, at the cost of a reminder landing up to ~15 minutes late, the same trade-off
 * HabitReminderWorker already accepts. Fires using the item's effective title - a slotted habit's
 * own title (looked up live, same "derive don't store" reasoning as the Schedule tab's row itself)
 * or the free-text title otherwise. */
class RoutineReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SoloLevelingApplication
        val now = LocalDateTime.now()
        val nowMinute = now.hour * 60 + now.minute

        val items = app.database.routineDao().getAllOnce()
        val habitsById = app.database.habitDao().getAllHabitsOnce().associateBy { it.id }

        items.forEach { item ->
            val reminderMinute = item.reminderTime ?: return@forEach
            val minutesSinceReminder = ((nowMinute - reminderMinute) + 1440) % 1440
            if (minutesSinceReminder > 14) return@forEach

            val text = item.habitId?.let { habitsById[it]?.title } ?: item.title.ifBlank { return@forEach }
            Notifier.show(
                applicationContext,
                NotificationChannels.ROUTINE,
                (ROUTINE_NOTIF_ID_BASE + item.id).toInt(),
                "Schedule reminder",
                text
            )
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "routine_reminder_sweep"
        // 10_000/20_000/30_000/40_000 are already Habit/Water/Mood/Gym's own bases, and
        // ReviewReminderWorker uses 50_000/50_001 - found via a real device notification dump
        // showing this worker's post landing under Review's channel/id instead of its own.
        private const val ROUTINE_NOTIF_ID_BASE = 60_000L
    }
}

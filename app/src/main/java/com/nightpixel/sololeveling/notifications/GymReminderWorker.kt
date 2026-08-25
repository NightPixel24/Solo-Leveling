package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nightpixel.sololeveling.SoloLevelingApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Spec Section 7 - "gym day reminder on scheduled days," fired once daily at
 * [ReminderTimes.GYM] when today has at least one [com.nightpixel.sololeveling.data.entity.Exercise]
 * (matched by `dayOfWeek`, same as `GymScreen`'s own weekly grouping) not yet all logged for today. */
class GymReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SoloLevelingApplication
        val today = LocalDate.now()
        val todayStr = today.toString()
        val scheduledToday = app.database.gymDao().observeExercisesWithSessions().first()
            .filter { it.exercise.dayOfWeek == today.dayOfWeek.value }
        val allLoggedAlready = scheduledToday.all { ews -> ews.sessions.any { it.date == todayStr } }

        if (scheduledToday.isNotEmpty() && !allLoggedAlready) {
            Notifier.show(
                applicationContext,
                NotificationChannels.GYM,
                GYM_NOTIF_ID,
                "Gym day",
                "${scheduledToday.size} exercise${if (scheduledToday.size == 1) "" else "s"} scheduled today"
            )
        }
        scheduleDailyAt(applicationContext, WORK_NAME, GymReminderWorker::class.java, ReminderTimes.GYM)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "gym_reminder"
        private const val GYM_NOTIF_ID = 40_000
    }
}

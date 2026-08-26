package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nightpixel.sololeveling.SoloLevelingApplication
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Spec Section 7 - "gym day reminder on scheduled days," originally fired only on a fixed
 * weekday an exercise was pinned to. Once exercises moved to a user-defined rotating split with
 * no calendar day baked in (user feedback, 2026-08-26), there's no more "scheduled day" to check -
 * firing every single day instead (now that every day is a potential workout day) would just be
 * daily spam for anyone running a routine with real rest days. Fires instead only once an actual
 * gap has formed - no workout logged in 2+ days - which is closer to the original's intent
 * (nudge before a scheduled day gets missed) without assuming a fixed schedule exists. */
class GymReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SoloLevelingApplication
        val today = LocalDate.now()
        val hasRoutine = app.database.splitDayDao().getAllOnce().isNotEmpty()
        val lastWorkoutDate = app.database.gymDao().getAllSessionsOnce()
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .maxOrNull()
        val daysSinceLastWorkout = lastWorkoutDate?.let { ChronoUnit.DAYS.between(it, today) } ?: Long.MAX_VALUE

        if (hasRoutine && daysSinceLastWorkout >= 2) {
            Notifier.show(
                applicationContext,
                NotificationChannels.GYM,
                GYM_NOTIF_ID,
                "Gym reminder",
                if (lastWorkoutDate == null) {
                    "No workouts logged yet - time to start your split"
                } else {
                    "It's been $daysSinceLastWorkout days since your last workout"
                }
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

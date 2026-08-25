package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.LocalDate

/** Spec Section 7 - "weekly review prompt (e.g. Sunday evening) and monthly review prompt," both
 * driven by one daily check at [ReminderTimes.REVIEW] rather than two separately-scheduled jobs:
 * WorkManager's `PeriodicWorkRequest` has no notion of "monthly" (months vary in length), so a
 * once-a-day check that asks "is today Sunday?" / "is today the last day of the month?" covers
 * both cadences with the same self-rescheduling chain the other daily reminders already use. */
class ReviewReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        if (today.dayOfWeek == DayOfWeek.SUNDAY) {
            Notifier.show(
                applicationContext,
                NotificationChannels.REVIEW,
                WEEKLY_NOTIF_ID,
                "Weekly review",
                "Take a few minutes to look back at this week"
            )
        }
        if (today.plusDays(1).dayOfMonth == 1) {
            Notifier.show(
                applicationContext,
                NotificationChannels.REVIEW,
                MONTHLY_NOTIF_ID,
                "Monthly review",
                "The month's wrapping up - take a look back at your progress"
            )
        }

        scheduleDailyAt(applicationContext, WORK_NAME, ReviewReminderWorker::class.java, ReminderTimes.REVIEW)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "review_reminder"
        private const val WEEKLY_NOTIF_ID = 50_000
        private const val MONTHLY_NOTIF_ID = 50_001
    }
}

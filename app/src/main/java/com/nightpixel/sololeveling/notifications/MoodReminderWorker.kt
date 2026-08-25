package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nightpixel.sololeveling.SoloLevelingApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Spec Section 7 - "evening mood check-in prompt," fired once daily at [ReminderTimes.MOOD_CHECKIN]
 * if today doesn't have a [com.nightpixel.sololeveling.data.entity.MoodEntry] yet, then reschedules
 * itself for tomorrow's occurrence (see `scheduleDailyAt`). */
class MoodReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SoloLevelingApplication
        val today = LocalDate.now().toString()
        val loggedToday = app.database.moodDao().observeEntries().first().any { it.date == today }
        if (!loggedToday) {
            Notifier.show(
                applicationContext,
                NotificationChannels.MOOD,
                MOOD_NOTIF_ID,
                "How was your day?",
                "Rate today's mood before you wrap up"
            )
        }
        scheduleDailyAt(applicationContext, WORK_NAME, MoodReminderWorker::class.java, ReminderTimes.MOOD_CHECKIN)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "mood_reminder"
        private const val MOOD_NOTIF_ID = 30_000
    }
}

package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nightpixel.sololeveling.SoloLevelingApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/** Spec Section 7 - "water reminders spaced through the day toward your daily goal." Unlike the
 * habit/mood/gym/review reminders, there's no single target time to hit - a plain periodic sweep
 * (every 2 hours, active only between 9am-9pm so it doesn't wake anyone) is a closer fit than the
 * self-rescheduling exact-time pattern the other workers use, and it silently no-ops once the
 * day's goal is already met rather than nagging further. */
class WaterReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val now = LocalTime.now()
        if (now.isBefore(ACTIVE_START) || now.isAfter(ACTIVE_END)) return Result.success()

        val app = applicationContext as SoloLevelingApplication
        val today = LocalDate.now().toString()
        val log = app.database.waterDao().observeLog(today).first()
        val goalBottles = log?.goalBottles ?: 8
        val bottlesLogged = log?.bottlesLogged ?: 0
        if (bottlesLogged >= goalBottles) return Result.success()

        val remaining = goalBottles - bottlesLogged
        Notifier.show(
            applicationContext,
            NotificationChannels.WATER,
            WATER_NOTIF_ID,
            "Stay hydrated",
            "$remaining bottle${if (remaining == 1) "" else "s"} left to hit today's water goal"
        )
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "water_reminder_sweep"
        private const val WATER_NOTIF_ID = 20_000
        private val ACTIVE_START = LocalTime.of(9, 0)
        private val ACTIVE_END = LocalTime.of(21, 0)
    }
}

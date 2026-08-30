package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Single entry point that wires up every spec Section 7 reminder - called once from
 * [com.nightpixel.sololeveling.SoloLevelingApplication.onCreate]. The periodic sweeps (Habit,
 * Water) use `ExistingPeriodicWorkPolicy.KEEP` so relaunching the app doesn't restart their
 * interval; the exact-time reminders (Mood/Gym/Review) go through `scheduleDailyAt`, which is
 * idempotent by design (see its doc comment) so calling it on every app start is harmless. */
object ReminderScheduler {
    fun scheduleAll(context: Context) {
        NotificationChannels.ensureCreated(context)

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            HabitReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(HabitReminderWorker::class.java, 15, TimeUnit.MINUTES).build()
        )
        workManager.enqueueUniquePeriodicWork(
            WaterReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(WaterReminderWorker::class.java, 2, TimeUnit.HOURS).build()
        )
        workManager.enqueueUniquePeriodicWork(
            RoutineReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(RoutineReminderWorker::class.java, 15, TimeUnit.MINUTES).build()
        )

        scheduleDailyAt(context, MoodReminderWorker.WORK_NAME, MoodReminderWorker::class.java, ReminderTimes.MOOD_CHECKIN)
        scheduleDailyAt(context, GymReminderWorker.WORK_NAME, GymReminderWorker::class.java, ReminderTimes.GYM)
        scheduleDailyAt(context, ReviewReminderWorker.WORK_NAME, ReviewReminderWorker::class.java, ReminderTimes.REVIEW)
    }
}

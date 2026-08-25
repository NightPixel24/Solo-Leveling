package com.nightpixel.sololeveling.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Default fire times for the reminders that want a specific time of day rather than "roughly
 * spaced through the day" (spec Section 7) - not user-configurable, unlike a habit's own
 * per-habit `reminderTime`; tunable later the same way XP/Gold grant amounts are. */
object ReminderTimes {
    val MOOD_CHECKIN: LocalTime = LocalTime.of(20, 0)
    val GYM: LocalTime = LocalTime.of(8, 0)
    val REVIEW: LocalTime = LocalTime.of(20, 30)
}

/** Millis until the next occurrence of [target] (today if still ahead of [from], otherwise
 * tomorrow) - the standard trick for making a self-rescheduling WorkManager job land on a fixed
 * wall-clock time, since `PeriodicWorkRequest` has no notion of "run at 8pm daily," only a fixed
 * interval from whenever it happened to first enqueue. */
fun millisUntilNext(target: LocalTime, from: LocalDateTime = LocalDateTime.now()): Long {
    var next = from.toLocalDate().atTime(target)
    if (!next.isAfter(from)) next = next.plusDays(1)
    return ChronoUnit.MILLIS.between(from, next)
}

/** Enqueues (or re-enqueues) a single [workerClass] run at the next occurrence of [time], as
 * unique work named [workName]. Called both to kick off a reminder chain at app startup and by
 * the worker itself at the end of `doWork()` to schedule tomorrow's occurrence - `REPLACE` is
 * correct in both cases: at startup it converges on the same wall-clock target no matter how many
 * times the app is relaunched that day, and after a run it swaps the just-completed work item for
 * the next one. */
fun <T : ListenableWorker> scheduleDailyAt(context: Context, workName: String, workerClass: Class<T>, time: LocalTime) {
    val request = OneTimeWorkRequest.Builder(workerClass)
        .setInitialDelay(millisUntilNext(time), TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
}

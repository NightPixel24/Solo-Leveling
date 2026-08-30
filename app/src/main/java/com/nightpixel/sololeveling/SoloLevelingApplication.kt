package com.nightpixel.sololeveling

import android.app.Application
import com.nightpixel.sololeveling.data.AppDatabase
import com.nightpixel.sololeveling.data.backup.BackupManager
import com.nightpixel.sololeveling.data.calendar.CalendarApiClient
import com.nightpixel.sololeveling.data.calendar.GoogleAuthManager
import com.nightpixel.sololeveling.data.gamification.XpEngine
import com.nightpixel.sololeveling.notifications.ReminderScheduler

class SoloLevelingApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val backupManager: BackupManager by lazy { BackupManager(database) }
    val xpEngine: XpEngine by lazy { XpEngine(database.statDao()) }
    val googleAuthManager: GoogleAuthManager by lazy {
        GoogleAuthManager(getString(R.string.google_web_client_id))
    }
    val calendarApiClient: CalendarApiClient by lazy { CalendarApiClient() }

    /** Google/Play Services already remembers the Calendar OAuth grant persistently (across
     * navigation and app restarts) - this just caches that fact for the current process so
     * CalendarScreen doesn't have to re-ask every time its composable is recreated (e.g. on
     * navigating away and back), only once per cold start. */
    var calendarAccessGranted: Boolean = false

    override fun onCreate() {
        super.onCreate()
        // Spec Section 7 - local reminders. Scheduling (not just showing) them doesn't need the
        // POST_NOTIFICATIONS runtime permission up front - Notifier checks it right before each
        // actual notification, so a "not granted yet" user still gets everything wired up and
        // simply starts seeing reminders once they grant it later.
        ReminderScheduler.scheduleAll(this)
    }
}

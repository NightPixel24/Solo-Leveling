package com.nightpixel.sololeveling.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Spec Section 7 - one channel per reminder category so the user can mute/tune each
 * independently from system settings, rather than one catch-all channel for everything. */
object NotificationChannels {
    const val HABITS = "habit_reminders"
    const val WATER = "water_reminders"
    const val MOOD = "mood_reminders"
    const val GYM = "gym_reminders"
    const val REVIEW = "review_reminders"
    const val ROUTINE = "routine_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            HABITS to "Habit reminders",
            WATER to "Water reminders",
            MOOD to "Mood check-in",
            GYM to "Gym day reminders",
            REVIEW to "Weekly/monthly review",
            ROUTINE to "Schedule reminders"
        ).forEach { (id, name) ->
            manager.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT))
        }
    }
}

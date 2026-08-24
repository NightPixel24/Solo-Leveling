package com.nightpixel.sololeveling.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Local mirror of Google Calendar events for offline viewing (spec Section 9).
 * `start`/`end`/`syncedAt` are epoch millis; Google Calendar is always the
 * source of truth - this cache is read-only from the app's perspective and
 * gets overwritten wholesale on every sync rather than diffed. */
@Serializable
@Entity(tableName = "calendar_event_cache")
data class CalendarEventCache(
    @PrimaryKey val googleEventId: String,
    val title: String,
    val start: Long,
    val end: Long,
    val syncedAt: Long = System.currentTimeMillis()
)

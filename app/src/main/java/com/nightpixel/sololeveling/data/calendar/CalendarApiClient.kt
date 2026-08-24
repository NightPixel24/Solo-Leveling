package com.nightpixel.sololeveling.data.calendar

import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Plain REST calls against Calendar API v3 rather than the heavyweight
 * google-api-client library, matching the rest of the app's "no backend,
 * keep it simple" approach (see CLAUDE.md).
 */
class CalendarApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listUpcomingEvents(accessToken: String, maxResults: Int = 100): List<CalendarEventCache> =
        withContext(Dispatchers.IO) {
            val timeMin = URLEncoder.encode(Instant.now().toString(), "UTF-8")
            val url = URL(
                "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                    "?timeMin=$timeMin&maxResults=$maxResults&singleEvents=true&orderBy=startTime"
            )
            val body = request(url, "GET", accessToken, null)
            parseEvents(body)
        }

    suspend fun createEvent(accessToken: String, title: String, startMillis: Long, endMillis: Long): CalendarEventCache =
        withContext(Dispatchers.IO) {
            val url = URL("https://www.googleapis.com/calendar/v3/calendars/primary/events")
            val zone = ZoneId.systemDefault().id
            val payload = buildJsonObject {
                put("summary", title)
                putJsonObject("start") {
                    put("dateTime", Instant.ofEpochMilli(startMillis).toString())
                    put("timeZone", zone)
                }
                putJsonObject("end") {
                    put("dateTime", Instant.ofEpochMilli(endMillis).toString())
                    put("timeZone", zone)
                }
            }
            val body = request(url, "POST", accessToken, payload.toString())
            parseEvent(json.parseToJsonElement(body).jsonObject)
        }

    private fun request(url: URL, method: String, accessToken: String, body: String?): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 15_000
            readTimeout = 15_000
            if (body != null) {
                doOutput = true
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body) }
            }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        check(code in 200..299) { "Calendar API error $code: $text" }
        return text
    }

    private fun parseEvents(body: String): List<CalendarEventCache> {
        val items = json.parseToJsonElement(body).jsonObject["items"]?.jsonArray ?: JsonArray(emptyList())
        return items.mapNotNull { runCatching { parseEvent(it.jsonObject) }.getOrNull() }
    }

    private fun parseEvent(obj: JsonObject): CalendarEventCache {
        val id = obj.getValue("id").jsonPrimitive.content
        val title = obj["summary"]?.jsonPrimitive?.contentOrNull ?: "(untitled)"
        val start = parseEventTime(obj["start"]?.jsonObject)
        val end = parseEventTime(obj["end"]?.jsonObject)
        return CalendarEventCache(googleEventId = id, title = title, start = start, end = end)
    }

    private fun parseEventTime(obj: JsonObject?): Long {
        val dateTime = obj?.get("dateTime")?.jsonPrimitive?.contentOrNull
        val date = obj?.get("date")?.jsonPrimitive?.contentOrNull
        return when {
            dateTime != null -> Instant.parse(dateTime).toEpochMilli()
            date != null -> LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            else -> 0L
        }
    }
}

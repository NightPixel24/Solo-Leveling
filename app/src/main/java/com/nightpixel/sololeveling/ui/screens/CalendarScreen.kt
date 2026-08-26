package com.nightpixel.sololeveling.ui.screens

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SoloLevelingApplication
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Google/Play Services remembers the Calendar grant persistently (across navigation and app
    // restarts) - this just tracks whether *this composition* already knows that, seeded from
    // the Application-level cache so revisiting the tab within the same process is instant, plus
    // a silent re-check below for cold starts where the cache itself was reset.
    var hasAccess by remember { mutableStateOf(app.calendarAccessGranted) }
    // The cache only proves access was granted; it never proves the opposite (a cold process
    // always starts with hasAccess=false even for an already-connected account), so without this
    // the "Connect" button flashed for a moment on every cold start before the silent re-check
    // below resolved and flipped hasAccess to true.
    var checkingAccess by remember { mutableStateOf(!app.calendarAccessGranted) }
    var connecting by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingTokenAction by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.WEEK) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val events by app.database.calendarDao().observeEvents().collectAsState(initial = emptyList())

    fun showError(message: String?) {
        scope.launch { snackbarHostState.showSnackbar(message ?: "Something went wrong") }
    }

    fun setAccess(granted: Boolean) {
        hasAccess = granted
        app.calendarAccessGranted = granted
    }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val action = pendingTokenAction
        pendingTokenAction = null
        if (action != null) {
            app.googleAuthManager.handleConsentResult(
                context = context,
                data = result.data,
                onAuthorized = action,
                onError = { showError(it.message) }
            )
        }
    }

    fun withCalendarAccess(onToken: (String) -> Unit) {
        app.googleAuthManager.requestCalendarAccess(
            context = context,
            onAuthorized = onToken,
            onNeedsConsent = { intentSender: IntentSender ->
                pendingTokenAction = onToken
                consentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            },
            onError = { showError(it.message) }
        )
    }

    fun sync(token: String) {
        syncing = true
        scope.launch {
            runCatching { app.calendarApiClient.listUpcomingEvents(token) }
                .onSuccess { app.database.calendarDao().replaceAll(it) }
                .onFailure { showError(it.message) }
            syncing = false
        }
    }

    fun connect() {
        connecting = true
        scope.launch {
            runCatching { app.googleAuthManager.signIn(context) }
                .onSuccess {
                    withCalendarAccess { token -> setAccess(true); sync(token) }
                }
                .onFailure { showError(it.message) }
            connecting = false
        }
    }

    // Cold start (fresh process): the Application-level cache reset too, so re-check with Play
    // Services whether the Calendar grant is still there before showing "Connect" - silent-only,
    // never launches the consent screen unprompted (onNeedsConsent/onError just leave the button
    // showing, same as if we'd never checked).
    LaunchedEffect(Unit) {
        if (!hasAccess) {
            app.googleAuthManager.requestCalendarAccess(
                context = context,
                onAuthorized = { token -> setAccess(true); checkingAccess = false; sync(token) },
                onNeedsConsent = { checkingAccess = false },
                onError = { checkingAccess = false }
            )
        } else {
            checkingAccess = false
            withCalendarAccess { token -> sync(token) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    if (hasAccess) {
                        IconButton(onClick = { withCalendarAccess { token -> sync(token) } }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (hasAccess) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add event")
                }
            }
        }
    ) { innerPadding ->
        when {
            checkingAccess -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !hasAccess -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Connect your Google account to see and create calendar events here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(onClick = ::connect, enabled = !connecting) {
                        Text(if (connecting) "Connecting..." else "Connect Google Calendar")
                    }
                }
            }
            syncing && events.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(Modifier.fillMaxSize().padding(innerPadding)) {
                TabRow(selectedTabIndex = viewMode.ordinal) {
                    CalendarViewMode.entries.forEach { mode ->
                        Tab(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            text = { Text(mode.label) }
                        )
                    }
                }
                when (viewMode) {
                    CalendarViewMode.WEEK -> WeekView(
                        selectedDate = selectedDate,
                        events = events,
                        onSelectDate = { selectedDate = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    CalendarViewMode.MONTH -> MonthView(
                        selectedDate = selectedDate,
                        events = events,
                        onSelectDate = {
                            selectedDate = it
                            viewMode = CalendarViewMode.WEEK
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, start, end ->
                showAddDialog = false
                withCalendarAccess { token ->
                    scope.launch {
                        runCatching { app.calendarApiClient.createEvent(token, title, start, end) }
                            .onSuccess { sync(token) }
                            .onFailure { showError(it.message) }
                    }
                }
            }
        )
    }
}

private enum class CalendarViewMode(val label: String) { WEEK("Week"), MONTH("Month") }

private fun eventsByLocalDate(events: List<CalendarEventCache>): Map<LocalDate, List<CalendarEventCache>> =
    events.groupBy { Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate() }

/** Replaces the old flat agenda list with an actual visual calendar (spec Section 4.1: "Month/
 * week/day views") - a 7-day strip for the selected week, each cell tappable, with the selected
 * day's events listed below. Only one day's events are shown at a time (rather than the whole
 * week grouped by day) since that's what the day strip's selection is for. */
@Composable
private fun WeekView(
    selectedDate: LocalDate,
    events: List<CalendarEventCache>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val eventsByDate = remember(events) { eventsByLocalDate(events) }
    val today = remember { LocalDate.now() }
    val weekStart = remember(selectedDate) { selectedDate.with(DayOfWeek.MONDAY) }
    val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val rangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSelectDate(selectedDate.minusWeeks(1)) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week")
            }
            Text(
                "${weekDays.first().format(rangeFormatter)} - ${weekDays.last().format(rangeFormatter)}",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { onSelectDate(selectedDate.plusWeeks(1)) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next week")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEachIndexed { index, date ->
                val selected = date == selectedDate
                val hasEvents = eventsByDate[date]?.isNotEmpty() == true
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .then(
                            if (!selected && date == today) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelectDate(date) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dayLabels[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(
                                if (hasEvents) {
                                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                CircleShape
                            )
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

        val dayEvents = remember(eventsByDate, selectedDate) {
            eventsByDate[selectedDate].orEmpty().sortedBy { it.start }
        }
        if (dayEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No events on this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayEvents, key = { it.googleEventId }) { event -> EventCard(event, timeFormatter) }
            }
        }
    }
}

@Composable
private fun MonthView(
    selectedDate: LocalDate,
    events: List<CalendarEventCache>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var month by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val eventsByDate = remember(events) { eventsByLocalDate(events) }
    val today = remember { LocalDate.now() }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = remember(month) {
        buildList {
            repeat(leadingBlanks) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
            while (size % 7 != 0) add(null)
        }
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(month.format(monthFormatter), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val hasEvents = eventsByDate[date]?.isNotEmpty() == true
                            val isToday = date == today
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isToday) {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelectDate(date) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium)
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(4.dp)
                                        .background(
                                            if (hasEvents) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEventCache, timeFormatter: DateTimeFormatter) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            val start = Instant.ofEpochMilli(event.start).atZone(ZoneId.systemDefault()).toLocalTime()
            val end = Instant.ofEpochMilli(event.end).atZone(ZoneId.systemDefault()).toLocalTime()
            Text(
                "${start.format(timeFormatter)} - ${end.format(timeFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startMillis: Long, endMillis: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var pickerMode by remember { mutableStateOf<PickerMode?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { pickerMode = PickerMode.DATE }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.format(dateFormatter))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickerMode = PickerMode.START_TIME }, modifier = Modifier.weight(1f)) {
                        Text("Start ${startTime.format(timeFormatter)}")
                    }
                    OutlinedButton(onClick = { pickerMode = PickerMode.END_TIME }, modifier = Modifier.weight(1f)) {
                        Text("End ${endTime.format(timeFormatter)}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val start = date.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
                    val end = date.atTime(endTime).atZone(zone).toInstant().toEpochMilli()
                    if (title.isNotBlank() && end > start) onConfirm(title.trim(), start, end)
                },
                enabled = title.isNotBlank() && endTime.isAfter(startTime)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    when (pickerMode) {
        PickerMode.DATE -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { pickerMode = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        pickerMode = null
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { pickerMode = null }) { Text("Cancel") } }
            ) { DatePicker(state = state) }
        }
        PickerMode.START_TIME -> TimePickerDialog(
            initial = startTime,
            onDismiss = { pickerMode = null },
            onConfirm = { startTime = it; pickerMode = null }
        )
        PickerMode.END_TIME -> TimePickerDialog(
            initial = endTime,
            onDismiss = { pickerMode = null },
            onConfirm = { endTime = it; pickerMode = null }
        )
        null -> Unit
    }
}

private enum class PickerMode { DATE, START_TIME, END_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimePicker(state = state)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
                }
            }
        }
    }
}

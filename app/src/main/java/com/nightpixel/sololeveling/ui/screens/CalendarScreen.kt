package com.nightpixel.sololeveling.ui.screens

import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nightpixel.sololeveling.SoloLevelingApplication
import com.nightpixel.sololeveling.data.calendar.GoogleAccountInfo
import com.nightpixel.sololeveling.data.entity.CalendarEventCache
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    var account by remember { mutableStateOf<GoogleAccountInfo?>(null) }
    var connecting by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingTokenAction by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val events by app.database.calendarDao().observeEvents().collectAsState(initial = emptyList())

    fun showError(message: String?) {
        scope.launch { snackbarHostState.showSnackbar(message ?: "Something went wrong") }
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
                .onSuccess { info ->
                    account = info
                    withCalendarAccess { token -> sync(token) }
                }
                .onFailure { showError(it.message) }
            connecting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    if (account != null) {
                        IconButton(onClick = { withCalendarAccess { token -> sync(token) } }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (account != null) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add event")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                account == null -> Column(
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
                syncing && events.isEmpty() -> CircularProgressIndicator()
                events.isEmpty() -> Text(
                    "No upcoming events",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> EventAgenda(events = events, modifier = Modifier.fillMaxSize())
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

@Composable
private fun EventAgenda(events: List<CalendarEventCache>, modifier: Modifier = Modifier) {
    val grouped = remember(events) {
        events.groupBy {
            Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSortedMap()
    }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (date, dayEvents) ->
            item(key = "header-$date") {
                Text(
                    date.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(dayEvents, key = { it.googleEventId }) { event ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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

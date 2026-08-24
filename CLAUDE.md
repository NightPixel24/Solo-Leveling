# Solo Leveling — build state

Full product spec: [docs/SPEC.md](docs/SPEC.md) — read it first for any feature work.
Build order is spec Section 10; work through phases in order, testing (`./gradlew assembleDebug`) between each.

## Current phase

**Phase 1 & 2 done**: project scaffold, nav shell, dark/blue/violet theme, minimal Room schema,
and a working JSON Export/Import (Settings screen, reached via the gear icon on Dashboard's top
bar). Export uses `ActivityResultContracts.CreateDocument`, import uses `OpenDocument` (SAF, so
the user can save to Drive or local storage per spec Section 3). `BackupData` in
`data/backup/BackupData.kt` is the one-field-per-table wrapper — add a field there and to
`BackupManager` every time a new module adds a Room table.
**Phase 3 done**: Tasks & Subtasks (`ui/screens/TasksScreen.kt`) — title/due date/priority/notes,
nested subtask checklist per task, all backed by `Task`/`Subtask` Room entities (schema v2,
`AppDatabase.MIGRATION_1_2`) and wired into the Phase 2 backup. Verified on-device including the
real v1→v2 migration path (app was already installed before the schema change landed).
Tasks screen (`TaskList` entity, schema v3, `AppDatabase.MIGRATION_2_3`): "+ Add list" sits in the
top bar next to the "Tasks" title; a horizontal row of bookmark-style tabs below it selects which
list is showing; one list's tasks fill the rest of the page at a time, backed by a `HorizontalPager`
kept in sync with the tab row both ways (tap a tab -> pager animates to it; swipe the page -> tab
selection follows). Long-press a tab for rename/delete. `Task.listId` has no DB-level foreign key
(would need a full table rebuild in SQLite just to add one) - list deletion cascades to its tasks
via `TaskListDao.deleteListCascading` instead, called from app code. A default "Tasks" list is
seeded both by the migration (existing installs) and `RoomDatabase.Callback.onCreate` (fresh
installs).
**Phase 4 done**: Habit Tracker (`ui/screens/HabitsScreen.kt`) — habits tagged Daily or Weekly,
each tagged with a `StatTag` (STR/VIT/DISCIPLINE/INT/AGILITY, spec Section 5.1's stat set —
gamification XP wiring itself is still Phase 10). One "did it today" checkbox per habit; daily
habits show a streak count, weekly habits show "X/Y this week" toward `targetPerWeek` plus a
streak once a week's target is met. Backed by `Habit`/`HabitLog` (schema v4,
`AppDatabase.MIGRATION_3_4`), `HabitLog` has a real FK+cascade to `Habit` (both tables are new,
unlike the `Task`→`TaskList` retrofit). Streak-freeze (spec Section 5.4) is NOT implemented yet —
it needs the Quest/weekly-review infrastructure from Phase 12, so today's streak calc is a plain
"consecutive days/weeks" count with no freeze exemption. `reminderTime` is captured (Material3
`TimePicker`) but not scheduled — actual notifications are Phase 16.
**Phase 5 done**: Gym Tracker (`ui/screens/GymScreen.kt`) — a weekly routine of exercises each
pinned to a day of week (Mon-Sun), grouped under day headers (only days with exercises shown).
Each exercise is Strength (sets/reps/weight target, feeds STR) or Cardio/Sport (duration target,
feeds AGILITY) via `ExerciseType`; checking a scheduled exercise opens a log dialog prefilled with
its targets, capturing actuals into a `GymSession` row for that date. Backed by `Exercise`/
`GymSession` (schema v5, `AppDatabase.MIGRATION_4_5`), `GymSession` has a real FK+cascade to
`Exercise` (both new tables). `Exercise.dayOfWeek` is stored as a plain Int (1=Mon..7=Sun,
`java.time.DayOfWeek.value`) rather than the enum type itself, avoiding a TypeConverter/serializer
for something that's just a day number. PR tracking / Boss Fights (spec Section 5.5) and the
STR/AGILITY/DISCIPLINE XP grants are still Phase 10/12 — this phase is purely the routine +
logging mechanism.
**Phase 6 done**: Google Calendar integration. `CalendarEventCache` (schema v6,
`AppDatabase.MIGRATION_5_6`) is a read-only local mirror of Google Calendar, wholesale-replaced on
every sync (`CalendarDao.replaceAll`) rather than diffed, since Google is always the source of
truth. Google Cloud Console OAuth is set up (Calendar API enabled; an Android-type OAuth client
registered with this app's package name + debug SHA-1; a Web-type client whose Client ID is in
`res/values/strings.xml` as `google_web_client_id` - public identifier, not a secret, safe to
commit). `data/calendar/GoogleAuthManager.kt` uses Credential Manager for identity (`signIn()`) and
the separate Play Services Identity Authorization Client for the actual Calendar OAuth scope +
access token (`requestCalendarAccess()`/`handleConsentResult()`) - deliberately separate per
Google's current guidance (identity vs. incremental API authorization); no token is persisted, the
app just re-requests (silently, once already granted) before each API call. `data/calendar/
CalendarApiClient.kt` hits the Calendar v3 REST endpoints directly with plain `HttpURLConnection`
(not the heavyweight `google-api-client` library). `ui/screens/CalendarScreen.kt`: connect button,
agenda-style event list grouped by day (full month/week/day grid per spec Section 4.1 is a later
polish pass), create-event dialog. `SoloLevelingApplication.calendarAccessGranted` caches the
"already connected" fact for the process lifetime, and `CalendarScreen` does a silent (no UI)
re-check with Play Services on cold start when that cache is empty - **fixed a real bug** where the
connected state was plain `remember {}` Compose state, so it reset (forcing reconnect) every time
the composable left composition, e.g. navigating to another tab and back, even though the
underlying Google grant was still valid the whole time.
Verified end-to-end on a real device (Pixel 7, real Google account over USB debugging) - the
`SoloLeveling_Pixel6` emulator can't test real sign-in (Play Services' add-account flow fails on
its `google_apis` image over this machine's network - see the `dev-machine-jdk-tls-workaround`
memory; tried fixing it by installing this machine's TLS-interception root CA into the emulator's
system trust store, but Android 14+'s real CA store lives in a read-only APEX module even with
root, so that path is dead-ended). A second AVD, `SoloLeveling_Pixel6_PlayStore` (`google_apis_
playstore` image), exists in case emulator-based auth testing is needed again, but hit the same
network-interception wall - a real device stays the reliable way to test anything touching Google
sign-in on this machine.
**Phase 7 done**: Mood Tracker. `ui/screens/LifeScreen.kt` now owns the "Life" bottom-nav slot with
a real `TabRow` (Mood/Food/Water, spec Section 8) - Food and Water are still `PlaceholderContent`
(a Scaffold-less variant of `PlaceholderScreen` added so placeholders can be embedded inside another
screen's tabs, not just used standalone) pending their own phases. Mood tab: a "Today" card (tap to
rate) plus a month heatmap with prev/next navigation; tapping any day (past or present) opens the
same rate/edit dialog, prefilled if that day already has an entry, with a Clear option. Backed by
`MoodEntry` (schema v7, `AppDatabase.MIGRATION_6_7`) - `date` (ISO string) is the primary key
directly since there's only ever one rating per day, no separate id/unique-index needed like
Habit/Gym logs. Year-at-a-glance view (spec also mentions this alongside month) is deferred as a
later polish pass, matching how Calendar's month/week/day grid was scoped down to an agenda list.
**Fixed a real bug found on-device**: the heatmap's last (partial) week row only padded leading
blank cells, not trailing ones, so a Row with fewer than 7 weighted children gave its lone cell
(e.g. day 31 in a 31-day month starting mid-week) the *entire* row's width via `weight(1f)` instead
of 1/7 of it, rendering as a giant oversized box. Fix: pad the cell list to a multiple of 7 on both
ends before chunking into week rows.
**Phase 8 done**: Food & Water Tracker. `ui/screens/LifeScreen.kt`'s Food and Water tabs are now
real (`FoodScreen()`/`WaterScreen()`, replacing the `PlaceholderContent` stand-ins from Phase 7).
Food: a camera FAB launches the system camera via `ActivityResultContracts.TakePicture()` against a
`FileProvider`-issued `content://` URI (`res/xml/file_paths.xml`, `AndroidManifest.xml` provider
entry) - a plain intent to the system camera app rather than an in-app CameraX preview, since spec
Section 2 allows either and this keeps the dependency footprint down; on capture, a confirm dialog
(Coil `AsyncImage` preview + description field) saves a `FoodLogEntry` row, and the list groups
entries by day (Today/Yesterday/date) newest-first. Water: a per-day `WaterLog` row (`date` as PK,
like `MoodEntry`) seeded on first view of a new day using the *previous* day's goal as the default
(`WaterDao.getLatestGoal()`) so the target doesn't silently reset; tapping a bottle icon in a
`FlowRow` sets the fill level directly (tap bottle N to fill up to N, or drain back to N-1 if
already filled) rather than a simple increment/decrement, and a settings icon opens a stepper
dialog to change the daily goal. Coil (`io.coil-kt:coil-compose:2.7.0`) was added as a dependency
for async thumbnail/preview loading rather than hand-rolling Bitmap decoding. Backed by
`FoodLogEntry`/`WaterLog` (schema v8, `AppDatabase.MIGRATION_7_8`), both new tables with no FK
needs (food entries are independent rows; water is one row per day). No bugs found this phase.
Verified on-device (`SoloLeveling_Pixel6` emulator): v7->v8 migration ran clean on launch; Water tab
- tapped bottle 3 of 8, progress bar and "3/8 bottles" text updated correctly; Food tab - captured a
real photo via the emulator's camera app, confirm dialog showed the photo and saved with a
description, entry appeared correctly under a "Today" header with thumbnail/description/timestamp;
final `adb logcat` sweep across the whole session showed no crashes.
Next up: **Phase 9** — Life Goals module (tiers, status tracking), per spec Section 10.

## Locked-in decisions

- Package/applicationId: `com.nightpixel.sololeveling`
- minSdk 26, targetSdk/compileSdk 35
- AGP 8.7.2, Kotlin 2.0.21, Gradle 8.9 (wrapper), Room 2.6.1 via KSP (not kapt)
- kotlinx.serialization (not manual `org.json`) for the backup JSON — scales better as more
  entities get added across later phases
- No Hilt/DI framework — manual instantiation via `SoloLevelingApplication` (single-user local app, kept deliberately simple)
- No light theme — the System-window dark aesthetic is fixed, not a user toggle
- Bottom nav screens are one file (`ui/screens/PlaceholderScreen.kt`) until each phase gives them real content
- Room schema JSON (`app/schemas/`) is committed — needed for migration tests per spec Section 3

## Build environment (this machine)

- Android SDK: `C:\Users\joshu\AndroidSdk` (cmdline-tools only, no Android Studio GUI installed)
- Portable JDK 17 at `C:\Users\joshu\AndroidSdk\jdk17\jdk-17.0.20+8`, used for all Gradle/Android tooling
- The system default JDK (Temurin 25) does NOT work for Gradle/sdkmanager on this machine — see
  the `dev-machine-jdk-tls-workaround` memory for why, before touching JDK/Gradle config here.
- `./gradlew.bat assembleDebug` works standalone (no manual env vars) once `~/.gradle/gradle.properties`
  has `org.gradle.java.home` pointing at the JDK 17 above.
- An AVD `SoloLeveling_Pixel6` (Pixel 6, API 35, x86_64, plain `google_apis` image) exists and is
  the standard way to test changes on this machine: `./gradlew installDebug` builds+installs to
  whichever device/emulator `adb` sees. Start it with `emulator -avd SoloLeveling_Pixel6` if it's
  not already running.
- A second AVD, `SoloLeveling_Pixel6_PlayStore` (same Pixel 6/API 35, but `google_apis_playstore`
  image), exists specifically for testing real Google sign-in - the plain `google_apis` image
  can't reliably add a real Google account (Play Services' own add-account flow fails on it).
  Neither emulator can actually complete real Google sign-in on this machine though (network-level
  TLS interception - see the `dev-machine-jdk-tls-workaround` memory); a real device over USB
  debugging is the reliable way to test anything touching Google auth here.
- `SoloLeveling_Pixel6` specifically has verity disabled and `/system` overlaid as writable (from
  the CA-install attempt during Phase 6 - didn't fix the auth issue, see memory above, but left
  the emulator in that state; harmless, just means that AVD's `/system` isn't read-only anymore).

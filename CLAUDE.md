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
unlike the `Task`→`TaskList` retrofit). Streak-freeze (spec Section 5.4) landed in Phase 12 once
the Quest infrastructure it was waiting on existed. `reminderTime` is captured (Material3
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
**Phase 9 done**: Life Goals module (`ui/screens/GoalsScreen.kt`). Goals are grouped under the
seven spec Section 4.7 tiers (1-Month through Lifetime), each with a title, description, optional
target date, status (Active/Completed/Failed), and optionally-linked existing Tasks as milestones -
a card shows "X/Y linked tasks done" with a progress bar computed from those tasks' `isDone` state.
Habit-linking (spec also mentions it) is deferred - Task already exposes a clean done/not-done
signal to build progress from, and adding Habit's streak-based semantics into the same UI would
roughly double this phase's scope for a feature that isn't blocking anything downstream yet.
Backed by `Goal` (schema v9, `AppDatabase.MIGRATION_8_9`), a new table with no FK needs; `tier` and
`status` are stored as enum columns via explicit `Converters.kt` entries (this codebase's established
pattern - Room's implicit enum-to-String magic is never relied on here). `linkedTaskIds` is a
comma-separated `Task.id` string rather than a join table or a TypeConverter-backed `List<Long>` -
same no-FK, no-extra-machinery reasoning as `Task.listId`. Reached from a flag icon on the
Dashboard's top bar (next to Settings) rather than a Rank badge - spec Section 4.7 says "tap the
Rank badge," but that badge doesn't exist until the gamification core (Phase 10) and Rank engine
(Phase 11) are built; the flag is a stand-in wired to the same `Routes.GOALS` destination so no
navigation code needs to change once the real badge lands.
**Fixed a real bug found on-device**: the edit dialog's three status chips (Active/Completed/Failed)
sat in a plain `Row`, and "Failed" wrapped its label onto two lines ("FAIL"/"ED") because the row ran
out of width - same class of overflow bug as Phase 4's stat-tag chips and Phase 7's heatmap row. Fix:
switched to `FlowRow` (matching the tier chips above it) and swapped the raw `GoalStatus.name` for a
title-cased label function shared with the status chip shown on each goal card.
Verified on-device (`SoloLeveling_Pixel6` emulator, upgrading an existing v8 install so the real
v8->v9 migration path ran): migration completed with no crash; added a goal ("Run 5K", 1-Month tier)
with an existing Task linked, saved, and confirmed the card showed "1/1 linked tasks done" with a
full progress bar (the linked task was already marked done); edited the same goal, moved it to the
Yearly tier and changed status to Completed, saved, and confirmed it re-grouped under a new "Yearly"
header with a green "Completed" label; deleted the goal via the edit dialog's Delete button and
confirmed the list returned to its empty state; a final `adb logcat` sweep across the whole session
showed no crashes.
**Phase 10 done**: Gamification core - Stat engine, XP/leveling, radar chart. `Stat` (one row per
`StatTag`, schema v10, `AppDatabase.MIGRATION_9_10`, seeded at level 1/0 XP for both fresh installs
and existing ones upgrading) and `XpLog` (an append-only audit trail, spec Section 5.1, not read
back by the app yet but kept so the "tunable" grant amounts below can be analyzed later) back
`data/gamification/XpEngine.kt` - a single `grant(tag, amount, source)` entry point that adds XP,
rolls levels up via spec Section 5.2's `xpForLevel(level) = round(50 * level^1.2)` while XP exceeds
the next threshold (capped at level 99), and writes the XpLog row. Exposed as
`SoloLevelingApplication.xpEngine`, a peer to `database`/`backupManager`. Grants are one-directional
- nothing in the spec calls for clawing XP back when a habit/task is unchecked, so every call site
only invokes `grant` on the false->true completion transition, never on undo.
Wired into every already-built screen the spec's Section 5.1 table names as a stat's source: Habit
completion (`HabitsScreen`) grants the spec's example +10 XP to the habit's tagged stat; Task
completion (`TasksScreen`) grants the spec's example +5 DISCIPLINE, and subtask completion grants a
smaller +2 DISCIPLINE (subtasks aren't in the spec's XP example list, so this amount is this app's
own tuned guess, same spirit as the given examples); Water goal hit (`WaterScreen`) grants the
spec's example +10 VIT, gated by a new `WaterLog.xpGranted` boolean so draining and refilling
bottles the same day after hitting the goal doesn't re-grant it; Food logged (`FoodScreen`) grants a
tuned +5 VIT (spec lists "food logged" as a VIT source but gives no example amount); Gym session
logged (`GymScreen`) grants +15 STR/AGILITY normally or +40 if it's a PR - "PR" here means the
logged weight (Strength) or duration (Cardio/Sport) beats every previous session logged for that
exact exercise, computed from the already-loaded `ExerciseWithSessions` list rather than needing
full Boss-Fight-style target/HP tracking (that's Phase 12 scope; this is just the "is this the best
I've logged" check needed to pick an XP amount now). Habit-streak bonuses and "showing up on a
scheduled gym day" (also DISCIPLINE sources per the spec table) are deferred - both need a bonus
amount and trigger condition the spec doesn't specify, same reasoning as other phases' deferrals.
`ui/components/RadarChart.kt` is a from-scratch Canvas composable (Compose has no built-in chart
API) - draws a generic N-axis pentagon (concentric percentage rings, axis lines, a filled/stroked
data polygon, and axis labels via `TextMeasurer`), called from the new `DashboardScreen.kt` body
with the 5 stats' `level / 99f` as the plotted fraction, plus a per-stat row below showing "Lv. X"
and a linear XP-within-level progress bar. This replaces the Dashboard's "coming soon" placeholder,
but only with the Section 5.1 pieces - Today's Quests, boss fights, the mood heatmap preview, and
the rest of spec Section 6's widgets stay deferred to Phase 15 ("Dashboard/Analytics screen tying
everything together"), which is also this phase's explicit slot in the build order, not this one's.
No bugs found this phase.
Verified on-device (`SoloLeveling_Pixel6` emulator, upgrading an existing v9 install so the real
v9->v10 migration ran): migration completed with no crash, Dashboard rendered the pentagon radar
chart with all 5 stats seeded at Lv. 1/0 XP; completing a DISCIPLINE-tagged habit moved that stat to
10/50 XP live on the Dashboard; logging a Gym session with no prior PR granted +15 STR (this device
already had gym history from earlier phase testing, so it correctly wasn't judged a PR), then
logging a heavier weight for the same exercise granted +40 and rolled STR from Lv. 1 to Lv. 2 landing
at exactly 5/115 XP - matching `xpForLevel(2) = round(50 * 2^1.2) = 115` by hand; completing a task
moved DISCIPLINE from 10 to 15 XP; a final `adb logcat` sweep across the whole session showed no
crashes.
**Phase 11 done**: Rank engine. `data/gamification/Rank.kt`'s `computeRank(goals: List<Goal>)` is a
pure function - E through SS (spec Section 5.3), computed live from `Goal` data every time it's
needed rather than persisted (there's no `Rank` table in spec Section 9's data model, so this
follows the same "derive, don't store" approach as habit streaks). Implements the spec's stated
default rule literally: rank is the highest rank among all *completed* goals' tiers, not a strict
step-by-step unlock - so completing a Yearly goal before ever completing a 1-Month/3-Month/6-Month
goal jumps straight to A. The spec also floats an "all goals at a tier" alternative as a future
Settings toggle; deferred, same as Phase 9 deferred Habit-linking, until there's real usage data to
justify it. `ui/components/RankBadge.kt` is a circular badge (spec Section 5.3: "sits next to the
Stat radar chart... but the two are visually distinct") showing the rank letters, dropped into
`DashboardScreen.kt` next to the radar chart with a "Rank" label; tapping it opens Life Goals,
matching spec Section 8 exactly, and replaces the flag icon that stood in for it since Phase 9.
**Fixed a real, pre-existing navigation bug found on-device**: tapping any bottom-nav item while on
a screen reached *outside* the bottom-nav graph (Life Goals, Settings - both pushed via a plain
`navController.navigate(route)` with no back-stack options) silently did nothing - confirmed via
temporary logging that `navController.navigate(...)`'s `popUpTo(startDestination){saveState=true} +
launchSingleTop + restoreState` combo (the standard bottom-nav pattern, copied from the official
Compose Navigation sample) is a documented no-op when the target is the graph's start destination
and it's already sitting, un-popped, beneath the current entry in the back stack - exactly the
shape of stack that Goals/Settings leave behind. This is what made the Rank badge navigation loop
back to itself instead of returning Home. Fixed in `BottomNavBar.kt` by trying
`navController.popBackStack(destination.route, inclusive = false)` first (always correctly jumps
back to an existing entry, including the start destination) and only falling through to the
original navigate+restoreState logic when the target isn't already in the back stack (i.e.
switching to a genuinely different, not-yet-visited bottom-nav tab). Verified this also fixes the
identical latent bug for the Settings screen, and that normal tab-to-tab switching (with scroll/tab
state restoration) still works afterward.
Verified on-device (`SoloLeveling_Pixel6` emulator; no schema change this phase, so a plain
`installDebug` update, not a migration test): cold-started with no completed goals and confirmed the
badge showed "E"; completed a 3-Month goal via Life Goals and confirmed a fresh cold start
recomputed the badge to "C" (`GoalTier.THREE_MONTH -> RankTier.C`); reproduced the navigation bug
via temporary log lines showing the back stack was byte-for-byte unchanged after a failed
Home-tap-from-Goals, then confirmed the `popBackStack`-first fix resolves it for both Goals and
Settings while leaving ordinary tab-switching intact; a final `adb logcat` sweep showed no crashes.
**Phase 12 done**: Quests + Boss Fights (PR Boss only - see below). `data/gamification/Quests.kt`
computes Today's/Weekly Quests (spec Section 5.4) live from each source's own data - a habit,
task, gym session, water log, and mood entry already record their own done state, so a separate
persisted `Quest` row would just be a second, potentially-stale copy of the same fact; this is the
same "derive, don't store" reasoning Rank (Phase 11) already established. Today's Quests: one entry
per DAILY habit (WEEKLY habits are excluded - they have no specific "due today"), one per exercise
scheduled today, the water goal, tonight's mood check-in, and tasks due today. Weekly Quests: the
spec's three example targets ("complete all scheduled gym days," "hit water goal 6/7 days," "zero
missed daily habits"), evaluated Monday..today with today itself never counted as a miss since the
day isn't over yet; all three passing marks a "good week" (spec Section 5.7 will use that for
monthly rewards once Phase 14 exists). Both show as new sections on `DashboardScreen.kt`. Side
Quests ("your Task list, reframed... a small bonus on top of normal task XP") is just an extra tuned
+3 DISCIPLINE grant alongside the existing task-completion XP, since a task already *is* a side
quest per the spec's own framing - no separate entity needed.
Streak-freeze (deferred since Phase 4) is now live: `dailyStreak()` in `HabitsScreen.kt` forgives
one missed day per rolling 7-day window without needing a separate freeze-usage table - a gap is
simply skipped (doesn't add to the streak count, doesn't break the chain before it) as long as the
last forgiven gap was 7+ days back.
Boss Fights (spec Section 5.5): PR Boss only. `Boss` (schema v11, `AppDatabase.MIGRATION_10_11`,
FK+cascade to `Exercise`) stores `defeated`/`defeatedAt` but *not* HP - HP is computed live as
`targetWeight - best logged weight for that exercise`, so nothing needs to stay in sync when a
session is logged or edited; `defeated` IS stored though, since the spec calls the win a "permanent
trophy" and a purely-computed flag could un-defeat itself if a later session were edited or deleted.
Managed from a new "Boss Fights" section at the top of `GymScreen.kt` (pick a Strength exercise,
name, target weight); defeating one (logging a session whose weight meets the target) grants a
tuned +50 STR bonus reward and shows a permanent "Defeated!" card. Streak Boss (habit/Cardio-Sport
streak HP with a Hard/Easy mode toggle) is deferred - the spec leaves too many undefined parameters
(period length, what counts as a "successful period" for an arbitrary habit vs. exercise, per-boss
mode choice) to implement well without more direction, same reasoning as other phases' deferrals.
**Fixed two real bugs found on-device**: (1) creating a boss against an exercise that already had a
logged PR meeting or beating the target (e.g. setting a lower target than your current best) left
it stuck at 0 HP forever, never flipping to defeated, because the defeat check only ran on the
*next* session log - fixed by also checking at boss-creation time. (2) `BossRow` only rendered a
delete button for non-defeated bosses, so a defeated boss's card - including ones created purely
for on-device testing - could never be removed; fixed by always showing delete, since the "permanent
trophy" is the XP reward already granted (which deleting the card doesn't undo), not the card itself.
Verified on-device (`SoloLeveling_Pixel6` emulator, upgrading an existing v10 install so the real
v10->v11 migration ran): migration succeeded with no crash; Dashboard's Today's/Weekly Quests
correctly reflected existing habit/task/mood state live; created a boss against an exercise with an
existing 100kg PR and a 65kg target and confirmed it showed "Defeated!" immediately with +50 STR
granted; deleted it via the newly-added always-visible delete button and confirmed the boss list
and Dashboard's Active Boss Fights section both updated correctly; a final `adb logcat` sweep across
the whole session showed no crashes.
**Phase 13 done**: Punishment Pool (`ui/screens/PunishmentScreen.kt`, reached from a new gavel icon
on the Dashboard's top bar - no assigned bottom-nav slot, same as Goals/Settings). You define
`PunishmentPoolItem`s (description + Minor/Major severity, spec Section 5.6). Auto-assignment is a
live scan (`data/gamification/Punishments.kt`'s `detectMissedItems`) that runs once when the
Punishment Pool screen opens rather than via a background job (that's Phase 16): it checks only the
most recently completed day (yesterday, for missed daily habits/scheduled gym days -> Minor) and
week (last Mon-Sun, for missed weekly-habit targets or an incomplete "all scheduled gym days" week
-> Major), the same bounded "don't retroactively backfill" approach Quests (Phase 12) uses.
`PunishmentAssignment` carries a `sourceRef` (not in the spec's own field list, e.g.
"habit-daily:3:2026-08-24") with a unique DB index, so `PunishmentDao.insertAssignment` uses
`OnConflictStrategy.IGNORE` - re-scanning the same miss on every screen visit is always safe and
never creates a duplicate debt. Schema v11->v12 (`AppDatabase.MIGRATION_11_12`) adds
`punishment_pool_items` and `punishment_assignments` (FK+cascade to the pool item), both wired into
the JSON backup. Resolving a debt ("Clear") just sets `resolved=true`/`resolvedAt`; nothing reverses
the miss itself, matching the one-directional pattern XP grants already established. No bugs found
this phase.
Verified on-device (`SoloLeveling_Pixel6` emulator, upgrading an existing v11 install so the real
v11->v12 migration ran): migration succeeded with no crash; added a Minor ("50 pushups") and a
Major ("Cold shower") pool item; opening the screen against this device's real habit/gym history
correctly auto-assigned one Minor debt (a missed daily habit yesterday) and two separate Major
debts (a missed weekly-habit target and an incomplete gym week, both independently drawing the
pool's only Major item) with the correct assigned dates; resolved the Minor debt via "Clear" and
confirmed it dropped out of Active Debts; reopened the screen again and confirmed the scan is
idempotent - no duplicate debts were created for the same already-recorded misses; a final `adb
logcat` sweep showed no crashes.
Next up: **Phase 14** — Reward Economy (Gold ledger, weekly/monthly redemption), per spec
Section 10.

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

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
**Phase 14 done**: Reward Economy (`ui/screens/RewardsScreen.kt`, now the real content behind the
"Rewards" bottom-nav tab - a `RewardsScreen()` placeholder had sat there since the tab itself was
added). Gold mirrors XP's established shape: `GoldBalance` (single denormalized row, id fixed at 0,
same pattern as `AppMeta`) plus `GoldTransaction` (an append-only signed ledger, `GoldBalance`/
`GoldTransaction` ~ `Stat`/`XpLog`), both driven by a new `GoldEngine.grantFromXp(xpAmount, source)`
- spec Section 5.7's literal "1 Gold per 10 XP" - called only from the two sources the spec names,
"habits and gym completions": `HabitsScreen`'s daily/weekly toggle and `GymScreen`'s session-log
call site. The Boss-defeat bonus (also in `GymScreen`) does *not* also mint Gold - it's a bonus
reward layered on an already-Gold-granting completion, not itself "a gym completion," so making it
double-dip felt like scope creep past what the spec states. Schema v12->v13
(`AppDatabase.MIGRATION_12_13`) adds `gold_balance`, `gold_transactions`, `reward_pool_items`, and
`reward_targets` (new `RewardDao`, wired into the Phase 2 backup same as every prior phase).
`RewardPoolItem` (title/cost/pool) is the user-defined Weekly/Monthly reward pool; `RewardTarget`
is "the one reward picked as this period's target," keyed uniquely by `(pool, periodStart)` so
picking again for a period replaces rather than duplicates - its `claimed` flag doubles as both
current state and, once claimed, a history row, the same dual role `PunishmentAssignment.resolved`
already plays for Debts. Progress toward a target is Gold *earned* (not net of spends) since the
period's start (`data/gamification/Rewards.kt`'s `goldEarnedSince`, summing positive
`GoldTransaction` rows by date) against the target's cost; hitting it unlocks "Claim," which spends
the cost via `GoldEngine.spend` (a negative transaction) and marks the target claimed. The Monthly
pool stays locked until "3 of the last 4 weeks were good weeks" (spec Section 5.7) -
`countGoodWeeksInLastN` reuses Phase 12's exact `computeWeeklyQuests` "good week" definition over
the 4 completed weeks before the current one, rather than inventing a second one. Dashboard's
Status Window (spec Section 6: "Rank badge, radar chart, Gold balance") now shows the live Gold
total next to the Rank badge.
Verified on-device (`SoloLeveling_Pixel6` emulator, upgrading an existing v12 install so the real
v12->v13 migration ran): migration succeeded with no crash, Dashboard showed "0" Gold next to the
Rank badge; opened Rewards (empty pools, Monthly correctly locked at "0/4 good weeks"); added a
Weekly reward ("Movie", 1 Gold cost) and picked it as the Weekly target, showing "0/1 Gold earned
this period"; completed a daily habit (+10 DISCIPLINE XP, +1 Gold per the 1-per-10 rate) and
confirmed both the Dashboard's Gold total and the target's progress bar updated live to 1; tapped
Claim and confirmed the balance dropped back to 0, the target card flipped to a green "Claimed!"
label, and a "History" section appeared showing "Movie - Weekly - claimed 2026-08-25"; a final `adb
logcat` sweep across the whole session showed no crashes.
**Phase 15 done**: Dashboard/Analytics screen tying everything together. `DashboardScreen.kt` gained
a `Home`/`Analytics` `TabRow` (matching `LifeScreen`'s established tabbed-screen shape) - `Home` is
the prior Status Window content plus the four pieces spec Section 6 still listed: a Quick Add row,
a Mood-this-month preview, and a Life Goals summary, with `Analytics` all-new. Everything is
computed live from data other phases already persist (XpLog/HabitLog/GymSession/MoodEntry/Goal) -
`data/gamification/Analytics.kt`'s five pure functions (`statXpTrends`, `habitCompletionRates`,
`gymVolumeByWeek`, `moodDistributionForMonth`, `goodWeekHistory`) - the same "derive, don't store a
second copy" approach Quests/Rank/Rewards already established, so this phase added no new Room
tables or migration. `statXpTrends` plots cumulative XP *gained within* the 30-day window per stat
rather than reconstructing each stat's all-time historical total backward from its current level -
same shape of increasing line, without needing extra machinery to walk XpLog backward from `Stat`.
`ui/components/LineChart.kt` is a second from-scratch Canvas composable (`RadarChart`, Phase 10, was
the first) for the multi-series stat-trend chart; the rest of Analytics (habit completion %, gym
weekly volume, mood distribution, good-week history) uses plain `LinearProgressIndicator`/`Row`-of-
`Box` bars, keeping the custom-Canvas budget to the one visualization that actually needed it.
Quick Add reuses existing screens' own logic rather than re-implementing it a second time: Habit
un-privates `HabitsScreen.kt`'s `toggleToday`; Water adds a small shared `logWaterBottle` helper
next to `WaterScreen.kt`'s own bottle-tap logic (same goal-default/one-time-`xpGranted` bookkeeping,
XP only - no Gold, matching Phase 14's "habits and gym completions" scope); Food un-privates
`FoodScreen.kt`'s `ConfirmFoodDialog`/`createPhotoFile`/`photoFileUri` so the Dashboard's camera
button drives the exact same capture-confirm-save flow instead of a second camera integration.
The Mood-this-month preview needed the same heatmap grid `LifeScreen`'s Mood tab already draws, so
`MonthHeatmap` (plus `moodColorValue`/`moodTextColor`/`moodLabel`) moved out to a new shared
`ui/components/MoodHeatmap.kt` - `LifeScreen.kt` now imports it instead of keeping its own copy.
Tapping the preview jumps to the Life tab's Mood sub-tab; doing that safely (the Dashboard is the
bottom-nav graph's start destination) needed the exact `popBackStack`-first fix Phase 11 added for
the bottom nav bar itself, so that logic became a `NavHostController.navigateToBottomNav` extension
in `BottomNavBar.kt`, used by both the bar and the Dashboard's new `onMoodClick` callback, rather
than a second copy of the fix. Life Goals summary shows, per `GoalTier` that has one, its
earliest-created `ACTIVE` goal (title + linked-task progress) - reasonable stand-in for "current"
per tier since goal data has no other ordering/priority field to prefer one over another.
**User-requested change**: the mood heatmap's "Bad" color was a plain dark gray
(`Color(0xFF2A2A2A)`) since Phase 7; changed to `SystemRed` in `MoodHeatmap.kt` (and the Analytics
tab's mood-distribution bar to match) so all three ratings read as distinct colors at a glance.
Verified on-device (`SoloLeveling_Pixel6` emulator, plain `installDebug` - no schema change this
phase): Dashboard's Home tab rendered Quick Add, Today's/Weekly Quests, the mood preview (an
already-rated day showing green, matching `LifeScreen`'s own heatmap pixel-for-pixel since both now
share `MonthHeatmap`), and Boss Fights/Life Goals sections correctly; tapping the mood preview
navigated to Life's Mood tab without the Phase-11-class back-stack bug; the Analytics tab rendered
a 5-series stat-trend line chart (STR/DISCIPLINE visibly trending up from real habit/gym history on
this device), habit completion-% bars, an 8-week gym volume bar chart with a real "Bench Press:
100kg" PR line, a mood distribution, and a "0/8 good weeks" row; opened the mood rating dialog and
confirmed the "Bad" swatch and legend dot render red instead of the old gray; a final `adb logcat`
sweep across the whole session showed no crashes.
**Phase 16 done**: Notifications. New `notifications/` package, built on WorkManager
(`androidx.work:work-runtime-ktx`, this phase's only new dependency) rather than AlarmManager's
exact alarms - spec Section 7 itself offers either, and nothing here needs to-the-second
precision. `NotificationChannels` creates one channel per category (Habits/Water/Mood/Gym/Review)
so the user can mute/tune each independently from system settings; `Notifier.show` is the one
place every worker calls through, checking the API 33+ `POST_NOTIFICATIONS` runtime permission
(requested from `MainActivity.onCreate` - unconditionally, since scheduling doesn't need it, only
actually displaying does) before building a notification whose tap target always just reopens
`MainActivity` (deep-linking to the specific screen would need nav-arg plumbing this phase doesn't
otherwise need, left for later polish). `ReminderScheduler.scheduleAll`, called once from
`SoloLevelingApplication.onCreate`, wires up five workers using two different scheduling shapes
depending on what spec Section 7 actually asks for each one:
- **Exact-time reminders** (Mood check-in 8pm, Gym-day 8am, weekly/monthly Review 8:30pm) use a
  self-rescheduling `OneTimeWorkRequest` chain (`scheduleDailyAt`/`millisUntilNext` in
  `ReminderScheduling.kt`) - each worker checks its condition, shows a notification if warranted,
  then re-enqueues itself for tomorrow's occurrence. `ReviewReminderWorker` covers both weekly
  ("is today Sunday?") and monthly ("is today the last day of the month?") from the one daily
  check, since `PeriodicWorkRequest` has no notion of "monthly" (months vary in length).
- **Approximate/spaced reminders** (Habit due-today sweep every 15 min - WorkManager's periodic
  minimum; Water reminder every 2 hours, active only 9am-9pm) use plain `PeriodicWorkRequest`s.
  `HabitReminderWorker` deliberately sweeps *all* habits each tick rather than scheduling one
  exact-time job per habit - that would need cancel/reschedule wiring on every habit add/edit/
  delete, whereas a periodic sweep just picks up changes for free on its next tick, at the cost of
  a reminder landing up to ~15 minutes late. Weekly habits are excluded from the sweep, same
  "no specific due-today" reasoning `computeDailyQuests` (Phase 12) already established.
`res/drawable/ic_notification.xml` is a new solid-silhouette status-bar icon reusing the launcher's
diamond motif (the launcher's own foreground vector uses a stroke + gradient fill unsuitable for a
notification icon, which the system tints to a flat silhouette).
Verified on-device (`SoloLeveling_Pixel6` emulator, plain `installDebug` - no schema change this
phase): launched the app and confirmed the `POST_NOTIFICATIONS` system permission dialog appears
(granted it); `adb shell dumpsys jobscheduler` showed WorkManager had scheduled real jobs with
computed delays matching every target time (~+8h45m for Gym's 8am, ~+20h45m for Mood's 8pm,
~+21h15m for Review's 8:30pm, ~+2h for Water's periodic interval, all relative to the ~11:13pm
test time); `adb shell cmd jobscheduler run -f` force-triggered the scheduled jobs and `adb logcat`
confirmed `HabitReminderWorker`/`WaterReminderWorker` completed with `Result.success()` against
real Room data with no crash; forcing `GymReminderWorker` produced a real system notification
("Gym day - 1 exercise scheduled today") visible in the notification shade with the correct app
name, custom diamond icon, and channel - confirming the full pipeline (permission check, channel,
`Notifier`, real DB-backed condition) works end-to-end, not just "compiles." The other workers'
conditions (mood already logged today, not Sunday/month-end, outside water's 9am-9pm window)
correctly evaluated false and suppressed their notifications when forced, which is correct
behavior, not a gap. A final `adb logcat` sweep across the whole session showed no crashes or ANRs.
**Phase 17 done**: Polish — the closing phase per spec Section 10. Three pieces, all scoped to
finishing what earlier phases already built rather than adding new modules:
- **Export/Import finalized and tested**: `BackupData`/`BackupManager` already covered all 22
  entities in `AppDatabase` (every phase since Phase 2 extended it as its own table landed), so
  there was no gap to close in code - this phase's job was proving it. Verified end-to-end on-device
  rather than by inspection: added a real task via the UI, exported via Settings, `pm clear`'d the
  app (simulating a full uninstall/data-loss, a stronger test than anything prior phases had done -
  they'd only spot-checked individual fields as each was added), reinstalled-equivalent by
  relaunching, imported the same file back, and confirmed via both direct sqlite3 queries and the
  live UI that the task, all 5 seeded stats, the default task list, and the gold balance all came
  back correctly.
- **Room migration test pass**: added `androidx.room:room-testing` and
  `app/src/androidTest/java/.../data/MigrationTest.kt` - 13 instrumented tests using
  `MigrationTestHelper` against the already-committed schema JSONs in `app/schemas/` (committed
  since Phase 1 specifically for this, per a locked-in decision that had been sitting unused until
  now): one test per adjacent version pair (1->2 through 12->13) so a broken migration's failure
  points at the exact version pair responsible, plus a full 1->13 chain test that also opens the
  fully-migrated database through Room's real generated DAOs (not just raw-SQL schema validation)
  and confirms the two migration-inserted seed rows (the default task list from `MIGRATION_2_3`,
  all 5 stats from `MIGRATION_9_10`) survived the whole chain. Needed `sourceSets { getByName
  ("androidTest").assets.srcDir("$projectDir/schemas") }` in `build.gradle.kts` so
  `MigrationTestHelper` can find the same JSON files `room.schemaLocation` already writes, without
  duplicating them. All 13 pass on-device.
- **Settings screen**: already met the spec's literal requirement (reachable from Dashboard,
  contains Export/Import) since Phase 2; this phase rounds it out with two sections that had no
  home yet - a "Notifications" section linking out to the system per-app notification settings
  (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`), useful now that Phase 16 actually has five
  reminder channels a user might want to individually mute/tune, and an "About" section showing
  the live app version via `PackageManager.getPackageInfo` rather than wiring up a `BuildConfig`
  field just for this one string. The spec's own floated "rank-unlock-rule toggle" (Section 5.3)
  stays deferred, unchanged from Phase 11's reasoning - it's explicitly framed there as "worth
  deciding once you're actually using it," not a concrete requirement blocking anything.
No bugs found this phase.
Verified on-device (`SoloLeveling_Pixel6` emulator; plain `installDebug` - no schema change this
phase): the full export/import round-trip described above; Settings screen rendered its three
sections correctly (`uiautomator dump` confirmed exact text/bounds), the Notification Settings
button opened the real system per-app notification screen, and "Solo Leveling v0.1.0" rendered from
the live `PackageManager` call; `connectedDebugAndroidTest` ran all 13 `MigrationTest` cases against
the real emulator with a clean `BUILD SUCCESSFUL`; a final `adb logcat` sweep across the whole
session showed no app crashes or ANRs (one unrelated `FrameTracker` IME-animation timeout, a known
harmless system log line, not an app error).

This closes spec Section 10's build order - all 17 phases are now done.

**Post-launch feedback pass (2026-08-26, v1.0.0 -> v1.1.0)**: first round of real-device testing
feedback, fixed same-day. Bugs found:
- **Real bug (not just missing feedback)**: `WaterScreen`'s day-row seeding `LaunchedEffect` was
  keyed on `log` (from `collectAsState(initial = null)`) and fired on that synthetic `null` before
  the Flow's real first emission could arrive, unconditionally overwriting *any* already-logged
  water for today back to 0 - a genuine race, not just a UX gap. Fixed by keying the effect on
  `Unit` and checking a new one-shot `WaterDao.getLogOnce(date)` against the real DB state instead
  of trusting the reactive stream's placeholder value.
- `SettingsScreen`'s Column had no `verticalScroll` - harmless when it only held Backup, but once
  Notifications/Danger Zone/About (this pass + Phase 17) pushed it past one screen, the bottom
  content (including the new Clear Data button) was silently unreachable. Added the scroll.
Everything else was polish/new capability, not bugs:
- Water: renamed "bottles (1L)" -> "cups (250ml)" throughout (same default of 8, now a sane 2L/day
  instead of an 8L/day target); Dashboard's quick-add now shows a Snackbar ("Logged 1 cup (x/y)" or
  "goal already reached") since it has no visible water counter of its own to react to.
- Mood heatmap: today's cell gets a colored ring border regardless of whether it's rated yet.
- Tasks: added a pencil-icon edit action (title/priority/due date/notes) via a `TaskEditorDialog`
  generalized from the old add-only dialog rather than a second copy; the always-visible "add
  subtask" text field collapsed to a subtle "+ Add subtask" text row that expands to the real input
  on tap, since a permanent input control in every expanded card read as too loud next to the task
  list above it.
- Calendar: replaced the flat agenda list with an actual visual calendar (spec Section 4.1) - a
  Week tab (7-day strip, tap a day to see its events) and a Month tab (grid with event-dot markers,
  tapping a day jumps to Week); also fixed a real flash-of-wrong-state bug where the "Connect
  Google Calendar" button showed for a moment on every cold start even for an already-connected
  account, because the access cache only proves *granted*, never *not yet checked* - added a
  `checkingAccess` state so a spinner shows instead until the silent re-check resolves either way.
- Dashboard: the Gold total moved from a full-size row next to the Rank badge into a small
  HUD-style badge in the top bar (spec Section 2 calls for a "System window" aesthetic; a
  dollar-sign row was reported as reading "cheap" and crowding the Rank badge); flattened all of
  Dashboard's `Card` elevations to 0dp (shadows were rendering on a near-black background for no
  visual benefit, and are one of the more common Compose scroll-jank sources when a screen has this
  many cards) and wrapped the radar chart's per-recomposition list build in `remember` - a
  best-effort perf pass for a reported "choppy scroll," not a verified profiling fix, since this
  session had no way to profile the actual device.
- Settings: added a "Clear All App Data" Danger Zone action for repeatable fresh-start testing -
  `BackupManager.wipeAll(context)` reuses `restore()`'s existing "missing fields get seeded"
  behavior by handing it an all-empty `BackupData` rather than a second seed path, and also deletes
  orphaned food photo files from internal storage.
Deferred as design decisions rather than guessed at: reshuffling the 7-item bottom nav down to a
Home-centered 5 (which 2 items move, and to where, is a real UX call); the requested Name + rank
Title + per-stat sub-title + equip system to replace the Rank badge (title copy/unlock rules would
otherwise be invented wholesale). Both are open questions for the user, not yet built.
Not independently re-tested on-device: `DISCIPLINE`'s XP sourcing already matches what was asked
("goes up as I do my habits and tasks and I'm consistent") - task completion always grants
DISCIPLINE (+5, plus +3 Side Quest bonus), and any habit tagged DISCIPLINE grants it on completion
- explained to the user as existing behavior rather than changed.
Verified on-device (`SoloLeveling_Pixel6` emulator, plain `installDebug` - no schema change): the
water race condition fixed and reproduced-then-confirmed-fixed via the exact repro (Dashboard
quick-add -> Life's Water tab, before showed 0/8, after correctly showed 1/8); Settings scroll
fix confirmed by reaching and using the new Clear Data button, which correctly zeroed every table
and deleted the (empty, in this test) food_photos directory with no crash; task edit dialog
confirmed end-to-end via direct sqlite3 verification (priority changed LOW -> HIGH); subtle
add-subtask row, mood today-highlight, and the relocated Gold badge all confirmed visually. Calendar
week/month views compile and render (Connect-account screen, no crash) but couldn't be verified
against real events - this dev machine cannot complete real Google sign-in on the emulator (see the
`dev-machine-jdk-tls-workaround` memory); a real device is needed to verify those views against
actual calendar data. A final `adb logcat` sweep showed no app crashes (one benign APK-swap
ResourcesManager log from the emulator's Wellbeing service mid-reinstall, and the same harmless
`FrameTracker` IME timeout seen in earlier phases).

**Second feedback pass (2026-08-26, v1.1.0 -> v1.2.0)**: same day, a follow-up round of feedback
plus a live design conversation about the stat set. Schema bump to v14 (`MIGRATION_13_14`) bundles
three unrelated-but-simultaneous changes rather than three separate version bumps for one evening:
- **AGILITY replaced with SPIRITUALITY**: the user felt AGILITY overlapped with STR ("if I go to
  the gym I always do cardio anyway") and wanted a stat fed by prayer/scripture/meditation habits
  instead. Since `StatTag` is stored as its raw `.name` string (`Converters.kt`), the migration
  does real `UPDATE ... SET tag = 'SPIRITUALITY' WHERE tag = 'AGILITY'` statements against
  `stats`/`habits`/`xp_logs`, not just a Kotlin rename - verified against real seeded data in
  `MigrationTest.migrate13To14`, not just schema-shape validation. Both `ExerciseType.STRENGTH`
  and `CARDIO_SPORT` gym sessions now grant STR (previously Cardio/Sport fed AGILITY) - `GymScreen`
  and `Exercise.kt`'s doc comment updated; `GymScreen`'s `TypeChip` now labels the exercise type
  itself ("STRENGTH"/"CARDIO") instead of a stat abbreviation, since both types feed the same stat
  now and the abbreviation stopped being a useful distinguisher.
- **Name + Title system**: replaces the Dashboard's old static "Rank" label. New `PlayerProfile`
  table (single row, id=0, same pattern as `AppMeta`/`GoldBalance`) holds an editable display name
  and an `equippedTitleId`. Titles themselves are *not* a table - `data/gamification/Titles.kt`
  computes every currently-unlocked title live from Rank + Stat data (one title per Rank tier E
  through SS, e.g. "S-Rank Hunter"/"National Level Hunter"; five level-gated tiers per stat, e.g.
  STR's "Novice Brawler" -> "Monarch of Strength" at levels 10/25/50/75/99) - the same
  "derive, don't store" approach Rank/Quests/Rewards already established, so only the *name* and
  the *equip choice* need persisting. Copy is this app's own invention (the spec names no titles at
  all), themed to match the "Hunter"/"Monarch" Solo Leveling flavor already used elsewhere. Tapping
  the name opens a rename dialog; tapping the title opens a picker listing every unlocked title
  with a checkmark on the equipped one.
- **Food ratings + optional photo**: `FoodLogEntry` gained a `rating: FoodRating`
  (HEALTHY/OK/UNHEALTHY, same three-tier shape and color language as `MoodColor`) and `photoUri`
  became nullable - SQLite can't ALTER a column's NOT NULL constraint in place, so the migration
  rebuilds the table (create-copy-drop-rename). `FoodScreen` gained a pencil icon in the top bar
  for "log without a photo" (reuses the same `ConfirmFoodDialog`, now `photoUri: Uri?`, rather than
  a second dialog), and a Dashboard Analytics "Food Health This Month" section
  (`foodHealthDistributionForMonth`, mirroring `moodDistributionForMonth`'s exact shape).
- **Low-vitality XP debuff**: not in the spec - a user-requested mechanic ("if I eat too much
  unhealthy food in a row then vitality xp is in a lowered gain mode until I eat healthy again").
  Simplified to a fixed rolling window rather than a stateful streak-since-last-healthy (consistent
  with this app's "derive, don't store" pattern): `data/gamification/Vitality.kt`'s
  `isLowVitalityMode` checks whether the most recent 3 logged meals are *all* UNHEALTHY: if so,
  every VIT grant (water goal, food logged, VIT-tagged habits - all three call sites now route
  through `applyVitalityMultiplier`) is halved until a non-UNHEALTHY entry changes that window.
  This is a deliberate simplification of "until I eat healthy again" (any OK or HEALTHY entry lifts
  it, not strictly HEALTHY) - flagged to the user as the chosen interpretation, not silently
  assumed.
- **Bottom nav reshuffle**: 7 items down to 5 with Home visually centered
  (`BottomNavDestination` reordered to Tasks/Habits/Dashboard/Gym/Life). Calendar and Rewards moved
  to Dashboard top-bar icons (`Routes.CALENDAR`/`Routes.REWARDS`), the same "reached from
  Dashboard, not the bar" treatment Settings/Goals/Punishments already had.
- **"Feeds X" labels**: small subtitle text added under each screen's own title/header (Gym "Feeds
  STR", Tasks "Feeds DISCIPLINE", Water "Feeds VIT", Food "Feeds VIT") - Habits already showed a
  per-habit stat chip, which is what prompted the ask ("similar to the habits section... maybe you
  should say this is VIT"). Mood intentionally has no such label since mood was never wired to any
  stat, in spec or in this codebase.
No bugs found this pass (the two real bugs - the water race condition and Settings' missing scroll
- were found and fixed in the *first* feedback pass earlier the same day, documented above).
Verified on-device (`SoloLeveling_Pixel6` emulator, real v13->v14 migration since this device
already had backup-test data from Phase 17): all 14 `connectedDebugAndroidTest` migration cases
passed, including the new `migrate13To14` (seeds a real 'AGILITY' stats row before migrating,
queries the migrated DB directly and confirms it reads back as 'SPIRITUALITY') and the extended
full-chain test (now asserting a `SPIRITUALITY` stat exists and the default `PlayerProfile` row
seeded correctly); radar chart, habit stat-tag chips, and `statColor` all confirmed showing
SPIRITUALITY correctly; created a SPIRITUALITY-tagged habit and confirmed completing it granted
+10 XP to the right stat; renamed the player profile (persisted correctly via sqlite3 check) and
confirmed the title picker opens showing "E-Rank Hunter" as the sole unlocked title at rank E;
logged 3 unhealthy meals via the new no-photo manual-entry dialog and confirmed the 4th VIT grant
(a water-goal hit) came in at half XP (18 total across 4 grants: 5+5+3+5, the 3rd meal's own grant
already reduced once the 3-unhealthy window formed), then logged a Healthy meal and confirmed the
very next VIT grant returned to full (+5, for 26 total) - the debuff both triggers and lifts
correctly; Dashboard's Analytics tab correctly showed "Healthy: 1, Unhealthy: 4" after the test
sequence; bottom nav confirmed showing exactly 5 items with Home centered, and Calendar/Rewards
confirmed reachable via their new Dashboard top-bar icons with no visual clipping despite 5 total
icons plus the Gold badge. A final `adb logcat` sweep showed no app crashes.

**Follow-up same day**: replaced the plain "Feeds STR/VIT/DISCIPLINE" text labels (Gym/Water/Food/
Tasks) with the same colored `StatChip` pill `HabitsScreen` already used per-habit, since the user
pointed out the two should look the same rather than one being plain text. Extracted `StatChip`
(and the `statTagColor` mapping it and `DashboardScreen`'s stat-trend chart/legend both relied on,
previously duplicated in each file) into a new `ui/components/StatChip.kt` - the first time this
color mapping has lived in one place instead of two. Verified on the user's real Pixel 7 (in-place
`installDebug`, no schema change): Gym now shows a red "STR" chip, Tasks a blue "DISCIPLINE" chip,
and both Water and Food a green "VIT" chip, all matching Habits' existing look exactly; a final
`adb logcat` sweep showed no crashes (one benign Play Protect `VerifyApps` scan log, not an app
error).

**Third feedback pass (2026-08-26, v1.2.0 -> v1.3.0)**: another same-day round, the biggest single
piece being a real schema/UX rework of Gym. Schema bump to v15 (`MIGRATION_14_15`) bundles two
unrelated-but-simultaneous changes, same "one evening, one version bump" approach prior passes used:
- **Permanent "Daily" task list**: `TaskList` gained `isProtected: Boolean` - a protected list can't
  be renamed or deleted (`TaskListTabRow`'s dropdown shows "Can't rename/delete Daily" instead,
  disabled). `position = -1` keeps it sorted first regardless of how many other lists exist, making
  it the default landing tab. The migration inserts a new "Daily" protected list *alongside*
  whatever lists already exist rather than renaming the user's existing default list - safer, since
  that list may have been renamed or hold data the user cares about. Fresh installs seed "Daily"
  directly (`seedDefaultListSql()`) instead of the old "Tasks" default.
  **Real bug caught during instrumented testing**: initially made `seedDefaultListSql()` (which now
  inserts `isProtected`) shared between fresh-install seeding and `MIGRATION_2_3` - but
  `MIGRATION_2_3` creates `task_lists` at its *original* 4-column v3 shape (no `isProtected` until
  v15), so replaying that migration on-device crashed with `SQLiteException: table task_lists has
  no column named isProtected`. Fixed by giving `MIGRATION_2_3` back its own literal insert SQL
  matching the schema shape it actually creates. Caught by the `connectedDebugAndroidTest` suite
  (`migrate2To3` failed), not by manual testing - exactly the kind of mistake the migration test
  pass exists to catch.
- **Add-subtask autofocus + Enter-to-confirm**: `AddSubtaskRow`'s field now requests focus via
  `FocusRequester` the moment it expands (no second tap needed to start typing), and Enter
  (`ImeAction.Done` + `KeyboardActions`) triggers the same confirm path as tapping the add icon.
- **Stat names**: Dashboard's per-stat level-bar rows now show the full word (`statTagFullName` in
  `StatChip.kt`: Strength/Vitality/Discipline/Intelligence/Spirituality) instead of the raw
  `StatTag.name`; every other stat label in the app (chips, radar chart axes) stays abbreviated,
  matching the user's explicit "abbreviate elsewhere, spell it out on the level bars" ask.
- **Gym: fixed weekdays -> user-defined workout split**: the user's exact complaint was that missing
  a scheduled gym weekday pushed every later exercise onto the wrong day. `Exercise.dayOfWeek`
  (Int 1-7) is replaced by `Exercise.splitDayId`, an FK to a new `SplitDay` entity (name + `colorHex`
  + `orderIndex`, cascades to its exercises on delete) - the user defines their own rotation ("Day 1
  - Back and Bi", colored green; "Day 2 - Chest and Shoulders", colored orange, etc.) with no
  calendar day baked in at all, so a missed day just shifts the rotation rather than corrupting a
  fixed schedule. `GymScreen` gained a `Routine`/`Calendar` `TabRow` (previously a single un-tabbed
  screen): Routine groups exercises under each split day's colored header (inline "+"/edit/delete
  per day, with a confirm dialog before delete since it cascades); Calendar is a new month grid
  (`WorkoutMonthCalendar`, mirroring `MonthHeatmap`'s leading/trailing-blank-padding fix but coloring
  each day by whichever split day was logged that date) with prev/next month nav and a color legend.
  Which split day was done on a given date is fully derived (`workoutCalendarForMonth` in
  `data/gamification/WorkoutCalendar.kt`) from existing `GymSession`+`Exercise.splitDayId` data,
  the same "derive, don't store a second copy" approach Rank/Quests/Rewards/Titles already
  established - no new log table needed. The same calendar (as a read-only preview, tap-through to
  the Gym tab) now also sits on the Dashboard's Home tab, right after Boss Fights.
  `AddExerciseDialog` no longer has a day picker at all - it's opened per-split-day-section, so the
  target day is already known from context; a `ColorSwatchPicker` (8-color preset palette, matching
  `SplitDay.COLOR_PALETTE`) replaces free-form color input for split days, same "preset palette, not
  a full picker" reasoning `FoodRating`/`MoodColor` already used for their own 3-color sets.
  Migration rebuilds `exercises` (create-copy-drop-rename, same pattern `FoodLogEntry`'s photoUri
  nullability change used) and creates one `SplitDay` per weekday that actually had an exercise
  pinned to it (named after the old weekday, e.g. "Monday", colored from the same rotating palette)
  - an empty gym routine migrates to an empty split rather than 7 blank placeholder days.
  **Ripple into Quests/Punishments/notifications**: `computeDailyQuests`/`computeWeeklyQuests`
  (`Quests.kt`), `detectMissedItems` (`Punishments.kt`), and `GymReminderWorker` all previously
  matched exercises to a calendar day via the now-deleted `dayOfWeek` field to answer "was gym
  scheduled today/this week." With no fixed schedule left to check fidelity against, these were
  redefined around a plain workout-frequency target instead of a spec-given number: `GYM_WEEKLY_TARGET
  = 3` (this app's own tuned value, same "no spec amount given" reasoning as other tuned XP numbers)
  workouts/week. Today's Quests collapses gym from "one item per exercise scheduled today" to a
  single "Log a workout today" item (no way to know which exercises are "due" without a schedule);
  Weekly Quests' "Complete all scheduled gym days" becomes "Work out X/3 days"; Punishments drops the
  per-day Minor case entirely (nothing "scheduled" to miss day-to-day anymore) and keeps only the
  weekly Major case, now measured against the same 3x/week target; `GymReminderWorker`'s "gym day
  reminder on scheduled days" becomes a gap-based reminder (fires once 2+ days have passed with no
  workout logged) rather than firing daily now that every day is a potential workout day.
- **Food: merged the camera FAB and pencil "log without a photo" button into one entry point** - the
  user's exact complaint was "I dont like how theres an edit button and camera button seperate from
  each other." `FoodScreen`'s single FAB (a generic "note+" `PostAdd` icon, not a camera) always
  opens the same `ConfirmFoodDialog` with no photo attached; the dialog itself gained an
  `onTakePhoto` callback - tapping an "Add a photo (optional)" prompt (or an already-attached photo,
  to retake) launches the camera and the result populates back into the still-open dialog rather
  than opening a second dialog. Dashboard's food quick-add keeps its "camera-first" shortcut
  behavior (still useful as a fast path) but now reuses the same updated dialog/retake plumbing.
- **Gold badge**: replaced the `MonetizationOn` coin icon in the Dashboard's top-bar HUD badge with
  plain "$" text (user feedback: "makes the app look cheep and doesnt fit the theme") - same badge
  position/styling, just no icon glyph.
Verified on the `SoloLeveling_Pixel6` emulator (phone was disconnected mid-session; emulator used as
fallback) - real v14->v15 migration path exercised via all 16 `connectedDebugAndroidTest` cases
(including the new `migrate14To15`, which seeds two exercises on different weekdays and asserts the
rebuilt `exercises` table's `splitDayId` and the generated `split_days` rows are correct), all
passing after the `MIGRATION_2_3` fix above (confirmed via `adb logcat`'s `TestRunner: finished:` /
`failed:` lines directly, since the Gradle task itself reported a false "Failed to receive the UTP
test results" - a known flaky result-transport issue on Windows, not a real failure). Manual pass:
created a "Day 1 - Back and Bi" split day (green), added a Deadlift exercise to it with no day
picker present, logged a 100kg session and confirmed both Gym's own Calendar tab and the Dashboard's
Workout Calendar preview immediately colored today green with the correct legend; confirmed Today's
Quests showed "Log a workout today" checked and This Week's Quests updated live from "Work out 0/3
days" to "1/3 days"; confirmed the Daily task list shows a lock icon and its long-press menu has
"Can't rename Daily"/"Can't delete Daily" both disabled; added a task and confirmed a single tap on
"+ Add subtask" both focused the field and let Enter confirm the add without touching the on-screen
add button; logged a food entry with no photo via the single FAB and confirmed it saved correctly
with a Healthy rating. A final `adb logcat` crash sweep showed no app crashes (only the same benign
`FrameTracker` IME-animation timeout seen in every prior phase).

**Fourth feedback pass (2026-08-26, v1.3.0 -> v1.3.1)**: two quick same-day changes.
- **Boss Fights removed** ("get rid of boss fights in the gym page"): a full removal, not just
  hiding the UI - `Boss` entity and `BossDao` deleted, `GymScreen`'s Boss Fights section/dialogs and
  `DashboardScreen`'s "Active Boss Fights" section/row both gone, boss XP-grant logic stripped out
  of `LogSessionDialog`'s confirm handler, and `BackupData`/`BackupManager` no longer carry boss
  rows. Schema bump to v16 (`MIGRATION_15_16`) drops the `bosses` table outright rather than
  leaving unused schema behind - matches this codebase's existing "delete dead code completely, no
  half-removed scaffolding" stance, just applied to the DB layer too. Today's/This Week's Quests
  and Punishments were already redefined around plain workout frequency in the prior pass (they
  never referenced Boss directly), so this removal doesn't touch them.
- **Dashboard reorganization**: Life Goals moved off the Rank badge and into a flag icon in the top
  bar, grouped with Calendar/Rewards/Punishment (all four are now the same "top-bar icon, no
  bottom-nav slot" pattern). The Rank badge's tap target was repurposed to open the stat radar
  chart in a dialog (`RadarChartDialog`) instead - the radar chart no longer sits permanently in
  the Home scroll ("hide the spiderchart normally"), only appearing on demand.
Verified on the `SoloLeveling_Pixel6` emulator (real v15->v16 migration, upgrading the same test
data used to verify the split-day rework): all 16 `connectedDebugAndroidTest` cases passed
(including the new `migrate15To16`, which seeds a real boss row and confirms the table is gone
post-migration via `sqlite_master`), confirmed the same way as the prior pass - directly via `adb
logcat`'s `TestRunner: finished:`/`failed:` lines, since the Gradle task itself again reported the
same known-flaky "Failed to receive the UTP test results" on this machine. Manual pass: Gym's
Routine tab now goes straight from the top bar into split days with no Boss Fights section at all;
Dashboard's top bar shows a flag icon that opens Life Goals; tapping the Rank badge opens a "Stat
Radar" dialog with the radar chart and a Close button, and the chart is no longer visible in the
normal Home scroll. A final `adb logcat` crash sweep showed no app crashes (only the same benign
`FrameTracker` IME-animation timeout seen in every prior phase).

**Fifth feedback pass (2026-08-30, v1.3.1 -> v1.4.0)**: two new features, bundled into one schema
bump per this session's established "one evening, one version bump" convention. Schema v16->v17
(`AppDatabase.MIGRATION_16_17`) adds two unrelated new tables:
- **Habits tab becomes "Routine"**: renamed throughout (`BottomNavDestination.Habits` ->
  `Routine`, icon `Repeat` -> `Schedule`) and now opens on a new Schedule sub-tab first (user
  feedback: "I also want it to open on a schedule screen first"), with the original habit list
  moved to a second Habits sub-tab (`TabRow`, same `RoutineScreen()`/`HabitsTab()` split as
  `GymScreen`'s Routine/Calendar or `LifeScreen`'s Mood/Food/Water). Schedule groups the day into
  `DayPart` (Morning/Day/Afternoon/Night, `data/entity/RoutineItem.kt`) - the user can drop in a
  free-text plan item or slot in an existing habit, "so they fit into my day rather than doing them
  at random times." Both shapes share one `RoutineItem` table (`habitId` null vs. set) rather than
  a sealed hierarchy Room can't map directly; a slotted habit's checkbox reads/writes the exact same
  `HabitLog` the Habits sub-tab already tracks (verified on-device: checking it off in Schedule
  shows checked in Habits and vice versa) - no second completion-tracking mechanism. The "+" in the
  top bar is contextual to the active sub-tab (opens `AddRoutineItemDialog` on Schedule,
  `AddHabitDialog` on Habits), matching `GymScreen`'s existing contextual-add pattern.
  **Real bug caught during manual on-device testing** (not migration tests this time): the day-part
  picker in `AddRoutineItemDialog` used a plain `Row` for the 4 `DayPart` chips - same overflow
  class this app has hit repeatedly (Phase 4 stat chips, Phase 7 heatmap, Phase 9 status chips,
  GoalsScreen). With 4 chips it overflowed and "Night" was completely unreachable - not just
  visually clipped but absent from the accessibility tree entirely (confirmed via `uiautomator
  dump` before and after the fix). Fixed by switching to `FlowRow`, matching the established fix.
- **Dashboard gains a Health tab**: `Home`/`Health`/`Analytics` (was `Home`/`Analytics`) - "I want
  to record my body stats. So weight, blood sugar mmol/L, and blood pressure. It needs to track the
  date time." `BodyStatEntry` (`data/entity/BodyStatEntry.kt`) is one table for all three types
  (`WEIGHT`/`BLOOD_SUGAR`/`BLOOD_PRESSURE`) rather than three near-identical tables - `value` holds
  Weight (kg, this app's own unit choice, no prior convention existed) or Blood Sugar (mmol/L, per
  the user's explicit unit), `systolic`/`diastolic` hold Blood Pressure; whichever pair doesn't
  apply to a row's type is left null. `timestamp` is a real epoch-millis date+time (unlike
  `MoodEntry`/`WaterLog`'s date-only PK) since more than one reading a day is expected - the log
  dialog combines Material3 `DatePickerDialog` and `TimePicker` (the same two components
  `TasksScreen`/`GoalsScreen` and `HabitsScreen` already use individually, just combined into one
  dialog here), defaulting to "now" but fully editable. `data/gamification/HealthAnalytics.kt`'s
  three pure functions (`weightTrend`/`bloodSugarTrend`/`bloodPressureTrend`) feed a new "Health
  Trends" section on the Analytics tab, reusing the existing `LineChart` component (blood pressure
  plots Systolic/Diastolic as two series, same multi-series shape the stat-trend chart already
  uses) - same "derive, don't store a second copy" reasoning, no new persisted trend data.
No other bugs found this pass.
Verified on the `SoloLeveling_Pixel6` emulator (phone had disconnected mid-session; emulator used
as fallback) - real v16->v17 migration path exercised via all 17 `connectedDebugAndroidTest` cases
(including the new `migrate16To17`, seeding a real habit row and asserting both new tables' shapes
against it), confirmed passing via `adb logcat`'s `TestRunner: finished:`/`run finished: 17 tests,
0 failed` lines (Gradle's own task result was reliable this run, no UTP flake). Manual pass: created
a habit, confirmed Routine opens on Schedule by default; the Night-chip overflow bug reproduced,
fixed, and reconfirmed fixed via `uiautomator dump`; slotted the habit into Morning via "Existing
habit," checked it off from Schedule, and confirmed the Habits sub-tab showed the same check + a
streak of 1, and the Dashboard's SPIRITUALITY stat moved to 10/50 XP and Gold to $1 - the shared-
HabitLog wiring is real, not just visually similar; added a free-text "Read a book" item under
Night and confirmed both sections render correctly grouped in chronological DayPart order; on the
Health tab, logged a Weight (72.5 kg), a Blood Pressure (120/80 mmHg), a second Weight (71.8 kg),
and a second Blood Pressure (118/76 mmHg), all with correct auto-filled date/time; confirmed the
Analytics tab's new "Health Trends" section rendered all three charts (Blood Sugar correctly showing
"Not enough readings yet" with zero entries) using real logged data. A final `adb logcat *:E` crash
sweep showed no app crashes (only the same benign `FrameTracker` IME-animation timeout seen in every
prior phase).

**Sixth feedback pass (2026-08-30, v1.4.0 -> v1.4.1)**: same-day polish on the Health tab just
shipped, no schema change.
- **Weight quick-add button alignment**: the icon (`Icons.Filled.MonitorWeight`) read visibly
  off-center next to "Weight" compared to the Sugar/BP buttons' icons, even though all three share
  identical `OutlinedButton`/`Icon`/`Text` structure - confirmed by unzipping the extended-icons jar
  (`unzip -l material-icons-extended-release-api.jar`) that the glyph itself, not the layout, was
  the culprit. Swapped to `Icons.Filled.Scale` (confirmed present in the same jar first), which
  reads centered.
- **Delete button removed from reading rows; long-press reveals it instead** - `BodyStatRow` now
  uses `combinedClickable` (`onLongClick` reveals a per-row `showDelete` state, a plain tap while
  revealed hides it again without deleting) rather than an always-visible trailing icon.
- **Tapping a type's heading (Weight/Blood Sugar/Blood Pressure) drills into a new
  `HealthDetailView`** - replaces the whole Health tab body (back arrow returns) with a period
  selector (`HealthPeriod`: Week/Month/6 Months/Year/All Time - `data/gamification/
  HealthAnalytics.kt`, fixed day-counts rather than calendar-aware for simplicity) plus a chart and
  the full scrollable reading list for that period, reusing the same `BodyStatRow` (long-press
  delete included) and `LineChart` component the rest of the tab already uses. The Analytics tab's
  own "Health Trends" section (last-30-readings, all types at once) is unrelated and unchanged.
Verified on the `SoloLeveling_Pixel6` emulator (the user's phone was locked mid-session, so this
pass was verified there instead - a plain `installDebug` update, no schema change): the Weight
icon now reads centered next to Sugar/BP; confirmed no delete icon shows on a normal tap and
long-pressing a specific row reveals its own delete icon (sibling rows untouched); tapped "Weight"
and confirmed the detail view opened with all 5 period chips wrapping correctly (no repeat of the
DayPart FlowRow bug - used FlowRow here from the start); deleted a reading from inside the detail
view and confirmed both the chart's "not enough readings" fallback and the main Health tab's list
updated correctly afterward. A final `adb logcat *:E` sweep showed no crashes (only the same benign
`FrameTracker` IME-animation timeout seen throughout this session).

**Seventh feedback pass (2026-08-30, v1.4.1 -> v1.5.0)**: currency removed from the Reward Economy
entirely (user feedback: "get rid of currency all together keep the rewards section... the
currency system was to complex"), replaced with a Minor/Major severity split - deliberately
mirroring the Punishment Pool's own Minor/Major split (`PunishmentSeverity` reused directly, same
`SeverityChip` visual language) for the opposite direction, a payoff instead of a debt. Schema
v17->v18 (`AppDatabase.MIGRATION_17_18`).
- **Gold is gone**: `GoldBalance`/`GoldTransaction` entities, `GoldEngine`, and every grant call
  site (`HabitsScreen`'s `toggleToday`, `GymScreen`'s session-log confirm, the Dashboard's "$"
  top-bar badge) all deleted outright - same "don't leave unused scaffolding around" stance this
  codebase already took for the Boss Fights removal. `RewardTarget` (the old "pick one reward as
  your period's target, earn Gold toward its cost" flow) is gone too, replaced below.
- **Two live-computed eligibility windows** (`data/gamification/Rewards.kt`), no persisted "unlock"
  flag: a good week unlocks exactly one Minor claim the following week ("Having a good week means
  in the following week I can claim 1 minor reward... at any time") - `wasLastWeekGood` reuses
  `computeWeeklyQuests`'s exact "good week" definition against the single week before this one; 3
  good weeks in the trailing 4 unlocks one Major claim this month ("After 3 good weeks in a month I
  can claim a major reward") - reuses `countGoodWeeksInLastN` unchanged, the same rolling-4-week
  simplification the old Gold-based Monthly-pool unlock already made (calendar months don't divide
  evenly into Monday-Sunday weeks, flagged as a carried-over interpretation, not a new assumption).
  `claimedMinorThisWeek`/`claimedMajorThisMonth` cap each to one claim per calendar week/month once
  eligible, checked against the new inventory below rather than a separate "already claimed" flag.
- **Claiming files a reward into a new inventory** (user feedback: "have an inventory to store my
  rewards in and when i 'use' them from my inventory that means i claimed it in real life") -
  `RewardInventoryItem` (title/severity snapshotted at claim time, not an FK to the pool item -
  deliberately unlike `PunishmentAssignment`'s FK+cascade precedent, since a reward already sitting
  in your inventory shouldn't vanish or go stale just because you later edit/delete that pool
  item's definition). `usedAt` null = pending in Inventory; set = History, the same dual-state role
  `PunishmentAssignment.resolved` already plays for Debts. `RewardPoolItem` keeps its `title` but
  swaps `cost`/`pool` for `severity: PunishmentSeverity` - the migration rebuilds the table
  (create-copy-drop-rename) and maps existing rows' old `pool` value (WEEKLY -> MINOR, MONTHLY ->
  MAJOR, the closest analogue: Weekly was the smaller/frequent pool, Minor is the smaller/frequent
  tier) rather than wiping the user's own reward titles; `gold_balance`/`gold_transactions`/
  `reward_targets` are dropped outright with no data preserved, since a Gold ledger and past
  claimed-target picks don't mean anything under the new system.
- **`RewardsScreen.kt` rewritten**: "This Week"/"This Month" eligibility cards (status text +
  Claim button once eligible) at the top, an Inventory section (pending items with a "Use" button),
  Minor/Major pool-management sections below that (mirroring `PunishmentScreen`'s own Pool section
  almost exactly), and a History section for used items. Claiming opens a picker of that severity's
  pool items (same `Surface`-row picker pattern `TitlePickerDialog`/`AddRoutineItemDialog`'s habit
  picker already use); the "New Reward" dialog is now just a title field plus a Minor/Major
  `FilterChip` pair - no cost field at all.
No bugs found this pass.
Verified on both the `SoloLeveling_Pixel6` emulator and the user's real Pixel 7 (phone had
reconnected and unlocked mid-session) - all 18 `connectedDebugAndroidTest` cases passed on *both*
devices simultaneously (`Starting 18 tests on SoloLeveling_Pixel6(AVD)` and `Starting 18 tests on
Pixel 7`, both confirmed via `adb logcat`'s `run finished: 18 tests, 0 failed` on each), including
the new `migrate17To18` (seeds a real Weekly-pool reward row and a gold_balance row, asserts the
reward survived re-shaped to `severity = 'MINOR'` and that `gold_balance`/`gold_transactions`/
`reward_targets` are all gone from `sqlite_master` afterward). Manual pass on the emulator: cold
start confirmed no crash and no "$" badge anywhere in the Dashboard top bar; Rewards screen showed
"Last week wasn't a good week" / "0/4 good weeks recently - need 3 to unlock" with no Claim buttons
(correct - this test data has no good weeks), confirming the eligibility gates read real data, not
a hardcoded unlocked state; added a Minor reward ("Coffee treat") via the cost-free "New Reward"
dialog and confirmed it appeared correctly under "Minor Pool" with a yellow MINOR chip. The real
Pixel 7's own v17->v18 migration (against genuine multi-week-old data, not fresh test data) also
completed with no crash and rendered the same clean Rewards screen. A final `adb logcat *:E` sweep
on both devices showed no crashes (only the same benign `FrameTracker` IME-animation timeout seen
throughout this session).

**Eighth feedback pass (2026-08-30, v1.5.0 -> v1.5.1)**: same-day follow-up - user asked "where is
inventory" right after the previous pass shipped. Real discoverability bug: `RewardsScreen.kt`'s
Inventory section, like Minor Pool/Major Pool/History, only rendered at all once it had at least
one row in it - a brand-new user (or anyone who hasn't claimed a reward yet) had no way to tell the
feature existed. Fixed by always rendering the "Inventory" header, with an explicit empty-state
line ("Nothing claimed yet - claim a reward above and it'll show up here") when there's nothing in
it yet, rather than hiding the whole section. Minor Pool/Major Pool/History stay conditional -
those are inherently empty until the user adds/claims something and don't need the same "prove this
exists" treatment Inventory does, since claiming is the screen's main call to action. No schema
change. Bumped to v1.5.1 (versionCode 10). Verified on the `SoloLeveling_Pixel6` emulator: Inventory
now shows immediately below the This Week/This Month cards with the empty-state message even with
zero claims; a final `adb logcat *:E` sweep showed no crashes.

## Locked-in decisions

- Package/applicationId: `com.nightpixel.sololeveling`
- Versioning: `versionName` follows semver (`MAJOR.MINOR.PATCH`) - bumped to `1.0.0` once all 17
  spec Section 10 phases landed (0.x.x meant "still incomplete" beforehand); MINOR for a new
  feature, PATCH for a bug fix, going forward. `versionCode` is a plain incrementing integer bumped
  alongside it. Both live in `app/build.gradle.kts` and `versionName` is user-visible in Settings.
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

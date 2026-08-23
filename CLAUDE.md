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
Next up: **Phase 6** — Google Calendar integration (OAuth, read + create events), per spec
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
- An AVD `SoloLeveling_Pixel6` (Pixel 6, API 35, x86_64) exists and is the standard way to test
  changes on this machine: `./gradlew installDebug` builds+installs to whichever device/emulator
  `adb` sees. Start it with `emulator -avd SoloLeveling_Pixel6` if it's not already running.

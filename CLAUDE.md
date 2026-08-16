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
Tasks screen is a horizontally-scrollable board (`TaskList` entity, schema v3,
`AppDatabase.MIGRATION_2_3`) - each named list is its own column with its own task feed, plus a
trailing "+ Add list" column. `Task.listId` has no DB-level foreign key (would need a full table
rebuild in SQLite just to add one) - list deletion cascades to its tasks via
`TaskListDao.deleteListCascading` instead, called from app code. A default "Tasks" list is seeded
both by the migration (existing installs) and `RoomDatabase.Callback.onCreate` (fresh installs).
Next up: **Phase 4** — Habit Tracker, per spec Section 10.

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

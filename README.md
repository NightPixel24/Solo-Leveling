# Solo Leveling

A personal Android life-RPG tracker (tasks, habits, gym, mood, food/water) wrapped
in a Sung Jin-Woo "System"-style gamification layer. Single user, single device,
sideloaded — no backend, Room (SQLite) is the source of truth.

Full product spec: [docs/SPEC.md](docs/SPEC.md).

## Status

Phase 1 (spec Section 10): project scaffold, navigation shell, dark/blue/violet
theme, and a minimal Room schema (`AppMeta` only). Every bottom-nav screen is a
placeholder until its phase is built.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite), no backend
- Navigation Compose
- KSP for Room annotation processing
- minSdk 26, targetSdk/compileSdk 35

## Building

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. Install over the existing
app with `adb install -r app-debug.apk` to keep local data (same package name +
signing key required — see spec Section 3 "Installing Updates on Your Phone").

## Data safety

Every schema change ships with a Room `Migration`, never a destructive rebuild.
A full JSON export/import (Settings screen, once built) is the backup and
phone-migration path — see spec Section 3.

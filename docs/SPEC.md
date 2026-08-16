# Solo Leveling — Personal Life-RPG Tracker
### Product Spec for Claude Code

Android-only, single user, solo-dev, $0 recurring cost.

> Note: "Solo Leveling" is the name of an existing manhwa/novel/anime franchise. Using it as a private app name on your own device is fine — just keep in mind if you ever publish it publicly (Play Store listing, sharing the APK widely) you'd want a different public-facing name to avoid any trademark issue. For personal use, no problem.

---

## 1. Overview

A daily-use Android app that tracks tasks, habits, gym progress, mood, food, and water, wrapped in a Sung Jin-Woo "System"-style RPG layer: stats, a goal-driven Rank, quests, boss fights, a punishment pool, and a reward economy. Everything feeds a central Dashboard styled like the System window.

---

## 2. Platform & Tech Stack

- **Platform:** Android only.
- **Language/UI:** Kotlin + Jetpack Compose (Material 3).
- **Theme:** Dark background (near-black), with electric blue and violet/purple accents, glowing panel borders, angular sans-serif type — a System-window look. Level-up and quest-complete moments get a blue/purple flash + haptic buzz.
- **Local database:** Room (SQLite). This is the source of truth — no backend server.
- **Settings:** Jetpack DataStore.
- **Background jobs:** WorkManager — daily quest generation, streak evaluation, notification scheduling.
- **Charts:** Vico or MPAndroidChart for the radar/spider chart and analytics graphs.
- **Camera/photos:** CameraX (or simple camera intent) + local file storage for food photos and progress photos.
- **Google Calendar:** Google Sign-In (Credential Manager) + Google Calendar API, called directly from the app with OAuth — no custom backend needed.

**Why no backend:** single user, single device, no login system beyond Google's own OAuth for Calendar. This keeps the whole project free — only cost is a one-time Google Cloud Console project (free tier covers this usage), and you can sideload the APK directly instead of publishing to Play Store.

---

## 3. Data Portability & Updating the System

You'll be tweaking the gamification rules yourself over time (new stats, different XP formulas, new quest logic), so data safety across app updates is treated as core, not an afterthought:

- **Room Migrations:** every time the data schema changes (new field, new table), the build must include a proper Room migration so an app update never silently wipes your existing data. Claude Code should write a migration for every schema-affecting change rather than falling back to destructive rebuild.
- **Export (Backup):** a full JSON export of every table (tasks, habits, gym data, stats/XP, goals, quests, mood, food/water logs, rewards, punishments, settings) to a file you can save anywhere (Google Drive, local storage, etc). Build this early and extend it every time you add a new module, so you always have a current, complete backup path.
- **Import (Restore):** loads a previously exported JSON file back into the local database. Should tolerate missing/extra fields gracefully (default values for anything new) so an old backup still loads after you've tweaked the system.
- **Recommended habit:** export a backup before installing any app update that changes core logic, and keep a few dated backups around. The export is also your way to migrate to a new phone if needed.

### Installing Updates on Your Phone

This is sideloaded, not Play Store, so how you install matters:

- **Signing key:** Android only updates an app in place (keeping its data) if the new APK has the same package name AND is signed with the same key as what's already installed. Set this up once in Phase 1 — either a dedicated release keystore, or just always building from the same machine's debug keystore — and keep using it for every future build. If the signing key ever changes, Android refuses to install over the existing app and forces an uninstall first, which wipes local data unless you'd exported.
- **Update loop:** request a change → Claude Code edits the code and pushes to your GitHub repo → build the APK (Android Studio, or Claude Code's build tooling if available) → install over the existing app with `adb install -r app.apk` (USB debugging) or by copying the APK to your phone and tapping it (shows "Update," not "Install," if signed consistently) → either way your data stays intact.
- **Schema changes** (new/changed tables) are handled by the Room migrations required in Section 3 above — that's what keeps the in-place update from corrupting or dropping existing rows.
- Still export a backup before any update you're unsure about, as a safety net independent of all of the above.

---

## 4. Core Modules

### 4.1 Calendar
- Two-way sync with Google Calendar (read existing events, create new ones from the app).
- Month/week/day views. Personal events and app-generated events (e.g. gym sessions) shown together, visually distinguished.

### 4.2 Tasks & Subtasks
- Standard task list: title, due date, priority, notes.
- Tasks can have nested subtasks (checklist within a task).
- Completing a task/subtask grants a small XP bonus (feeds DISCIPLINE, see Section 5).

### 4.3 Habit Tracker
- Habits are tagged **Daily** or **Weekly** (e.g. "3x/week").
- Each habit is tagged with which Stat it feeds when created — e.g. a study or brain-training habit tags **INT**, a health-related one tags **VIT**, a general consistency habit tags **DISCIPLINE**.
- Daily view: checklist of today's habits. Weekly habits show progress toward their weekly target (e.g. 2/3 done).
- Streak counter per habit, with a streak-freeze mechanic (Section 5.4).

### 4.4 Gym Tracker
- You build a weekly routine: assign exercises to specific days.
- Each exercise has a **type**: **Strength** (sets/reps/weight, feeds STR) or **Cardio/Sport** (duration, distance/intensity, feeds AGILITY) — a single week can mix both, e.g. Mon/Wed/Fri lifting, Tue/Sat a run or a sport session.
- Each week is a checklist — check off each planned day as completed.
- Progress over time (weight/volume for Strength, frequency/duration for Cardio/Sport) drives STR and AGILITY, and can trigger Boss Fights (Section 5.5). Just showing up on scheduled gym days (regardless of type) also feeds DISCIPLINE.

### 4.5 Mood Tracker
- Once per day (evening), rate the day on a 3-color scale: **Green = good, Yellow = ok, Black = bad**. Optional note field.
- Calendar heatmap view (month and year-at-a-glance) colored by these entries.

### 4.6 Food & Water Tracker
- **Food:** take a photo, write what it was, timestamp. Simple chronological log per day — no calorie estimation for v1 (flagged as a later option in Section 10).
- **Water:** you set a daily goal in liters, converted to number of 1L bottles. Each day shows a row of bottle icons you tap to fill in as you drink; progress bar toward the daily goal.

### 4.7 Life Goals
- You define goals at seven tiers: **1-Month, 3-Month, 6-Month, Yearly, 5-Year, 10-Year, Lifetime (end goal)**.
- Each goal: title, description, target date, status (active/completed/failed), and optionally linked Tasks/Habits as milestones so you can see partial progress before it's fully done.
- This module is what drives your **Rank** (Section 5.3) — it's the "solo leveling" long game, separate from the day-to-day XP grind.

---

## 5. Gamification System

### 5.1 Stats & the Status Window (radar chart)

Five stats, each with its own level (1–99) and XP bar, plotted as a pentagon radar chart on the Dashboard so a lopsided shape tells you at a glance what to work on:

| Stat | Fed by |
|---|---|
| **STR** (Strength) | Gym: Strength-type weight increases / PRs |
| **VIT** (Vitality) | Water goal hit, food logged, habits tagged "health" |
| **DISCIPLINE** | Habit streaks, task/subtask completion, showing up on any scheduled gym day, weekly quest completion |
| **INT** (Intelligence) | Study/brain-training habits |
| **AGILITY** | Gym: Cardio/Sport-type sessions (frequency, duration, intensity) |

### 5.2 XP & Leveling (per-stat)

- Each stat levels independently, 1–99.
- XP required for next level: `XP_needed = round(50 * level^1.2)` — a tunable starting curve.
- Example grants (tune later): habit completion +10 XP to its tagged stat, gym session logged +15–40 XP depending on PR vs normal session, water goal hit +10 VIT, task completed +5 DISCIPLINE, weekly quest bundle +50 XP split across relevant stats.
- Stat levels also drive your Gold income (Section 5.7) — they're your "grind" layer.

### 5.3 Rank (E → SS) — driven by Life Goals, not XP

Rank reflects real progress on your life goals, not daily grinding. It advances one step each time you complete your first goal at the next tier:

| Milestone | Rank Unlocked |
|---|---|
| Starting point | **E** |
| Complete a 1-Month goal | **D** |
| Complete a 3-Month goal | **C** |
| Complete a 6-Month goal | **B** |
| Complete a Yearly goal | **A** |
| Complete a 5-Year goal | **S** |
| Complete your 10-Year / Lifetime goal | **SS** |

Default rule: completing **one** goal at a tier unlocks that rank (simplest to reason about). If you'd rather require *all* active goals at a tier to be done before advancing, that's a one-line toggle in Settings — worth deciding once you're actually using it and see how many goals you tend to run per tier.

The Rank badge sits next to the Stat radar chart on the Dashboard, but the two are visually distinct — Stats/radar = your daily grind, Rank = your life trajectory.

### 5.4 Quests

- **Daily Quests** — auto-generated every morning: one entry per habit due today, gym session if scheduled today, water goal, tonight's mood check-in, and any tasks due today. Shown as "Today's Quests" on the Dashboard.
- **Weekly Quests** — auto-generated each Monday from weekly targets, e.g. "Complete all scheduled gym days," "Hit water goal 6/7 days," "Zero missed daily habits." Completing all (or a configurable %) of them makes the week count as a **"good week"** (used for monthly rewards, Section 5.7).
- **Side Quests** — your Task list, reframed. Completing one gives a small bonus on top of normal task XP.
- Streak-freeze: once per week (configurable), a missed daily habit doesn't break its streak.

### 5.5 Boss Fights

- **PR Boss** (Strength gym exercises): set a target lift, e.g. "Bench Press 100kg." Boss HP = target − current max. Each new max logged does damage; hitting the target defeats the boss for a bonus reward and a permanent trophy.
- **Streak Boss** (habits or Cardio/Sport, e.g. "Run 3x/week for 8 weeks straight"): HP counts down by 1 per successful period; missing one either resets HP (Hard mode) or just pauses that period (Easy mode) — your choice per boss.

### 5.6 Punishment Pool

- You pre-populate a list of punishments (e.g. "50 pushups," "cold shower," "no phone after 9pm"), tagged **Minor** or **Major**.
- Missing a daily habit or scheduled gym day → app randomly assigns one **Minor** punishment as a "Debt" you mark complete to clear it.
- Missing a whole week's target for a habit/gym → randomly assigns one **Major** punishment instead.
- The pool's contents are entirely up to you — the app just picks randomly and tracks completion.

### 5.7 Reward Economy (Weekly / Monthly)

- Habits and gym completions grant **Gold** in addition to stat XP (e.g. 1 Gold per 10 XP — tune later).
- You maintain a **Reward Pool**: real-world rewards you define, each with a Gold cost, split into a **Weekly pool** (smaller) and **Monthly pool** (bigger).
- Each week, pick one reward from the Weekly pool as your target; once you've earned enough Gold that week it unlocks and Gold is spent.
- If **3 of the last 4 weeks** were "good weeks" (Section 5.4), you unlock the ability to pick one reward from the Monthly pool.
- Gold balance and reward history live on a dedicated Rewards screen.

---

## 6. Dashboard & Analytics

The main screen on open. Includes:
- Status Window: Rank badge (goal-driven), radar chart of the 5 stats, Gold balance.
- Today's Quests checklist.
- Active boss fight(s) with HP bars.
- Mood heatmap preview (current month).
- Quick-add buttons: log water, log food, check off a habit.
- Life Goals summary (current active goal per tier, progress).
- Analytics tab: stat trends over time, habit completion %, gym volume/PR progression, mood distribution per month, weekly/monthly good-week history.

---

## 7. Notifications

Local notifications (WorkManager/AlarmManager, no push server needed):
- Reminders for habits due today (time configurable per habit).
- Water reminders spaced through the day toward your daily goal.
- Evening mood check-in prompt.
- Gym day reminder on scheduled days.
- Weekly review prompt (e.g. Sunday evening) and monthly review prompt.

---

## 8. Screens / Navigation

Bottom nav: **Dashboard · Calendar · Tasks · Habits · Gym · Mood/Food/Water (tabbed) · Rewards**. Life Goals and Rank detail are reached by tapping the Rank badge on the Dashboard. Settings (including Export/Import) reachable from Dashboard.

---

## 9. Data Model (entities, high level)

- `Task` (title, dueDate, priority, notes) → `Subtask` (parentTaskId, title, done)
- `Habit` (title, frequency [daily/weekly], targetPerWeek, statTag, reminderTime) → `HabitLog` (habitId, date, done)
- `Exercise` (name, dayOfWeek, type [strength/cardio_sport], targetSets, targetReps, targetWeight, targetDuration) → `GymSession` (exerciseId, date, actualSets, actualReps, actualWeight, actualDuration, intensity)
- `Stat` (name [STR/VIT/DISCIPLINE/INT/AGILITY], level, currentXP)
- `XPLog` (source, statAffected, amount, timestamp) → audit trail for tuning formulas later
- `Goal` (tier [1mo/3mo/6mo/yearly/5yr/10yr/lifetime], title, description, targetDate, status, linkedTaskIds)
- `Quest` (type [daily/weekly/side], description, sourceRef, done, date)
- `Boss` (type [PR/streak], name, targetValue, currentHP, mode, defeated)
- `PunishmentPoolItem` (description, severity)
- `PunishmentAssignment` (itemId, dateAssigned, resolved)
- `RewardPoolItem` (description, cost, tier [weekly/monthly])
- `RewardRedemption` (itemId, dateRedeemed, weekOrMonth)
- `MoodEntry` (date, color, note)
- `FoodLogEntry` (date, photoUri, description)
- `WaterLog` (date, bottlesLogged, goalBottles)
- `CalendarEventCache` (googleEventId, title, start, end, syncedAt) → local mirror for offline viewing
- `AppMeta` (schemaVersion) → used by Room migrations and the Export/Import format

---

## 10. Suggested Build Order (test between each phase, as you asked)

1. Project scaffold, navigation shell, Room schema, dark/blue/purple theme.
2. Basic Export/Backup scaffold (JSON write) — extend it every phase after as new tables are added.
3. Tasks & Subtasks.
4. Habit Tracker.
5. Gym Tracker (Strength + Cardio/Sport types).
6. Google Calendar integration (OAuth, read + create events).
7. Mood Tracker (color scale + heatmap view).
8. Food & Water Tracker (camera capture, bottle checklist).
9. Life Goals module (tiers, status tracking).
10. Gamification core: Stat engine (STR/VIT/DISCIPLINE/INT/AGILITY), XP/leveling, radar chart.
11. Rank engine (goal-tier based, E→SS).
12. Quests (daily/weekly auto-generation) + Boss Fights.
13. Punishment Pool.
14. Reward Economy (Gold ledger, weekly/monthly redemption).
15. Dashboard/Analytics screen tying everything together.
16. Notifications.
17. Polish: full Export/Import coverage finalized and tested, Room migration test pass, Settings screen.

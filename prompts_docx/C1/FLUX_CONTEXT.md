# Flux App Family — Project Context Document

**Version:** 1.0 | **Date:** March 2026 | **Author:** Ari

---

## What This Document Is

This is a pure context document. It explains what we are building, why, and how the two apps relate to each other. It contains **no coding instructions**. This document is provided to both the AI coding agent (Antigravity) and to Claude when starting new sessions, so both have the same foundational understanding of the project without needing to re-explain it every time.

---

## 1. Who Is Building This

**Ari** is a student preparing simultaneously for:
- **Math Olympiad:** IOQM → INMO → IMO
- **JEE Foundation:** Neev Diamond batch at PW (Physics, Chemistry, Math)
- **School:** ICSE Class 10 → 12
- **Programming:** General programming development

Ari directs AI agents to write code and produce specifications. He understands architecture and logic but delegates all implementation entirely to AI. He does not write code himself. All outputs are intended for AI-directed development.

---

## 2. The Flux Family

Two custom Android apps are being built as replacements for two existing apps Ari uses:

| App | Replaces | Abbreviation |
|---|---|---|
| **FluxDone** | TickTick (task management) | FD |
| **FluxFoxus** | Regain (screen discipline and focus) | FF |

Both apps are:
- Offline-first (SQLite-backed, fully functional without internet)
- Android Phase 1 → iOS + Web Phase 2
- Built in Flutter (Dart)
- Part of the same **Flux family** with a shared naming convention

The two apps are **separate applications** that communicate with each other via Android MethodChannel IPC. They are not combined into one app. The reason for keeping them separate: independent updates, cleaner UX, manageability, and future independent publishing.

---

## 3. FluxDone (FD) — Task Management App

### What it replaces
TickTick. Ari's entire academic task workflow runs through TickTick today. FD must replicate this workflow exactly before any improvements are made.

### What Ari actually uses in TickTick
**Core:** Lists and Folders, Sections inside lists, custom colors per list, Calendar view (primarily for task *creation* via timeline tapping — not passive viewing), due dates and reminders, Smart Lists (Today, Tomorrow, All, Completed, Trash), recurring tasks, task completion.

**Occasional:** Habit tracker, subtasks, priority flags.

**Not used:** Pomodoro, attachments, filters, collaboration, tags.

### Key behavioral insight
The Calendar View is Ari's **primary task creation surface**. He opens the calendar, sees existing time blocks, finds a gap, taps the timeline to place a new task with a start and end time. This is not a passive view — it is the main way timed tasks get created. The Calendar View must be optimized for creation, not just viewing.

### FD's academic color system
7 domain colors are system-locked and mirrored in the Notewise companion app:

| Domain | Hex |
|---|---|
| Number Theory | #2E7D32 |
| Geometry | #1565C0 |
| Combinatorics | #43A047 |
| Algebra P1 | #FB8C00 |
| Algebra P2 | #E64A19 |
| Tests | #E53935 |
| Neev Diamond | #576481 |

All other lists (Programming, Short Notes, School, Doubts, etc.) use user-defined hex colors via a color picker. No system-locked values.

### FD's lists
NT, Geometry, Combinatorics, Algebra P1, Algebra P2, Tests, Doubts, Short Notes, School, Neev Diamond (sections: Physics/Chem/Math), Programming, Books, Extra, Others.

### FD's key design principles
- **Offline-first:** All core features work with zero network
- **Modular architecture:** Every feature is an independent module — one broken feature must not affect others
- **Token-based theming from day one:** All color values must be sourced from a `ThemeTokens` class. No hardcoded colors anywhere in the widget tree. This enables Phase 3 JSON custom theming with no widget rewrites.
- **Clone first, improve later:** Phase 1 replicates TickTick exactly. Improvements come in Phase 2.
- **Both light and dark themes ship in Phase 1**

---

## 4. FluxFoxus (FF) — Screen Discipline and Focus App

### What it replaces
Regain. FF is not a wellness app — it is an **enforcement and tracking tool** with deliberate friction mechanics designed to make breaking focus habits costly enough to think twice, but never punitive enough to destroy motivation.

### What Ari actually uses in Regain
**Core:** Focus Timer (plain timer — Ari uses external music via Spotify/YT Music/Echo Nightly, NOT built-in music), App Limits (solo for daily screen time control), Block Scheduling (combined with App Limits for structured study sessions), YouTube Study Mode, Screen Time Tracking.

**Occasional:** Planner/Calendar (general session planning).

**Not used:** Built-in focus music, Multiplayer Focus/leaderboards, AI companion Rega.

### FF's core concept: Presets
A **Preset** is the central abstraction in FF. It is a reusable focus configuration that stores: break count (0–6), break duration (1–15 mins each), per-app restrictions, YouTube mode (Block/Allow/Study Mode), and an optional description. Presets are applied to sessions — they do not store time windows.

### FF's key features
- **Focus Timer:** Three modes — Countdown, Stopwatch, Open-Ended. Displayed as a mechanical flip clock (HH:MM). Full-screen takeover during active session.
- **Break System:** 0–6 breaks per preset, 1–15 min each. Break exhaustion greyscales the break button.
- **Stop Focusing Modal:** Both buttons (Keep Going / Quit Session) disabled for 15–25 seconds. Wait time is formula-driven based on current streak and today's distracting app usage.
- **Streaks:** Count of consecutive days meeting minimum session target. Resets to 0 on confirmed quit.
- **App Limits:** Per-app daily time budgets. Extra time sessions (0–6 sessions, 5/10/15 min each, max 60 min total). Turn-off flow with 3-second countdown and streak warning.
- **YouTube Study Mode:** Channel whitelist enforcement. Non-whitelisted channels trigger a confirmation sentence flow (5 short sentences for first 25 channels, ~50 longer sentences after). Edge grabber toggle on screen right edge.
- **2/5/10/20 Intervention Window:** Overlay when a Distracting app is opened outside a focus session. User selects a time window (2/5/10/20 min). Wait period before re-selecting on expiry. Does NOT affect streaks.
- **Screen Time Tracking:** Android UsageStats API. 4 categories: Productive, Semi-Productive, Distracting, Others.
- **Planner:** Read-only session view. Day strip (Mon–Sun). Stats row. Session cards with FF/FD badges.
- **Home Screen Widget:** 4-segment donut ring, category legend, 15-min WorkManager refresh.
- **Weekly Report:** Every Sunday notification. Streak + focus totals + week comparison.

### FF's design system (dark-only)
- Background: #0F172A (Midnight Slate)
- Surface: #1E293B (Slate Blue)
- Primary: #6366F1 (Electric Indigo)
- Accent: #22D3EE (Soft Cyan)
- Text: #F8FAFC (Ghost White)
- Font: Inter / Gilroy
- Style: Flat 2D, no shadows, no gradients, "Academic Pro" aesthetic
- **Dark mode only in Phase 1**

---

## 5. FD ↔ FF Integration

The two apps communicate via **Android MethodChannel IPC** on the channel name: `com.fluxfoxus/fd_integration`

### FD → FF: FocusBlockRequest
When FD creates, updates, or deletes a task that has both a `start_time` and `end_time` set, it sends a `FocusBlockRequest` to FF.

**Payload:**
```
FocusBlockRequest {
  taskId: String
  taskName: String
  startTime: DateTime (nullable)
  endTime: DateTime (nullable)
  duration: Duration (nullable)
  listId: String
  sectionId: String (nullable)
  action: Enum (CREATE / UPDATE / DELETE)
}
```

**FF response:**
- CREATE → FF creates a FocusSession linked to taskId using last-used preset
- UPDATE → FF updates the linked FocusSession's time/duration
- DELETE → FF deletes the linked FocusSession

**Recurring tasks:** Each new recurring task instance generated by FD fires its own FocusBlockRequest (CREATE action) to FF independently.

### FF → FD: createTask
When a user starts a focus session from FF directly (not triggered from FD), FF calls `createTask` on FD. FD writes a "Focus Session" task to the **"FF" section** of FD's default list.

The "FF" section must exist in FD's default list from initial app setup. FD creates it automatically on first launch. Its sectionId is stored in shared_preferences so FF can always resolve it.

**FocusSession task description contains:** preset name, session duration, mode, start time.

### 15-minute heads-up notification
For all scheduled sessions (FF-created or FD-synced), FF pushes a notification 15 minutes before session start time: "[Session name] starts in 15 minutes." Tapping it opens FF with the session's preset pre-loaded.

---

## 6. Tech Stack (Both Apps)

| Area | FluxDone | FluxFoxus |
|---|---|---|
| Framework | Flutter (Dart) | Flutter (Dart) |
| State management | flutter_bloc (BLoC/Cubit) | riverpod (v2+) |
| Local DB | sqflite (SQLite) | sqflite + Hive |
| Navigation | go_router | go_router |
| DI | get_it + injectable | get_it |
| Notifications | flutter_local_notifications | flutter_local_notifications |
| IPC | MethodChannel (com.fluxfoxus/fd_integration) | MethodChannel (same) |
| Background tasks | — | workmanager + flutter_foreground_task |
| Charts | — | fl_chart |
| Widget | — | home_widget |

---

## 7. Documentation Structure

The full specification for both apps is contained in the following documents (all provided alongside this context file):

**FluxDone:**
- `FluxDone_PRD_v2.docx` — Product Requirements Document v2.0
- `FluxDone_TRD_v2.docx` — Technical Requirements Document v2.0
- `ui_SCR-01_list-view.md` through `ui_SCR-13_google-drive-backup.md` — 13 locked UI specification files
- `ui_ICONS_icon-system.md` — Icon system specification

**FluxFoxus:**
- `FF_PRD_v1.0.md` — Product Requirements Document
- `FF_TRD_v1.0.md` — Technical Requirements Document
- `ui_home.md`, `ui_focus_session.md`, `ui_preset.md`, `ui_app_limits.md`, `ui_planner.md`, `ui_usage_stats.md`, `ui_distracted_intervention.md`, `ui_widget.md`, `ui_navigation.md`, `ui_design_system.md` — 10 locked UI specification files

**All UI spec files are locked at v1.0. Do not deviate from them without explicit instruction from Ari.**

---

## 8. Development Philosophy

- **Spec-first, always.** All decisions are locked in PRD/TRD/UI specs before any code is written.
- **Modular.** Every feature is a self-contained module. One broken module must not cascade.
- **Offline-first.** Core functionality works with zero network. Network features are enhancements.
- **Token-based theming.** All colors via ThemeTokens in FD. No hardcoded values.
- **Clone first.** Phase 1 replicates the existing apps' workflows exactly. Phase 2 introduces improvements.
- **FD before FF.** Build and validate FluxDone completely before beginning FluxFoxus.
- **IPC last.** The MethodChannel bridge between FD and FF is built after both apps are individually stable.

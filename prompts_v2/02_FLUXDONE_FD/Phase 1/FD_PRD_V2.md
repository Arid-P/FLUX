# FluxDone (FD) — Product Requirements Document (PRD v2.0)

**Version:** 2.0 (Native Kotlin + Jetpack Compose & Compose Multiplatform Architecture)  
**Date:** March 2026 / v2 Overhaul  
**Target:** Android Mobile (`com.fluxdone`, API 26+) + Ubuntu Linux Desktop (`com.fluxdone.desktop`, Compose Multiplatform / JetBrains Compose)  
**Author:** Ari

---

## 1. Project Overview & Product Vision

### 1.1 What FluxDone Replaces
FluxDone (FD) is a high-performance, offline-first task and habit management application built to completely replace **TickTick** in Ari's academic workflow. It replicates TickTick's proven organizational patterns (lists, folders, sections, calendar time-blocking, recurrence, habits) while stripping out unused bloat (pomodoros, collaboration, file attachments) and optimizing specifically for intensive STEM study.

### 1.2 Core Success Definition
1. **Zero Regression from TickTick:** Every task and time-blocking workflow Ari performs in TickTick must be supported with zero friction.
2. **Calendar View as Primary Creation Surface:** Creating a timed task directly from the timeline grid must require minimal taps (tap slot → enter title → confirm).
3. **Curated Academic Theming:** 12-swatch curated palette featuring the 7 signature academic colors for Olympiad and JEE preparation mirrored with Notewise notebooks, plus custom hex color support.
4. **100% Offline-First:** Operates completely without internet using local SQLite storage via Room.

---

## 2. Target User & Behavioral Patterns

### User Profile: Ari
- Preparing concurrently for Math Olympiad (IOQM/INMO/IMO), JEE Foundation (PW Neev Diamond), and School (ICSE).
- High task volume with rigid daily schedules. Requires clear visual separation between subjects.

### Key Behavioral Insights:
1. **Timeline Tapping:** Ari does not use the calendar view as a passive agenda viewer. He opens the 3-day or 1-day timeline, visually identifies open gaps between study blocks, taps the empty time slot, and schedules a task with explicit start and end times.
2. **Sections Inside Subject Lists:** Neev Diamond requires subdivision into Physics, Chemistry, and Mathematics sections.
3. **Habit Streaks & Multi-Step Targets:** Daily habits (problem-solving, revision) support both 1-tap completions and numeric step targets (e.g. 12 problems in steps of 3) with tactile check-offs and streak tracking.

---

## 3. Core Features Inventory

| Feature | Priority | Phase | Description |
| :--- | :--- | :---: | :--- |
| **Lists, Folders & Sections** | P0 (Must Have) | Phase 1 | 4-tier tree: Folders contain Lists; Lists contain Sections and Tasks. |
| **Sections inside Lists** | P0 (Must Have) | Phase 1 | Collapsible section dividers within lists (e.g., Neev Diamond → Physics, Chem, Math). |
| **Calendar View** | P0 (Must Have) | Phase 1 | 3-Day and 1-Day vertical timeline grids with drag-to-create time-blocks. |
| **Curated Color System** | P0 (Must Have) | Phase 1 | 12 curated swatches (including 7 academic colors) + custom hex input. |
| **Smart Lists & Trash** | P0 (Must Have) | Phase 1 | Dynamic auto-filtered lists: Today (with 1-by-1 overdue triage), Tomorrow, Upcoming, All, Completed (4 chronological buckets), and 30-day soft-delete Trash. |
| **Recurring Tasks** | P0 (Must Have) | Phase 1 | RFC 5545 RRULE-based recurring schedules with Option A lifecycle and 3-choice delete dialog. |
| **Subtasks & Notes** | P1 (Core) | Phase 1 | Checkable subtask checklists with bidirectional sync and live markdown notes. |
| **Habit Tracker** | P1 (Core) | Phase 1 | Dual modes (1-Tap Simple & Numeric with custom step size), hard ceiling, long-press decrement, and rest days. |
| **FD ↔ FF IPC Broadcast**| P0 (Must Have) | Phase 2 | Dispatches native Android broadcasts (`com.flux.action.FOCUS_BLOCK_SYNC`) to FluxFoxus upon timed task creation/update/delete. |
| **Local Backup / Export**| P1 (Core) | Phase 2 | JSON and SQLite database snapshot export/import. |
| **Desktop Companion** | P0 (Must Have) | Phase 3 | Native Ubuntu Linux companion via Compose Multiplatform (JetBrains Compose). |
| **Cloud Sync & Web** | P2 (Future) | Phase 4 | Multi-device sync to Ubuntu Desktop and browser access via central DB. |

---

## 4. Academic Domain Color System & Curated Palette

The curated 12-swatch palette includes the 7 signature academic domain colors to maintain visual consistency with Ari's Notewise notes:

```
[#2E7D32] Number Theory (Olympiad Emerald)
[#1565C0] Geometry (Royal Azure)
[#43A047] Combinatorics (Forest Green)
[#FB8C00] Algebra P1 (Amber Gold)
[#E64A19] Algebra P2 (Vermillion Orange)
[#E53935] Tests & Mocks (Crimson Red)
[#576481] Neev Diamond (Deep Slate)
```

*Personal lists (School, Programming, Books, Misc) select from additional vibrant swatches (Amethyst Purple, Teal, Rose, Electric Indigo, Charcoal) or any custom hex color code.*

---

## 5. Screen Inventory & UX Map

```
SCR-01: Main List View (Tasks, sections, quick check-off, filter pills)
SCR-02: Task Detail Sheet (Title, dates, times, recurrence, subtasks, notes, list selector)
SCR-03: Quick Task Creation Sheet (Compact bottom sheet for rapid entry)
SCR-04: Calendar View (3-Day and 1-Day vertical timeline with timeline-tapping creation)
SCR-05: Smart Lists View (Aggregated Today with overdue triage modal, Tomorrow, Upcoming, All, Completed)
SCR-06: Habits View (Daily grid, dual modes, numeric target counters, celebratory particle feedback)
SCR-07: Habit Creation / Edit Sheet (Frequency, target counts, increment steps, scheduled days)
SCR-08: Side Navigation Drawer (Folders, lists, smart list visibility edit, trash, settings)
SCR-09: Settings Screen (Theme toggle, smart list management, default reminder times, IPC status)
SCR-10: Folder Management Dialog (Add/rename/reorder folders)
SCR-11: List Management Dialog (Add/rename/assign colors/reorder lists)
SCR-12: Trash Screen (Soft-deleted tasks with 30-day countdown, safe restore, and permanent purge)
SCR-13: Backup & Export Screen (Local JSON export and snapshot backup)
```

---

## 6. Key User Flows

### Flow 1: Create a Timed Task from Calendar View (SCR-04)
1. User navigates to Calendar View (3-day or 1-day timeline).
2. User taps an unoccupied time slot (e.g., Wednesday 4:00 PM).
3. The timeline immediately draws a placeholder ghost block snapped to the nearest 15-minute increment (4:00 PM – 5:00 PM).
4. Quick Task Creation Sheet (SCR-03) slides up from the bottom with start/end times pre-filled.
5. User enters title (e.g., *"INMO Combinatorics Sheet 3"*), selects list (*"Combinatorics"*), and confirms.
6. Block renders on timeline in `#43A047`.
7. **Phase 2 Trigger:** In Phase 2, FD dispatches an explicit native Android broadcast `com.flux.action.FOCUS_BLOCK_SYNC` with action `CREATE` to FluxFoxus.

### Flow 2: Daily Planning & Overdue Triage (SCR-01 & SCR-05)
1. User opens FD to the "Today" Smart List.
2. Unfinished tasks from prior days appear in an "Overdue" section.
3. Tapping `[ 📅 Reschedule Overdue ]` opens the dedicated `RescheduleOverdueSheet`.
4. User triages overdue tasks one-by-one (options: *Today*, *Tomorrow*, *Next Week*, *Pick Date*, *Won't Do / Trash*), or batch shifts all to Today.
5. Tasks are checked off throughout the day with tactile bounce and strike-through animation.

### Flow 3: Habit Check-In Flow (SCR-06)
1. User switches to the Habits tab.
2. Taps the circular progress card for *"Solve 12 IOQM Problems"* (target 12, increment step 3).
3. Count increments from `9/12` to `12/12` (strictly capped at 12).
4. Circular ring fills, tactile vibration triggers, confetti particles explode, and current streak increments.

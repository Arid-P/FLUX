# FluxDone (FD) — Product Requirements Document (PRD v2.0)

**Version:** 2.0 (Kotlin + Jetpack Compose Architecture)  
**Date:** March 2026 / v2 Overhaul  
**Target:** Android (Mobile) + Ubuntu Linux (Desktop)  
**Author:** Ari

---

## 1. Project Overview & Product Vision

### 1.1 What FluxDone Replaces
FluxDone (FD) is a high-performance, offline-first task and habit management application built to completely replace **TickTick** in Ari's academic workflow. It replicates TickTick's proven organizational patterns (lists, folders, sections, calendar time-blocking, recurrence, habits) while stripping out unused bloat (pomodoros, collaboration, file attachments) and optimizing specifically for intensive STEM study.

### 1.2 Core Success Definition
1. **Zero Regression from TickTick:** Every task and time-blocking workflow Ari performs in TickTick must be supported with zero friction.
2. **Calendar View as Primary Creation Surface:** Creating a timed task directly from the timeline grid must require minimal taps (tap slot → enter title → confirm).
3. **Rigid Academic Theming:** 7 domain colors for Olympiad and JEE preparation are locked and mirrored with Notewise notebooks.
4. **100% Offline-First:** Operates completely without internet using local SQLite storage.

---

## 2. Target User & Behavioral Patterns

### User Profile: Ari
- Preparing concurrently for Math Olympiad (IOQM/INMO/IMO), JEE Foundation (PW Neev Diamond), and School (ICSE).
- High task volume with rigid daily schedules. Requires clear visual separation between subjects.

### Key Behavioral Insights:
1. **Timeline Tapping:** Ari does not use the calendar view as a passive agenda viewer. He opens the 3-day or 1-day timeline, visually identifies open gaps between study blocks, taps the empty time slot, and schedules a task with explicit start and end times.
2. **Sections Inside Subject Lists:** Neev Diamond requires subdivision into Physics, Chemistry, and Mathematics sections.
3. **Habit Streaks:** Daily habits (problem-solving, revision) must have tactile check-offs and streak tracking.

---

## 3. Core Features Inventory

| Feature | Priority | Description |
| :--- | :--- | :--- |
| **Lists & Folders** | P0 (Must Have) | Nested hierarchy: Folders contain Lists; Lists contain Sections and Tasks. |
| **Sections inside Lists** | P0 (Must Have) | Collapsible section dividers within lists (e.g., Neev Diamond → Physics, Chem, Math). |
| **Calendar View** | P0 (Must Have) | 3-Day and 1-Day vertical timeline grids with drag-to-create time-blocks. |
| **Academic Color System** | P0 (Must Have) | 7 locked system hex colors for Olympiad & JEE domains. |
| **Smart Lists** | P0 (Must Have) | Dynamic auto-filtered lists: Today, Tomorrow, All, Completed, Trash. |
| **Recurring Tasks** | P0 (Must Have) | RRULE-based recurring schedules (daily, weekly on specific days, custom intervals). |
| **Subtasks & Notes** | P1 (Core) | Checkable subtask checklists and markdown notes inside tasks. |
| **Habit Tracker** | P1 (Core) | Daily habit check-offs with target counts and streak counters. |
| **FD ↔ FF IPC Broadcast**| P0 (Must Have) | Dispatches native Android broadcasts to FluxFoxus upon timed task creation/update/delete. |
| **Local Backup / Export**| P1 (Core) | JSON and SQLite database export/import. |
| **Cloud Sync & Web** | P2 (Phase 2) | Multi-device sync to Ubuntu Desktop and browser access via central DB. |

---

## 4. Academic Domain Color System (System-Locked)

The following 7 academic domain colors are system-locked to maintain identical visual consistency with Ari's Notewise notes:

```
[#2E7D32] Number Theory (Olympiad)
[#1565C0] Geometry (Olympiad)
[#43A047] Combinatorics (Olympiad)
[#FB8C00] Algebra P1 (Olympiad)
[#E64A19] Algebra P2 (Olympiad)
[#E53935] Tests & Mocks
[#576481] Neev Diamond (PW JEE Foundation)
```

*Personal lists (School, Programming, Books, Misc) use custom user-defined colors from a curated 16-color picker.*

---

## 5. Screen Inventory & UX Map

```
SCR-01: Main List View (Tasks, sections, quick check-off, filter pills)
SCR-02: Task Detail Sheet (Title, dates, times, recurrence, subtasks, notes, list selector)
SCR-03: Quick Task Creation Sheet (Bottom sheet for rapid entry)
SCR-04: Calendar View (3-Day and 1-Day vertical timeline with timeline-tapping creation)
SCR-05: Smart Lists View (Aggregated Today, Tomorrow, All views)
SCR-06: Habits View (Daily grid, streak flame counters, celebratory completion)
SCR-07: Habit Creation / Edit Sheet (Frequency, target counts, reminders)
SCR-08: Side Navigation Drawer (Folders, lists, academic domains, trash, settings)
SCR-09: Settings Screen (Theme toggle, default reminder times, IPC status)
SCR-10: Folder Management Dialog (Add/rename/reorder folders)
SCR-11: List Management Dialog (Add/rename/assign colors/reorder lists)
SCR-12: Trash Screen (Soft-deleted tasks with restore and permanent purge)
SCR-13: Backup & Export Screen (Local JSON export and Google Drive backup)
```

---

## 6. Key User Flows

### Flow 1: Create a Timed Task from Calendar View (SCR-04)
1. User navigates to Calendar View (3-day or 1-day timeline).
2. User taps an unoccupied time slot (e.g., Wednesday 4:00 PM).
3. The timeline immediately draws a placeholder block snapped to the nearest 15-minute increment (4:00 PM – 5:00 PM).
4. Quick Task Creation Sheet (SCR-03) slides up from the bottom with start/end times pre-filled.
5. User enters title (e.g., *"INMO Combinatorics Sheet 3"*), selects list (*"Combinatorics"*), and confirms.
6. Block renders on timeline in `#43A047`.
7. **Background Trigger:** FD dispatches an explicit native Android broadcast `com.flux.action.FOCUS_BLOCK_SYNC` with action `CREATE` to FluxFoxus.

### Flow 2: Daily Planning Flow (SCR-01 & SCR-05)
1. User opens FD to the "Today" Smart List.
2. Unfinished tasks from yesterday with due dates appear in an "Overdue" section.
3. User re-schedules overdue tasks to Today with a single swipe gesture.
4. Tasks are checked off throughout the day with immediate strike-through animation.

### Flow 3: Habit Check-In Flow (SCR-06)
1. User switches to the Habits tab.
2. Taps the circular progress ring for *"Solve 5 Olympiad Problems"*.
3. Count increments from `4/5` to `5/5`.
4. Ring turns green, tactile vibration triggers, and current streak increments.

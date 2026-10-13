# FluxDone (FD) — Master UI Specifications (SCR-01 through SCR-13)

**Version:** 2.0 (Native Jetpack Compose & Compose Multiplatform Screen Specifications)  
**Target:** Android Mobile (`com.fluxdone`) & Ubuntu Linux Desktop (`com.fluxdone.desktop`, JetBrains Compose)  
**Theme:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`

---

## Screen Inventory

| Screen ID | Title | Key Composable Components |
| :--- | :--- | :--- |
| **SCR-01** | Main Task List | Header, Curated Domain Badges, Sticky Section Expanders, Task Cards, Swipe Actions |
| **SCR-02** | Task Detail Sheet | Editable Title, Date/Time Controls, RRULE Recurrence Picker, Subtasks List, Live Markdown Notes |
| **SCR-03** | Quick Creation Sheet | Compact Bottom Sheet, Keyboard Auto-Focus, List Tag Pill, Date Pill, Confirm Button |
| **SCR-04** | Calendar View | 3-Day / 1-Day Viewport Switcher, Dynamic All-Day Strip, 24h Time Grid, Drag-to-Create Tap Canvas, Red "Now" Line |
| **SCR-05** | Smart Lists | Filtered Task Cards (Today with Overdue Triage Modal, Tomorrow, Upcoming, All, Completed with 4 Date Buckets) |
| **SCR-06** | Habits Grid | Daily Habit Progress Cards, Target Progress (`3/5`), Streak Counters, Confetti Particle Feedback |
| **SCR-07** | Habit Creation / Edit | Icon Grid Picker, 12-Swatch Palette Picker, Frequency Controls, Step Size Counter |
| **SCR-08** | Side Navigation Drawer | Smart Lists Section with Quick Edit `[ ✎ ]`, Folders & Lists Tree, Trash, Settings |
| **SCR-09** | Settings | Theme Mode Selector (System/Dark/Light), Smart List Visibility, Notification Defaults, IPC Diagnostics |
| **SCR-10** | Folder Management | Inline Dialog: Create, Rename, Reorder, Delete Folders |
| **SCR-11** | List Management | Inline Dialog: List Name, 12-Swatch & Hex Palette Picker, Sectioned Toggle, Folder Assignment, Delete |
| **SCR-12** | Trash | Soft-deleted task list with 30-day retention countdown, "Restore" (with Inbox fallback), and "Delete Permanently" |
| **SCR-13** | Backup & Export | JSON Export, Database Snapshot, Google Drive backup status |

---

## Detailed Screen Specifications

### SCR-01: Main Task List View
- **Top App Bar:** Current List / Smart List Title, Search icon, Multi-select action, Overflow menu.
- **Section Headers:** Sticky headers for sections (e.g. *"Physics"*, *"Chemistry"*, *"Math"* in Neev Diamond). Displays task count and collapsible arrow indicator.
- **Task Card Interaction:**
  - Checkbox: Circular/rounded checkbox on left. Tapping plays a subtle spring-bounce animation (`scale(0.85f) → scale(1.15f) → scale(1.0f)`), strikes through task title, and moves it to completed group after 400ms delay.
  - Priority indicator: Left border color or flag icon (High = Red `#E53935`, Medium = Orange `#FB8C00`, Low = Blue `#1565C0`, None = Transparent).
  - Domain Chip: Small colored capsule with list name if viewed from an aggregated list (e.g., Today).
  - Swipe Left: Reveals "Delete" (moves to Trash with 5s Undo snackbar).
  - Swipe Right: Quick reschedule to Tomorrow.

---

### SCR-02: Task Detail Sheet
- Accessible by tapping any task card in SCR-01 or timeline block in SCR-04.
- Rendered as an expandable full-height modal bottom sheet or dedicated route.
- **Header:** List selector dropdown with color dot; Priority flag picker (None, Low, Med, High).
- **Title & Notes:** Large unframed text field for Title; live markdown-rendered notes area with clickable web links.
- **Date & Time Controls:**
  - Due Date selector with quick buttons (*Today*, *Tomorrow*, *Next Week*).
  - Start Time & End Time pickers with duration auto-calculator.
- **Recurrence Picker:** Single line displaying current recurrence rule (*"Repeats: None"*). Tapping opens custom rule dialog (Daily, Weekdays, Weekly on Mon/Wed/Fri, Custom Interval).
- **Subtasks Checklist:**
  - Add subtask input field with enter-to-add flow.
  - Checkable subtask rows with automatic bidirectional parent completion synchronization.

---

### SCR-03: Quick Task Creation Sheet
- Invoked via Floating Action Button (+) or by tapping an empty slot on Calendar SCR-04.
- Compact modal sheet docked above the software keyboard.
- **Fields:**
  - Auto-focused single-line title input.
  - Due Date / Start Time pill.
  - List selection pill (defaults to current active list).
  - Blue/Primary submit arrow button.
- Submitting immediately inserts task into local Room database (and in Phase 2, dispatches IPC broadcast if start/end times are specified).

---

### SCR-04: Calendar View (Primary Academic Time-Blocking Surface)
- **Top Bar:** View switcher (*"3-Day"* / *"1-Day"*), Month-Year label, Mini-calendar date expander, Today quick-jump button.
- **Dynamic All-Day Strip:** Pinned above 24h timeline. Expands dynamically for date-only tasks (up to 3 visible chips) while shrinking scrollable timeline underneath to preserve screen height.
- **Timeline Canvas (Custom `@Composable`):**
  - Left column: 24-hour labels (12 AM to 11 PM) with 64.dp height per hour.
  - Grid background: Subtle horizontal hairline dividers every hour (`1.dp`) and light dashed lines every 30 minutes.
  - Current Time Line: Bold red horizontal line with a circular dot anchored at the current minute, auto-advancing every 60 seconds.
- **Task Time-Blocks:**
  - Rectangular cards positioned vertically by `(start_time - day_start) * 64.dp / 3600_000`.
  - Height determined by `duration_minutes * (64.dp / 60)`. Minimum rendered height is `24.dp`.
  - Colored using the parent list's curated color (e.g., Combinatorics `#43A047`).
  - Completed tasks rendered at 50% opacity with strike-through title.
- **Interaction & Creation Gesture:**
  - Tapping empty grid space immediately draws a ghost block and opens Quick Task Creation (SCR-03) pre-filled with the tapped 15-minute slot.
  - Dragging to reschedule snaps start time to 15-minute grid while **strictly preserving original duration**.

---

### SCR-05: Smart Lists View
- Aggregated views for rapid daily focus:
  - **Today:** Top "Overdue" section with alert pill and `[ 📅 Reschedule Overdue ]` button opening dedicated one-by-one triage modal (`RescheduleOverdueSheet`).
  - **Tomorrow & Upcoming:** Forward-looking study scheduling.
  - **All Active:** Global task inventory across all folders and lists.
  - **Completed:** Categorized into 4 exact chronological buckets: **Today**, **Yesterday**, **Previous 7 Days**, and **Older (Before 7 Days)**, equipped with a live search bar.
- Smart list visibility configurable via navigation drawer `[ ✎ ]` shortcut and Settings.

---

### SCR-06: Habits Tracker View
- **Header:** Weekly calendar strip (Mon–Sun) showing overall completion dots.
- **Habit Cards:**
  - Card with habit title, icon, and circular progress ring.
  - Dual Modes: 1-Tap Simple (`BINARY`) and Multi-Step Numeric (`NUMERIC` with custom increment step size).
  - Hard Ceiling Rule: Progress strictly caps at target count; no overshooting.
  - Short tap increments; long-press decrements floored at 0.
  - Streak badge: Flame icon with current consecutive days counter. Non-scheduled days are rest days that never break streaks.
  - Confetti particle explosion and tactile vibration upon hitting target count.

---

### SCR-08: Side Navigation Drawer
- **Smart Lists Header:** Quick edit button `[ ✎ ]` to toggle visibility and reorder smart lists.
- **Ari's Academic Hierarchy:**
  - System Folders:
    - **Olympiad Prep:** Number Theory (`#2E7D32`), Geometry (`#1565C0`), Combinatorics (`#43A047`), Algebra P1 (`#FB8C00`), Algebra P2 (`#E64A19`), Tests (`#E53935`).
    - **JEE Foundation:** Neev Diamond (`#576481` with sections for Physics, Chemistry, Math).
    - **Personal / Extra:** Programming, School, Books, Notes, Others.
- **Trash & Settings:** Pinned at bottom drawer footer.

---

### SCR-12: Trash Screen
- Soft-deleted items display a 30-day countdown badge (`X days left before permanent deletion`).
- **1-Tap Restore:** Re-activates item in its original list and section; safely falls back to default `Inbox` list if the parent list was permanently deleted.
- **Permanent Purge Safety Gates:** Confirmation dialog for individual hard deletes; red warning dialog for "Empty Trash".

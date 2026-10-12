# FluxDone (FD) — Master UI Specifications (SCR-01 through SCR-13)

**Version:** 2.0 (Jetpack Compose Screen Specifications)  
**Target:** Android & Compose Multiplatform (Ubuntu Desktop)  
**Theme:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`

---

## Screen Inventory

| Screen ID | Title | Key Composable Components |
| :--- | :--- | :--- |
| **SCR-01** | Main Task List | Header, Category Chips, Section Expanders, Task Cards, Swipe Actions |
| **SCR-02** | Task Detail Sheet | Editable Title, Date/Time Picker, Recurrence Selector, Subtasks List, Notes |
| **SCR-03** | Quick Creation Sheet | Compact Bottom Sheet, Keyboard Auto-Focus, List Tag Pill, Confirm Button |
| **SCR-04** | Calendar View | 3-Day / 1-Day Toggle, 24h Time Grid, Drag-to-Create Tap Canvas, Red "Now" Line |
| **SCR-05** | Smart Lists | Filtered Task Cards (Today, Tomorrow, All, Completed), Batch Reschedule |
| **SCR-06** | Habits Grid | Daily Habit Ring Cards, Target Progress (`3/5`), Streak Counters, Confetti Particle |
| **SCR-07** | Habit Creation / Edit | Icon Grid Picker, Color Picker, Frequency Controls, Target Counter |
| **SCR-08** | Side Navigation Drawer | Academic Subject List with locked hex indicators, Folder Trees, Trash, Settings |
| **SCR-09** | Settings | Theme Mode Selector (System/Dark/Light), Notification Defaults, IPC Diagnostics |
| **SCR-10** | Folder Management | Inline Dialog: Create, Rename, Reorder, Delete Folders |
| **SCR-11** | List Management | Inline Dialog: List Name, Color Palette Picker, Folder Assignment, Delete |
| **SCR-12** | Trash | Soft-deleted task list with "Restore" and "Delete Permanently" actions |
| **SCR-13** | Backup & Export | JSON Export, Database Snapshot, Google Drive backup status |

---

## Detailed Screen Specifications

### SCR-01: Main Task List View
- **Top App Bar:** Current List / Smart List Title, Search icon, Multi-select action, Overflow menu.
- **Section Headers:** Sticky headers for sections (e.g. *"Physics"*, *"Chemistry"*, *"Math"* in Neev Diamond). Displays task count and collapsible arrow indicator.
- **Task Card Interaction:**
  - Checkbox: Circular checkbox on left. Tapping plays a subtle scale-bounce animation, strikes through task title, and moves it to completed group after 400ms delay.
  - Priority indicator: Left border color or flag icon (High = Red, Medium = Orange, Low = Blue, None = Transparent).
  - Domain Chip: Small colored capsule with list name if viewed from an aggregated list (e.g., Today).
  - Swipe Left: Reveals "Delete" (moves to Trash).
  - Swipe Right: Quick reschedule to Tomorrow.

---

### SCR-02: Task Detail Sheet
- Accessible by tapping any task card in SCR-01 or timeline block in SCR-04.
- Rendered as an expandable full-height modal bottom sheet or dedicated route.
- **Header:** List selector dropdown with color dot; Priority flag picker (None, Low, Med, High).
- **Title & Notes:** Large unframed text field for Title; multi-line markdown-friendly notes area.
- **Date & Time Controls:**
  - Due Date selector with quick buttons (*Today*, *Tomorrow*, *Next Week*).
  - Start Time & End Time pickers with duration auto-calculator.
- **Recurrence Picker:** Single line displaying current recurrence rule (*"Repeats: None"*). Tapping opens custom rule dialog (Daily, Weekdays, Weekly on Mon/Wed/Fri, Custom Interval).
- **Subtasks Checklist:**
  - Add subtask input field with enter-to-add flow.
  - Checkable subtask rows with drag-handle for manual reordering.

---

### SCR-03: Quick Task Creation Sheet
- Invoked via Floating Action Button (+) or by tapping an empty slot on Calendar SCR-04.
- Compact modal sheet docked above the software keyboard.
- **Fields:**
  - Auto-focused single-line title input.
  - Due Date / Start Time pill.
  - List selection pill (defaults to current active list).
  - Blue/Primary submit arrow button.
- Submitting immediately inserts task into local Room database and dispatches IPC broadcast if times are specified.

---

### SCR-04: Calendar View (Primary Academic Time-Blocking Surface)
- **Top Bar:** View switcher (*"3-Day"* / *"Day"*), Month-Year label, Mini-calendar date expander, Today quick-jump button.
- **Timeline Canvas (Custom `@Composable`):**
  - Left column: 24-hour labels (12 AM to 11 PM) with 64.dp height per hour.
  - Grid background: Subtle horizontal hairline dividers every hour (`1.dp`) and light dashed lines every 30 minutes.
  - Current Time Line: Bold red horizontal line with a circular dot anchored at the current minute, auto-advancing every 60 seconds.
- **Task Time-Blocks:**
  - Rectangular cards positioned vertically by `(start_time - day_start) * 64.dp / 3600_000`.
  - Height determined by `duration_minutes * (64.dp / 60)`. Minimum rendered height is `24.dp`.
  - Colored using the parent list's locked domain color (e.g., Combinatorics `#43A047`).
  - Text: Task title (bold), duration caption (*"4:00 PM - 5:30 PM"*).
- **Interaction & Creation Gesture:**
  - Tapping empty grid space immediately opens Quick Task Creation (SCR-03) pre-filled with the tapped 15-minute slot.

---

### SCR-06: Habits Tracker View
- **Header:** Weekly calendar strip (Mon–Sun) showing overall completion dots.
- **Habit Cards:**
  - Card with habit title, icon, and circular progress ring.
  - Target Progress indicator (e.g., `3/5 Problems Done`).
  - Streak badge: Flame icon with current consecutive days counter.
  - Tapping card increments counter. When target is reached, plays a celebratory particle effect and haptic feedback.

---

### SCR-08: Side Navigation Drawer
- **Ari's Academic Hierarchy:**
  - System Folders:
    - **Olympiad Prep:** Number Theory (`#2E7D32`), Geometry (`#1565C0`), Combinatorics (`#43A047`), Algebra P1 (`#FB8C00`), Algebra P2 (`#E64A19`), Tests (`#E53935`).
    - **JEE Foundation:** Neev Diamond (`#576481` with sections for Physics, Chemistry, Math).
    - **Personal / Extra:** Programming, School, Books, Notes, Others.
- **Smart Filters:** Today, Tomorrow, Next 7 Days, All, Trash.
- **Settings & Backup:** Quick access links at the bottom drawer footer.

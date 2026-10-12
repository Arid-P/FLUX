# ui_SCR-04_calendar-view.md
## FluxDone — Calendar View
**Screen ID:** SCR-04  
**Version:** 1.0  
**Status:** Locked  
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-04 is the full-screen interactive timeline view of FluxDone. It is the primary task creation surface for timed tasks — the user's main workflow for scheduling blocks of study time. It renders all tasks with a defined start and end time as colored duration blocks on a two-axis scrollable grid (Y = time, X = date). It additionally renders Google Calendar events as read-only overlay blocks. The screen supports three view modes: Day, 3-day, and Week.

---

## 2. Navigation & Entry Points

- Accessed via the **Calendar** icon in the bottom navigation bar (portrait) or left navigation rail (landscape)
- No deep-link entry in Phase 1
- On entry, the screen restores the last used view mode (Day / 3-day / Week) and scrolls the time grid to the current time, centered vertically

---

## 3. Overall Layout Architecture

### 3.1 Portrait Layout

```
┌─────────────────────────────────────────────┐
│ Status Bar (System)                         │
├─────────────────────────────────────────────┤
│ Top App Bar (64dp)                          │
│ [Hamburger] [Month+Year] [Day|3Day|Week] [⋮]│
├─────────────────────────────────────────────┤
│ Date Header Row (56dp)                      │
│ [All-day row per column if tasks exist]     │
├────────┬────────────────────────────────────┤
│ Time   │ Scrollable Calendar Grid           │
│ Labels │ (horizontal + vertical scroll)     │
│ Column │                                    │
│ (64dp) │                         [FAB]      │
└────────┴────────────────────────────────────┘
│ Bottom Navigation Bar (56dp)                │
└─────────────────────────────────────────────┘
```

### 3.2 Landscape Layout

```
┌──────┬──────────────────────────────────────┐
│ Left │ Top App Bar                          │
│ Nav  ├──────────────────────────────────────┤
│ Rail │ Date Header Row                      │
│      ├───────┬──────────────────────────────┤
│      │ Time  │ Scrollable Calendar Grid     │
│      │ Labels│                              │
│      │       │                     [FAB]    │
└──────┴───────┴──────────────────────────────┘
```

---

## 4. Top App Bar

**Component:** `AppBar`  
**Height:** 64dp  
**Background color:** Surface color (theme-dependent — white in light mode, dark surface in dark mode)  
**Elevation:** 2dp with bottom divider

### 4.1 Elements (left to right)

| Position | Component | Type | Behavior |
|---|---|---|---|
| Left | Hamburger button | `IconButton` | Opens side drawer (SCR-08) |
| Left-center | Month + Year label | `Text` | Non-interactive. Updates as user scrolls horizontally. Example: "March 2026" |
| Center-right | View mode toggle | Segmented `TabBar` (3 tabs) | Switches between Day / 3-day / Week |
| Right | Overflow menu button | `IconButton` (3-dot vertical) | Opens overflow menu (see Section 4.3) |

### 4.2 View Mode Toggle (Segmented TabBar)

**Component:** Custom segmented control rendered as a `TabBar` with 3 tabs  
**Tabs:** `Day` | `3-day` | `Week`  
**Width:** Wraps content, centered in available space  
**Tab height:** 36dp  
**Tab padding:** 12dp horizontal per tab  
**Corner radius:** 8dp (pill-style selected indicator)

**States:**

| State | Text color | Indicator |
|---|---|---|
| Selected | Primary color (list-agnostic app primary) | Filled rounded rect background |
| Unselected | Secondary text color (muted) | None |

**Behavior:**
- Tapping a tab immediately switches the grid to that view mode
- The last used tab is persisted to `shared_preferences` and restored on next app launch
- Tab switch animates with a 200ms `easeInOut` crossfade on the grid content and a sliding indicator

### 4.3 Overflow Menu

**Component:** `PopupMenuButton`  
**Trigger:** 3-dot icon button (top-right)  
**Position:** Anchored below top-right corner of app bar  

**Menu items:**
- "Go to today" — scrolls grid to current date and time
- "Google Calendar sync" — triggers a manual refresh of Google Calendar overlay events (Phase 1: visible but disabled if not connected; shows "Connect in Settings")

---

## 5. Date Header Row

**Component:** Fixed non-scrolling horizontal row pinned below the top app bar  
**Height:** 56dp (expands by ~32dp per active all-day row if date-only tasks exist)  
**Background:** Same as top app bar surface color  
**Bottom border:** 1dp divider

### 5.1 Day Columns in Header

Each day column in the header aligns exactly with its corresponding column in the grid below.

**Column count per view mode:**
- Day: 1 column
- 3-day: 3 columns
- Week: 7 columns

**Per column content (top to bottom):**
1. Weekday abbreviation label — e.g., "Mon", "Tue" — 12sp, medium weight, secondary text color
2. Date number — e.g., "9", "10" — 14sp, bold weight

**Today's column:**
- Date number rendered inside a filled circle
- Circle diameter: 28dp
- Circle color: App primary color
- Date number color: White (#FFFFFF)

**Selected/active day (Day view only):**
- Same filled circle treatment as today, but using primary color at 70% opacity if not today

### 5.2 All-Day Task Row

Rendered as a fixed-height row directly below the weekday/date labels, inside the date header, when one or more date-only tasks exist for a visible day.

**Height per row:** 28dp  
**Multiple date-only tasks:** Stack vertically, each adding 28dp to the header height  
**Maximum visible rows:** 3 (overflow indicated by "+N more" chip)

**All-day task block:**
- Full width of the day column
- Background: List color at 80% opacity
- Corner radius: 4dp
- Text: Task title, 11sp, medium weight, white or dark depending on list color luminance
- No time range text (date-only)
- Horizontal padding: 4dp internal

---

## 6. Time Labels Column

**Component:** Fixed non-scrolling left column  
**Width:** 64dp  
**Background:** Same as screen background (no border)  
**Scrolls:** Vertically in sync with the calendar grid (Y-axis)

### 6.1 Time Label Items

- Labels shown for every hour: 12:00 AM, 1:00 AM, ... 11:00 PM
- **Typography:** 12sp, regular weight, secondary text color (#BDBDBD in light mode)
- **Alignment:** Right-aligned within the 64dp column, vertically centered on the hour line
- **Label position:** Top of each hour slot (label aligns with the hour divider line)

---

## 7. Calendar Grid

**Component:** Custom `InteractiveViewer` or `CustomScrollView` with a custom `RenderBox`  
**Scroll axes:**
- **Vertical:** Time axis. 12:00 AM (top) → 11:59 PM (bottom)
- **Horizontal:** Date axis. Scrolls continuously left (past) and right (future)

**On launch / view switch:** Grid auto-scrolls vertically to center the current time in the viewport.

### 7.1 Grid Dimensions

| Property | Value |
|---|---|
| Hour slot height | 60dp |
| Total grid height | 60dp × 24 = 1440dp |
| Day column width (Day view) | Full available width minus 64dp time label column |
| Day column width (3-day view) | (Full width − 64dp) ÷ 3 |
| Day column width (Week view) | (Full width − 64dp) ÷ 7 |

### 7.2 Grid Lines

**Horizontal hour lines:**
- 1dp height
- Color: Divider color (light mode: #E0E0E0, dark mode: #2C2C2C)
- Spans full width of grid (excluding time label column)
- Rendered at every hour boundary

**Horizontal half-hour lines:**
- 1dp height, dashed style (4dp dash, 4dp gap)
- Color: Divider color at 50% opacity
- Rendered at every 30-minute mark

**Vertical day column lines:**
- 1dp width
- Color: Divider color
- Renders between each day column

### 7.3 Current Time Indicator

**Rendered only on today's column.**

**Components:**
1. Horizontal red line spanning the full width of today's column
   - Height: 2dp
   - Color: #F44336 (Material Red 500)
2. Red filled circle at the left edge of the line (intersection with time label column border)
   - Diameter: 10dp
   - Color: #F44336

**Behavior:**
- Updates position in real time (re-renders every 60 seconds)
- Visible across all view modes when today is in the visible date range

---

## 8. Task Blocks

### 8.1 Block Appearance

**Component:** `Positioned` container inside the calendar grid's `Stack`

**Dimensions:**
- Width: Fills the day column width (or a fraction of it when overlapping — see Section 8.3)
- Height: Proportional to task duration. 60dp = 1 hour. Minimum height: 24dp (tasks shorter than 24 minutes still render at 24dp minimum)
- Top offset: Calculated from `start_time` relative to midnight of that day

**Visual style:**
- Background: List color (from `task_lists.color_hex`) at 90% opacity
- Corner radius: 6dp
- Left border accent: 3dp solid strip, list color at 100% opacity (same color, full opacity)
- No elevation / shadow

**Content (inside block, clipped to block bounds):**
1. Task title — 13sp, medium weight, white text (or dark if list color is light — determined by luminance threshold 0.4)
2. Time range — 11sp, regular weight, white at 80% opacity. Format: "9:00 PM – 11:00 PM"

**Content layout:**
- Title on top line
- Time range on second line
- Both left-aligned with 6dp left padding, 4dp top padding
- Content hidden if block height < 32dp (too small to render text legibly)

### 8.2 Task Block Interaction States

| State | Visual |
|---|---|
| Default | As described above |
| Pressed | Background darkens by 10% (overlay black at 10% opacity) |
| Dragging | Elevation raises to 8dp, shadow visible, opacity 95%, slight scale 1.02 |

**Tap behavior:** Opens SCR-02 (Task Detail Sheet) for that task.

**Drag behavior:** See Section 11 (Gestures — Drag to Reschedule).

### 8.3 Overlapping Task Blocks

When two or more tasks share overlapping time ranges on the same day, they render side by side (Google Calendar column layout).

**Rules:**
- Overlapping tasks divide the day column width equally
- 2 overlapping tasks: each gets 50% of column width
- 3 overlapping tasks: each gets 33% of column width
- Maximum rendered columns: 3. If more than 3 tasks overlap, the 3rd column shows a "+N" overflow chip instead of additional blocks
- Each column has 2dp horizontal gap between adjacent columns
- Overlap detection is recalculated on every data change

---

## 9. Google Calendar Overlay Blocks

**Component:** `Positioned` container, same grid stack as task blocks but rendered at lower Z-index (behind FD task blocks)

**Visual style:**
- Background: Transparent (no fill)
- Border: 1.5dp solid outline, color taken from Google Calendar event color if available, otherwise #9E9E9E (muted gray)
- Corner radius: 6dp
- Content: Event title only (no time range text inside block)
- Title: 12sp, regular weight, color matches border color

**Distinction from FD task blocks:**
- No filled background (outline only)
- No left border accent strip
- Muted color palette
- Slightly lower opacity: 80%

**Tap behavior:** Launches Google Calendar app via Android Intent, deep-linking to that specific event. If Google Calendar is not installed, opens the event URL in the system browser.

**Overlap with FD tasks:** FD task blocks always render on top of Google Calendar overlay blocks. Google Calendar blocks participate in overlap column layout only with other Google Calendar blocks, not with FD task blocks.

---

## 10. Tap-to-Create Interaction

### 10.1 Trigger
- **Single tap** on an empty grid cell (no task block or overlay block present at that position)
- **Long press** on an empty grid cell

### 10.2 Behavior

1. A **ghost block** (preview block) appears at the tapped position
   - Height: 60dp (1 hour default duration)
   - Background: App primary color at 30% opacity
   - Dashed border: 1.5dp, app primary color
   - Corner radius: 6dp
2. The ghost block has a **drag handle** at its bottom edge — a horizontal bar, 20dp wide, 4dp tall, centered, primary color at 60% opacity
3. The user can **drag the bottom edge** of the ghost block downward to extend the duration before confirming
   - Grid snaps to 15-minute intervals during drag
   - Time range label updates in real time inside the ghost block as the user drags
4. SCR-03 (Task Creation Sheet) opens simultaneously (or after drag release — see below)

### 10.3 Creation Sheet Timing

- **Single tap:** Task Creation Sheet opens immediately with start time pre-filled from tapped slot. Ghost block remains visible beneath the sheet.
- **Long press + drag:** Task Creation Sheet opens after the user releases the drag handle. Start time and end time are both pre-filled based on ghost block position and height.

### 10.4 Pre-filled Fields in Task Creation Sheet

| Field | Value |
|---|---|
| Date | Date of the tapped column |
| Start time | Time of the tapped slot (snapped to nearest 15 minutes) |
| End time | Start time + 1 hour (default) or start time + dragged duration |

---

## 11. Gestures

### 11.1 Vertical Scroll
- **Target:** Calendar grid
- **Action:** Scrolls time axis (Y). Reveals earlier or later hours.
- **Physics:** `BouncingScrollPhysics`
- **Scroll range:** 12:00 AM (top) to 11:59 PM (bottom)

### 11.2 Horizontal Scroll
- **Target:** Calendar grid and date header row (scroll in sync)
- **Action:** Moves date axis (X). Reveals past or future days.
- **Physics:** `PageScrollPhysics` in Day and 3-day view (snaps to day boundaries). `ClampingScrollPhysics` in Week view.
- **Infinite scroll:** Grid extends infinitely in both directions. Past limit: app launch date. Future limit: 1 year from today (configurable in Phase 2).

### 11.3 Tap on Empty Grid Cell
- **Action:** Opens tap-to-create flow (Section 10)

### 11.4 Long Press on Empty Grid Cell
- **Action:** Opens tap-to-create flow (Section 10) with ghost block immediately visible

### 11.5 Tap on Task Block
- **Action:** Opens SCR-02 (Task Detail Sheet) for that task

### 11.6 Tap on Google Calendar Overlay Block
- **Action:** Launches Google Calendar app via Intent to that event

### 11.7 Drag to Reschedule (Existing Task Block)

**Trigger:** Long press on an existing task block (not a Google Calendar overlay block). After 300ms hold, the block enters drag mode.

**Drag behavior:**
1. Block lifts (elevation 8dp, scale 1.02, opacity 95%)
2. User drags the block freely on the Y-axis (time) and X-axis (date column)
3. Block snaps to 15-minute intervals on Y-axis during drag
4. A **ghost outline** remains at the original position during drag (dashed border, original list color at 40% opacity) to indicate the original time slot
5. On release: block animates to snapped position (200ms `easeOut`), ghost disappears
6. Task `start_time`, `end_time`, and `task_date` are updated in the database immediately on release
7. If released on a different day column, `task_date` updates accordingly

**Duration resize drag:**
- Long press on the **bottom edge** of an existing task block (bottom 12dp zone)
- Drag downward to extend duration, upward to shorten
- Snaps to 15-minute intervals
- Minimum duration: 15 minutes
- On release: `end_time` updates in database

**Conflict handling:** No automatic conflict resolution in Phase 1. Overlapping after drag is allowed and renders using the overlap column layout.

### 11.8 Tap on FAB
- **Action:** Opens SCR-03 (Task Creation Sheet) without pre-filled time. Date defaults to currently visible date (center column in 3-day view, first visible day in week view).

---

## 12. Floating Action Button (FAB)

**Component:** `FloatingActionButton`  
**Size:** 56dp  
**Position:** Bottom-right corner of the calendar grid area  
**Margin:** 16dp from right edge, 16dp above bottom navigation bar  
**Icon:** Add (`Icons.add`), 24dp, white  
**Background color:** App primary color  
**Elevation:** 6dp default, 12dp pressed  
**Z-index:** Above all grid content, below bottom navigation bar

**Pressed state:**
- Background darkens by 10%
- Scale: 0.95 (120ms `easeInOut`)

**Behavior:** Opens SCR-03 (Task Creation Sheet). Date pre-filled with currently centered visible date. No time pre-filled.

---

## 13. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| View mode tab switch | Grid content crossfades, tab indicator slides | 200ms | `easeInOut` |
| Horizontal scroll (Day/3-day) | Page snap | 300ms | `easeOut` |
| Horizontal scroll (Week) | Free scroll, no snap | — | `ClampingScrollPhysics` |
| Ghost block appears (tap-to-create) | Fade in + scale from 0.95 to 1.0 | 150ms | `easeOut` |
| Ghost block drag (bottom edge) | Real-time resize, no animation | — | — |
| Task block drag lift | Elevation + scale increase | 200ms | `easeOut` |
| Task block drop | Slide to snapped position | 200ms | `easeOut` |
| Ghost outline disappears on drop | Fade out | 150ms | `easeIn` |
| FAB press | Scale 1.0 → 0.95 → 1.0 | 120ms | `easeInOut` |
| Task Creation Sheet open | Bottom sheet slides up | 300ms | Spring physics |

---

## 14. Empty States

### 14.1 Day View — No Tasks
- Grid renders normally with hour lines and current time indicator
- No illustration or empty state message overlaid on the grid
- FAB remains visible and actionable

### 14.2 No Google Calendar Connection
- Google Calendar overlay blocks simply absent
- No error state shown on the grid
- If the user has previously connected and the token has expired, a **snackbar** appears: "Google Calendar sync failed. Reconnect in Settings." with an "Open Settings" action button. Snackbar duration: 4 seconds.

### 14.3 Date with No Tasks (3-day / Week view)
- That day's column renders normally (grid lines, no blocks)
- No per-column empty state message

---

## 15. Elevation & Z-Index Layer Order (bottom to top)

| Layer | Z-index |
|---|---|
| Calendar grid background | 0 |
| Hour/day grid lines | 1 |
| Google Calendar overlay blocks | 2 |
| FD task blocks (default) | 3 |
| Ghost outline (drag origin) | 4 |
| Ghost block (tap-to-create preview) | 5 |
| FD task block (dragging state) | 6 |
| Current time indicator | 7 |
| FAB | 8 |
| Bottom navigation bar | 9 |
| Top app bar | 10 |

---

## 16. Accessibility

- All task blocks have semantic labels: "[Task title], [Start time] to [End time], [List name]"
- Google Calendar overlay blocks: "[Event title], Google Calendar event, [Start time] to [End time]"
- FAB: "Create new task"
- View mode tabs: "Day view", "3-day view", "Week view" — selected state announced
- Minimum touch target for all interactive elements: 48dp × 48dp
- Current time indicator is decorative — no semantic label required
- Drag-to-reschedule: accessible alternative via task detail edit (SCR-02) for users who cannot perform drag gestures

---

## 17. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Screen root | `Scaffold` |
| Top app bar | `AppBar` |
| View mode toggle | Custom `TabBar` with 3 tabs |
| Date header row | Custom `SliverPersistentHeader` or `PreferredSize` widget |
| Time label column | `CustomPaint` inside fixed-width `SizedBox` |
| Calendar grid | `CustomScrollView` + `CustomPainter` for grid lines + `Stack` for blocks |
| Task block | `Positioned` + `GestureDetector` inside `Stack` |
| Google Calendar block | `Positioned` + `GestureDetector` (read-only) inside `Stack` |
| Ghost block (tap-to-create) | `Positioned` + `AnimatedContainer` |
| Current time indicator | `CustomPaint` updated via `Timer.periodic` |
| FAB | `FloatingActionButton` |
| Bottom nav bar | `NavigationBar` (portrait) / `NavigationRail` (landscape) |
| Overflow menu | `PopupMenuButton` |

---

## 18. Data Requirements

| Data | Source | Notes |
|---|---|---|
| Tasks with start_time + end_time for visible date range | `ITaskRepository.getTasksByDateRange()` | Refreshed on date range change |
| Date-only tasks (no start/end time) for visible date range | `ITaskRepository.getTasksByDateRange()` | Rendered in all-day row |
| List color for each task | `IListRepository.getColorHexByListId()` | Cached in memory |
| Google Calendar events for visible date range | `ICalendarRepository.getEventsForRange()` | Cached per session, silent failure |
| Last used view mode | `shared_preferences` key: `calendar_last_view_mode` | Persisted across sessions |

---

## 19. Out of Scope for Phase 1

- Month view
- List view (within calendar — the TickTick "List" tab)
- Pinch-to-zoom on the time grid (hour slot height adjustment)
- Multi-day task blocks spanning across day column boundaries
- Drag-to-create (drag from empty space to set both start and end time in one gesture — only tap-to-create and bottom-edge drag are in scope)
- Google Calendar event editing from within FD

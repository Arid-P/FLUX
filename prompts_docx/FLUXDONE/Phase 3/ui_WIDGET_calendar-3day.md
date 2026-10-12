# ui_WIDGET_calendar-3day.md
## FluxDone — 3-Day Calendar View Home Screen Widget
**Widget ID:** WIDGET-02
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Widget Purpose

WIDGET-02 is the secondary FluxDone Android home screen widget. It provides a glanceable static 3-day calendar grid showing timed tasks as colored duration blocks across today, tomorrow, and the day after. It is a read-oriented widget — its primary interactions are navigation into SCR-04 (Calendar View) rather than inline task manipulation. It complements WIDGET-01 (Task View) by giving the user a time-axis perspective of their schedule directly from the home screen.

---

## 2. Widget Identity

| Property | Value |
|---|---|
| Widget ID | WIDGET-02 |
| File | `ui_WIDGET_calendar-3day.md` |
| Android widget class name (TBD at implementation) | `CalendarThreeDayWidget` |
| Flutter package | `home_widget` |
| Phase | Phase 3 |
| Platform | Android only |

---

## 3. Overall Layout Architecture

### 3.1 Layout Zones (Top → Bottom)

```
┌─────────────────────────────────────────────────────┐
│ HEADER BAR                                          │
│ [<]  [Mar]  [>]                          [⋮]        │
├─────────────────────────────────────────────────────┤
│ ALL-DAY EVENT ROW (visible only if events exist)    │
│ [ All-day event block ] [ ] [ ]                     │
├──────────┬──────────────┬──────────────┬────────────┤
│ TIME     │ TODAY        │ TOMORROW     │ DAY+2      │
│ LABELS   │ Mon 21       │ Tue 22       │ Wed 23     │
│          ├──────────────┼──────────────┼────────────┤
│  1 PM  ──│              │ ┌──────────┐ │            │
│          │ ┌──────────┐ │ │ Task     │ │            │
│  3 PM  ──│ │ Task     │ │ └──────────┘ │ ┌────────┐ │
│          │ └──────────┘ │              │ │ Task   │ │
│  5 PM  ──│              │ ┌──────────┐ │ └────────┘ │
│          │              │ │ Task     │ │            │
│  7 PM  ──│              │ └──────────┘ │            │
│          │   ─────────── (current time line)        │
│  9 PM  ──│              │              │            │
└──────────┴──────────────┴──────────────┴────────────┘
```

### 3.2 Layout Rules

- Time labels column is fixed-width on the left (~40–48dp)
- Three day columns are equal width, each occupying ~33% of the remaining horizontal space
- All-day event row renders above the time grid only when at least one all-day event exists within the visible 3-day window; hidden entirely otherwise
- Time grid is a static snapshot — no scrolling occurs inside the widget
- Content is clipped at widget edges with no overflow indicators
- Faint vertical separators between day columns

---

## 4. Widget Sizes

### 4.1 Supported Sizes

| Size | Grid cells | Description |
|---|---|---|
| Default | ~4×3 | Standard placement size |
| Minimum | ~4×2 | Header + day column headers + limited time grid visible |
| Maximum | Full screen | Full time window visible with all task blocks |

### 4.2 Resize Behaviour

- Widget is freely resizable via Android's standard long-press resize handles (corner and edge drag)
- Resize is linear progressive reveal: increasing height reveals more of the time window; decreasing height clips the bottom of the grid
- Font size does not scale with widget size — typography remains constant at all sizes
- Header bar and day column headers remain visible at all sizes including minimum
- The time grid clips at the bottom edge — no scroll indicator or overflow label is shown

---

## 5. Header Bar

**Height:** ~48dp
**Background:** Same as widget body background (no visual separation, no divider beneath)

### 5.1 Elements (left to right)

| Position | Element | Detail |
|---|---|---|
| Left | Left arrow (<) | Navigation — shifts 3-day window 3 days into the past |
| Left-center | Month label | Text showing current month name. Example: "Mar". Tappable. |
| Right-center | Right arrow (>) | Navigation — shifts 3-day window 3 days into the future |
| Right | Overflow menu (⋮) | Vertical 3-dot icon. Contents TBD at implementation. |

### 5.2 Typography — Month Label

- Font size: medium
- Font weight: medium/semi-bold
- Color: `ThemeTokens.widgetOnSurface`

### 5.3 Icon Colors

- All header icons (arrows + ⋮): `ThemeTokens.widgetOnSurface`
- Icon size: ~16–20dp for arrows, ~20–24dp for ⋮

### 5.4 Header Interactions

| Element | Tap Action |
|---|---|
| Month label ("Mar") | Opens FD and navigates to SCR-04 (Calendar View) |
| Left arrow (<) | Shifts the 3-day window 3 days into the past. Widget re-renders with new date range. Today indicator updates accordingly. |
| Right arrow (>) | Shifts the 3-day window 3 days into the future. Widget re-renders with new date range. |
| ⋮ icon | Opens overflow menu (contents TBD at implementation) |

### 5.5 Navigation State Persistence

- The current 3-day window offset (how many days shifted from today) is stored in `shared_preferences` keyed by widget instance ID
- Key: `widget_[instanceId]_calendar_offset` (integer, days from today's default window)
- Default offset: 0 (today as left column)
- Offset persists across widget updates and app restarts

---

## 6. Day Column Headers

**Height:** ~40–48dp
**Background:** Same as widget body (no visual separation from time grid)
**Bottom border:** Faint 1dp divider separating column headers from time grid

### 6.1 Per Column Content (top to bottom)

| Element | Detail |
|---|---|
| Date number | e.g. "21". Larger font. Medium weight. |
| Day label | e.g. "Fri". Smaller font. Regular weight. Muted color. |

### 6.2 Typography

| Element | Size | Weight | Color |
|---|---|---|---|
| Date number | Medium-large (~16sp) | Medium | `ThemeTokens.widgetOnSurface` |
| Day label | Small (~12sp) | Regular | `ThemeTokens.widgetOnSurfaceSecondary` |

### 6.3 Today Indicator

- The column corresponding to today's date receives a visual emphasis treatment
- Implementation: date number rendered with slightly higher contrast or brightness relative to other columns
- No circle, underline, or background fill — emphasis is subtle
- When the 3-day window is navigated away from today (offset ≠ 0), no today indicator is shown — all columns render at equal emphasis

### 6.4 Column Widths

- All three columns are equal width
- Each column = (total widget width − time label column width) ÷ 3
- Faint vertical separator lines (~1dp, `ThemeTokens.widgetDivider`) between columns

---

## 7. All-Day Event Row

**Visibility:** Renders above the time grid only when at least one all-day (date-only) task or event exists within the visible 3-day window. Hidden entirely when no all-day events exist.

**Height:** ~24–28dp per row. Multiple all-day events in the same day column stack vertically, each adding ~24–28dp to the row height.

### 7.1 All-Day Event Block

| Property | Value |
|---|---|
| Width | Full width of the corresponding day column |
| Background | `ThemeTokens.widgetAllDayEventSurface` (light grey / muted tone) |
| Corner radius | ~4dp |
| Text | Task/event title. Small font (~11sp). Truncated if it exceeds column width. |
| Text color | `ThemeTokens.widgetAllDayEventOnSurface` (dark text for contrast on light background) |
| Internal padding | ~4dp horizontal |

### 7.2 Overflow

- Maximum 2 all-day rows visible per column before overflow
- If more than 2 all-day events exist for a column: show "+N more" label in small muted text below the second row
- Tapping "+N more" opens SCR-04 at that date

---

## 8. Time Grid

### 8.1 Time Labels Column

| Property | Value |
|---|---|
| Width | ~40–48dp |
| Position | Fixed left column, does not scroll |
| Labels | Every 2 hours within the visible time window (e.g. 1 PM, 3 PM, 5 PM, 7 PM, 9 PM, 11 PM) |
| Font size | Small (~11–12sp) |
| Font weight | Regular |
| Color | `ThemeTokens.widgetOnSurfaceMuted` |
| Alignment | Right-aligned within column, vertically centered on hour line |

### 8.2 Horizontal Grid Lines

- Present at every hour across the full width of the 3 day columns
- Style: 1dp, low-opacity
- Color: `ThemeTokens.widgetGridLine`
- Even vertical spacing per hour slot

### 8.3 Visible Time Window

- User-configurable in widget settings (see Section 11)
- Defined by a start time and end time (e.g. 8 AM – 10 PM)
- The widget renders only the portion of the day between the configured start and end times
- Tasks or events outside the configured time window are not rendered in the grid (they may appear in the all-day row if they are date-only tasks)
- The time grid is a static snapshot — it does not scroll

### 8.4 Hour Slot Height

- Determined by: (available widget body height − header height − day column header height − all-day row height) ÷ number of hours in visible window
- Scales with widget height during resize — more height = taller hour slots = more readable task blocks

---

## 9. Current Time Indicator

### 9.1 Visibility Rule

The current time indicator renders **only** when both conditions are met:
1. Today's date is within the currently visible 3-day window (i.e. offset = 0 or today falls in the window)
2. The current time falls within the configured visible time window (between start and end time in widget settings)

If either condition is false, no indicator is shown.

### 9.2 Visual Specification

| Property | Value |
|---|---|
| Style | Solid horizontal line spanning the full width of today's day column only |
| Color | `ThemeTokens.widgetCurrentTimeLine` |
| Thickness | 1–2dp |
| Left edge | Small filled circle (~6dp diameter, same color) anchored at the left edge of today's column |
| Position | Calculated from current time within the visible time window |

### 9.3 Update

- The current time indicator position updates each time the widget refreshes (periodic or event-triggered)
- It does not animate or move in real time between updates

---

## 10. Task Blocks

### 10.1 General Rendering

| Property | Value |
|---|---|
| Shape | Rounded rectangle |
| Corner radius | ~6–8dp |
| Fill | List color (user-defined hex or system academic domain color) |
| Position | Placed within the corresponding day column at the vertical position matching the task's start time |
| Height | Proportional to task duration within the visible time window |
| Width | Full width of the day column minus internal column padding (~4dp each side) |

### 10.2 Block Content

**Normal block (sufficient height for 2 lines):**
- Line 1: Task title — small-medium font (~12sp), regular weight, white text (`ThemeTokens.widgetOnTaskBlock`)
- Line 2: Time range — small font (~11sp), regular weight, white text at reduced opacity

**Short block (insufficient height for 2 lines — e.g. 30-minute task):**
- Line 1: Task title only — small font, clipped/truncated if needed
- Time range is omitted

**Minimum visible block height:** ~16dp. Blocks shorter than this are rendered at minimum height and show no text.

### 10.3 Text Overflow

- Task title truncates with ellipsis if it exceeds the block width
- No wrapping — single line for title, single line for time range

### 10.4 Overlapping Blocks

- Overlapping tasks in the same day column are rendered side by side, each taking an equal fraction of the column width
- Consistent with SCR-04 overlap column layout

### 10.5 Task Block Interaction

| Tap Target | Action |
|---|---|
| Tap on any task block | Opens FD and navigates to SCR-04 (Calendar View), scrolled to the date and time of the tapped task, with the task visually highlighted |

### 10.6 List Color Dots

- No list color dot is shown on task blocks — the block background color already communicates the list identity
- Academic domain colors and user-defined list colors are applied directly as the block fill

---

## 11. Empty Time Slot Interaction

| Tap Target | Action |
|---|---|
| Tap on any empty grid cell | Opens FD and navigates to SCR-04 (Calendar View) at the tapped date and approximate time slot |

The tapped time slot is passed as a deep-link parameter to SCR-04 so the calendar scrolls to the correct position.

---

## 12. States

### 12.1 Populated State

All visible sections render with day column headers, time grid, task blocks, and all-day event row (if applicable) as described in Sections 6–10.

### 12.2 Empty State

Triggered when: the visible 3-day window and visible time window contain zero timed tasks and zero all-day events.

**Display:**
- Header bar renders normally (arrows, month label, ⋮)
- Day column headers render normally
- Time grid renders with hour lines and time labels
- No task blocks shown
- No empty state message — grid lines only

### 12.3 Partially Empty State

When some day columns have tasks and others do not:
- Columns with tasks render task blocks normally
- Columns without tasks render grid lines only — no per-column empty message

### 12.4 Loading State

Not shown to the user. Widget renders last cached data immediately. Background update is silent.

### 12.5 Error State

Not shown. Silent failure. Last cached data persists. No error UI within the widget.

---

## 13. Update Mechanism

### 13.1 Periodic Update

- **Frequency:** Every 15 minutes
- **Implementation:** Android `WorkManager` with `PeriodicWorkRequest`
- **Constraint:** No network required (all data is local SQLite)

### 13.2 Event-Triggered Update

The widget triggers an immediate refresh when any of the following FD data writes occur:

| Trigger Event | Description |
|---|---|
| Task created | New timed task added that falls within the widget's visible date + time window |
| Task updated | Due date, time, title, or list changed for a visible task |
| Task deleted | A visible task is deleted |
| Task completed | Any task completed within FD app |
| Navigation arrow tapped | User shifts the 3-day window — widget re-renders with new date range |

**Implementation:** FD app calls `HomeWidget.updateWidget()` after each qualifying data write.

### 13.3 Stale Data Behaviour

Widget renders last cached data silently between updates. No staleness indicator is shown.

---

## 14. Widget Settings Screen

The widget has a dedicated settings screen accessible via Android's widget long-press → "Widget settings".

### 14.1 Settings Options

| Setting | Type | Default | Description |
|---|---|---|---|
| Start time | Time picker | 8:00 AM | The earliest hour shown in the time grid |
| End time | Time picker | 10:00 PM | The latest hour shown in the time grid. Must be after start time. |
| Theme | Single select | System | Options: Light / Dark / System (follows device theme) |
| Background opacity | Slider | 90% | Range: 0% (fully transparent) to 100% (fully opaque) |

### 14.2 Validation

- End time must be at least 1 hour after start time — enforced in settings UI
- Minimum visible window: 1 hour. Maximum: 24 hours (midnight to midnight).

### 14.3 Settings Persistence

All settings are stored in `shared_preferences` keyed by widget instance ID to support multiple independent widget instances.

| Key | Type |
|---|---|
| `widget_[instanceId]_start_time` | Integer (hour, 0–23) |
| `widget_[instanceId]_end_time` | Integer (hour, 0–23) |
| `widget_[instanceId]_theme` | String: "light" / "dark" / "system" |
| `widget_[instanceId]_opacity` | Double (0.0–1.0) |
| `widget_[instanceId]_calendar_offset` | Integer (days offset from default window) |

---

## 15. Data Sources

| Data | Source | Query |
|---|---|---|
| Timed tasks for visible 3-day window | `ITaskRepository` | `getTasksByDateRange(startDate, endDate)` filtered to tasks with `start_time` and `end_time` set |
| All-day (date-only) tasks for visible 3-day window | `ITaskRepository` | `getTasksByDateRange(startDate, endDate)` filtered to tasks without `start_time` |
| List color for each task | `IListRepository` | `getListById(task.listId)` |
| Widget calendar offset | `shared_preferences` | `widget_[instanceId]_calendar_offset` |
| Widget time window settings | `shared_preferences` | `widget_[instanceId]_start_time`, `widget_[instanceId]_end_time` |
| Widget theme preference | `shared_preferences` | `widget_[instanceId]_theme` |
| Widget opacity preference | `shared_preferences` | `widget_[instanceId]_opacity` |

---

## 16. Theme & Color Tokens

All colors are sourced exclusively from `ThemeTokens`. No hardcoded hex values anywhere in widget implementation.

### 16.1 Token Reference Table

| Token Name | Usage | Light Mode | Dark Mode |
|---|---|---|---|
| `ThemeTokens.widgetSurface` | Widget background | Light surface | Dark surface |
| `ThemeTokens.widgetOnSurface` | Header text, date numbers, primary text | Dark | White |
| `ThemeTokens.widgetOnSurfaceSecondary` | Day labels | Medium grey | Light grey |
| `ThemeTokens.widgetOnSurfaceMuted` | Time labels | Muted grey | Muted grey |
| `ThemeTokens.widgetDivider` | Column separators, header bottom border | Light grey | Dark grey |
| `ThemeTokens.widgetGridLine` | Horizontal hour lines | Very light grey | Very dark grey |
| `ThemeTokens.widgetCurrentTimeLine` | Current time indicator line + dot | App primary color | App primary color |
| `ThemeTokens.widgetAllDayEventSurface` | All-day event block background | Light grey | Dark muted surface |
| `ThemeTokens.widgetAllDayEventOnSurface` | All-day event block text | Dark | Light |
| `ThemeTokens.widgetOnTaskBlock` | Text inside timed task blocks | White | White |
| `ThemeTokens.widgetIconPrimary` | Header icons (arrows + ⋮) | Dark | White |

### 16.2 Task Block Colors

Task block fill colors use the list's user-defined hex color or the system-locked academic domain color. These are raw color values from the data layer, not ThemeTokens — they are user data.

### 16.3 Theme Selection Logic

| Widget Setting | Behaviour |
|---|---|
| Light | Always uses light theme tokens |
| Dark | Always uses dark theme tokens |
| System | Reads `UiModeManager` and applies light or dark tokens accordingly |

### 16.4 Background Opacity

`ThemeTokens.widgetSurface` is applied at the user-configured opacity value using `Color.withOpacity()`. At values below 100%, the device wallpaper is visible through the widget background.

---

## 17. Flutter & Android Implementation Notes

### 17.1 Package

- **`home_widget`** Flutter package is used for all home screen widget implementation
- Widget UI is rendered as Android `RemoteViews` (XML-based layout)
- All tap interactions are handled via `PendingIntent` through the `home_widget` callback mechanism

### 17.2 Component Mapping

| UI Element | Android Equivalent |
|---|---|
| Widget root container | `RemoteViews` with rounded rect background drawable |
| Header bar | `LinearLayout` (horizontal) |
| Month label | `TextView` |
| Navigation arrows | `ImageView` (left/right) |
| Overflow icon | `ImageView` |
| All-day event row | `LinearLayout` (horizontal, conditionally visible) |
| All-day event block | `TextView` with colored background drawable |
| Day column headers | `LinearLayout` (horizontal, 3 equal children) |
| Date number | `TextView` |
| Day label | `TextView` |
| Time grid | Custom `RemoteViews` layout using absolute positioning or `RelativeLayout` |
| Time label | `TextView` (repeated per hour) |
| Hour grid lines | `View` elements with 1dp height |
| Task block | `TextView` with colored background drawable, absolutely positioned |
| Current time indicator | `View` with circle drawable at left edge |

### 17.3 Static Snapshot Rendering

Since the widget is a static snapshot (not scrollable), the entire time grid is pre-rendered server-side as a bitmap or composed `RemoteViews` layout before being pushed to the widget. All task block positions and heights are calculated in Dart before rendering.

### 17.4 Time-to-Pixel Calculation

```
hourSlotHeight = availableGridHeight / numberOfHoursInWindow
taskTopOffset = (task.startHour - windowStartHour) * hourSlotHeight
               + (task.startMinute / 60) * hourSlotHeight
taskHeight = max(minBlockHeight,
             (task.durationMinutes / 60) * hourSlotHeight)
```

### 17.5 Deep-Link Parameters for SCR-04

When opening SCR-04 from the widget, the following parameters are passed via deep-link URI:

| Scenario | Parameters |
|---|---|
| Tap on task block | `taskId=[id]`, `date=[yyyy-MM-dd]`, `time=[HH:mm]` |
| Tap on empty time slot | `date=[yyyy-MM-dd]`, `time=[HH:mm]` |
| Tap on month label | No parameters — SCR-04 opens at current date |

### 17.6 Minimum Touch Targets

All interactive elements must meet the Android minimum touch target of **48dp × 48dp**. Invisible padding zones are added around small icons and narrow task blocks as needed.

---

## 18. Accessibility

- Day column headers: semantic label = "[day name], [date], [month]" (e.g. "Friday, 21 March")
- Today's column: semantic label appends ", today"
- Task blocks: semantic label = "[task title], [start time] to [end time], [list name]"
- All-day event blocks: semantic label = "[event title], all day"
- Empty time slots: semantic label = "[day name] [time slot], no tasks"
- Left arrow: semantic label = "Previous 3 days"
- Right arrow: semantic label = "Next 3 days"
- Month label: semantic label = "Open calendar view"
- Overflow icon: semantic label = "More options"
- Current time indicator: decorative — no semantic label required
- Minimum touch targets: 48dp × 48dp for all interactive elements

---

## 19. Out of Scope for Phase 3

- 1-day or 7-day column variants (3-day only)
- Scrollable time grid inside widget
- Inline task creation from widget (tapping empty slot navigates to SCR-04, not inline creation)
- Inline task completion (this widget is read-oriented — use WIDGET-01 for completion)
- Google Calendar overlay events (Phase 3 widget shows FD tasks only; Google Calendar events visible in SCR-04 only)
- Drag-to-reschedule inside widget
- iOS widgets (Android only)
- Pinch-to-zoom on widget time grid

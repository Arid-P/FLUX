# ui_WIDGET_task-view.md
## FluxDone — Task View Home Screen Widget
**Widget ID:** WIDGET-01
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Widget Purpose

WIDGET-01 is the primary FluxDone Android home screen widget. It provides a glanceable, scrollable view of the user's task workload for the current day — showing Overdue, Today, and optionally Tomorrow tasks — directly on the Android home screen. It supports inline task completion, task detail navigation, and task creation without opening the full app. It is the most important widget in the FD widget family.

---

## 2. Widget Identity

| Property | Value |
|---|---|
| Widget ID | WIDGET-01 |
| File | `ui_WIDGET_task-view.md` |
| Android widget class name (TBD at implementation) | `TaskViewWidget` |
| Flutter package | `home_widget` |
| Phase | Phase 3 |
| Platform | Android only |

---

## 3. Overall Layout Architecture

### 3.1 Layout Zones (Top → Bottom)

```
┌─────────────────────────────────────────────┐
│ HEADER BAR                                  │
│ [ListName ▼]              [+]  [⋮]          │
├─────────────────────────────────────────────┤
│ SECTION LABEL — OVERDUE (if non-empty)      │
│ ┌─────────────────────────────────────────┐ │
│ │ Task Row                                │ │
│ │ Task Row                                │ │
│ └─────────────────────────────────────────┘ │
├─────────────────────────────────────────────┤
│ SECTION LABEL — TODAY (if non-empty)        │
│ ┌─────────────────────────────────────────┐ │
│ │ Task Row                                │ │
│ │ Task Row                                │ │
│ └─────────────────────────────────────────┘ │
├─────────────────────────────────────────────┤
│ SECTION LABEL — TOMORROW (if enabled        │
│ in widget settings and non-empty)           │
│ ┌─────────────────────────────────────────┐ │
│ │ Task Row                                │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ [Content cut off at widget bottom edge]     │
└─────────────────────────────────────────────┘
```

### 3.2 Layout Rules

- No footer bar
- Content fills available height; overflow is cut off at the widget bottom edge with no fade, no scroll indicator, and no "X more" label
- Sections that are empty are hidden entirely — their section label does not render
- Section order is fixed: Overdue → Today → Tomorrow

---

## 4. Widget Sizes

### 4.1 Supported Sizes

| Size | Grid cells | Description |
|---|---|---|
| Default | ~4×3 | Standard placement size |
| Minimum | 4×2 | Header + ~1–2 task rows visible |
| Maximum | Full screen | All tasks across all sections visible |

### 4.2 Resize Behaviour

- Widget is **freely resizable** via Android's standard long-press resize handles (corner and edge drag)
- Resize is **linear progressive reveal**: increasing height reveals more task rows; decreasing height hides rows from the bottom
- Font size does **not** scale with widget size — typography remains constant at all sizes
- Header bar remains visible at all sizes including minimum
- Layout structure does **not** change at any size — no layout breakpoints, no component removal at small sizes
- At minimum size (4×2), the header bar is always visible and at least 1–2 task rows are visible beneath it

---

## 5. Header Bar

**Height:** ~48dp
**Background:** Same as widget body background (no visual separation, no divider beneath)

### 5.1 Elements (left to right)

| Position | Element | Detail |
|---|---|---|
| Left | List title with caret | Text label showing current filter scope. "Today ▼" when global view; "ListName ▼" when filtered to a specific list. Caret (▼) indicates tappability. |
| Right (secondary) | Plus icon (+) | Thin stroke icon, ~20–24dp |
| Right (primary) | Overflow menu (⋮) | Vertical 3-dot icon, ~20–24dp |

### 5.2 Typography — List Title

- Font size: medium (matches task title size)
- Font weight: semi-bold
- Color: `ThemeTokens.widgetOnSurface` (primary text color)

### 5.3 Icon Colors

- Both + and ⋮ icons: `ThemeTokens.widgetOnSurface`

### 5.4 Header Interactions

| Element | Tap Action |
|---|---|
| "ListName ▼" title | Opens list picker bottom sheet (see Section 9.1) |
| + icon | Opens FD task creation screen with today's date pre-filled |
| ⋮ icon | Opens overflow menu (contents TBD at implementation) |

---

## 6. Section Labels

**Applicable sections:** OVERDUE, TODAY, TOMORROW

### 6.1 Visual Specification

- Text: ALL CAPS
- Font size: small (~11–12sp)
- Font weight: regular
- Color: `ThemeTokens.widgetOnSurfaceMuted` (muted secondary text color)
- Padding top: ~12–16dp above section label (to separate from previous section)
- Padding bottom: ~4–8dp below section label (before first task row)
- Padding left: ~12–16dp (aligns with task content left edge)

### 6.2 Visibility Rule

A section label renders **only** if its corresponding section contains at least one task. If the section is empty, the section label and all its rows are hidden entirely. No placeholder text is shown within empty sections.

---

## 7. Task Row

The task card in the widget is **identical to the in-app task card**. All fields, layout, colors, typography, and metadata indicators that appear on a task card inside FD also appear in the widget task row.

### 7.1 Row Structure (left to right)

| Element | Detail |
|---|---|
| Checkbox | Rounded square. Stroke color: `ThemeTokens.widgetCheckboxStroke` (yellow/orange). Fill: transparent when unchecked. Size: ~18–20dp. |
| Task content block | Takes remaining horizontal space. Contains title, optional subtitle/description, and time metadata. |
| Right metadata | List color dot (circle, ~8–10dp) + list name label (small grey text) |

### 7.2 Task Content Block (vertical stack)

| Element | Typography | Color |
|---|---|---|
| Task title | Medium size, regular weight | `ThemeTokens.widgetOnSurface` |
| Subtitle / description (if present) | Small size, regular/light weight | `ThemeTokens.widgetOnSurfaceSecondary` |
| Time metadata | Small size, regular weight | Varies by section (see below) |

### 7.3 Time Metadata Color Rules

| Context | Color Token |
|---|---|
| Overdue tasks | `ThemeTokens.widgetTimeOverdue` (red) |
| Today tasks | `ThemeTokens.widgetTimeToday` (orange) |
| Tomorrow tasks | `ThemeTokens.widgetTimeTomorrow` (neutral/secondary) |

Time format example: "Mar 12, 4:00PM – 6:30PM" / "Today, 12:00PM – 2:30PM"

### 7.4 Row Dimensions

| Property | Value |
|---|---|
| Row height | ~56–64dp |
| Left padding (to checkbox) | ~12–16dp |
| Checkbox to content gap | ~12dp |
| Right padding | ~12–16dp |
| Between-row vertical spacing | ~8–12dp |
| Between-section vertical spacing | ~12–16dp |

### 7.5 Dividers

No divider lines between task rows. Separation is achieved via vertical spacing only.

### 7.6 Completed Task Appearance

- When a task is marked complete via the widget checkbox:
  - The task moves to the **bottom of its section**
  - No strikethrough styling is applied in the widget
  - A system toast is shown: "Task completed" with a check icon (standard Android toast)
- Completed tasks that were already at bottom before interaction remain there

---

## 8. States

### 8.1 Populated State

All visible sections render with their section labels and task rows as described in Sections 6 and 7.

### 8.2 Empty State

Triggered when: Overdue section is empty AND Today section is empty AND (Tomorrow section is either disabled or empty).

**Display:**
- Header bar renders normally (list title, +, ⋮)
- No section labels shown
- Center of widget body shows:
  - Text: "No tasks today"
  - Typography: medium size, regular weight
  - Color: `ThemeTokens.widgetOnSurfaceMuted`
  - Alignment: horizontally and vertically centered within the body area

### 8.3 Loading State

Not shown to the user. Widget renders last cached data immediately. Background update is silent. If no cached data exists on first placement, the empty state is shown.

### 8.4 Error State

Not shown. Silent failure. Last cached data persists. No error UI within the widget.

---

## 9. Interactions

### 9.1 List Picker (Header Title Tap)

**Trigger:** Tap on "ListName ▼" or "Today ▼" in the header
**Result:** Opens a bottom sheet inside FD titled "Choose List"
**Bottom sheet options:**
- "All Lists" (maps to global Today view — header shows "Today ▼")
- Each FD list the user has created, shown with list color dot and list name

**Behavior after selection:**
- Widget header updates to show selected list name + ▼
- Widget task list filters to show only tasks from that list (within the active sections)
- Selection persists across widget updates and app restarts (stored in `shared_preferences` per widget instance)

### 9.2 Task Row Tap

**Trigger:** Tap anywhere on a task row excluding the checkbox
**Result:** Opens FD and navigates directly to the task detail screen (SCR-02) for that task

### 9.3 Checkbox Tap

**Trigger:** Tap on the checkbox of any task row
**Result:**
1. Task is marked complete in FD's SQLite database immediately
2. Task moves to the bottom of its section within the widget
3. Android toast appears: "Task completed" with check icon
4. Widget refreshes (event-triggered update — see Section 11)

### 9.4 Plus Icon Tap

**Trigger:** Tap on + icon in header
**Result:** Opens FD and navigates to task creation screen (SCR-03) with today's date pre-filled

### 9.5 Overflow Menu Tap (⋮)

**Trigger:** Tap on ⋮ icon in header
**Result:** Opens overflow menu
**Contents:** TBD at implementation

---

## 10. Widget Settings Screen

The widget has a dedicated settings screen accessible via Android's widget long-press → "Widget settings" (or equivalent Android entry point). This is a native Android configuration Activity.

### 10.1 Settings Options

| Setting | Type | Default | Description |
|---|---|---|---|
| Show Tomorrow section | Toggle | Off | When enabled, a TOMORROW section appears in the widget below TODAY, showing tasks due tomorrow. When disabled, TOMORROW section is hidden entirely. |
| Theme | Single select | System | Options: Light / Dark / System (follows device theme) |
| Background opacity | Slider | 90% | Controls the opacity of the widget background. Range: 0% (fully transparent) to 100% (fully opaque). Wallpaper is visible beneath the widget at values below 100%. |

### 10.2 Settings Persistence

All widget settings are persisted to `shared_preferences` keyed by widget instance ID to support multiple widget instances with independent configurations.

---

## 11. Update Mechanism

### 11.1 Periodic Update

- **Frequency:** Every 15 minutes
- **Implementation:** Android `WorkManager` with `PeriodicWorkRequest`
- **Constraint:** No network required (all data is local SQLite)

### 11.2 Event-Triggered Update

The widget triggers an immediate refresh when any of the following FD data writes occur:

| Trigger Event | Description |
|---|---|
| Task created | New task added that falls within widget's visible date scope |
| Task completed | Task marked complete via widget checkbox or within FD app |
| Task updated | Due date, time, title, or list changed for a visible task |
| Task deleted | A visible task is deleted |
| List filter changed | User selects a different list in the widget's list picker |

**Implementation:** FD app calls `HomeWidget.updateWidget()` after each qualifying data write.

### 11.3 Stale Data Behaviour

If the widget has not updated within the last 15 minutes (e.g. device was off), it renders the last cached data silently. No staleness indicator is shown.

---

## 12. Data Sources

| Data | Source | Query |
|---|---|---|
| Overdue tasks | `ITaskRepository` | All incomplete tasks with due date < today |
| Today tasks | `ITaskRepository` | All incomplete tasks with due date = today |
| Tomorrow tasks (if enabled) | `ITaskRepository` | All incomplete tasks with due date = tomorrow |
| List name + color for each task | `IListRepository` | `getListById(task.listId)` |
| Widget filter preference (selected list) | `shared_preferences` | Key: `widget_[instanceId]_list_filter` |
| Widget theme preference | `shared_preferences` | Key: `widget_[instanceId]_theme` |
| Widget opacity preference | `shared_preferences` | Key: `widget_[instanceId]_opacity` |

---

## 13. Theme & Color Tokens

All colors are sourced exclusively from `ThemeTokens`. No hardcoded hex values anywhere in widget implementation.

### 13.1 Token Reference Table

| Token Name | Usage | Light Mode Value | Dark Mode Value |
|---|---|---|---|
| `ThemeTokens.widgetSurface` | Widget background color | Light surface | Dark surface (~#1E1E2E or equivalent) |
| `ThemeTokens.widgetOnSurface` | Primary text, header title, task titles | Dark text | White |
| `ThemeTokens.widgetOnSurfaceSecondary` | Task subtitle text, list name label | Medium grey | Light grey |
| `ThemeTokens.widgetOnSurfaceMuted` | Section labels, empty state text | Muted grey | Muted grey |
| `ThemeTokens.widgetCheckboxStroke` | Checkbox border (unchecked) | Yellow/orange | Yellow/orange |
| `ThemeTokens.widgetTimeOverdue` | Time metadata — overdue tasks | Red | Red |
| `ThemeTokens.widgetTimeToday` | Time metadata — today tasks | Orange | Orange |
| `ThemeTokens.widgetTimeTomorrow` | Time metadata — tomorrow tasks | Secondary text | Secondary text |
| `ThemeTokens.widgetIconPrimary` | Header icons (+ and ⋮) | Dark | White |

### 13.2 List Color Dots

List color dots on task rows use the list's user-defined hex color (or the system-locked academic domain color where applicable). These are passed as raw color values from the data layer and are not mapped to ThemeTokens — they are user data, not theme tokens.

### 13.3 Theme Selection Logic

| Widget Setting | Behaviour |
|---|---|
| Light | Widget always uses light theme tokens regardless of system theme |
| Dark | Widget always uses dark theme tokens regardless of system theme |
| System | Widget reads `UiModeManager` and applies light or dark tokens accordingly |

### 13.4 Background Opacity

`ThemeTokens.widgetSurface` is applied at the user-configured opacity value (0–100%) using Flutter's `Color.withOpacity()`. At values below 100%, the device wallpaper is visible through the widget background.

---

## 14. Flutter & Android Implementation Notes

### 14.1 Package

- **`home_widget`** Flutter package is used for all home screen widget implementation
- Widget UI is rendered as an Android `RemoteViews` layout (XML-based, not Flutter canvas)
- All widget tap actions are handled via `PendingIntent` through the `home_widget` callback mechanism

### 14.2 Component Mapping

| UI Element | Android / Flutter Equivalent |
|---|---|
| Widget root container | `RemoteViews` with rounded rect background drawable |
| Header bar | `LinearLayout` (horizontal) |
| List title + caret | `TextView` |
| Plus icon | `ImageView` |
| Overflow icon | `ImageView` |
| Section label | `TextView` |
| Task list | `ListView` (RemoteViews list) via `home_widget` |
| Task row | Custom `RemoteViews` item layout |
| Checkbox | `ImageView` (toggled via PendingIntent callback) |
| Task title | `TextView` |
| Task subtitle | `TextView` |
| Time metadata | `TextView` |
| List color dot | `View` with circular background drawable |
| List name label | `TextView` |

### 14.3 Tap Handling

All tappable elements in the widget are wired to `PendingIntent` objects that either:
- Launch FD with a specific deep-link URI (task detail, task creation), or
- Trigger a `BroadcastReceiver` callback handled by `home_widget` (checkbox completion, list picker)

### 14.4 List Picker Bottom Sheet

The list picker bottom sheet is rendered inside FD (not in the widget RemoteViews). Tapping the header title fires a PendingIntent that opens FD to a dedicated list picker Activity/screen, which writes the selection back to `shared_preferences` and triggers a widget update on close.

### 14.5 Minimum Touch Targets

All interactive elements must meet the Android minimum touch target of **48dp × 48dp** even if their visual size is smaller. Invisible padding zones are added around small icons as needed.

---

## 15. Accessibility

- Task rows: semantic label = "[Task title], due [date and time], [list name]"
- Checkbox: semantic label = "Mark [task title] as complete"
- Header title: semantic label = "Filter by list. Currently showing [list name or All Lists]"
- Plus icon: semantic label = "Create new task"
- Overflow icon: semantic label = "More options"
- Empty state text: semantic label = "No tasks today"
- Minimum touch targets: 48dp × 48dp for all interactive elements

---

## 16. Out of Scope for Phase 3

- Habits section (deferred — FD Habits feature not yet specced)
- iOS widgets (FD Phase 3 is Android only)
- Widget lock screen placement
- Pinned shortcuts from widget
- Multi-widget instance with different list filters shown simultaneously (supported by architecture but not a Phase 3 design requirement)
- In-widget task editing (title, date, time changes)
- Drag-to-reorder tasks within widget

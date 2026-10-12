# ui_ICONS_icon-system.md
## FluxDone — Icon System
**Document ID:** UI-ICONS
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Icon System Overview

FluxDone uses **Material Icons (filled variant) as the base icon set**. Custom SVG icons are used only where Material Icons has no equivalent. This keeps implementation effort minimal while maintaining a consistent, Android-native visual identity.

---

## 2. Global Icon Metrics

| Property | Value |
|---|---|
| Default size | 20dp |
| FAB icon size | 24dp |
| Metadata / micro icon size | 16dp |
| Small badge / inline icon size | 12dp |
| Icon style | Filled (default), Outlined (inactive states where specified) |
| Rendering | `Icon` widget with `size` and `color` parameters |

---

## 3. Color System

| State | Color | Notes |
|---|---|---|
| Default (inactive) | Secondary text color — `#757575` light / `#9E9E9E` dark | All non-selected icons |
| Active / selected | App primary color (theme-dependent) | Full opacity fill |
| Active on dark surface (nav rail) | `#FFFFFF` | e.g., bottom nav selected in dark mode |
| Disabled | Secondary text color at 40% opacity | Non-interactive states |
| Destructive | `#E53935` | Delete, trash, permanent actions |
| Success | `#43A047` | Restore, connected, complete |
| Warning / overdue | `#FB8C00` | Overdue dates, expiry warnings |

**Active state rule:** When an icon transitions to active/selected, it switches from the default secondary color to the app primary color at full opacity. No opacity reduction — the primary color is always rendered at 100%.

---

## 4. Bottom Navigation Bar Icons

| Tab | Inactive icon | Active icon | Material identifier |
|---|---|---|---|
| Tasks (List View) | `Icons.checklist` | `Icons.checklist` (primary color) | Material filled |
| Calendar | `Icons.calendar_month` | `Icons.calendar_month` (primary color) | Material filled |
| Habits | `Icons.loop` | `Icons.loop` (primary color) | Material filled |
| Settings | `Icons.settings` | `Icons.settings` (primary color) | Material filled |

**Size:** 24dp (bottom nav icons are slightly larger per Material spec)
**Inactive color:** Secondary text color
**Active color:** App primary color
**Label:** 12sp, medium weight, same color as icon

---

## 5. Navigation Rail Icons (Landscape)

Identical icons to bottom navigation bar. In landscape the rail shows icons + labels side by side.

**Selected item background:** App primary color at 12% opacity, corner radius 8dp, spans full rail width
**Selected icon + label color:** App primary color
**Unselected icon + label color:** Secondary text color

---

## 6. Side Drawer Icons

| Element | Icon | Size | Notes |
|---|---|---|---|
| Hamburger button | `Icons.menu` | 20dp | Opens drawer |
| Folder | `Icons.folder` | 20dp | Default folder rows |
| Folder (active) | `Icons.folder` | 20dp | Primary color |
| Expand chevron (collapsed) | `Icons.expand_more` | 16dp | Rotates on expand |
| Expand chevron (expanded) | `Icons.expand_more` rotated 180° | 16dp | Via `AnimatedRotation` |
| Color swatch | Custom circle (see Section 13) | 12dp | List color indicator |
| Add list/folder | `Icons.add` | 20dp | Primary color |
| Settings shortcut | `Icons.settings` | 20dp | Secondary text color |
| Smart List — Today | `Icons.today` | 20dp | — |
| Smart List — Tomorrow | `Icons.event` | 20dp | — |
| Smart List — Upcoming | `Icons.date_range` | 20dp | — |
| Smart List — All | `Icons.list_alt` | 20dp | — |
| Smart List — Completed | `Icons.check_circle` | 20dp | — |
| Smart List — Trash | `Icons.delete` | 20dp | — |

---

## 7. Top App Bar Icons

| Element | Icon | Size |
|---|---|---|
| Hamburger | `Icons.menu` | 20dp |
| Sort | `Icons.sort` | 20dp |
| Overflow menu | `Icons.more_vert` | 20dp |
| Back arrow | `Icons.arrow_back` | 20dp |
| Search | `Icons.search` | 20dp |
| Close (multi-select) | `Icons.close` | 20dp |
| External link | `Icons.open_in_new` | 20dp |

**Touch target:** All icon buttons wrapped in `IconButton` with minimum 48dp touch target.
**Ripple:** `InkWell` ripple, primary color at 12% opacity, 24dp radius.

---

## 8. Task Card Icons (Metadata Row)

All metadata icons render at **16dp**, secondary text color, inline with their text label.

| Metadata | Icon | Notes |
|---|---|---|
| Due date | `Icons.calendar_today` | 16dp, secondary text color. Red (`#E53935`) if overdue. |
| Start time | `Icons.access_time` | 16dp |
| Subtask count | `Icons.check_box_outlined` | 16dp |
| Reminder | `Icons.notifications` | 16dp, icon only (no text) |
| Recurring task | `Icons.repeat` | 16dp, icon only |

---

## 9. Priority Flag Icons

**Component:** `Icon` widget
**Base icon:** `Icons.flag_outlined` (inactive / None priority)
**Active icon:** `Icons.flag` (filled, when priority is set)
**Size:** 16dp in task cards, 20dp in task detail sheet

| Priority | Icon | Color |
|---|---|---|
| None | Not rendered | — |
| Low | `Icons.flag` (filled) | `#1565C0` Royal Blue |
| Medium | `Icons.flag` (filled) | `#FB8C00` Orange |
| High | `Icons.flag` (filled) | `#E53935` Red |

**In priority picker:** All four options shown as `Icons.flag` in their respective colors, plus a `Icons.flag_outlined` in secondary text color for "None".

---

## 10. Checkbox (Custom Component)

The checkbox is the most frequently rendered interactive element in the app. It is fully custom — not the Material `Checkbox` widget.

### 10.1 Visual Spec

**Component:** `CustomPainter` or `AnimatedContainer` + `CustomPaint`
**Size:** 20dp visual, 40dp touch target (via `GestureDetector` padding)
**Shape:** Rounded square
**Corner radius:** 4dp

### 10.2 States

**Unchecked:**
- Border: 1.5dp stroke, list color (`color_hex` of task's parent list)
- Fill: Transparent
- Checkmark: Not rendered

**Checked:**
- Border: None (fill replaces border)
- Fill: List color at 100% opacity
- Checkmark: White (`#FFFFFF`), 2dp stroke, rendered via `CustomPainter`

**Unchecked (Smart List context):**
- Border: 1.5dp stroke, app primary color (list color not available for mixed lists)

**Disabled (completed task in completed section):**
- Border: List color at 40% opacity
- Fill: List color at 40% opacity
- Checkmark: White at 70% opacity

### 10.3 Checkmark Geometry

```
    Short arm: starts at bottom-left quadrant
    Long arm: ends at top-right quadrant

    Path:
     • Start: (4dp, 10dp) from top-left of checkbox
     • Vertex: (8dp, 14dp)
     • End: (16dp, 6dp)

    Stroke: 2dp, round cap, round join, white
```

### 10.4 Check Animation (Unchecked → Checked)

1. Border color fades from list color to transparent (80ms)
2. Fill expands from center — scale 0 → 1 (100ms, `easeOut`)
3. Checkmark stroke draws from vertex outward — path animation (120ms, `easeOut`, starts at 20ms offset)

**Total animation duration:** 220ms

### 10.5 Uncheck Animation (Checked → Unchecked)

Reverse of check animation:
1. Checkmark fades out (80ms)
2. Fill shrinks to center — scale 1 → 0 (100ms, `easeIn`)
3. Border fades in (80ms, starts at 60ms offset)

**Total animation duration:** 180ms

### 10.6 Press State

- Scale: 0.92 (both checked and unchecked)
- Duration: 100ms `easeInOut`
- Ripple: List color at 15% opacity, bounded to 24dp radius

---

## 11. Action / Contextual Icons

| Action | Icon | Color | Size |
|---|---|---|---|
| Delete / trash | `Icons.delete_outline` | `#E53935` | 20dp |
| Delete forever | `Icons.delete_forever` | `#E53935` | 20dp |
| Restore from trash | `Icons.restore_from_trash` | `#43A047` | 20dp |
| Complete (swipe) | `Icons.check_circle_outline` | `#FFFFFF` | 24dp |
| Move to list | `Icons.drive_file_move_outline` | Secondary | 20dp |
| Duplicate | `Icons.content_copy` | Secondary | 20dp |
| Drag handle (reorder) | `Icons.drag_handle` | Secondary at 60% | 20dp |
| Add subtask | `Icons.add_task` | Secondary | 20dp |
| Pin | `Icons.push_pin_outlined` | Secondary | 20dp |
| Share | `Icons.share_outlined` | Secondary | 20dp |
| Edit | `Icons.edit_outlined` | Secondary | 20dp |
| Google Calendar event | `Icons.event_note` | `#9E9E9E` | 16dp |

---

## 12. Calendar View Icons

| Element | Icon | Size | Notes |
|---|---|---|---|
| Previous period | `Icons.chevron_left` | 20dp | Top app bar navigation |
| Next period | `Icons.chevron_right` | 20dp | Top app bar navigation |
| Today shortcut | `Icons.today` | 20dp | Overflow menu |
| FAB add | `Icons.add` | 24dp | White on primary color background |
| Current time dot | Custom circle (see Section 13) | 10dp | Red, `CustomPaint` |
| Drag resize handle | Custom horizontal bar (see Section 13) | 20dp × 4dp | Bottom of task block |

---

## 13. Custom SVG / CustomPaint Components

The following elements have no direct Material equivalent and are implemented via `CustomPainter`:

### 13.1 Color Swatch Circle

**Use:** List color indicator in drawer, color picker
**Shape:** Filled circle
**Size:** Variable — 12dp (drawer), 20dp (color picker row), 40dp (color picker preview), 48dp (large preview)
**Implementation:** `Container` with `BoxDecoration(shape: BoxShape.circle, color: Color(hex))`
**No CustomPainter needed** — pure Flutter decoration.

### 13.2 Current Time Indicator (Calendar)

**Shape:** Horizontal line + left circle dot
**Line:** 2dp height, `#F44336`, spans full column width
**Dot:** 10dp filled circle, `#F44336`, left edge of line
**Implementation:** `CustomPainter` inside `Stack`, redrawn every 60 seconds via `Timer.periodic`

### 13.3 Task Block Drag Resize Handle

**Use:** Bottom edge of calendar task blocks (tap-to-create + existing block resize)
**Shape:** Horizontal rounded rectangle
**Dimensions:** 20dp wide, 4dp tall
**Color:** White at 60% opacity
**Corner radius:** 2dp
**Position:** Horizontally centered, 4dp from bottom edge of task block
**Implementation:** `Container` with `BoxDecoration` inside `Positioned`

### 13.4 Ghost Block Dashed Border

**Use:** Tap-to-create preview block in Calendar View
**Shape:** Rounded rectangle with dashed border
**Implementation:** `CustomPainter` — draws dashed path manually (4dp dash, 4dp gap, 1.5dp stroke, primary color)
**Corner radius:** 6dp
**Fill:** Primary color at 30% opacity

### 13.5 Section Collapse Chevron

Already covered by `Icons.expand_more` with `AnimatedRotation`. No custom paint needed.

---

## 14. Icon Animation Specifications

| Icon / Component | Trigger | Animation | Duration | Curve |
|---|---|---|---|---|
| Checkbox unchecked → checked | Tap | Fill expand + checkmark draw | 220ms total | `easeOut` |
| Checkbox checked → unchecked | Tap | Checkmark fade + fill shrink | 180ms total | `easeIn` |
| Checkbox press | Press down | Scale 1.0 → 0.92 | 100ms | `easeInOut` |
| Checkbox release | Press up | Scale 0.92 → 1.0 | 100ms | `easeInOut` |
| Folder chevron collapse | Tap folder row | Rotate 0° → 180° | 200ms | `easeInOut` |
| Folder chevron expand | Tap folder row | Rotate 180° → 0° | 200ms | `easeInOut` |
| Nav icon active transition | Tab switch | Color transition secondary → primary | 180ms | `easeInOut` |
| Nav icon inactive transition | Tab switch | Color transition primary → secondary | 180ms | `easeInOut` |
| FAB press | Press down | Scale 1.0 → 0.95 | 120ms | `easeInOut` |
| FAB release | Press up | Scale 0.95 → 1.0 | 120ms | `easeInOut` |
| Icon button ripple | Tap | Radial ripple expand + fade | 150ms | — |

---

## 15. Icon Button Specs (Global)

All icon buttons in the app share these base specs:

**Component:** `IconButton`
**Touch target:** 48dp × 48dp (enforced by `IconButton` default)
**Splash radius:** 24dp
**Ripple color:** App primary color at 12% opacity (light mode), white at 12% opacity (dark mode / dark surfaces)
**Disabled opacity:** 40%
**Padding:** 8dp (IconButton default — icon centered in touch target)

---

## 16. Iconography Principles

1. **Filled icons for active/functional states** — filled icons have stronger visual weight and are easier to scan at a glance
2. **Outlined for inactive/secondary** — where differentiation between active and inactive states matters (e.g., nav bar), outlined inactive + filled active creates a clear visual hierarchy
3. **Color carries meaning** — icon color is never arbitrary. Red = destructive. Green = success/complete. Orange = warning. Primary = active/selected. Secondary = neutral/inactive.
4. **Size hierarchy** — 24dp (primary actions: FAB, nav bar), 20dp (standard: app bar, drawer, list actions), 16dp (metadata: task card inline), 12dp (micro: badges, inline markers)
5. **No icon used without text label or clear context** — every standalone icon has either a visible label, a tooltip, or an unambiguous semantic meaning in context

---

## 17. Flutter Implementation Notes

| Scenario | Recommended Widget |
|---|---|
| Standard Material icon | `Icon(Icons.xxx, size: 20, color: color)` |
| Icon button with ripple | `IconButton(icon: Icon(...), onPressed: ...)` |
| Animated icon color | `AnimatedTheme` or `TweenAnimationBuilder<Color>` |
| Checkbox | `CustomPainter` + `GestureDetector` |
| Dashed border (ghost block) | `CustomPainter` with path dashing |
| Current time dot + line | `CustomPainter` inside `Stack` |
| Color swatch | `Container` with `BoxDecoration(shape: BoxShape.circle)` |
| Chevron rotation | `AnimatedRotation(turns: expanded ? 0.5 : 0.0)` |
| Nav bar icon active state | `NavigationBar` with `selectedIndex` driving color |

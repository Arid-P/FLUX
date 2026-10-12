# ui_SCR-06_habit-detail.md
## FluxDone — Habit Detail / Edit
**Screen ID:** SCR-06
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-06 is the full habit detail and inline editing surface. It shows the habit's streak stats, a monthly calendar completion history, and all editable fields inline. Changes auto-save in real time. There is no separate edit screen — all editing happens directly on this screen.

---

## 2. Navigation & Entry Points

- **Entry:** Tap habit card in SCR-05
- **Exit:** Back arrow in top app bar or Android back gesture
- **Transition in:** Horizontal slide-in from right (300ms `easeInOut`)
- **Transition out:** Horizontal slide-out to right

---

## 3. Overall Layout

```
┌─────────────────────────────────────────────┐
│ Status Bar                                  │
├─────────────────────────────────────────────┤
│ Top App Bar (56dp)                          │
│ [←] [Habit name]              [Delete] [⋮] │
├─────────────────────────────────────────────┤
│ Scrollable Body                             │
│                                             │
│  ┌── Stats Row ──────────────────────────┐  │
│  │ Current streak | Longest | Total      │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌── Monthly Calendar ───────────────────┐  │
│  │ [← Month Year →]                      │  │
│  │ M  T  W  T  F  S  S                  │  │
│  │ •     •  •        •                  │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  ┌── Editable Fields ────────────────────┐  │
│  │ Habit name                            │  │
│  │ Icon + Color                          │  │
│  │ Frequency                             │  │
│  │ Daily target                          │  │
│  │ Reminder                              │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## 4. Top App Bar

**Component:** `AppBar`
**Height:** 56dp
**Background:** Surface color
**Elevation:** 0dp, 1dp bottom divider

| Position | Component | Details |
|---|---|---|
| Left | Back arrow `IconButton` | `Icons.arrow_back`, 20dp |
| Center | Habit name `Text` | 18sp, semibold, primary text color |
| Right | Delete `IconButton` | `Icons.delete_outline`, 20dp, `#E53935` |
| Right | Overflow `IconButton` | `Icons.more_vert`, 20dp |

**Delete tap:** Opens confirmation `AlertDialog`:
- Title: "Delete habit?"
- Body: "All completion history will be permanently deleted."
- Actions: "Cancel", "Delete" (red)
- On confirm: deletes habit + all `habit_completions`, navigates back to SCR-05, habit card animates out

**Overflow menu:**
- "Archive habit" (Phase 2 — grayed out)
- "Share streak" (Phase 2 — grayed out)

---

## 5. Stats Row

**Component:** `Row` of 3 equal-width stat blocks
**Height:** 80dp
**Background:** Habit color at 10% opacity
**Corner radius:** 12dp
**Margin:** 16dp horizontal, 16dp top

### 5.1 Stat Block

```
Column (centered)
 ├── Value Text (large)
 └── Label Text (small)
```

| Stat | Value source | Label |
|---|---|---|
| Current streak | `habits.current_streak` | "Current streak" |
| Longest streak | `habits.longest_streak` | "Longest streak" |
| Total completions | COUNT of `habit_completions` | "Total completions" |

**Value typography:** 24sp, bold, habit color
**Label typography:** 11sp, regular, secondary text color
**Dividers:** 1dp vertical dividers between stat blocks, secondary text color at 20% opacity

---

## 6. Monthly Calendar

**Component:** Custom calendar grid
**Margin:** 16dp horizontal, 16dp top
**Background:** Surface color
**Corner radius:** 12dp
**Padding:** 16dp

### 6.1 Month Navigation Header

```
Row
 ├── IconButton (Icons.chevron_left, 20dp) — previous month
 ├── Expanded: "March 2026" Text (16sp, semibold, centered)
 └── IconButton (Icons.chevron_right, 20dp) — next month
```

**Navigation animation:** Horizontal slide 200ms `easeInOut`
**Future months:** Navigable (for planning view) but completion dots absent

### 6.2 Weekday Header Row

7 columns: M, T, W, T, F, S, S
**Typography:** 11sp, regular, secondary text color
**Height:** 32dp

### 6.3 Day Cells

**Cell size:** Equal width distribution, 36dp height
**Layout per cell:**

```
Column (centered)
 ├── Date number Text
 └── Completion dot (conditional)
```

**Completion states:**

| State | Visual |
|---|---|
| Completed | Filled circle (habit color), 28dp, white date number |
| Partially completed (target > 1) | Outlined circle (habit color), 28dp, habit color date number |
| Not completed (scheduled day) | No circle, primary text color date number |
| Not scheduled (frequency excludes this day) | No circle, secondary text color at 40% opacity |
| Today | Underline below date number (1.5dp, habit color) |
| Future | Secondary text color at 60% opacity |

**Tap on past date cell:**
- Toggles completion for that date
- Updates `habit_completions` table (insert or delete)
- Streak recalculated via `StreakCalculator`
- Stats row updates immediately

---

## 7. Editable Fields Section

Rendered below the monthly calendar. All fields auto-save on change (immediate for pickers, 500ms debounce for text).

**Section background:** Surface color
**Corner radius:** 12dp
**Margin:** 16dp horizontal, 16dp top
**Internal rows:** `ListTile` pattern, 1dp dividers

### 7.1 Habit Name Field

**Component:** `TextField`
**Padding:** 16dp
**Font:** 16sp, regular, primary text color
**Placeholder:** "Habit name"
**Decoration:** None (collapsed)
**Auto-save:** 500ms debounce → `IHabitRepository.updateHabit()`

### 7.2 Icon + Color Row

**Leading icon:** `Icons.palette_outlined`, 20dp
**Label:** "Appearance"
**Trailing:** Preview showing current icon (20dp) on habit color circle (32dp)
**Tap:** Opens icon + color picker bottom sheet (see Section 8)

### 7.3 Frequency Row

**Leading icon:** `Icons.repeat`, 20dp
**Label:** "Frequency"
**Value:** Human-readable (e.g., "Daily", "Mon, Wed, Fri", "3× per week")
**Trailing:** `Icons.chevron_right`
**Tap:** Opens frequency picker bottom sheet (same as SCR-07 Section — see SCR-07 spec)

### 7.4 Daily Target Row

**Leading icon:** `Icons.flag_outlined`, 20dp
**Label:** "Daily target"
**Value:** "N times per day"
**Trailing:** Row of `IconButton` minus/plus

```
Row
 ├── IconButton (Icons.remove, 20dp) — decrements, min 1
 ├── "N" Text (16sp, medium, primary color)
 └── IconButton (Icons.add, 20dp) — increments, max 99
```

Auto-saves immediately on tap.

### 7.5 Reminder Row

**Leading icon:** `Icons.notifications_outlined`, 20dp
**Label:** "Reminder"
**Value:** Time string (e.g., "8:00 AM") or "None"
**Trailing:** `Icons.chevron_right`
**Tap:** Opens clock face time picker bottom sheet. On selection, saves reminder time to `habits` table (Phase 1: stored but notification scheduling in Phase 2).

---

## 8. Icon + Color Picker Bottom Sheet

**Component:** `showModalBottomSheet`
**Height:** 70% screen height
**Corner radius:** 16dp top corners

### 8.1 Layout

```
Column
 ├── Handle bar
 ├── Title: "Appearance" (16sp, semibold, centered)
 ├── Preview row (large icon on color background, 64dp circle)
 ├── Divider
 ├── "Color" section header
 ├── Color preset grid (4 × N swatches)
 ├── Hex input field
 ├── Divider
 ├── "Icon" section header
 └── Icon grid (scrollable)
```

### 8.2 Color Presets

**Grid:** 4 columns, wrap layout
**Swatch size:** 40dp circle
**Spacing:** 8dp
**Selected state:** White checkmark (`Icons.check`, 16dp) centered on swatch
**Colors (12 presets):** `#E53935`, `#FB8C00`, `#43A047`, `#1565C0`, `#7B1FA2`, `#00838F`, `#F57F17`, `#5E35B1`, `#546E7A`, `#2E7D32`, `#576481`, `#E64A19`

### 8.3 Hex Input

Same spec as SCR-08 color picker hex input.

### 8.4 Icon Grid

**Grid:** 5 columns, wrap layout
**Cell size:** 48dp
**Icon size:** 24dp
**Selected state:** Habit color background circle, 40dp, white icon
**Available icons (minimum set for Phase 1):**
`Icons.fitness_center`, `Icons.menu_book`, `Icons.self_improvement`, `Icons.directions_run`, `Icons.water_drop`, `Icons.bedtime`, `Icons.code`, `Icons.music_note`, `Icons.brush`, `Icons.restaurant`, `Icons.favorite`, `Icons.star`, `Icons.lightbulb`, `Icons.school`, `Icons.sports_soccer`

**Apply button:** Full width, 48dp, habit color background, "Apply", dismisses sheet, auto-saves.

---

## 9. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Screen open | Horizontal slide in | 300ms | `easeInOut` |
| Month navigation | Horizontal slide (calendar grid) | 200ms | `easeInOut` |
| Day cell tap (complete) | Fill circle expand from center | 200ms | `easeOut` |
| Day cell tap (uncomplete) | Fill circle shrink | 150ms | `easeIn` |
| Stats update | Number count-up animation | 300ms | `easeOut` |
| Icon/color picker open | Slide up | 300ms | Spring |
| Frequency picker open | Slide up | 300ms | Spring |

---

## 10. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Screen root | `Scaffold` |
| Top app bar | `AppBar` |
| Scrollable body | `SingleChildScrollView` + `Column` |
| Stats row | `Row` of 3 custom stat blocks |
| Monthly calendar | Custom `GridView` + `CustomPainter` |
| Habit name field | `TextField` |
| Frequency/reminder rows | `ListTile` + `InkWell` |
| Daily target row | Custom `Row` with `IconButton` |
| Icon + color picker | `showModalBottomSheet` |
| Delete dialog | `showDialog` + `AlertDialog` |

---

---

# ui_SCR-07_habit-creation-sheet.md
## FluxDone — Habit Creation Sheet
**Screen ID:** SCR-07
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-07 is the habit creation surface. It opens as a bottom sheet from the FAB on SCR-05. The user sets the habit name, icon, color, frequency rule, daily target, and optional reminder before saving. On save, the habit is persisted to the database and appears in SCR-05.

---

## 2. Entry Points

- **Entry:** FAB tap on SCR-05
- **Dismissal:** Swipe down, tap scrim, Android back gesture
- **Discard dialog:** If habit name is non-empty and user dismisses → "Discard habit?" `AlertDialog`

---

## 3. Sheet Behaviour

**Component:** `showModalBottomSheet` with `isScrollControlled: true`
**Initial height:** 60% of screen height
**Max height:** 90% of screen height (expands with keyboard)
**Corner radius:** 16dp top corners
**Handle bar:** Standard (4dp × 32dp, `#E0E0E0`, centered, 8dp from top)
**Keyboard behaviour:** Sheet height increases via `MediaQuery.viewInsets.bottom` when keyboard appears

---

## 4. Layout Structure

```
Column
 ├── Handle bar
 ├── Title row: "New Habit" (16sp, semibold, centered, 16dp top padding)
 ├── Appearance preview row (icon + color)
 ├── Divider
 ├── Habit name field
 ├── Divider
 ├── Frequency row
 ├── Divider
 ├── Daily target row
 ├── Divider
 ├── Reminder row
 ├── Divider
 └── Create button (full width)
```

---

## 5. Appearance Preview Row

**Height:** 72dp
**Layout:**

```
Row (centered)
 ├── Habit icon on color circle (56dp)
 └── "Change" TextButton (14sp, app primary color)
```

**Habit icon circle:**
- 56dp diameter
- Background: selected habit color
- Icon: selected icon, 28dp, white
- Default color: app primary color
- Default icon: `Icons.star`

**"Change" tap:** Opens icon + color picker bottom sheet (same spec as SCR-06 Section 8)

---

## 6. Habit Name Field

**Component:** `TextField`
**Padding:** 16dp horizontal, 12dp vertical
**Font:** 16sp, regular, primary text color
**Placeholder:** "Habit name"
**Decoration:** None (collapsed)
**Autofocused:** Yes — keyboard appears on sheet open
**Mandatory:** Yes — Create button disabled if empty

---

## 7. Frequency Row

**Leading icon:** `Icons.repeat`, 20dp, secondary text color
**Label:** "Frequency"
**Value:** Current selection (default: "Daily")
**Trailing:** `Icons.chevron_right`
**Tap:** Opens frequency picker bottom sheet (Section 9)

---

## 8. Daily Target Row

**Leading icon:** `Icons.flag_outlined`, 20dp
**Label:** "Daily target"
**Trailing:**

```
Row
 ├── IconButton (Icons.remove, 20dp) — min 1
 ├── "N times" Text (14sp, medium)
 └── IconButton (Icons.add, 20dp) — max 99
```

**Default:** 1 time per day
**Label updates:** "1 time", "2 times", "3 times" etc.

---

## 9. Frequency Picker Bottom Sheet

**Component:** `showModalBottomSheet` (stacked above creation sheet)
**Height:** 55% screen height
**Corner radius:** 16dp top corners

### 9.1 Preset Options (RadioListTile)

| Option | `frequency_rule` JSON |
|---|---|
| Daily (default) | `{"type":"daily"}` |
| Specific days | `{"type":"specific_days","days":[N...]}` |
| X times per week | `{"type":"x_per_week","times_per_week":N}` |

### 9.2 Specific Days Sub-UI

Visible when "Specific days" selected. Rendered inline below the radio option:

```
Row of 7 day chips: M  T  W  T  F  S  S
```

**Day chip specs:**
- Size: 36dp circle
- Selected: habit color fill, white text
- Unselected: outlined, secondary text color
- Multiple selection allowed (at least 1 required)

### 9.3 X Times Per Week Sub-UI

Visible when "X times per week" selected:

```
Row
 ├── IconButton (Icons.remove) — min 1
 ├── "N times per week" Text
 └── IconButton (Icons.add) — max 7
```

**Confirm button:** "Done" — full width, 48dp, habit color, dismisses sheet, updates frequency row value.

---

## 10. Reminder Row

**Leading icon:** `Icons.notifications_outlined`, 20dp
**Label:** "Reminder"
**Value:** "None" (default) or selected time (e.g., "8:00 AM")
**Trailing:** `Icons.chevron_right`
**Tap:** Opens clock face time picker bottom sheet (same spec as SCR-03 Section 7 — time only, no date)

**Clear reminder:** When a time is selected, a small `Icons.close` (14dp) appears trailing the time value. Tapping it clears the reminder back to "None".

---

## 11. Create Button

**Component:** Full-width `ElevatedButton`
**Height:** 52dp
**Margin:** 16dp horizontal, 16dp top, 16dp bottom
**Corner radius:** 12dp
**Background:** App primary color (disabled: secondary text color at 40%)
**Label:** "Create habit", 16sp, medium, white
**Disabled state:** When habit name is empty

**Tap behavior (valid):**
1. Creates `habits` row in database with all field values
2. Sets `current_streak = 0`, `longest_streak = 0`, `is_active = 1`
3. If reminder set: schedules local notification (Phase 2)
4. Dismisses sheet (slide down, 250ms)
5. New habit card fades + slides into SCR-05 list (250ms `easeOut`)

---

## 12. Validation

| Field | Rule | Behaviour on violation |
|---|---|---|
| Habit name | Non-empty | Create button disabled |
| Frequency | At least 1 day if specific_days | "Done" button disabled in frequency picker |
| Daily target | 1–99 | Enforced by min/max on +/− buttons |
| Reminder | Optional | No validation |

---

## 13. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Sheet open | Slide up from bottom | 300ms | Spring |
| Sheet dismiss | Slide down | 250ms | `easeIn` |
| Keyboard appear | Sheet height increase | Sync | — |
| Frequency picker open | Slide up (stacked) | 300ms | Spring |
| Icon/color picker open | Slide up (stacked) | 300ms | Spring |
| Day chip selection | Fill scale 0.8→1.0 | 150ms | `easeOut` |
| Create button enabled/disabled | Opacity transition | 200ms | `easeInOut` |
| New habit appears in SCR-05 | Fade + slide in | 250ms | `easeOut` |

---

## 14. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Sheet container | `showModalBottomSheet` with `isScrollControlled: true` |
| Appearance preview | Custom `Row` with `CircleAvatar` |
| Habit name | `TextField` |
| Frequency row | `ListTile` + `InkWell` |
| Frequency picker | `showModalBottomSheet` with `RadioListTile` |
| Day chips | Custom `Row` of toggle `FilterChip` |
| Daily target | Custom `Row` with `IconButton` |
| Reminder | `ListTile` + clock face `showModalBottomSheet` |
| Create button | `ElevatedButton` full width |
| Discard dialog | `showDialog` + `AlertDialog` |
| Icon/color picker | Same `showModalBottomSheet` as SCR-06 Section 8 |

---

## 15. Data Written on Create

| Field | Database column | Notes |
|---|---|---|
| Habit name | `habits.name` | Required |
| Color | `habits.color_hex` | 6-char hex, no # prefix |
| Icon | `habits.icon_identifier` | String key mapping to `Icons.*` |
| Frequency rule | `habits.frequency_rule` | JSON string |
| Daily target | `habits.target_count` | Integer, default 1 |
| Reminder time | `habits.reminder_time` | Unix ms time-of-day, nullable |
| Current streak | `habits.current_streak` | 0 on creation |
| Longest streak | `habits.longest_streak` | 0 on creation |
| Is active | `habits.is_active` | 1 on creation |
| Created at | `habits.created_at` | Unix ms |

**Note:** `habits` table requires two new columns not in TRD v1: `target_count` (INTEGER NOT NULL DEFAULT 1) and `icon_identifier` (TEXT NOT NULL DEFAULT 'star'). These must be added in TRD v2.

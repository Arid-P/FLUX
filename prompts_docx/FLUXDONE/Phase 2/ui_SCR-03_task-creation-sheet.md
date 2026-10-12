# ui_SCR-03_task-creation-sheet.md
## FluxDone — Task Creation Sheet
**Screen ID:** SCR-03
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-03 is the primary task creation surface. It opens as a bottom sheet from any screen in the app. In its collapsed state it exposes the minimum fields needed to create a task quickly. It can expand to fill the full screen, at which point it becomes functionally equivalent to SCR-02 (Task Detail) in creation mode. On save, the task is immediately persisted to the local database and appears in its target list.

---

## 2. Entry Points

| Source | Pre-filled fields |
|---|---|
| FAB tap (SCR-01 List View) | List = currently viewed list |
| FAB tap (Calendar View) | List = last used list |
| Tap empty slot (Calendar View) | Date = tapped date, Start time = tapped slot time |
| Long press empty slot (Calendar View) | Date + Start time + End time (from drag) |

---

## 3. Sheet Behaviour

**Component:** `DraggableScrollableSheet` inside `showModalBottomSheet`
**Initial size:** 45% of screen height (collapsed state)
**Expanded size:** 100% of screen height (full screen state)
**Min size:** 45% (cannot collapse below initial)
**Corner radius:** 16dp top corners (collapsed), 0dp (fully expanded)
**Background:** Surface color (white light / dark surface dark)
**Handle bar:** 4dp × 32dp, `#E0E0E0`, centered, 8dp from top

**Dismissal:**
- Swipe down below initial size → sheet dismisses (no save)
- Tap scrim → sheet dismisses (no save)
- Android back gesture → sheet dismisses (no save)
- If title is non-empty and user dismisses → show `AlertDialog`: "Discard task?" with "Discard" and "Keep editing" actions

**Expansion trigger:**
- Drag handle dragged upward past 80% screen height → snaps to full screen
- Tapping the expand icon (top-right of sheet header) → expands to full screen
- Description field tapped → auto-expands to full screen (description not available in collapsed state)

---

## 4. Collapsed State Layout

```
┌─────────────────────────────────────────────┐
│ [Drag handle — centered]                    │
├─────────────────────────────────────────────┤
│ [Checkbox]  [Title field]          [Expand] │
├─────────────────────────────────────────────┤
│ [Date chip] [Time chip] [List chip] [Priority icon] [More •••] │
├─────────────────────────────────────────────┤
│ [Submit button — right aligned]             │
└─────────────────────────────────────────────┘
```

### 4.1 Title Field Row

**Component:** `TextField`
**Padding:** 16dp left, 16dp right, 12dp top
**Layout:**

```
Row
 ├── Checkbox (20dp, unchecked, list color border)
 ├── 12dp gap
 ├── Title TextField (expanded)
 └── Expand IconButton (Icons.open_in_full, 20dp)
```

**Title TextField specs:**
- Placeholder: "Task title"
- Font: 16sp, regular (400), primary text color
- Placeholder color: secondary text color
- Single line (auto-expands to 2 lines max in collapsed state)
- No border decoration (`InputDecoration.collapsed`)
- `TextInputAction.done`
- Autofocused on sheet open — keyboard appears immediately
- Mandatory: validation on submit (see Section 8)

**Expand IconButton:**
- Icon: `Icons.open_in_full`, 20dp, secondary text color
- Tap → expands sheet to full screen, focuses title field, cursor at end

### 4.2 Metadata Row

**Component:** `SingleChildScrollView` (horizontal) wrapping `Row`
**Height:** 40dp
**Padding:** 16dp left, 16dp right
**Spacing between items:** 8dp

**Items (left to right):**

| Item | Component | Default label | Tap behavior |
|---|---|---|---|
| Date | `FilterChip` | "Date" or pre-filled date | Opens date picker bottom sheet (Section 6) |
| Start time | `FilterChip` | "Time" or pre-filled time | Opens time picker (Section 7). Hidden if no date set. |
| End time | `FilterChip` | "End time" | Opens end time picker (Section 7). Visible only after start time set. |
| List | `FilterChip` | Current list name | Opens list selector bottom sheet (Section 9) |
| Priority | `IconButton` | `Icons.flag_outlined`, secondary color | Opens priority dropdown (Section 10) |
| More | `IconButton` | `Icons.more_horiz`, secondary color | Expands sheet to full screen, scrolls to Reminder/Recurrence fields |

**FilterChip specs:**
- Height: 32dp
- Corner radius: 16dp (pill)
- Background: Secondary text color at 10% opacity
- Border: 1dp, secondary text color at 30% opacity
- Label: 13sp, medium (500), primary text color
- Selected/filled state: List color at 15% opacity, list color border
- Icon (leading): 14dp, matches label color

### 4.3 Submit Button

**Component:** `FloatingActionButton.small` or custom `InkWell` circle
**Size:** 40dp
**Position:** Bottom-right of sheet, 16dp from right, 12dp from bottom
**Icon:** `Icons.send`, 20dp, white
**Background:** App primary color
**Tap behavior:** Validates and saves (see Section 8)

---

## 5. Expanded State Layout (Full Screen)

When expanded to full screen, SCR-03 becomes functionally identical to SCR-02 in creation mode. See SCR-02 for full expanded layout specification.

**Differences from SCR-02 (edit mode):**
- Top app bar shows "New Task" as title instead of list name
- No overflow menu (3-dot) — task doesn't exist yet
- Save button replaces the X/close button on the right of the top app bar
- Auto-save does NOT apply — task is only created on explicit save tap
- Back arrow / back gesture → "Discard task?" confirmation dialog

---

## 6. Date Picker Bottom Sheet

**Trigger:** Tap Date chip in metadata row
**Component:** `showModalBottomSheet`
**Corner radius:** 16dp top corners
**Height:** ~55% screen height

### 6.1 Layout

```
Column
 ├── Handle bar
 ├── Month/year header row [← Month Year →]
 ├── Weekday header row [M T W T F S S]
 ├── Calendar grid (6 rows × 7 columns)
 ├── Divider
 └── Quick shortcuts row [Today] [Tomorrow] [Next week] [No date]
```

### 6.2 Calendar Grid

**Cell size:** Equal width distribution across available width, height 44dp
**Today's date:** Outlined circle, app primary color border, 32dp diameter
**Selected date:** Filled circle, app primary color, 32dp diameter, white text
**Other month dates:** Secondary text color at 40% opacity
**Current month dates:** Primary text color

**Navigation:** Left/right arrow `IconButton` (20dp) changes month. Animation: horizontal slide 200ms `easeInOut`.

### 6.3 Quick Shortcuts

**Component:** `Row` of `TextButton` items
**Items:** "Today", "Tomorrow", "Next week", "No date"
**Font:** 13sp, medium, app primary color
**Tap:** Sets date and dismisses sheet

### 6.4 Confirm

Tapping a date cell automatically confirms and dismisses the sheet. Date chip updates immediately.

---

## 7. Time Picker

**Trigger:** Tap Start time chip or End time chip
**Component:** `showModalBottomSheet` containing `TimePickerDialog` styled as clock face

### 7.1 Clock Face Specs

**Style:** Circular clock face (not spinner)
**Mode:** Hours first → tap confirms hour → switches to minutes
**Clock face diameter:** ~240dp
**Background:** Surface color
**Hand color:** App primary color
**Selected hour/minute indicator:** Filled circle, app primary color, white text
**AM/PM toggle:** Two `TextButton` items below clock face

### 7.2 Start vs End Time

- **Start time picker:** Sets `start_time`. After selection, end time chip becomes visible.
- **End time picker:** Sets `end_time`. Must be after start time. If user picks a time before start time, end time auto-advances to start time + 1 hour and shows snackbar: "End time must be after start time."

### 7.3 Duration Display

When both start and end time are set, the time chip updates to show:
"9:00 PM – 11:00 PM (2h)" — 13sp, medium

---

## 8. Validation & Save Behaviour

### 8.1 Submit tap (collapsed state)

1. Title empty → no-op (submit button remains tappable but does nothing; title field border flashes red for 300ms)
2. Date not set → show snackbar: "Please set a date for this task." Duration: 3 seconds.
3. Title non-empty + date set → task created, sheet dismisses with slide-down animation (300ms), snackbar: "Task added to [List name]"

### 8.2 Save tap (expanded / full screen state)

Same validation as above. On success: navigates back to previous screen, task appears in list.

### 8.3 Discard dialog

**Component:** `AlertDialog`
- Title: "Discard task?"
- Body: "Your task will not be saved."
- Actions: "Keep editing" (dismisses dialog), "Discard" (dismisses sheet/screen, no save)

---

## 9. List Selector Bottom Sheet

**Trigger:** Tap List chip in metadata row
**Component:** `showModalBottomSheet`
**Height:** 65% screen height
**Corner radius:** 16dp top corners

### 9.1 Layout

```
Column
 ├── Handle bar
 ├── Title row: "Move to list" (16sp, semibold, centered)
 ├── Search field (TextField, "Search lists", Icons.search leading)
 └── Scrollable list
      ├── Folder header row (folder name, not tappable)
      │    └── List rows (indented 16dp)
      └── (repeated per folder)
```

### 9.2 List Row

**Component:** `ListTile`
**Leading:** Color swatch circle (12dp, list color)
**Title:** List name, 14sp, regular
**Trailing:** `Icons.check` (20dp, app primary color) — only on currently selected list
**Tap:** Selects list, dismisses sheet, updates List chip label

### 9.3 Folder Header Row

**Height:** 36dp
**Padding:** 16dp left
**Text:** Folder name, 12sp, semibold, all caps, secondary text color
**Not tappable**

---

## 10. Priority Dropdown

**Trigger:** Tap priority `IconButton` (`Icons.flag_outlined`)
**Component:** Custom `OverlayEntry` or `PopupMenuButton` anchored to the icon
**Width:** 180dp
**Corner radius:** 8dp
**Background:** Surface color
**Elevation:** 8dp

### 10.1 Options (top to bottom)

| Option | Icon | Color | Label |
|---|---|---|---|
| High | `Icons.flag` | `#E53935` | "High" |
| Medium | `Icons.flag` | `#FB8C00` | "Medium" |
| Low | `Icons.flag` | `#1565C0` | "Low" |
| None | `Icons.flag_outlined` | Secondary text | "None" |

**Row height:** 44dp
**Icon size:** 20dp
**Label:** 14sp, regular, primary text color
**Selected state:** Row background: app primary color at 10% opacity
**Tap:** Sets priority, dismisses dropdown, updates priority icon color

---

## 11. Reminder & Recurrence (via More button)

Tapping "More" (`Icons.more_horiz`) expands the sheet to full screen and scrolls to the Reminder and Recurrence sections. These fields are not available in the collapsed state.

See SCR-02 Sections 9 and 10 for full Reminder and Recurrence specs.

---

## 12. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Sheet open | Slide up from bottom | 300ms | `easeOutCubic` |
| Sheet dismiss (swipe down) | Slide down | 250ms | `easeIn` |
| Sheet expand to full screen | Height animation 45% → 100% | 350ms | `easeOutCubic` |
| Corner radius on expand | 16dp → 0dp | 350ms | `easeOutCubic` |
| Keyboard appear | Sheet shifts up via `viewInsets` | Sync with keyboard | — |
| Date picker open | Slide up | 300ms | Spring |
| Time picker open | Slide up | 300ms | Spring |
| List selector open | Slide up | 300ms | Spring |
| Priority dropdown open | Fade in + scale 0.9 → 1.0 | 150ms | `easeOut` |
| Submit success | Sheet slides down | 300ms | `easeIn` |
| Title validation flash | Border red flash | 300ms | — |
| Chip selected state | Background color transition | 150ms | `easeInOut` |

---

## 13. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Sheet container | `DraggableScrollableSheet` |
| Title field | `TextField` with `InputDecoration.collapsed` |
| Checkbox | Custom `CustomPainter` (see ICONS spec) |
| Metadata row | `SingleChildScrollView` + `Row` |
| Date/time/list chips | `FilterChip` |
| Priority button | `IconButton` + custom `OverlayEntry` |
| Date picker | `showModalBottomSheet` with custom calendar |
| Time picker | `showModalBottomSheet` with `ClockFace` widget |
| List selector | `showModalBottomSheet` with `ListView` |
| Submit button | Custom circular `InkWell` |
| Discard dialog | `showDialog` + `AlertDialog` |
| Snackbar | `ScaffoldMessenger.showSnackBar` |

---

## 14. Data Written on Save

| Field | Database column | Notes |
|---|---|---|
| Title | `tasks.title` | Required, non-empty |
| Date | `tasks.task_date` | Required |
| Start time | `tasks.start_time` | Optional |
| End time | `tasks.end_time` | Optional, requires start_time |
| List | `tasks.list_id` | Required, defaults to current list |
| Priority | `tasks.priority` | 0=None, 1=Low, 2=Medium, 3=High |
| Reminder | `reminders` table | Optional, created after task insert |
| Recurrence | `tasks.recurrence_rule` | Optional JSON string |
| Created at | `tasks.created_at` | Unix ms, set on creation |
| Updated at | `tasks.updated_at` | Unix ms, same as created_at on creation |

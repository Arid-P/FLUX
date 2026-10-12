# ui_SCR-02_task-detail-sheet.md
## FluxDone — Task Detail / Edit Sheet
**Screen ID:** SCR-02
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-02 is the full task editing surface. It opens when the user taps any task card in SCR-01 or any task block in SCR-04. It is a bottom sheet that expands to fill the full screen. All task fields are editable. Changes auto-save to the local database in real time — there is no explicit save button. The user closes the sheet via the X button or Android back gesture.

---

## 2. Entry Points

| Source | Behaviour |
|---|---|
| Tap task card (SCR-01) | Opens sheet for that task, all fields populated |
| Tap task block (SCR-04 Calendar) | Opens sheet for that task, all fields populated |
| "More" button from SCR-03 (collapsed) | Transitions SCR-03 to SCR-02 for the newly created task |

---

## 3. Sheet Behaviour

**Component:** `DraggableScrollableSheet` inside `showModalBottomSheet`
**Initial size:** 85% of screen height
**Expanded size:** 100% of screen height (full screen)
**Min size:** 85% (cannot collapse below initial)
**Corner radius:** 16dp top corners (85% state), 0dp (fully expanded)
**Background:** Surface color (white light / dark surface dark)
**Handle bar:** 4dp × 32dp, `#E0E0E0`, centered, 8dp from top

**Dismissal:**
- X button (`Icons.close`) in top app bar → sheet dismisses, all changes already auto-saved
- Android back gesture → same as X button
- Swipe down below 85% threshold → sheet dismisses
- No discard dialog — auto-save means all changes are always persisted

**Expansion trigger:**
- Drag handle dragged upward past 95% screen height → snaps to full screen
- Description field tap → auto-expands to full screen

---

## 4. Top App Bar (Sheet Header)

**Component:** Fixed `Row` at top of sheet (not a true `AppBar` — sheet has no `Scaffold`)
**Height:** 56dp
**Padding:** 16dp left, 16dp right
**Bottom border:** 1dp divider

### 4.1 Elements (left to right)

| Position | Component | Details |
|---|---|---|
| Left | Close button | `IconButton`, `Icons.close`, 20dp, secondary text color. Dismisses sheet. |
| Center | List name | `TextButton` showing current list name. 15sp, medium. Tap → opens list selector bottom sheet (Section 11). Trailing `Icons.expand_more` (14dp). |
| Right | Overflow menu | `IconButton`, `Icons.more_vert`, 20dp. Opens overflow menu (Section 12). |

---

## 5. Full Layout Structure

```
DraggableScrollableSheet
 ├── Handle bar
 ├── Sheet Header (fixed)
 │    ├── Close button
 │    ├── List name selector
 │    └── Overflow menu
 ├── Scrollable body
 │    ├── Checkbox + Title field row
 │    ├── Description editor
 │    ├── Divider
 │    ├── Date & Time row
 │    ├── Recurrence row
 │    ├── Reminder row
 │    ├── Priority row
 │    ├── Divider
 │    ├── Subtask section
 │    └── Bottom padding (88dp — clears keyboard)
 └── Rich text toolbar (docked above keyboard when description focused)
```

---

## 6. Checkbox + Title Field Row

**Padding:** 16dp left, 16dp right, 16dp top
**Layout:**

```
Row
 ├── Checkbox (20dp custom — see ICONS spec)
 ├── 12dp gap
 └── Title TextField (expanded)
```

### 6.1 Title TextField

**Component:** `TextField`
**Font:** 18sp, medium (500), primary text color
**Placeholder:** "Task title"
**Decoration:** None (`InputDecoration.collapsed`)
**Max lines:** Unbounded (wraps to multiple lines)
**Completed state:** Strikethrough, secondary text color, checkbox filled
**Auto-save:** Every keystroke triggers `updateTask()` with 500ms debounce

### 6.2 Checkbox Behaviour in SCR-02

Tapping the checkbox marks the task complete:
- Checkbox animates to filled state (see ICONS spec)
- Title text transitions to strikethrough + secondary color (300ms `easeOut`)
- Task remains visible in SCR-02 (does not disappear)
- Task is marked complete in database immediately
- On sheet close: task moves to Completed section in SCR-01

---

## 7. Description Editor

**Component:** `TextField` (multiline) with custom rich text toolbar
**Padding:** 16dp left, 16dp right, 8dp top
**Placeholder:** "Add description…"
**Placeholder color:** Secondary text color
**Font:** 14sp, regular, primary text color
**Min lines:** 3
**Max lines:** Unbounded (scrolls within sheet)
**Auto-save:** Every change triggers `updateTask()` with 500ms debounce

### 7.1 Rich Text Toolbar

**Visibility:** Appears only when description field is focused. Docked directly above the system keyboard.
**Component:** Custom `Row` in a `AnimatedContainer` that slides up with keyboard
**Height:** 48dp
**Background:** Surface color with 2dp top border (`#E0E0E0`)
**Horizontal scroll:** `SingleChildScrollView` — overflows horizontally if needed

**Toolbar items (left to right):**

| Item | Icon | Action | Toggle state |
|---|---|---|---|
| Bold | **B** (text, 16sp, semibold) | Wraps selection in `**` | Active: primary color background |
| Italic | *I* (text, 16sp, italic) | Wraps selection in `*` | Active: primary color background |
| Heading 1 | H1 (text, 14sp) | Prepends `# ` to line | Active: primary color background |
| Heading 2 | H2 (text, 14sp) | Prepends `## ` to line | Active: primary color background |
| Heading 3 | H3 (text, 14sp) | Prepends `### ` to line | Active: primary color background |
| Bullet list | `Icons.format_list_bulleted` (20dp) | Prepends `- ` to line | Active: primary color background |
| Numbered list | `Icons.format_list_numbered` (20dp) | Prepends `1. ` to line | Active: primary color background |
| Inline code | `Icons.code` (20dp) | Wraps selection in `` ` `` | Active: primary color background |

**Toolbar item specs:**
- Size: 40dp touch target, icon/text centered
- Default color: secondary text color
- Active/toggled color: app primary color
- Active background: app primary color at 12% opacity, 6dp corner radius
- Spacing: 4dp between items

### 7.2 Markdown Rendering

The description field renders markdown inline as the user types:

| Markdown | Renders as |
|---|---|
| `# text` | H1 — 20sp, bold |
| `## text` | H2 — 17sp, bold |
| `### text` | H3 — 15sp, bold |
| `**text**` | Bold |
| `*text*` | Italic |
| `- text` | Bullet list item with `•` prefix |
| `1. text` | Numbered list item |
| `` `text` `` | Inline code — monospace font, secondary bg |

---

## 8. Metadata Section

All metadata rows follow a shared pattern:

**Component:** `InkWell` wrapping `ListTile`
**Row height:** 52dp
**Padding:** 16dp left, 16dp right
**Divider:** 1dp between rows, `#E0E0E0` light / `#2C2C2C` dark

```
Row
 ├── Leading icon (20dp, secondary text color)
 ├── 16dp gap
 ├── Label + current value
 └── Trailing (chevron or none)
```

---

## 9. Date & Time Rows

### 9.1 Date Row

**Leading icon:** `Icons.calendar_today`, 20dp
**Label:** "Date"
**Value:** Formatted date (e.g., "Mar 18, 2026") or "No date" in secondary color
**Trailing:** `Icons.chevron_right`, 16dp, secondary color
**Tap:** Opens date picker bottom sheet (same as SCR-03 Section 6)

### 9.2 Start Time Row

**Leading icon:** `Icons.access_time`, 20dp
**Label:** "Start time"
**Value:** Formatted time (e.g., "9:00 PM") or "None"
**Trailing:** `Icons.chevron_right`
**Tap:** Opens clock face time picker (same as SCR-03 Section 7)
**Visibility:** Always visible (not conditional)

### 9.3 End Time Row

**Leading icon:** `Icons.access_time_filled`, 20dp
**Label:** "End time"
**Value:** Formatted time or "None"
**Trailing:** `Icons.chevron_right`
**Tap:** Opens clock face time picker
**Visibility:** Always visible
**Validation:** Must be after start time (same rule as SCR-03)

---

## 10. Recurrence Row

**Leading icon:** `Icons.repeat`, 20dp
**Label:** "Repeat"
**Value:** Current rule description (e.g., "Daily", "Weekly on Wed", "None")
**Trailing:** `Icons.chevron_right`
**Tap:** Opens recurrence picker bottom sheet

### 10.1 Recurrence Picker Bottom Sheet

**Component:** `showModalBottomSheet`
**Height:** 55% screen height

**Options list (RadioListTile):**

| Option | recurrence_rule JSON |
|---|---|
| None (default) | null |
| Daily | `{"type":"daily"}` |
| Weekly (current day) | `{"type":"weekly","days":[N]}` |
| Monthly (current date) | `{"type":"monthly_date","day":N}` |
| Yearly | `{"type":"yearly"}` |
| Every weekday (Mon–Fri) | `{"type":"weekly","days":[1,2,3,4,5]}` |
| Custom… | Opens custom recurrence screen |

**Custom recurrence screen (full screen route `/task/recurrence/custom`):**

```
Column
 ├── Top app bar: [← back] "Custom repeat"
 ├── Frequency row: "Every [N] [Day/Week/Month]"
 │    ├── Number picker (1–99, scroll wheel)
 │    └── Unit dropdown: Day / Week / Month
 ├── Day selector (visible when Week selected)
 │    └── Row of 7 day chips: M T W T F S S
 │         Each chip: 36dp circle, toggleable
 │         Selected: filled primary color
 │         Unselected: outlined secondary color
 └── Done button (full width, 48dp, primary color)
```

### 10.2 Recurring Task Edit Dialog

When editing a field on a task that has `recurrence_parent_id` set or has a `recurrence_rule`:

**Component:** `AlertDialog`
**Title:** "Edit recurring task"
**Options (RadioListTile):**
- "This task only"
- "This and future tasks"
- "All tasks in this series"

**Actions:** "Cancel", "Confirm"
This dialog appears before the edit is committed to the database.

---

## 11. Reminder Row

**Leading icon:** `Icons.notifications_outlined`, 20dp
**Label:** "Reminder"
**Value:** Current reminder description (e.g., "15 minutes before", "None")
**Trailing:** `Icons.chevron_right`
**Tap:** Opens reminder picker bottom sheet

### 11.1 Reminder Picker Bottom Sheet

**Component:** `showModalBottomSheet`
**Height:** 55% screen height

**Options (RadioListTile):**
- "None" (default)
- "At start time"
- "5 minutes before"
- "10 minutes before"
- "15 minutes before"
- "30 minutes before"
- "1 hour before"
- "1 day before"
- "Custom…" → opens time picker for absolute time

**Multiple reminders:** The user can add more than one reminder. After selecting an option, a "+ Add another reminder" row appears below the current reminder row in SCR-02. Each additional reminder shows as a separate row with a `Icons.close` (16dp) button to remove it.

---

## 12. Priority Row

**Leading icon:** `Icons.flag_outlined` (unfilled) or `Icons.flag` (filled, priority color), 20dp
**Label:** "Priority"
**Value:** "None", "Low", "Medium", or "High"
**Trailing:** None (dropdown anchored to row)
**Tap:** Opens priority dropdown anchored to the row (same dropdown spec as SCR-03 Section 10)

---

## 13. List Selector (via Header)

**Trigger:** Tap list name `TextButton` in sheet header
**Component:** Same list selector bottom sheet as SCR-03 Section 9

**On list change:**
- `tasks.list_id` updates immediately in database
- Sheet header list name label updates immediately
- Task card color in SCR-01 updates on next render
- If task was previously shown in SCR-01 for the old list, it disappears from that list and appears in the new list

---

## 14. Subtask Section

**Position:** Below priority row, above bottom padding
**Section header:**

```
Row
 ├── Icons.check_box_outlined (20dp, secondary color)
 ├── "Subtasks" (14sp, medium, secondary color)
 └── Subtask count "N/M" (13sp, secondary color, right-aligned)
```

### 14.1 Subtask List

**Component:** `ReorderableListView` (non-lazy)
**Each subtask row:**

```
Row (52dp height)
 ├── Drag handle (Icons.drag_handle, 20dp, secondary color at 60%)
 ├── Subtask checkbox (16dp, custom — see ICONS spec, smaller variant)
 ├── 8dp gap
 ├── Subtask title TextField (14sp, regular)
 └── Delete button (Icons.close, 16dp, secondary color — visible on focus)
```

**Subtask checkbox:**
- Same custom rounded square as task checkbox but 16dp visual size
- Border color: secondary text color (not list color — subtasks are list-agnostic)
- Checked fill: secondary text color
- Completed subtask title: strikethrough, secondary color at 60%

**Subtask title TextField:**
- No border decoration
- 14sp, regular
- Placeholder: "Subtask title"
- `TextInputAction.next` — creates next subtask on keyboard action
- Auto-save on every change (500ms debounce)

**Delete button:**
- Visible only when the subtask row is focused or on hover
- Tap → removes subtask from database, animates out (height collapse, 200ms)

**Drag to reorder:**
- Long press drag handle → lifts row (elevation 4dp, 200ms)
- Drag to new position → gap indicator
- Release → updates `subtasks.sort_order` in database

### 14.2 Add Subtask Row

Rendered below all existing subtasks.

```
Row (48dp height)
 ├── Icons.add (20dp, app primary color)
 └── "Add subtask" (14sp, regular, app primary color)
```

**Tap:** Adds a new empty subtask row at the bottom of the list, focuses its `TextField`, keyboard appears.

---

## 15. Overflow Menu

**Trigger:** `Icons.more_vert` in sheet header
**Component:** `PopupMenuButton` anchored to top-right

**Items:**

| Item | Icon | Action |
|---|---|---|
| Pin task | `Icons.push_pin_outlined` | Toggles `is_pinned` flag (Phase 2). Shows "Pinned" / "Unpin" based on state. |
| Duplicate | `Icons.content_copy` | Creates an identical copy of the task in the same list. Snackbar: "Task duplicated". |
| Share | `Icons.share_outlined` | Opens Android share sheet with task title and date as plain text. |
| Convert to note | `Icons.note_outlined` | Phase 2 — grayed out in Phase 1. |
| Delete | `Icons.delete_outline` (red `#E53935`) | Shows confirmation `AlertDialog`. On confirm: soft-deletes task to Trash, dismisses sheet, snackbar: "Task moved to Trash" with "Undo" action. |

**Delete confirmation `AlertDialog`:**
- Title: "Delete task?"
- Body: "This task will be moved to Trash."
- Actions: "Cancel", "Delete" (red text)

---

## 16. Auto-save Behaviour

All field changes trigger `ITaskRepository.updateTask()` with a **500ms debounce** — rapid changes are batched into a single database write.

**Fields that auto-save:**
- Title (debounced 500ms)
- Description (debounced 500ms)
- Subtask titles (debounced 500ms per subtask)
- Date, start time, end time (immediate on picker dismiss)
- Priority (immediate on dropdown selection)
- Reminder (immediate on picker dismiss)
- Recurrence (immediate on picker dismiss)
- Subtask completion (immediate on checkbox tap)

**No auto-save for:**
- List change (immediate but not "auto" — requires explicit tap)

---

## 17. Keyboard Behaviour

- When title or description field is focused: keyboard slides up, sheet content scrolls upward via `SingleChildScrollView` + `MediaQuery.viewInsets.bottom` padding
- Rich text toolbar docks above keyboard (only when description focused)
- Other metadata rows remain accessible by scrolling within the sheet body
- Keyboard dismiss: tap anywhere outside text fields, or use system keyboard dismiss gesture

---

## 18. Empty & Error States

### 18.1 Empty Description

- Placeholder "Add description…" shown in secondary text color
- Rich text toolbar does not appear until field is tapped

### 18.2 Empty Subtask List

- Only the "Add subtask" row shown, no empty state illustration

### 18.3 No Date Set

- Date row value: "No date" — secondary text color, italic
- Reminder row: disabled (40% opacity) — reminder requires a date

### 18.4 Recurring Task

- Recurrence row shows current rule in human-readable form
- Editing any date/time field on a recurring task triggers the edit scope dialog (Section 10.2)

---

## 19. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Sheet open | Slide up from bottom | 300ms | `easeOutCubic` |
| Sheet dismiss | Slide down | 250ms | `easeIn` |
| Sheet expand to full screen | Height 85% → 100% | 300ms | `easeOutCubic` |
| Corner radius on expand | 16dp → 0dp | 300ms | `easeOutCubic` |
| Keyboard appear | Content shifts up via `viewInsets` | Sync | — |
| Rich text toolbar appear | Slide up above keyboard | 200ms | `easeOut` |
| Rich text toolbar disappear | Slide down | 150ms | `easeIn` |
| Toolbar item active toggle | Background fade | 150ms | `easeInOut` |
| Checkbox complete | Fill + checkmark draw | 220ms | `easeOut` |
| Title strikethrough | Text decoration fade | 300ms | `easeOut` |
| Subtask add | Height expand + fade in | 200ms | `easeOut` |
| Subtask delete | Height collapse + fade out | 200ms | `easeIn` |
| Subtask drag lift | Elevation + scale | 200ms | `easeOut` |
| Subtask drag drop | Snap to position | 200ms | `easeOut` |
| Priority dropdown open | Fade + scale 0.9→1.0 | 150ms | `easeOut` |
| Overflow menu open | Fade + scale | 150ms | `easeOut` |
| Delete → sheet dismiss | Confirmation → dismiss | 300ms | `easeIn` |

---

## 20. Accessibility

- Title field: "Task title, required"
- Checkbox: "Mark [title] as complete / incomplete"
- Description: "Task description, optional"
- Date row: "Date: [value]. Tap to change."
- Priority row: "Priority: [value]. Tap to change."
- Subtask checkbox: "Mark subtask [title] as complete"
- Drag handle: "Drag to reorder subtask [title]"
- Close button: "Close task detail"
- Overflow menu: "More options"
- Minimum touch target: 48dp × 48dp

---

## 21. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Sheet container | `DraggableScrollableSheet` |
| Sheet header | Custom `Row` with `InkWell` items |
| Title field | `TextField` with `InputDecoration.collapsed` |
| Checkbox | Custom `CustomPainter` (ICONS spec) |
| Description field | `TextField` multiline |
| Rich text toolbar | Custom `Row` in `AnimatedContainer` above keyboard |
| Metadata rows | `ListTile` inside `InkWell` |
| Date picker | `showModalBottomSheet` with custom calendar |
| Time picker | `showModalBottomSheet` with clock face |
| Recurrence picker | `showModalBottomSheet` with `RadioListTile` list |
| Custom recurrence | Full screen route with `go_router` |
| Reminder picker | `showModalBottomSheet` with `RadioListTile` list |
| Priority dropdown | `PopupMenuButton` or custom `OverlayEntry` |
| List selector | `showModalBottomSheet` with `ListView` |
| Subtask list | `ReorderableListView` |
| Overflow menu | `PopupMenuButton` |
| Delete dialog | `showDialog` + `AlertDialog` |
| Snackbar | `ScaffoldMessenger.showSnackBar` |

---

## 22. Data Requirements

| Data | Source |
|---|---|
| Task (all fields) | `ITaskRepository.getTaskById(id)` |
| Subtasks | Loaded with task — joined query |
| Reminders | Loaded with task — joined query |
| All folders + lists | `IListRepository.getAllFolders()` + `getListsByFolderId()` |
| List color | `IListRepository.getColorHexByListId(listId)` |

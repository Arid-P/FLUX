# ui_SCR-08_side-drawer.md
## FluxDone — Side Drawer
**Screen ID:** SCR-08
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-08 is the primary navigation surface of FluxDone. It provides access to all Folders, Lists, and Smart Lists, and serves as the entry point for list and folder management. In portrait mode it is a modal drawer sliding in from the left. In landscape mode it is a permanent navigation rail always visible on the left edge of the screen.

---

## 2. Navigation & Entry Points

- **Portrait:** Hamburger button (`Icons.menu`) in any screen's top app bar
- **Portrait dismiss:** Tap the scrim to the right, swipe left on drawer, or Android back gesture
- **Landscape:** Always visible — no open/close action required
- **Transition:** Horizontal slide-in from left (300ms, `easeInOut`) with scrim fade-in

---

## 3. Overall Layout

### 3.1 Portrait — Modal Drawer

```
┌──────────────────────┬──────────────────┐
│                      │                  │
│  Side Drawer         │  Scrim           │
│  (80% screen width)  │  (#00000066)     │
│                      │                  │
│                      │                  │
└──────────────────────┴──────────────────┘
```

**Width:** 80% of screen width
**Max width:** 320dp (caps on large screens)
**Background:** Surface color (white light / dark surface dark)
**Elevation:** 16dp (casts shadow onto scrim)

### 3.2 Landscape — Permanent Navigation Rail

**Width:** 280dp (fixed)
**Background:** Surface color
**Elevation:** 0dp (flat — separated from content by 1dp right border)
**Always visible:** No open/close. Content area fills remaining width.

---

## 4. Drawer Structure (top to bottom)

```
Column
 ├── Header (App logo + name)
 ├── Divider
 ├── Scrollable List Body
 │    ├── LISTS section
 │    │    └── Folders with nested Lists (expandable)
 │    ├── Divider
 │    ├── SMART LISTS section
 │    │    └── Today / Tomorrow / Upcoming / All / Completed / Trash
 │    └── Add List / Folder button
 └── Settings shortcut (pinned to bottom)
```

---

## 5. Header

**Height:** 72dp
**Padding:** 16dp left, 16dp right, 12dp top, 12dp bottom
**Background:** Surface color (same as drawer — no distinct header color)

### 5.1 Header Anatomy

```
Row
 ├── App logo (32dp × 32dp)
 └── App name Text
```

| Element | Details |
|---|---|
| App logo | FD logo asset, 32dp × 32dp, corner radius 8dp |
| App name | "FluxDone", 20sp, semibold (600), primary text color |
| Spacing between logo and name | 12dp |

---

## 6. Section Labels

**Component:** `Padding` + `Text`
**Height:** 32dp
**Padding:** 16dp left, 8dp bottom
**Typography:** 11sp, semibold (600), all caps, secondary text color (`#757575` light / `#9E9E9E` dark)
**Not tappable**

Two section labels used:
- "LISTS" — above the folders/lists block
- "SMART LISTS" — above the smart lists block

---

## 7. Folder Row

**Component:** Custom `InkWell` wrapping `Row`
**Height:** 44dp
**Padding:** 16dp left, 16dp right

### 7.1 Folder Row Anatomy

```
Row
 ├── Folder icon (20dp)
 ├── Folder name Text
 ├── Spacer
 ├── Task count badge Text
 └── Chevron icon (16dp, rotates on expand)
```

| Element | Typography / Size | Color |
|---|---|---|
| Folder icon | `Icons.folder_outlined`, 20dp | Secondary text color |
| Folder name | 14sp, medium (500) | Primary text color |
| Task count | 12sp, regular | Secondary text color |
| Chevron | `Icons.expand_more`, 16dp | Secondary text color |

### 7.2 Folder Expand/Collapse

- **Default state:** Restored from `shared_preferences` key: `folder_{id}_expanded`
- **Tap behavior:** Toggles expanded/collapsed state
- **Chevron animation:** Rotates 180° on collapse, 180° back on expand — 200ms `easeInOut`
- **Expand/collapse animation:** `AnimatedSize`, 250ms, `easeInOut`

### 7.3 Folder Long Press

Long press (300ms) on a folder row opens a context `BottomSheet` with:
- "Rename folder" → opens rename `AlertDialog`
- "Change color" → opens color picker bottom sheet
- "Reorder" → activates drag-to-reorder mode (see Section 12)
- "Delete folder" → opens confirmation `AlertDialog`

---

## 8. List Row

Nested inside its parent folder, indented to indicate hierarchy.

**Component:** Custom `InkWell` wrapping `Row`
**Height:** 44dp
**Left indent:** 32dp (16dp base + 16dp nesting indent)
**Right padding:** 16dp

### 8.1 List Row Anatomy

```
Row
 ├── Color swatch (12dp circle)
 ├── List name Text
 ├── Spacer
 └── Task count badge Text
```

| Element | Details |
|---|---|
| Color swatch | Filled circle, 12dp diameter, list color (`color_hex`) |
| Left border | 3dp left border on the row itself, list color |
| List name | 14sp, regular (400), primary text color |
| Task count | 12sp, regular, secondary text color |

### 8.2 Active List Indicator

When this list is the currently viewed list:
- **Background fill:** App primary color at 12% opacity, spanning full row width
- **Corner radius:** 8dp on the background fill

### 8.3 List Row Long Press

Long press (300ms) on a list row opens a context `BottomSheet` with:
- "Rename list" → opens rename `AlertDialog`
- "Change color" → opens hex color picker bottom sheet
- "Move to folder" → opens folder picker bottom sheet
- "Reorder" → activates drag-to-reorder mode
- "Delete list" → opens confirmation `AlertDialog`

### 8.4 List Row Tap

Tapping a list row:
1. Highlights the row (active state)
2. Navigates to SCR-01 (List View) for that list
3. In portrait: closes the drawer (300ms slide-out)
4. In landscape: drawer stays open, content area updates

---

## 9. Smart Lists Section

Rendered below the Lists section, separated by a full-width 1dp `Divider`.

### 9.1 Smart List Rows

**Component:** `InkWell` wrapping `Row`
**Height:** 44dp
**Padding:** 16dp left, 16dp right (no indent — same level as folders)

```
Row
 ├── Smart list icon (20dp)
 ├── Smart list name Text
 ├── Spacer
 └── Task count badge Text
```

| Smart List | Icon | Count shown |
|---|---|---|
| Today | `Icons.today_outlined` | Tasks due today |
| Tomorrow | `Icons.event_outlined` | Tasks due tomorrow |
| Upcoming | `Icons.date_range_outlined` | Tasks in next 7 days |
| All | `Icons.list_alt_outlined` | All incomplete tasks |
| Completed | `Icons.check_circle_outline` | All completed tasks |
| Trash | `Icons.delete_outline` | All trashed tasks |

**Typography:** 14sp, regular, primary text color
**Count badge:** 12sp, regular, secondary text color

**Active state:** Same background fill as user list rows (primary color at 12% opacity, 8dp radius).

---

## 10. Add List / Folder Button

Rendered at the bottom of the scrollable list body, below Smart Lists.

**Component:** `TextButton` with leading icon
**Height:** 44dp
**Padding:** 16dp left

```
Row
 ├── Icons.add (20dp, app primary color)
 └── "Add List or Folder" Text (14sp, app primary color)
```

**Tap behavior:** Opens a small `AlertDialog` or `BottomSheet` asking:
- "New List" button → opens inline list creation (see SCR-11)
- "New Folder" button → opens inline folder creation (see SCR-10)

---

## 11. Settings Shortcut

**Component:** `InkWell` wrapping `Row`
**Position:** Pinned to bottom of drawer (not scrollable — fixed at bottom)
**Height:** 56dp
**Top border:** 1dp `Divider`
**Padding:** 16dp left, 16dp right

```
Row
 ├── Icons.settings_outlined (20dp)
 └── "Settings" Text (14sp, regular)
```

**Tap behavior:** Navigates to SCR-09 (Settings Screen). In portrait: closes drawer first (200ms), then navigates.

---

## 12. Drag-to-Reorder

Activated via "Reorder" in the long-press context menu for a folder or list.

**On activation:**
- Drag handles (`Icons.drag_handle`, 20dp, secondary text color) appear on right side of all rows of the same type (folders if reordering folders, lists if reordering lists within a folder)
- All long-press context menus are temporarily disabled

**Drag behavior:**
- Long press drag handle to lift row (elevation 8dp, scale 1.02, 200ms)
- Drag to new position — gap indicator shows insertion point
- On release: `sort_order` updates in database for all affected rows

**Exit reorder mode:**
- Tap anywhere outside a drag handle
- Tap a "Done" `TextButton` that replaces the section header during reorder mode

---

## 13. Rename Dialog

**Component:** `showDialog` + `AlertDialog`
**Title:** "Rename folder" or "Rename list"
**Content:** `TextField` pre-filled with current name, autofocused, single line
**Actions:**
- "Cancel" — dismisses dialog, no change
- "Save" — validates non-empty, updates name in database, dismisses

**TextField specs:**
- Border: underline style (Material default)
- Max length: 50 characters
- Input action: `TextInputAction.done` — submits form

---

## 14. Color Picker Bottom Sheet

**Component:** `showModalBottomSheet`
**Title:** "Choose color"
**Corner radius:** 16dp top corners

**Content:**
1. **Preset color row** — horizontal scrollable row of 12 preset color swatches (40dp circles, 8dp gap between)
2. **Hex input field** — `TextField` with `#` prefix, accepts 6-character hex input, live preview swatch (24dp circle) to the right of the field
3. **Current color preview** — 48dp circle showing current selection

**Preset colors (12 swatches):**
`#2E7D32`, `#1565C0`, `#43A047`, `#FB8C00`, `#E64A19`, `#E53935`, `#576481`, `#7B1FA2`, `#00838F`, `#F57F17`, `#5E35B1`, `#546E7A`

**Confirm button:** "Apply" — full width, app primary color, 48dp height, 8dp corner radius. Applies color and dismisses sheet.

---

## 15. Delete Confirmation Dialog

**Component:** `AlertDialog`

**For folder deletion:**
- Title: "Delete folder?"
- Body: "This will permanently delete the folder and all its lists and tasks. This cannot be undone."
- Actions: "Cancel", "Delete" (red text `#E53935`)

**For list deletion:**
- Title: "Delete list?"
- Body: "This will permanently delete the list and all its tasks. This cannot be undone."
- Actions: "Cancel", "Delete" (red text `#E53935`)

---

## 16. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Drawer open (portrait) | Slide in from left | 300ms | `easeInOut` |
| Drawer close (portrait) | Slide out to left | 300ms | `easeInOut` |
| Scrim fade in | Opacity 0 → 0.4 | 300ms | `easeInOut` |
| Scrim fade out | Opacity 0.4 → 0 | 300ms | `easeInOut` |
| Folder expand | `AnimatedSize` height increase | 250ms | `easeInOut` |
| Folder collapse | `AnimatedSize` height decrease | 250ms | `easeInOut` |
| Chevron rotate | 180° rotation | 200ms | `easeInOut` |
| Active list highlight | Background fade in | 150ms | `easeOut` |
| Context bottom sheet | Slide up | 300ms | Spring |
| Color picker sheet | Slide up | 300ms | Spring |
| Drag lift | Elevation + scale | 200ms | `easeOut` |
| Drag drop | Snap to position | 200ms | `easeOut` |

---

## 17. Accessibility

- Folder rows: "Folder: [name], [N] tasks, [expanded/collapsed]"
- List rows: "List: [name], [N] tasks, [selected/not selected]"
- Smart list rows: "[name], [N] tasks"
- Settings shortcut: "Open Settings"
- Add button: "Add new list or folder"
- Drag handle: "Drag to reorder [name]"
- Minimum touch target: 48dp × 48dp

---

## 18. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Portrait drawer | `Drawer` inside `Scaffold.drawer` |
| Landscape rail | Permanent `NavigationDrawer` or custom `Container` |
| Scrollable body | `ListView` |
| Folder row | Custom `InkWell` + `AnimatedRotation` for chevron |
| Folder children | `AnimatedSize` wrapping `Column` |
| List row | Custom `InkWell` + `Container` with left border |
| Drag reorder | `ReorderableListView` |
| Rename dialog | `showDialog` + `AlertDialog` + `TextField` |
| Color picker | `showModalBottomSheet` with custom content |
| Context menu | `showModalBottomSheet` with `ListTile` options |
| Delete dialog | `showDialog` + `AlertDialog` |

---

## 19. Data Requirements

| Data | Source |
|---|---|
| All folders | `IListRepository.getAllFolders()` |
| Lists per folder | `IListRepository.getListsByFolderId(folderId)` |
| Incomplete task count per list | `ITaskRepository` — count query per list |
| Smart list counts | `SmartListQueryService` |
| Folder expanded states | `shared_preferences` key: `folder_{id}_expanded` |
| Currently active list ID | Navigation state via `go_router` |

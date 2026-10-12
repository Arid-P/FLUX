# FD Phase 4 — Feature 5: Multi-Select + Bulk Time-Shift on Calendar View

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Multi-select allows the user to select multiple timed task blocks on the Calendar View (SCR-04) and move them together as a group, maintaining their relative time gaps. This solves the primary use case of rescheduling a large block of planned tasks (e.g. 2 weeks of study sessions) after a missed day or schedule disruption — without moving each task one by one.

---

## 2. P1/P4 References

| Reference | Location |
|---|---|
| Calendar View — SCR-04 | `ui_SCR-04_calendar-view.md` |
| SCR-04 deep-link amendment | `FD_P3_F2_widgets-scr04-amendment.md` |
| Drag-to-reschedule (P2) | PRD v2 §7.2 |
| FocusBlockRequest UPDATE | PRD v2 §7.1, FLUX_CONTEXT.md §5 |
| Focus overlap warning | `FD_P4_F1_focus-overlap-warning.md` |

---

## 3. Feature Specification

### 3.1 Activation

A new button appears in SCR-04's top app bar: a dashed rectangle icon (□ with dashed border, ~24dp).

| State | Icon appearance |
|---|---|
| Inactive | Dashed rectangle, secondary text color |
| Active | Dashed rectangle, app primary color, filled background chip |

Tap to activate → tap again to deactivate. Deactivation clears all current selections.

### 3.2 Selection

When multi-select mode is active:
- Tap any timed task block → selected (block gains a primary color border overlay, 2dp, + checkmark badge on top-right corner)
- Tap a selected block again → deselected
- Only **timed tasks** (tasks with both `start_time` and `end_time`) are selectable
- Untimed tasks, all-day tasks, FF session overlays, and Google Calendar events are not selectable
- A selection count chip appears in the top app bar: *"[N] selected"*
- Normal calendar navigation (swipe between days, mode switching) remains active during selection

### 3.3 Move Trigger

Long-press any **selected** task block → drag begins for the entire group.

- The long-pressed block is the **anchor block** — its position during drag determines where all other blocks land
- All selected blocks move in unison, maintaining their relative time offsets from the anchor
- Non-selected blocks remain static

### 3.4 Relative Gap Preservation

```dart
// On drag start
final anchorOriginalStart = anchorBlock.startTime;
final offsets = selectedBlocks.map((block) =>
  block.startTime.difference(anchorOriginalStart)
).toList();

// On drag update (anchor lands at newAnchorStart)
for (int i = 0; i < selectedBlocks.length; i++) {
  selectedBlocks[i].proposedStart = newAnchorStart + offsets[i];
  selectedBlocks[i].proposedEnd = proposedStart + selectedBlocks[i].duration;
}
```

All blocks snap to 15-minute grid intervals (same as single task drag — PRD v2 §7.1 step 5).

### 3.5 Cross-Date Navigation During Drag

When dragging toward the left or right edge of the visible calendar:
- **Auto-scroll:** calendar shifts to the next/previous day column when the dragged block reaches within 40dp of the edge
- Scroll speed: constant — 1 day column per 0.5 seconds of edge contact
- No maximum scroll distance — user can drag across any number of days
- The anchor block's ghost follows the finger; other selected blocks' ghosts move off-screen and re-appear as the calendar scrolls to their new positions

### 3.6 Overlap Warning During Drag

If any proposed block position overlaps an FF focus session (using the same overlap detection as FD P4 F1):
- That specific block's ghost turns amber (warning state — same as F1)
- Does not block the move — informational only
- On drop, FocusBlockRequest (UPDATE) is sent for each moved task as normal

### 3.7 Drop and Confirm

On release (lift finger):
- All selected blocks snap to their final 15-minute grid positions
- A confirmation bottom sheet appears:

```
─────────────────────────────────
  Move [N] tasks?

  Anchor: [task name]
  [Original date/time] → [New date/time]
  
  + [N-1] other tasks shifted by [±Xh Ym / ±N days]

  [Cancel]        [Move Tasks]
─────────────────────────────────
```

- **Cancel:** blocks animate back to original positions. Selection preserved
- **Move Tasks:** all tasks updated in SQLite. FocusBlockRequest (UPDATE) sent to FF for each task with a linked FocusSession. Selection cleared after successful move

### 3.8 Selection Persistence

After a successful move:
- Selection **persists** — the same group of tasks remains selected
- User can immediately drag again to further adjust
- Selection clears only when: user taps the deactivate button, user taps outside all selected blocks, or user navigates away from SCR-04

### 3.9 Undo

After a move is confirmed, a snackbar appears:
- *"[N] tasks moved. Undo"*
- Undo available for **10 seconds**
- Tapping Undo: reverses all task time updates atomically (SQLite transaction rollback equivalent — re-apply original times)
- After 10 seconds: snackbar dismisses, undo no longer available

---

## 4. Database Impact

No schema changes. The move operation updates existing fields on the `tasks` table:
- `task_date`
- `start_time`
- `end_time`
- `updated_at`

All N task updates are wrapped in a single SQLite transaction — atomic success or atomic rollback.

---

## 5. FF IPC Impact

For each moved task that has a linked FocusSession in FF:
- FD sends `FocusBlockRequest` with `action: UPDATE` and updated `startTime` / `endTime`
- Sent after the SQLite transaction commits successfully
- Sent sequentially (not in batch) — FF processes each update independently
- If FF IPC fails for any task: logged silently, no retry in P4 (retry mechanism is a P5 consideration)

---

## 6. Module Boundary

**Owned by:** `calendar/` module (extended from P1/P4 F1)

```
features/
└── calendar/
    ├── domain/
    │   ├── multi_select_state.dart          ← NEW
    │   └── use_cases/
    │       └── bulk_move_tasks.dart         ← NEW
    └── presentation/
        ├── calendar_screen.dart             ← MODIFIED (multi-select button, selection state)
        ├── task_block.dart                  ← MODIFIED (selected state, checkmark badge)
        ├── multi_select_drag_handler.dart   ← NEW
        └── bulk_move_confirmation_sheet.dart ← NEW
```

Modifications to existing modules:
- `fluxfoxus_bridge/` — bulk UPDATE FocusBlockRequest sending after move confirms

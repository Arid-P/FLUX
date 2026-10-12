# FD Phase 4 — Feature 1: Focus Time Blocking Overlap Warning

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

When the user creates a timed task on the Calendar View (SCR-04) that overlaps with an existing FluxFoxus focus session, FD shows an inline warning on the calendar before the task is submitted. The user is informed of the conflict and can proceed or adjust the time.

---

## 2. P1/P2 References

| Reference | Location |
|---|---|
| Calendar View — SCR-04 | `ui_SCR-04_calendar-view.md` |
| SCR-04 deep-link amendment | `FD_P3_F2_widgets-scr04-amendment.md` |
| FD → FF IPC — FocusBlockRequest | PRD v2 §7.1, FLUX_CONTEXT.md §5 |
| Ghost block — tap to create flow | PRD v2 §7.1 |
| FF focus sessions | FF_PRD_v1_0.md §3.2 |
| MethodChannel IPC | TRD v2 §3.3, FLUX_CONTEXT.md §5 |

---

## 3. Feature Specification

### 3.1 What Triggers the Warning

A warning is shown when:
- User taps or long-presses an empty time slot on SCR-04 to create a timed task
- The ghost block's time range overlaps with one or more existing FF focus sessions on the same date
- FF is installed and reachable via MethodChannel

No warning is shown if:
- FF is not installed
- FF is installed but IPC call fails (silent — no warning, no error)
- The task being created has no end time (untimed tasks don't create time blocks)

### 3.2 How FF Session Data Is Fetched

FD queries FF for existing focus sessions in the visible calendar date range via a new MethodChannel call on `com.fluxfoxus/fd_integration`:

**FD → FF request:**
```dart
// Method: 'getFocusSessions'
// Arguments:
{
  'startDate': int,   // Unix ms
  'endDate': int,     // Unix ms
}
```

**FF response:**
```dart
// Returns List of:
{
  'sessionId': String,
  'sessionName': String,
  'startTime': int,   // Unix ms
  'endTime': int,     // Unix ms
  'presetName': String,
}
```

This call is made once when SCR-04 opens and cached for the visible date range. Re-fetched on calendar navigation or pull-to-refresh.

### 3.3 Overlap Detection

```dart
bool overlaps(TimeRange ghostBlock, TimeRange ffSession) {
  return ghostBlock.start < ffSession.end && 
         ghostBlock.end > ffSession.start;
}
```

Evaluated in real time as the user drags the ghost block bottom edge to set end time.

### 3.4 Warning Display

When an overlap is detected, the ghost block gains a visual warning state:

| Property | Normal ghost block | Warning ghost block |
|---|---|---|
| Background | Primary color at 30% opacity | Amber (`#F59E0B`) at 30% opacity |
| Border | Dashed primary color | Dashed amber |
| Label inside block | Time range | Time range + ⚠️ icon |

Below the ghost block, a small inline label appears:
- *"⚠️ Overlaps with [session name] ([start]–[end])"*
- Font: 11sp, amber color
- If multiple overlaps: *"⚠️ Overlaps with [N] focus sessions"*

### 3.5 User Can Proceed

The warning is **non-blocking**. The user can still tap submit and create the task even with an overlap. The warning is informational only — FD does not prevent the creation.

When task is submitted with an overlap:
- Task is created normally
- FocusBlockRequest (CREATE) is sent to FF as normal (PRD v2 §7.1 step 9)
- FF handles the overlapping session conflict on its side (FF's responsibility, not FD's)

---

## 4. FF Session Rendering on Calendar (Bonus Display)

As a natural extension of fetching FF session data, FF sessions are rendered as a subtle overlay on SCR-04's timeline:

- **Style:** Semi-transparent block, Electric Indigo (`#6366F1`) at 15% opacity, no border
- **Label:** FF aperture icon (small, 12dp) + session name, truncated
- **Non-interactive:** tapping an FF session block has no action in FD
- **Distinguishable:** FF session blocks are clearly visually distinct from FD task blocks (which are list-colored, fully opaque)

This gives the user a complete picture of their day — FD tasks + FF sessions — without leaving the calendar.

---

## 5. New MethodChannel Call

Added to `com.fluxfoxus/fd_integration` channel (TRD v2 §3.3):

| Method | Direction | Purpose |
|---|---|---|
| `getFocusSessions` | FD → FF | Fetch FF sessions for a date range |

This is the second FD → FF query call alongside the existing `getTaskBlocks` (added in P3 F2 Session Templates).

---

## 6. Module Boundary

**Owned by:** `calendar/` module (extended from P1) + `fluxfoxus_bridge/` module (extended from P1)

```
features/
└── calendar/
    ├── data/
    │   └── calendar_repository_impl.dart   ← EXTENDED (FF session fetch + cache)
    ├── domain/
    │   ├── ff_session_block.dart            ← NEW model
    │   └── use_cases/
    │       └── get_ff_sessions.dart         ← NEW
    └── presentation/
        ├── calendar_screen.dart             ← MODIFIED (FF session overlay rendering)
        ├── ghost_block.dart                 ← MODIFIED (warning state)
        └── overlap_warning_label.dart       ← NEW

features/
└── fluxfoxus_bridge/
    └── data/
        └── method_channel_service.dart     ← EXTENDED (getFocusSessions method)
```

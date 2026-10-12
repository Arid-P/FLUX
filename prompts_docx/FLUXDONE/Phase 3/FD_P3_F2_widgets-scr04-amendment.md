# FD Phase 3 — Widget Amendment 2: SCR-04 Deep-Link Amendment

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  
**Amends:** `ui_SCR-04_calendar-view.md` — Phase 3 addition

---

## 1. Overview

WIDGET-02 (3-Day Calendar Widget) navigates to SCR-04 (Calendar View) with deep-link parameters that SCR-04 does not currently handle. This document specifies the required SCR-04 behaviour when launched from the widget.

This is a Phase 3 addition. SCR-04's Phase 1 behaviour is unchanged.

---

## 2. New Deep-Link Parameters

SCR-04 must handle the following parameters when launched via deep-link from WIDGET-02:

| Scenario | Parameter | Format | Example |
|---|---|---|---|
| Tap on task block | `taskId` | String (task UUID) | `taskId=abc-123` |
| Tap on task block | `date` | `yyyy-MM-dd` | `date=2026-03-21` |
| Tap on task block | `time` | `HH:mm` (24h) | `time=15:00` |
| Tap on empty time slot | `date` | `yyyy-MM-dd` | `date=2026-03-21` |
| Tap on empty time slot | `time` | `HH:mm` (24h) | `time=14:30` |
| Tap on month label | *(no parameters)* | — | Opens at current date |

---

## 3. Required SCR-04 Behaviour on Launch

### 3.1 Task block tap (`taskId` + `date` + `time` present)

1. SCR-04 opens in the view mode that was last active (Day / 3-day / Week)
2. Timeline scrolls to the position of `time` within the `date` column
3. The task identified by `taskId` is visually highlighted:
   - Task block renders with a 2dp Electric Indigo border overlay
   - Highlight persists for 2 seconds then fades out (300ms fade)
4. If `taskId` does not exist (deleted between widget render and tap): SCR-04 opens at `date`/`time` with no highlight, no error shown

### 3.2 Empty time slot tap (`date` + `time` present, no `taskId`)

1. SCR-04 opens in the view mode that was last active
2. Timeline scrolls to `time` within the `date` column
3. A ghost block appears at the tapped time position (same ghost block as the normal tap-to-create flow — PRD v2 §7.1, step 4)
4. Task creation sheet (SCR-03) opens with `date` and `time` pre-filled
5. This mirrors the standard calendar tap-to-create flow exactly

### 3.3 Month label tap (no parameters)

1. SCR-04 opens in the view mode that was last active
2. Timeline scrolls to current time of day
3. No highlight, no ghost block

---

## 4. go_router Integration

Deep-link URI format from widget:

```
fluxdone://calendar?taskId=[id]&date=[yyyy-MM-dd]&time=[HH:mm]
fluxdone://calendar?date=[yyyy-MM-dd]&time=[HH:mm]
fluxdone://calendar
```

Route handler in `app_router.dart`:

```dart
GoRoute(
  path: '/calendar',
  builder: (context, state) {
    final taskId = state.uri.queryParameters['taskId'];
    final dateStr = state.uri.queryParameters['date'];
    final timeStr = state.uri.queryParameters['time'];

    return CalendarScreen(
      initialTaskId: taskId,
      initialDate: dateStr != null ? DateTime.parse(dateStr) : null,
      initialTime: timeStr != null ? _parseTime(timeStr) : null,
    );
  },
),
```

---

## 5. CalendarScreen Parameter Handling

`CalendarScreen` receives three new optional constructor parameters:

```dart
class CalendarScreen extends StatefulWidget {
  final String? initialTaskId;   // If set: scroll to task and highlight
  final DateTime? initialDate;   // If set: scroll to this date
  final TimeOfDay? initialTime;  // If set: scroll to this time within the date

  const CalendarScreen({
    this.initialTaskId,
    this.initialDate,
    this.initialTime,
    super.key,
  });
}
```

These parameters are consumed once on `initState` and not persisted. Normal calendar navigation is unaffected after the initial scroll.

---

## 6. Notes

- This amendment only adds new behaviour triggered by deep-link parameters. All existing SCR-04 interactions (tap, long-press, drag, mode switching) are unchanged
- The task highlight animation (2dp border + 2s fade) is the only new visual element added to SCR-04 in Phase 3
- Parameters are passed via Android `Intent` extras wrapped in the `home_widget` PendingIntent mechanism — not via URL scheme on older Android versions. The go_router URI format above is the Flutter-side representation

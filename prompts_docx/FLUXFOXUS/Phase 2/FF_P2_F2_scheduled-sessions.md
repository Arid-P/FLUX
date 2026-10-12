# FF Phase 2 — Feature 2: Scheduled Sessions

**Version:** 1.0  
**Phase:** 2  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Scheduled Sessions allow the user to define planned focus sessions directly in FF with a specific start time, end time, a linked preset, and an optional recurrence rule. This is distinct from the two P1 session types:

| Type | Abbreviation | Origin | P1 or P2 |
|---|---|---|---|
| Manual Session | msession | User taps "Start Focusing" on Home screen | P1 |
| Task Session | tsession | Created automatically from FD task via FocusBlockRequest | P1 |
| **Scheduled Session** | **ssession** | User creates in FF directly with defined time window | **P2** |

ssessions are the FF-native equivalent of FD's timed tasks. They give FF standalone scheduling capability without requiring FD.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Preset data model and properties | PRD §3.1.2, TRD §3.1 |
| FocusSession data model | TRD §3.2 |
| Block Scheduling — FD → FF sync rules | PRD §3.8.2 |
| 15-minute heads-up notification | PRD §3.8.3 |
| Planner screen — session card display | PRD §3.10.3 |
| Session source enum (`ff` / `fd`) | TRD §3.2 |
| go_router route definitions | TRD §6 |

---

## 3. Feature Specification

### 3.1 What is a Scheduled Session

A Scheduled Session is a user-defined focus block with:
- A name (required)
- A linked preset (required)
- A start time (required)
- An end time (required — duration is derived, not set directly)
- An optional recurrence rule
- An optional description

ssessions appear in the Planner screen (PRD §3.10) alongside tsessions and completed msessions. They behave identically to tsessions in terms of the 15-minute heads-up notification, Planner display, and session card format.

**ssessions do NOT send a FocusBlockRequest to FD.** They are FF-native. FD has no awareness of ssessions.

### 3.2 Creation Flow

**Entry point:** Planner screen → FAB ("Add Session") — replaces the P1 "Add Preset" FAB label

A bottom sheet opens with the following fields:

| Field | Type | Constraints |
|---|---|---|
| Session name | Text input | Required, max 64 chars |
| Preset | Dropdown selector | Required. Shows all saved presets (emoji + name). Last-used preset pre-selected |
| Date | Date picker | Required. Defaults to today |
| Start time | Time picker | Required |
| End time | Time picker | Required. Must be after start time. Minimum duration: 15 minutes |
| Recurrence | Chips / picker | Optional. See §3.4 |
| Description | Text input | Optional, free text |

**Validation:**
- End time before start time: inline error, submit blocked
- Duration < 15 minutes: inline error *"Session must be at least 15 minutes"*, submit blocked
- No preset selected: submit blocked
- Overlap with existing ssession on same day: yellow warning (non-blocking) — *"This overlaps with [session name]"*. User can proceed

**On submit:** ssession saved to SQLite. Planner refreshes. 15-minute heads-up notification scheduled via `android_alarm_manager_plus` (TRD §1.4).

### 3.3 Editing a Scheduled Session

Tap a Planner session card → session detail bottom sheet opens.

For ssessions (not tsessions, not msessions):
- Edit button available (pencil icon, top right)
- Opens same creation bottom sheet, pre-filled
- If session is recurring: edit scope dialog appears (identical pattern to FD's recurring task edit scope):
  - *"This session only"*
  - *"This and future sessions"*
  - *"All sessions in series"*
- Completed or active ssessions: edit button hidden (read-only)

### 3.4 Recurrence Rules

Matches FD's recurrence model for consistency (FD PRD §5, feature FD-07).

| Rule | Options |
|---|---|
| Frequency | Daily / Weekly / Monthly |
| Interval | Every N days/weeks/months (N: 1–30) |
| Days of week | Multi-select (Mon–Sun), only for Weekly |
| End condition | Never / On date / After N occurrences |

Recurrence rules stored as JSON string in `recurrence_rule` column (same approach as FD TRD §5.1 — avoids a separate recurrence table in P2).

**Recurring session generation:** instances are generated on-demand for the next 60 days whenever a new ssession series is created or edited. A WorkManager periodic task regenerates upcoming instances every 7 days.

### 3.5 Deleting a Scheduled Session

Long-press on Planner session card → context menu → Delete.

If recurring: delete scope dialog (same as edit scope: this only / this and future / all in series).

Completed ssessions cannot be deleted (they are part of session history). They can only be dismissed from the Planner view.

### 3.6 Session Start Behaviour

When a scheduled ssession's start time arrives:
1. 15-minute heads-up notification fires (PRD §3.8.3 — same as tsession)
2. At start time: a second notification fires — *"[Session name] is starting now"* with action: "Start Session"
3. Tapping "Start Session": opens FF home screen with the ssession's preset pre-loaded, user taps "Start Focusing" to begin
4. Session does not auto-start — user must explicitly tap to begin (consistent with P1 msession behaviour)
5. If user does not start within 30 minutes of scheduled start: session status → `skipped`, Planner shows it as skipped with a grey style

### 3.7 Display in Planner

ssessions use the same session card format as tsessions (PRD §3.10.3) with one distinction:

| Field | tsession | ssession |
|---|---|---|
| Source badge | FD | FF |
| Sync icon (↻) | Yes | No |
| Edit available | No (read-only, managed by FD) | Yes |
| Recurrence indicator | If recurring in FD | If recurring in FF |

---

## 4. Data Model

### 4.1 New Model: ScheduledSession

```dart
@freezed
class ScheduledSession with _$ScheduledSession {
  const factory ScheduledSession({
    required String id,              // UUID
    required String name,            // Max 64 chars
    required String presetId,
    required DateTime startTime,
    required DateTime endTime,
    String? recurrenceRule,          // JSON string, null if non-recurring
    String? seriesId,                // UUID linking recurring instances
    String? description,
    required ScheduledSessionStatus status, // pending / active / completed / skipped
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _ScheduledSession;
}

enum ScheduledSessionStatus { pending, active, completed, skipped }
```

### 4.2 Relationship to FocusSession (TRD §3.2)

When a ssession is started by the user, a `FocusSession` record is created (same as P1) with:
- `source = SessionSource.ff`
- `fdTaskId = null`
- `scheduledStart` and `scheduledEnd` populated from the `ScheduledSession`

The `ScheduledSession` record is updated: `status = active`. On session end: `status = completed`.

---

## 5. Database Schema Changes

### 5.1 New Table: scheduled_sessions

```sql
CREATE TABLE scheduled_sessions (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  preset_id TEXT NOT NULL,
  start_time INTEGER NOT NULL,
  end_time INTEGER NOT NULL,
  recurrence_rule TEXT,
  series_id TEXT,
  description TEXT,
  status TEXT NOT NULL DEFAULT 'pending',
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY (preset_id) REFERENCES presets(id)
);
```

No changes to existing P1 tables.

---

## 6. Notifications

Two notifications per ssession (extending TRD §1.7 — `flutter_local_notifications`):

| Notification | Trigger | Content | Action |
|---|---|---|---|
| Heads-up | 15 min before start | *"[Name] starts in 15 minutes"* | Opens FF home with preset pre-loaded |
| Start | At start time | *"[Name] is starting now"* | *"Start Session"* → Home screen |

Both scheduled via `android_alarm_manager_plus` at ssession creation/edit time. Cancelled and rescheduled on edit.

---

## 7. Routing

New route added to TRD §6:

```dart
GoRoute(
  path: '/planner/session/create',
  builder: (_, __) => const ScheduledSessionCreateSheet(),
),
GoRoute(
  path: '/planner/session/:id/edit',
  builder: (_, state) => ScheduledSessionEditSheet(
    sessionId: state.pathParameters['id']!,
  ),
),
```

---

## 8. Module Boundary

**Owned by:** `planner/` module (extends P1 planner module)

```
features/
└── planner/
    ├── data/
    │   ├── scheduled_session_dao.dart        ← NEW
    │   └── planner_repository_impl.dart      ← EXTENDED
    ├── domain/
    │   ├── scheduled_session.dart            ← NEW model
    │   └── use_cases/
    │       ├── create_scheduled_session.dart ← NEW
    │       ├── edit_scheduled_session.dart   ← NEW
    │       └── delete_scheduled_session.dart ← NEW
    └── presentation/
        ├── planner_screen.dart               ← MODIFIED (FAB label, ssession cards)
        ├── scheduled_session_create_sheet.dart ← NEW
        └── scheduled_session_edit_sheet.dart   ← NEW
```

No other P1 modules are modified except:
- `core/notifications/` — two new notification types registered

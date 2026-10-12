# FF Phase 3 — Feature 2: Session Templates + Auto-Scheduling

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Session Templates are reusable session configurations that combine a preset with a target duration and optional recurrence preferences — but without a fixed time. Auto-Scheduling takes a template and finds available time slots in the user's FD calendar, then proposes a schedule for the user to confirm or adjust.

No AI involved. Pure calendar math.

---

## 2. P1/P2 References

| Reference | Location |
|---|---|
| Preset data model | PRD §3.1.2, TRD §3.1 |
| Scheduled Sessions (ssession) | P2 F2 |
| FD ↔ FF IPC — MethodChannel | PRD §3.12, TRD §1.5 |
| FocusBlockRequest payload | PRD §3.12.2 |
| Planner screen | PRD §3.10 |
| 15-minute heads-up notification | PRD §3.8.3 |

---

## 3. Session Templates

### 3.1 What is a Session Template

A template defines:
- A name
- A linked preset
- A target duration (how long each session should be)
- A preferred time of day range (optional — used as a hint for auto-scheduling)
- A description (optional)

Templates do not have a fixed date or time. They are the blueprint. Auto-scheduling or manual scheduling stamps them into actual ssessions (P2 F2).

### 3.2 Template Creation

**Entry point:** Planner screen → Templates tab (new tab added to Planner in P3) → FAB

Bottom sheet fields:

| Field | Type | Constraints |
|---|---|---|
| Template name | Text input | Required, max 64 chars |
| Preset | Dropdown | Required |
| Target duration | Duration picker | Required. 15 min – 8 hours, step 15 min |
| Preferred time range | Time range picker | Optional. e.g. "6:00 PM – 10:00 PM" |
| Description | Text input | Optional |

### 3.3 Template Management

Templates are listed in the Planner → Templates tab.

- Tap: opens template detail (read-only with Edit + Schedule buttons)
- Long-press: context menu → Edit / Delete
- No hard cap on template count

---

## 4. Auto-Scheduling

### 4.1 Trigger

**Entry point:** Template detail screen → "Schedule" button  
OR  
Planner screen → FAB → "Schedule from Template" option

### 4.2 Scheduling Flow

**Step 1 — Configure request:**

A bottom sheet opens:

| Field | Type | Default |
|---|---|---|
| Template | Pre-filled from entry point or selector | — |
| Number of sessions | Stepper (1–7) | 1 |
| Schedule for | Date range picker | Today + next 6 days |
| Respect preferred time range | Toggle | ON if template has a preferred range |

**Step 2 — FF scans FD calendar:**

FF queries FD via MethodChannel for all existing timed tasks in the selected date range. FD returns task blocks (start time, end time, date) for the range.

FF identifies free slots:
- Minimum slot size = template target duration + 15 min buffer on each side
- Slots are filtered to the preferred time range if the toggle is ON
- Slots are sorted by preference: preferred time range first, then earliest available
- FF selects the best N slots (where N = number of sessions requested)

If fewer than N slots are found: FF informs the user how many it could fit and asks whether to proceed with fewer or expand the date range.

**Step 3 — Proposed schedule preview:**

Full-screen preview showing:
- A day-by-day list of proposed sessions
- Each row: date + time range + duration
- User can tap any row to manually adjust the time (opens time picker)
- "Confirm" button (full-width, Electric Indigo) + "Cancel" link

**Step 4 — Confirm:**

On confirm: FF creates ssession records (P2 F2 ScheduledSession model) for each proposed slot, linked to the template's preset. Standard 15-minute heads-up notifications scheduled for each.

### 4.3 FD Calendar Query — MethodChannel

New IPC call added to the existing `com.fluxfoxus/fd_integration` channel:

**FF → FD request:**
```dart
// Method name: 'getTaskBlocks'
// Arguments:
{
  'startDate': int,   // Unix ms
  'endDate': int,     // Unix ms
}
```

**FD response:**
```dart
// Returns List of:
{
  'taskId': String,
  'startTime': int,   // Unix ms
  'endTime': int,     // Unix ms
  'date': int,        // Unix ms for midnight
}
```

FD returns only tasks with both `start_time` and `end_time` set (timed tasks only). Untimed tasks are excluded.

### 4.4 Slot Finding Algorithm

```dart
List<TimeSlot> findAvailableSlots({
  required List<TaskBlock> existingBlocks,
  required Duration targetDuration,
  required DateRange dateRange,
  TimeRange? preferredTimeRange,
  required int count,
}) {
  final buffer = const Duration(minutes: 15);
  final requiredSlotSize = targetDuration + buffer + buffer;
  final List<TimeSlot> candidates = [];

  for (final day in dateRange.days) {
    final dayStart = preferredTimeRange?.start ?? TimeOfDay(hour: 6, minute: 0);
    final dayEnd = preferredTimeRange?.end ?? TimeOfDay(hour: 23, minute: 0);
    
    final dayBlocks = existingBlocks
        .where((b) => b.date.isSameDay(day))
        .sortedBy((b) => b.startTime);

    // Find gaps between blocks within the preferred window
    // Each gap >= requiredSlotSize is a candidate slot
    // Position session at: gap.start + buffer
  }

  // Sort: preferred time range slots first, then by date/time
  return candidates.take(count).toList();
}
```

---

## 5. Data Model

### 5.1 New Model: SessionTemplate

```dart
@freezed
class SessionTemplate with _$SessionTemplate {
  const factory SessionTemplate({
    required String id,                    // UUID
    required String name,
    required String presetId,
    required Duration targetDuration,
    TimeRange? preferredTimeRange,
    String? description,
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _SessionTemplate;
}
```

### 5.2 ScheduledSession (P2 F2) — Extended

One new optional field linking an ssession back to its template:

```dart
// Added to P2 ScheduledSession model:
String? templateId,   // null if not created from a template
```

---

## 6. Database Schema

### 6.1 New Table: session_templates

```sql
CREATE TABLE session_templates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  preset_id TEXT NOT NULL,
  target_duration_seconds INTEGER NOT NULL,
  preferred_start_time TEXT,    -- 'HH:MM' or null
  preferred_end_time TEXT,      -- 'HH:MM' or null
  description TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY (preset_id) REFERENCES presets(id)
);
```

### 6.2 Modified Table: scheduled_sessions (P2)

```sql
ALTER TABLE scheduled_sessions ADD COLUMN template_id TEXT REFERENCES session_templates(id);
```

---

## 7. Routing

```dart
GoRoute(
  path: '/planner/templates',
  builder: (_, __) => const SessionTemplatesScreen(),
),
GoRoute(
  path: '/planner/templates/create',
  builder: (_, __) => const SessionTemplateCreateSheet(),
),
GoRoute(
  path: '/planner/templates/:id',
  builder: (_, state) => SessionTemplateDetailScreen(
    templateId: state.pathParameters['id']!,
  ),
),
GoRoute(
  path: '/planner/templates/:id/schedule',
  builder: (_, state) => AutoScheduleScreen(
    templateId: state.pathParameters['id']!,
  ),
),
```

---

## 8. Module Boundary

**Owned by:** `planner/` module (extended from P2)

```
features/
└── planner/
    ├── data/
    │   ├── session_template_dao.dart         ← NEW
    │   └── planner_repository_impl.dart      ← EXTENDED
    ├── domain/
    │   ├── session_template.dart             ← NEW model
    │   └── use_cases/
    │       ├── create_session_template.dart  ← NEW
    │       ├── find_available_slots.dart     ← NEW
    │       └── auto_schedule_sessions.dart   ← NEW
    └── presentation/
        ├── planner_screen.dart               ← MODIFIED (Templates tab)
        ├── session_templates_screen.dart     ← NEW
        ├── session_template_create_sheet.dart ← NEW
        ├── session_template_detail_screen.dart ← NEW
        └── auto_schedule_screen.dart         ← NEW
```

Modifications to existing modules:
- `fd_integration/` — new `getTaskBlocks` MethodChannel call

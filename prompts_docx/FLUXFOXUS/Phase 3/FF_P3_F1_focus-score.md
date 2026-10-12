# FF Phase 3 — Feature 1: Focus Score

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Focus Score is a single daily integer (0–100) that collapses the day's focus activity into one glanceable number. It is derived from four weighted components drawn entirely from data FF already collects in P1/P2. No new data collection is required.

The score is computed daily. The user configures **checkpoint days** — intermediate days within the week where FF surfaces a partial average. Saturday is always the final checkpoint (full week average). The Sunday weekly report shows the full week graph but no score number.

---

## 2. P1/P2 References

| Reference | Location |
|---|---|
| Session history record | PRD §3.2.7 |
| Break system — breaks used per session | PRD §3.3 |
| App Limits — daily usage tracking | PRD §3.6 |
| Screen Time Tracking — per-category daily totals | PRD §3.9.3 |
| Weekly report — Sunday trigger, contents | PRD §3.11 |
| Post-session reflection rating | P2 F5 |
| StreakDayRecord | P2 F4 |

---

## 3. Score Formula

### 3.1 Components

| Component | Weight | Source |
|---|---|---|
| Session time vs daily target | 40% | Session history + user-configured daily target |
| Break discipline | 25% | Breaks used vs breaks available across all sessions |
| App limit compliance | 20% | Distracting app usage vs configured daily limits |
| Session completion rate | 15% | Completed sessions vs quit sessions |

### 3.2 Component Calculations

**Session time vs daily target (40%)**
```
target_minutes = user-configured daily focus target (minutes)
actual_minutes = sum of actual_focus_seconds / 60 across all completed sessions today
component = clamp(actual_minutes / target_minutes, 0.0, 1.0) × 100
```

**Break discipline (25%)**
```
total_breaks_available = sum of breakCount across all today's sessions (preset value)
total_breaks_used = sum of breaksUsed across all today's sessions
If total_breaks_available = 0: component = 100 (no breaks = perfect discipline)
Else: component = clamp(1 - (total_breaks_used / total_breaks_available), 0.0, 1.0) × 100
```

**App limit compliance (20%)**
```
For each app with an active limit:
  overage = max(0, actual_usage - daily_limit)
  compliance_ratio = clamp(1 - (overage / daily_limit), 0.0, 1.0)
component = average(compliance_ratio) × 100 across all limited apps
If no apps have limits configured: component = 100
```

**Session completion rate (15%)**
```
completed = count of sessions with status = completed today
stopped = count of sessions with status = stopped today
total = completed + stopped
If total = 0: component = 100 (no sessions started = neutral, not penalised)
Else: component = (completed / total) × 100
```

### 3.3 Final Score

```
focus_score = round(
  (component_time × 0.40) +
  (component_break × 0.25) +
  (component_limit × 0.20) +
  (component_completion × 0.15)
)
```

Range: 0–100 (integer). Computed once at midnight by WorkManager. Updated in real-time during the day for the current day's live preview.

### 3.4 User-Adjustable Weights

**Location:** Settings → Focus Score → Component Weights

Four sliders (each 0–100, step 5). A live constraint enforces that the four values always sum to 100. If the user adjusts one slider, the remaining three auto-scale proportionally.

Default values match the formula above (40 / 25 / 20 / 15). Collapsed under an "Advanced" disclosure by default.

---

## 4. Daily Target Configuration

**Location:** Settings → Focus Score → Daily Focus Target

- Time picker: total focus minutes per day the user aims for
- Default: 120 minutes (2 hours)
- Minimum: 15 minutes
- This is independent of the streak minimum sessions setting (PRD §3.5.3) — they serve different purposes

---

## 5. Checkpoint Day System

### 5.1 Week Structure

- Week runs **Monday → Saturday**
- **Sunday** = weekly report day (graph only, no score)
- **Saturday** = always the final checkpoint (full Mon–Sat average shown)

### 5.2 User-Configured Checkpoint Days

**Location:** Settings → Focus Score → Checkpoint Days

Multi-select day picker: Monday through Friday (Saturday is fixed, not selectable).

User picks 0–4 intermediate checkpoint days. On each selected day, FF surfaces the average Focus Score for the days from the previous checkpoint (or Monday) up to and including that day.

**Example:** User selects Tuesday, Friday.
- Tuesday: average of Mon + Tue scores
- Friday: average of Wed + Thu + Fri scores
- Saturday: average of Mon–Sat (full week)

### 5.3 Checkpoint Notification

On each checkpoint day, a notification is pushed at 9:00 PM:
- *"Focus checkpoint: Your average score [Mon–Tue] is [X]/100"*
- Tapping opens the Focus Score screen

---

## 6. Display Surfaces

### 6.1 Focus Score Screen (New)

Accessible from Home screen stats area (tap) or Settings.

**Contents:**
- Today's live score (large, prominent — updates every 5 minutes while app is open)
- Score ring: circular progress ring (0–100), colour-coded:
  - 0–39: danger red
  - 40–69: amber
  - 70–89: Electric Indigo
  - 90–100: Soft Cyan
- Component breakdown: 4 rows showing each component's contribution
- This week's daily scores: 7-cell strip (Mon–Sat + Sun blank), each cell shows score or "—" for future days
- Current checkpoint period average

### 6.2 Weekly Report Integration (Sunday)

The Sunday report (PRD §3.11.2) gains:
- Full week bar chart: Mon–Sat daily scores (fl_chart bar chart)
- 7 bars, colour-coded by score range (same colours as §6.1)
- Week average score: single number, prominent
- No score for Sunday itself

### 6.3 Home Screen Widget Integration (P1 widget — extended)

The P1 home widget (PRD §3.13) gains one new data point when Focus Score is configured:
- Today's score shown as a small number badge below the donut ring
- Only shown if daily target is configured

---

## 7. Data Model

### 7.1 New Model: DailyFocusScore

```dart
@freezed
class DailyFocusScore with _$DailyFocusScore {
  const factory DailyFocusScore({
    required DateTime date,
    required int score,                    // 0–100
    required int componentTime,            // 0–100
    required int componentBreak,           // 0–100
    required int componentLimit,           // 0–100
    required int componentCompletion,      // 0–100
    required bool isFinal,                 // false = intra-day estimate, true = midnight-computed
    required DateTime computedAt,
  }) = _DailyFocusScore;
}
```

---

## 8. Database Schema

### 8.1 New Table: daily_focus_scores

```sql
CREATE TABLE daily_focus_scores (
  date INTEGER PRIMARY KEY,              -- Unix ms for midnight UTC
  score INTEGER NOT NULL,
  component_time INTEGER NOT NULL,
  component_break INTEGER NOT NULL,
  component_limit INTEGER NOT NULL,
  component_completion INTEGER NOT NULL,
  is_final INTEGER NOT NULL DEFAULT 0,
  computed_at INTEGER NOT NULL
);
```

### 8.2 New Settings (Hive)

| Key | Type | Default |
|---|---|---|
| `focus_score_daily_target_minutes` | int | 120 |
| `focus_score_weight_time` | int | 40 |
| `focus_score_weight_break` | int | 25 |
| `focus_score_weight_limit` | int | 20 |
| `focus_score_weight_completion` | int | 15 |
| `focus_score_checkpoint_days` | List\<int\> | [] (empty = no intermediate checkpoints) |

---

## 9. Routing

```dart
GoRoute(
  path: '/focus-score',
  builder: (_, __) => const FocusScoreScreen(),
),
```

---

## 10. Module Boundary

**New module:** `focus_score/`

```
features/
└── focus_score/
    ├── data/
    │   ├── focus_score_dao.dart
    │   └── focus_score_repository_impl.dart
    ├── domain/
    │   ├── daily_focus_score.dart
    │   └── use_cases/
    │       ├── compute_focus_score.dart
    │       └── get_weekly_scores.dart
    └── presentation/
        ├── focus_score_screen.dart
        ├── score_ring_widget.dart
        └── focus_score_settings_section.dart
```

Modifications to existing modules:
- `widget/` — score badge on home widget
- `usage_stats/` (weekly report) — weekly bar chart
- `home/presentation/home_screen.dart` — tappable score entry point
- `core/background/` — WorkManager task for midnight score finalization + checkpoint notifications

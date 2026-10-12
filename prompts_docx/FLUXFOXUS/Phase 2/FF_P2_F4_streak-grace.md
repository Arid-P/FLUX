# FF Phase 2 — Feature 4: Streak Protection Grace

**Version:** 1.0  
**Phase:** 2  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Streak Protection Grace allows the user one missed day per calendar month without resetting their streak. The missed day is logged visibly — it does not silently disappear. The goal is to reduce the catastrophic motivation collapse that a single bad day can cause on a long streak, while preserving the integrity of the streak as a meaningful metric.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Streak system — definition and reset rules | PRD §3.5 |
| Streak day definition | PRD §3.5.2 |
| Streak configuration (minimum sessions/day) | PRD §3.5.3 |
| Streak reset triggers | PRD §3.5.4 |
| Streak display locations | PRD §3.5.5 |
| StreakRecord data model | TRD §3.4 |
| Stop Focusing modal — streak display | PRD §3.4.2 |

---

## 3. Feature Specification

### 3.1 Grace Day Rules

- **One grace day per calendar month.** Calendar month = Jan, Feb, Mar, etc. Not a rolling 30-day window
- A grace day is consumed automatically on the first day of any calendar month that the user misses their minimum session target
- If the user misses a second day in the same calendar month: streak resets normally (P1 behaviour)
- Grace days do not accumulate. Unused grace days do not carry over to the next month
- Grace day consumption is irreversible — it cannot be manually triggered or manually reversed

### 3.2 What Counts as "Using" a Grace Day

At midnight each day (evaluated by a WorkManager task — TRD §1.4), FF checks if yesterday's session target was met:
- Target met → streak increments normally
- Target not met AND grace day available for current month AND grace day not yet used → grace day consumed, streak preserved, missed day logged
- Target not met AND grace day already used this month → streak resets to 0 (P1 behaviour)
- Target not met AND minimum sessions configured = 0 → streak unchanged (P1 §3.5.2 — streak neutral)

### 3.3 Visual Indicator — Missed Day Log

The missed day is permanently visible in streak history. It is never hidden.

**Planner screen — streak bar:**

The flame icon streak display (PRD §3.5.5) gains a secondary label when a grace day has been used in the current month:

- Normal: *"🔥 47 days"*
- Grace used: *"🔥 47 days · 🛡️ Grace used"*

The shield emoji and label are shown for the remainder of the month in which grace was used.

**Streak history view (new P2 addition — see §3.5):**

The missed day appears as a distinct row in the streak history calendar:
- Day cell background: dark amber (#B45309 at 20% opacity)
- Day cell icon: shield icon (small, amber)
- Tooltip on tap: *"Grace day used. Streak preserved."*

### 3.4 Stop Focusing Modal — Grace Awareness

The Stop Focusing modal (PRD §3.4) is updated when a grace day is available:

**Current P1 modal secondary text:**
> *"You will break your focus streak if you stop now."*

**P2 update — when grace day available and not yet used this month:**
> *"You will use your monthly grace day if you stop now. Your streak will be preserved, but grace days don't accumulate."*

Streak display in modal remains *"[X] days → 0 days"* for the no-grace case, or *"[X] days → [X] days 🛡️"* for the grace case.

### 3.5 Streak History View (New P2 Screen)

A new screen accessible from the Planner's streak bar (tap → opens Streak History).

**Contents:**
- Monthly calendar grid (current month, with back/forward navigation by month)
- Each day cell coloured by status:
  - ✅ Green: session target met
  - ❌ Red: streak reset day
  - 🛡️ Amber: grace day used
  - ⬜ Grey: future day or day before FF was installed
- Summary row below calendar:
  - Current streak
  - Longest streak
  - Grace days used this month: *"1 / 1"* or *"0 / 1"*
  - Total sessions this month

---

## 4. Data Model Changes

### 4.1 StreakRecord (TRD §3.4) — Extended

```dart
@freezed
class StreakRecord with _$StreakRecord {
  const factory StreakRecord({
    required int currentStreak,
    required int longestStreak,
    required int minimumSessionsPerDay,
    required DateTime lastStreakDate,
    // NEW P2 FIELDS:
    required int graceDaysUsedThisMonth,   // 0 or 1
    DateTime? lastGraceDayUsedDate,        // null if never used
    required DateTime updatedAt,
  }) = _StreakRecord;
}
```

### 4.2 New Model: StreakDayRecord

```dart
@freezed
class StreakDayRecord with _$StreakDayRecord {
  const factory StreakDayRecord({
    required DateTime date,
    required StreakDayStatus status,
    required int sessionsCompleted,
  }) = _StreakDayRecord;
}

enum StreakDayStatus { met, missed, graceUsed, reset, neutral }
```

---

## 5. Database Schema Changes

### 5.1 Modified Table: streak_records

Two new columns added to existing `streak_records` table (TRD §4 — table not fully shown in TRD but implied by StreakRecord model):

```sql
ALTER TABLE streak_records ADD COLUMN grace_days_used_this_month INTEGER NOT NULL DEFAULT 0;
ALTER TABLE streak_records ADD COLUMN last_grace_day_used_date INTEGER;
```

### 5.2 New Table: streak_day_records

```sql
CREATE TABLE streak_day_records (
  date INTEGER PRIMARY KEY,            -- Unix ms for midnight UTC of the day
  status TEXT NOT NULL,                -- 'met' / 'missed' / 'grace_used' / 'reset' / 'neutral'
  sessions_completed INTEGER NOT NULL DEFAULT 0
);
```

---

## 6. Grace Day Reset Logic

At the start of each new calendar month, `grace_days_used_this_month` resets to 0. This is handled by the same WorkManager midnight evaluation task that checks streak continuity.

```dart
void evaluateStreakForYesterday() {
  final yesterday = DateTime.now().subtract(const Duration(days: 1));
  final record = streakRepository.get();
  
  // Reset grace counter if new month
  final isNewMonth = yesterday.month != record.lastStreakDate.month;
  if (isNewMonth) {
    streakRepository.resetGraceDaysForNewMonth();
  }
  
  final sessionsYesterday = sessionRepository.countCompletedForDate(yesterday);
  final targetMet = sessionsYesterday >= record.minimumSessionsPerDay;
  
  if (record.minimumSessionsPerDay == 0) {
    // Streak neutral — no change
    streakDayRepository.record(yesterday, StreakDayStatus.neutral, sessionsYesterday);
    return;
  }
  
  if (targetMet) {
    streakRepository.incrementStreak();
    streakDayRepository.record(yesterday, StreakDayStatus.met, sessionsYesterday);
  } else if (record.graceDaysUsedThisMonth < 1) {
    streakRepository.consumeGraceDay(yesterday);
    streakDayRepository.record(yesterday, StreakDayStatus.graceUsed, sessionsYesterday);
  } else {
    streakRepository.resetStreak();
    streakDayRepository.record(yesterday, StreakDayStatus.reset, sessionsYesterday);
  }
}
```

---

## 7. Routing

New route:

```dart
GoRoute(
  path: '/planner/streak-history',
  builder: (_, __) => const StreakHistoryScreen(),
),
```

---

## 8. Module Boundary

**Owned by:** `streaks/` module (extends P1 streaks module)

```
features/
└── streaks/
    ├── data/
    │   ├── streak_repository_impl.dart     ← EXTENDED (grace day logic)
    │   └── streak_day_dao.dart             ← NEW
    ├── domain/
    │   ├── streak_record.dart              ← EXTENDED (new fields)
    │   ├── streak_day_record.dart          ← NEW model
    │   └── use_cases/
    │       ├── evaluate_streak_for_day.dart ← EXTENDED
    │       └── get_streak_history.dart      ← NEW
    └── presentation/
        ├── streak_history_screen.dart       ← NEW
        └── streak_bar_widget.dart           ← MODIFIED (grace label)
```

Modifications to existing P1 modules:
- `focus_timer/presentation/stop_focusing_modal.dart` — grace-aware copy
- `planner/presentation/planner_screen.dart` — streak bar tap navigates to streak history

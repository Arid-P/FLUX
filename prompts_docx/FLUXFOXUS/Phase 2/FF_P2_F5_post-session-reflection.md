# FF Phase 2 — Feature 5: Post-Session Reflection

**Version:** 1.0  
**Phase:** 2  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Post-Session Reflection is a brief, optional rating prompt shown after a focus session ends. The user rates how focused they felt during the session using a 3-tap interaction. The data feeds into the Planner's session history and the weekly report.

The feature is **off by default** and must be explicitly enabled in Settings — consistent with the Habit Log toggle pattern in FluxDone (FD PRD §5, Habit Tracker feature). When disabled, the flow after session end is identical to P1.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Session end flow — Countdown | PRD §3.2.5 |
| Session end flow — Stopwatch / Open-Ended | PRD §3.2.6 |
| Session history record | PRD §3.2.7 |
| FocusSession data model | TRD §3.2 |
| Weekly report contents | PRD §3.11.2 |
| Planner session card | PRD §3.10.3 |
| Completed session display | PRD §3.10.4 |

---

## 3. Feature Specification

### 3.1 Toggle

**Location:** Settings → Sessions → Post-Session Reflection

| Control | Type | Default |
|---|---|---|
| Post-Session Reflection | Toggle | **OFF** |

When OFF: zero change to P1 session end behaviour. No prompt, no data collected.  
When ON: reflection prompt appears after every completed session (both tsession and msession).

### 3.2 Trigger Conditions

The reflection prompt appears when:
- Session status transitions to `completed` (natural countdown end OR user confirms quit in Stop Focusing modal)
- Feature toggle is ON

The prompt does **not** appear when:
- Session status = `stopped` via Quit Session and the user had already been through the Stop Focusing modal friction — in this case the session is already marked as a quit. The reflection only fires on genuinely completed sessions
- The app is in the background when the session ends — in this case a notification is sent instead (see §3.4)

### 3.3 Reflection Prompt UI

A bottom sheet appears immediately after session end (after the session complete vibration + sound, before navigating to Home).

**Layout:**

```
─────────────────────────────────
  Session complete ✓
  [Session name] · [Duration]
  
  How focused were you?
  
  [😴 Low]   [😐 Medium]   [🔥 High]
  
                        [Skip →]
─────────────────────────────────
```

- **Three rating tiles** (equal width, 3-column row):
  - 😴 Low
  - 😐 Medium  
  - 🔥 High
- Tapping a tile: immediately submits and dismisses. No confirmation needed
- **Skip button:** text button, right-aligned below tiles. Tapping = no rating recorded for this session. Sheet dismisses
- Bottom sheet is **not dismissible by back tap or swipe** — user must tap a rating or Skip explicitly
- No timeout / auto-dismiss — user must act

**Copy:**
- Header: *"Session complete ✓"*
- Sub-header: *"[Session name] · [Xh Ym focused]"*
- Prompt: *"How focused were you?"*

### 3.4 Background Session End — Notification Fallback

When a Countdown session ends while FF is in the background:
- Standard P1 session-end notification fires (PRD §3.2.5)
- If reflection toggle is ON: notification gains an action row with three buttons: **Low | Medium | High**
- Tapping a notification action records the rating and dismisses the notification
- If no action tapped within 1 hour: session is recorded with `reflection_rating = null`

### 3.5 Rating Data

Three values: `low`, `medium`, `high`. Stored as a string on the session record.

A skipped reflection or a background notification with no action = `null` (no rating). This is distinct from a deliberate "Low" rating.

---

## 4. Data Model Changes

### 4.1 FocusSession (TRD §3.2) — Extended

One new field added:

```dart
@freezed
class FocusSession with _$FocusSession {
  const factory FocusSession({
    // ... all existing P1 fields unchanged ...
    
    // NEW P2 FIELD:
    ReflectionRating? reflectionRating,   // null if skipped or feature off
  }) = _FocusSession;
}

enum ReflectionRating { low, medium, high }
```

---

## 5. Database Schema Changes

### 5.1 Modified Table: focus_sessions

One new column:

```sql
ALTER TABLE focus_sessions ADD COLUMN reflection_rating TEXT;
-- Nullable. Values: 'low' / 'medium' / 'high' / null
```

No new tables required.

---

## 6. Weekly Report Integration

The weekly report (PRD §3.11.2) gains one new data point when reflection toggle is ON and at least 3 sessions in the week have ratings:

- **Average focus quality:** displayed as a simple label — *"Focus quality: High / Medium / Low"* (majority vote across the week's rated sessions)
- If fewer than 3 rated sessions: this line is omitted from the report (not enough data)
- Unrated sessions (null) are excluded from the average calculation

---

## 7. Planner Session Card Integration

Completed session cards in the Planner (PRD §3.10.4) gain a small rating indicator:

- 🔥 (high) / 😐 (medium) / 😴 (low) — displayed as a small emoji at the far right of the session card, same row as the duration
- No emoji shown if session has no rating (null)
- Only visible on completed sessions (opacity 0.6 cards — PRD §3.10.4)

---

## 8. Module Boundary

**Owned by:** `focus_timer/` module (minimal extension)

```
features/
└── focus_timer/
    ├── data/
    │   └── session_dao.dart               ← MODIFIED (new column)
    ├── domain/
    │   └── focus_session.dart             ← EXTENDED (new field + enum)
    └── presentation/
        ├── focus_session_screen.dart       ← MODIFIED (trigger reflection on complete)
        ├── post_session_reflection_sheet.dart ← NEW
        └── session_complete_notification.dart ← MODIFIED (action buttons if toggle ON)
```

Modifications to existing P1 modules:
- `planner/presentation/session_card.dart` — rating emoji indicator
- `usage_stats/` (weekly report) — average focus quality line
- `settings/presentation/` — Sessions section with reflection toggle

# FF Phase 3 — Feature 4: AI Improvements

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Phase 3 expands the AI layer introduced in P2 (AI Break Negotiation — P2 F3) with four new touchpoints. All four use the same app-wide AI toggle and user-provided API key established in P2. No new API key or toggle infrastructure is needed.

| Touchpoint | Abbreviation |
|---|---|
| A — AI Session Debrief | ASD |
| B — AI Preset Suggester | APS |
| C — AI Streak Coach | ASC |
| E — AI Difficulty Auto-Adjust | ADA |

---

## 2. P1/P2 References

| Reference | Location |
|---|---|
| AI toggle, API key, secure storage | P2 F3 §3.1 |
| AI module structure (`features/ai/`) | P2 F3 §8 |
| Post-session reflection rating | P2 F5 |
| Break negotiation outcomes | P2 F3 §4.3 |
| Negotiation difficulty settings | P2 F3 §3.3 |
| StreakRecord + grace day | P2 F4 |
| Preset data model | PRD §3.1.2 |
| Session history | PRD §3.2.7 |
| Weekly report | PRD §3.11 |

---

## 3. A — AI Session Debrief (ASD)

### 3.1 Overview

After every completed session, the user can ask the AI "how did I do?" The AI receives a structured summary of the session and returns a short, personalised observation. The user can ask one follow-up question.

### 3.2 Trigger

Immediately after a session transitions to `completed`:
- The post-session reflection sheet (P2 F5) shows first (if enabled)
- After reflection sheet is dismissed (or if toggle is OFF): a subtle "Ask AI" button appears on the session complete screen
- Button label: *"How did I do? →"* (secondary style, not primary)
- Button is only visible if AI toggle is ON and API key is valid

### 3.3 ASD Screen

Opens as a bottom sheet.

**Layout:**
```
─────────────────────────────────
  Session Debrief
  [Session name] · [Duration] · [Rating emoji if rated]

  [AI response — paragraph, max 100 words]

  ─────────────────────────────
  [Follow-up input field]        [Send →]
  (disabled after first follow-up)
─────────────────────────────────
```

- Loading state: shimmer placeholder while API call is in flight
- Follow-up field is enabled after the initial response arrives
- After user sends one follow-up and AI responds: input field disables permanently with label *"One follow-up per session"*
- "Close" button: bottom of sheet

### 3.4 API Request — Initial Debrief

**System prompt:**
```
You are a concise, honest focus coach reviewing a student's study session. 
Give a short, direct observation (max 100 words). Do not give generic praise. 
Reference the actual data. Tone: calm, analytical, occasionally encouraging.
Respond in plain text only — no markdown, no bullet points.
```

**User message (structured session data):**
```
Session: [name]
Duration: [Xh Ym actually focused]
Breaks used: [N] of [total available]
Reflection rating: [low / medium / high / not rated]
Time of day: [started at HH:MM]
Day: [Monday / Tuesday / ...]
Sessions completed today: [N]
Daily focus target: [X minutes]
Progress toward target: [X%]
```

### 3.5 API Request — Follow-Up

Full conversation history sent (system prompt + initial data message + AI response + user follow-up). Max 3 messages in history (initial + AI response + follow-up). No further turns.

### 3.6 Storage

The debrief response and follow-up are stored alongside the session record.

```sql
ALTER TABLE focus_sessions ADD COLUMN debrief_initial_response TEXT;
ALTER TABLE focus_sessions ADD COLUMN debrief_followup_question TEXT;
ALTER TABLE focus_sessions ADD COLUMN debrief_followup_response TEXT;
```

---

## 4. B — AI Preset Suggester (APS)

### 4.1 Overview

Before starting a session, FF analyses the user's recent session history and surfaces a preset suggestion as a subtle chip below the preset selector on the Home screen.

### 4.2 Suggestion Chip

**Location:** Home screen, directly below the preset selector dropdown (PRD §3.2.3, `ui_home.md`)

**Appearance:**
- Soft chip (outlined, secondary style — not Electric Indigo)
- Label: *"💡 Try: [Preset Name]"*
- Tapping the chip: applies the suggested preset to the selector
- Dismissible: small × icon on the right of the chip. Dismissing hides chip for 24 hours
- Only shown if AI toggle is ON, API key valid, and at least 5 sessions of history exist

### 4.3 Suggestion Logic

Suggestion is computed in the background (not on-demand) via a WorkManager task that runs once daily at 6:00 AM.

**API request:**

**System prompt:**
```
You are a focus session advisor. Based on the user's session history, 
suggest which preset they should use for their next session.
Respond ONLY with valid JSON: {"preset_id": "string", "reason": "string (max 20 words)"}
The preset_id must be one of the provided preset IDs. 
If you cannot make a confident suggestion, return {"preset_id": null, "reason": null}
```

**User message:**
```
Available presets: [JSON array of {id, name, breakCount, breakDuration}]
Recent sessions (last 14 days):
- [date, day_of_week, time_of_day, preset_used, duration, breaks_used, reflection_rating]
Current time: [HH:MM]
Current day: [Monday / Tuesday / ...]
```

**Response handling:**
- `preset_id: null` → no chip shown
- Valid `preset_id` → chip shown with the preset name
- API error → no chip shown (silent fail)

### 4.4 Storage (Hive)

| Key | Type | Description |
|---|---|---|
| `aps_suggested_preset_id` | String? | Current suggestion, null if none |
| `aps_suggestion_reason` | String? | Reason text for the suggestion |
| `aps_last_computed` | DateTime | When suggestion was last computed |
| `aps_dismissed_until` | DateTime? | If chip was dismissed, hide until this time |

---

## 5. C — AI Streak Coach (ASC)

### 5.1 Overview

When the streak is at risk (grace day already used this month, no session completed yet today, and it is approaching the end of the day), the AI generates a personalised nudge notification referencing the user's actual history data.

### 5.2 Risk Detection

A WorkManager task evaluates streak risk at **3 scheduled times daily**: 8:00 AM, 2:00 PM, 6:00 PM.

**Risk conditions (all must be true):**
- Current streak > 0
- No session completed today yet
- Grace day already used this month (P2 F4) OR streak ≤ 3 days (new streak — still worth protecting)
- Time is past 6:00 PM (only the 6:00 PM check sends the notification — earlier checks are for retry logic only)

### 5.3 Retry Logic on API Failure

The 8:00 AM and 2:00 PM checks are **pre-generation attempts**. If the API call succeeds at 8:00 AM, the generated message is cached. If it fails, the 2:00 PM check retries. If that fails, the 6:00 PM check retries once more.

If all 3 attempts fail: the notification is sent using **yesterday's generated message** (if available in cache) with a generic opener prepended: *"(From yesterday's plan)"*. If no cached message exists at all: notification is not sent.

### 5.4 API Request

**System prompt:**
```
You are a streak coach for a student. Write a short, direct push notification 
(max 60 words) to remind them to complete a focus session today before they 
lose their streak. Reference their actual streak length and recent performance. 
Do not be preachy. Be direct and honest.
Respond in plain text only — this is notification body text.
```

**User message:**
```
Current streak: [N] days
Grace day used this month: [yes/no]
Sessions completed today: 0
Recent 7 days: [date, sessions_completed, focus_minutes per day]
Time now: [HH:MM]
```

### 5.5 Notification

- **Title:** *"Streak at risk 🔥"*
- **Body:** AI-generated text (max 60 words)
- **Action:** *"Start Session"* → opens FF home screen
- One notification per day maximum — if user completes a session after the notification fires, no further notifications that day

### 5.6 Storage (Hive)

| Key | Type | Description |
|---|---|---|
| `asc_cached_message` | String? | Last successfully generated message |
| `asc_cached_date` | DateTime? | Date the cached message was generated |
| `asc_sent_today` | bool | Whether notification was sent today |

---

## 6. E — AI Difficulty Auto-Adjust (ADA)

### 6.1 Overview

FF monitors break negotiation outcomes over time. If the user wins or loses 5 consecutive negotiations at the current difficulty level, the AI generates a difficulty change suggestion with reasoning. The user must explicitly confirm to change.

Manual trigger is also available from Settings → AI → Break Negotiation → Difficulty.

### 6.2 Auto-Monitor Logic

Tracked per negotiation outcome: `granted` (user won) or `denied` (user gave up after final denial).

```dart
// Evaluated after every negotiation resolves
void evaluateDifficultyTrend() {
  final recent = negotiationRepository.getLastN(5);
  if (recent.length < 5) return;  // Not enough data yet
  
  final allGranted = recent.every((n) => n.granted);
  final allDenied = recent.every((n) => !n.granted);
  
  if (allGranted || allDenied) {
    triggerADASuggestion(
      direction: allGranted ? 'increase' : 'decrease',
      recentOutcomes: recent,
    );
  }
}
```

After a suggestion is shown (confirmed or dismissed), the counter resets. The next suggestion requires another 5 consecutive outcomes.

### 6.3 Manual Trigger

**Location:** Settings → AI → Break Negotiation → Difficulty → *"Review difficulty with AI"* button

Tapping triggers the same `triggerADASuggestion` flow with the last 10 negotiations as context (not limited to 5 consecutive).

### 6.4 ADA Suggestion API Request

**System prompt:**
```
You are a focus discipline advisor. Based on the user's recent break negotiation 
outcomes, suggest whether they should increase or decrease their negotiation 
difficulty setting. Be specific about why. Explain what the change will mean 
in practice.
Respond ONLY with valid JSON:
{
  "suggested_difficulty": "easy" | "medium" | "hard",
  "reasoning": "string (max 80 words)",
  "expected_impact": "string (max 40 words)"
}
```

**User message:**
```
Current difficulty: [easy / medium / hard]
Recent negotiations (last [N]):
- [session_name, requested_duration, granted: true/false, rounds_of_negotiation]
Trigger: [auto_5_consecutive | manual_review]
```

### 6.5 ADA Suggestion UI

A bottom sheet opens (from Settings or automatically after the 5th consecutive outcome):

```
─────────────────────────────────
  Difficulty Adjustment

  Current: Medium
  Suggested: Hard

  [AI reasoning — paragraph]

  Expected impact:
  [expected_impact text]

  [Keep Medium]    [Switch to Hard]
─────────────────────────────────
```

- **"Keep [current]":** dismisses sheet, no change. Counter resets
- **"Switch to [suggested]":** updates difficulty in Settings. Counter resets. Snackbar: *"Difficulty updated to Hard"*
- If user dismisses by back tap: treated as "Keep current"

### 6.6 Storage

Negotiation outcome tracking already exists in `ai_break_negotiations` table (P2 F3 §6.1). No new tables needed.

New Hive key:

| Key | Type | Description |
|---|---|---|
| `ada_consecutive_outcome` | String? | 'granted' or 'denied' — current streak direction |
| `ada_consecutive_count` | int | Count of consecutive same outcomes |
| `ada_last_suggestion_date` | DateTime? | Cooldown — no auto-suggest within 7 days of last |

---

## 7. Module Boundary

All 4 touchpoints extend the existing `ai/` module (P2 F3):

```
features/
└── ai/
    ├── data/
    │   ├── ai_api_client.dart              ← EXTENDED (new request types)
    │   ├── ai_settings_repository.dart     ← EXTENDED (new Hive keys)
    │   └── ai_break_dao.dart               ← EXTENDED (debrief columns)
    ├── domain/
    │   └── use_cases/
    │       ├── get_session_debrief.dart     ← NEW (ASD)
    │       ├── get_preset_suggestion.dart   ← NEW (APS)
    │       ├── generate_streak_nudge.dart   ← NEW (ASC)
    │       └── evaluate_difficulty_trend.dart ← NEW (ADA)
    └── presentation/
        ├── session_debrief_sheet.dart       ← NEW (ASD)
        ├── ai_settings_section.dart         ← EXTENDED (ADA manual trigger)
        └── ada_suggestion_sheet.dart        ← NEW (ADA)
```

Modifications to existing modules:
- `home/presentation/home_screen.dart` — APS suggestion chip
- `focus_timer/presentation/` — ASD "Ask AI" button on session complete screen
- `focus_timer/data/session_dao.dart` — debrief columns (ASD)
- `streaks/` — ASC risk evaluation hook
- `core/background/` — WorkManager tasks for APS (6AM daily) and ASC (8AM, 2PM, 6PM)

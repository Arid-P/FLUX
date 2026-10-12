# FluxFoxus (FF) — Product Requirements Document
**Version:** 1.0  
**Date:** March 2026  
**Author:** Ari (directed), AI-assisted  
**Status:** Draft — Pending final review  
**Platform:** Android (Phase 1). iOS + Web = Phase 2.  
**UI References:** ui_design_system.md, ui_home.md, ui_focus_session.md, ui_distracted_intervention.md, ui_usage_stats.md, ui_planner.md, ui_preset.md, ui_app_limits.md, ui_widget.md, ui_navigation.md

---

## 1. Product Overview

### 1.1 What is FluxFoxus?
FluxFoxus (FF) is a screen discipline and focus app for Android. It is the second app in the **Flux family**, the first being **FluxDone (FD)**, a task management app. FF replaces the Regain app for the primary user (Ari) and is designed for students who need structured, disciplined screen time management while preparing for high-stakes exams (Math Olympiad IOQM→INMO→IMO, JEE Foundation, ICSE Class 10→12).

FF is not a wellness app. It is an enforcement and tracking tool with deliberate friction mechanics designed to make breaking focus habits costly enough to think twice — but never punitive enough to destroy motivation.

### 1.2 Core Philosophy
- **Discipline over motivation:** FF assumes motivation fluctuates. Friction makes stopping expensive.
- **Offline-first:** All core features work without internet. No cloud dependency.
- **Modular:** Each feature is independent. A broken feature must not cascade.
- **Minimal UI, maximum clarity:** Every pixel earns its place.
- **Flux family coherence:** FF and FD communicate. Tasks in FD become sessions in FF automatically.

### 1.3 What FF Replaces (Regain)
FF replicates Regain's core workflow and fixes its pain points:
- ✅ Focus Timer (plain timer, external music via Spotify/YT Music/Echo Nightly)
- ✅ App Limits (solo screen time control)
- ✅ Block Scheduling (combined with App Limits for structured study sessions)
- ✅ YouTube Study Mode
- ✅ Screen Time Tracking
- ⚡ Planner / Calendar (minimal, sessions only — not a task manager)
- ❌ Built-in focus music (not implemented)
- ❌ Multiplayer / leaderboards (not implemented)
- ❌ AI companion (not implemented)

**Regain pain points being fixed:**
- Cluttered UI → FF has a clean, information-dense but uncluttered layout
- App Limits and Block Scheduling are separate systems → FF unifies them under Presets
- No concept of reusable configurations → FF introduces Presets as the central abstraction

---

## 2. Target User

**Primary user:** Ari — student, Class 10→12 ICSE, preparing for IOQM→INMO→IMO and JEE Foundation (Neev Diamond batch at PW). Android-only. Does not write code but directs AI to build.

**User behavior profile:**
- Long study sessions (2–5 hours)
- Uses Spotify / YT Music / Echo Nightly for external focus music
- Needs hard enforcement, not suggestions
- Has recurring structured schedules (lectures, practice, self-study)
- Uses TickTick for task management (FluxDone is the custom replacement)
- Screen time is a known problem — distracted apps are Instagram, YouTube (non-study), games

---

## 3. Feature Specifications

---

### 3.1 Presets

#### 3.1.1 Overview
A Preset is the central abstraction in FF. It is a reusable focus configuration that stores all session settings except the time window (start time, end time, duration). Presets are created once and applied to sessions, tasks, and list sections.

#### 3.1.2 Preset Properties
| Property | Type | Constraints |
|---|---|---|
| Name | String | Required, max 64 chars |
| Emoji icon | Emoji character | Selected from picker, default ⏳ |
| Break count | Integer | 0–6. 0 = no breaks |
| Break duration (each) | Integer (minutes) | 1–15 |
| App restrictions | Map<AppId, BlockState> | Per-app: blocked or not blocked |
| YouTube mode | Enum | BLOCK / ALLOW / STUDY_MODE |
| Description | String | Optional, free text |

#### 3.1.3 Preset Selection (Default)
- The last used preset is automatically selected as the default for the next session
- If no preset has ever been used: no default, user must select or create one before starting a session
- There is no explicit "default preset" setting — last-used serves as default
- If FF has not been used in 60+ hours: a confirmation nudge appears before the session starts asking the user to verify the preset is still correct

#### 3.1.4 Preset Application to FD Tasks
When applying a preset from FF to a FD list or list section:
1. User selects a FD list or a specific section within a list
2. User selects a preset to apply
3. FF iterates all tasks under the selected list/section
4. For each task that has a duration set: FF automatically creates a FocusSession linked to that task, using the selected preset
5. Tasks without a duration: skipped silently (no alert, no flag)
6. Recurring tasks: each instance of the recurring task generates a new session upon instance creation in FD — no manual re-linking needed

#### 3.1.5 Preset Limits
- No hard cap on number of presets
- Presets are stored locally in SQLite

---

### 3.2 Focus Timer

#### 3.2.1 Overview
FF provides a full-screen focus session experience with a mechanical flip clock display. Three timer modes are available.

#### 3.2.2 Timer Modes
| Mode | Description | Clock behavior |
|---|---|---|
| Countdown | Counts down from a set duration to 00:00 | HH:MM decreasing |
| Stopwatch | Counts up from 00:00, no end | HH:MM increasing |
| Open-Ended | Counts up from 00:00, user ends manually | HH:MM increasing, no end time shown in header |

**Clock display:** HH:MM format. Top flip card = HOURS, bottom flip card = MINUTES.

#### 3.2.3 Starting a Session from FF Home Screen
1. User opens FF
2. Home screen shows currently selected preset and its details
3. User optionally changes the preset via the dropdown chevron
4. User taps "Start Focusing"
5. Session starts in the selected mode
6. A task named "Focus Session" is automatically created in FD:
   - Placed in the "FF" section of the user's default FD list
   - Description contains: preset name, session duration, mode, start time

#### 3.2.4 Starting a Session from FD
When a FD task with a linked FocusSession is due:
- A 15-minute heads-up notification is pushed
- User can tap the notification to jump to FF and start the session
- Session auto-links to the FD task

#### 3.2.5 Session End — Countdown
- When HH:MM hits 00:00: vibration + system notification
- UI auto-navigates to home screen
- Session is logged in session history

#### 3.2.6 Session End — Stopwatch / Open-Ended
- User taps "Stop Focusing"
- Stop Focusing modal appears
- If user confirms quit: session ends, navigates to home
- If user keeps going: session resumes

#### 3.2.7 Session History
- All completed sessions are stored locally
- Each record: session ID, preset ID, start time, end time, actual duration focused, mode, breaks used, source (FF or FD)

---

### 3.3 Break System

#### 3.3.1 Overview
Breaks are configured per preset. They are intentional rest periods within a focus session. Breaks are not pauses — they are timed periods with their own countdown.

#### 3.3.2 Break Configuration
| Property | Constraint |
|---|---|
| Break count | 0–6 per session |
| Break duration (each) | 1–15 minutes |
| Total break time | No enforced limit in session breaks (unlike app limit extra time which caps at 60 mins) |

#### 3.3.3 Taking a Break
1. User taps "Break" button during active session
2. Focus session timer pauses
3. Break window opens (Soft Cyan flip clock counting down break duration)
4. Break count decrements by 1
5. When break ends (countdown hits 00:00): vibration + notification, auto-returns to focus session
6. User can also tap "End Break Early" to return immediately

#### 3.3.4 Break Exhaustion
- When all configured breaks are used: "Break" button greyscales and becomes non-interactive
- If user then taps "Stop Focusing": Stop Focusing modal appears (same flow as manual stop — no distinction in copy or behavior)

#### 3.3.5 No Breaks Configured
- When a preset has 0 breaks: "Break" button is absent from the focus session screen entirely

---

### 3.4 Stop Focusing / Streak Break Flow

#### 3.4.1 Trigger
Appears when:
- User taps "Stop Focusing" during an active session
- All breaks are exhausted AND user taps "Stop Focusing"

Both cases show the identical modal with identical copy.

#### 3.4.2 Modal Content
- Alert icon (Electric Indigo)
- "Finish the goal?" — primary text
- "You will break your focus streak if you stop now." — secondary text
- Streak display: "[X] days → 0 days"

#### 3.4.3 Wait Timer
Both action buttons ("Keep Going" and "Quit Session") start disabled.
A countdown runs for 15–25 seconds before both buttons activate.

**Wait time formula:**

```
wait_time = clamp(streak_contribution + usage_contribution, 15, 25)

streak_contribution = streak_weight × 25
usage_contribution = usage_weight × 25 × (today_distracting_usage / personal_avg_distracting_usage)
```

Where streak_weight and usage_weight are determined by current streak length:

| Streak | streak_weight | usage_weight |
|---|---|---|
| < 10 days | 0.55 | 0.45 |
| 10–20 days | 0.45 | 0.55 |
| 21–29 days | 0.40 | 0.60 |
| 30–44 days | 0.25 | 0.75 |
| 45–59 days | 0.15 | 0.85 |
| 60–89 days | 0.10 | 0.90 |
| ≥ 100 days | 0.00 | 1.00 |

**Note:** Productive app time is excluded from usage calculation. Only time in Distracting-category apps counts.

#### 3.4.4 Outcomes
- **"Keep Going":** modal closes, session resumes
- **"Quit Session":** session ends, streak resets to 0, navigates to home, session logged as manually stopped

---

### 3.5 Streaks

#### 3.5.1 Overview
The streak is a count of consecutive days on which the user met their minimum focus session requirement. It is a core motivation mechanic.

#### 3.5.2 Streak Day Definition
A day counts toward the streak if:
- The number of completed sessions ≥ the user-configured minimum
- If the user-configured minimum = 0: the day neither adds to nor breaks the streak (streak remains unchanged)
- If sessions available for the day < minimum configured: all sessions must be completed for the day to count

#### 3.5.3 Streak Configuration
User sets: "minimum sessions per day to maintain streak" (integer, 0 = streak neutral)

#### 3.5.4 Streak Reset
Streak resets to 0 when:
- User confirms "Quit Session" in the Stop Focusing modal
- User runs out of breaks AND confirms stop

#### 3.5.5 Streak Display
- Home screen: not directly shown (shown via header pills for avg focus)
- Planner screen: streak counter in streak bar (flame icon + "[X] days")
- 2/5/10/20 window: top-right of overlay
- Weekly report: included in report summary
- Stop Focusing modal: shown as "[X] days → 0 days"
- App Limits settings sheet: per-app streak badge

---

### 3.6 App Limits

#### 3.6.1 Overview
App Limits are persistent daily time budgets for individual apps. They are separate from preset-based session blocking. App Limits apply all day, every day, unless paused. They are most similar to iOS Screen Time limits.

#### 3.6.2 Time Limit Configuration
- User sets a daily time limit per app
- Allowed range: 15 minutes to (app's average daily usage + 1 hour)
- Range is computed from Android UsageStats API data — available from day 1 (no cold start problem)
- Minimum selectable: 15 minutes

#### 3.6.3 Extra Time Sessions
Each app limit can have "extra time sessions" — equivalent to breaks for app usage:
- Count: 0–6 extra sessions
- Duration per session: 5, 10, or 15 minutes (chips — no stepper for duration)
- **Hard cap: total extra time ≤ 60 minutes** (enforced — sum of count × duration cannot exceed 60)
- Example: 6 sessions × 10 mins = 60 mins ✓. 6 sessions × 11 mins = 66 mins ✗ (blocked by UI)

#### 3.6.4 Enforcement
When daily limit is hit:
- The 2/5/10/20 intervention window appears when user opens the app
- Extra time sessions are offered if configured
- When extra time sessions are exhausted: same flow as session break exhaustion

#### 3.6.5 Turning Off a Limit
When user turns off an app limit:
- Streak warning modal appears (per-app streak, not global streak)
- 3-second countdown before "Turn Off Limit" button activates
- User selects turn-off duration: Rest of day / Till tomorrow / 7 days
- App shows "Paused — till [date/time]" badge on main screen
- Option to permanently delete the limit (destructive action, danger styling)

#### 3.6.6 App Categories for App Limits
Apps are grouped into: Productive, Semi-Productive, Distracting, Others
- FF ships with a predefined categorization of common apps
- User can recategorize any app at any time
- Distracting apps are added to App Limits by default when a limit is created

---

### 3.7 YouTube Study Mode

#### 3.7.1 Overview
YouTube Study Mode enforces a channel whitelist. Only whitelisted channels are accessible without friction. Non-whitelisted channels trigger an intervention.

#### 3.7.2 Activation
Three ways to activate:
1. Tap "Watch only Study channels on YouTube" card in the 2/5/10/20 intervention window
2. Session preset has YouTube mode set to STUDY_MODE (user's choice — not forced)
3. Manual toggle via edge grabber when inside YouTube

#### 3.7.3 Non-Whitelisted Channel Enforcement
When a video from a non-whitelisted channel is detected:
- Video is closed
- Overlay message: "This channel is not on your study list."
- Options: "Got it" (close) or "Add to Study List" (triggers confirmation flow)

#### 3.7.4 Channel Addition — Confirmation Flow
- A random sentence is displayed from the pool
- User must type the sentence exactly (case-insensitive match)
- On match: channel added, user can watch
- On cancel: channel not added, video remains blocked

**Sentence pool:**
- Channels 1–25: pool of 5 short sentences (low friction, encourages early whitelist building)
- Channels 26+: pool of ~50 longer sentences (meaningful friction)
- No same sentence twice in a row

#### 3.7.5 Edge Grabber
- Visible on right edge of screen when Study Mode is active and YouTube is in foreground
- Collapsed: Electric Indigo vertical tab with FF aperture logo
- Expanded (on tap): floating panel with Study Mode toggle + close button
- Toggle OFF → Study Mode deactivates → 2/5/10/20 window appears for YouTube

#### 3.7.6 Session-Scoped Study Mode
When a focus session ends:
- If Study Mode was activated during the session: it automatically deactivates
- If Study Mode was already active before the session: state is preserved after session ends

#### 3.7.7 Time Windows Outside Focus Session (2/5/10/20)
When not in a focus session and YouTube (or any Distracting app) is opened:
- The 2/5/10/20 window appears
- User selects how long they want to use the app
- After the selected time: content pauses, window reappears with a wait period
- Wait periods: 2 min → 5s wait, 5 min → 10s wait, 10 min → 10s wait, 20 min → 15s wait
- These wait periods do not affect streaks — pure friction only

---

### 3.8 Block Scheduling

#### 3.8.1 Overview
Block scheduling in FF is not a separate system. Scheduled focus blocks are created in two ways:
1. **From FD:** When a FD task with a duration and time is created, FF receives a FocusBlockRequest and creates a matching session automatically
2. **From FF Planner:** User can manually add sessions to the planner

Blocks are visible in the Planner screen as session cards.

#### 3.8.2 FD → FF Sync Rules
| FD Action | FF Response |
|---|---|
| Task created with duration + time | Auto-create linked FocusSession |
| Task deleted | Delete linked FocusSession |
| Task rescheduled | Update FocusSession time |
| Task moved to different list/section | Update FocusSession metadata |
| Recurring task generates new instance | Create new FocusSession for that instance |

Sync is silent — no confirmation prompt required (auto-apply).

#### 3.8.3 15-Minute Heads-Up Notification
For all scheduled sessions (FF-created or FD-synced):
- A notification is pushed 15 minutes before the session start time
- Notification content: "[Session name] starts in 15 minutes"
- Tapping notification: opens FF, navigates to home with the session's preset pre-loaded

---

### 3.9 Screen Time Tracking

#### 3.9.1 Data Source
- Android UsageStats API (`android.permission.PACKAGE_USAGE_STATS`)
- Data available from day 1 — no cold start gap
- Granularity: per-app, per-day

#### 3.9.2 Category Assignment
All installed apps are assigned to one of 4 categories:
- **Productive:** Apps that directly support study/work (e.g. FluxDone, calculators, study apps)
- **Semi-Productive:** Apps that are useful but not directly academic (e.g. Gmail, browser, Maps)
- **Distracting:** Entertainment, social media, games (e.g. Instagram, YouTube general, TikTok)
- **Others:** Utility apps, system apps, anything uncategorized (e.g. Spotify, Camera)

FF ships with a predefined categorization for common apps. Users can recategorize at any time.

#### 3.9.3 Tracked Metrics
- Per-app daily usage time
- Per-category daily totals
- Total screen-on time per day
- Focus time per day (from FF session logs)
- Average daily focus time (rolling week)
- Average daily screen time (rolling week)

#### 3.9.4 Productive Time Exclusion
When calculating usage for the streak wait-time formula: only Distracting-category time is used. Productive, Semi-Productive, and Others are excluded.

---

### 3.10 Planner

#### 3.10.1 Overview
The Planner is a read-only (sessions only) calendar view showing scheduled focus sessions by day. It is minimal — not a task manager. Sessions from both FF and FD are shown.

#### 3.10.2 Features
- Horizontal day strip (Mon–Sun), scrollable to adjacent weeks
- "Today" button to jump to current date
- Stats row: total focus time + total screen time for selected day
- Session list: all sessions for selected day
- "Add Preset" FAB: opens Preset Creation flow

#### 3.10.3 Session Card Information
Each session card shows:
- Preset emoji + session name
- Preset name pill
- Source badge (FF or FD)
- Time range
- Sync icon (↻) for FD-sourced sessions only
- Duration with contextual sub-label: "Planned" (future) / "Live" (active) / "Spent" (completed)

#### 3.10.4 Completed Session Display
- Opacity reduced to 0.6
- Teal left border
- Checkmark indicator
- Duration shows actual time spent (not planned)

---

### 3.11 Weekly Report

#### 3.11.1 Trigger
Every Sunday, a weekly report notification is pushed.

#### 3.11.2 Report Contents
- Current streak (days)
- Total focus time for the week
- Comparison vs previous week (delta, formatted as +/-Xh Ym)
- Best focus day of the week
- Worst focus day of the week
- Auto-generated summary sentence

#### 3.11.3 In-App Summary Card
On Sundays, a dismissible summary card appears at the top of the Weekly tab in Usage Stats.

---

### 3.12 FluxDone Integration

#### 3.12.1 Communication Protocol
- **Android Intent / MethodChannel** (Flutter inter-app communication)
- FF registers an intent filter for FocusBlockRequest actions from FD
- FD sends FocusBlockRequest when a task with duration + time is created/updated/deleted

#### 3.12.2 FocusBlockRequest Payload
```
FocusBlockRequest {
  taskId: String
  taskName: String
  startTime: DateTime (nullable)
  endTime: DateTime (nullable)
  duration: Duration (nullable)
  listId: String
  sectionId: String (nullable)
  action: Enum (CREATE / UPDATE / DELETE)
}
```

#### 3.12.3 FF Response to FocusBlockRequest
- CREATE: Create FocusSession linked to taskId, use last-used preset (or no preset if none)
- UPDATE: Update linked FocusSession's time/duration
- DELETE: Delete linked FocusSession

#### 3.12.4 Visibility of FD Data in FF
FF can read FD's task lists and sections (via MethodChannel query) to allow the user to apply presets to lists/sections from within FF. FF does not write to FD except to create the "Focus Session" task described in §3.2.3.

---

### 3.13 Home Screen Widget

#### 3.13.1 Overview
An Android home screen widget showing today's screen time breakdown.

#### 3.13.2 Widget Contents
- Total screen time today (large, prominent)
- 4-segment donut ring (Productive / Semi-Productive / Distracting / Others)
- Category legend with times
- "Live Focus" indicator when a session is active
- FF branding (aperture icon, small)

#### 3.13.3 Update Frequency
- Every 15 minutes via WorkManager
- Immediate update on session start/end

---

## 4. Non-Functional Requirements

### 4.1 Performance
- App cold start: < 2 seconds
- Session timer accuracy: ±1 second
- Widget refresh: ≤ 15 minute delay from actual data change
- UsageStats query: < 500ms

### 4.2 Storage
- Local SQLite for: sessions, presets, streaks, session history
- Hive for: user preferences, app categorization, channel whitelist
- No cloud sync in Phase 1

### 4.3 Permissions Required
| Permission | Purpose |
|---|---|
| `PACKAGE_USAGE_STATS` | Screen time data |
| `SYSTEM_ALERT_WINDOW` | App limit overlay (2/5/10/20 window) |
| `ACCESSIBILITY_SERVICE` | App detection, redirecting from distracted apps |
| `FOREGROUND_SERVICE` | Focus timer running in background |
| `RECEIVE_BOOT_COMPLETED` | Restore block schedules after reboot |
| `VIBRATE` | Session end / break end feedback |
| `POST_NOTIFICATIONS` | Session heads-up, weekly report, break end |

### 4.4 Offline-First
- All features must function without internet
- No API calls to external services in Phase 1
- FD communication is local IPC only

### 4.5 Modularity
- Each feature module (Timer, AppLimits, Streaks, Planner, StudyMode, FDIntegration) must be independently functional
- A crash or data corruption in one module must not affect others
- All modules communicate via internal event bus / repository pattern

---

## 5. Out of Scope (Phase 1)

- iOS and Web versions
- Cloud sync or backup
- Multiplayer / social features
- Built-in focus music
- AI companion or AI-generated insights
- Lock screen widgets
- Notification-based task creation
- Any FD UI changes (FD is a separate app)

---

## 6. Open Questions / Future Considerations

- Phase 2: iOS — will require rewrite of Accessibility Service and UsageStats equivalents using Screen Time API
- Phase 2: Web — focus timer only (no app blocking in browser context)
- Future: cross-device streak sync (requires cloud layer)
- Future: FD ↔ FF bidirectional richer sync (e.g. session completion updating FD task status)

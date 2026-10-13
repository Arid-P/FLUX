# 📅 Topic FF-P2-2: Advanced Scheduled Sessions & RRULE Calendar Engine Specification

**Document ID:** `FF_P2_SCHEDULED_SESSIONS_SPEC`  
**Phase:** Phase 2 (Hardened Enforcement & Advanced Scheduling)  
**Target Package:** `com.flux.focus.features.scheduled`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + Android `AlarmManager` Exact Alarms + `WorkManager`)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Scheduled Sessions provide FluxFoxus with standalone, calendar-grade focus scheduling (`session_origin = 'SCHEDULED'`). While manual sessions (`MANUAL`) are initiated on impulse and task sessions (`TASK_SYNC`) originate from FluxDone integration, **Scheduled Sessions are immutable, high-discipline commitments** created directly within FluxFoxus.

### 1.1 The Inviolable Automatic Start Principle
* **No Waiting for User Permission:** In standard calendar or pomodoro apps, scheduled sessions post a passive notification and wait for the user to open the app and tap "Start". If the user is unmotivated or distracted, they simply ignore the alert.
* **Autonomous Enforcement:** In FluxFoxus, **scheduled sessions start automatically on the second**. At the exact scheduled start time, the background Foreground Service activates, Room writes `status = 'ACTIVE'`, and the Accessibility Engine immediately locks all restricted apps.
* **Discipline Over Autopilot:** If the user is watching YouTube or scrolling social media when the scheduled time arrives, the blocking intervention cuts in instantly.

---

## 2. Dependencies & System Interactions

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Room Session Entity** | [`FF_P1_PRESETS_AND_FOCUS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESETS_AND_FOCUS_SPEC.md) | Uses unified `focus_sessions` table with `session_origin = 'SCHEDULED'` and `status = 'PLANNED'`. |
| **Blocking Engine** | [`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md) | Enforces linked preset rules immediately upon auto-start. |
| **Clock Surface** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Launched via `fullScreenIntent` if screen is off/locked, or heads-up banner if screen is on. |
| **Anti-Tamper Guard** | [`FF_P2_UNINSTALL_PROTECTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_UNINSTALL_PROTECTION_SPEC.md) | Mid-session inviolability locks Device Admin removal while the auto-started session is active. |

---

## 3. Scheduling Architecture & Android System Integration

### 3.1 Exact Alarms & Foreground Service Pipeline

```
+-----------------------------------------------------------------------------------+
|                            Android AlarmManager                                   |
|                setExactAndAllowWhileIdle(RTC_WAKEUP, triggerTime)                 |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                     ScheduledSessionAlarmReceiver : BroadcastReceiver             |
|  1. Wakes device from deep Doze.                                                  |
|  2. Fetches Session by ID from FocusSessionDao.                                   |
|  3. Starts FocusSessionService via ContextCompat.startForegroundService().       |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                        FocusSessionService : ForegroundService                    |
|  1. Promotes notification to ONGOING High-Priority foreground channel.           |
|  2. Updates Room DB: status = 'ACTIVE', actual_start_time = System.currentTimeMillis()|
|  3. Broadcasts com.flux.action.SESSION_STATE_CHANGED (AccessibilityService locks)|
|  4. Fires FullScreenIntent (if device locked) -> Opens FlipClockActivity.        |
+-----------------------------------------------------------------------------------+
```

### 3.2 Permission Model (Android 12+ / 14+)
* **`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`:** Required in `AndroidManifest.xml`.
* **Runtime Verification:** `alarmManager.canScheduleExactAlarms()` is checked during onboarding and schedule creation. If disabled by the OS, a prompt directs the user to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.

---

## 4. Feature Specifications & Edge Case Rules

### 4.1 Automatic Start Mechanics
1. **Punctual Execution:** At `startTime` (e.g. 2:00:00 PM), `AlarmManager` fires.
2. **Immediate Enforcement:** The linked preset's whitelist/blacklist takes effect without waiting for user action.
3. **Notification Surface:**
   * **Screen On:** High-priority heads-up banner: *"🔔 [Session Title] has started. Focus Mode is ACTIVE."* Tapping opens the 3D Flip-Clock.
   * **Screen Off / Locked:** Android `fullScreenIntent` launches `FlipClockActivity` over the keyguard.

---

### 4.2 Mid-Window Device Reboot / Turn-On Behavior (The Catch-Up Rule)

#### Concrete Real-World Example:
> **The Scenario:**  
> You have a scheduled focus session: **"Deep Study" from 2:00 PM to 4:00 PM** (Target duration: 2 hours).
> 
> * **Case A (Brief Reboot / Battery Died):**  
>   At 1:55 PM, your phone turns off or battery dies. At **2:15 PM**, you plug it in and power it back on.  
>   FluxFoxus boots up via `BootCompletedReceiver`. It detects that current time (2:15 PM) is inside the scheduled window (2:00 PM – 4:00 PM). The remaining duration is **1 hour 45 minutes (105 mins)**.  
>   Because 105 mins exceeds your configured threshold, FluxFoxus **auto-starts immediately** for the remaining 1 hour 45 minutes (ending at 4:00 PM sharp). Focus blocking engages the second your home screen appears!
> 
> * **Case B (Extended Power Off):**  
>   You leave your phone off all afternoon and power it on at **3:55 PM**. Only **5 minutes** remain before the session ends.  
>   Because 5 minutes is less than your configured threshold (e.g., 15 mins), FluxFoxus **does not** abruptly lock your phone for an impractical 5-minute block. Instead, it marks the session as `EXPIRED_ON_BOOT` and alerts you with a summary.

#### User-Configurable Threshold in Settings:
* **Setting Location:** `Settings → Sessions → Scheduled Sessions → Boot Resume Threshold`
* **Options:** `5 minutes | 10 minutes | 15 minutes (Default) | 20 minutes | 30 minutes`
* **Formula:**
  $$\text{remainingMinutes} = \frac{\text{scheduledEndTime} - \text{currentTimeMillis}}{60{,}000}$$
  * If $\text{remainingMinutes} \ge \text{userThreshold}$: **Auto-starts immediately** for remaining duration.
  * If $\text{remainingMinutes} < \text{userThreshold}$: Marks session `SKIPPED`, logs missed block.

---

### 4.3 Ongoing Manual Session Conflict (Smooth Handover)

If the user is already in an active manual focus session (`session_origin = 'MANUAL'`) when a scheduled session arrives:

```
+-----------------------------------------------------------------------------------+
|               1:40 PM - 2:00 PM (Manual Focus Session Running)                    |
|                        Elapsed: 20 minutes focused                                |
+-----------------------------------------------------------------------------------+
                                          |
                      2:00 PM: Scheduled Session Alarm Hits!
                                          v
+-----------------------------------------------------------------------------------+
|                             Smooth Session Handover                               |
|  1. Manual session ends gracefully and is marked COMPLETED (saving 20m stats).    |
|  2. Scheduled session starts instantly (0s delay):                               |
|     - Preset swaps to the Scheduled Session's configured preset.                 |
|     - Title updates to Scheduled Session Title.                                  |
|     - Clock resets or chains target time to scheduled end time.                   |
|  3. Audio Cue: Smooth transition chime (zero interruption in blocking state).     |
+-----------------------------------------------------------------------------------+
```

---

### 4.4 Configurable 15-Minute Heads-Up Reminder
* **Per-Schedule Toggle:** During schedule creation/editing, the user can toggle:
  ```
  🔔 15-Minute Heads-Up Notification      [ ON / OFF ] (Default: ON)
  ```
* **Informational Philosophy (Zero Skip Buttons):**
  * The reminder exists **strictly to inform the user** so they can wrap up calls, save documents, or prepare their desk.
  * **No Skip Button:** The notification does **not** have a skip or cancel action. A user cannot impulsively bypass the upcoming session from the lock screen.
* **Notification Actions:**
  * `[ Start Early ]` → Immediately begins the scheduled session ahead of time (extends total focus).
  * `[ Dismiss ]` → Clears the heads-up banner; session will still auto-start at the scheduled minute.

---

### 4.5 Calendar Conflict & Overlap Warning
* **Non-Blocking Rule:** Overlapping scheduled sessions are permitted.
* **UI Warning:** If a planned session overlaps an existing one, the creation sheet displays a warning chip:
  * Container: Surface Card `#1B263B`, Border `#E07A5F` (Coral).
  * Label: *"⚠️ Overlaps with [Session Name] ([Start]–[End])"*.
  * The user can still proceed and save.

---

## 5. Recurrence Engine (RRULE / RFC-5545)

### 5.1 Recurrence Pattern Schema
Recurrence rules are stored in `focus_sessions.recurrence_rule` as JSON:

```json
{
  "freq": "WEEKLY",
  "interval": 1,
  "byDay": ["MO", "TU", "WE", "TH", "FR"],
  "until": 1799884800000,
  "count": null
}
```

### 5.2 Recurrence Scopes (Edit & Delete)

When an existing recurring session instance is modified or deleted from the Planner:

```
+-------------------------------------------------------+
|  Edit Recurring Session                               |
|                                                       |
|  This session is part of a recurring series.          |
|  Which sessions would you like to update?             |
|                                                       |
|  (●) This session only                                |
|      Applies change only to today's instance.         |
|                                                       |
|  ( ) This and following sessions                      |
|      Splits series and updates future occurrences.    |
|                                                       |
|  ( ) All sessions in series                           |
|      Modifies all past and future series instances.   |
|                                                       |
|  [ Cancel ]                         [ Confirm Update ]|
+-------------------------------------------------------+
```

---

## 6. Database Schema & Data Models

### 6.1 Unified `focus_sessions` Table (Room Entity)

```kotlin
package com.flux.focus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("preset_id"),
        Index("start_time"),
        Index("series_id"),
        Index("status")
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_origin")
    val sessionOrigin: String, // "MANUAL", "TASK_SYNC", "SCHEDULED"

    @ColumnInfo(name = "preset_id")
    val presetId: String,

    @ColumnInfo(name = "session_title")
    val sessionTitle: String,

    @ColumnInfo(name = "linked_task_id")
    val linkedTaskId: String? = null,

    @ColumnInfo(name = "timer_mode")
    val timerMode: String = "COUNTDOWN", // "COUNTDOWN", "STOPWATCH"

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long,

    @ColumnInfo(name = "actual_start_time")
    val actualStartTime: Long? = null,

    @ColumnInfo(name = "target_duration_minutes")
    val targetDurationMinutes: Int,

    @ColumnInfo(name = "actual_duration_seconds")
    val actualDurationSeconds: Int = 0,

    @ColumnInfo(name = "breaks_taken")
    val breaksTaken: Int = 0,

    @ColumnInfo(name = "breaks_total_seconds")
    val breaksTotalSeconds: Int = 0,

    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null, // JSON RRULE

    @ColumnInfo(name = "series_id")
    val seriesId: String? = null, // Links recurring instances

    @ColumnInfo(name = "status")
    val status: String, // "PLANNED", "ACTIVE", "PAUSED", "COMPLETED", "SKIPPED", "EXPIRED_ON_BOOT"

    @ColumnInfo(name = "penalty_delay_applied_seconds")
    val penaltyDelayAppliedSeconds: Int = 0,

    @ColumnInfo(name = "headsup_notification_armed")
    val headsupNotificationArmed: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
```

### 6.2 Settings DataStore Key for Boot Resume Threshold

```kotlin
package com.flux.focus.features.scheduled.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey

object ScheduledSessionPreferenceKeys {
    // Threshold in minutes (Default: 15)
    val BOOT_RESUME_THRESHOLD_MINUTES = intPreferencesKey("boot_resume_threshold_minutes")
}
```

---

## 7. Jetpack Compose UI Specifications

### 7.1 Schedule Creation Bottom Sheet (`/planner/session/create`)

```
+-------------------------------------------------------+
|  ━  Schedule Focus Session                            |
+-------------------------------------------------------+
|                                                       |
|  SESSION TITLE                                        |
|  +-------------------------------------------------+  |
|  | Deep Research & Drafting                        |  |
|  +-------------------------------------------------+  |
|                                                       |
|  SELECT PRESET                                        |
|  [ ⚡ Deep Work ] [ 📚 Study Mode ] [ 🎯 Coding ]     |
|                                                       |
|  DATE & TIME                                          |
|  Date: Today, Oct 12                                  |
|  From: [ 02:00 PM ]   To: [ 04:00 PM ] (120 mins)     |
|                                                       |
|  RECURRENCE                                           |
|  [ None ]  [ Daily ]  [ Mon–Fri ]  [ Custom ]         |
|                                                       |
|  HEADS-UP ALERT                                       |
|  +-------------------------------------------------+  |
|  | 🔔 15m Heads-Up Notification      [ ON ] (●)    |  |
|  | Alerts you 15 minutes before auto-lock          |  |
|  +-------------------------------------------------+  |
|                                                       |
|  ⚠️ Overlaps with "Reading" (03:30 PM - 04:30 PM)      |
|                                                       |
|  +-------------------------------------------------+  |
|  |             [ Schedule Session ]                |  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

### 7.2 Holst Design Tokens Applied

| Element | Token / Color | Value / Usage |
|---|---|---|
| Sheet Surface | `SurfaceCard` | `#1B263B` |
| Primary Accent | `AccentSage` | `#778D7A` (Active pills, Primary CTA) |
| Highlight Text | `PrimarySand` | `#D4C4A8` (Time values, Headers) |
| Non-Blocking Warning | `DangerCoral` | `#E07A5F` (Overlap indicator chip) |
| Body Typography | `TextLight` | `#F4F1DE` (Input text, labels) |
| Secondary Text | `TextMuted` | `#A0AAB2` (Subtitle descriptions) |

---

## 8. Verification & Branch-Testing Isolation Workflow

### 8.1 Zero-Mock Automated & Manual Test Cases
1. **Auto-Start On The Dot Test:**
   * Create scheduled session for current time + 2 minutes.
   * Lock screen or switch to YouTube.
   * **Verify:** At the exact minute, foreground service starts, heads-up/full-screen intent triggers, and opening YouTube displays the intervention block screen.
2. **Boot Mid-Window Catch-Up Test:**
   * Configure `Boot Resume Threshold = 15m`.
   * Schedule session: 2:00 PM – 4:00 PM.
   * Simulate `BOOT_COMPLETED` broadcast at 2:30 PM (remaining = 90m).
   * **Verify:** Session auto-starts immediately for 90 minutes.
   * Simulate `BOOT_COMPLETED` broadcast at 3:50 PM (remaining = 10m).
   * **Verify:** Session does not start; status transitions to `EXPIRED_ON_BOOT`.
3. **Smooth Handover Test:**
   * Start manual session at 1:45 PM.
   * Scheduled session set for 2:00 PM.
   * At 2:00 PM, verify manual session completes into Room, and scheduled session seamlessly takes over.

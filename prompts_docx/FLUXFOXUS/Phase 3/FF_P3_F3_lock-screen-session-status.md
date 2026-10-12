# FF Phase 3 — Feature 3: Lock Screen Session Status

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

While a focus session is active, the lock screen displays the flip clock and remaining session time as a persistent ambient reminder. The user does not need to unlock and open FF to check how much time is left — it is always visible on the lock screen.

---

## 2. P1/P2 References

| Reference | Location |
|---|---|
| Focus Timer — flip clock display (HH:MM) | PRD §3.2.2 |
| Focus Session — foreground service | TRD §1.4 (`flutter_foreground_task`) |
| Session status enum | TRD §3.2 |
| Break system — break countdown | PRD §3.3.3 |
| Session end — vibration + notification | PRD §3.2.5 |

---

## 3. Feature Specification

### 3.1 Trigger

Lock screen display activates when:
- A focus session status = `active`
- OR a break countdown is active

Lock screen display deactivates when:
- Session status transitions to `completed`, `stopped`, or `skipped`
- Break ends and session resumes (reverts to session display)

### 3.2 Lock Screen Content

**During active session:**

```
┌─────────────────────────────────┐
│  [FF aperture icon]  FluxFoxus  │
│                                 │
│       ┌──────┐  ┌──────┐       │
│       │  01  │  │  43  │       │
│       └──────┘  └──────┘       │
│         HRS        MINS         │
│                                 │
│  Python Practice                │
│  Ends at 11:00 PM               │
│                                 │
│  [■ Stop]          [⏸ Break]   │
└─────────────────────────────────┘
```

- **Flip clock:** HH:MM remaining (Countdown mode) or elapsed (Stopwatch / Open-Ended)
- **Session name:** truncated at 32 chars with ellipsis
- **End time:** *"Ends at [HH:MM AM/PM]"* for Countdown. *"Started at [HH:MM]"* for Stopwatch/Open-Ended
- **Two action buttons:**
  - Stop — triggers Stop Focusing modal (user must unlock to interact with modal)
  - Break — triggers break immediately if breaks remaining. Greyed if exhausted or 0 breaks preset

**During break countdown:**

```
┌─────────────────────────────────┐
│  [FF aperture icon]  FluxFoxus  │
│                                 │
│           BREAK                 │
│       ┌──────┐  ┌──────┐       │
│       │  00  │  │  08  │       │
│       └──────┘  └──────┘       │
│         HRS        MINS         │
│                                 │
│  Python Practice                │
│  Break [N] of [total]           │
│                                 │
│              [⏭ End Break]     │
└─────────────────────────────────┘
```

- Flip clock shows break time remaining in Soft Cyan (matching P1 break screen — PRD §3.3.3)
- *"BREAK"* label above clock in Soft Cyan
- *"Break [N] of [total]"* sub-label
- Single action: End Break Early

### 3.3 Action Button Behaviour

Lock screen action buttons (Stop / Break / End Break) trigger their respective flows but require the device to be unlocked to complete the interaction:

- **Stop:** tapping on lock screen navigates to FF's Stop Focusing modal on unlock. Does not directly end the session from the lock screen
- **Break:** if breaks remaining, starts break immediately (no unlock required for this action). Break countdown appears on lock screen
- **End Break Early:** ends break immediately, resumes session timer on lock screen (no unlock required)

This is consistent with Android's notification action model — destructive actions require unlock, non-destructive actions can execute directly.

### 3.4 Toggle

**Location:** Settings → Sessions → Lock Screen Display

| Control | Type | Default |
|---|---|---|
| Show session on lock screen | Toggle | ON |

When OFF: no lock screen content. The existing foreground service notification (TRD §1.4) continues to run (required for timer accuracy) but no lock screen UI is shown.

---

## 4. Implementation Approach

### 4.1 Android Mechanism

Android lock screen widgets were deprecated in API 17. The correct modern approach is a **Media-Style Notification** displayed on the lock screen, combined with a `Notification.Builder` with `setVisibility(VISIBILITY_PUBLIC)`.

This is the same mechanism used by Spotify, YouTube Music, and other timer/media apps to show persistent lock screen content.

```kotlin
// In FluxFoxusNotificationService (Kotlin platform channel)
val notification = NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
    .setContentTitle(sessionName)
    .setContentText(formattedTimeRemaining)
    .setSmallIcon(R.drawable.ic_ff_aperture)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
    .addAction(R.drawable.ic_break, "Break", breakPendingIntent)
    .setStyle(MediaStyleCompat()
        .setShowActionsInCompactView(0, 1))
    .build()
```

### 4.2 Custom Lock Screen Layout

For the flip clock visual (not achievable with standard notification style), a **RemoteViews** custom notification layout is used:

```kotlin
val remoteViews = RemoteViews(context.packageName, R.layout.lock_screen_session)
remoteViews.setTextViewText(R.id.tv_hours, formattedHours)
remoteViews.setTextViewText(R.id.tv_minutes, formattedMinutes)
remoteViews.setTextViewText(R.id.tv_session_name, sessionName)
remoteViews.setTextViewText(R.id.tv_end_time, endTimeLabel)

notification.setCustomBigContentView(remoteViews)
```

**Note:** The full mechanical flip animation from the in-app session screen is NOT replicated on the lock screen. The lock screen shows static digit panels that update every second via notification update calls. True flip animation requires a live View — not possible in RemoteViews.

### 4.3 Update Frequency

The foreground service (already running in P1 for timer accuracy) calls `notificationManager.notify()` every second to update the displayed time. This is the same pattern used by countdown timer apps on Android and is acceptable for foreground services.

### 4.4 Flutter ↔ Kotlin Bridge

The lock screen notification is managed entirely in Kotlin (platform side), not in Flutter widgets. The existing `flutter_foreground_task` (TRD §1.4) already runs a foreground service — the lock screen notification is an extension of that service's notification, not a new service.

Flutter side sends session state updates to the Kotlin service via MethodChannel:

```dart
// New method on existing IPC channel or dedicated channel
await platform.invokeMethod('updateLockScreenSession', {
  'sessionName': session.sessionName,
  'timeRemainingSeconds': timeRemaining.inSeconds,
  'mode': session.mode.name,
  'endTime': session.scheduledEnd?.millisecondsSinceEpoch,
  'breaksRemaining': breaksRemaining,
  'isBreakActive': isBreakActive,
  'breakTimeRemainingSeconds': breakTimeRemaining?.inSeconds,
});
```

---

## 5. Module Boundary

**Owned by:** `focus_timer/` module + new Kotlin platform service extension

```
features/
└── focus_timer/
    ├── data/
    │   └── lock_screen_service.dart     ← NEW (MethodChannel wrapper)
    └── presentation/
        └── focus_session_screen.dart    ← MODIFIED (sends state updates to lock screen service)

android/
└── app/src/main/kotlin/
    └── LockScreenSessionService.kt      ← NEW (RemoteViews + notification management)
    └── lock_screen_session.xml          ← NEW (RemoteViews layout)
```

Modifications to existing modules:
- `settings/presentation/` — Lock Screen Display toggle in Sessions section

# 🔒 Topic FF-P3-3: Lock Screen Live Focus Session Status Specification

**Document ID:** `FF_P3_LOCK_SCREEN_STATUS_SPEC`  
**Phase:** Phase 3 (Intelligence, Lock Screen Presence & Session Ergonomics)  
**Target Package:** `com.flux.focus.features.timer.notification`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Android Foreground Service + `NotificationCompat` + Custom `RemoteViews` + Hardware Chronometer)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

During intense study sessions, users keep their phone on their desk. Picking up the phone, unlocking it, and navigating apps just to check "how much time is left" frequently triggers impulse checks that derail deep focus.

**Lock Screen Live Session Status** projects a clean, ambient flip-clock card directly onto the Android lock screen. The user can glance at remaining time with **zero phone unlocks**, while maintaining strict friction against accidental or impulsive session termination.

### 1.1 Core Principles
* **Glanceable Ambient Surface:** Clock digits and session title remain visible on the keyguard at all times.
* **Zero-CPU Hardware Chronometer:** Uses Android's native `android.widget.Chronometer` view inside `RemoteViews` so the Android display hardware decrements seconds without waking the application CPU every 1,000ms.
* **Asymmetric Action Security:** Non-destructive actions (Take Break, End Break Early) execute instantly from the lock screen. Destructive termination (Stop Session) requires device unlock and directs the user to the **15–25s Asymmetric Friction Gate**.
* **Zero Accidental Dismissal:** Marked `setOngoing(true)` to prevent swipe clearing.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Focus Timer Service** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | `FocusTimerService` updates the lock screen notification upon state transitions (`ACTIVE`, `PAUSED_BREAK`, `COMPLETED`). |
| **Quit Friction Gate** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Tapping "Stop Session" on lock screen unlocks into the 15–25s locked quit gate; zero bypass from the notification shade. |
| **Preset Break Allocation** | [`FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md) | Displays `Break [N] of [Total]`. When breaks reach 0, the lock-screen break button greys out. |

---

## 3. Lock Screen Visual States & Behavior

### 3.1 State A: Active Focus Session Running

```
+-------------------------------------------------------+
|  [⚙️ FF] FluxFoxus                          Active    |
|                                                       |
|       ┌──────┐  ┌──────┐                              |
|       │  01  │  │  42  │   Ends at 11:00 PM           |
|       └──────┘  └──────┘   Deep Focus Block           |
|         HRS        MINS                               |
|                                                       |
|  [ ⏸ Take Break ]             [ ■ Stop Session ]     |
+-------------------------------------------------------+
```

* **Visual Elements:**
  * Background: Oxford Slate `#1B263B` with Muted Steel border `#415A77`.
  * Digits: Primary Sand `#D4C4A8` on Abyssal Navy `#0D1B2A` card tiles.
  * Subtext: Session name + projected end time (e.g. *"Ends at 11:00 PM"*).
* **Action Buttons:**
  * `[ ⏸ Take Break ]`: Active in Sage Green `#778D7A` if preset breaks remaining.
  * **Exhausted Breaks Rule:** Once preset breaks hit 0, the button is **greyed out and disabled** (`alpha = 0.38`, non-clickable). AI break negotiation is reserved for in-app flow.
  * `[ ■ Stop Session ]`: Styled in Muted Surface `#1B263B` with border `#415A77`.

---

### 3.2 State B: Break Countdown Active

```
+-------------------------------------------------------+
|  [⚙️ FF] FluxFoxus                       ☕ On Break   |
|                                                       |
|       ┌──────┐  ┌──────┐                              |
|       │  00  │  │  04  │   Break 1 of 2               |
|       └──────┘  └──────┘   Resting...                 |
|         MINS       SECS                               |
|                                                       |
|  [ ⏭ End Break Early ]                                |
+-------------------------------------------------------+
```

* **Visual Elements:**
  * Header Badge: *"☕ On Break"* in Sage Green `#778D7A`.
  * Sub-label: *"Break [N] of [Total] · Resting..."*.
  * Action: Full-width `[ ⏭ End Break Early ]` button.

---

## 4. Action Security Model & Lock Screen Interaction Rules

```
+-----------------------------------------------------------------------------------+
|                        Action Security Matrix on Lock Screen                      |
+-----------------------------------------------------------------------------------+
|  Action               | Device Unlock Required? | Target Destination              |
|-----------------------|-------------------------|---------------------------------|
|  [ ⏸ Take Break ]     | NO (Zero friction)      | FocusTimerService (Starts Break)|
|  [ ⏭ End Break Early] | NO (Zero friction)      | FocusTimerService (Resumes Work)|
|  [ ■ Stop Session ]   | YES (Mandatory Unlock)  | FlipClockActivity (Quit Gate)   |
|  Tap Notification Body| YES (Standard Android)  | FlipClockActivity (App Surface) |
+-----------------------------------------------------------------------------------+
```

### 4.1 Stop Session Destructive Handling
* When the user taps `[ ■ Stop Session ]` on the keyguard:
  1. The `PendingIntent` does **not** broadcast a termination event.
  2. It launches `FlipClockActivity` with an intent extra `EXTRA_TRIGGER_QUIT_MODAL = true`.
  3. Android OS requests fingerprint, face, or PIN unlock.
  4. Once unlocked, the user is landed directly in the **15–25s Asymmetric Friction Gate** (`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`).
  5. It is physically impossible to kill a session directly from the lock screen without enduring the friction cooldown.

---

## 5. Technical Implementation (Native Android Kotlin)

### 5.1 RemoteViews Custom Layout (`res/layout/notification_focus_lock_screen.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="#1B263B"
    android:padding="16dp">

    <!-- Header Row -->
    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <TextView
            android:id="@+id/tv_app_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="FluxFoxus"
            android:textColor="#F4F1DE"
            android:textSize="12sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/tv_status_badge"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_alignParentEnd="true"
            android:text="Active"
            android:textColor="#778D7A"
            android:textSize="12sp"
            android:textStyle="bold" />
    </RelativeLayout>

    <!-- Center Content: Chronometer & Session Details -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="12dp"
        android:gravity="center_vertical">

        <!-- Hardware Chronometer (Zero CPU Wakeup) -->
        <Chronometer
            android:id="@+id/chronometer_countdown"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:countDown="true"
            android:textColor="#D4C4A8"
            android:textSize="32sp"
            android:textStyle="bold"
            android:fontFamily="monospace"
            android:background="#0D1B2A"
            android:paddingHorizontal="12dp"
            android:paddingVertical="4dp" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:layout_marginStart="14dp">

            <TextView
                android:id="@+id/tv_session_name"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Deep Focus Block"
                android:textColor="#F4F1DE"
                android:textSize="14sp"
                android:textStyle="bold"
                android:maxLines="1"
                android:ellipsize="end" />

            <TextView
                android:id="@+id/tv_end_time_hint"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Ends at 11:00 PM"
                android:textColor="#A0AAB2"
                android:textSize="12sp"
                android:layout_marginTop="2dp" />
        </LinearLayout>
    </LinearLayout>

    <!-- Action Buttons Row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="40dp"
        android:orientation="horizontal"
        android:layout_marginTop="14dp">

        <Button
            android:id="@+id/btn_action_break"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:text="Take Break"
            android:textColor="#F4F1DE"
            android:textSize="12sp"
            android:background="#778D7A" />

        <Space
            android:layout_width="12dp"
            android:layout_height="match_parent" />

        <Button
            android:id="@+id/btn_action_stop"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:text="Stop Session"
            android:textColor="#F4F1DE"
            android:textSize="12sp"
            android:background="#415A77" />
    </LinearLayout>
</LinearLayout>
```

---

### 5.2 Notification Builder in `FocusLockScreenNotificationManager.kt`

```kotlin
package com.flux.focus.features.timer.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.flux.focus.R
import com.flux.focus.features.focus.FlipClockActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusLockScreenNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "flux_focus_active_session"
        const val NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannel()
    }

    fun buildLockScreenNotification(
        sessionTitle: String,
        targetDurationMs: Long,
        endTimeFormatted: String,
        breaksRemaining: Int,
        isBreakActive: Boolean,
        remainingTimeMs: Long
    ): Notification {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_focus_lock_screen)

        // Set Texts
        remoteViews.setTextViewText(R.id.tv_session_name, sessionTitle)
        
        // Zero-CPU Hardware Chronometer Setup
        val baseTime = SystemClock.elapsedRealtime() + remainingTimeMs
        remoteViews.setChronometer(R.id.chronometer_countdown, baseTime, null, true)

        if (isBreakActive) {
            remoteViews.setTextViewText(R.id.tv_status_badge, "☕ On Break")
            remoteViews.setTextViewText(R.id.tv_end_time_hint, "Resting...")
            remoteViews.setTextViewText(R.id.btn_action_break, "End Break Early")
            remoteViews.setViewVisibility(R.id.btn_action_stop, View.GONE)
            
            val endBreakIntent = Intent(context, FocusActionReceiver::class.java).apply {
                action = "ACTION_END_BREAK_EARLY"
            }
            remoteViews.setOnClickPendingIntent(
                R.id.btn_action_break,
                PendingIntent.getBroadcast(context, 101, endBreakIntent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            remoteViews.setTextViewText(R.id.tv_status_badge, "Active")
            remoteViews.setTextViewText(R.id.tv_end_time_hint, "Ends at $endTimeFormatted")
            remoteViews.setViewVisibility(R.id.btn_action_stop, View.VISIBLE)

            // Break Button Logic (Grey out if 0 breaks remaining)
            if (breaksRemaining > 0) {
                remoteViews.setTextViewText(R.id.btn_action_break, "Take Break ($breaksRemaining)")
                val takeBreakIntent = Intent(context, FocusActionReceiver::class.java).apply {
                    action = "ACTION_TAKE_PRESET_BREAK"
                }
                remoteViews.setOnClickPendingIntent(
                    R.id.btn_action_break,
                    PendingIntent.getBroadcast(context, 102, takeBreakIntent, PendingIntent.FLAG_IMMUTABLE)
                )
            } else {
                remoteViews.setTextViewText(R.id.btn_action_break, "No Breaks Left")
                remoteViews.setInt(R.id.btn_action_break, "setBackgroundColor", 0x60415A77) // Dimmed
                remoteViews.setOnClickPendingIntent(R.id.btn_action_break, null) // Disabled
            }

            // Stop Button (Requires Unlock -> Direct to FlipClockActivity)
            val stopIntent = Intent(context, FlipClockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_TRIGGER_QUIT_MODAL", true)
            }
            remoteViews.setOnClickPendingIntent(
                R.id.btn_action_stop,
                PendingIntent.getActivity(context, 103, stopIntent, PendingIntent.FLAG_IMMUTABLE)
            )
        }

        // Tap Body Opens App
        val contentIntent = Intent(context, FlipClockActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(context, 100, contentIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ff_aperture)
            .setCustomBigContentView(remoteViews)
            .setCustomContentView(remoteViews)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Focus Session",
            NotificationManager.IMPORTANCE_LOW // Low importance prevents intrusive audio dings on every state shift
        ).apply {
            description = "Displays live flip clock countdown and session status on the lock screen."
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}
```

---

## 6. Settings Configuration

* **Location:** `Settings → Sessions → Lock Screen Display`
* **Toggle:** `Show Live Session on Lock Screen` (`ON` by default).
* **DataStore Preference:**
  ```kotlin
  val SHOW_LOCK_SCREEN_SESSION = booleanPreferencesKey("show_lock_screen_session") // Default: true
  ```
* **Behavior:** When toggled OFF, `FocusTimerService` retains its minimal background notification for foreground service lifecycle, but removes the public custom `RemoteViews` lock screen card.

---

## 7. Verification & Branch-Testing Isolation Workflow

### 7.1 Zero-Mock Automated & Manual Test Cases
1. **Lock Screen Visibility Test:**
   * Start 60-minute session.
   * Lock phone display.
   * **Verify:** Lock screen displays custom card with `Chronometer` smoothly counting down in Primary Sand `#D4C4A8`.
2. **Non-Destructive Break Action Test:**
   * Tap `[ ⏸ Take Break ]` on lock screen.
   * **Verify:** Session enters break mode immediately; card transitions to `☕ On Break` with break countdown without asking for fingerprint/PIN.
3. **Exhausted Breaks Disabled Test:**
   * Consume all preset breaks.
   * Lock screen.
   * **Verify:** Break button is dimmed, text reads *"No Breaks Left"*, and tapping does nothing.
4. **Destructive Friction Test:**
   * Tap `[ ■ Stop Session ]` on lock screen.
   * **Verify:** Android prompts for fingerprint/PIN unlock.
   * Upon unlocking, device opens directly to `FlipClockActivity` with the 15–25s quit gate countdown running.

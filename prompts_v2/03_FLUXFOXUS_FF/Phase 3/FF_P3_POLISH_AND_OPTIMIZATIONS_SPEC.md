# 🛡️ Topic FF-P3-5: Edge Cases, Battery Optimizations & System Polish Specification

**Document ID:** `FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC`  
**Phase:** Phase 3 (Intelligence, Lock Screen Presence & Session Ergonomics)  
**Target Package:** `com.flux.focus.core.optimization`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Android Accessibility Node Traversal + Hardware Monotonic Clock + Doze Profiling)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Focus enforcers operating on mobile devices face intense operating system hostility: aggressive OEM background cleaners kill timers, dynamic UI view recycling circumvents accessibility interceptors, and system clock shifts corrupt session calculations.

This specification hardens FluxFoxus against real-world Android platform anomalies, ensuring **rock-solid reliability, battery conservation, and zero timer corruption**.

---

## 2. Critical Android Edge-Case Hardening

### 2.1 EC-1: YouTube Shorts Accessibility Tree & Whitelist Matching
* **The Problem:** In the YouTube Android app (`com.google.android.youtube`), the Shorts player uses a distinct view hierarchy from standard watch pages. View IDs are frequently obfuscated (`shorts_container`, `reel_player`), and channel handle nodes (`@channelName`) are lazily attached to dynamic reel view holders.
* **The Hardened Inspection Strategy:**
  1. `FocusAccessibilityService` inspects the root window node hierarchy when YouTube is in foreground during `STUDY_MODE`.
  2. If the current view matches a Shorts player container (`reel_recycler` or presence of Shorts UI elements):
     - Traverses accessibility node child tree to locate channel identifiers:
       ```kotlin
       fun extractYouTubeShortsChannelHandle(rootNode: AccessibilityNodeInfo): String? {
           // Searches for '@' prefix channel handles or author view nodes
           val authorNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_channel_name")
           if (authorNodes.isNotEmpty()) {
               return authorNodes[0].text?.toString()?.trim()
           }
           // Fallback: Deep breadth-first search for text containing '@'
           return findNodeTextMatchingRegex(rootNode, Regex("^@[a-zA-Z0-9_.-]+$"))
       }
       ```
  3. **Whitelist Evaluation:**
     - If the detected Shorts channel matches an entry in `global_content_whitelists` (e.g. `@3blue1brown`, `@Veritasium`): **Allow playback smoothly**.
     - If the channel is **not whitelisted** OR if the channel handle cannot be determined after 800ms: Immediately launch the **Study Mode Intervention Overlay** (`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`), terminating the Shorts rabbit-hole.

---

### 2.2 EC-2: Hardware Monotonic Clock Anchor (Timezone & DST Proofing)
* **The Problem:** Standard Android timers using `System.currentTimeMillis()` can jump forward or backward when:
  * The user travels across timezone boundaries.
  * Daylight Saving Time (DST) shifts at 02:00 AM.
  * The network updates the system wall clock.
  * A 90-minute session could abruptly finish in 30 minutes or lock the user for 2.5 hours.
* **The Solution:** Timer engine relies strictly on the hardware monotonic clock:
  ```kotlin
  val startElapsedRealtime = SystemClock.elapsedRealtime()
  val targetDurationMs = targetMinutes * 60 * 1000L
  val targetElapsedRealtime = startElapsedRealtime + targetDurationMs

  fun getRemainingTimeMs(): Long {
      val currentElapsed = SystemClock.elapsedRealtime()
      return maxOf(0L, targetElapsedRealtime - currentElapsed)
  }
  ```
  * `SystemClock.elapsedRealtime()` continues ticking during device sleep and is completely immune to wall-clock, timezone, or network time modifications.

---

### 2.3 EC-3: Boot State UsageStats Re-Sync
* **The Problem:** If a device powers off while an app limit is active, in-memory caches are wiped, and cached database records may miss the last 2 minutes of app usage before the shutdown.
* **The Solution:**
  * When `BootCompletedReceiver` runs upon `RECEIVE_BOOT_COMPLETED`:
    1. It queries Android `UsageStatsManager.queryUsageStats()` directly for the range `[Midnight Today, Now]`.
    2. Updates `app_limits` database records with exact empirical operating system usage.
    3. Re-arms intervention overlay monitors with 100% accurate daily totals.

---

### 2.4 EC-4: WorkManager Midnight Boundary Drift Protection
* **The Problem:** Android `WorkManager` periodic tasks have a minimum 15-minute flex execution window. A task scheduled for 00:01 AM may execute at 00:12 AM or 23:55 PM. Evaluating streaks based on the *execution instant* risks misattributing sessions to the wrong calendar day.
* **The Solution:**
  * `DailyStreakWorker` evaluates sessions strictly by their recorded epoch completion timestamp (`session.end_time` / `session.actual_start_time`).
  * The calendar day boundary is computed as:
    $$\text{Target Evaluation Date} = \text{yesterday's date in local system timezone}$$
  * A session completed at 11:58 PM is guaranteed to belong to yesterday, regardless of whether the worker executes at 00:01 AM or 00:20 AM.

---

### 2.5 EC-5: Compose Screen Rotation & ViewModel State Preservation
* **The Problem:** Rotating the phone or unfolding a foldable device destroys the Activity. Re-instantiating the Flip-Clock Composable could cause jarring visual glitches, flickering flip tiles, or dropped animation states.
* **The Solution:**
  * `FlipClockViewModel` holds timer state in an immutable `StateFlow<FlipClockUiState>`.
  * Flip digit transitions use Compose `rememberSaveable` with a custom `Saver` to retain intermediate angular rotations across Activity recreations.

---

## 3. Battery & Doze Mode Optimizations

### 3.1 WakeLock Hygiene
```kotlin
package com.flux.focus.core.optimization

import android.content.Context
import android.os.PowerManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusWakeLockManager @Inject constructor(
    private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquireSessionWakeLock(sessionId: String) {
        if (wakeLock?.isHeld == true) return
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "fluxfoxus:focus_session_$sessionId"
        ).apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L) // 4-hour safety timeout
        }
    }

    fun releaseSessionWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }
}
```
* **Strict Rule:** `releaseSessionWakeLock()` is guaranteed to execute in `FocusTimerService.onDestroy()` and upon any session termination (completed, quit, or expired).

---

## 4. Non-Functional Requirements (NFR) Benchmarks

| Metric | Threshold | Verification Method |
|---|---|---|
| **App Cold Start** | $< 1.5\text{ seconds}$ | Android Vitals / Jetpack Macrobenchmark |
| **Timer Precision** | Drift $< 50\text{ms}$ over 2 hours | Monotonic clock vs Hardware chronometer |
| **Focus Score Computation** | $< 50\text{ms}$ | Microbenchmark on local Room DAO queries |
| **UsageStats Ingestion** | $< 250\text{ms}$ | Aggregated query across installed packages |
| **Accessibility Inspection Latency** | $< 15\text{ms}$ per window event | Zero dropped frames on UI transitions |

---

## 5. Verification & Testing Workflow

### 5.1 Zero-Mock Automated & Manual Test Cases
1. **YouTube Shorts Whitelist Test:**
   * Open YouTube Shorts on a whitelisted channel (e.g. `@3blue1brown`).
   * **Verify:** Video plays smoothly without interruption.
   * Swipe to a non-whitelisted Shorts video.
   * **Verify:** Accessibility engine detects unapproved handle within 800ms and fires intervention overlay.
2. **Timezone Transition Test:**
   * Start 60m session.
   * Change device timezone in OS Settings from UTC+5:30 to UTC-5:00.
   * **Verify:** Remaining countdown time does NOT jump; continues ticking down second-by-second.
3. **Reboot Recovery Test:**
   * Consume 45 minutes of a 60m app limit.
   * Force restart device emulator.
   * **Verify:** Upon boot, `UsageStatsManager` sync restores the exact 45 minutes used; remaining budget is exactly 15 minutes.

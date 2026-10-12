# FluxFoxus (FF) — Phase 1: Mechanical Flip Clock & Active Session Engine

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** Active Focus Session, Split-Flap Animation, Break Window, Friction Modal & Completion  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`)

---

## 1. Screen Architecture & Visual Hierarchy

The Active Focus Session screen is an immersive, distraction-free takeover. System status bars are hidden using Android Immersive Sticky mode, and the screen is kept awake via `FLAG_KEEP_SCREEN_ON`.

```
+-------------------------------------------------------+
|  [📐 Olympiad Math Deep Work]     [04:00 PM - 06:00 PM] |
+-------------------------------------------------------+
|                                                       |
|                  +-----------------+                  |
|                  |       0 1       |  (HOURS)         |
|                  |=================|                  |
|                  |       4 5       |  (MINUTES)       |
|                  +-----------------+                  |
|                                                       |
|  [Break (2/2) (3/4 width)]       [Pause ⏸ (1/4 width)] |
|  [Stop Focusing / Finish Session (Full-width)]        |
+-------------------------------------------------------+
```

### 1.1 Header Row
- **Left — Preset Badge:** Capsule pill in Oxford Slate (`#1B263B`) with 1dp border `#415A77`:
  - Contains Emoji + Preset Name (e.g., `📐 Olympiad Math Deep Work`).
  - Font: `Inter` SemiBold (14sp), Eggshell Cream (`#F4F1DE`).
- **Right — Session Status & Window:**
  - In Focus Mode: Time range (e.g., `"04:00 PM - 06:00 PM"`), color `#8E9CAE`.
  - In Break Mode: Shifts to `"BREAK IN PROGRESS"`, color Muted Sage Green (`#778D7A`).
  - In Paused Mode: Flashes `"PAUSED (04:12 remaining)"`, color Warm Sand (`#D4C4A8`).

---

## 2. The Mechanical Flip Clock (Jetpack Compose Canvas)

The flip clock is the visual anchor of FluxFoxus. It replicates a tactile, split-flap mechanical station clock.

### 2.1 Card Geometry & Tokens
- **Two Split Cards Stacked Vertically:**
  - Top Card: **Hours (`HH`)**
  - Bottom Card: **Minutes (`MM`)**
  - Container Color: Oxford Slate (`#1B263B`)
  - Border: 1dp Muted Steel Blue (`#415A77`)
  - Corner Radius: 16dp
  - Width: ~72% of screen width. Height: ~140dp per card.
- **Split Hairline:** 1.5dp horizontal divider across the exact vertical midpoint colored in Warm Sand Khaki (`#D4C4A8`).
- **Typography:**
  - Font: `JetBrains Mono` or tabular monospace (`FontFeatureSettings("tnum")`).
  - Weight: Bold (700), size: 54sp.
  - Focus Mode Color: Eggshell Cream (`#F4F1DE`).
  - Break Mode Color: Muted Sage Green (`#778D7A`).

### 2.2 Split-Flap 3D Physics & Flip Animation
To ensure the physical flap flip executes properly in Jetpack Compose:
1. **Flap Partitioning:** Each card is composed of 4 layered visual half-surfaces:
   - `UpperStatic`: Shows upcoming top half.
   - `LowerStatic`: Shows current bottom half.
   - `UpperFlap`: Rotates downwards on the X-axis from $0^\circ$ to $90^\circ$.
   - `LowerFlap`: Rotates downwards on the X-axis from $-90^\circ$ to $0^\circ$.
2. **Compose Transformation Parameters:**
   ```kotlin
   Modifier.graphicsLayer {
       rotationX = animatedAngle // 0f to 180f
       cameraDistance = 12f * density // Real perspective depth
       transformOrigin = TransformOrigin(0.5f, 1.0f) // Hinged at the horizontal split line
   }
   ```
3. **Timing & Easing:**
   - Duration: 400ms per digit change.
   - Easing: `FastOutSlowInEasing` with simulated gravity acceleration.
   - Shadow Overlay: A linear black gradient simulates the natural shadow cast as the top flap flips forward.
   - Audio / Haptic: A crisp mechanical click sound effect and subtle haptic vibration trigger at the midpoint flap snap.

---

## 3. Two-Row Bottom Controls Bar

The controls are structured into two intentional rows docked above the bottom safe area:

```
Row 1: [ Take Break (2/2)  (75% Width) ]  [ Pause ⏸ (25% Width) ]
Row 2: [ Stop Focusing / Finish Session   (100% Full Width)     ]
```

### 3.1 Row 1: Break & Pause

#### A. Take Break Button (75% / Three-Quarters Width)
- **Container:** Capsule pill in Oxford Slate (`#1B263B`) with 1dp border `#778D7A`.
- **Text:** `"Take Break (" + breaksRemaining + "/" + totalBreaks + ")"` in Muted Sage Green (`#778D7A`).
- **States:**
  - *Active:* Tappable when `breaksRemaining > 0`. Tapping immediately transitions session into **Break Mode**.
  - *Exhausted:* When breaks hit `0`, button opacity drops to 30%, text reads `"No Breaks Left"`, non-interactive.
  - *No Preset Breaks:* If preset configured `break_count == 0`, button is disabled or hidden.

#### B. Pause Button (25% / One-Quarter Width)
- **Container:** Compact capsule pill in Oxford Slate (`#1B263B`) with 1dp border `#415A77`.
- **Icon / Text:** `⏸` icon in Warm Sand (`#D4C4A8`).
- **Discipline Safety Limit (Option B — Max 5 Minutes):**
  - Pausing is strictly limited to an emergency **5-minute window**.
  - When paused, a countdown badge displays remaining pause seconds: `"⏸ 04:30"`.
  - If the 5-minute pause timer expires without user resuming:
    - Audio alert chime sounds.
    - If user does not resume within 30 seconds, session automatically quits with premature quit penalty.
  - Tapping again (`▶`) immediately resumes normal session countdown.

### 3.2 Row 2: Stop Focusing vs. Finish Session (100% Full Width)
A full-width pill button whose behavior and label adapt dynamically to the timer mode:

#### In Countdown / Timed Window Mode: "Stop Focusing"
- **Label:** `"Stop Focusing"` in `#8E9CAE` text, border 1dp `#415A77`.
- **Behavior:** Stopping before scheduled end time is treated as a **premature surrender**.
- **Action:** Triggers the **Stop-Focusing Friction Modal** (Section 5) with the mandatory 15–25 second penalty wait and streak forfeiture warning.

#### In Stopwatch / Open-Ended Mode: "Finish Session"
- **Label:** `"Finish Session"` in Eggshell Cream (`#F4F1DE`), container in Warm Sand Khaki (`#D4C4A8`) with dark navy text.
- **Behavior:** In Stopwatch mode, user wrap-up is an intentional, successful completion!
- **Discipline Validation:**
  - If elapsed time $\ge$ minimum target (e.g., 20 minutes): Tapping completes session immediately with **zero penalty and streak maintained**.
  - If elapsed time $< 10$ minutes: Warning dialog appears: *"Sessions under 10 minutes do not qualify for streak credit. Finish anyway?"* (Does not reset streak, but doesn't log credit).

---

## 4. Break Window Architecture

When the user taps *"Take Break"*:
1. **Visual Transition:**
   - Screen background remains Abyssal Navy (`#0D1B2A`).
   - Clock digits and card split hairline transition from Warm Sand (`#D4C4A8`) to **Muted Sage Green (`#778D7A`)**.
   - Header displays: `"BREAK (04:45 remaining)"`.
2. **Timer Behavior:**
   - Clock counts down from the preset break duration (e.g., `05:00`) to `00:00`.
   - The main focus timer is paused in background.
3. **Controls During Break:**
   - Row 1 is hidden.
   - Row 2 displays a single full-width button:
     **`"End Break Early"`** (Pill in `#778D7A` with Eggshell text `#F4F1DE`).
   - Tapping ends the break immediately and resumes the focus session.
4. **Break Expiration:**
   - When break timer hits `00:00`:
     - Android heads-up notification and audio chime summon the user back.
     - Decrements `breaksRemaining` by 1.
     - Auto-resumes active focus countdown.

---

## 5. Stop-Focusing Friction Modal

Displayed when *"Stop Focusing"* is pressed during an active countdown:

```
+-------------------------------------------------------+
|                  [ ⚠️ Penalty Alert ]                 |
|                                                       |
|                  Surrender Focus?                     |
|     Stopping now will forfeit your 18-day streak      |
|           and log a premature quit penalty.           |
|                                                       |
|                     (  19s  )                         |
|               Locked Countdown Ring                   |
|                                                       |
|    [ Keep Going ]            [ Quit Session (19s) ]   |
|      (Disabled)                   (Disabled)          |
+-------------------------------------------------------+
```

### 5.1 Modal Properties
- **Container:** Center card in Oxford Slate (`#1B263B`) with 1dp border `#415A77`, corner radius 16dp.
- **Backdrop:** Dimmed scrim (`#0D1B2A` at 85% opacity). Outside tap dismissal is disabled.
- **Delay Formula (15–25 Seconds):**
  $$\text{Wait Seconds} = \text{clamp}\left(15 + \left\lfloor\frac{\text{Today's Distraction Minutes}}{15}\right\rfloor, 15, 25\right)$$
  - Displays circular progress ring in `#D4C4A8` counting down to 0.

### 5.2 Button Locking Rules & Asymmetric Gate
- **`[ Keep Going ]` is ALWAYS FREE & UNLOCKED:**
  - Active, full opacity, styled in Muted Sage Green (`#778D7A`).
  - Immediately clickable at second 1 or second 19 to instantly abandon quitting and return to deep work with zero friction.
- **`[ Quit Session ]` is STRICTLY LOCKED:**
  - Disabled, unclickable, and dimmed (30% alpha) until the countdown delay reaches `0s`.
  - **Once Timer Hits 0:** Becomes active in `#1B263B` with Crimson border (`#DC2626`). Tapping terminates session, resets daily streak to `0` (`💤 0`), and navigates to Home.

---

## 6. Post-Session Completion Experience

When a countdown session hits `00:00` or a stopwatch session is successfully finished:
1. **Audio & Haptic Feedback:** Harmonious chime alert and gentle double haptic vibration.
2. **Completion Summary Overlay (2-Second Celebration Card):**
   - Translucent card over flip clock:
     - Checkmark icon in `#778D7A`.
     - Header: *"Focus Session Complete!"*
     - Time Logged: E.g., *"1h 45m dedicated to Olympiad Math"*.
     - Streak Update: Animated flame counter: *"🔥 18 Days → 19 Days"*.
3. **Smooth Transition:** Fades out after 2.5 seconds (or upon user tap) and smoothly returns to the Home dashboard (`Tab.Home`).

---

## 7. Android System Foreground Service & Persistence

To guarantee uninterrupted execution on Android:
1. **`FocusTimerService` (Foreground Service):**
   - Starts upon session initiation with `Service.startForeground()`.
   - Posts persistent Android Notification with `Ongoing = true`, displaying remaining time (`HH:MM`) and active mode.
   - Holds `PowerManager.PARTIAL_WAKE_LOCK` to ensure Android Doze Mode does not halt timer ticks when screen is turned off.
2. **Screen-On Persistence:**
   - `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)` keeps the flip clock visible on Ari's desk throughout study sessions.

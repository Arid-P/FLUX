# FluxFoxus (FF) — Product Requirements Document (PRD v2.0)

**Version:** 2.0 (Kotlin + Jetpack Compose Architecture)  
**Date:** March 2026 / v2 Overhaul  
**Target Platform:** Native Android (Mobile Enforcer) & Ubuntu Linux (Desktop Daemon)  
**Author:** Ari

---

## 1. Product Overview & Core Philosophy

### 1.1 What is FluxFoxus?
FluxFoxus (FF) is an uncompromising screen discipline and focus enforcement tool designed to replace **Regain** in Ari's academic life. It protects intensive study hours for the Math Olympiad (IOQM/INMO/IMO), JEE Foundation (PW Neev Diamond), and ICSE examinations.

### 1.2 Core Philosophy: Friction Over Willpower
- **Discipline Over Motivation:** Willpower is an unreliable resource during 4-hour problem sets. FF introduces deliberate, escalating friction mechanics to make breaking focus habits mentally expensive.
- **Enforcement, Not Gentle Tracking:** FF does not merely suggest putting down the phone; it intercepts distracting apps, locks screens, and enforces mandatory delay timers.
- **Offline-First:** 100% operational with zero internet connectivity.
- **Cross-Device Parity:** Enforces discipline on Ari's Android smartphone and Ubuntu Linux workstation.

---

## 2. Target User & Behavioral Profile

### Ari's Daily Routine:
- 2–5 hour deep-study blocks requiring uninterrupted focus.
- Distraction vectors: Instagram, YouTube (entertainment channels), mobile games, and desktop web browsing.
- Audio: External study audio via Spotify / YouTube Music (study playlists) / Echo Nightly. FF does **not** include built-in ambient noise or music players.

---

### 3.0 Home Dashboard (`Tab.Home`) & Daily Streak Engine
- **Executive Shortcut Cockpit:** Home acts as the instant launcher mirroring `Tab.Focus`. Tapping *"Start Focusing"* starts the timer immediately using the active preset.
- **Header:** Features `[⚙️ FF] FluxFoxus [🔥 18]` (or dormant `💤 0` when unlit), responsive weekly average pills (`Focused: 2h 53m` & `Weekly: 3h 20m`), and a top-right Settings gear icon (`/settings`).
- **Momentum Card:** Continuous 24-hour smooth Bézier stacked area chart (Productive `#778D7A`, Semi-Productive `#D4C4A8`, Distracting `#E07A5F`, Others `#415A77`) with 6am, 12pm, 6pm, 12am markers and 2×2 legend grid.
- **Session Quick-Launcher:** Displays today's cumulative focus time (e.g. `1hr 43m`), active preset emoji, preset name pill with quick-switcher chevron `▾`, and full session details.
- **Streak Rules:**
  - **Daily Focus Qualifier:** Completing at least 1 focus session (scheduled or standalone) increments the streak (+1).
  - **Weekly Relief Days (Sundays):** Designated rest days act as **Streak Neutral Days**—resting preserves the streak without resetting to zero.
  - Detailed in [`FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md).

### 3.1 Presets & Focus Unified Hub (`Tab.Focus`)
A **Preset** is the central abstraction in FF encapsulating an enforcement strategy. Presets and the focus launcher are unified under the **Focus** tab:
- **Preset Properties:**
  - Name (e.g., *"Olympiad Math Deep Work"*, *"JEE Lecture"*, *"Reading"*)
  - Emoji identifier: Full Android/Gboard Unicode catalog (`androidx.emoji2:emoji2-emojipicker`) with quick academic favorites row (📐, 🔬, 📚, 💻, 🎯, ⚡, 🧠, ⏳, ✍️, ♟️)
  - Break count: `0` to `6` breaks per session (`0` = strict no-break session)
  - Break duration: `1` to `15` minutes per break
  - Per-app blocking rules: Configured via dedicated `/presets/select-apps` screen with one-tap *"Block All Distracting"*
  - Multi-State Content-Filtered Apps: YouTube and Web Browsers (Chrome, Firefox, Brave) support `BLOCK`, `ALLOW`, or `STUDY_MODE` (enforced via native Accessibility Service)
  - Global Academic Whitelists: Shared across all presets for study channels and academic domains, with 100% character accuracy discipline quote typing verification on additions (detailed in [`FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md))
- **Default Selection:** The last-used preset automatically defaults for the next session.
- **Sorting:** Interactive dropdown on the tab (*Recently Used*, *Alphabetical*, *Most Used*).
- **The Floating Island Mode Selector:** A pill container spanning above the CTA button offering 3 focus launch modes:
  1. **Timer (Countdown):** Set minutes (`25m`, `45m`, `60m`, `90m`, `120m`, custom).
  2. **Stopwatch (Count-up):** Counts up indefinitely from `00:00`.
  3. **From–To (Window):** Explicit start and end clock times.
- **60-Hour Inactivity Guard:** If FF has not been used in 60+ hours, an explicit confirmation sheet prompts the user to verify preset settings before starting.

### 3.2 Standalone Planner Scheduling (Phase 1 Core)
FF provides complete standalone scheduling capability in Phase 1:
- The Planner screen features a **`+ Schedule Session`** FAB.
- **Preset-First Selection:** User selects the Preset first.
- **Auto-Fallback Naming:** If no custom session title is provided, the **Preset’s title is automatically assigned**.
- Direct persistence to local Room SQLite with 15-minute heads-up notification scheduled via `AlarmManager`. Zero dependence on FluxDone.

### 3.3 The Mechanical Flip Clock & Focus Timer
- **Visual Design:** Fullscreen immersive mechanical split-flap clock displaying hours and minutes (`HH:MM`). Rendered strictly in **Holst (Холст)** (`#0D1B2A` background, `#1B263B` flap tiles, `#D4C4A8` split hairline, `#F4F1DE` tabular digits).
- **Physical 3D Flip Animation:** 4-layer half-card structure executing a 400ms top-down X-axis rotation with camera distance perspective and mechanical click haptics.
- **Timer Modes:**
  1. **Countdown:** Decrements to `00:00`. Premature stopping triggers the Stop-Focusing Friction Modal.
  2. **Stopwatch:** Increments from `00:00`. Tapping *"Finish Session"* (if $\ge 20$ min) completes successfully with zero penalty and streak credit.
  3. **From–To:** Time-window countdown.
- **Two-Row Controls Layout:**
  - Row 1: `Take Break` (75% / 3/4 width pill in `#778D7A`) + `Pause ⏸` (25% / 1/4 width pill, capped at a **5-minute emergency limit**).
  - Row 2: Full-width button: `"Stop Focusing"` (Countdown mode) or `"Finish Session"` (Stopwatch mode).
- **Post-Session Celebration:** Upon completion, displays a 2.5-second celebratory card showing focus time logged and streak flame increment (`🔥 18 → 19 Days`) before returning to Home.

### 3.4 The Stop-Focusing Friction Modal
To eliminate impulsive session quits:
- When the user taps *"Quit Session"* or presses `Esc`:
  - An emergency friction dialog appears displaying two buttons: *"Keep Going"* and *"Quit Session"*.
  - **Asymmetric Lock Rule:** *"Keep Going"* is **always FREE and unlocked at all times** (instantly clickable to return to study). *"Quit Session"* is **strictly locked and unclickable** for 15 to 25 seconds.
  - A countdown ring displays the remaining lock seconds.
  - Delay calculation: Base 15 seconds + 1 additional second per 15 minutes of distracting app usage recorded today (capped at 25 seconds).
  - Quitting an active session resets the user's daily focus streak to `0` (`💤 0`).

### 3.5 Break Management System
- User taps *"Take Break"* during an active session (if breaks remain).
- The flip clock shifts to the secondary accent color (`#778D7A`) and counts down the designated break minutes.
- When break duration expires, an aggressive heads-up notification and audio chime summon the user back.
- When all preset breaks are spent, the *"Take Break"* button is disabled and greystyled.

### 3.6 App Limits & Daily Budgets
- Daily time budgets configured per distracting app (e.g., 30 mins max on Instagram).
- **Emergency Extra Time:** User can request extra time passes (up to 3 passes of 5/10/15 mins per day, maximum 45 mins total).
- **Hard Turn-Off Protection:** Disabling an active app limit requires surviving a 3-second hold button with an explicit warning showing streak forfeiture.

### 3.7 YouTube Study Mode & Edge Grabber (Mobile & Desktop)
- Restricts YouTube viewing strictly to pre-approved educational channels (PW, Olympiad math channels, 3Blue1Brown).
- **Friction Sentence Engine:** Attempting to watch non-whitelisted videos triggers a full-screen block requiring the user to type verification sentences:
  - First 25 violations: 5 short sentences.
  - 25+ violations: Escalates to 50 complex philosophical and discipline sentences.
- **Dynamic Edge Grabber Pill:** A floating handle on the screen right edge inside YouTube:
  - **Visible when Study Mode is OFF.**
  - **Crucial Enforcement Trigger:** When the user toggles Study Mode OFF via the Edge Grabber, FF **immediately summons the 2×2 Time Selection Grid (5 min | 10 min | 20 min | 40 min)** to prevent open-ended binging.

### 3.8 The Distracted Intervention Window (5 / 10 / 20 / 40 min)
- When a blacklisted distracting app is opened *outside* of a scheduled focus session:
  - FF immediately launches a system overlay window on top of the distracting app.
  - Forces the user to intentionally pick an allowed usage window: **5 min**, **10 min**, **20 min**, or **40 min**.
  - A persistent floating pill displays remaining seconds. When the window hits `0`, the app is minimized and locked for a mandatory cooldown period.
  - Does not reset focus session streaks.

### 3.9 Stats Screen (`Tab.Stats`), App Detail Page & Category Overrides
- **Dual-View Structure:** Features a top segmented pill switcher between **`Daily`** and **`Weekly Trends`**.
- **Focus vs Screen Time Ratio:**
  $$\text{Focus Ratio} = \frac{\text{Focus Time}}{\max(1\text{ min}, \text{Casual Screen Time})}$$
  Evaluates device discipline via tiers: Attention Leak ($<0.5x$), Developing Rhythm ($0.5x–1.0x$), Deep Work ($1.0x–2.0x$), and Monk Mode ($>2.0x$).
- **7-Day Stacked Trends & Week-over-Week Reports:**
  - `Daily`: 7-day stacked bar chart with today highlighted, daily category summary pills, and today's app usage ranking list.
  - `Weekly Trends`: Weekly total and daily averages, week-over-week deltas (Focus Δ, Distraction Δ), weekly stacked usage chart, and weekly app usage rankings.
- **Dedicated App Detail Page (`AppDetailScreen`):**
  - Tapping any app from `Tab.Stats` or `Tab.Limits` opens its dedicated detail page.
  - Features `Daily ▾` and `Weekly ▾` dropdown modes, date navigator, hero usage duration, and 7-day single-app bar chart.
  - **Category Customization:** User can change the app's category (`Productive`, `Semi-Productive`, `Distracting`, `Others`) with instant, zero-latency reactive recalculation across the app via Room DB table `app_category_overrides`.
  - **Manage Blocks Integration:** Direct links to configure daily time budget limits and feature-level blocks (e.g. IG Reels, YouTube Shorts).
  - Detailed in [`FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md).

### 3.10 Home Screen Widget (Jetpack Glance)
- Compact 2×2 and 4×2 Android home screen widgets.
- **Visuals:** 4-segment Donut Ring in Holst colors (`#778D7A` Productive, `#D4C4A8` Semi-Productive, `#E07A5F` Distracting, `#415A77` Others).
- **Center:** Total daily screen time with top Focus Ratio badge.
- **4×2 Quick Launcher:** One-tap button to launch the last-used focus session directly from the home screen.

### 3.11 Permissions Onboarding Wizard & System Health Monitor
- **Single-Page Interactive Checklist (`/onboarding/wizard`):** Guides Ari through activating the 5 core Focus Shields:
  1. `POST_NOTIFICATIONS` (Mandatory runtime alert)
  2. `PACKAGE_USAGE_STATS` (Mandatory usage access)
  3. `SYSTEM_ALERT_WINDOW` (Mandatory overlay permission)
  4. `BIND_ACCESSIBILITY_SERVICE` (Mandatory core interceptor)
  5. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Optional background exception)
- **Lifecycle Auto-Detection:** Using Compose `LifecycleEventEffect(ON_RESUME)`, returning from OS settings automatically checks and marks shields as `[ Granted 🟢 ]` with haptic feedback.
- **OEM Killer Survival:** Manufacturer-aware guides (`Build.MANUFACTURER`) providing targeted tips for Samsung OneUI, Xiaomi MIUI/HyperOS, and OnePlus OxygenOS.
- **Ambient System Health Monitor:** Continuously evaluates permission status on `Tab.Home`. If Android OS revokes an accessibility service after reboot, an ambient alert banner provides 1-tap recovery.
- Detailed in [`FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md).

---

## 4. UI Design System Lock: Holst (Холст)

All screens, widgets, and dialogs are governed by the authoritative **Holst (Холст)** palette sourced from `Theme/Холст.jpeg`:
- **Background:** `#0D1B2A` (Abyssal Navy)
- **Surface Cards:** `#1B263B` (Oxford Slate)
- **Border / Dividers:** `#415A77` (Muted Steel Blue)
- **Accent / Breaks:** `#778D7A` (Muted Sage Green)
- **Primary / Actions:** `#D4C4A8` (Warm Sand Khaki)
- **High Contrast Text:** `#F4F1DE` (Eggshell Cream)
- **Muted Text:** `#8E9CAE` (Slate Blue-Grey)

*Zero unapproved purples or oversaturated neon cyans are permitted.*

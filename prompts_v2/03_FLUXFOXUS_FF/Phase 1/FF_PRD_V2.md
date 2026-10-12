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

## 3. Core Feature Specifications

### 3.1 Presets & Focus Unified Hub (`Tab.Focus`)
A **Preset** is the central abstraction in FF encapsulating an enforcement strategy. Presets and the focus launcher are unified under the **Focus** tab:
- **Preset Properties:**
  - Name (e.g., *"Olympiad Math Deep Work"*, *"JEE Lecture"*, *"Reading"*)
  - Emoji identifier (e.g., 📐, 🔬, 📖)
  - Break count: `0` to `6` breaks per session (`0` = strict no-break session)
  - Break duration: `1` to `15` minutes per break
  - Per-app blocking rules: Whitelist or Blacklist configuration
  - YouTube Study Mode state: `BLOCK`, `ALLOW`, or `STRICT_WHITELIST`
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
  - **Both buttons are completely locked and unclickable for 15 to 25 seconds.**
  - A countdown ring displays the remaining lock seconds.
  - Delay calculation: Base 15 seconds + 1 additional second per 15 minutes of distracting app usage recorded today (capped at 25 seconds).
  - Quitting an active session resets the user's daily focus streak to `0`.

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

### 3.9 Screen Time Categorization & Usage Stats
Uses Android `UsageStatsManager` to aggregate daily screen time across 4 categories:
1. **Productive:** Academic tools, Notewise, FD, educational readers.
2. **Semi-Productive:** Utility, system settings, communication.
3. **Distracting:** Social media, video entertainment, games.
4. **Others:** Unclassified apps.

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

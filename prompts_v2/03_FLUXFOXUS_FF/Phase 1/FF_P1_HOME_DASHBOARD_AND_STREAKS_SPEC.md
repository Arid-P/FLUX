# FluxFoxus (FF) — Phase 1: Home Dashboard & Daily Streak Engine Specification

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** Home Dashboard (`Tab.Home`) Executive Shortcut Architecture, Responsive Header, `💤 0` Dormant Streak Mechanics, 24h Smooth Bézier Momentum Chart, Session Quick-Launcher Card, and Complete Daily Streak Engine  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`, `#E07A5F`)

---

## 1. Architectural Overview & Philosophy

`Tab.Home` is the **executive shortcut and command center** of FluxFoxus. It is not an administrative configuration surface—it is a fast, high-momentum cockpit designed to get Ari into deep work in under 2 seconds.

### Key Pillars:
1. **Executive Shortcut Principle:** `Tab.Home` mirrors the state of `Tab.Focus`. Tapping `"Start Focusing"` on Home immediately triggers the active preset without requiring tab switching.
2. **Ambient Weekly Context:** Dynamic header pills present **weekly averages** rather than raw isolated daily numbers, anchoring Ari's sense of weekly rhythm.
3. **The 24-Hour Continuous Momentum Curve:** A smooth Bézier stacked area chart visualizing digital consumption across the 4 Holst categories over the past 24 hours.
4. **Binary & Micro Streak Enforcement:**
   - **Global Focus Streak (Macro):** Consecutive days with at least 1 completed focus session ($\ge 1$). Active flame `{🔥} 18` or dormant `{💤} 0`.
   - **App Limit Streaks (Micro):** Consecutive days staying under daily app budgets.
   - **Weekly Relief Days:** Sunday (or designated rest day) acts as a **Streak Neutral Day**—resting freezes the streak safely without resetting it to zero.
5. **Psychological Friction Gate:** In the Stop-Focusing modal, **`[ Keep Going ]` is always FREE and unlocked**, making persistence frictionless while penalizing quitting behind a locked 15–25s countdown.

---

## 2. Screen Architecture: Home Dashboard (`Tab.Home`)

Accessible via the 1st bottom navigation tab.

```
+-------------------------------------------------------+
|  [⚙️ FF] FluxFoxus [🔥 18]     +-----------------+[⚙️] |
|                                | Focused: 2h 53m |    |
|                                | Weekly:  3h 20m |    |
|                                +-----------------+    |
+-------------------------------------------------------+
|  MOMENTUM                                   Last 24h  |
|  +-------------------------------------------------+  |
|  |             .~~-.__                             |  |
|  |           .~  🔴 Coral  ~.                      |  |
|  |         .~    🟡 Sand     ~.                    |  |
|  |       .~      🟢 Sage       ~.                  |  |
|  |     .~                          ~.              |  |
|  |  +-----------------------------+                |  |
|  |  6am       12pm       6pm     12am              |  |
|  |-------------------------------------------------|  |
|  |  🟢 Productive: 2h 15m  |  🟡 Semi-Prod: 45m    |  |
|  |  🔴 Distracting: 52m    |  ⚪ Others: 10m       |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
|  [ ⚡ Olympiad Math starts in 12 min (if scheduled) ] |
+-------------------------------------------------------+
|  SESSION QUICK LAUNCHER                               |
|  +-------------------------------------------------+  |
|  |  1hr 43m       [ 📐 ] [ Olympiad Math ]   [ ▾ ] |  |
|  |  Today Focus   Emoji    Preset Name      Switch |  |
|  |-------------------------------------------------|  |
|  |  Duration:          04:00 PM — 06:00 PM (or ∞)  |  |
|  |  Breaks:            2 Sessions                  |  |
|  |  Break Time (each): 5m                          |  |
|  |  Description:       "Problem set 4 proofs"      |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
|  [            START FOCUSING (Warm Sand)           ]  |
+-------------------------------------------------------+
```

---

## 3. Detailed Component Specifications

### 3.1 Responsive Top App Bar
The header balances branding, streak motivation, high-level metrics, and settings access:

```
+-------------------------------------------------------+
|  [⚙️ FF] FluxFoxus [🔥 18]     +-----------------+[⚙️] |
|                                | Focused: 2h 53m |    |
|                                | Weekly:  3h 20m |    |
|                                +-----------------+    |
+-------------------------------------------------------+
```

1. **Brand & Title (Left):**
   - FF Aperture Emblem: 24×24dp in Warm Sand Khaki (`#D4C4A8`).
   - Title text: *"FluxFoxus"* in Eggshell Cream (`#F4F1DE`, 20sp bold).
2. **Compact Streak Pill (Directly Adjacent to Title):**
   - **Active Streak ($>0$):** Pill in Surface `#1B263B` with border `#415A77`. Displays **`🔥 18`** (Flame emoji + streak count) in Warm Sand Khaki (`#D4C4A8`, weight 700, 13sp).
   - **Dormant Streak ($=0$):** Displays **`💤 0`** (Sleeping emoji + zero) in Slate Blue-Grey (`#8E9CAE`), motivating Ari to ignite the fire today.
3. **Dynamic Weekly Average Pills (Center / Right):**
   - Renders inside a responsive Compose layout:
     - **Compact Screens:** Stacked vertically inside a clean rounded surface capsule.
     - **Wide / Landscape Screens:** Flow side-by-side horizontally.
   - **Pill 1 — Focused:** Average daily focus time this week up to today (e.g. *"Focused: 2h 53m"*). Accent: Sage Green (`#778D7A`).
   - **Pill 2 — Weekly:** Average daily total screen time this week (e.g. *"Weekly: 3h 20m"*). Accent: Warm Sand (`#D4C4A8`).
4. **Settings Action Button (Far Right):**
   - Gear icon button (`Icons.Default.Settings`) in `#F4F1DE`.
   - Tapping opens the **App & Account Settings** navigation route (`/settings`), providing access to OS permissions, relief day settings, and profile info.

---

### 3.2 The Momentum Card (24-Hour Continuous Area Chart)

Provides an immediate, ambient overview of digital consumption over the last 24 hours without opening deep analytical tabs:
- **Card Container:** Background `#1B263B`, border 1dp `#415A77`, corner radius 12dp, padding 16dp.
- **Card Header Row:**
  - Left: *"Momentum"* in Eggshell Cream (`#F4F1DE`, 16sp bold).
  - Right: *"Last 24h"* in Slate Blue-Grey (`#8E9CAE`, 12sp).
- **Smooth Bézier Stacked Area Chart:**
  - Height: 150dp.
  - Drawn via Compose `Canvas` using cubic Bézier curves (`cubicTo`) sampled from hourly `UsageStatsManager` buckets.
  - **Layer Stacking Order (Bottom to Top):**
    1. Base: **Productive** — Muted Sage Green (`#778D7A`)
    2. Middle: **Semi-Productive** — Warm Sand Khaki (`#D4C4A8`)
    3. Upper: **Distracting** — Terracotta Coral (`#E07A5F`)
    4. Peak: **Others** — Muted Steel Blue (`#415A77`)
  - **X-Axis Time Markers:** `6am`, `12pm`, `6pm`, `12am` in `#8E9CAE` (10sp).
- **2×2 Legend Grid (Directly Beneath Chart):**
  - Divided by 1dp `#415A77` hairline dividers:
    - Row 1: `🟢 Productive: [Xh Xm]` (Sage) | `🟡 Semi-Prod: [Xh Xm]` (Sand)
    - Row 2: `🔴 Distracting: [Xh Xm]` (Coral) | `⚪ Others: [Xh Xm]` (Steel Blue)
  - Displays actual cumulative time spent in each category over the last 24 hours.

---

### 3.3 Upcoming Scheduled Session Alert Banner (Dynamic)

- **Condition:** Displayed only when a session is scheduled in the Planner within the next 15 minutes.
- **Visuals:** Surface container `#1B263B` with 2dp left indicator stripe in Warm Sand `#D4C4A8`.
- **Text:** *"⚡ [Preset Name] starts in [X] min"* in `#F4F1DE` (14sp).
- **Interaction:** Tapping jumps directly to the session in the Planner timeline.

---

### 3.4 The Session Quick Launcher Card

Acts as the instant trigger for Ari's active focus routine:
- **Top Header Row:**
  - **Today's Cumulative Focus Duration:** e.g. `"1hr 43m"` in bold 22sp Eggshell Cream `#F4F1DE` (prominent real-time daily focus counter).
  - **Active Preset Emoji:** 28×28dp icon (e.g. `📐`).
  - **Active Preset Name:** Pill container `#1B263B` with text e.g. `[ Olympiad Math ]`.
  - **Quick Switcher Chevron `▾`:** Tapping pops a compact bottom sheet listing all saved presets to switch the active preset immediately without leaving Home.
- **Detail Sub-Rows (Divided by subtle hairlines):**
  - **Duration:** Planned focus window (e.g. `"04:00 PM — 06:00 PM"`) or `"∞ (Stopwatch)"`.
  - **Breaks:** Configured breaks (e.g. `"2 Breaks"`).
  - **Break Time (each):** Duration per break (e.g. `"5m"`).
  - **Description:** Syllabus notes (e.g. *"Problem set 4 proofs"*).
- **Empty State:** If no preset exists: displays *"No Preset Selected"* with a `+ Create Preset` CTA.

---

### 3.5 Primary CTA: "Start Focusing" / "Resume Session"

- **Sticky Full-Width Pill Button:** Pinned at bottom of screen.
- **Idle State:**
  - Text: **`"Start Focusing"`** in Dark Abyssal Navy `#0D1B2A` on Warm Sand Khaki `#D4C4A8` background (16sp bold).
  - Tap Behavior: **Invokes the exact same launcher as `Tab.Focus`**, launching the active preset into the full-screen mechanical flip clock immediately.
- **Active Running State (Session Running in Background):**
  - Text: **`"Resume Session (42:15 remaining)"`** in Eggshell Cream `#F4F1DE` on Muted Sage Green `#778D7A` background.
  - Tap Behavior: Restores the full-screen 3D Flip Clock with zero latency.

---

## 4. The Daily Streak Engine

### 4.1 Streak Architecture & Two-Tier Rules
FluxFoxus operates two separate discipline streak trackers:

#### 1. Macro: Global Focus Streak
- **The Core Qualifier:**
  $$\text{Daily Focus Goal Met} \iff \text{Completed at least 1 focus session (scheduled or standalone) today}$$
- Any completed session (whether started from Home, Presets, or synced from FluxDone) satisfies the day.
- **Daily Increment:** At midnight, if daily focus goal was met:
  $$\text{current\_streak} \leftarrow \text{current\_streak} + 1$$
- **Streak Break:** If zero focus sessions were completed on a standard active day:
  $$\text{current\_streak} \leftarrow 0 \quad (\text{resets to dormant } 💤 0)$$

#### 2. Micro: Per-App Limit Streaks
- Tracks consecutive days Ari stayed within his daily budget for individual blacklisted apps (e.g. *14 days staying under 45m Instagram limit*).
- Displayed on the App Limit Card in `Tab.Limits` and `AppDetailScreen`.
- Disabling the app limit or exceeding daily passes breaks that app's streak.

---

### 4.2 Weekly Relief Days (Sundays / Off Days)

For academic balance, Ari cannot be penalized for scheduled rest days:
- **Default Relief Day:** **Sunday** (user-configurable in Settings).
- **Relief Day Rules:**
  - **Resting:** If Ari completes **0 sessions** on a Relief Day, **the streak is protected and frozen**. It does NOT break, and does not reset to zero.
  - **Working:** If Ari completes $\ge 1$ session on a Relief Day, the streak **increments (+1)** as an earned bonus day!
- **Database Representation:**
  ```sql
  -- relief_days bitmask or JSON array in app settings: default ['SUNDAY']
  ```

---

### 4.3 Stop-Focusing Friction Modal: The Asymmetric Lock Gate

To protect the streak against momentary impulses to quit during deep work:

```
+-------------------------------------------------------+
|  ⚠️ Premature Quit Warning                            |
+-------------------------------------------------------+
|  Quitting will forfeit your 18-day focus streak!      |
|  Current Streak: 🔥 18 Days  ➔  💤 0 Days             |
|                                                       |
|                  [ ⏳ 19s Delay ]                     |
|                                                       |
|  +-------------------------------------------------+  |
|  |  [ Keep Going (Always FREE & Unlocked) 🟢 ]     |  |
|  +-------------------------------------------------+  |
|  |  [ Confirm Quit (Locked until 0s) 🔒 ]         |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

1. **`[ Keep Going ]` is FREE at all times:**
   - Unlocked, active, and highlighted in Muted Sage Green `#778D7A`.
   - Ari can tap it instantly at second 1 or second 19 to dismiss the modal and immediately resume focus without penalty.
2. **`[ Confirm Quit ]` is STRICTLY LOCKED:**
   - Disabled, greyed out, and unclickable until the 15–25s countdown hits `0`.
   - Countdown formula:
     $$\text{delay} = \text{clamp}(15 + \min(10, \text{current\_streak} / 2), 15, 25) \text{ seconds}$$
   - Only when countdown reaches `0` does *"Confirm Quit"* become active in Crimson `#DC2626`.
   - Confirming quit forfeits the session and immediately resets the streak:
     $$\text{current\_streak} \leftarrow 0 \quad (💤 0)$$

---

### 4.4 Midnight Streak Evaluation Pipeline (WorkManager)

```kotlin
class StreakEvaluationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val streakRepository: StreakRepository,
    private val sessionRepository: FocusSessionRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val todayCalendar = Calendar.getInstance()
        val yesterday = todayCalendar.apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday.time)
        val dayOfWeek = yesterday.get(Calendar.DAY_OF_WEEK) // Sunday = 1

        val reliefDays = settingsRepository.getReliefDays() // e.g., setOf(Calendar.SUNDAY)
        val sessionsYesterday = sessionRepository.getCompletedSessionsCountOnDate(yesterdayDateStr)

        val currentStreak = streakRepository.getCurrentStreak()

        if (sessionsYesterday >= 1) {
            // Focus achieved: advance streak
            streakRepository.incrementStreak(yesterdayDateStr)
        } else if (dayOfWeek in reliefDays) {
            // Relief day: streak is neutral, preserve without reset
            streakRepository.preserveStreakForReliefDay(yesterdayDateStr)
        } else {
            // Target missed on active day: streak breaks
            streakRepository.resetStreak()
        }

        return Result.success()
    }
}
```

---

## 5. Room Database Schema Update

```sql
-- Table: streaks (Single-row singleton)
CREATE TABLE streaks (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_completed_date TEXT NOT NULL,         -- 'YYYY-MM-DD'
    relief_days_json TEXT NOT NULL DEFAULT '["SUNDAY"]', -- Relief days of week
    updated_at INTEGER NOT NULL
);

-- Table: app_limit_streaks (Per-app discipline streaks)
CREATE TABLE app_limit_streaks (
    package_name TEXT PRIMARY KEY NOT NULL,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_successful_date TEXT NOT NULL,        -- 'YYYY-MM-DD'
    updated_at INTEGER NOT NULL
);
```

---

## 6. Strict Holst UI Color Token Lock

| UI Element | Token Name | Hex Value |
| :--- | :--- | :--- |
| **Screen Background** | `color_background` | `#0D1B2A` |
| **Card Surfaces** | `color_surface` | `#1B263B` |
| **Borders & Dividers** | `color_border` | `#415A77` |
| **Primary Action CTA / Active Streak Flame** | `color_primary` (Warm Sand) | `#D4C4A8` |
| **Dormant Streak ($=0$) / Inactive** | `color_text_muted` (Slate Grey) | `#8E9CAE` |
| **Productive Layer / Keep Going Button** | `color_accent` (Muted Sage) | `#778D7A` |
| **Distracting Layer / Premature Quit Alert** | `color_warning` (Terracotta Coral) | `#E07A5F` |
| **High-Contrast Text / Titles** | `color_text_primary` | `#F4F1DE` |

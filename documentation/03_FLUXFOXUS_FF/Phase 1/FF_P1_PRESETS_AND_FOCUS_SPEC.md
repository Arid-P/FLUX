# FluxFoxus (FF) — Phase 1: Presets, Focus & Standalone Scheduling Specification

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** Presets Management, Focus Launch Island, and Standalone Planner Scheduling  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`)

---

## 1. Architectural Overview & Rationale

In the legacy architecture, FluxFoxus suffered from two critical design flaws:
1. **The Timer/Preset Disconnect:** Presets and Focus were split across disjointed screens, requiring unnecessary navigation jumps.
2. **The Planner Scheduling Trap:** Standalone session scheduling was missing in Phase 1, forcing FF to rely on FluxDone IPC just to display scheduled blocks on the calendar.

This specification permanently resolves both flaws in Phase 1:
- Unifies Presets and Focus initiation into a streamlined **"Focus" (`Tab.Focus`)** screen featuring an interactive Presets list and a **Floating Island Mode Selector**.
- Establishes **Standalone Session Scheduling (`ssession`)** in the Planner with **Preset-First selection** and automatic fallback naming.

---

## 2. Navigation Tab Structure (The 5 Bottom Tabs)

| Tab ID | Nav Label | Icon | Core Role |
| :--- | :--- | :--- | :--- |
| `Tab.Home` | **Home** | `Icons.Default.Home` | Daily summary, streak flame counter, active session jump card. |
| `Tab.Focus` | **Focus** | `Icons.Default.Timer` | Presets list + Floating Island Mode Selector + "Start Focusing" CTA. |
| `Tab.Planner`| **Planner** | `Icons.Default.CalendarToday` | Horizontal day strip + scheduled sessions + `+ Schedule Session` FAB. |
| `Tab.Limits` | **Limits** | `Icons.Default.Block` | Daily app time budgets, extra time tokens, and friction controls. |
| `Tab.Stats` | **Stats** | `Icons.Default.BarChart` | 4-category screen time metrics, charts, and weekly discipline report. |

*(On the bottom navigation bar, single-word label "Focus" prevents text truncation, while the internal page header displays "Focus & Presets").*

---

## 3. The "Focus & Presets" Screen (`Tab.Focus`)

The unified Focus tab serves as the central command center for starting immediate sessions with any preset.

```
+-------------------------------------------------------+
|  FOCUS & PRESETS                      [Sort ▼]  [+ New] |
+-------------------------------------------------------+
|  PRESETS LIST (Scrollable)                            |
|                                                       |
|  +-------------------------------------------------+  |
|  | (•) 📐 Olympiad Math Deep Work                  |  |
|  |     2 breaks · 5m each | YouTube Whitelist      |  |
|  |     [Edit ✏️]                                    |  |
|  +-------------------------------------------------+  |
|                                                       |
|  +-------------------------------------------------+  |
|  | ( ) 🔬 JEE Physics Problems                     |  |
|  |     1 break · 10m each | All Media Blocked      |  |
|  |     [Edit ✏️]                                    |  |
|  +-------------------------------------------------+  |
|                                                       |
|  +-------------------------------------------------+  |
|  | ( ) 📖 Deep Reading & Theory                    |  |
|  |     No breaks · 0m | YouTube Blocked            |  |
|  |     [Edit ✏️]                                    |  |
|  +-------------------------------------------------+  |
|                                                       |
|  [ FLOATING ISLAND:  (Timer) | (Stopwatch) | (From-To) ]  |
|  [ BUTTON:           START FOCUSING (45m)            ]  |
+-------------------------------------------------------+
```

### 3.1 Header & Sorting
- **Title:** "Focus & Presets" (Display typography, Eggshell Cream `#F4F1DE`).
- **Sorting Dropdown (Top Right):**
  - *"Recently Used"* (Default)
  - *"Alphabetical (A–Z)"*
  - *"Most Used"*
- **Action Button:** `+ New` button styled in `#415A77` border pill to launch the Preset Creator.

### 3.2 Presets Selection List
- Scrollable list of user presets rendered in Oxford Slate cards (`#1B263B` with 1dp `#415A77` border).
- **Default Selection:** The **last-used preset is selected by default** upon opening.
- **Card Selection State:**
  - Selected Card: Border transitions to Warm Sand (`#D4C4A8`) with 2dp width and subtle inner highlight. Radio dot on the left is filled with `#D4C4A8`.
  - Unselected Card: 1dp border in `#415A77`.
  - Tapping anywhere on the card selects it as the active preset for the session.
- **Card Badges & Information:**
  - Emoji + Preset Name.
  - Break Badge: E.g., *"2 breaks · 5m each"* in Muted Sage Green (`#778D7A`).
  - YouTube Badge: E.g., *"Study Mode Whitelist (12 channels)"*.
  - App Rules Summary: E.g., *"8 apps blocked"*.
  - Right Action: Edit pencil icon (`Icons.Default.Edit`) navigating to the Preset Editor.

### 3.3 The Floating Island (Mode Selector Pill)
Positioned directly above the primary action button, spanning the exact width of the CTA:
- **Surface:** Floating capsule container in `#1B263B` with 1dp border `#415A77` and subtle elevation.
- **3-Segment Mode Selector:**
  1. **Timer (Countdown):** Standard countdown duration. Displays duration chips (`25m`, `45m`, `60m`, `90m`, `120m`, or custom picker).
  2. **Stopwatch (Count-up):** Starts at `00:00` and increments upwards indefinitely.
  3. **From–To (Time Window):** Allows picking explicit start and end times (e.g., `4:00 PM – 5:30 PM`).
- The selected segment is highlighted with a capsule background in `#778D7A` (Muted Sage Green) with `#F4F1DE` text.

### 3.4 Primary Action Button: "Start Focusing"
- Wide pill button docked at the bottom of the screen above the navigation bar.
- Container color: Warm Sand Khaki (`#D4C4A8`).
- Text: Bold Abyssal Navy (`#0D1B2A`):
  - In Timer Mode: `"Start Focusing (45m)"`
  - In Stopwatch Mode: `"Start Stopwatch Session"`
  - In From-To Mode: `"Start Scheduled Window (90m)"`
- **Execution:** Instantly launches the **Full-Screen Mechanical Flip Clock** in the selected mode, attaches the selected preset, and starts the persistent Android `ForegroundService`.

---

## 4. Standalone Planner Scheduling Flow (Phase 1 Native)

The Planner is fully functional as a standalone scheduling calendar in Phase 1 without requiring FluxDone.

```
+-------------------------------------------------------+
|  PLANNER                          [Today]    [March]  |
+-------------------------------------------------------+
|  [Mon 12] [Tue 13] [(Wed 14)] [Thu 15] [Fri 16] [Sat]  |
+-------------------------------------------------------+
|  STATS: Focus: 3h 15m  |  Usage: 1h 45m               |
+-------------------------------------------------------+
|  SCHEDULED SESSIONS (Scrollable)                      |
|                                                       |
|  +-------------------------------------------------+  |
|  | 04:00 PM - 06:00 PM (120m)               [FF]   |  |
|  | 📐 Olympiad Math Deep Work                       |  |
|  | Preset: Olympiad Math | 2 breaks | Heads-up ON  |  |
|  +-------------------------------------------------+  |
|                                                       |
|  +-------------------------------------------------+  |
|  | 07:30 PM - 09:00 PM (90m)                [FD]   |  |
|  | 🔬 Neev Diamond Chemistry (From FD)              |  |
|  +-------------------------------------------------+  |
|                                                       |
|                          [ FAB: + Schedule Session ]  |
+-------------------------------------------------------+
```

### 4.1 The "+ Schedule Session" Bottom Sheet
When the user taps the **`+ Schedule Session`** FAB on the Planner screen:
1. **Step 1: Select Preset (FIRST STEP):**
   - The user selects the desired preset from a horizontal chip carousel or dropdown (e.g., *"📐 Olympiad Math Deep Work"*).
   - Defaults to the last-used preset.
2. **Step 2: Session Name (AUTOMATIC FALLBACK):**
   - Text field displays placeholder or pre-filled value matching the selected Preset’s name.
   - If the user leaves the text field untouched, **the Preset’s name is automatically used as the session title**.
   - If the user types a custom title (e.g., *"INMO Mock Test 2"*), the custom title overrides the default.
3. **Step 3: Date, Start Time & End Time:**
   - Date defaults to currently selected day on the Planner calendar strip.
   - Start time & End time pickers with automatic duration calculation (e.g., `4:00 PM – 6:00 PM → 120 mins`).
4. **Step 4: Recurrence Rule:**
   - Dropdown: `None` | `Daily` | `Mon–Fri` | `Custom Weekly`.
5. **Step 5: Heads-up Reminder:**
   - Toggle: `15-minute heads-up notification` (Enabled by default).
6. **Confirmation & Persistence:**
   - Tapping *"Schedule Session"* writes a new record to Room SQLite `focus_sessions` table with status `'SCHEDULED'`.
   - Schedules a precise Android `AlarmManager` reminder for 15 minutes prior to session start.
   - Zero IPC or network calls required.

---

## 5. Preset Creation & Edit Flow (`/presets/create` & `/presets/edit/{id}`)

Accessed via the `+ New` button or the edit pencil icon on any preset card:
- **Full-Screen Push Screen:** Clean, distraction-free form layout.
- **Header:** Title ("New Preset" or "Edit Preset"), Close button (×).
- **Form Sections:**
  1. **Preset Identity:** Text field for name (max 64 chars) + Emoji button opening an 8-column bottom sheet.
  2. **Break Configuration:** 
     - Stepper 1: Number of breaks (`0` to `6`, where `0` disables breaks).
     - Stepper 2: Duration per break (`1` to `15` minutes).
  3. **App Restrictions:**
     - 4 collapsible category containers: *Distracting* (default toggled ON/blocked), *Productive*, *Semi-Productive*, *Others*.
     - Search bar to find specific installed applications.
     - Toggle switches for per-app blocking.
  4. **YouTube Study Mode:**
     - 3-position segmented selector: `Block completely` | `Allow completely` | `Study Mode`.
     - When `Study Mode` is active, displays button linking to `/presets/youtube_whitelist`.
  5. **Description / Focus Intentions:** Multi-line notes field.
- **Sticky Footer:** Full-width pill button in `#D4C4A8`: *"Save Preset"*.

---

## 6. Room Database Schema Specifications

```sql
-- Presets Table
CREATE TABLE presets (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL DEFAULT '⏳',
    break_count INTEGER NOT NULL DEFAULT 2,          -- 0 to 6
    break_duration_minutes INTEGER NOT NULL DEFAULT 5, -- 1 to 15
    youtube_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
    browser_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
    description TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Per-Preset App Rules Table
CREATE TABLE preset_app_rules (
    preset_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    rule_mode TEXT NOT NULL DEFAULT 'BLOCKED',       -- 'BLOCKED', 'ALLOWED', 'STUDY_MODE'
    PRIMARY KEY(preset_id, package_name),
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE CASCADE
);

-- Scheduled & Active Focus Sessions (Both Native FF & FD Synced)
CREATE TABLE focus_sessions (
    id TEXT PRIMARY KEY NOT NULL,
    session_origin TEXT NOT NULL DEFAULT 'MANUAL',    -- 'MANUAL' (impulse), 'SCHEDULED' (calendar), or 'TASK_SYNC' (FD synced)
    preset_id TEXT NOT NULL,                          -- FK to presets(id)
    session_title TEXT NOT NULL,                      -- Custom name or fallback to preset name
    linked_task_id TEXT,                              -- Nullable UUID from FluxDone (if synced)
    timer_mode TEXT NOT NULL,                         -- 'COUNTDOWN', 'STOPWATCH', 'FROM_TO'
    start_time INTEGER NOT NULL,                      -- UTC epoch ms
    end_time INTEGER,                                 -- Nullable UTC epoch ms (computed or actual)
    target_duration_minutes INTEGER NOT NULL,
    actual_duration_seconds INTEGER NOT NULL DEFAULT 0,
    breaks_taken INTEGER NOT NULL DEFAULT 0,
    breaks_total_seconds INTEGER NOT NULL DEFAULT 0,
    recurrence_rule TEXT,                             -- Nullable RRULE JSON
    status TEXT NOT NULL,                             -- 'PLANNED', 'ACTIVE', 'PAUSED_BREAK', 'COMPLETED', 'QUIT_PREMATURELY', 'SKIPPED', 'EXPIRED_ON_BOOT'
    penalty_delay_applied_seconds INTEGER NOT NULL DEFAULT 0,
    headsup_notification_armed INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE CASCADE
);
```

---

## 7. Verification & Negative Constraints for AI Agents

1. **Preset Selection Must Precede Naming:** When building the scheduling UI, the Preset selector must appear at the top of the form. The session title field must auto-populate with the selected Preset's name if left empty by the user.
2. **Planner is Never Dependent on FD:** The Planner must render all sessions where `status == 'SCHEDULED'` regardless of whether FluxDone is installed or has sent broadcasts.
3. **No Disjointed Tabs:** Do not create separate "Presets" and "Focus" bottom tabs. They are unified under `Tab.Focus` with the Presets list and the Floating Island mode selector.
4. **Strict Color Fidelity:** All interactive highlights must use **Holst Warm Sand (`#D4C4A8`)** and **Oxford Slate (`#1B263B`)**. Zero purples or blues.

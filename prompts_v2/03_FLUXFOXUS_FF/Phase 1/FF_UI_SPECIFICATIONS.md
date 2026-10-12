# FluxFoxus (FF) — Master UI Specifications (v2.0)

**Version:** 2.0 (Jetpack Compose Screen & Overlay Specifications)  
**Target:** Native Android & Compose Desktop  
**Theme:** Strictly Dark Mode — **Holst (Холст)** Palette (`FLUX_DESIGN_TOKENS.md`)

---

## 1. Design System Rules (Holst / Холст)

> [!IMPORTANT]
> **Strict Color Lock (Holst):** Sourced directly from `Theme/Холст.jpeg`.
> - Background: `#0D1B2A` (Abyssal Navy)
> - Surface Cards: `#1B263B` (Oxford Slate)
> - Border / Dividers: `#415A77` (Muted Steel Blue)
> - Accent / Break State: `#778D7A` (Muted Sage Green)
> - Primary / Action: `#D4C4A8` (Warm Sand Khaki)
> - Text Primary: `#F4F1DE` (Eggshell Cream)
> - Text Muted: `#8E9CAE` (Slate Blue-Grey)

---

## 2. Navigation Architecture (5 Bottom Tabs)

The mobile interface is structured around 5 bottom navigation items:
1. **Home (`Tab.Home`):** Dashboard overview, current streak, quick summary, jump to active session.
2. **Focus (`Tab.Focus`):** Presets selection list + Floating Island Mode Selector + "Start Focusing" CTA (displays active flip-clock timer when session runs).
3. **Planner (`Tab.Planner`):** Horizontal day strip, daily session timeline, and `+ Schedule Session` FAB (preset-first scheduling).
4. **Limits (`Tab.Limits`):** Per-app daily time budgets, extra time tokens, and friction controls.
5. **Stats (`Tab.Stats`):** Usage statistics, 4-category breakdown, and streak history.

---

## 3. Screen Specifications

### 3.1 Home Screen (`Tab.Home`)
Executive shortcut cockpit for immediate momentum and deep work launch:
- **Responsive Header:**
  - Left: Aperture emblem (24dp) + *"FluxFoxus"* title (`#F4F1DE`, 20sp) + compact Streak Pill: **`🔥 18`** when active or **`💤 0`** when dormant.
  - Center/Right: Responsive weekly average pills (side-by-side or stacked):
    - `Focused: 2h 53m` (daily focus avg this week in Sage `#778D7A`).
    - `Weekly: 3h 20m` (daily total screen time avg this week in Sand `#D4C4A8`).
  - Far Right: Settings gear icon button (`Icons.Default.Settings`) navigating to App & Account settings.
- **Momentum Card (Last 24h):**
  - Surface `#1B263B` with border `#415A77` and title *"Momentum"* (subtitle *"Last 24h"*).
  - Smooth Bézier stacked area chart (~150dp height) with 6am, 12pm, 6pm, 12am X-axis labels.
  - Stacking layers: Productive (`#778D7A`), Semi-Productive (`#D4C4A8`), Distracting (`#E07A5F`), Others (`#415A77`).
  - 2×2 Legend Grid beneath chart with hairlines displaying exact 24h category minutes.
- **Upcoming Session Alert Banner (Dynamic):**
  - Slides in when a session starts in $\le 15$ mins: *"⚡ [Preset Name] starts in [X] min"*.
- **Session Quick-Launcher Card:**
  - Displays today's cumulative focus duration in bold (e.g. `"1hr 43m"` in `#F4F1DE`).
  - Active preset emoji (28dp), Preset name pill (`[ Olympiad Math ]`), and switcher chevron `▾` (pops quick preset picker bottom sheet).
  - Detail rows: Duration window (or $\infty$), break count, break duration, and syllabus notes.
- **Primary Action CTA:**
  - Full-width sticky pill: **`"Start Focusing"`** in Warm Sand `#D4C4A8` (idle) or **`"Resume Session"`** in Sage `#778D7A` (running).
  - Tapping invokes the exact same launch action as `Tab.Focus` with zero screen hopping.
- Detailed in [`FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md).

---

### 3.2 Focus & Presets Screen (`Tab.Focus`)
The central command surface for starting sessions with any preset:
- **Header:** Title *"Focus & Presets"*, Sorting dropdown at top right (*Recently Used*, *Alphabetical*, *Most Used*), `+ New` button.
- **Presets Selection List:**
  - Vertical scrollable list of user presets rendered in `#1B263B` cards.
  - **Last-used preset is selected by default** with a `#D4C4A8` border highlight and radio dot.
  - Each card displays emoji, name, break pill (`#778D7A`), YouTube status, and edit pencil icon.
  - Tapping any card selects it as the active candidate.
- **Floating Island Mode Selector:**
  - Docked just above the primary CTA button, spanning the full button width.
  - Pill container in `#1B263B` with 3 segmented mode selectors:
    1. **Timer (Countdown):** Quick chips (`25m`, `45m`, `60m`, `90m`, `120m`, custom).
    2. **Stopwatch (Count-up):** Increments from `00:00`.
    3. **From–To (Window):** Select explicit start and end times (e.g., `4:00 PM - 5:30 PM`).
  - Active segment highlighted in `#778D7A` (Muted Sage Green).
- **Primary Action Button ("Start Focusing"):**
  - Full-width pill in `#D4C4A8` (Warm Sand Khaki) with bold `#0D1B2A` text.
  - Tapping launches the **Full-Screen Mechanical Flip Clock** in the selected mode with the chosen preset.

---

### 3.3 Focus Session Screen (Mechanical Flip Clock)
- **Immersive Mode:** System bars hidden (Immersive Sticky). Screen stays awake (`FLAG_KEEP_SCREEN_ON`).
- **Flip Clock Layout (Custom `@Composable`):**
  - Two large rectangular split cards: Top card displays Hours (`01`), bottom card displays Minutes (`45`).
  - Container color: Oxford Slate (`#1B263B`), 1dp border `#415A77`, corner radius 16dp.
  - Font: Monospace tabular numerals (`JetBrains Mono`), color Eggshell Cream (`#F4F1DE`).
  - Flap divider: 1.5dp horizontal hairline separator colored in Warm Sand (`#D4C4A8`).
  - **3D Flip Animation Physics:** 4-layer half-card rendering. Digit transitions execute a 400ms top-down flap rotation on the X-axis (`graphicsLayer { rotationX = ...; cameraDistance = 12f * density }`) hinged at the horizontal split line with shadow gradients and tactile mechanical click haptics.
- **Two-Row Controls Bar (Docked at Bottom):**
  - **Row 1:**
    - Left (75% / Three-Quarters Width): `"Take Break (2/2)"` capsule pill in `#1B263B` with `#778D7A` border. Shifts session to Break Mode. If breaks exhausted, greystyled at 30% opacity.
    - Right (25% / One-Quarter Width): `"Pause ⏸"` capsule pill in `#1B263B` with `#D4C4A8` icon. Limited to a **5-minute emergency limit** with live countdown badge; auto-penalty applies if not resumed.
  - **Row 2 (100% Full Width):**
    - In Countdown Mode: `"Stop Focusing"` pill. Tapping triggers the Stop-Focusing Friction Modal with the 15–25s locked delay.
    - In Stopwatch Mode: `"Finish Session"` pill in `#D4C4A8` with dark navy text. Tapping completes the session cleanly without penalty or streak forfeiture.
- **Completion Summary Card:** Upon session complete or stopwatch finish, displays a 2.5-second celebratory card showing focus time logged and streak flame increment (`🔥 18 → 19 Days`) before returning to Home.

---

### 3.4 Planner Screen & Standalone Scheduling (`Tab.Planner`)
- **Header:** Month-Year display, "Today" quick-jump button.
- **Calendar Strip:** Horizontally scrollable 7-day strip (Mon–Sun).
- **Scheduled Sessions List:** Cards showing time window, title, origin badge (`[FF]` or `[FD]`), linked preset, and heads-up reminder status.
- **FAB (`+ Schedule Session`):**
  - Positioned at bottom right.
  - Tapping opens the **Schedule Session Bottom Sheet**:
    1. **Select Preset (First Field):** Carousel of presets, defaults to last-used.
    2. **Session Name:** Auto-populates with selected Preset’s name if left empty; editable for custom titles.
    3. **Date & Times:** Start time, End time, auto-calculated duration.
    4. **Recurrence & Reminder:** Recurrence rule picker + 15-minute heads-up notification toggle.
    5. Saves directly to Room DB (`status = 'SCHEDULED'`) with zero dependency on FluxDone.

---

### 3.5 Stop-Focusing Friction Modal
- Displayed when the user taps *"Quit Session"* during active focus.
- **Backdrop:** Dimmed translucent scrim (`#0D1B2A` with 80% alpha).
- **Center Dialog Container:** Surface `#1B263B` with `#415A77` border.
- **Delay Ring Indicator:** Circular progress ring in `#D4C4A8` displaying locked seconds countdown (e.g., `"19s"`).
- **Warning Text:** *"Quitting will forfeit your 18-day streak and register a premature quit penalty."*
- **Button Controls & Asymmetric Lock Rule:**
  - Button 1: *"Keep Going"* in Muted Sage Green (`#778D7A`). **Always FREE, unlocked, and immediately clickable at all times** to encourage instant return to focus.
  - Button 2: *"Confirm Quit"* in Crimson Alert (`#DC2626`). **Strictly locked, disabled, and unclickable** until the countdown hits `0`.

---

### 3.6 Break Session State
- When break is active:
  - The flip clock border and primary accents shift from Warm Sand (`#D4C4A8`) to Muted Sage Green (`#778D7A`).
  - Top header label reads: *"Break in Progress (4m 12s remaining)"*.
  - Footer displays a single button: *"End Break Early"*.
  - When break timer expires, plays audio alert and automatically resumes focus session countdown.

---

### 3.7 Preset Creation & Edit Flow (`/presets/create`, `/presets/edit`)
- Full-screen push screen (`Scaffold`) with scrollable column and sticky bottom action bar.
- **Top Bar:** Back arrow `←`, title *"New Preset"* / *"Edit Preset"*, and *"Save"* text CTA in `#D4C4A8`.
- **Preset Identity:**
  - Emoji button: Opens Gboard/Android standard full emoji picker (`androidx.emoji2:emoji2-emojipicker`) with a top quick-access row of academic icons (📐, 🔬, 📚, 💻, 🎯, ⚡, 🧠, ⏳, ✍️, ♟️).
  - Outlined Preset Name text field in Eggshell Cream `#F4F1DE`.
- **Break Configuration:**
  - Break Count: Stepper `[ - ]` `2 breaks` `[ + ]` (`0` to `6`). Setting `0` disables break duration.
  - Break Duration: Stepper `[ - ]` `5 mins` `[ + ]` (`1` to `15` min).
- **Content-Filtered Apps (Multi-State Rules):**
  - Granular 3-way segmented pill controls (`Block` | `Allow` | `Study Mode 🛡️`) for **YouTube** and **Web Browsers (Chrome, Firefox, Brave)**.
  - Link: `[ ⚙️ Manage Global Content Whitelists → ]` routes to the **Content Whitelist Hub**.
- **Dedicated App Selection Screen (`/presets/select-apps`):**
  - Card displays summary: *"Blocked Apps: 5 Distracting, 1 Semi-Prod"*.
  - Tapping opens dedicated screen with search bar, quick-action chips (`[ ⚡ Block All Distracting ]`, `[ Reset to Defaults ]`), and collapsible category groups (🔴 Distracting, 🟢 Productive, 🟡 Semi-Productive, ⚪ Others) with custom lock checkboxes.
- **Global Content Whitelist Hub (`/presets/content-whitelists`):**
  - Manages global study channels and academic web domains shared across all presets.
  - **Discipline Sentence Verification:** Adding any channel or domain requires typing a randomly selected academic discipline aphorism with 100% character accuracy to prevent impulse whitelisting.
- **Sticky Footer:** Full-width pill *"Save Preset"* in Warm Sand `#D4C4A8` (and muted *"🗑️ Delete Preset"* button when editing).
- Detailed in [`FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md).

---

### 3.8 Distracted Intervention Overlay (5 / 10 / 20 / 40 min)
- Rendered via Android `WindowManager` with `LayoutParams.TYPE_APPLICATION_OVERLAY`.
- Floats directly over any blacklisted app that is launched outside a scheduled session.
- **UI Card:** Dark card `#1B263B` with border `#415A77` and text: *"Intentional Usage Check: How long do you need Instagram?"*
- **2×2 Time Selection Grid:**
  - 4 pill buttons in `#778D7A`: `5 min`, `10 min`, `20 min`, `40 min`.
  - Tapping a button dismisses the overlay and starts a floating countdown badge.
  - When time expires, accessibility service closes the distracting app and enforces a mandatory cooldown before re-selection.
- **Exit Button:** Full-width pill in `#D4C4A8`: `"Exit [App Name]"`.

---

### 3.9 YouTube Study Mode Friction Screen & Dynamic Edge Grabber
- Invoked when user opens a non-whitelisted YouTube video.
- Full-screen takeover displaying:
  - Video title and channel name.
  - Warning: *"Channel 'XYZ' is not on your academic study whitelist."*
  - Sentence prompt area: Displays random discipline quote (e.g., *"Discipline is choosing between what you want now and what you want most."*).
  - Text input field: User must type the sentence with 100% character accuracy to unlock a pass.
  - Whitelist button: *"Add to Academic Whitelist"* (requires entering master passcode).
- **Dynamic Edge Grabber:**
  - Floating pill handle docked to screen right edge inside YouTube.
  - **Visible when Study Mode is OFF.**
  - **Turning Study Mode OFF:** If toggled OFF, immediately triggers the 2×2 Time Selection Grid (5 min | 10 min | 20 min | 40 min) to prevent unrestricted consumption.

---

### 3.10 Stats Screen (`Tab.Stats`)
- Accessible via the 5th bottom navigation tab.
- **Top Segmented Switcher:** Holst pill container (`#1B263B` with border `#415A77`) toggling between **`Daily`** and **`Weekly Trends`**.
- **Hero Focus Ratio Card:**
  - Multiplier ratio display: e.g. **`2.1x`** in Eggshell Cream (`#F4F1DE`, 28sp bold) with discipline badge (e.g. `[Monk Mode ⚡]`).
  - Subtext: *"3h 15m Deep Focus vs 1h 32m Casual Screen"*.
  - Horizontal dual-split progress bar: Sage Green (`#778D7A`) focus share vs Terracotta Coral (`#E07A5F`) casual share.
- **7-Day Stacked Bar Chart:**
  - In `Daily`: Shows rolling 7-day stacked bars (Productive, Semi-Productive, Distracting, Others) with today's bar highlighted in Warm Sand (`#D4C4A8`) border.
  - In `Weekly Trends`: Shows Monday–Sunday stacked usage with horizontal average daily line, week-over-week deltas (Focus Δ, Distraction Δ), and discipline grade.
- **Category Filter Pills:** Horizontal row of 4 category pills (`🟢 Productive`, `🟡 Semi-Productive`, `🔴 Distracting`, `⚪ Others`) that filter the app list below.
- **App Usage List:**
  - Card list ranked descending by screen time.
  - Displays app icon, title, duration + percentage, and category indicator dot.
  - **Tapping any app row navigates immediately to `AppDetailScreen`.**

---

### 3.11 Dedicated App Detail Page (`AppDetailScreen`)
- Full-screen view opened when any app is clicked from `Tab.Stats` or `Tab.Limits`.
- **Top Bar:** Back button `←`, App icon (32×32dp), App title (`#F4F1DE`), and contextual Help button.
- **Period Dropdown:**
  - **`Daily ▾` Mode:** Shows selected day usage (e.g. `6h 34m`), `< Sun, 11 Oct >` date navigation, and 7-day bar chart where the selected day is highlighted in the category color (Sage `#778D7A` or Coral `#E07A5F`) while other days are muted steel `#415A77`.
  - **`Weekly ▾` Mode:** Shows daily average (e.g. `2h 57m`), date range `< 05 Oct - 11 Oct >`, delta comparison pill (`- 12% vs last week`), and all active days colored by category.
- **Usage Category Card:**
  - Card `#1B263B` with label *"Category"* and selector pill (e.g. `🟢 Productive ▾`).
  - Tapping opens modal bottom sheet with 4 categories (`Productive`, `Semi-Productive`, `Distracting`, `Others`).
  - Changing category triggers instant Room DB update (`app_category_overrides`) with zero-latency reactive recalculation across all app screens.
- **Manage Blocks Section:**
  - Direct bridge to `Tab.Limits`: displays current daily limit with button to add/edit limit.
  - Granular feature-level sub-blocks (e.g. IG Reels or YouTube Shorts toggle).
- **Privacy Lock Footer:** Locked badge confirming all data stays strictly on-device.

---

### 3.12 Home Screen Widget: 4-Category Donut Ring (Jetpack Glance)
- Compact 2×2 or 4×2 Android home screen widget built with Jetpack Glance.
- **Visuals:**
  - 4-segment Canvas Donut Ring strictly styled in Holst tokens:
    - Productive: Muted Sage Green (`#778D7A`)
    - Semi-Productive: Warm Sand Khaki (`#D4C4A8`)
    - Distracting: Terracotta Coral (`#E07A5F`)
    - Others: Muted Steel Blue (`#415A77`)
  - Center of Donut: Total Screen Time (e.g., `"4h 12m"`) in Eggshell Cream (`#F4F1DE`, 20sp bold).
  - Header: Focus Ratio badge (e.g., `⚡ 2.1x Focus`).
- **4×2 Quick Launcher:** One-tap button launching the last-used preset directly into a focus session.

---

### 3.13 Permissions Onboarding Wizard (`/onboarding/wizard`) & System Health UI
- **Single-Page Checklist:**
  - Surface `#1B263B` with border `#415A77` and corner radius 12dp per card.
  - Interactive status button: `[ Enable ➔ ]` in Warm Sand `#D4C4A8` ➔ updates automatically to `[ Granted 🟢 ]` in Sage `#778D7A` upon return from Android settings.
  - Primary bottom button *"Continue to Dashboard"* remains disabled until Notifications, Usage Access, Overlays, and Accessibility are granted.
  - Optional Unrestricted Battery card does not block completion.
- **OEM Killer Guide Dialog:**
  - Manufacturer-aware tips (`Build.MANUFACTURER`) for Samsung OneUI, Xiaomi MIUI/HyperOS, and OnePlus OxygenOS.
- **Ambient Health Recovery Card (Tab.Home):**
  - Emergency card in `#1B263B` with Coral border `#E07A5F`: *"⚠️ Focus Shields Compromised: Accessibility service was disabled. [Restore Protection ➔]"*.
- Detailed in [`FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md).



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

### 3.1 Home Screen
- **Header:** Current date, user avatar / Ari profile badge, streak flame counter with count (e.g., `"🔥 18 Days"`).
- **Active / Last-Used Preset Card:**
  - Surface container in `#1B263B` with 1dp border `#415A77`.
  - Emoji icon (e.g., 📐), Preset title (*"Olympiad Math Deep Work"*), Break pills (*"2 breaks · 5m each"*), YouTube status (*"Study Mode Whitelist"*).
  - Dropdown chevron allowing instant switching between saved presets.
- **Primary CTA:** Wide pill button in `#D4C4A8` with dark navy text: `"Start Focusing"`.
- **Today's Summary Strip:** Horizontal row showing Total Focus Time (`3h 45m`), Distracted Time (`22m`), and Tasks Done (`7`).

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
- **Button Controls:**
  - Button 1: *"Keep Going"*
  - Button 2: *"Confirm Quit"*
  - **Crucial Rule:** Both buttons remain disabled, unclickable, and muted until the countdown hits `0`.

---

### 3.6 Break Session State
- When break is active:
  - The flip clock border and primary accents shift from Warm Sand (`#D4C4A8`) to Muted Sage Green (`#778D7A`).
  - Top header label reads: *"Break in Progress (4m 12s remaining)"*.
  - Footer displays a single button: *"End Break Early"*.
  - When break timer expires, plays audio alert and automatically resumes focus session countdown.

---

### 3.7 Preset Creation & Edit Flow
- Single-page form inside a bottom sheet or screen:
  - Preset Name text field.
  - Emoji picker grid (⏳, 📐, 🔬, 📚, 💻, 🎯, ⚡, 🧠).
  - Break Count: Stepper selector (`0`, `1`, `2`, `3`, `4`, `5`, `6`). Setting `0` disables break duration.
  - Break Duration: Slider from `1 min` to `15 min`.
  - App Restriction Mode: Multi-select list of installed apps with search bar and quick filters (*Distracting*, *All*).
  - YouTube Mode: Segmented control: `Block` | `Allow` | `Study Mode`.
  - Save button in `#D4C4A8`.

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

### 3.10 Home Screen Widget (Jetpack Glance)
- Compact 2x2 or 4x2 Android home screen widget.
- **Visuals:**
  - Donut ring divided into 4 colored segments representing daily usage categories:
    - Productive: Green (`#2E7D32`)
    - Semi-Productive: Blue (`#1565C0`)
    - Distracting: Red / Orange (`#E64A19`)
    - Others: Grey (`#64748B`)
  - Center of Donut: Total Screen Time (e.g., `"4h 12m"`).
  - Quick launcher button for last-used preset.

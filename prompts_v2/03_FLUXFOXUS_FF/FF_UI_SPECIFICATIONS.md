# FluxFoxus (FF) — Master UI Specifications (v2.0)

**Version:** 2.0 (Jetpack Compose Screen & Overlay Specifications)  
**Target:** Native Android & Compose Desktop  
**Theme:** Strictly Dark Mode — **Rustic Medley** Palette (`FLUX_DESIGN_TOKENS.md`)

---

## 1. Design System Rules (Rustic Medley)

> [!IMPORTANT]
> **Zero Neon / Zero Purple:** Under no circumstances should any Composable render blue, purple, cyan, or neon accents.
> - Background: `#13191F` (River Styx)
> - Surface Cards: `#2B2F2E` (Carbon Fibre)
> - Border / Dividers: `#594C3D` (Afternoon Tea)
> - Accent / Break State: `#906D4B` (Tanned Wood)
> - Primary / Action: `#CA9C68` (Amber Autumn)
> - Text Primary: `#F8FAFC` (Ghost White)
> - Text Muted: `#94A3B8` (Slate Grey)

---

## 2. Navigation Architecture (5 Bottom Tabs)

The mobile interface is structured around 5 bottom navigation items:
1. **Home (`Tab.Home`):** Dashboard overview, current streak, quick preset launcher.
2. **Focus (`Tab.Focus`):** Direct entry to the active or standby mechanical flip-clock timer.
3. **Presets (`Tab.Presets`):** Management of reusable focus presets and YouTube whitelist.
4. **Limits (`Tab.Limits`):** Per-app daily time budgets, extra time tokens, and friction controls.
5. **Stats (`Tab.Stats`):** Usage statistics, 4-category breakdown, and streak history.

---

## 3. Screen Specifications

### 3.1 Home Screen
- **Header:** Current date, user avatar / Ari profile badge, streak flame counter with count (e.g., `"🔥 18 Days"`).
- **Active / Last-Used Preset Card:**
  - Surface container in `#2B2F2E` with 1dp border `#594C3D`.
  - Emoji icon (e.g., 📐), Preset title (*"Olympiad Math Deep Work"*), Break pills (*"2 breaks · 5m each"*), YouTube status (*"Study Mode Whitelist"*).
  - Dropdown chevron allowing instant switching between saved presets.
- **Primary CTA:** Wide pill button in `#CA9C68` with bold black/dark text: `"Start Focusing"`.
- **Today's Summary Strip:** Horizontal row showing Total Focus Time (`3h 45m`), Distracted Time (`22m`), and Tasks Done (`7`).

---

### 3.2 Focus Session Screen (Mechanical Flip Clock)
- **Immersive Mode:** System bars hidden (Immersive Sticky). Screen stays awake (`FLAG_KEEP_SCREEN_ON`).
- **Flip Clock Layout (Custom `@Composable`):**
  - Two large rectangular split cards: Top card displays Hours (`01`), bottom card displays Minutes (`45`).
  - Font: Monospace tabular numerals (`JetBrains Mono`), color `#F8FAFC`.
  - Flap divider: 1.5dp horizontal hairline separator colored in `#CA9C68`.
  - Subtle split shadow gradient simulated along the flap crease.
- **Controls Footer:**
  - Left Button: *"Take Break"* (displays remaining count: `2/2`). If `0`, button is greystyled (`#2B2F2E` with `#94A3B8` text).
  - Right Button: *"Quit Session"* (triggers Stop-Focusing Friction Modal).

---

### 3.3 Stop-Focusing Friction Modal
- Displayed when the user taps *"Quit Session"* during active focus.
- **Backdrop:** Dimmed translucent scrim (`#13191F` with 80% alpha).
- **Center Dialog Container:** Surface `#2B2F2E` with `#594C3D` border.
- **Delay Ring Indicator:** Circular progress ring in `#CA9C68` displaying locked seconds countdown (e.g., `"19s"`).
- **Warning Text:** *"Quitting will forfeit your 18-day streak and register a premature quit penalty."*
- **Button Controls:**
  - Button 1: *"Keep Going"*
  - Button 2: *"Confirm Quit"*
  - **Crucial Rule:** Both buttons remain disabled, unclickable, and muted until the countdown hits `0`.

---

### 3.4 Break Session State
- When break is active:
  - The flip clock border and primary accents shift from Amber (`#CA9C68`) to Tanned Wood (`#906D4B`).
  - Top header label reads: *"Break in Progress (4m 12s remaining)"*.
  - Footer displays a single button: *"End Break Early"*.
  - When break timer expires, plays audio alert and automatically resumes focus session countdown.

---

### 3.5 Preset Creation & Edit Flow
- Single-page form inside a bottom sheet or screen:
  - Preset Name text field.
  - Emoji picker grid (⏳, 📐, 🔬, 📚, 💻, 🎯, ⚡, 🧠).
  - Break Count: Stepper selector (`0`, `1`, `2`, `3`, `4`, `5`, `6`). Setting `0` disables break duration.
  - Break Duration: Slider from `1 min` to `15 min`.
  - App Restriction Mode: Multi-select list of installed apps with search bar and quick filters (*Distracting*, *All*).
  - YouTube Mode: Segmented control: `Block` | `Allow` | `Study Mode`.
  - Save button in `#CA9C68`.

---

### 3.6 Distracted Intervention Overlay (2 / 5 / 10 / 20 min)
- Rendered via Android `WindowManager` with `LayoutParams.TYPE_APPLICATION_OVERLAY`.
- Floats directly over any blacklisted app that is launched outside a scheduled session.
- **UI Card:** Dark card `#2B2F2E` with text: *"Intentional Usage Check: How long do you need Instagram?"*
- **Time Selection Buttons:**
  - 4 pill buttons in `#906D4B`: `2 min`, `5 min`, `10 min`, `20 min`.
  - Tapping a button dismisses the overlay and starts a floating countdown badge.
  - When time expires, accessibility service closes the distracting app and enforces a 5-minute cooldown.

---

### 3.7 YouTube Study Mode Friction Screen
- Invoked when user opens a non-whitelisted YouTube video.
- Full-screen takeover displaying:
  - Video title and channel name.
  - Warning: *"Channel 'XYZ' is not on your academic study whitelist."*
  - Sentence prompt area: Displays random discipline quote (e.g., *"Discipline is choosing between what you want now and what you want most."*).
  - Text input field: User must type the sentence with 100% character accuracy to unlock a 10-minute pass.
  - Whitelist button: *"Add to Academic Whitelist"* (requires entering master passcode).

---

### 3.8 Home Screen Widget (Jetpack Glance)
- Compact 2x2 or 4x2 Android home screen widget.
- **Visuals:**
  - Donut ring divided into 4 colored segments representing daily usage categories:
    - Productive: Green (`#2E7D32`)
    - Semi-Productive: Blue (`#1565C0`)
    - Distracting: Red / Orange (`#E64A19`)
    - Others: Grey (`#64748B`)
  - Center of Donut: Total Screen Time (e.g., `"4h 12m"`).
  - Quick launcher button for last-used preset.

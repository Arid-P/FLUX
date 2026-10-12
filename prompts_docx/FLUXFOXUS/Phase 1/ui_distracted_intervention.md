# FluxFoxus (FF) — UI: Distracted App Intervention
**Version:** 1.0  
**References:** ui_design_system.md  

---

## 1. Overview

FF intercepts distracted app usage in two scenarios:
1. **Outside focus session:** 2/5/10/20 time window overlay appears when a distracted-category app is opened
2. **Inside focus session:** User is redirected back to the FF focus session screen immediately upon opening a distracted app

This document covers scenario 1 (the intervention overlay) and the YouTube Study Mode edge grabber.

---

## 2. The 2/5/10/20 Intervention Window

### 2.1 Trigger
Activated when:
- User opens any app categorized as **Distracting**
- User is NOT currently in an active focus session
- App Limits are active for that app (or global distraction blocking is on)

### 2.2 Visual Structure
A bottom sheet-style overlay that slides up over the blurred app content. The app behind is visible but blurred — providing context that this is an intervention on the actual app.

**Container:**
- Background: `color_surface` (#1E293B)
- Border radius: 20px top corners
- Padding: 20px

**Blurred Background:**
- The app content behind is blurred (Android blur overlay)
- Provides context without allowing use

### 2.3 Content Layout (Top to Bottom)

#### Row 1 — App Info + Streak
Horizontal row:
- Left: App icon(s) + daily limit text e.g. "1h limit"
  - App icon: actual app icon, 28px, rounded
  - If multiple apps share the limit: show 2 icons side by side
  - Limit text: type_body, Ghost White
- Right: Streak counter — flame icon + "[X] days" in Ghost White
  - Flame icon: ~16px
  - Text: type_caption, Ghost White

#### Row 2 — Progress Bar
- Full width progress bar
- Shows: time spent today vs limit
- Fill color: gradient from Soft Cyan (#22D3EE, left) to Electric Indigo (#6366F1, right)
- Background track: `color_background` (#0F172A)
- Height: 8px, border radius: 4px

#### Row 3 — Time Labels
Two labels below the progress bar:
- Left: "[X]m / Spent today" — type_caption, Ghost White
- Right: "[X]h [Y]m / Limit left" — type_caption, Ghost White

#### Row 4 — YouTube Study Mode Card (YouTube only)
Only shown when the intercepted app is YouTube:
- Full-width card
- Background: `color_surface_elevated` with Soft Cyan border (1px #22D3EE)
- Left: YouTube icon (small, 20px)
- Text: "Watch only Study channels on YouTube"
  - type_body, Ghost White
- The entire card is tappable
- **On tap:** Activates YouTube Study Mode directly
  - Card updates to show "Study Mode: ON" state
  - The 2/5/10/20 time option buttons disappear
  - Edge grabber appears on screen right edge
  - YouTube opens in Study Mode

#### Row 5 — "How long do you want to use?" Label
- type_body, Ghost White, centered
- Padding: 8px 0

#### Row 6 — Time Option Grid (2×2)
Four pill buttons in a 2-column 2-row grid:

| Col 1 | Col 2 |
|---|---|
| 2 mins | 5 mins |
| 10 mins | 20 mins |

**Unselected state:**
- Background: transparent
- Border: 1px `color_border`
- Text: Ghost White, type_body, weight 500

**Selected state:**
- Background: Electric Indigo (#6366F1)
- Border: none
- Text: Ghost White, type_body, weight 600

**Default:** No option pre-selected. User must actively choose.

**Border radius:** 20px (pill shape)

#### Row 7 — Exit Button
- Full-width pill button
- Background: Electric Indigo (#6366F1)
- Text: "Exit [App Name]" — dynamic, uses actual app name
  - Example: "Exit YouTube", "Exit Instagram"
- Ghost White text, type_body, weight 600
- Border radius: 28px

### 2.4 App Session Flow (After Selecting Time)
1. User selects time option (e.g. 10 mins)
2. Overlay disappears
3. User uses app normally for selected duration
4. After duration expires: video/content pauses, overlay reappears
5. On reappearance: a wait period applies before user can select again:
   - 2 min option: 5 second wait
   - 5 min option: 10 second wait
   - 10 min option: 10 second wait
   - 20 min option: 15 second wait
6. During wait: time options shown but disabled with countdown
7. After wait: user can select a new time option or exit

### 2.5 No Interaction with Streak
The 2/5/10/20 session flow and its wait periods are pure friction. They do NOT affect streaks. Streak changes only occur via the Stop Focusing / break limit flow.

---

## 3. YouTube Study Mode

### 3.1 Overview
YouTube Study Mode enforces a channel whitelist on YouTube. When active, only whitelisted channels are allowed. Non-whitelisted channels trigger an intervention.

### 3.2 Activation Methods
1. **Via 2/5/10/20 window:** Tap the "Watch only Study channels on YouTube" card
2. **During focus session:** Shown as an option when session preset has Study Mode enabled (user choice, not forced)
3. **Manual toggle:** Via the edge grabber when already in YouTube

### 3.3 Non-Whitelisted Channel Intervention
When user opens a video from a non-whitelisted channel:
- Video is closed/interrupted
- Overlay message: "This channel is not on your study list."
- Two options shown:
  - **"Got it"** — closes YouTube or returns to home feed
  - **"Add to Study List"** — triggers the channel addition flow (see below)

### 3.4 Channel Addition Flow
When user taps "Add to Study List":

**Step 1 — Confirmation Sentence**
- A random sentence is displayed from the confirmation pool
- User must type the sentence exactly in the input field below it
- Submit button activates only when typed text matches exactly (case-insensitive)
- On successful match: channel is added to whitelist, user can continue watching

**Sentence Pool Rules:**
- First 25 channels ever added: pool of 5 short sentences (low friction to encourage early adoption)
- After 25 channels: pool of ~50 longer sentences (higher friction)
- Sentences are randomly selected each time — not the same sentence twice in a row

**UI for sentence confirmation:**
- Full-screen or large bottom sheet
- Sentence displayed in a `color_surface` card with Soft Cyan border
- Input field below: standard text input, `color_surface` background
- Submit button: Electric Indigo pill, disabled until text matches
- Cancel option: plain text button "Cancel" top right

### 3.5 Edge Grabber

#### Appearance
- Position: anchored to right edge of screen, vertically centered
- Width: ~32px
- Height: ~80px
- Shape: vertical pill/tab
- Background: Electric Indigo (#6366F1)
- Embedded icon: FF aperture logo in Ghost White, small (~16px), centered vertically
- Always visible when Study Mode is active and YouTube is in foreground

#### Collapsed State (Default)
- Shows only the Indigo vertical tab with aperture logo
- Does not obscure content significantly

#### Expanded State (After Tap)
Tapping the grabber expands it horizontally to reveal a floating control panel:

**Control Panel:**
- Background: Midnight Slate (#0F172A)
- Border radius: 12px
- Padding: 12px 16px
- Contents (left to right in a single row):
  - "YouTube Study Mode" label — Ghost White, type_body
  - Toggle switch — Soft Cyan (#22D3EE) when ON, #334155 when OFF
  - × close button — Ghost White icon

**Toggle States:**
- **ON (default when grabber visible):** Cyan toggle, Study Mode active
- **OFF:** Greyscale toggle, 2/5/10/20 window reappears for YouTube

#### Auto-dismiss
- Grabber disappears when:
  - User leaves YouTube (any other app comes to foreground)
  - Active focus session ends (if Study Mode was session-scoped)
  - User turns off Study Mode via the toggle

### 3.6 Study Mode During Focus Session
- When a focus session is active and user opens YouTube:
  - If preset has Study Mode: Study Mode activates automatically, edge grabber appears
  - If preset does not have Study Mode: user is redirected back to focus session screen (standard distracted app block)
- Study Mode is the user's choice in preset config — never forced by FF

---

## 4. Inside Focus Session — Distracted App Block

### 4.1 Behavior
When user opens a Distracting-category app during an active focus session:
- No overlay on the distracted app
- User is immediately redirected back to the FF focus session screen
- The focus session continues uninterrupted
- No notification — silent redirect

### 4.2 Exceptions
- YouTube with Study Mode active: allowed (Study Mode overrides the block)
- Apps in the Productive category: never blocked
- Apps in Semi-Productive category: not blocked during sessions (configurable per preset)

# FluxFoxus (FF) — UI: Focus Session, Break Window, Stop Modal
**Version:** 1.0  
**References:** ui_design_system.md, ui_navigation.md  

---

## 1. Active Focus Session Screen

### 1.1 Overview
Full-screen takeover. Bottom navigation hidden. Mechanical flip clock dominates the center. Minimal UI — maximum focus. Used for all three timer modes: Countdown, Stopwatch, Open-Ended.

### 1.2 Background
- Solid Midnight Slate (#0F172A)
- No texture, no aurora, no gradients
- Status bar: transparent

### 1.3 Header Row
Full-width horizontal row at the top of the screen:

**Left — Preset Name Pill**
- Preset name text inside a pill
- Background: Electric Indigo (#6366F1)
- Text: Ghost White (#F8FAFC), type_body, weight 600
- Border radius: 20px
- Padding: 6px 14px
- Example: "Python Practice"

**Right — Session Info**
- Normal mode: session time range e.g. "09:00 PM - 12:00 AM"
  - Style: type_caption, `color_text_muted` (#94A3B8)
- Break mode: "BREAK" label replaces or appears alongside time range
  - "BREAK" style: type_body, weight 700, Soft Cyan (#22D3EE)
  - Time range remains below in `color_text_muted`

### 1.4 Main Display — Flip Clock

#### Container Cards
- Two large rounded-square cards stacked vertically, dominating the screen center
- Each card:
  - Background: `color_surface` (#1E293B)
  - Border: 1px Electric Indigo (#6366F1)
  - Border radius: 16px
  - Large enough to fill ~70% of screen width
  - Internal padding: generous (card itself is the visual hero)

#### Flip Animation
- Horizontal split line divides each card at the midpoint
- Line color: Midnight Slate (#0F172A) — creates the mechanical flip illusion
- Digit changes trigger a flip animation (top half flips down)

#### Digit Display
- **Focus mode:** Ghost White (#F8FAFC) digits
- **Break mode:** Soft Cyan (#22D3EE) digits
- Font: type_display (48px+), weight 700, tabular numerals
- Top card: HOURS (HH format)
- Bottom card: MINUTES (MM format)
- Unit label: bottom-right corner of each card — "HOURS" / "MINUTES" in type_micro, `color_text_muted`

#### Timer Modes
| Mode | Behavior | Display Start |
|---|---|---|
| Countdown | HH:MM counting down to 00:00 | Preset/task duration |
| Stopwatch | HH:MM counting up from 00:00 | 00:00 |
| Open-Ended | HH:MM counting up from 00:00 | 00:00, no end time shown in header |

### 1.5 Bottom Controls Bar
Three elements in a horizontal row at the screen bottom (above safe area):

**Left (~50% width) — Stop Focusing Button**
- Wide horizontal pill
- Background: `color_surface` (#1E293B)
- Text: "Stop Focusing" (Ghost White, type_body, weight 400 — thin font)
- Border radius: 28px
- Padding: 14px vertical

**Center — Break Pill**
- Smaller pill button
- Style: outline, Soft Cyan (#22D3EE) border
- Text: "Break" (Ghost White, type_body, weight 600)
- Border radius: 20px

**Right — Pause/Play Circle**
- Circular button
- Background: Electric Indigo (#6366F1)
- Icon: pause (||) or play (▶) in Ghost White
- Diameter: ~52px

#### Break Button States
| State | Appearance |
|---|---|
| Breaks available | Soft Cyan outline, Ghost White text, fully interactive |
| No breaks configured in preset | Button absent entirely |
| All breaks exhausted | Greyscale (opacity 0.4), non-interactive |

---

## 2. Break Window

### 2.1 Overview
Same full-screen layout as focus session. Palette shifts from Indigo to Cyan to signal break state. Bottom navigation remains hidden.

### 2.2 Header Changes
- Left pill: same preset name, same Electric Indigo pill
- Right: "BREAK" in Soft Cyan (#22D3EE), weight 700
- Below "BREAK": session time range in `color_text_muted`

### 2.3 Flip Clock Changes
- Card background: #1A2333 (slightly different from focus mode's #1E293B)
- Digit color: Soft Cyan (#22D3EE) instead of Ghost White
- Counts DOWN from break duration to 00:00
- Same flip animation mechanics

### 2.4 Bottom Controls During Break
- "Stop Focusing" pill: same as focus mode (left)
- "Break" pill: ABSENT (replaced by nothing — only Stop Focusing and End Break Early)
- Pause button: ABSENT (timer cannot be paused during break)
- Single centered full-width button:

**"End Break Early" Button**
- Full width pill
- Background: Electric Indigo (#6366F1)
- Text: "End Break Early" (Ghost White, type_heading_2, weight 600)
- Border radius: 28px
- Padding: 16px vertical
- Tapping ends break early and resumes focus session

### 2.5 Break End Behavior
- When break countdown hits 00:00:
  - Vibration + notification
  - Auto-returns to focus session screen
  - Break count decremented by 1

---

## 3. Stop Focusing Modal

### 3.1 Trigger Conditions
Displayed when either:
- User taps "Stop Focusing" during an active session
- All breaks are exhausted (same flow, same modal — no distinction in copy)

### 3.2 Modal Container
- Floating card, centered on screen
- Background: #1A2333
- Border radius: 12px (sharp, geometric — echoes flip clock card aesthetic)
- Backdrop: blurred background of the focus session screen
- NOT dismissible by tapping outside — user must interact with buttons
- Padding: 24px

### 3.3 Modal Content (Top to Bottom)

**Alert Icon**
- Mechanical-style alert bell icon
- Color: Electric Indigo (#6366F1)
- Size: ~40px
- Centered

**Primary Text**
- "Finish the goal?"
- Style: type_heading_2, Ghost White, weight 600, centered

**Secondary Text**
- "You will break your focus streak if you stop now."
- Style: type_body, `color_text_muted`, centered

**Streak Display**
- Label: "FluxFoxus Streak" in `color_text_muted`, type_caption, centered
- Row below:
  - Left: [Small Electric Indigo dot] + "[X] days" in Ghost White, weight 600
  - Center: Indigo arrow (→)
  - Right: [Small grey dot] + "0 days" in `color_text_muted`
- This row shows current streak → reset value

### 3.4 Wait Timer
- Both buttons start in disabled state (opacity 0.3)
- A countdown runs: duration is 15–25 seconds, calculated by formula (see PRD §streak)
- The "Quit Session" button displays countdown inline: "Quit Session (18)" counting down
- When countdown reaches 0: both buttons become active

### 3.5 Action Buttons (Bottom of Modal)
Two symmetrical pill buttons side by side:

**Left — "Keep Going" (Primary)**
- Background: Electric Indigo (#6366F1)
- Text: "Keep Going" (Ghost White, type_body, weight 600)
- Border radius: 20px
- On activate (countdown done + tap): closes modal, resumes session

**Right — "Quit Session" (Secondary)**
- Style: outline, Soft Cyan (#22D3EE) border
- Text: "Quit Session ([countdown])" → "Quit Session" when active
- Ghost White text
- Border radius: 20px
- On activate (countdown done + tap): ends session, resets streak to 0, navigates to home

### 3.6 Wait Time Formula
Duration (seconds) = weighted average of two factors:

| Streak Length | Streak Weight | Usage Weight |
|---|---|---|
| < 10 days | 55% | 45% |
| 10–20 days | 45% | 55% |
| 21–29 days | 40% | 60% |
| 30–44 days | 25% | 75% |
| 45–59 days | 15% | 85% |
| 60–89 days | 10% | 90% |
| ≥ 100 days | 0% | 100% |

- Streak score: normalized (streak_days / max_streak) × 25 seconds max contribution
- Usage score: based on today's distracting app usage (productive time excluded) normalized against personal average
- Result: clamped between 15 and 25 seconds
- **Productive app time is excluded from usage calculation**

---

## 4. Session Completion

### 4.1 Countdown Timer Reaches 00:00
- Vibration trigger
- System notification: "Session complete"
- UI auto-navigates to Home screen
- No completion screen or animation — straight to home

### 4.2 Stopwatch / Open-Ended Manual End
- User taps "Stop Focusing" → Stop Focusing modal appears
- If user confirms via "Quit Session": navigates to home, session logged as complete
- Streak logic applies based on whether minimum sessions for the day were met

# FluxFoxus (FF) — UI: Planner Screen
**Version:** 1.0  
**References:** ui_design_system.md, ui_navigation.md  

---

## 1. Overview
The Planner screen is a read-only view of scheduled focus sessions for the selected day. Sessions come from two sources: created manually in FF, or synced automatically from FluxDone (FD). The Planner also serves as the entry point for creating new Presets via the FAB.

---

## 2. Screen Structure

```
[ Header ]
[ Streak Bar ]
[ Calendar Strip (horizontal day selector) ]
[ Stats Row (2 cards) ]
[ Session List (scrollable) ]
[ FAB — Add Preset ]
[ Bottom Navigation ]
```

---

## 3. Header

- Left: Month + Year label — "March 2026" (type_heading_1, Ghost White)
- Right: Two buttons in `color_surface` pills:
  - "Today" — jumps calendar strip to today
  - "Help"

---

## 4. Streak Bar

Immediately below header:
- Left: Small flame/streak icon (Electric Indigo, ~20px square, border radius 4px)
- Text: "Current streak: [X] days"
  - "Current streak:" in `color_text_muted`, type_caption
  - "[X] days" in Soft Cyan (#22D3EE), type_caption, weight 600

---

## 5. Calendar Strip

### 5.1 Layout
Horizontally scrollable strip of 7 visible day items. Centers on current week by default.

### 5.2 Day Item
Each day item:
- Day abbreviation: Mon/Tue/Wed/Thu/Fri/Sat/Sun — type_micro, `color_text_muted`
- Date number: type_body, Ghost White, weight 600
- Width: ~48px
- Padding: 8px 12px
- Border radius: 8px

### 5.3 States
| State | Background | Text color |
|---|---|---|
| Inactive | Transparent | `color_text_muted` (day) / Ghost White (date) |
| Active/Selected | Electric Indigo (#6366F1) | Ghost White (both) |
| Today (unselected) | Subtle Electric Indigo rgba(99,102,241,0.15) | Ghost White |

### 5.4 Behavior
- Tapping a day selects it and updates the session list below
- Swiping scrolls the strip to adjacent weeks

---

## 6. Stats Row

Two equal-width cards side by side:

**Card 1 — Focus**
- Label: "Focus" (type_label, `color_text_muted`, uppercase)
- Value: Total focus time for selected day (type_heading_2, Ghost White, weight 700)
- Example: "2h 27m"

**Card 2 — Usage**
- Label: "Usage" (type_label)
- Value: Total screen time for selected day (type_heading_2, Ghost White, weight 700)
- Example: "6h 18m"

Card style: `color_surface`, 1px `color_border`, border radius 8px, padding 16px

---

## 7. Session List

### 7.1 Scrollable Area
Fills remaining screen space between stats row and FAB. Independently scrollable.

### 7.2 Session Card

**Container:**
- Background: `color_surface` (#1E293B)
- Border: 1px `color_border`
- Border radius: 8px
- Border-left: 3px solid Electric Indigo (#6366F1) — active/upcoming sessions
- Border-left: 3px solid Productive (#14B8A6) — completed sessions
- Padding: 16px
- Margin-bottom: 12px

**Layout (single card):**
```
[ Session Icon ] [ Session Title ]  [ Preset Pill ]  [ Source Badge ]
                 [ Time Range ]  [Sync Icon if FD]
                                                       [ Duration + Label ]
```

**Session Icon:**
- The emoji assigned to the preset
- 40×40px container, background rgba(99,102,241,0.1), border radius 8px

**Session Title:**
- type_body, Ghost White, weight 600

**Preset Pill:**
- The preset name
- Background: rgba(34,211,238,0.15), text Soft Cyan (#22D3EE)
- type_micro, weight 600, uppercase, letter-spacing 0.5px
- Border radius: 4px, padding: 2px 8px

**Source Badge:**
- "FF" or "FD" label
- Background: rgba(99,102,241,0.2), text Electric Indigo (#6366F1)
- type_micro, weight 700
- Border radius: 4px, padding: 2px 6px

**Time Range:**
- "12:00 pm - 2:00 pm" — type_caption, `color_text_muted`
- Sync icon (↻) only shown for FD-sourced sessions — Electric Indigo, 12px

**Duration + Label (right side):**
- Duration value: type_body, Ghost White, weight 600
- Sub-label below value: type_micro, `color_text_muted`
  - Future/upcoming session: "Planned"
  - Currently active session: "Live" in Soft Cyan (#22D3EE)
  - Completed session: "Spent" in Productive (#14B8A6)

### 7.3 Completed Session State
- Overall card opacity: 0.6
- Border-left: 3px Productive (#14B8A6)
- Checkmark (✓) in Productive color, top-right corner of card
- Duration shows actual time spent ("Spent" sub-label)

### 7.4 Active Session State
- Border-left: 3px Electric Indigo
- Duration shows live elapsed time (updates every second)
- "Live" sub-label in Soft Cyan

### 7.5 Empty State
When no sessions scheduled for selected day:
- FF aperture icon centered (muted, ~64px)
- "No sessions for this day" text, `color_text_muted`, type_body
- Container: `color_surface`, border radius 16px, padding 60px 20px

---

## 8. FAB — Add Preset

- Position: centered horizontally, absolute positioned above bottom navigation
- Bottom offset: ~100px (clears bottom nav bar)
- Style: pill button
- Background: Electric Indigo (#6366F1)
- Text: "+ Add Preset" — Ghost White, type_body, weight 600
- Icon: "+" left of text, font-size 20px
- Border radius: 28px
- Padding: 14px 28px
- On tap: opens Preset Creation flow (full-screen push navigation)

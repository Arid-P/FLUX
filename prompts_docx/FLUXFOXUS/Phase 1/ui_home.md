# FluxFoxus (FF) — UI: Home Screen
**Version:** 1.0  
**References:** ui_design_system.md, ui_navigation.md  

---

## 1. Overview
The home screen is the default landing screen of FF. It serves as a command center — showing today's usage at a glance, the active or next preset, and a single "Start Focusing" CTA. It is not a pure dashboard; it is a quick-start screen with embedded stats context.

---

## 2. Layout Structure (Top to Bottom)

```
[ Header ]
[ Momentum Card ]
[ --- Section Divider --- ]
[ Session Card ]
[ --- Section Divider --- ]
[ Start Focusing Button ]
[ --- Section Divider --- ]
[ Bottom Navigation Bar ]
```

---

## 3. Header

### 3.1 Left Side
- Small FF aperture emblem icon (white, ~24px)
- "FluxFoxus" text immediately to the right (type_heading_1, Ghost White)

### 3.2 Right Side
Two pills stacked vertically, right-aligned:

**Pill 1 — Focused**
- Label: "Focused:" in Ghost White (type_caption)
- Value: average daily focus time this week in Soft Cyan (#22D3EE) (type_caption, weight 600)
- Example: "Focused: 2h 53m"
- Background: `color_surface` (#1E293B)
- Border radius: 20px
- Padding: 6px 12px

**Pill 2 — Weekly**
- Label: "Weekly:" in Ghost White (type_caption)
- Value: average daily total screen time this week in Soft Cyan (#22D3EE)
- Example: "Weekly: 3h 20m"
- Same style as Pill 1

> Both values are weekly averages, not totals. "Focused" = avg daily focus time this week. "Weekly" = avg daily screen time this week up to today.

---

## 4. Momentum Card

### 4.1 Card Container
- Background: `color_surface` (#1E293B)
- Border: 1px `color_border`
- Border radius: 8px
- Padding: 16px
- Margin: 0 20px

### 4.2 Card Header Row
- Left: "Momentum" label (type_heading_2, Ghost White)
- Right: "Last 24h" label (type_caption, `color_text_muted`)

### 4.3 Area Chart
- Type: Smooth Bézier stacked area chart (identical to Usage Stats Today tab)
- Height: ~160px
- X-axis labels: 6am, 12pm, 6pm, 12am (type_micro, `color_text_muted`)
- Y-axis: no labels on home screen version (clean, minimal)
- Layer order (bottom to top): Productive → Semi-Productive → Distracting → Others
- Colors: category palette (see ui_design_system.md §2.2)
- No interactive tooltips on home screen version
- No border on chart itself

### 4.4 Legend Grid (2×2)
Below the chart, a 2-column 2-row grid:

| Col 1 | Col 2 |
|---|---|
| [Teal dot] Productive / [time] | [Blue dot] Semi-Productive / [time] |
| [Charcoal dot] Distracting / [time] | [Grey dot] Others / [time] |

- Dot size: 8px circle
- Category name: type_caption, `color_text_muted`
- Time value: type_body, Ghost White, weight 700
- Divider line between the two rows: 1px `color_border`
- Divider line between the two columns: 1px `color_border`

---

## 5. Session Card

### 5.1 Card Container
- Background: `color_surface` (#1E293B)
- Border: 1px `color_border`
- Border radius: 8px
- Padding: 16px
- Margin: 0 20px

### 5.2 Top Row (Preset Selector)
A single horizontal row containing 4 elements left to right:

**Element 1 — Today's Focus Time**
- Display: total focus time for today (not average) e.g. "1hr 43m"
- Style: type_display (large, bold), Ghost White
- Background: subtle `color_surface_elevated` pill or no background — prominent text only

**Element 2 — Preset Emoji**
- The emoji assigned to the currently selected preset
- Size: ~24px
- Tappable: tapping navigates to the Focus tab / preset selection

**Element 3 — Preset Name**
- The name of the currently selected (last used) preset e.g. "Preset"
- Style: `color_surface` pill, Ghost White text (type_body, weight 500)
- Border: 1px `color_border`
- Border radius: 20px

**Element 4 — Dropdown Chevron**
- A standalone chevron/dropdown button (▾)
- Background: `color_surface` pill
- Border: 1px `color_border`
- Tapping opens a dropdown/bottom sheet to select a different preset
- If no preset exists: shows "No Preset" with a "+" to create one

### 5.3 Session Details Rows
Below the top row, 4 detail rows with label (left, `color_text_muted`) and value (right, Ghost White):

| Label | Value | Notes |
|---|---|---|
| Duration | "08:30pm — 11:00pm" or "∞" | Time range from preset-linked task, or ∞ for open-ended |
| Breaks | "4 Sessions" | Total number of breaks configured in preset |
| Break Time (each) | "10m" | Duration per break |
| Description | "[preset description text]" | Italic, Ghost White, type_body. Hidden if no description. |

- Each row: full width, padding 8px 0, border-bottom 1px `color_border` (except last)
- "DESCRIPTION" sub-label in type_label (uppercase, muted) above description text

### 5.4 Empty State (No Preset)
When no preset has been used/created:
- Top row shows: "No Preset" + "Create one →" link in Electric Indigo
- Detail rows hidden
- "Start Focusing" button still visible but disabled until preset selected

---

## 6. Start Focusing Button

- Full width pill button
- Background: Electric Indigo (#6366F1)
- Text: "Start Focusing" (Ghost White, type_heading_2, weight 600)
- Border radius: 28px
- Padding: 16px vertical
- Margin: 0 20px
- On tap: navigates to active focus session screen
- Disabled state (no preset): opacity 0.5, non-interactive

---

## 7. Section Dividers
Three subtle dividers separate the major sections:
1. Between header and Momentum card
2. Between Momentum card and Session card
3. Between Session card and Start Focusing button

Each divider: 1px height, `color_border`, full width.

---

## 8. States

### 8.1 Active Session State
When a focus session is already running:
- "Start Focusing" button changes to "Resume Session" in Soft Cyan (#22D3EE) background
- Session card shows elapsed time instead of planned duration
- Tapping "Resume Session" returns to active session screen

### 8.2 Scheduled Session Upcoming
When a FD-synced session is scheduled within the next 15 minutes:
- A small notification banner appears below the header:
  "[Session name] starts in [X] min"
  Background: rgba(99,102,241,0.15), Electric Indigo border left 3px
  Tappable to view session details in Planner

### 8.3 No Data State (First Launch)
- Momentum card shows empty chart with "No data yet" centered text
- Both header pills show "—"
- Session card shows empty preset state

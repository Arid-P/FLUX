# FluxFoxus (FF) — UI: Home Screen Widget
**Version:** 1.0  
**References:** ui_design_system.md  

---

## 1. Overview
FF provides an Android home screen widget that shows today's screen time breakdown at a glance. It is a 4-category donut ring widget with a category legend. It updates periodically in the background.

---

## 2. Widget Container

- Shape: Rounded rectangle
- Background: Slate Blue (#1E293B)
- Corner radius: 16px
- Padding: 16px internal
- No shadow (flat, consistent with FF design language)
- Android widget size: approximately 4×2 grid cells (standard medium widget)

---

## 3. Layout (Left to Right)

### 3.1 Left Zone — Glance Time
- Main value: total screen time today — e.g. "3h 9m"
  - Font: type_display (large, 28–32px), Ghost White (#F8FAFC), weight 700
- Sub-label below: "Today's usage"
  - Font: type_caption, #94A3B8 (muted)
- Optional: small "Live Focus" indicator when a session is active
  - Small Electric Indigo (#6366F1) pulsing dot + "Live" label in type_micro

### 3.2 Center Zone — Donut Ring
- 4-segment donut ring
- Segment order (clockwise from top): Productive → Semi-Productive → Distracting → Others
- Colors: fixed category palette
  - Productive: #14B8A6
  - Semi-Productive: #3F51B5
  - Distracting: #1A2333
  - Others: #94A3B8
- Gap between segments: 2px
- Ring thickness: medium (not too thin, not filled)
- Center of ring: empty (no text inside ring)
- Size: fills the center zone proportionally

### 3.3 Right Zone — Category Legend
4 rows, one per category:

| Dot | Label | Time |
|---|---|---|
| Teal dot (#14B8A6) | Productive | [time] |
| Blue dot (#3F51B5) | Semi-Prod | [time] |
| Charcoal dot (#1A2333)* | Distracting | [time] |
| Grey dot (#94A3B8) | Others | [time] |

*Distracting dot on widget: since the widget background is #1E293B, the #1A2333 dot may be nearly invisible. Apply a 1px #94A3B8 border around the Distracting dot on the widget.

- Dot size: 8px circle
- Label: type_caption, #94A3B8
- Time: type_caption, Ghost White (#F8FAFC), weight 600
- Row alignment: dot + label left-aligned, time right-aligned
- Rows separated by ~4px vertical gap

---

## 4. Branding

- Bottom-right corner of widget: tiny FF aperture symbol
- Color: Ghost White (#F8FAFC), very small (~12px), low opacity (0.4)
- Does not interfere with data display

---

## 5. Widget States

### 5.1 Normal State
Displays today's data as described above.

### 5.2 Live Focus Active
- "Live Focus" indicator visible (pulsing Indigo dot + "Live" label)
- Donut ring may animate subtly (optional)
- Productive time updates in near-real-time

### 5.3 No Data State (First Launch)
- Donut ring shows a single grey ring (empty state)
- Glance time: "--"
- Legend: all times "--"

---

## 6. Update Frequency
- Widget updates every 15 minutes via WorkManager background job
- Updates immediately when a session ends
- Updates immediately when a focus session starts

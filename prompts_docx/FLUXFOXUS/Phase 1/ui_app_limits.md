# FluxFoxus (FF) — UI: App Limits (Blocks Tab)
**Version:** 1.0  
**References:** ui_design_system.md, ui_navigation.md  

---

## 1. Overview
The Blocks tab (5th tab in bottom navigation) is where the user manages per-app daily time limits. This is separate from preset-based blocking during sessions — this is persistent daily usage limits. Apps are grouped by category. Each app has a configurable daily time limit and optional extra time sessions.

---

## 2. Main Screen Structure

```
[ Header ]
[ Section Header: "App Limits" + Add App button ]
[ Category Group: Distracting ]
[ App Cards ]
[ Category Group: Semi-Productive ]
[ App Cards ]
[ Category Group: Others ]
[ App Cards ]
[ Bottom Navigation ]
```

---

## 3. Header

- Title: "Blocks" — type_heading_1, Ghost White
- Right: "Help" button — `color_surface` pill, type_caption, Ghost White

---

## 4. Section Header Row

- Left: "App Limits" — type_heading_2, Ghost White
- Right: "+ Add App" button — text only, Productive (#14B8A6) color, type_body, weight 500

---

## 5. Category Groups

Each category group:

**Category Label Row:**
- Left: Category dot (8px circle) + category name (type_label, uppercase, `color_text_muted`)
- No interactive elements on this row — purely a label
- Padding: 24px 20px 12px

**App Cards below the label row:**
Contained in a vertically stacked list (no container card around the group — each app card is individual).

---

## 6. App Card

### 6.1 Container
- Background: `color_surface` (#1E293B)
- Border radius: 12px
- Padding: 16px
- Margin-bottom: 12px
- Tappable: opens App Settings bottom sheet

### 6.2 App Header Row
- Left: App icon (44×44px, border radius 10px)
- Center: App name (type_heading_3, Ghost White, weight 500) + sub-label "[X]m spent / [Y]m limit" (type_caption, `color_text_muted`)
- Right: Chevron (›, `color_text_muted`, 20px)

### 6.3 Status Badge Row (below app header)
Two possible states:

**Active / Blocking:**
- Badge: inline pill, background rgba(20,184,166,0.15), text Productive (#14B8A6)
- Left dot: 8px Productive color
- Text: "Blocking" — type_caption, weight 500

**Paused:**
- Badge: inline pill, background rgba(148,163,184,0.15), text `color_text_muted`
- Left dot: 8px `color_text_muted`
- Text: "Paused" — type_caption, weight 500
- Sub-text below badge: "Turned off till [Day, Date Time]" — type_caption, `color_text_muted`

---

## 7. App Settings Bottom Sheet

### 7.1 Trigger
User taps any app card on the main screen.

### 7.2 Sheet Header
- App icon (40×40px, left)
- App name + " Limit" — type_heading_2, Ghost White
- Streak badge — "[🔥] [X] days" — background rgba(255,193,7,0.15), text #FFC107, border radius 12px
- Close button (×) — right

### 7.3 Time Picker — Dual Column Scroll

**Container:**
- Background: rgba(99,102,241,0.1)
- Border radius: 16px
- Padding: 20px

**Time Display:**
- Center: "[H] hr [MM] min" — type_display (32px), Soft Cyan (#22D3EE)
- "hr" and "min" labels: type_body, `color_text_muted`

**Two scroll columns (Hours | Minutes):**
- Each column: scroll-snap, one value visible at center = selected
- Hours column: 0–6 (range enforced: min 15 mins total)
- Minutes column: 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55
- Selected item: type_heading_2 (24px), Ghost White, weight 600
- Unselected items: type_body (18px), `color_text_muted`
- Center highlight band: rgba(99,102,241,0.2), border radius 8px, height 40px
- Column label above: "HOURS" / "MINUTES" — type_micro, uppercase, `color_text_muted`

**Range enforcement:**
- The selectable range for each app is: 15 mins minimum to (average daily usage + 1 hour) maximum
- Range is derived from actual Android screen time data
- Day 1 (no history): uses Android UsageStats API which provides data even from first launch

### 7.4 Suggestion Bar
Below time picker:
- Left: 💡 icon
- Text: "Suggested limit is [X]h based on your average usage"
  - "[X]h" in Productive (#14B8A6), weight 600
- Background: rgba(20,184,166,0.1), border-left 3px Productive
- Border radius: 0 8px 8px 0
- Type: type_caption, `color_text_muted`

### 7.5 Extra Time Sessions Section

**Container:**
- Background: rgba(34,211,238,0.05)
- Border: 1px rgba(34,211,238,0.2)
- Border radius: 12px
- Padding: 16px

**Header row:**
- Left: "Extra Time Sessions" — type_body, Ghost White, weight 600
- Right: "Max 60 mins total" badge — rgba(34,211,238,0.1) background, Soft Cyan text, type_micro

**Row 1 — Number of sessions:**
- Label: "Number of sessions" — type_caption, `color_text_muted`
- Right: Stepper [−] [value] [+]
  - Stepper buttons: 36×36px, rgba(148,163,184,0.2) background, Ghost White icon, border radius 10px
  - Value: type_body (18px), Ghost White, weight 600
  - Range: 0–6

**Row 2 — Duration per session:**
- Label: "Duration per session" — type_caption, `color_text_muted`
- Right: Chip selector row
  - Chips: "5m", "10m", "15m" (20m chip removed — max 15 min per session)
  - Inactive chip: rgba(148,163,184,0.1) background, 1px transparent border, `color_text_muted` text
  - Active chip: rgba(34,211,238,0.2) background, 1px Soft Cyan border, Soft Cyan text
  - Border radius: 8px, padding: 8px 16px

**Row 3 — Total extra time (calculated):**
- Divider: 1px `color_border` above this row
- Left: "Total extra time" — Soft Cyan (#22D3EE), type_caption
- Right: "[X] mins" — Soft Cyan, weight 600
- If total > 60 mins: text turns #EF4444 (danger) as visual warning

### 7.6 Action Buttons

**Save Changes:**
- Full width
- Background: Ghost White (#F8FAFC) — intentional exception for App Limits screen
- Text: Midnight Slate (#0F172A), type_body, weight 600
- Border radius: 12px

**Turn Off Block:**
- Full width
- Background: rgba(239,68,68,0.15)
- Text: #EF4444, type_body, weight 600
- Border radius: 12px

---

## 8. Turn Off Block Flow

### 8.1 Streak Warning Sheet
Appears when user taps "Turn Off Block":

**Content:**
- Handle
- Title: "You'll lose your streak if you turn off [App Name] Limit" — type_heading_2, Ghost White, centered
- Streak display card:
  - Large app icon (64×64px, centered)
  - "[App Name] Streak" label — `color_text_muted`, type_caption
  - Row: "[🔥] [X] days" → "[🌧️] 0 days"
    - Current streak: #FFC107, weight 600
    - Arrow: `color_text_muted`, 24px
    - Result: `color_text_muted`, weight 600
- Countdown button: initially disabled
  - Text: "Wait [X]s to Turn off limit..."
  - Background: rgba(148,163,184,0.2), `color_text_muted` text
  - On countdown complete: background #EF4444, Ghost White text, "Turn Off Limit" label, cursor active
  - Countdown: 3 seconds (fixed, not formula-based — this is app limit, not session)
- Cancel button: `color_surface` background, Ghost White text

### 8.2 Turn Off Duration Sheet
Appears after countdown completes and user taps "Turn Off Limit":

**Content:**
- Title: "How long do you want to turn off [App Name]?" — type_heading_2
- Three radio options:
  - "Rest of the day"
  - "Till tomorrow"
  - "7 days"
- Radio style: 20px circle, #475569 border inactive, Electric Indigo active
- Info bar: "App Reminder will be turned off till [date]" — `color_text_muted`, type_caption, `color_surface` background
- Turn Off button: Ghost White background (consistent with App Limits primary button style)
- Delete Limit button: #EF4444 destructive style

---

## 9. Add App Sheet

### 9.1 Trigger
User taps "+ Add App" in section header.

### 9.2 Sheet Content
- Handle + title: "Select an app to add limit"
- Search box: full width, standard style
- App list grouped by category (same as preset app selection)
- Each app shows: icon + name + "[X] mins avg" usage
- Right side: "+ Add" button — rgba(99,102,241,0.2) background, Electric Indigo text, type_caption, border radius 8px

### 9.3 After Adding
- Sheet closes
- App appears in the main list under its category
- App Settings sheet opens automatically for the newly added app so user can set the time limit immediately

---

## 10. Toast Notifications
- Short confirmation toasts for: "Limit updated!", "Settings saved", "[App] limit deleted", etc.
- Style: `color_surface` background, Ghost White text, 1px `color_border`, border radius 8px
- Position: above bottom navigation bar, centered horizontally
- Duration: 2 seconds, fade in/out

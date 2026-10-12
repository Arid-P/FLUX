# FluxFoxus (FF) — UI: Usage Stats Screen
**Version:** 1.0  
**References:** ui_design_system.md, ui_navigation.md  

---

## 1. Overview
The Usage Stats screen is accessed via the "Usage" tab in the bottom navigation bar. It shows screen time data broken down by category and individual app. Three time granularities are available: Today, Daily (week view), Weekly (multi-week view).

---

## 2. Screen Structure

```
[ Top Bar ]
[ Tab Selector: Today | Daily | Weekly ]
[ Chart (changes per tab) ]
[ 2×2 Category Legend Grid ]
[ App Section (Search + List) ]
```

---

## 3. Top Bar

- Back arrow: left, Ghost White, 24px (navigates back to previous screen or home)
- Title: "Usage Stats" — type_heading_1, Ghost White, centered
- Right: "Help" button — `color_surface` pill, Ghost White text, type_caption

---

## 4. Tab Selector

Three tabs in a horizontal row:
- Labels: "Today" | "Daily" | "Weekly"
- Style: text tabs with underline indicator
- Active tab: Ghost White text, Electric Indigo (#6366F1) underline indicator
- Inactive tabs: `color_text_muted`
- Font: type_body, weight 600 active / weight 500 inactive
- Default tab on open: Today

---

## 5. Today Tab

### 5.1 Chart — Smooth Bézier Area Chart
- Type: stacked area, smooth Bézier curves
- X-axis labels: 6am, 12pm, 6pm, 12am
- Y-axis: no labels (clean)
- Height: ~200px
- Layer order (bottom to top): Productive → Semi-Productive → Distracting → Others
- Colors: fixed category palette (ui_design_system.md §2.2)
- Subtle horizontal reference lines at regular intervals

### 5.2 Legend Grid (2×2)
- Teal dot — Productive — [time]
- Blue dot — Semi-Productive — [time]
- Charcoal dot — Distracting — [time]
- Grey dot — Others — [time]
- Grid: 2 columns × 2 rows separated by 1px `color_border` dividers
- Category name: type_caption, `color_text_muted`
- Time value: type_body, Ghost White, weight 700

---

## 6. Daily Tab

### 6.1 Chart — Stacked Bar Chart (7 days)
- 7 vertical bars, one per day (Mon–Sun)
- X-axis labels: Mon, Tue, Wed, Thu, Fri, Sat, Sun
- Y-axis: hour labels (0h, 2h, 4h, 6h)
- Bar width: equal, with small gaps between bars
- Layer order (bottom to top): Productive → Semi-Productive → Distracting → Others

**Default State:**
- Today's bar: fully colored in category palette
- All other bars: dark greyscale (structure visible but all layers in muted dark tones — not the category colors)

**Selected State (tap any bar):**
- Tapped bar: fully colored in category palette
- All other bars (including today if not selected): greyscale
- Selection persists until another bar is tapped

### 6.2 Legend Grid
- Same 2×2 structure as Today
- Shows totals for the selected/highlighted day

---

## 7. Weekly Tab

### 7.1 Chart — Stacked Bar Chart (multi-week)
- Bars represent weeks: "Week of [Month Day]"
- X-axis labels: "Week of Sep 1", "Week of Sep 8", etc.
- 5 most recent weeks shown
- All bars fully colored in category palette (no greyscale/highlight pattern)
- Layer order: same fixed order

### 7.2 Legend Grid
- Same 2×2 structure
- Shows totals for the entire displayed period

---

## 8. App Section (All Tabs)

### 8.1 Search Bar
- Full-width
- Background: `color_surface` (#1E293B)
- Border: 1px `color_border`
- Border radius: 8px
- Placeholder: "Search apps" in `color_text_muted`
- Search icon left: `color_text_muted`

### 8.2 App List
Scrollable list below search bar. Each app item:

**Layout (single row):**
- Left: App icon (actual system icon, 40×40px, border radius 10px)
- Center-left: App name (type_body, Ghost White, weight 500)
- Center: Category pill — colored per category system (type_micro, weight 600)
- Right: Time used (type_body, Ghost White, weight 600)

**Category pill colors:** per ui_design_system.md §5.4

**Sorting:** By time used (descending) by default

**Tapping an app row:**
- Opens a detail view or bottom sheet with per-app breakdown for the selected time period

### 8.3 Empty State
If no app usage data for the selected period:
- "No data for this period" centered text in `color_text_muted`
- FF aperture icon above text, muted

---

## 9. Weekly Report

### 9.1 Trigger
- Every Sunday, a weekly report notification is pushed to the user
- Tapping the notification opens the Usage Stats screen on the Weekly tab

### 9.2 Report Content (in notification and/or in-app summary card)
- Current streak (days)
- Total time focused (week)
- Comparison vs previous week (delta, +/-)
- Max focus day of the week
- Min focus day of the week
- Summary text (auto-generated: e.g. "You focused 2h more than last week")

### 9.3 In-App Weekly Summary Card
Shown at the top of the Weekly tab on Sundays:
- Background: rgba(99,102,241,0.1), Electric Indigo border left 3px
- Contains all report content listed above
- Dismissible (× button top right)

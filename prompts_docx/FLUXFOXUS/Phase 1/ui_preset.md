# FluxFoxus (FF) — UI: Preset Creation Flow
**Version:** 1.0  
**References:** ui_design_system.md, ui_navigation.md  

---

## 1. Overview
The Preset Creation flow is a full-screen push navigation screen (not a bottom sheet). It is accessed via the "Add Preset" FAB on the Planner screen. A Preset stores all focus session configuration except the time window — it is a reusable template applied to sessions.

---

## 2. Screen: Create / Edit Preset

### 2.1 Header
- Title: "New Preset" (or "Edit Preset" when editing) — type_heading_1, Ghost White, centered
- Right: Close button (×) — `color_surface` background, `color_text_muted` icon, 32×32px, border radius 8px

### 2.2 Scrollable Content
All sections below are inside a scrollable container. Padding: 20px horizontal.

---

## 3. Section: Preset Name

**Section label:** "PRESET NAME" — type_label (uppercase, muted)

**Card (single row):**
- Background: `color_surface`, border 1px `color_border`, border radius 8px
- Left: Emoji icon selector button
  - 40×40px, background rgba(99,102,241,0.1), border radius 8px
  - Shows currently selected emoji (default: ⏳)
  - Tappable: opens emoji picker bottom sheet
- Right: Text input
  - Label above: "Name" — type_caption, `color_text_muted`
  - Input: type_heading_3, Ghost White, transparent background, no border
  - Placeholder: "Enter preset name" in `color_text_muted`, opacity 0.5

---

## 4. Section: Break Configuration

**Section label:** "BREAK CONFIGURATION" — type_label

**2-card grid (side by side):**

**Card 1 — Number of Breaks:**
- Label: "NUMBER OF BREAKS" — type_micro, `color_text_muted`, uppercase
- Control row: [−] button | [value] | [+] button
  - Stepper buttons: 32×32px, rgba(99,102,241,0.2) background, Electric Indigo icon, border radius 6px
  - Value: type_display (24px), Ghost White, weight 700
  - Unit below value: "breaks" — type_micro, `color_text_muted`
- Range: 0–6 (0 = no breaks)
- Disabled (−) at 0, disabled (+) at 6

**Card 2 — Duration Each:**
- Label: "DURATION EACH" — type_micro, `color_text_muted`, uppercase
- Same stepper control
- Value: number + "mins" unit
- Range: 1–15 minutes
- Disabled (−) at 1, disabled (+) at 15

Card style: `color_surface`, border 1px `color_border`, border radius 8px, padding 16px

---

## 5. Section: App Restrictions

**Section label:** "APP RESTRICTIONS" — type_label

Four collapsible category sections stacked vertically. Each category:

### 5.1 Category Header Row (always visible)
- Left: Category dot (8px circle, category color) + category name (type_body, Ghost White, weight 600)
- Right: App count summary text (type_caption, `color_text_muted`) + chevron (▼/▲)
  - Example: "5 apps blocked", "2 apps allowed"
- Background: `color_surface`
- Border: 1px `color_border`
- Border radius: 8px 8px 0 0 (when expanded) / 8px (when collapsed)
- Tappable: toggles expanded/collapsed state

### 5.2 App List (expanded state)
- Appears below category header, connected visually
- Background: `color_surface`
- Border: 1px `color_border`, border-top: none
- Border radius: 0 0 8px 8px

**Each app row:**
- Left: App icon (36×36px, border radius 8px, #334155 background)
- Center: App name — type_body, Ghost White, weight 500
- Right: Toggle switch (44×24px)
  - Toggle ON = app is BLOCKED during this preset
  - Toggle OFF = app is not blocked
  - Active color: Electric Indigo (#6366F1)
  - Inactive: #334155
- Row padding: 12px 16px
- Divider: 1px rgba(148,163,184,0.05) between rows

### 5.3 Default States by Category
- **Distracting apps:** Toggle ON by default (blocked)
- **Productive, Semi-Productive, Others:** Toggle OFF by default (not blocked)
- User can override any individual app

### 5.4 YouTube Special Handling
YouTube appears in its own subsection within Distracting (or as a "Special Apps" group at the top of App Restrictions). Instead of a simple toggle, it has 3 radio options:

- **Block completely** — YouTube is fully blocked (radio button)
- **Allow completely** — YouTube is unrestricted (radio button)
- **Study Mode ([X] channels)** — YouTube Study Mode active (radio button, selected by default)
  - Shows count of whitelisted channels
  - "Configure →" link to the right → navigates to Channel Whitelist screen

**Radio button style:**
- 20px circle, border 2px #475569 inactive / #22D3EE active
- Active center dot: 10px, Soft Cyan (#22D3EE)

---

## 6. Section: YouTube Configuration

**Section label:** "YOUTUBE CONFIGURATION" — type_label

Single card row:
- Background: rgba(34,211,238,0.05), border 1px rgba(34,211,238,0.2)
- Left: "YouTube Study Mode" label (type_body, Ghost White, weight 600) + "Enabled" badge (Soft Cyan background, Soft Cyan text, type_micro)
- Right: "Configure →" in Electric Indigo, type_body
- Sub-label: "[X] channels whitelisted" — type_caption, `color_text_muted`
- Tapping "Configure →" pushes Channel Whitelist screen

---

## 7. Section: Description

**Section label:** "DESCRIPTION" — type_label

Single card with textarea:
- Background: `color_surface`, border 1px `color_border`, border radius 8px
- Textarea: transparent background, Ghost White text, type_body (14px), line-height 1.5
- Min height: 80px, resizable
- Placeholder: "What do you plan on focusing? Add notes..."
- No resize handle visible

---

## 8. Save Button

- Full-width pill at screen bottom (sticky, outside scroll area)
- Container: `color_background`, border-top 1px `color_border`, padding 20px
- Button: Electric Indigo (#6366F1) background, "Save Preset" Ghost White text, type_heading_2, weight 600
- Border radius: 8px
- Width: 100%

---

## 9. Emoji Picker Bottom Sheet

Triggered by tapping the emoji icon button in the Preset Name section.

**Sheet:**
- Handle: 40px wide, 4px tall, #475569, centered
- Title: "Select Icon" — type_heading_2, Ghost White
- Grid: 8 columns, emoji items in ~32px cells
- Each emoji item: rounded square background rgba(148,163,184,0.1), tap to select and close sheet
- Hover/tap state: rgba(99,102,241,0.2) background

---

## 10. App Selection Modal

When user taps a category header to expand, they can also be pushed to a full "Select Apps" screen for more comprehensive selection.

**Header:**
- Title: "Select Apps to Block" — type_heading_1
- Close button (×) — right

**Search bar:** standard pattern (ui_design_system.md §5.6)

**App groups:**
- Grouped by the 4 categories (Productive, Semi-Productive, Distracting, Others)
- Category label row: colored dot + category name (type_label, uppercase)
- Each app row: icon + name + usage average ("56 mins avg" in `color_text_muted`) + toggle

---

## 11. Channel Whitelist Screen

### 11.1 Header
- Back arrow left
- Title: "Study Channels" — type_heading_1, Ghost White, centered
- Sub-label: "[X] channels" — type_caption, `color_text_muted`, below title
- Right: "+" button — Electric Indigo icon, 32×32px

### 11.2 Search Bar
Standard search pattern.

### 11.3 Channel List
Each channel row:
- Left: Channel icon (circular, 40px diameter) or placeholder icon
- Center: Channel name — type_body, Ghost White, weight 500
- Right: Remove button (× icon, Ghost White, 20px)
- Row padding: 14px 16px
- Divider: 1px `color_border` between rows
- Container: `color_surface`, border 1px `color_border`, border radius 8px

### 11.4 Empty State
- FF aperture icon centered, muted, 64px
- "No channels added yet" — type_body, `color_text_muted`
- "Add your first study channel" — type_caption, `color_text_muted`

### 11.5 Add Channel Flow
Triggered by "+" button in header.

**Bottom sheet:**
- Handle + title: "Add Study Channel"
- Text field: channel name or URL input (standard input style)
- Below input: Confirmation sentence card
  - Background: `color_surface`, border 1px Soft Cyan (#22D3EE)
  - Sentence text: type_body, Ghost White
  - Sentence is randomly selected from pool
- Sentence input field below card: same input style
- Submit button: Electric Indigo pill, "Add Channel", disabled until text matches
- Cancel: plain text button, `color_text_muted`, no background

**Sentence pool rules:**
- Total channels ≤ 25: pool of 5 short sentences
- Total channels > 25: pool of ~50 longer sentences
- Never same sentence twice in a row

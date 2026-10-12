# FluxFoxus (FF) — UI Design System
**Version:** 1.0  
**Last Updated:** March 2026  
**Scope:** All UI components, colors, typography, spacing, and interaction patterns for FluxFoxus Android app.

---

## 1. Brand Identity

### 1.1 App Name
- Full name: **FluxFoxus**
- Short code: **FF**
- Family: Flux (alongside FluxDone / FD)

### 1.2 Logo & Icon
- **Full logo:** FF aperture emblem + "FLUXFOXUS" wordmark
- **Wordmark gradient:** "FLUX" in Soft Cyan (#22D3EE) transitioning smoothly into "FOCUS" in Electric Indigo (#6366F1)
- **Emblem:** Symmetrical 2D vector — camera aperture/lens eye in Soft Cyan (#22D3EE) at center, surrounded by 8-segment concentric ring in Electric Indigo (#6366F1), two thin Electric Indigo outer guide rings. Flat 2D, razor-sharp edges, no gradients on emblem itself.
- **In-app usage:** Emblem only (no wordmark). Small white version used in bottom navigation Home tab and top-left header.
- **Icon variants:**
  - `ic_launcher_adaptive.png` — 1024×1024px, emblem only, transparent background
  - `ic_launcher_legacy.png` — 1024×1024px, emblem on Slate Blue (#1E293B) rounded square (corner radius ~220px)

---

## 2. Color Palette

### 2.1 Core UI Palette
| Token | Hex | Usage |
|---|---|---|
| `color_background` | #0F172A | Main app background (Midnight Slate) |
| `color_surface` | #1E293B | Cards, sheets, modals (Slate Blue) |
| `color_primary` | #6366F1 | Primary actions, active states, buttons (Electric Indigo) |
| `color_accent` | #22D3EE | Timer display, break state, highlights (Soft Cyan) |
| `color_text_primary` | #F8FAFC | Headers, titles, primary text (Ghost White) |
| `color_text_muted` | #94A3B8 | Labels, subtitles, secondary text |
| `color_border` | rgba(148,163,184,0.1) | Card borders, dividers |
| `color_surface_elevated` | #1A2333 | Slightly elevated surface, distracting category |

### 2.2 Category Palette
Used for area charts, stacked bar charts, donut rings, category pills, and legend dots.
| Token | Hex | Category | Vibe |
|---|---|---|---|
| `color_productive` | #14B8A6 | Productive | Teal-Aqua |
| `color_semi_productive` | #3F51B5 | Semi-Productive | Medium Blue |
| `color_distracting` | #1A2333 | Distracting | Charcoal Slate |
| `color_others` | #94A3B8 | Others | Neutral Grey |

> **Critical note:** `color_distracting` (#1A2333) is visually very dark. On pill/tag elements rendered on `color_surface` (#1E293B) backgrounds, always add a 1px border using `color_text_muted` (#94A3B8) to ensure visibility.

### 2.3 Semantic Colors
| Token | Hex | Usage |
|---|---|---|
| `color_danger` | #EF4444 | Destructive actions, streak reset warning |
| `color_warning` | #FFC107 | Streak display, caution states |
| `color_success` | #14B8A6 | Completed sessions (same as productive) |

### 2.4 Chart Layer Order
For all stacked area and bar charts, layers render in this fixed order (bottom to top):
1. Productive (#14B8A6) — bottom
2. Semi-Productive (#3F51B5)
3. Distracting (#1A2333)
4. Others (#94A3B8) — top/tip

---

## 3. Typography

### 3.1 Font Family
- **Primary:** Inter (preferred) or Gilroy
- **Fallback:** system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif
- **Style:** Geometric sans-serif. Clean, sharp, modern. No decorative fonts anywhere.

### 3.2 Type Scale
| Role | Size | Weight | Color | Usage |
|---|---|---|---|---|
| `type_display` | 32–48px | 700 | #F8FAFC | Flip clock digits, hero numbers |
| `type_heading_1` | 22px | 600 | #F8FAFC | Screen titles, section headers |
| `type_heading_2` | 18px | 600 | #F8FAFC | Card titles, sheet titles |
| `type_heading_3` | 16px | 600 | #F8FAFC | App names, session titles |
| `type_body` | 15px | 500 | #F8FAFC | Body text, list items |
| `type_label` | 13px | 600 | #94A3B8 | Section labels (uppercase, 0.8px tracking) |
| `type_caption` | 12px | 500 | #94A3B8 | Sub-labels, timestamps, badges |
| `type_micro` | 11px | 500 | #94A3B8 | Pill labels, tiny badges |

### 3.3 Letter Spacing
- Section labels: `letter-spacing: 0.8px` + `text-transform: uppercase`
- Display numbers: `letter-spacing: -0.5px`
- Body: `letter-spacing: -0.2px`

---

## 4. Spacing & Layout

### 4.1 Base Unit
- Base spacing unit: **4px**
- Common multiples: 4, 8, 12, 16, 20, 24, 32, 40, 48

### 4.2 Screen Padding
- Horizontal screen padding: **20px**
- Card internal padding: **16px**
- Bottom navigation height: **72px** (including 24px bottom safe area inset)

### 4.3 Border Radius
| Component | Radius |
|---|---|
| Cards | 8px |
| Modals / Bottom sheets | 16–20px top corners |
| Pills / Buttons | 28px (full pill) |
| Small pills / badges | 4–6px |
| App icons | 10px |
| FAB | 28px |
| Widget | 16px |

### 4.4 Borders
- Default card border: `1px solid rgba(148, 163, 184, 0.1)`
- Active / focused border: `1px solid #6366F1`
- Accent border: `1px solid rgba(34, 211, 238, 0.2)`
- Distracting pill border: `1px solid #94A3B8` (visibility fix)

---

## 5. Component Patterns

### 5.1 Cards
- Background: `color_surface` (#1E293B)
- Border: 1px `color_border`
- Border radius: 8px
- Padding: 16px
- No shadows. Flat surface only.
- Left accent border (3px) used for session cards — color matches category or active state (Electric Indigo for active)

### 5.2 Buttons

#### Primary Button (Full Width Pill)
- Background: Electric Indigo (#6366F1)
- Text: Ghost White (#F8FAFC), 16px, weight 600
- Border radius: 28px
- Padding: 16px vertical
- Width: 100%
- Example: "Start Focusing", "Save Preset", "End Break Early"

#### Exception — App Limits Primary Button
- Background: Ghost White (#F8FAFC)
- Text: Midnight Slate (#0F172A)
- Same geometry as above
- This is intentional — deliberate visual distinction for the App Limits screen

#### Secondary Button (Outline Pill)
- Background: transparent
- Border: 1px solid #22D3EE (Soft Cyan)
- Text: Ghost White (#F8FAFC)
- Example: "Break" button during focus session, "Quit Session" in stop modal

#### Destructive Button
- Background: rgba(239, 68, 68, 0.15)
- Text: #EF4444
- Example: "Delete Limit", "Turn Off Block"

#### Disabled State
- Opacity: 0.3
- cursor: not-allowed
- No interaction

### 5.3 Toggle Switches
- Track inactive: #334155
- Track active: Electric Indigo (#6366F1)
- Thumb: Ghost White (#F8FAFC)
- Dimensions: 48×28px (large), 44×24px (small/in-list)
- Transition: 0.2s

### 5.4 Category Pills / Tags
- Productive: background rgba(20,184,166,0.15), text #14B8A6, border rgba(20,184,166,0.3)
- Semi-Productive: background rgba(63,81,181,0.15), text #6366F1, border rgba(63,81,181,0.3)
- Distracting: background #1A2333, text #94A3B8, border 1px solid #94A3B8
- Others: background rgba(148,163,184,0.15), text #94A3B8, border rgba(148,163,184,0.3)
- Padding: 2–4px vertical, 8–10px horizontal
- Border radius: 4–6px

### 5.5 Bottom Sheets / Modals
- Background: `color_surface` (#1E293B)
- Top corners: 20px border radius
- Drag handle: 40px wide, 4px tall, #475569, centered, margin-bottom 20px
- Overlay backdrop: rgba(0,0,0,0.7)
- Max height: 85vh
- Overflow: scrollable

### 5.6 Search Bars
- Background: `color_surface` (#1E293B)
- Border: 1px `color_border`
- Border radius: 8px
- Padding: 12–14px
- Icon: muted grey search icon left
- Placeholder: #94A3B8

### 5.7 Steppers (Increment/Decrement)
- Button: 32–36px square, border radius 6–10px
- Active background: rgba(99,102,241,0.2)
- Icon color: Electric Indigo (#6366F1)
- Disabled: opacity 0.3

### 5.8 Section Dividers
- 3 subtle horizontal lines used as visual buffers between major sections (as per home screen sketch)
- Color: rgba(148,163,184,0.1)
- Height: 1px

---

## 6. Charts & Data Visualization

### 6.1 Area Chart (Today tab / Home Momentum card)
- Type: Smooth Bézier stacked area chart
- X-axis: 6am, 12pm, 6pm, 12am
- Layer order (bottom to top): Productive → Semi-Productive → Distracting → Others
- Colors: fixed category palette
- No grid lines except subtle horizontal reference lines
- No tooltips on home screen version; full tooltips on Usage Stats

### 6.2 Stacked Bar Chart (Daily tab)
- 7 bars, Mon–Sun
- Default: today's bar fully colored in category palette order; all other bars in dark greyscale (structure visible but muted)
- On tap: tapped bar becomes fully colored, all others including today go greyscale
- Bar labels: day abbreviation below

### 6.3 Stacked Bar Chart (Weekly tab)
- Bars represent weeks (Week of [date])
- All bars fully colored in category palette order
- No highlight/selection interaction

### 6.4 Donut Ring (Widget)
- 4 segments in fixed order: Productive → Semi-Productive → Distracting → Others
- Center text: total time (e.g. "3h 9m"), sub-label "Today's usage"
- Gap between segments: 2px

---

## 7. Icons

### 7.1 Bottom Navigation Icons
| Tab | Icon | Source |
|---|---|---|
| Home | FF Aperture logo (white) | Custom (FF brand) |
| Usage | Clock outline | Image 10 reference |
| Focus | Stylized eye | Image 11 reference |
| Planner | Calendar outline | Image 12 reference |
| Block | Circular geometric symbol | Image 13 reference |

All nav icons: Ghost White (#F8FAFC) inactive, Electric Indigo (#6366F1) active.
Both icon and label change color on active state.

### 7.2 Icon Style
- Line weight: consistent, geometric
- Style: outline (not filled) for navigation
- Size: 24×24px in navigation bar

---

## 8. Motion & Interaction

### 8.1 Transitions
- Bottom sheet open/close: translateY, 0.3s ease-out
- Toggle switch: 0.2s
- Category expand/collapse: max-height transition, 0.3s
- Active state press: scale(0.98), 0.1s

### 8.2 Flip Clock Animation
- Mechanical flip animation on digit change
- Horizontal split line visible on card
- Digits: Ghost White in focus mode, Soft Cyan (#22D3EE) in break mode

### 8.3 Countdown Timer (Stop Modal / App Limit turn-off)
- Visual: text countdown in brackets next to action text e.g. "Quit Session (18)"
- Counts down to 0, then button becomes active
- Disabled state: opacity 0.3, cursor not-allowed
- Active state: full color, cursor pointer

### 8.4 Edge Grabber
- Position: right edge of screen, vertically centered
- Width: ~32px, height: ~80px
- Color: Electric Indigo (#6366F1)
- FF aperture logo embedded (Ghost White, small)
- On tap: expands horizontally to show toggle panel
- Toggle panel: Midnight Slate (#0F172A) background, "YouTube Study Mode" label + Soft Cyan toggle + × close button

---

## 9. Accessibility & Platform Notes

- Minimum touch target: 44×44px
- All interactive elements meet minimum touch target
- Platform: Android Phase 1
- Dark mode only (no light mode in Phase 1)
- Status bar: transparent, dark icons
- Navigation bar: matches app background (#0F172A)
- Font scaling: respect system font scale settings

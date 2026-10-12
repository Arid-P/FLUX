# FluxFoxus (FF) — UI Navigation
**Version:** 1.0  
**References:** ui_design_system.md  

---

## 1. Bottom Navigation Bar

### 1.1 Overview
FF uses a persistent bottom navigation bar present on all top-level screens. It contains 5 tabs. The bar uses a floating pill/container design as shown in the home screen sketch — the 5 tabs are grouped inside a rounded container that sits above the screen bottom edge.

### 1.2 Container
- Background: `color_surface` (#1E293B)
- Border radius: 28px (full pill container)
- Margin: 16px horizontal, 16px from bottom safe area edge
- Height: 72px including internal padding
- Border: 1px solid `color_border`
- Slight elevation effect via border — no drop shadow

### 1.3 Tab Specification

| Position | Label | Icon | Icon Source |
|---|---|---|---|
| 1 (leftmost) | Home | FF Aperture emblem | Custom brand icon |
| 2 | Usage | Clock outline | Reference: clock icon |
| 3 | Focus | Stylized eye | Reference: eye icon |
| 4 | Planner | Calendar outline | Reference: calendar icon |
| 5 (rightmost) | Block | Circular geometric symbol | Reference: block symbol |

### 1.4 Tab States

#### Inactive State
- Icon color: Ghost White (#F8FAFC)
- Label color: Ghost White (#F8FAFC)
- Label size: 12px, weight 500
- Icon size: 24×24px
- Background: transparent

#### Active State
- Icon color: Electric Indigo (#6366F1)
- Label color: Electric Indigo (#6366F1)
- Label size: 12px, weight 600
- Background: transparent (no pill highlight on individual tab)

### 1.5 Tab Groups
Per the hand-drawn sketch, tabs are visually grouped into two pairs plus Home:
- **Left group:** Usage + Focus (share a sub-container / visual grouping)
- **Right group:** Planner + Block (share a sub-container / visual grouping)
- **Home** stands alone on the far left

Each group has a subtle rounded pill background within the main container to distinguish groups visually.

### 1.6 Navigation Behavior
- Tapping a tab navigates to that top-level screen
- No animation between top-level tabs (instant switch)
- Bottom nav is hidden during active focus session (full-screen session view)
- Bottom nav reappears when session ends and user returns to home

---

## 2. Top-Level Screen Map

| Tab | Primary Screen | Secondary Screens (pushed) |
|---|---|---|
| Home | Home dashboard | — |
| Usage | Usage Stats (Today tab default) | — |
| Focus | Focus session start / active session | Preset selection, session history |
| Planner | Planner (today's date default) | Session detail |
| Block | App Limits list | App limit settings sheet, add app sheet |

---

## 3. Navigation Patterns

### 3.1 Push Navigation
Used for: Preset creation flow, Channel whitelist, App selection modal
- Slides in from right
- Back arrow top-left to return

### 3.2 Bottom Sheet
Used for: App limit settings, streak warning, turn-off duration picker, emoji picker, add app sheet
- Slides up from bottom
- Drag handle at top
- Dismissible by dragging down or tapping overlay

### 3.3 Modal Overlay
Used for: Stop Focusing confirmation, Break limit confirmation
- Centered floating card
- Background blurred
- Not dismissible by tapping outside (must interact with buttons)

### 3.4 Full Screen Takeover
Used for: Active focus session screen, Break window
- Hides bottom navigation
- No back gesture available during active session (must use Stop Focusing flow)

---

## 4. Header Patterns

### 4.1 Home Screen Header
- Left: Small FF aperture icon + "FluxFoxus" text (type_heading_1)
- Right: Two pills — "Focused: [value]" and "Weekly: [value]"
- Pill style: `color_surface` background, Ghost White label, Soft Cyan (#22D3EE) value

### 4.2 Standard Screen Header
- Back arrow left (Ghost White, 24px)
- Screen title center (type_heading_1)
- Optional action right (e.g. "Help" button in `color_surface` pill)

### 4.3 Focus Session Header
- Left: Preset name in Electric Indigo pill (Ghost White text)
- Right: Session time range in `color_text_muted`, with "BREAK" in Soft Cyan when break is active

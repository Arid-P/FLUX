# Flux Family — Master Design Tokens & System Theme (v2.0)

**Version:** 2.0  
**Scope:** Global Design System for FluxDone (FD) and FluxFoxus (FF)  
**Target Framework:** Jetpack Compose (Android) & Compose Multiplatform (Desktop)

---

## 1. Architectural Theme Overview

The Flux family maintains two distinct visual personalities unified under modern, distraction-free principles:
1. **FluxFoxus (FF):** Strictly dark, disciplined, and mechanical. Governed authoritatively by the **Rustic Medley** aesthetic. No shadows, flat 2D surfaces, high contrast, tactile mechanical timer elements.
2. **FluxDone (FD):** Academic precision. Clean, structured, token-driven list hierarchy. Governed by the **7 System-Locked Academic Domain Colors** for Olympiad and JEE preparation, with customizable accent palettes for secondary personal lists. Supports both Dark and Light themes from Day 1.

---

## 2. FluxFoxus Design System: "Rustic Medley" (Authoritative Override)

> [!IMPORTANT]
> **Strict Color Lock:** The previous Electric Indigo / Midnight Slate / Soft Cyan color scheme is permanently decommissioned. Under no circumstances should an AI agent generate purple, blue, cyan, or neon accents for FluxFoxus.

### 2.1 Color Tokens Specification

| Token Name | Hex Code | Compose Color Definition | Semantic Role |
| :--- | :--- | :--- | :--- |
| `ff_background` | `#13191F` | `Color(0xFF13191F)` | **River Styx:** Master screen background. Deep, warm slate-black. |
| `ff_surface` | `#2B2F2E` | `Color(0xFF2B2F2E)` | **Carbon Fibre:** Cards, bottom sheets, navigation bar, dialog backdrops. |
| `ff_border` | `#594C3D` | `Color(0xFF594C3D)` | **Afternoon Tea:** 1dp card borders, list dividers, hairline separators. |
| `ff_accent` | `#906D4B` | `Color(0xFF906D4B)` | **Tanned Wood:** Secondary badges, break timers, sub-labels, active chip borders. |
| `ff_primary` | `#CA9C68` | `Color(0xFFCA9C68)` | **Amber Autumn:** Primary action buttons, active countdown digits, flip-clock split line, streak flames. |
| `ff_text_primary` | `#F8FAFC` | `Color(0xFFF8FAFC)` | **Ghost White:** High-contrast text, primary headers, active time digits. |
| `ff_text_muted` | `#94A3B8` | `Color(0xFF94A3B8)` | **Slate Grey:** Secondary labels, disabled action states, timestamps. |
| `ff_error_danger` | `#DC2626` | `Color(0xFFDC2626)` | **Crimson Alert:** Destructive actions, quit confirmation warnings, expired intervention indicators. |

### 2.2 Typography Tokens (Compose `Typography`)
- **Primary Body & Interface:** `Inter` (Sans-serif, clean, optimized for legibility at small sizes).
- **Mechanical Flip Clock & Timers:** `JetBrains Mono` or tabular monospace figures with zero proportional letter-spacing to prevent layout shifting during countdowns.
- **Weights:** Regular (400), Medium (500), SemiBold (600), Bold (700).

### 2.3 Tactile Flip-Clock Design Rules
- Split flap card rendering with a horizontal 1dp hairline divider colored in `ff_primary` (`#CA9C68`).
- Subtle top-half shadow gradient simulated via pure hex shades (no heavy blur rendering).
- Tabular figures (`FontFeatureSettings("tnum")`) to ensure zero digit wobble during millisecond and second ticks.

---

## 3. FluxDone Design System: Academic Precision

### 3.1 The 7 System-Locked Academic Domain Colors
These 7 colors represent Ari's academic life and are system-locked. They mirror the companion Notewise digital notebooks and cannot be edited or reassigned by the user:

| Academic Domain | Hex Code | Compose Token | Semantic Category |
| :--- | :--- | :--- | :--- |
| **Number Theory** | `#2E7D32` | `Color(0xFF2E7D32)` | Math Olympiad (IOQM/INMO/IMO) |
| **Geometry** | `#1565C0` | `Color(0xFF1565C0)` | Math Olympiad (IOQM/INMO/IMO) |
| **Combinatorics** | `#43A047` | `Color(0xFF43A047)` | Math Olympiad (IOQM/INMO/IMO) |
| **Algebra P1** | `#FB8C00` | `Color(0xFFFB8C00)` | Math Olympiad (IOQM/INMO/IMO) |
| **Algebra P2** | `#E64A19` | `Color(0xFFE64A19)` | Math Olympiad (IOQM/INMO/IMO) |
| **Tests & Mocks** | `#E53935` | `Color(0xFFE53935)` | Mock tests & performance tracking |
| **Neev Diamond** | `#576481` | `Color(0xFF576481)` | PW JEE Foundation (Physics/Chem/Math) |

*User-Created Lists (e.g., Programming, School, Books, Extra) select colors freely from a 16-color curated palette or standard hex picker.*

### 3.2 Global Surface & Theme Modes (Light & Dark)

#### Dark Mode (Primary Night Study Aesthetic):
- `fd_dark_background`: `#121214` (Deep Charcoal)
- `fd_dark_surface`: `#1E1E22` (Elevated Charcoal)
- `fd_dark_border`: `#2E2E34` (Hairline Border)
- `fd_dark_text_primary`: `#F4F4F6`
- `fd_dark_text_secondary`: `#8E8E98`

#### Light Mode (Daytime Study Aesthetic):
- `fd_light_background`: `#F8F9FA` (Clean Off-White)
- `fd_light_surface`: `#FFFFFF` (Pure White Card)
- `fd_light_border`: `#E2E8F0` (Soft Slate)
- `fd_light_text_primary`: `#0F172A` (Ink Black)
- `fd_light_text_secondary`: `#64748B` (Slate Grey)

### 3.3 Calendar Timeline Grid Dimension Tokens
The Calendar view is the primary task creation surface. Every slot dimension must be strictly tokenized:
- `TimelineHourHeight`: `64.dp` (Standard 1-hour vertical block).
- `Timeline15MinSlot`: `16.dp` (Minimum snap-to-grid increment for task drag/creation).
- `CurrentTimeIndicatorColor`: `#EF4444` (Vibrant Red line across current hour).
- `GridLineColor`: `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)`.

---

## 4. Jetpack Compose Theme Implementation Blueprint

All Compose UI code must consume colors via `MaterialTheme` or a custom `FluxTheme` object. No hardcoded hex values are permitted in `@Composable` widget code.

### Architectural Token Pattern:
```kotlin
// Example semantic usage in Compose
Surface(
    color = FluxTheme.colors.surface,
    border = BorderStroke(1.dp, FluxTheme.colors.border),
    shape = RoundedCornerShape(12.dp)
) {
    Text(
        text = "Combinatorics Problem Set",
        color = FluxTheme.colors.textPrimary,
        style = FluxTheme.typography.bodyMedium
    )
}
```

This token architecture ensures seamless, instant switching between FD light/dark themes and provides single-line theming updates for FF across both Android and Ubuntu Desktop.

# Flux Family — Master Design Tokens & System Theme (v2.0)

**Version:** 2.0  
**Scope:** Global Design System for FluxDone (FD) and FluxFoxus (FF)  
**Target Framework:** Jetpack Compose (Android) & Compose Multiplatform (Desktop)

---

## 1. Architectural Theme Overview

The Flux family maintains two distinct visual personalities unified under modern, distraction-free principles:
1. **FluxFoxus (FF):** Strictly dark, disciplined, and mechanical. Governed authoritatively by the **Holst (Холст)** aesthetic sourced from `Theme/Холст.jpeg`. No shadows, flat 2D surfaces, high contrast, tactile mechanical timer elements.
2. **FluxDone (FD):** Academic precision. Clean, structured, token-driven list hierarchy. Governed by the **7 System-Locked Academic Domain Colors** for Olympiad and JEE preparation, with customizable accent palettes for secondary personal lists. Harmonizes cleanly with the deep Holst navy/slate dark surfaces. Supports both Dark and Light themes from Day 1.

---

## 2. FluxFoxus Design System: "Holst" (Холст) — Authoritative Override

> [!IMPORTANT]
> **Strict Color Lock (Holst):** Sourced directly from `Theme/Холст.jpeg`. All previous palettes (Rustic Medley, Electric Indigo, Midnight Slate) are permanently superseded. Under no circumstances should an AI agent generate purple, cyan, or oversaturated neon accents for FluxFoxus.

### 2.1 Color Tokens Specification

| Token Name | Hex Code | Compose Color Definition | Semantic Role |
| :--- | :--- | :--- | :--- |
| `ff_background` | `#0D1B2A` | `Color(0xFF0D1B2A)` | **Abyssal Navy:** Master screen background. Deep, tranquil, anti-glare canvas. |
| `ff_surface` | `#1B263B` | `Color(0xFF1B263B)` | **Oxford Slate:** Cards, bottom sheets, navigation bar, dialog backdrops, flip flap tiles. |
| `ff_border` | `#415A77` | `Color(0xFF415A77)` | **Muted Steel Blue:** 1dp card borders, list dividers, hairline separators. |
| `ff_accent` | `#778D7A` | `Color(0xFF778D7A)` | **Muted Sage Green:** Secondary badges, break timers, streak badges, secondary status pills. |
| `ff_primary` | `#D4C4A8` | `Color(0xFFD4C4A8)` | **Warm Sand Khaki:** Primary action buttons ("Start Focusing"), active countdown digits, flip-clock split line, active tab pills. |
| `ff_text_primary` | `#F4F1DE` | `Color(0xFFF4F1DE)` | **Eggshell Cream:** High-contrast text, primary headers, active time digits without harsh glare. |
| `ff_text_muted` | `#8E9CAE` | `Color(0xFF8E9CAE)` | **Slate Blue-Grey:** Secondary labels, disabled action states, timestamps. |
| `ff_error_danger` | `#DC2626` | `Color(0xFFDC2626)` | **Crimson Alert:** Destructive actions, quit confirmation warnings, expired intervention indicators. |

### 2.2 Typography Tokens (Compose `Typography`)
- **Primary Body & Interface:** `Inter` (Sans-serif, clean, optimized for legibility at small sizes).
- **Mechanical Flip Clock & Timers:** `JetBrains Mono` or tabular monospace figures with zero proportional letter-spacing to prevent layout shifting during countdowns.
- **Weights:** Regular (400), Medium (500), SemiBold (600), Bold (700).

### 2.3 Tactile Flip-Clock Design Rules
- Split flap card rendering with a horizontal 1.5dp hairline divider colored in `ff_primary` (`#D4C4A8`).
- Top flap card and bottom flap card rendered in `ff_surface` (`#1B263B`).
- Numerals rendered in tabular `ff_text_primary` (`#F4F1DE`).
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

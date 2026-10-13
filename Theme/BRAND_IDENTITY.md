# 🎨 Flux App Family — Official Brand Identity & Icon Specification

This document establishes the official, locked brand identity, launcher icons, and visual language for **FluxFoxus (FF)** and **FluxDone (FD)**.

---

## 1. FluxFoxus (FF) — Official Identity (Approved: Option 1)

* **Launcher Icon Asset:** [`Theme/logos/ff_icon_primary.jpg`](file:///home/hp/Flux_Prompts/Theme/logos/ff_icon_primary.jpg)
* **Design Concept:** Razor-sharp stylized Origami Fox face formed by continuous geometric brass/gold lines, seamlessly merging an hourglass sand timer in the muzzle and an optical focus aperture/target in the forehead.
* **Symbolism:** Relentless discipline, temporal vigilance, predator-focus on deep work, zero bypasses.

### Core Chromatic Architecture (Rustic Medley Override)
| Token | Role | Hex Value | Color Name |
| :--- | :--- | :--- | :--- |
| `color_background` | Base Canvas / Squircle Background | `#13191F` | River Styx |
| `color_surface` | Card Surfaces & Dialogs | `#2B2F2E` | Carbon Fibre |
| `color_border` | Hairline Specular Borders | `#594C3D` | Afternoon Tea |
| `color_accent` | Break Timers & Sub-Labels | `#906D4B` | Tanned Wood |
| `color_primary` | Primary CTAs & Glowing Line Accents | `#CA9C68` / `#FFB703` | Amber Autumn / Solar Gold |
| `color_text_primary` | High Contrast Headers | `#F8FAFC` | Ghost White |

---

## 2. FluxDone (FD) — Official Identity (Approved: Option 1)

* **Launcher Icon Asset:** [`Theme/logos/fd_icon_primary.jpg`](file:///home/hp/Flux_Prompts/Theme/logos/fd_icon_primary.jpg)
* **Design Concept:** 3D isometric monogram letter 'D' intertwining with a victorious checkmark via an infinite mobius ribbon loop, rendered in luminous electric cyan to emerald mint gradients on a deep obsidian slate squircle.
* **Symbolism:** Kinetic velocity, seamless momentum ("Flux"), and definitive completion ("Done").

### Core Chromatic Architecture (Kinetic Obsidian)
| Token | Role | Hex Value | Color Name |
| :--- | :--- | :--- | :--- |
| `color_background` | Deep Canvas | `#0B0F14` | Obsidian Core |
| `color_surface` | Elevated Cards & Timeline Panels | `#1C2025` | Slate Surface |
| `color_border` | Translucent Specular Rim | `rgba(255, 255, 255, 0.08)` | Specular Glass |
| `color_primary` | Primary Check Action & Glow | `#00F5D4` | Kinetic Neon Cyan |
| `color_secondary` | Velocity Progress Trajectory | `#10B981` | Emerald Ribbon |
| `color_text_primary` | High Contrast Readouts | `#F8FAFC` | Ghost White |

---

## 3. Tooling & Cloud Design System

* **Google Stitch MCP Project:** `6251538759701554507`
* **Google Stitch Design System:** `Kinetic Obsidian` (`assets/d7e79850c9554b4eb98bae0f8008f939`)

---

## 4. Platform Asset Sizing Requirements

When generating platform-specific build binaries (`./gradlew assembleDebug`):

1. **Android Adaptive Icons (`com.fluxdone` & `com.fluxfoxus`):**
   - Foreground layer: 108dp x 108dp (safe zone 72dp circle)
   - Background layer: 108dp x 108dp solid `#0B0F14` (FD) / `#13191F` (FF)
   - High-res store banner: 512px x 512px PNG (32-bit ARGB)

2. **Ubuntu Linux Desktop Icons (`com.fluxdone.desktop` & `com.fluxfoxus.daemon`):**
   - Hicolor icon sizes: `16x16`, `32x32`, `48x48`, `64x64`, `128x128`, `256x256`, `512x512`
   - Location: `~/.local/share/icons/hicolor/<size>/apps/fluxdone.png`

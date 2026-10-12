# FF Phase 4 — Feature 2: Pre-Open Friction

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Pre-Open Friction intercepts the user before a distracting app opens with a mandatory pause screen. The pause breaks the autopilot habit loop — by the time the countdown ends, the impulse to open the app often passes. This operates outside focus sessions and sits before the existing 2/5/10/20 intervention window in the flow.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Accessibility Service — app detection | FF_TRD_v1_0.md §1.6, §5 |
| 2/5/10/20 Intervention Window | FF_PRD_v1_0.md §3.7.7 |
| App categories | FF_PRD_v1_0.md §3.6.6 |
| SYSTEM_ALERT_WINDOW overlay | FF_TRD_v1_0.md §1.6 |
| App Limits — enforcement | FF_PRD_v1_0.md §3.6.4 |

---

## 3. Feature Specification

### 3.1 Trigger Conditions

Pre-Open Friction appears when ALL of the following are true:
- Pre-Open Friction toggle is ON (global)
- The opened app is in the Distracting category
- Per-app friction is not disabled for this specific app
- No active focus session is running (during sessions, preset app blocking handles enforcement — friction does not stack)

### 3.2 Friction Screen

Full-screen overlay using `SYSTEM_ALERT_WINDOW` (same permission as 2/5/10/20 window).

**Layout:**
```
─────────────────────────────────
  [App icon — 64dp]
  [App name]
  
  
       [Slow expanding circle
        animation — calm visual]
  
  
  Opening in [N]s
  
  
  [Go Back]      (appears after countdown)
  [Open App]     (appears after countdown)
─────────────────────────────────
```

**Visual details:**
- Background: FF background color (`#0F172A`) at 95% opacity — app behind is barely visible
- Expanding circle: Electric Indigo (`#6366F1`) at 20% opacity, expands from center outward over the full countdown duration
- App icon: rounded square, 64dp
- App name: 18sp, medium weight, `#F8FAFC`
- Countdown label: *"Opening in [N]s"* — updates every second
- Both buttons hidden until countdown reaches 0

**On countdown complete:**
- **"Open App"** (Electric Indigo, primary): closes overlay → app opens → 2/5/10/20 window appears if applicable
- **"Go Back"** (secondary, outlined): closes overlay → returns to previous app/home screen. App does not open

### 3.3 Countdown Duration

User-configurable in Settings:
- Options: **3s / 5s / 10s**
- Default: **5s**
- Global setting — applies to all apps under friction

### 3.4 Relationship to 2/5/10/20 Window

Friction and 2/5/10/20 are **sequential**, not alternatives:

```
User opens distracting app
        ↓
Pre-Open Friction screen (countdown)
        ↓ (if "Open App" tapped)
App opens briefly
        ↓
2/5/10/20 Intervention Window (if applicable)
        ↓ (user selects time window)
App fully accessible for selected duration
```

If the app has no active limit and no 2/5/10/20 applies: friction screen is the only gate. App opens freely after countdown.

---

## 4. Configuration

### 4.1 Global Settings

**Location:** Settings → App Limits → Pre-Open Friction

| Control | Type | Default |
|---|---|---|
| Pre-Open Friction | Toggle | OFF |
| Countdown duration | Chip selector (3s / 5s / 10s) | 5s |

### 4.2 Per-App Exclusion

**Location:** App Limits screen → tap any app → app detail → "Pre-Open Friction" toggle

- Default: ON for all Distracting apps when global toggle is ON
- User can exclude specific apps (e.g. they want friction for Instagram but not YouTube)
- Exclusion stored in Hive per package name

---

## 5. Storage (Hive)

| Key | Type | Description |
|---|---|---|
| `friction_enabled` | bool | Global toggle |
| `friction_countdown_seconds` | int | 3 / 5 / 10 |
| `friction_excluded_apps` | List\<String\> | Package names excluded from friction |

---

## 6. Implementation Notes

### 6.1 Detection

Uses existing `FluxFoxusAccessibilityService` (`TYPE_WINDOW_STATE_CHANGED` events — TRD §5). When a Distracting app comes to foreground and friction conditions are met, the overlay is shown immediately before the app renders to the user.

### 6.2 Timing

The overlay must appear fast enough that the app content is not visible behind it. The `SYSTEM_ALERT_WINDOW` overlay is drawn on top of everything — the app may briefly render underneath at very low opacity (95% background opacity handles this visually).

### 6.3 "Go Back" Implementation

Tapping "Go Back" calls `ActivityManager` to move the previous task back to foreground, effectively cancelling the app open. Same mechanism used by app lockers on Android.

---

## 7. Module Boundary

**New module:** `friction/`

```
features/
└── friction/
    ├── data/
    │   └── friction_settings_repository.dart
    ├── domain/
    │   └── use_cases/
    │       └── should_show_friction.dart
    └── presentation/
        ├── friction_overlay.dart              ← SYSTEM_ALERT_WINDOW overlay
        └── friction_settings_section.dart
```

Modifications to existing modules:
- `app_limits/presentation/` — per-app friction toggle in app detail
- `core/accessibility/` — friction trigger hook in accessibility service event handler

# FD Phase 3 — Feature 1: JSON Custom Theming

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

JSON Custom Theming allows the user to fully customize FluxDone's visual appearance by editing a structured JSON file. The system builds on the token-based ThemeTokens architecture required from Phase 1 (TRD v2 §2.3) — no widget rewrites are needed. Phase 3 adds only the UI layer on top of the already-existing token infrastructure.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Token-based theming requirement | TRD v2 §2.3 |
| ThemeTokens class | `core/theme/theme_tokens.dart` |
| AppTheme construction | `core/theme/app_theme.dart` |
| Theme toggle (light/dark) | PRD v2 §5, Settings feature |
| Settings screen structure | `ui_SCR-09_settings.md` |

---

## 3. Scope

A custom theme can override the following categories:

### 3.1 ThemeTokens (UI Colors)
All color tokens defined in `ThemeTokens` — surfaces, backgrounds, text colors, primary, accent, dividers, icons, navigation colors, card colors, chip colors, input field colors, bottom sheet colors.

### 3.2 List Colors
Per-list color overrides. A theme can specify a color for any list by list name. List names are matched case-insensitively. Lists not specified in the theme retain their user-defined hex color.

### 3.3 Typography
- Font sizes: title, subtitle, body, caption, section header, metadata, bottom nav label
- Font weights: per text style (regular / medium / semibold / bold)
- Note: font family is NOT customizable in Phase 3. Roboto remains the base font. Custom font family support is a potential Phase 4 addition.

### 3.4 Explicitly Out of Scope
- Font family (Phase 4 consideration)
- Icon shapes or sizes
- Animation durations
- Widget (home screen widget) theming — widgets use their own token subset
- FluxFoxus theming (separate app)

---

## 4. JSON Schema

### 4.1 File Format

```json
{
  "meta": {
    "name": "My Custom Theme",
    "version": "1.0",
    "base": "light",
    "author": "Ari",
    "created_at": "2026-03-21"
  },
  "tokens": {
    "primary": "#6366F1",
    "onPrimary": "#FFFFFF",
    "surface": "#FFFFFF",
    "onSurface": "#1C1B1F",
    "background": "#F8F9FA",
    "onBackground": "#1C1B1F",
    "secondary": "#757575",
    "onSecondary": "#FFFFFF",
    "error": "#E53935",
    "onError": "#FFFFFF",
    "divider": "#E0E0E0",
    "cardSurface": "#FFFFFF",
    "bottomNavBackground": "#FFFFFF",
    "bottomNavSelected": "#6366F1",
    "bottomNavUnselected": "#757575",
    "drawerBackground": "#FFFFFF",
    "chipBackground": "#F0F0F0",
    "chipOnBackground": "#424242"
  },
  "typography": {
    "taskTitle": { "size": 16, "weight": "medium" },
    "taskSubtitle": { "size": 13, "weight": "regular" },
    "sectionHeader": { "size": 12, "weight": "semibold" },
    "metadata": { "size": 12, "weight": "regular" },
    "bodyText": { "size": 14, "weight": "regular" },
    "caption": { "size": 11, "weight": "regular" },
    "bottomNavLabel": { "size": 12, "weight": "medium" }
  },
  "listColors": {
    "Programming": "#5E35B1",
    "School": "#00838F",
    "Doubts": "#F57F17"
  }
}
```

### 4.2 Schema Rules
- All color values: 6-character hex strings with `#` prefix
- Font sizes: integer, range 8–32
- Font weights: one of `"regular"` / `"medium"` / `"semibold"` / `"bold"`
- `meta.base`: `"light"` or `"dark"` — defines which built-in theme this overrides
- All fields are optional except `meta.name` and `meta.base` — unspecified tokens fall back to the base theme values
- Unknown keys are ignored silently (forward compatibility)

### 4.3 Validation Rules
On import or save, FD validates:
- `meta.name` present and non-empty
- `meta.base` is `"light"` or `"dark"`
- All color values match `#[0-9A-Fa-f]{6}` pattern
- All font sizes within range 8–32
- All font weights are valid enum values
- File size ≤ 3 MB (total storage cap across all saved themes)

Validation errors are shown inline with specific field-level messages. Invalid themes cannot be saved or applied.

---

## 5. Theme Management

### 5.1 Saved Themes Screen

**Route:** `/settings/themes`  
**Access:** Settings → Appearance → Themes

**Layout:**
- List of all saved custom themes (name, base, author if set)
- Active theme has a checkmark indicator
- Built-in themes (Light, Dark) always shown at top, non-deletable
- FAB: "New Theme"
- Long-press on custom theme: context menu → Edit / Export / Delete
- Tap on theme: applies it immediately (app restarts)

### 5.2 Storage Cap
- Total storage for all saved custom themes: **3 MB hard cap**
- Individual theme file size is typically 2–5 KB — cap supports hundreds of themes in practice
- If cap is reached: user must delete an existing theme before saving a new one
- Cap is enforced at save time with an inline error if exceeded

---

## 6. Theme Creation Flow

### 6.1 Entry Point
Settings → Appearance → Themes → FAB ("New Theme")

### 6.2 Step 1 — Base Selection
Bottom sheet:
- "Start from Light theme"
- "Start from Dark theme"

Selection pre-populates all token values from the chosen built-in theme.

### 6.3 Step 2 — In-App Editor

Full-screen editor screen. Three tabs:

**Tab 1: Colors**
- Grouped list of all ThemeToken keys
- Each row: token name (human-readable label) + current color preview circle (24dp) + hex value
- Tap row: opens color picker (same hex picker component as list colors — PRD v2 §8.3)
- Live preview panel at top: small mockup showing primary UI elements with current token values applied

**Tab 2: Typography**
- Each text style as a row: label + size stepper + weight selector (segmented control)
- Live preview: sample task card below the controls updating in real time

**Tab 3: List Colors**
- Search field at top
- All user's lists shown with current color
- Tap list row: opens hex color picker
- "Reset to default" option per list

**Save button:** top app bar. Validates before saving. On success: navigates back to Themes list, new theme shown as active.

### 6.4 Theme Naming
On first save: AlertDialog prompts for theme name (required, max 64 chars).

---

## 7. Download / Upload Flow

### 7.1 Export (Download)
- Access: Themes list → long-press theme → "Export"
- FD writes the theme JSON to device Downloads folder as `[theme-name].json`
- Android share sheet is also offered — user can share directly to Files, Drive, etc.

### 7.2 Import (Upload)
- Access: Themes list → overflow menu → "Import from file"
- Opens Android file picker filtered to `.json` files
- FD reads the file, validates it (§4.3), and if valid: adds to saved themes list
- If invalid: error bottom sheet with specific validation failure reasons
- Duplicate name handling: if a theme with the same name already exists, user is prompted to rename or overwrite

---

## 8. Applying a Theme

- Tap any theme in the Themes list → confirmation snackbar: *"Applying [theme name]... App will restart."*
- App calls `SystemNavigator.pop()` equivalent — full Flutter restart
- On relaunch: `ThemeTokens` loads values from the active custom theme JSON stored in `shared_preferences`
- Fallback: if stored theme JSON is invalid on load, FD silently falls back to the built-in Light or Dark theme (based on last `meta.base` value)

### 8.1 Theme Loading at App Start

```dart
// In app.dart, before MaterialApp construction
Future<ThemeTokens> loadActiveTheme() async {
  final prefs = await SharedPreferences.getInstance();
  final activeThemeJson = prefs.getString('active_custom_theme');
  
  if (activeThemeJson == null) {
    return ThemeTokens.light(); // or .dark() based on system/user pref
  }
  
  try {
    final parsed = CustomTheme.fromJson(jsonDecode(activeThemeJson));
    return ThemeTokens.fromCustomTheme(parsed);
  } catch (_) {
    return ThemeTokens.light(); // Silent fallback
  }
}
```

---

## 9. Settings Integration

New sub-section added to Settings → Appearance:

| Control | Type | Behaviour |
|---|---|---|
| Active theme | Display row | Shows current theme name + "Change" link → `/settings/themes` |
| Themes | Navigation row | Opens `/settings/themes` |

The existing light/dark toggle (P1) becomes part of the Themes screen in P3 — the active theme implicitly sets light or dark via `meta.base`.

---

## 10. Data & Storage

| Store | Key | Value |
|---|---|---|
| `shared_preferences` | `active_custom_theme` | JSON string of active theme, or null for built-in |
| `shared_preferences` | `saved_themes_index` | JSON array of saved theme metadata (name, base, file path) |
| Device filesystem | `files/themes/[uuid].json` | Individual saved theme files via `path_provider` |

No SQLite tables needed — theme data is config, not relational data.

---

## 11. Module Boundary

**Owned by:** `settings/` module (extended from P1)

```
features/
└── settings/
    ├── data/
    │   └── theme_repository_impl.dart      ← EXTENDED (custom theme load/save)
    ├── domain/
    │   ├── custom_theme.dart               ← NEW model
    │   └── use_cases/
    │       ├── import_theme.dart           ← NEW
    │       ├── export_theme.dart           ← NEW
    │       └── apply_theme.dart            ← NEW
    └── presentation/
        ├── themes_screen.dart              ← NEW
        ├── theme_editor_screen.dart        ← NEW
        └── theme_import_sheet.dart         ← NEW

core/
└── theme/
    ├── theme_tokens.dart                   ← EXTENDED (fromCustomTheme factory)
    └── custom_theme.dart                   ← NEW (JSON model + validator)
```

# FD Phase 3 — Widget Amendment 1: ThemeTokens Additions

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  
**Amends:** TRD v2 — `core/theme/theme_tokens.dart`

---

## 1. Overview

WIDGET-01 (Task View) and WIDGET-02 (3-Day Calendar) introduce 15 new color tokens that do not exist in the Phase 1 ThemeTokens class. These tokens must be added to the `ThemeTokens` class before widget implementation begins in Phase 3.

These are additive changes only. No existing P1 or P2 tokens are modified.

---

## 2. New Tokens

### 2.1 From WIDGET-01 (Task View Widget)

| Token | Usage | Light Mode Value | Dark Mode Value |
|---|---|---|---|
| `widgetSurface` | Widget background color | `#FFFFFF` | `#1E1E2E` |
| `widgetOnSurface` | Primary text — header title, task titles, icons | `#1C1B1F` | `#F8FAFC` |
| `widgetOnSurfaceSecondary` | Task subtitle text, list name label | `#757575` | `#9E9E9E` |
| `widgetOnSurfaceMuted` | Section labels (OVERDUE / TODAY / TOMORROW), empty state text | `#BDBDBD` | `#616161` |
| `widgetCheckboxStroke` | Checkbox border (unchecked state) | `#FB8C00` | `#FB8C00` |
| `widgetTimeOverdue` | Time metadata color — overdue tasks | `#E53935` | `#EF5350` |
| `widgetTimeToday` | Time metadata color — today tasks | `#FB8C00` | `#FFA726` |
| `widgetTimeTomorrow` | Time metadata color — tomorrow tasks | `#757575` | `#9E9E9E` |
| `widgetIconPrimary` | Header icons (+ and ⋮) | `#1C1B1F` | `#F8FAFC` |

### 2.2 From WIDGET-02 (3-Day Calendar Widget) — Additional Tokens

| Token | Usage | Light Mode Value | Dark Mode Value |
|---|---|---|---|
| `widgetDivider` | Column separators, header bottom border | `#E0E0E0` | `#37474F` |
| `widgetGridLine` | Horizontal hour lines in time grid | `#F5F5F5` | `#263238` |
| `widgetCurrentTimeLine` | Current time indicator line and dot | `#6366F1` | `#6366F1` |
| `widgetAllDayEventSurface` | All-day event block background | `#F0F0F0` | `#2C2C3E` |
| `widgetAllDayEventOnSurface` | All-day event block text | `#424242` | `#E0E0E0` |
| `widgetOnTaskBlock` | Text inside timed task blocks (on colored background) | `#FFFFFF` | `#FFFFFF` |

---

## 3. Implementation

Add all 15 tokens to `ThemeTokens` class in `core/theme/theme_tokens.dart`:

```dart
// Widget tokens — added in Phase 3
// These are used exclusively by home screen widget RemoteViews rendering
// and the home_widget package bridge.

final Color widgetSurface;
final Color widgetOnSurface;
final Color widgetOnSurfaceSecondary;
final Color widgetOnSurfaceMuted;
final Color widgetCheckboxStroke;
final Color widgetTimeOverdue;
final Color widgetTimeToday;
final Color widgetTimeTomorrow;
final Color widgetIconPrimary;
final Color widgetDivider;
final Color widgetGridLine;
final Color widgetCurrentTimeLine;
final Color widgetAllDayEventSurface;
final Color widgetAllDayEventOnSurface;
final Color widgetOnTaskBlock;
```

Populate values in both `ThemeTokens.light()` and `ThemeTokens.dark()` factories using the values in §2 above.

---

## 4. JSON Custom Theming Integration

All 15 widget tokens are included in the JSON custom theming schema (FD P3 F1). They appear under the `tokens` key using the exact camelCase names listed above.

Example:
```json
{
  "tokens": {
    "widgetSurface": "#FFFFFF",
    "widgetCurrentTimeLine": "#6366F1"
  }
}
```

Users can override widget token colors in custom themes. If not specified, widget tokens fall back to base theme values.

---

## 5. Notes

- Widget tokens are prefixed with `widget` to distinguish them from in-app tokens and prevent naming collisions
- `widgetCurrentTimeLine` defaults to the app primary color (`#6366F1`) in both light and dark modes — consistent with the in-app calendar current time indicator (SCR-04)
- `widgetCheckboxStroke` defaults to orange (`#FB8C00`) in both modes — matches the Algebra P1 list color used as the default checkbox color in WIDGET-01
- `widgetOnTaskBlock` is always white in both modes — task block backgrounds are always colored (user list colors), white text ensures contrast

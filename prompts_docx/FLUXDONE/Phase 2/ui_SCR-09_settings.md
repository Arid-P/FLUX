# ui_SCR-09_settings.md
## FluxDone — Settings Screen
**Screen ID:** SCR-09
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-09 is the full-screen app-level settings surface for FluxDone. It provides access to appearance customization, Google account and service connections, notification configuration, and app information. The root Settings screen is a grouped scrollable list. Several rows navigate to dedicated sub-screens. Settings is accessed from the bottom navigation bar (portrait) or left navigation rail (landscape) via the Settings icon.

---

## 2. Navigation & Entry Points

- **Primary entry:** Settings icon in the bottom navigation bar (portrait) or left navigation rail (landscape)
- **Secondary entry:** Settings shortcut at the bottom of the side drawer (SCR-08)
- **Exit:** Android system back gesture or back arrow (`←`) in the top app bar
- **Sub-screen exit:** Same — back arrow or back gesture returns to root Settings screen
- **Transition in:** Standard Material horizontal slide-in from right (`go_router` push)
- **Transition out:** Horizontal slide-out to right on back

---

## 3. Overall Layout Architecture

### 3.1 Portrait Layout

```
┌─────────────────────────────────────────────┐
│ Status Bar (System)                         │
├─────────────────────────────────────────────┤
│ Top App Bar (56dp)                          │
│ [←] Settings                               │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ APPEARANCE                          │   │
│  │ Theme                               │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ ACCOUNT                             │   │
│  │ Google                              │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ CALENDAR                            │   │
│  │ Google Calendar                     │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ NOTIFICATIONS                       │   │
│  │ Notifications                       │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ ABOUT                               │   │
│  │ Version                             │   │
│  │ Credits                             │   │
│  └─────────────────────────────────────┘   │
│                                             │
├─────────────────────────────────────────────┤
│ Bottom Navigation Bar (56dp)                │
└─────────────────────────────────────────────┘
```

### 3.2 Landscape Layout

```
┌──────┬──────────────────────────────────────┐
│ Left │ Top App Bar                          │
│ Nav  ├──────────────────────────────────────┤
│ Rail │ Scrollable Settings List             │
│      │                                      │
│      │                                      │
└──────┴──────────────────────────────────────┘
```

---

## 4. Top App Bar

**Component:** `AppBar`
**Height:** 56dp
**Background color:** Surface color (theme-dependent)
**Elevation:** 0dp (flat, bottom divider 1dp)

### 4.1 Elements

| Position | Component | Details |
|---|---|---|
| Left | Back arrow `IconButton` | `Icons.arrow_back`, 24dp, navigates back on tap |
| Left-center | Screen title `Text` | "Settings", 20sp, medium weight |
| Right | Empty | No overflow menu on root Settings screen |

### 4.2 Sub-screen Top App Bar

Identical to root Settings app bar except:
- Title changes to reflect the sub-screen name (e.g., "Theme", "Google", "Notifications")
- Back arrow always returns to the parent screen (root Settings or the sub-screen that opened it)

---

## 5. Settings List Layout

**Component:** `ListView` (non-lazy — settings list is short enough)
**Scroll axis:** Vertical
**Physics:** `BouncingScrollPhysics`
**Background:** App background color (theme-dependent — off-white in light mode, dark background in dark mode)
**Padding:** 16dp top, 16dp bottom, 0dp horizontal (sections handle their own horizontal padding)

---

## 6. Section Group Component

Each section is a visually grouped card containing one or more rows.

**Component:** `Card` wrapping a `Column` of `ListTile` rows
**Background:** Surface color (white in light mode, dark surface in dark mode)
**Corner radius:** 12dp
**Elevation:** 0dp (flat — no shadow)
**Margin:** 16dp left, 16dp right, 0dp top, 12dp bottom (gap between sections)
**Internal dividers:** 1dp `Divider` between rows within the same section, color: `#E0E0E0` (light) / `#2C2C2C` (dark)

### 6.1 Section Header

**Component:** `Padding` + `Text` rendered above each section card
**Height:** 32dp
**Padding:** 16dp left, 8dp bottom
**Typography:** 12sp, medium weight (500), all caps, secondary text color (`#757575` light / `#9E9E9E` dark)
**Not tappable**

---

## 7. Settings Row Component

**Base component:** `ListTile`
**Row height:** 56dp (single-line), 72dp (two-line with sublabel)
**Horizontal padding:** 16dp left, 16dp right
**Vertical padding:** 8dp top, 8dp bottom

### 7.1 Row Anatomy

```
[Leading icon]  [Label]          [Trailing control / chevron]
                [Sublabel]
```

| Element | Typography | Color |
|---|---|---|
| Label | 16sp, regular (400) | Primary text (`#212121` light / `#E0E0E0` dark) |
| Sublabel | 13sp, regular (400) | Secondary text (`#757575` light / `#9E9E9E` dark) |
| Leading icon | 24dp | Secondary text color |
| Chevron icon | 24dp, `Icons.chevron_right` | `#9E9E9E` |
| Trailing value text | 14sp, regular | Secondary text color |

### 7.2 Row Interaction States

| State | Visual |
|---|---|
| Default | As above |
| Pressed | `InkWell` ripple, color: primary color at 12% opacity |
| Disabled | All text and icons at 40% opacity, no ripple |

### 7.3 Row Types Used in SCR-09

| Type | Trailing | Behavior |
|---|---|---|
| Navigation row | Chevron `›` | Tap → navigates to sub-screen |
| Switch row | `Switch` widget | Tap row or switch → toggles value |
| Static info row | None | Non-tappable, displays information only |

---

## 8. Root Settings Screen — Full Row Map

### 8.1 Section: APPEARANCE

| Row | Type | Leading icon | Label | Sublabel | Trailing |
|---|---|---|---|---|---|
| Theme | Navigation | `Icons.palette_outlined` | "Theme" | Current value: "Light" or "Dark" | Chevron |

### 8.2 Section: ACCOUNT

| Row | Type | Leading icon | Label | Sublabel | Trailing |
|---|---|---|---|---|---|
| Google | Navigation | Google logo (asset) | "Google" | Signed-in email if connected, "Not connected" if not | Chevron |

### 8.3 Section: CALENDAR

| Row | Type | Leading icon | Label | Sublabel | Trailing |
|---|---|---|---|---|---|
| Google Calendar | Navigation | `Icons.calendar_today_outlined` | "Google Calendar" | "Connected" or "Not connected" | Chevron |

### 8.4 Section: NOTIFICATIONS

| Row | Type | Leading icon | Label | Sublabel | Trailing |
|---|---|---|---|---|---|
| Notifications | Navigation | `Icons.notifications_outlined` | "Notifications" | "On" or "Off" based on master switch state | Chevron |

### 8.5 Section: ABOUT

**No section card** — About rows render as static info rows with no card background, no dividers, and no tap interaction.

| Row | Label | Value | Notes |
|---|---|---|---|
| Version | "Version" | e.g., "0.1.0" | Right-aligned, secondary text color |
| Credits | "Made by Ari" | — | Centered, secondary text color, 14sp |

**About section layout:**
- 32dp top padding above version row
- Version row: label left-aligned, value right-aligned, 56dp height
- Credits row: full-width centered text, 48dp height
- 24dp bottom padding below credits

---

## 9. Sub-screen: Theme

**Route:** `/settings/theme`
**Title:** "Theme"
**Purpose:** User selects between Light and Dark app theme.

### 9.1 Layout

Single section card with 2 radio rows. No section header.

### 9.2 Rows

| Row | Leading | Label | Trailing |
|---|---|---|---|
| Light | `Icons.light_mode_outlined` | "Light" | `Radio` button |
| Dark | `Icons.dark_mode_outlined` | "Dark" | `Radio` button |

### 9.3 Radio Row Specs

**Component:** `RadioListTile<ThemeMode>`
**Row height:** 56dp
**Radio button size:** 20dp
**Radio selected color:** App primary color
**Radio unselected color:** `#9E9E9E`

### 9.4 Behavior

- Tapping either row immediately applies the selected theme app-wide
- Theme change animates with a 300ms fade transition across the entire app
- Selection persists to `shared_preferences` key: `app_theme` (values: `light`, `dark`)
- On next launch, the persisted theme is applied before first frame renders (no flash)

---

## 10. Sub-screen: Google

**Route:** `/settings/google`
**Title:** "Google"
**Purpose:** Central hub for all Google service connections — account, Calendar, and Drive backup.

### 10.1 Layout

Three sections, each a separate card.

### 10.2 Section: GOOGLE ACCOUNT

**State A — Not signed in:**

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Sign in with Google | Navigation | "Sign in with Google" | "Connect your Google account" | Chevron |

**State B — Signed in:**

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Account | Static info | User's full name | User's email address | Google avatar (32dp circle) |
| Sign out | Navigation | "Sign out" | — | Chevron |

Sign out tap → shows confirmation `AlertDialog`:
- Title: "Sign out?"
- Body: "This will disconnect Google Calendar and Google Drive backup."
- Actions: "Cancel" (dismisses), "Sign out" (signs out, clears all Google tokens, returns to State A)

### 10.3 Section: GOOGLE CALENDAR

**State A — Not connected (or Google account not signed in):**

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Google Calendar | Navigation | "Google Calendar" | "Not connected" | Chevron |

Tap → if Google account not signed in: shows snackbar "Sign in to your Google account first." Duration: 3 seconds.
Tap → if Google account signed in: initiates Google Calendar OAuth scope request flow. On success, sublabel updates to "Connected".

**State B — Connected:**

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Google Calendar | Static | "Google Calendar" | "Connected" | `Icons.check_circle_outline` in green (`#43A047`) |
| Disconnect | Navigation | "Disconnect Google Calendar" | — | Chevron |

Disconnect tap → shows confirmation `AlertDialog`:
- Title: "Disconnect Google Calendar?"
- Body: "Calendar events will no longer appear in FluxDone."
- Actions: "Cancel", "Disconnect"

### 10.4 Section: GOOGLE DRIVE BACKUP

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Google Drive Backup | Navigation | "Google Drive Backup" | Last backup timestamp if available, "Never backed up" if not | Chevron |

Tap → navigates to SCR-13 (Google Drive Backup Screen).
If Google account not signed in: shows snackbar "Sign in to your Google account first." Duration: 3 seconds.

---

## 11. Sub-screen: Notifications

**Route:** `/settings/notifications`
**Title:** "Notifications"
**Purpose:** Configure app-wide notification behaviour.

### 11.1 Layout

Two sections.

### 11.2 Section: GENERAL

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Notifications | Switch row | "Notifications" | "Enable or disable all FluxDone notifications" | `Switch` |

**Switch specs:**
- Component: `Switch`
- Track color ON: App primary color at 50% opacity
- Track color OFF: `#BDBDBD`
- Thumb color ON: App primary color
- Thumb color OFF: `#FFFFFF`
- Animation: 200ms `easeInOut` thumb slide

**Behavior:** Toggling OFF disables all local notifications app-wide. All scheduled reminders are cancelled. Toggling back ON re-schedules all active reminders. State persists to `shared_preferences` key: `notifications_enabled`.

When master switch is OFF, all rows below are visually disabled (40% opacity, no ripple).

### 11.3 Section: DEFAULTS

| Row | Type | Label | Sublabel | Trailing |
|---|---|---|---|---|
| Default Reminder | Navigation | "Default Reminder" | Current value (e.g., "15 minutes before") | Chevron |
| Notification Sound | Navigation | "Notification Sound" | Current sound name (e.g., "Default") | Chevron |
| System Notification Settings | Navigation | "System Notification Settings" | "Manage notification channels in Android settings" | `Icons.open_in_new` (24dp) |

**Default Reminder row — tap behavior:**
Opens a bottom sheet with a list of preset options:

Options:
- "At start time"
- "5 minutes before"
- "10 minutes before"
- "15 minutes before" (default)
- "30 minutes before"
- "1 hour before"
- "1 day before"

Each option is a `RadioListTile`. Selected option has primary color radio fill. Tapping an option selects it and dismisses the bottom sheet. Value persists to `shared_preferences` key: `default_reminder_offset`.

**Notification Sound row — tap behavior:**
Opens a bottom sheet listing available notification sounds. Each row has a label and a play `IconButton` (`Icons.play_circle_outline`, 24dp) to preview the sound. Selecting a sound persists to `shared_preferences` key: `notification_sound`. Sounds available in Phase 1: system default only (additional sounds Phase 2).

**System Notification Settings row — tap behavior:**
Launches Android system notification settings for FluxDone via `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)`. Opens outside the app in Android Settings. Uses `Icons.open_in_new` trailing icon instead of chevron to signal external navigation.

---

## 12. Bottom Sheet Component (Used in Notifications Sub-screen)

**Component:** `showModalBottomSheet`
**Corner radius:** 16dp top corners
**Background:** Surface color
**Handle bar:** 4dp × 32dp rounded rect, `#E0E0E0`, centered, 8dp from top
**Max height:** 60% of screen height
**Scroll:** `SingleChildScrollView` if content exceeds max height
**Dismiss:** Swipe down or tap outside

---

## 13. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Navigate to sub-screen | Horizontal slide in from right | 300ms | `easeInOut` |
| Navigate back from sub-screen | Horizontal slide out to right | 300ms | `easeInOut` |
| Theme change applied | Full-app fade transition | 300ms | `easeInOut` |
| Switch toggle | Thumb slide + track color transition | 200ms | `easeInOut` |
| Row press ripple | Material ripple from tap point | 150ms | — |
| Bottom sheet open | Slide up from bottom | 300ms | Spring physics |
| Bottom sheet dismiss | Slide down | 250ms | `easeIn` |
| Dialog open | Fade in + scale 0.9 → 1.0 | 220ms | `easeOut` |
| Dialog dismiss | Fade out | 180ms | `easeIn` |
| Snackbar appear | Slide up from bottom | 200ms | `easeOut` |
| Snackbar dismiss | Fade out | 200ms | `easeIn` |

---

## 14. Empty & Error States

### 14.1 Google Account Sign-in Failure
- Snackbar: "Couldn't sign in. Please try again."
- Duration: 4 seconds
- Action button: "Retry"

### 14.2 Google Calendar Connection Failure
- Snackbar: "Couldn't connect Google Calendar. Please try again."
- Duration: 4 seconds
- No action button

### 14.3 Notification Permission Denied (Android)
- If the user has denied notification permission at the Android OS level, the master Notifications switch renders as disabled
- Sublabel changes to: "Permission required — tap to open system settings"
- Tapping the row (not the switch) launches Android app notification settings via Intent

---

## 15. Accessibility

- All interactive rows have semantic labels matching their label text
- Switch rows: accessible label includes current state, e.g., "Notifications, switch, on"
- Radio rows: accessible label includes selected state, e.g., "Dark, radio button, selected"
- Back arrow: semantic label "Navigate back"
- Minimum touch target for all interactive elements: 48dp × 48dp
- Section headers are non-interactive — excluded from accessibility focus order
- Static About rows are non-interactive — screen reader reads them as plain text

---

## 16. Data & Persistence

| Setting | Storage | Key | Values |
|---|---|---|---|
| Theme | `shared_preferences` | `app_theme` | `light`, `dark` |
| Notifications master | `shared_preferences` | `notifications_enabled` | `true`, `false` |
| Default reminder offset | `shared_preferences` | `default_reminder_offset` | Integer minutes (0, 5, 10, 15, 30, 60, 1440) |
| Notification sound | `shared_preferences` | `notification_sound` | String sound identifier |
| Google account token | Android Keystore (via `google_sign_in`) | Managed by SDK | — |
| Google Calendar connected | `shared_preferences` | `google_calendar_connected` | `true`, `false` |
| Last backup timestamp | `shared_preferences` | `last_backup_timestamp` | Unix ms or null |

---

## 17. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Screen root | `Scaffold` |
| Top app bar | `AppBar` with `leading: BackButton()` |
| Settings list | `ListView` |
| Section card | `Card` wrapping `Column` |
| Section header | `Padding` + `Text` |
| Navigation row | `ListTile` with `trailing: Icon(Icons.chevron_right)` |
| Switch row | `SwitchListTile` |
| Radio row | `RadioListTile<T>` |
| Static info row | `ListTile` with `enabled: false` |
| Internal divider | `Divider(height: 1, thickness: 1)` |
| Bottom sheet | `showModalBottomSheet` |
| Confirmation dialog | `showDialog` + `AlertDialog` |
| Snackbar | `ScaffoldMessenger.of(context).showSnackBar` |
| Theme radio sub-screen | Separate `Scaffold` route via `go_router` |
| Google sub-screen | Separate `Scaffold` route via `go_router` |
| Notifications sub-screen | Separate `Scaffold` route via `go_router` |

---

## 18. Route Map

| Screen | Route |
|---|---|
| Root Settings | `/settings` |
| Theme sub-screen | `/settings/theme` |
| Google sub-screen | `/settings/google` |
| Notifications sub-screen | `/settings/notifications` |
| Google Drive Backup | `/settings/google/backup` → SCR-13 |

---

## 19. Out of Scope for Phase 1

- Custom JSON theming (Phase 3 — token-based architecture required from day one but user-facing JSON editor deferred)
- Per-list notification settings
- Multiple notification sounds (Phase 1 ships system default only)
- Widgets / home screen configuration
- Language / locale settings
- Data export (other than Google Drive backup)
- FluxFoxus integration settings (Phase 2)

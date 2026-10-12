# ui_SCR-13_google-drive-backup.md
## FluxDone — Google Drive Backup Screen
**Screen ID:** SCR-13
**Version:** 1.0
**Status:** Locked
**Last Updated:** March 2026

---

## 1. Screen Purpose

SCR-13 is the Google Drive backup and restore management screen. It shows the linked Google account, last backup timestamp, manual backup trigger, and restore-from-backup option. It is a Phase 2 feature — the screen exists in Phase 1 as a placeholder with all controls disabled and a "Coming in Phase 2" banner. Full functionality activates in Phase 2 when Google Drive API integration is implemented.

---

## 2. Navigation & Entry Points

- **Entry:** "Google Drive Backup" row in SCR-09 Settings → Google sub-screen
- **Route:** `/settings/google/backup`
- **Exit:** Back arrow in top app bar or Android back gesture
- **Transition in:** Horizontal slide-in from right (300ms `easeInOut`)
- **Transition out:** Horizontal slide-out to right

---

## 3. Overall Layout

```
┌─────────────────────────────────────────────┐
│ Status Bar                                  │
├─────────────────────────────────────────────┤
│ Top App Bar (56dp)                          │
│ [←] Google Drive Backup                    │
├─────────────────────────────────────────────┤
│ [Phase 2 banner — if Phase 1]               │
├─────────────────────────────────────────────┤
│ Scrollable Body                             │
│                                             │
│  ┌── ACCOUNT section ──────────────────┐   │
│  │ Google account row                  │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌── BACKUP section ───────────────────┐   │
│  │ Last backup row                     │   │
│  │ Back up now row                     │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌── RESTORE section ──────────────────┐   │
│  │ Restore from backup row             │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌── ABOUT section ────────────────────┐   │
│  │ Storage info row                    │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

---

## 4. Top App Bar

**Component:** `AppBar`
**Height:** 56dp
**Background:** Surface color
**Elevation:** 0dp, 1dp bottom divider

| Position | Component | Details |
|---|---|---|
| Left | Back arrow `IconButton` | `Icons.arrow_back`, 20dp |
| Center | Title `Text` | "Google Drive Backup", 18sp, semibold |

---

## 5. Phase 1 Placeholder Banner

**Visible in Phase 1 only.** Removed entirely in Phase 2.

**Component:** `Container`
**Height:** 48dp
**Background:** `#FB8C00` (Orange) at 15% opacity
**Border:** 1dp bottom, `#FB8C00` at 40% opacity

```
Row (padding: 16dp horizontal, 12dp vertical)
 ├── Icons.info_outline (20dp, #FB8C00)
 ├── 12dp gap
 └── "Google Drive backup is coming in Phase 2" Text
      (13sp, regular, #FB8C00)
```

**All interactive rows in Phase 1:** Rendered at 40% opacity, `onTap: null` (non-interactive). Snackbar on any tap attempt: "Google Drive backup is not yet available." Duration: 3 seconds.

---

## 6. Section Layout

Identical section card pattern to SCR-09:
- `Card` with 12dp corner radius, 0dp elevation
- 16dp left/right margin, 12dp bottom margin
- Section header: 12sp, semibold, all caps, secondary text color, 32dp height, 16dp left padding
- Internal rows: `ListTile` pattern, 1dp dividers

---

## 7. ACCOUNT Section

### 7.1 Google Account Row

**State A — Not signed in:**

| Element | Details |
|---|---|
| Leading | `Icons.account_circle_outlined`, 20dp, secondary color |
| Label | "Google Account" |
| Value | "Not signed in" — secondary text color |
| Trailing | `Icons.chevron_right` |

**Tap:** Shows snackbar: "Sign in to your Google account in Settings first." Duration: 3 seconds. No navigation.

**State B — Signed in:**

| Element | Details |
|---|---|
| Leading | Google account avatar (32dp circle, loaded from Google Sign-In) or `Icons.account_circle` fallback |
| Label | User's full name (15sp, regular) |
| Value | User's email address (13sp, secondary color) |
| Trailing | None |

Non-tappable in Phase 2 (account management is in SCR-09 Settings → Google).

---

## 8. BACKUP Section

### 8.1 Last Backup Row

**Non-tappable — static info row.**

| Element | Details |
|---|---|
| Leading | `Icons.history`, 20dp, secondary color |
| Label | "Last backup" |
| Value | Formatted timestamp or "Never" |

**Timestamp format:** "Mar 18, 2026 at 9:32 PM" (uses `intl` package for locale-aware formatting)
**"Never" state:** Value text = "Never backed up", secondary text color, italic

### 8.2 Back Up Now Row

**State A — Idle (no backup in progress):**

| Element | Details |
|---|---|
| Leading | `Icons.backup_outlined`, 20dp, app primary color |
| Label | "Back up now" (app primary color) |
| Trailing | None |

**Tap behavior:**
1. Row enters loading state (see State B)
2. Calls `BackupService.uploadDatabase()`
3. Uploads `fluxdone.db` to `FluxDone_Backup` folder in Google Drive
4. On success: transitions to State A, updates "Last backup" timestamp, shows snackbar: "Backup complete"
5. On failure: returns to State A, shows snackbar: "Backup failed. Please try again." with "Retry" action

**State B — Backup in progress:**

| Element | Details |
|---|---|
| Leading | `CircularProgressIndicator` (20dp, app primary color) |
| Label | "Backing up…" (secondary text color) |
| Trailing | None |

Non-tappable during backup. Row at 70% opacity.

**State C — Disabled (not signed in):**
- Row at 40% opacity, non-tappable
- Tap → snackbar: "Sign in to your Google account first."

---

## 9. RESTORE Section

### 9.1 Restore From Backup Row

| Element | Details |
|---|---|
| Leading | `Icons.restore`, 20dp, secondary color |
| Label | "Restore from backup" |
| Trailing | `Icons.chevron_right` |

**Tap behavior:**
1. Shows confirmation `AlertDialog`:
   - Title: "Restore from backup?"
   - Body: "This will replace all current data with the last backup from Google Drive. The app will restart after restoring. This cannot be undone."
   - Actions: "Cancel", "Restore" (app primary color text)
2. On confirm: enters loading state — full-screen progress overlay (see Section 10)
3. Downloads `fluxdone.db` from Google Drive, replaces local database
4. Shows `AlertDialog`: "Restore complete. The app needs to restart." with single "Restart" action
5. App restarts programmatically

**State — No backup exists:**
- Row at 40% opacity
- Tap → snackbar: "No backup found in Google Drive." Duration: 3 seconds.

**State — Not signed in:**
- Row at 40% opacity
- Tap → snackbar: "Sign in to your Google account first."

---

## 10. Full-Screen Restore Progress Overlay

**Visible only during active restore operation.**

**Component:** `Stack` overlay covering entire screen
**Background:** `#000000` at 60% opacity

```
Column (centered)
 ├── CircularProgressIndicator (48dp, white)
 ├── 24dp gap
 └── "Restoring backup…" Text (16sp, medium, white)
```

Non-dismissible — back gesture disabled during restore.

---

## 11. ABOUT Section

### 11.1 Storage Info Row

**Non-tappable — static info row.**

| Element | Details |
|---|---|
| Leading | `Icons.storage_outlined`, 20dp, secondary color |
| Label | "Backup location" |
| Value | "FluxDone_Backup folder in Google Drive" |

**Typography:** 13sp, regular, secondary text color for value.

### 11.2 Backup Size Row (Phase 2 only — hidden in Phase 1)

| Element | Details |
|---|---|
| Leading | `Icons.folder_outlined`, 20dp, secondary color |
| Label | "Backup size" |
| Value | File size string (e.g., "2.4 MB") or "—" if no backup |

---

## 12. Empty & Error States

| State | Behaviour |
|---|---|
| Not signed in | All rows disabled, snackbar on tap |
| No backup exists | Restore row disabled, Last backup = "Never" |
| Backup in progress | Back Up Now row in loading state, non-tappable |
| Restore in progress | Full-screen overlay, non-dismissible |
| Network error on backup | Snackbar with Retry action |
| Network error on restore | Snackbar: "Restore failed. Check your connection." |
| Google Drive quota exceeded | Snackbar: "Google Drive storage is full." |

---

## 13. Animations & Transitions

| Interaction | Animation | Duration | Curve |
|---|---|---|---|
| Screen open | Horizontal slide in | 300ms | `easeInOut` |
| Back Up Now tap → loading | Row content crossfade | 200ms | `easeInOut` |
| Backup complete → idle | Row content crossfade | 200ms | `easeInOut` |
| Last backup timestamp update | Number/text fade transition | 300ms | `easeOut` |
| Restore overlay appear | Fade in | 200ms | `easeOut` |
| Restore overlay dismiss | Fade out | 200ms | `easeIn` |
| Snackbar appear | Slide up | 200ms | `easeOut` |
| Confirmation dialog open | Fade + scale 0.9→1.0 | 220ms | `easeOut` |

---

## 14. Accessibility

- Back Up Now row: "Back up now button" / "Backing up, please wait" (during backup)
- Restore row: "Restore from backup button"
- Last backup row: "Last backup: [timestamp]"
- Progress overlay: announce "Restoring backup, please wait" to screen reader
- Minimum touch target: 48dp × 48dp

---

## 15. Flutter Component Mapping

| UI Element | Flutter Widget |
|---|---|
| Screen root | `Scaffold` |
| Top app bar | `AppBar` with `BackButton` |
| Phase 1 banner | `Container` with `Row` |
| Section card | `Card` wrapping `Column` |
| Section header | `Padding` + `Text` |
| Static info row | `ListTile` with `enabled: false` |
| Navigation row | `ListTile` with `InkWell` |
| Back Up Now row | `ListTile` with state-driven content |
| Loading indicator | `CircularProgressIndicator` |
| Restore overlay | `Stack` + `ModalBarrier` |
| Confirmation dialog | `showDialog` + `AlertDialog` |
| Snackbar | `ScaffoldMessenger.showSnackBar` |

---

## 16. Data & Persistence

| Data | Storage | Key / Location |
|---|---|---|
| Last backup timestamp | `shared_preferences` | `last_backup_timestamp` (Unix ms) |
| Google account token | Android Keystore via `google_sign_in` | Managed by SDK |
| Google Drive folder ID | `shared_preferences` | `drive_backup_folder_id` |
| Backup file size | Fetched from Drive API on screen open | Not persisted locally |

---

## 17. Phase 1 vs Phase 2 Behaviour Summary

| Element | Phase 1 | Phase 2 |
|---|---|---|
| Phase banner | Visible | Hidden |
| All rows | 40% opacity, non-interactive | Fully interactive |
| Back Up Now | Disabled | Active |
| Restore | Disabled | Active |
| Backup size row | Hidden | Visible |
| Google Drive API | Not called | Fully integrated |

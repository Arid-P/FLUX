# FD Phase 2 — Amendment: Automatic Google Drive Backup

**Version:** 1.0  
**Phase:** 2 (Amendment)  
**Status:** Locked  
**Author:** Ari  
**Amends:** PRD v2 §5 (FD-16 Google Drive Backup), TRD v2, `ui_SCR-13_google-drive-backup.md`

---

## 1. Overview

Google Drive backup is **automatic** — not manual-only. This amendment corrects the original SCR-13 spec which incorrectly described backup as a manual-only action. The correct behaviour is change-triggered auto-backup with a debounce delay, a periodic fallback, and a manual trigger still available for on-demand use.

---

## 2. Backup Trigger Model

Three trigger types, all uploading the same SQLite database file to Google Drive:

### 2.1 Change-Triggered Auto-Backup (Primary)

**Trigger:** Any qualifying data write in FD.

**Qualifying writes:**
- Task created, updated, completed, deleted, restored
- List created, updated, deleted
- Folder created, updated, deleted
- Section created, updated, deleted
- Habit created, updated, completed, deleted
- Note created, updated, deleted (P3)
- Settings changed (theme, notification prefs)

**Debounce:** 120 seconds after the last qualifying write. If another write occurs within the debounce window, the timer resets. This prevents a backup from firing on every single keystroke during a bulk operation.

**Implementation:** WorkManager `OneTimeWorkRequest` with a 120-second initial delay, enqueued with `ExistingWorkPolicy.REPLACE` so each new write resets the timer.

```dart
void scheduleAutoBackup() {
  final workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
    .setInitialDelay(const Duration(seconds: 120))
    .setConstraints(Constraints(requiredNetworkType: NetworkType.connected))
    .build();

  WorkManager.instance.enqueueUniqueWork(
    'auto_backup',
    ExistingWorkPolicy.replace,
    workRequest,
  );
}
```

### 2.2 Periodic Fallback Backup

**Trigger:** Every 24 hours, regardless of whether any changes occurred.

**Purpose:** Catches cases where the change-triggered backup failed silently (e.g. no network at time of trigger).

**Implementation:** WorkManager `PeriodicWorkRequest` with 24-hour interval.

**Constraint:** Requires network connectivity. If no network: WorkManager retries with exponential backoff until network is available.

### 2.3 Manual Trigger

**Trigger:** User taps "Back Up Now" in SCR-13.

**Purpose:** Immediate on-demand backup. Fires without debounce, without waiting for the next auto cycle.

**Behaviour:** Full-screen non-dismissible overlay during upload (existing SCR-13 spec). On completion: success snackbar + timestamp update.

---

## 3. Backup Conditions

Auto-backup (both change-triggered and periodic) only runs when:
- User is signed in to Google (Google account linked in Settings)
- Network connectivity is available
- Google Drive permission is granted

If any condition is not met: backup is queued silently. Retried by WorkManager when conditions are met. No error shown to user for silent auto-backup failures — only the "Last backup" timestamp communicates freshness.

---

## 4. SCR-13 UI Updates

### 4.1 Auto-Backup Status

SCR-13 (Google Drive Backup screen) gains a new status row:

| Element | Value |
|---|---|
| Auto-backup label | "Auto-backup" |
| Status | "On" (Soft Cyan) when Google account linked |
| Sub-label | "Backs up automatically after changes and every 24 hours" |

### 4.2 Last Backup Timestamp

Unchanged from original spec — shows timestamp of last successful backup (manual or auto).

New addition: if last backup was > 48 hours ago AND user is signed in, timestamp label turns amber with a warning icon: *"Last backup was [X] days ago"*

### 4.3 Manual Trigger Button

"Back Up Now" button remains. Label updates during auto-backup in progress: *"Backing up..."* (disabled during auto-backup to prevent concurrent uploads).

### 4.4 Auto-Backup Toggle

New toggle in SCR-13:

| Control | Type | Default |
|---|---|---|
| Auto-backup | Toggle | ON |

When OFF: change-triggered and periodic backups are disabled. Manual only. User is shown a warning: *"Auto-backup is off. Your data will not be backed up automatically."*

---

## 5. Backup File

No change to backup file format — same raw SQLite database file upload as originally specced. File name: `fluxdone_backup_[timestamp].db` or overwrite a fixed filename `fluxdone_backup.db` (single file, always overwritten — not versioned).

**Decision:** Single file, always overwritten. Versioning is a P4+ consideration.

---

## 6. Storage Changes

New `shared_preferences` keys:

| Key | Type | Description |
|---|---|---|
| `backup_auto_enabled` | bool | Auto-backup toggle state |
| `backup_last_success_timestamp` | int | Unix ms of last successful backup |
| `backup_last_attempt_timestamp` | int | Unix ms of last attempt (success or fail) |

---

## 7. Module Boundary

Modifications to existing P2 `backup/` module:

```
features/
└── backup/
    ├── data/
    │   └── backup_repository_impl.dart    ← MODIFIED (auto-backup scheduling)
    ├── domain/
    │   └── use_cases/
    │       └── schedule_auto_backup.dart  ← NEW
    └── presentation/
        └── backup_screen.dart             ← MODIFIED (auto-backup status, toggle, warning)

android/
└── app/src/main/kotlin/
    └── BackupWorker.kt                    ← NEW (WorkManager worker for Drive upload)
```

`scheduleAutoBackup()` is called from the data layer after every qualifying write — injected into each relevant repository's write methods via the event bus (rxdart) rather than directly coupling repositories to the backup module.

```dart
// In BackupService — listens to event bus
eventBus.on<TaskCreatedEvent>().listen((_) => scheduleAutoBackup());
eventBus.on<TaskUpdatedEvent>().listen((_) => scheduleAutoBackup());
eventBus.on<TaskDeletedEvent>().listen((_) => scheduleAutoBackup());
// ... same for all qualifying events
```

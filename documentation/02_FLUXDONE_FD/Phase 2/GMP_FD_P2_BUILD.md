# 🚀 GMP — Phase 2 Grand Master Prompt: FluxDone (FD) Build Instructions

**Version:** 2.0 (Native Android Kotlin Phase 2 Master Build Specification)  
**Role:** Autonomous AI Senior Android System & UI Engineer  
**Target:** Android Application (`com.fluxdone`, Jetpack Compose) & Ubuntu Linux Desktop (`com.fluxdone.desktop`, Compose Multiplatform / JetBrains Compose)  
**Prerequisites:** Phase 1 Complete Suite ([`GMP_FD_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/GMP_FD_BUILD.md)) & [`IPC_SPEC_FD_FF_NATIVE.md`](file:///home/hp/Flux_Prompts/documentation/01_INTEGRATION_SYNC/IPC_SPEC_FD_FF_NATIVE.md)

---

## 0. Non-Negotiable Operational Rules & Invariants

1. **Zero Flutter / Zero Dart:** 100% Native Kotlin (Jetpack Compose on Android & JetBrains Compose Multiplatform on Desktop).
2. **Zero Mocks in Production Code:** All components must bind to real Android APIs (`AlarmManager`, `NotificationManagerCompat`, `StorageAccessFramework`, `PackageManager`, Room DAOs, DataStore Preferences). Hardcoded dummy lists or mock data are strictly prohibited in `main`.
3. **Mandatory Subtask Completion Invariant:** Completing a task (via single check-off, notification shade action, swipe gesture, or batch operations) **strictly and atomically marks all child subtasks complete** (`is_completed = 1`). Unchecking any subtask revives the parent task.
4. **Calendar Slot Priority Invariant:** In Quick Capture, if a time slot was pre-selected on the Calendar canvas (SCR-04), it has the absolute highest priority; typed times in the title are ignored by the scheduler.
5. **The 20-Task Activity Grace Horizon:** Restoring or importing a backup automatically creates a safety snapshot (`pre_restore_safety_backup.db`) allowing 1-tap rollback for up to **20 subsequent task mutations** before automatic cleanup.
6. **The 3 Actionable Notification Buttons:**
   - `[ ✓ Mark Done ]`: Background completion with subtask sync and haptics.
   - `[ 🚫 Won't Do ]`: Background skip / won't-do; advances recurring tasks per RRULE.
   - `[ 📅 Reschedule ]`: Launches floating dialog activity (`NotificationRescheduleActivity`) offering quick snoozes (`+15m`, `+30m`, `+1h`, `+2h`, `Tomorrow`, `Next Week`, `Pick Date`).
7. **Debounced Rate-Limited IPC Broadcast:**
   - Dispatches `com.flux.action.FOCUS_BLOCK_SYNC` to `com.fluxfoxus`.
   - When moving blocks on the Calendar timeline, applies an adjustable delay buffer (**15 seconds minimum**, default 30s, **5 minutes maximum**) to prevent broadcast spam during schedule adjustments.
8. **Autonomous Execution Order:** Implement Phase 2 features sequentially:
   - Module 1: Quick Capture & Natural Language Parsing (`FD_P2_QUICK_CAPTURE_NLP_SPEC.md`)
   - Module 2: Batch Operations & Multi-Select Engine (`FD_P2_BATCH_OPERATIONS_SPEC.md`)
   - Module 3: Habit Matrix Analytics & Consistency Heatmaps (`FD_P2_HABIT_ANALYTICS_SPEC.md`)
   - Module 4: Local Backup, JSON Schema Export & Import (`FD_P2_BACKUP_EXPORT_SPEC.md`)
   - Module 5: Notification & Reminders Engine (`FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md`)
   - Module 6: Settings Screen & FluxFoxus IPC Bridge (`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`)

---

## 1. Module 1: Quick Capture & Natural Language Parsing (Smart Recognition)

* **Specification:** [`FD_P2_QUICK_CAPTURE_NLP_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_QUICK_CAPTURE_NLP_SPEC.md)
* **Tasks:**
  1. Implement `SmartRecognitionParser.kt` in pure Kotlin with regex tokenizers for relative dates (`today`/`tmr`), weekdays, specific dates, times, spans (`4pm - 5:30pm`), priorities (`!high`), and recurrence (`every day`).
  2. Implement List Routing: `#ExistingList` routes task to list; `#unrecognized_tag` presents inline `[ + Create list "tag" ]` pill; if ignored, preserves tag text in title/notes.
  3. Enforce Calendar slot priority invariant: When `isCalendarSlotLocked = true`, typed times in title are ignored.
  4. Build `QuickCreationSheet` (SCR-03) with 300ms debounce, colored underline tokens, dismissable attribute chips, and title cleanup on submit.
  5. Integrate Android `SpeechRecognizer` via mic icon button in title field.

---

## 2. Module 2: Batch Operations & Multi-Select Engine

* **Specification:** [`FD_P2_BATCH_OPERATIONS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_BATCH_OPERATIONS_SPEC.md)
* **Tasks:**
  1. Implement Multi-Select Mode in SCR-01 and SCR-05 (activated via long-press on task card or top bar button).
  2. Build Contextual Top Bar (`✓ X Selected`, `Select All`, `Close ✕`).
  3. Build Docked Bottom Action Strip:
     - `[ 📅 Date ]`: Absolute date or relative `+1 Day` shift.
     - `[ 📁 Move ]`: 2-step destination picker (List $\to$ Section).
     - `[ 🚩 Priority ]`: Bulk priority flag picker.
     - `[ ✓ Done ]`: Atomic bulk complete enforcing the mandatory subtask completion invariant.
     - `[ 🗑️ Trash ]`: Bulk soft-delete with 30-day retention and unified Recurrence Scope Dialog.
  4. Implement `TaskBatchDao.kt` executing all mutations within atomic `@Transaction` blocks.
  5. Provide 5-second Undo snackbar for batch deletions.

---

## 3. Module 3: Habit Matrix Analytics & Consistency Heatmaps

* **Specification:** [`FD_P2_HABIT_ANALYTICS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_HABIT_ANALYTICS_SPEC.md)
* **Tasks:**
  1. Build `HabitMatrixScreen.kt` with Calendar Month navigation (`[ < ] Month Year [ > ]`).
  2. Implement Matrix Grid rendering:
     - 100% Target Met = Solid habit color.
     - Partial Progress = Proportional alpha ($\max(0.25f, \text{completed}/\text{target})$).
     - Missed Day = Dark muted slate surface (`#1B263B`).
     - Rest Day = Hollow border with centered dot (`·`).
     - Future Day = Ghost outline.
  3. Implement fair consistency formula: Exclude rest days from denominator ($\text{Rate} = \frac{\text{Days Met}}{\text{Scheduled Days}} \times 100\%$).
  4. Build Single-Habit Analytics Cockpit: Current Streak vs Best Streak, Weekly Distribution Bar Chart (Mon–Sun), and monthly mini-calendar grid.
  5. Implement `HabitAnalyticsDao.kt` and `HabitMatrixCalculator.kt`.

---

## 4. Module 4: Local Backup, JSON Schema Export & Import

* **Specification:** [`FD_P2_BACKUP_EXPORT_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_BACKUP_EXPORT_SPEC.md)
* **Tasks:**
  1. Build `BackupScreen.kt` (SCR-13) using Android Storage Access Framework (`ACTION_CREATE_DOCUMENT`, `ACTION_OPEN_DOCUMENT`).
  2. Implement Portable JSON v2.0 export/import with full relational schema and SHA-256 integrity checksum.
  3. Implement Binary SQLite Snapshot export: Flush WAL via `PRAGMA wal_checkpoint(FULL);` before copying `fluxdone.db`.
  4. Implement Merge & Update Newer conflict resolution strategy.
  5. Implement the **20-Task Activity Grace Horizon**: Create `pre_restore_safety_backup.db` before restore; show rollback banner in SCR-13; decrement remaining action counter on each user task mutation; delete snapshot upon reaching 0.

---

## 5. Module 5: Notification & Reminders Engine

* **Specification:** [`FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md)
* **Tasks:**
  1. Implement `TaskAlarmScheduler.kt` leveraging `AlarmManager.setExactAndAllowWhileIdle()`.
  2. Register 3 dedicated Notification Channels: `Task Alarms`, `Habit Nudges`, `Daily Agenda`.
  3. Implement the 3 Actionable Notification Buttons:
     - `[ ✓ Mark Done ]`: Handled silently in background via `NotificationActionReceiver`, applying subtask invariant.
     - `[ 🚫 Won't Do ]`: Marks skipped, advances recurring tasks per RRULE.
     - `[ 📅 Reschedule ]`: Launches floating dialog `NotificationRescheduleActivity` offering quick snooze offsets (`+15m`, `+30m`, `+1h`, `+2h`, `Tomorrow`, `Next Week`, `Pick Date`).
  4. Implement Persistent Daily Morning Agenda notification (07:00 AM default, configurable in Settings).
  5. Implement `BootCompletedReceiver` re-arming all pending alarms on reboot or timezone shift.

---

## 6. Module 6: Settings Screen & FluxFoxus (FF) IPC Bridge

* **Specification:** [`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md)
* **Tasks:**
  1. Build `SettingsScreen.kt` (SCR-09) covering Appearance, Smart Lists manager, Task Ergonomics, Notifications, Data, and Account profile.
  2. Implement Account & Profile section with architectural placeholder for Phase 4 Google Sign-In & Cloud Sync.
  3. Implement Swipe Gesture Customization (Left: Trash [default], Won't Do, Complete; Right: Tomorrow [default], Done, Snooze 1h).
  4. Implement Trash Retention Duration setting (7d, 14d, 30d [default], 60d, Never).
  5. Implement `FocusBlockBroadcastSender.kt` dispatching `com.flux.action.FOCUS_BLOCK_SYNC` to `com.fluxfoxus`.
  6. Implement Reschedule Sync Delay Buffer (**15s minimum**, default 30s, **5m maximum**) with coroutine debounce pipeline.
  7. Implement dynamic FluxFoxus package detection and `[ Test Sync 🔄 ]` diagnostic ping button.

---

## 7. Verification & Quality Gates

Before declaring Phase 2 complete, execute and pass all verification checks:
```bash
# 1. Compile and execute unit tests (Smart Recognition regex, Batch DAO transactions, Matrix Calculator, Backup Checksums)
./gradlew testDebugUnitTest

# 2. Build debug APK
./gradlew assembleDebug

# 3. Verify APK exists and has zero manifest, resource, or permission conflicts
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

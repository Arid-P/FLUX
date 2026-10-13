# 💾 Topic FD-P2-4: Local Backup, JSON Schema Export & Import Specification

**Document ID:** `FD_P2_BACKUP_EXPORT_SPEC`  
**Phase:** Phase 2 (Advanced Ergonomics, Settings & Inter-App IPC)  
**Target Package:** `com.flux.done.features.backup`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Room SQLite File Ops + `kotlinx.serialization` + SAF)  
**Screen References:** SCR-13 (Backup & Export Screen)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari’s academic and Olympiad preparation workflow, months of meticulously scheduled problem sets, past mock scores, live notes, and habit streaks represent invaluable intellectual property. Storing data solely in a live app database without reliable backup mechanisms creates vulnerability.

**Topic FD-P2-4 establishes the Complete Offline Backup, Export & Import Engine for FluxDone.**  
It provides zero-friction data portability and bulletproof safety guarantees:
* **Storage Access Framework (SAF):** Uses native Android `ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT` intents to save and load backup files anywhere the user chooses (Downloads, SD card, USB drive, or synced cloud folder) without requiring dangerous runtime storage permissions.
* **Dual Backup Formats (Option 1.A):**
  1. **Standard JSON Backup (`.json`):** Human-readable, AI-parseable, cross-platform portable format containing full relational schemas and SHA-256 integrity checksums.
  2. **Binary SQLite Database Snapshot (`.db`):** Instant 1:1 binary clone of the entire Room SQLite database file (including WAL checkpoint flush).
* **Merge & Update Newer Conflict Resolution (Option 2.A):** When importing, existing items are updated only if the backup record has a strictly newer `updated_at` timestamp; existing newer data is preserved, and missing items are inserted.
* **The 20-Task Activity Grace Horizon (Option 3.A + User Invariant):** Before executing any restore or import, FluxDone automatically creates a local safety snapshot (`pre_restore_safety_backup.db`). An interactive rollback banner remains active in SCR-13 for a **grace horizon of up to 20 user task mutations** (completions, creations, or edits). If the user realizes the import was incorrect, they can 1-tap revert. Upon the 20th task mutation, the safety snapshot is finalized and deleted.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Database Schema** | [`FD_TRD_KOTLIN.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_TRD_KOTLIN.md) | Ingests and serializes all 7 tables: `folders`, `task_lists`, `sections`, `tasks`, `subtasks`, `habits`, `habit_completions`. |
| **Recurrence Engine** | [`FD_P1_RECURRENCE_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_RECURRENCE_ENGINE_SPEC.md) | Exports RFC 5545 RRULE strings and `recurrence_series_id` losslessly. |
| **Habit Tracker & Heatmaps**| [`FD_P2_HABIT_ANALYTICS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_HABIT_ANALYTICS_SPEC.md) | Preserves `habit_completions` records with exact `completion_date` keys for unbroken consistency heatmaps. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Backup & Export Screen (`BackupScreen.kt` — SCR-13)

Accessible via Settings or Navigation Drawer footer:

```
+-----------------------------------------------------------------+
|  ←  BACKUP & EXPORT                                             |
+-----------------------------------------------------------------+
|  DATABASE STATUS                                                |
|  Current Storage: 1.4 MB • 428 Tasks • 6 Habits • 12 Lists      |
|  Last Export: Yesterday, 21:30 PM (JSON)                        |
+-----------------------------------------------------------------+
|  [ ⏪ ROLLBACK AVAILABLE: 17 of 20 tasks remaining ]            | <-- Dynamic Rollback Banner
|  Restored Oct 12, 14:00. Tap to revert to pre-import state.     |
|  [ Revert Database Now ⏪ ]                                      |
+-----------------------------------------------------------------+
|  EXPORT DATA                                                    |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  📄 Export as Portable JSON (.json)                        │ |
|  │     Human-readable, AI-parseable, portable across devices  │ |
|  │     [ Save JSON File → ]                                   │ |
|  └────────────────────────────────────────────────────────────┘ |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  💾 Export as Binary SQLite Snapshot (.db)                 │ |
|  │     Exact 1:1 database clone for instant offline clone     │ |
|  │     [ Save .db Snapshot → ]                                │ |
|  └────────────────────────────────────────────────────────────┘ |
+-----------------------------------------------------------------+
|  IMPORT & RESTORE                                               |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  📥 Restore from File (.json or .db)                       │ |
|  │     Select a previously exported file to merge or restore. │ |
|  │     [ Select Backup File 📁 ]                              │ |
|  └────────────────────────────────────────────────────────────┘ |
+-----------------------------------------------------------------+
```

#### Interactive Elements & Workflows:
1. **Export as JSON:**
   * Tapping `[ Save JSON File → ]` summons Android SAF `ACTION_CREATE_DOCUMENT` with default filename:  
     `fluxdone_backup_YYYYMMDD_HHMMSS.json`.
   * Upon URI selection, writes JSON stream with SHA-256 verification and displays:  
     *"Backup saved successfully (428 tasks, 6 habits)"*.
2. **Export as Binary Snapshot:**
   * Tapping `[ Save .db Snapshot → ]` executes SQLite WAL checkpoint (`PRAGMA wal_checkpoint(FULL)`), copies `fluxdone.db` to the chosen SAF destination as `fluxdone_snapshot_YYYYMMDD_HHMMSS.db`.
3. **Import Flow with Safety Snapshot:**
   * Tapping `[ Select Backup File 📁 ]` opens SAF `ACTION_OPEN_DOCUMENT` filtering for `*/*` (`.json` or `.db`).
   * When file is picked, parses header and displays the **Restore Confirmation Dialog**:
     ```
     +-------------------------------------------------------+
     |  Restore Data from Backup?                            |
     +-------------------------------------------------------+
     |  File: fluxdone_backup_20261012.json                  |
     |  Contains: 395 Tasks, 6 Habits, 10 Lists              |
     |                                                       |
     |  Restore Mode:                                        |
     |  (•) Merge & Update Newer (Recommended)               |
     |  ( ) Full Replace (Wipes active and restores backup)  |
     |                                                       |
     |  🛡️ A safety snapshot will be kept for 20 tasks.     |
     |  [ Begin Restore ]                       [ Cancel ]   |
     +-------------------------------------------------------+
     ```
4. **The 20-Task Activity Grace Horizon:**
   * When restore completes, saves `pre_restore_safety_backup.db` in internal cache and sets DataStore preference `rollback_remaining_actions = 20`.
   * Displays the dynamic **Rollback Banner** in SCR-13:  
     *"Rollback available: X of 20 tasks remaining"*.
   * Whenever Ari completes, creates, or edits a task in FD, the counter decrements: `20 → 19 → ... → 0`.
   * If Ari taps `[ Revert Database Now ⏪ ]`: Confirms dialog $\to$ restores `pre_restore_safety_backup.db` $\to$ database rolls back instantly.
   * When counter reaches 0: Deletes `pre_restore_safety_backup.db` and hides banner permanently.

---

## 4. Standardized JSON Schema v2.0 Specification

The JSON export format is structured for deterministic parsing and human/AI legibility:

```json
{
  "_comment": "FluxDone Backup File v2.0 — Generated on 2026-10-13T19:00:00Z. Contains offline tasks, habits, and organizational hierarchy. All dates are ISO-8601 or epoch ms. Priorities: 0=None, 1=Low, 2=Medium, 3=High.",
  "schema_version": "2.0",
  "app_version": "2.0.0",
  "exported_at": 1791899400000,
  "sha256_checksum": "a3f8c9b1...",
  "data": {
    "folders": [
      {
        "id": "fld_olympiad_prep",
        "name": "Olympiad Prep",
        "sort_order": 0,
        "is_deleted": 0,
        "created_at": 1791800000000,
        "updated_at": 1791800000000
      }
    ],
    "task_lists": [
      {
        "id": "list_geometry",
        "folder_id": "fld_olympiad_prep",
        "name": "Geometry",
        "color_hex": "#1565C0",
        "is_sectioned": 1,
        "is_system": 0,
        "sort_order": 1,
        "is_deleted": 0,
        "created_at": 1791800000000,
        "updated_at": 1791800000000
      }
    ],
    "sections": [
      {
        "id": "sec_triangles",
        "list_id": "list_geometry",
        "name": "Triangles & Incenters",
        "sort_order": 0,
        "is_deleted": 0,
        "created_at": 1791800000000
      }
    ],
    "tasks": [
      {
        "id": "task_diophantine_1",
        "list_id": "list_geometry",
        "section_id": "sec_triangles",
        "title": "Solve 5 Incenter Lemma Problems",
        "notes": "Focus on cyclic quadrilaterals and angle chasing.",
        "start_time": 1791900000000,
        "end_time": 1791905400000,
        "duration_minutes": 90,
        "due_date": 1791905400000,
        "is_completed": 1,
        "completed_at": 1791905000000,
        "is_pinned": 0,
        "priority": 3,
        "recurrence_rule": "RRULE:FREQ=WEEKLY;BYDAY=TU,TH",
        "recurrence_series_id": "rec_series_101",
        "is_deleted": 0,
        "created_at": 1791800000000,
        "updated_at": 1791905000000,
        "subtasks": [
          {
            "id": "sub_1",
            "task_id": "task_diophantine_1",
            "title": "Problem 1 (INMO 2018)",
            "is_completed": 1,
            "sort_order": 0
          }
        ]
      }
    ],
    "habits": [
      {
        "id": "habit_ioqm",
        "title": "Solve 12 Hard IOQM Problems",
        "habit_type": "NUMERIC",
        "target_count": 12,
        "increment_count": 3,
        "icon_identifier": "lightbulb",
        "color_hex": "#FB8C00",
        "frequency_type": "SPECIFIC_DAYS",
        "scheduled_days_mask": 63,
        "current_streak": 18,
        "best_streak": 42,
        "is_archived": 0,
        "is_deleted": 0,
        "created_at": 1791500000000
      }
    ],
    "habit_completions": [
      {
        "id": "comp_20261013_ioqm",
        "habit_id": "habit_ioqm",
        "completion_date": "2026-10-13",
        "completed_count": 12,
        "is_target_met": 1,
        "completed_at": 1791903000000
      }
    ]
  }
}
```

---

## 5. Technical Implementation & Room Database Operations

### 5.1 Backup & Restore Manager (`BackupManager.kt`)

```kotlin
package com.flux.done.core.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.flux.done.core.database.FluxDoneDatabase
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class BackupManager(
    private val context: Context,
    private val database: FluxDoneDatabase
) {

    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true 
        encodeDefaults = true 
    }

    // 1. Export as JSON
    suspend fun exportToJson(destinationUri: Uri): Result<Unit> = runCatching {
        // Step A: Read all tables from database
        val backupData = database.withTransaction {
            database.extractAllEntitiesForBackup()
        }
        val serializedJson = json.encodeToString(FluxBackupEnvelope.serializer(), backupData)
        val checksum = calculateSha256(serializedJson)
        val envelopeWithChecksum = backupData.copy(sha256_checksum = checksum)
        val finalJson = json.encodeToString(FluxBackupEnvelope.serializer(), envelopeWithChecksum)

        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            output.write(finalJson.toByteArray())
        } ?: throw IllegalStateException("Cannot open output stream for URI: $destinationUri")
    }

    // 2. Export as Binary SQLite Snapshot
    suspend fun exportSqliteSnapshot(destinationUri: Uri): Result<Unit> = runCatching {
        // Step A: Flush WAL journal into main DB file
        database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
        
        val dbFile = context.getDatabasePath("fluxdone.db")
        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
            dbFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot write database snapshot to URI: $destinationUri")
    }

    // 3. Create Safety Snapshot before Restore (20-Task Activity Horizon)
    suspend fun createPreRestoreSafetySnapshot(): File {
        database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")
        val dbFile = context.getDatabasePath("fluxdone.db")
        val safetyFile = File(context.filesDir, "pre_restore_safety_backup.db")
        dbFile.copyTo(safetyFile, overwrite = true)
        return safetyFile
    }

    // 4. Rollback to Pre-Restore Safety Snapshot
    suspend fun rollbackToSafetySnapshot(): Result<Unit> = runCatching {
        val safetyFile = File(context.filesDir, "pre_restore_safety_backup.db")
        if (!safetyFile.exists()) throw IllegalStateException("Safety snapshot file not found.")

        database.close()
        val dbFile = context.getDatabasePath("fluxdone.db")
        safetyFile.copyTo(dbFile, overwrite = true)
        safetyFile.delete() // Cleanup upon rollback
    }

    // 5. Import & Merge Newer
    suspend fun importJsonBackup(sourceUri: Uri, fullReplace: Boolean): Result<ImportStats> = runCatching {
        val inputString = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Cannot read backup from URI: $sourceUri")

        val envelope = json.decodeFromString(FluxBackupEnvelope.serializer(), inputString)
        
        // Create safety snapshot before altering live state
        createPreRestoreSafetySnapshot()

        database.withTransaction {
            if (fullReplace) {
                database.clearAllTables()
            }
            database.insertOrUpdateNewerEntities(envelope.data)
        }

        ImportStats(
            taskCount = envelope.data.tasks.size,
            habitCount = envelope.data.habits.size,
            listCount = envelope.data.task_lists.size
        )
    }

    private fun calculateSha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

data class ImportStats(val taskCount: Int, val habitCount: Int, val listCount: Int)
```

---

## 6. MVI Architecture & State Management

### 6.1 Backup & Export MVI State (`BackupViewModel.kt`)

```kotlin
package com.flux.done.features.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.done.core.backup.BackupManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BackupUiState(
    val lastBackupTimestampMs: Long? = null,
    val databaseSizeBytes: Long = 0L,
    val totalTaskCount: Int = 0,
    val totalHabitCount: Int = 0,
    val isRollbackAvailable: Boolean = false,
    val rollbackRemainingActions: Int = 0,
    val isRestoreConfirmDialogOpen: Boolean = false,
    val pendingRestoreStats: String? = null,
    val statusSnackbarMessage: String? = null,
    val isLoading: Boolean = false
)

sealed interface BackupIntent {
    object ExportJsonRequested : BackupIntent
    object ExportSqliteRequested : BackupIntent
    data class FileSelectedForImport(val uri: android.net.Uri) : BackupIntent
    data class ConfirmRestore(val fullReplace: Boolean) : BackupIntent
    object ExecuteRollback : BackupIntent
    object DismissDialog : BackupIntent
    object ClearSnackbar : BackupIntent
}
```

---

## 7. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **WAL Checkpoint Before Snapshot** | Room runs in WAL (Write-Ahead Logging) mode. Before copying `fluxdone.db`, the engine executes `PRAGMA wal_checkpoint(FULL);` so all pending transactions in `-wal` and `-shm` are flushed to the primary database file. |
| **Exact 20-Task Activity Horizon Cleanup** | Each time a task is created, updated, or checked off, `rollbackRemainingActions` decrements. When it transitions from 1 to 0, `pre_restore_safety_backup.db` is immediately deleted from app storage, freeing storage cleanly. |
| **Corrupted File or Malformed JSON** | If JSON parsing fails or checksum validation fails, import aborts before touching SQLite; zero changes are made, and an error alert is presented: *"Invalid backup file format or checksum mismatch"*. |
| **UUID Conflict with Newer Local Record** | In "Merge & Update Newer" mode, if local task has `updated_at = Oct 13 18:00` and backup has `updated_at = Oct 12 10:00`, the local task is kept untouched. If backup is newer, the backup record overwrites. |
| **Storage Access Framework Revocation** | Uses `takePersistableUriPermission` on Android SAF URIs where applicable to ensure background Workers don't fail when saving scheduled exports. |

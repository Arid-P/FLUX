# ☁️ Topic FD-P4-1: Cross-Device Cloud Sync Engine & LWW Conflict Resolution Specification

**Document ID:** `FD_P4_CLOUD_SYNC_ENGINE_SPEC`  
**Phase:** Phase 4 (Cloud Sync, Cross-Device Ecosystem & Advanced Notes)  
**Target Package:** `com.flux.done.sync.cloud` (Shared KMP Multiplatform Module)  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Room 2.7+ KMP + Google Drive REST API + Ktor Client + Coroutines Flow)  
**Target Platforms:** Android Phone & Tablet (`com.fluxdone`), Ubuntu Linux Desktop (`com.fluxdone.desktop`)  
**Prerequisites:** [`FD_TRD_KOTLIN.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_TRD_KOTLIN.md), [`FD_P2_BACKUP_EXPORT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_BACKUP_EXPORT_SPEC.md), & [`FD_P3_SHARED_DATABASE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%203/FD_P3_SHARED_DATABASE_SPEC.md)  

---

## 1. Overview & Zero-Cost Cloud Architecture

For academic productivity, a student cannot afford monthly database subscriptions, vendor lock-in, or server maintenance overhead. Simultaneously, tasks, calendar time-blocks, and habit logs must seamlessly mirror across their **Android phone**, **tablet**, and **Ubuntu Linux workstation**.

**Topic FD-P4-1 establishes the $0 Free-Forever Multi-Device Cloud Sync Engine for FluxDone.**

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                FLUXDONE ZERO-COST CLOUD TOPOLOGY                            |
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                             |
|   ┌───────────────────────────┐                           ┌─────────────────────────────┐   |
|   │ 📱 ANDROID PHONE / TABLET │                           │ 💻 UBUNTU LINUX WORKSTATION │   |
|   │ Room SQLite Local Storage │                           │ Room KMP SQLite Local Store │   |
|   └─────────────┬─────────────┘                           └──────────────┬──────────────┘   |
|                 │                                                        │                  |
|                 │ [ 45-Second Debounced Delta ]                          │ [ 45-Second ]    |
|                 ▼                                                        ▼                  |
|   +─────────────────────────────────────────────────────────────────────────────────────+   |
|   |                  GOOGLE DRIVE HIDDEN APP DATA FOLDER (`appDataFolder`)              |   |
|   |  • Scope: `https://www.googleapis.com/auth/drive.appdata`                           |   |
|   |  • Cost: $0 Free Forever (Uses existing 15GB Google Storage)                        |   |
|   |  • Privacy: Sandboxed, hidden from user Drive files, zero third-party servers       |   |
|   |  • Persistence: Never pauses, never sleeps after inactivity                         |   |
|   +─────────────────────────────────────────────────────────────────────────────────────+   |
|                                                                                             |
|   [ FUTURE PHASE 5 EXTENSION HOOK: WEB COMPANION PORTAL ]                                   |
|   • Cloudflare D1 Edge SQLite activated ONLY if user logs into web portal                   |
|   • Secondary Sync Target with conservative 2–3 Minute Debounce to protect free quotas      |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```

---

## 2. Core Architectural Invariants

1. **The $0 Free-Forever Backend Invariant:**
   * Uses Google Drive's hidden `appDataFolder` API directly via TLS 1.3 HTTP/REST.
   * Zero third-party cloud database subscriptions (e.g. Supabase, Firebase) are required for Phase 4 native cross-device synchronization.
   * Zero risk of project hibernation (eliminating Supabase's 7-day inactivity pause).
2. **The Strict 45-Second Debounce Engine:**
   * When any local mutation occurs (task created, notes typed, due date modified, habit logged), the client arms a **45-second countdown timer**.
   * If any further edits occur before the timer reaches 0, the timer **resets to 45 seconds**.
   * When 45 quiet seconds elapse without mutations, the differential sync payload is generated and uploaded.
   * **Bypass Triggers:** Immediate synchronization triggers on:
     * App launch / foregrounding.
     * Window focus / un-minimizing.
     * App shutdown / window close.
     * Manual 1-tap "Sync Now" button in Settings.
3. **Field-Level Last-Write-Wins (LWW) Conflict Resolution (Option A):**
   * If the same task is edited on both Phone and Linux Desktop while offline, mutations are resolved at the **attribute level** using a column timestamp map (`column_updated_at_utc`).
   * Non-conflicting edits are merged gracefully (e.g. Phone edits title at 14:02:00; Desktop adjusts due date at 14:02:20 $\implies$ both title and due date changes are preserved).
4. **Tombstone Deletion Propagation:**
   * Deleting an item marks `is_deleted = 1` with a `deleted_at_utc` timestamp.
   * Tombstones propagate across paired devices for **30 days** before physical garbage collection (`DELETE FROM tasks WHERE is_deleted = 1 AND deleted_at_utc < now - 30d`).
5. **Phase 5 Web Dual-Sync Compatibility Hook:**
   * The sync engine is architected with a pluggable `CloudSyncAdapter` interface.
   * Google Drive `appDataFolder` is the primary adapter (45s debounce).
   * In Phase 5, when web access is needed, a `CloudflareD1SyncAdapter` can be plugged in alongside Google Drive with a **2–3 minute debounce** to conservatively respect Cloudflare's free write quotas.

---

## 3. Database Schema Delta & Tombstone Tracking

To support differential sync without scanning millions of unchanged rows, the multiplatform Room schema is augmented with sync metadata columns:

### 3.1 Entity Metadata Columns

Every syncable entity (`tasks`, `subtasks`, `lists`, `folders`, `sections`, `habits`, `habit_logs`, `tag_entity`) implements `SyncableEntity`:

```kotlin
package com.flux.done.sync.cloud.model

interface SyncableEntity {
    val id: String // UUID v4 primary key
    val updatedAtUtc: Long // Milliseconds UTC epoch
    val isDeleted: Boolean // Tombstone flag
    val deletedAtUtc: Long? // Tombstone timestamp
    val syncState: SyncState // CLEAN, DIRTY_MUTATED, DIRTY_DELETED
}

enum class SyncState {
    CLEAN,          // Synchronized with cloud
    DIRTY_MUTATED,  // Modified locally; needs push
    DIRTY_DELETED   // Deleted locally; needs tombstone push
}
```

### 3.2 SQL Migration: Sync Tracking Columns

```sql
-- Augment core tables with sync tracking metadata
ALTER TABLE tasks ADD COLUMN updated_at_utc INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000);
ALTER TABLE tasks ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN deleted_at_utc INTEGER DEFAULT NULL;
ALTER TABLE tasks ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'CLEAN';
ALTER TABLE tasks ADD COLUMN field_timestamps_json TEXT DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_tasks_sync_state ON tasks(sync_state);
CREATE INDEX IF NOT EXISTS idx_tasks_updated_at ON tasks(updated_at_utc);
```

---

## 4. The 45-Second Debounced Sync Engine

### 4.1 State Machine & Timer Mechanics

```
[ User Interaction / Local DAO Mutation ]
                 │
                 ▼
    [ SyncScheduler.onMutationOccurred() ]
                 │
                 ├──────────────────────────────┐
                 ▼                              ▼
      [ Is Timer Running? ]          [ Is Bypass Condition? ]
       ├─ YES: Reset timer to 45s      (App close, window focus, Sync Now)
       └─ NO:  Start 45s countdown              │
                 │                              ▼
         [ 45s Elapsed ]              [ Immediate Sync ]
                 │                              │
                 └──────────────┬───────────────┘
                                ▼
         [ Execute Differential Push & Pull ]
```

### 4.2 Implementation Specification (`DebouncedSyncScheduler.kt`)

```kotlin
package com.flux.done.sync.cloud

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DebouncedSyncScheduler(
    private val syncEngine: CloudSyncEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var debounceJob: Job? = null
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    // 45-second debounce countdown
    private val debounceDelayMs = 45_000L

    fun onMutationOccurred() {
        debounceJob?.cancel()
        _syncStatus.value = SyncStatus.PendingDebounce(remainingSeconds = 45)

        debounceJob = scope.launch {
            delay(debounceDelayMs)
            executeSync()
        }
    }

    fun triggerImmediateSync(source: SyncTriggerSource) {
        debounceJob?.cancel()
        scope.launch {
            executeSync(source)
        }
    }

    private suspend fun executeSync(source: SyncTriggerSource = SyncTriggerSource.DebounceTimer) {
        _syncStatus.value = SyncStatus.Syncing(source)
        try {
            syncEngine.performBidirectionalSync()
            _syncStatus.value = SyncStatus.Success(System.currentTimeMillis())
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
        }
    }
}

enum class SyncTriggerSource {
    DebounceTimer,
    AppForeground,
    WindowFocus,
    AppShutdown,
    ManualUserTap
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class PendingDebounce(val remainingSeconds: Int) : SyncStatus
    data class Syncing(val source: SyncTriggerSource) : SyncStatus
    data class Success(val lastSyncedAt: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
```

---

## 5. Field-Level Last-Write-Wins (LWW) Conflict Resolution

When two devices edit the same task while offline, simple row-level LWW would cause one device's work to be completely erased. FluxDone implements **Field-Level LWW** using a granular JSON column map (`field_timestamps_json`).

### 5.1 Field Timestamp Tracking Format

```json
{
  "title": 1791888000000,
  "notes": 1791888000000,
  "due_date": 1791888020000,
  "start_time": 1791888020000,
  "is_completed": 1791887900000,
  "priority": 1791888000000,
  "list_id": 1791888000000
}
```

### 5.2 Merge Resolution Algorithm (`FieldLevelConflictResolver.kt`)

```kotlin
package com.flux.done.sync.cloud.conflict

import com.flux.done.database.entity.TaskEntity
import kotlinx.serialization.json.Json

object FieldLevelConflictResolver {

    fun mergeTaskEntities(
        local: TaskEntity,
        remote: TaskEntity
    ): TaskEntity {
        val localTimestamps = parseTimestamps(local.fieldTimestampsJson)
        val remoteTimestamps = parseTimestamps(remote.fieldTimestampsJson)

        // Resolve each mutable field individually by higher timestamp
        val mergedTitle = if (localTimestamps.getOrDefault("title", 0L) >= remoteTimestamps.getOrDefault("title", 0L)) {
            local.title
        } else {
            remote.title
        }

        val mergedNotes = if (localTimestamps.getOrDefault("notes", 0L) >= remoteTimestamps.getOrDefault("notes", 0L)) {
            local.notes
        } else {
            remote.notes
        }

        val mergedDueDate = if (localTimestamps.getOrDefault("due_date", 0L) >= remoteTimestamps.getOrDefault("due_date", 0L)) {
            local.dueDate
        } else {
            remote.dueDate
        }

        val mergedStartTime = if (localTimestamps.getOrDefault("start_time", 0L) >= remoteTimestamps.getOrDefault("start_time", 0L)) {
            local.startTime
        } else {
            remote.startTime
        }

        val mergedPriority = if (localTimestamps.getOrDefault("priority", 0L) >= remoteTimestamps.getOrDefault("priority", 0L)) {
            local.priority
        } else {
            remote.priority
        }

        val (mergedCompleted, mergedCompletedAt) = if (localTimestamps.getOrDefault("is_completed", 0L) >= remoteTimestamps.getOrDefault("is_completed", 0L)) {
            Pair(local.isCompleted, local.completedAt)
        } else {
            Pair(remote.isCompleted, remote.completedAt)
        }

        val combinedTimestamps = mutableMapOf<String, Long>().apply {
            putAll(localTimestamps)
            remoteTimestamps.forEach { (k, v) ->
                put(k, maxOf(v, getOrDefault(k, 0L)))
            }
        }

        return local.copy(
            title = mergedTitle,
            notes = mergedNotes,
            dueDate = mergedDueDate,
            startTime = mergedStartTime,
            priority = mergedPriority,
            isCompleted = mergedCompleted,
            completedAt = mergedCompletedAt,
            updatedAtUtc = maxOf(local.updatedAtUtc, remote.updatedAtUtc),
            fieldTimestampsJson = Json.encodeToString(combinedTimestamps),
            syncState = SyncState.CLEAN
        )
    }

    private fun parseTimestamps(json: String?): Map<String, Long> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            Json.decodeFromString<Map<String, Long>>(json)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
```

---

## 6. Google Drive `appDataFolder` Storage & Transport Engine

### 6.1 Authentication & Scope Binding

* **OAuth Scope:** `https://www.googleapis.com/auth/drive.appdata`
* **Storage Path:** Hidden root directory `appDataFolder` in user's personal Google Drive.
* **Sync File Structure:**
  * `fluxdone_manifest.json`: Contains sync schema version (`2.0`), client device ID, and `last_sync_utc`.
  * `fluxdone_delta_<epoch>.json`: Differential batch of mutated entities since previous version.
  * `fluxdone_snapshot.db`: Compressed SQLite snapshot written every 10 delta cycles to compact transaction history.

### 6.2 Bidirectional Sync Pipeline (`GoogleDriveSyncEngine.kt`)

```
1. PULL PHASE:
   - Query Google Drive `appDataFolder` for `fluxdone_delta_*.json` created after `last_pulled_utc`.
   - Download remote deltas.
   - For each remote entity:
     - If local does not exist -> Insert directly.
     - If local exists -> Apply `FieldLevelConflictResolver.merge()`.
     - If remote is tombstone (`is_deleted = 1`) -> Soft delete locally.
   - Save highest remote timestamp as new `last_pulled_utc`.

2. PUSH PHASE:
   - Query Room DB for entities where `sync_state != 'CLEAN'`.
   - If zero dirty entities exist -> Sync complete.
   - Package dirty entities into `fluxdone_delta_<clientEpoch>.json`.
   - Upload file to Google Drive `appDataFolder`.
   - Mark local entities as `sync_state = 'CLEAN'`.
```

---

## 7. Future Phase 5 Dual-Sync Architecture Hook

To seamlessly accommodate the **Phase 5 Web Companion Portal** without breaking Phase 4:

```kotlin
package com.flux.done.sync.cloud.adapter

interface CloudSyncAdapter {
    val adapterId: String
    val debounceIntervalMs: Long
    suspend fun pushDelta(delta: SyncDeltaPayload): Result<Unit>
    suspend fun pullRemoteDeltas(sinceEpoch: Long): Result<SyncDeltaPayload>
}

// 1. Primary Phase 4 Adapter (Android & Desktop Linux)
class GoogleDriveAppDataSyncAdapter : CloudSyncAdapter {
    override val adapterId = "google_drive_appdata"
    override val debounceIntervalMs = 45_000L // Strict 45s debounce
    // ...
}

// 2. Future Phase 5 Adapter (Activated ONLY if user logs into Web Portal)
class CloudflareD1SyncAdapter : CloudSyncAdapter {
    override val adapterId = "cloudflare_d1_edge"
    override val debounceIntervalMs = 150_000L // Conservative 2.5m debounce to conserve free quotas
    // ...
}
```

---

## 8. Verification & Testing Contracts

1. **The 45-Second Debounce Test:** Edit a task title at $T=0$. Verify sync status is `PendingDebounce(45)`. At $T=30$, edit task notes. Verify countdown resets to 45s. Verify that zero network requests are dispatched until $T=75$s.
2. **Field-Level LWW Non-Conflicting Merge Test:**
   * Device 1 (offline): Updates task title to *"Revise Incenter Lemmas"*.
   * Device 2 (offline): Reschedules same task due date to *"Tomorrow 16:00"*.
   * Bring both online and trigger sync.
   * **Verification:** Final merged task retains title *"Revise Incenter Lemmas"* AND due date *"Tomorrow 16:00"*.
3. **Tombstone Replication Test:** Delete a task on Phone. Sync to Google Drive. Pull on Linux Desktop. Verify the task row on Linux is soft-deleted (`is_deleted = 1`) and hidden from the task stream, without throwing foreign key violations.
4. **Offline Resilience Test:** Disconnect Wi-Fi. Create 5 tasks and 2 checklists. Reconnect Wi-Fi. After 45 seconds, verify all 5 tasks and their subtasks are cleanly uploaded to `appDataFolder` and marked `sync_state = 'CLEAN'`.

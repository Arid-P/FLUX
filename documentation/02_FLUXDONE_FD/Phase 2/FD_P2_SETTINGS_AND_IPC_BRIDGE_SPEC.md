# ⚙️ Topic FD-P2-6: Settings Screen & FluxFoxus (FF) IPC Bridge Specification

**Document ID:** `FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC`  
**Phase:** Phase 2 (Advanced Ergonomics, Settings & Inter-App IPC)  
**Target Package:** `com.flux.done.features.settings` & `com.flux.done.core.ipc`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Android Explicit Broadcast IPC + DataStore Preferences)  
**Screen References:** SCR-09 (Settings Screen)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari’s unified productivity ecosystem, **FluxDone (FD)** manages *what* needs to be done, while **FluxFoxus (FF)** enforces *discipline* during deep study sessions. Decoupling the two applications ensures that FluxDone remains an ultra-fast, lightweight task manager without bloat, while maintaining seamless real-time interoperability with FluxFoxus.

**Topic FD-P2-6 establishes the Master Settings Surface (SCR-09) and the Live Inter-App IPC Bridge.**  
It delivers complete customization and inter-app synchronization:
* **Central Settings Surface (SCR-09):** Grouped configuration covering Appearance, Smart Lists visibility, Task Ergonomics, Notifications, Data Backup, and Account profile.
* **Account Landing Zone:** A dedicated Account section reserving space for Google Sign-In and Cloud Synchronization. In Phase 2, it displays the local offline profile (*"Local Profile (Ari) — Cloud Sync coming in Phase 4"*), establishing an architectural anchor for Phase 4 without dummy code.
* **Curated Swipe Gesture Customization:** Allows Ari to customize Left Swipe (`Move to Trash` [default], `Mark Won't Do`, `Complete`) and Right Swipe (`Reschedule Tomorrow` [default], `Mark Done`, `Snooze 1h`).
* **Configurable Trash Retention:** Sets auto-purge countdown duration (**30 days default**, 7d, 14d, 60d, Never).
* **Live FluxFoxus IPC Bridge (`FocusBlockBroadcastSender.kt`):** Automatically dispatches `com.flux.action.FOCUS_BLOCK_SYNC` explicit broadcasts to `com.fluxfoxus` whenever timed study blocks are created, rescheduled, or deleted.
* **Configurable IPC Sync Delay & Rate Limiting:** When tasks are rescheduled on the Calendar canvas, an adjustable delay buffer (**Minimum 15 seconds**, Default 30 seconds, **Maximum 5 minutes**) prevents rapid broadcast hammering during active schedule reorganization.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Inter-App IPC Master** | [`IPC_SPEC_FD_FF_NATIVE.md`](file:///home/hp/Flux_Prompts/documentation/01_INTEGRATION_SYNC/IPC_SPEC_FD_FF_NATIVE.md) | Conforms to `com.flux.action.FOCUS_BLOCK_SYNC` Intent contract with `EXTRA_VERSION = 1`. |
| **FluxFoxus Receiver** | [`FF_TRD_KOTLIN.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_TRD_KOTLIN.md) | Target receiver is `com.fluxfoxus.core.ipc.FdIntegrationReceiver` running `goAsync()`. |
| **Calendar View** | [`FD_P1_CALENDAR_TIMELINE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_CALENDAR_TIMELINE_SPEC.md) | Drag-to-reschedule triggers the debounced IPC sync pipeline. |
| **Smart Lists** | [`FD_P1_SMART_LISTS_AND_TRASH_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_SMART_LISTS_AND_TRASH_SPEC.md) | Smart list visibility toggles in Settings sync bidirectionally with the Drawer `[ ✎ ]` shortcut. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Settings Screen Layout (`SettingsScreen.kt` — SCR-09)

Accessible via the Navigation Drawer footer or bottom navigation bar:

```
+-----------------------------------------------------------------+
|  ←  SETTINGS                                                    |
+-----------------------------------------------------------------+
|  ACCOUNT & PROFILE                                              |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  👤 Local Profile: Ari                                     │ |
|  │     Offline Storage Active • Phase 4 Cloud Sync Reserved   │ |
|  │     [ Google Sign-In & Cloud Sync (Phase 4) → ]            │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  APPEARANCE & GENERAL                                           |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  🌓 Theme Mode: System Default (Dark / Light / System)      │ |
|  │  📅 First Day of Week: Monday                              │ |
|  │  🕒 Time Format: 24-Hour (16:00)                           │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  SMART LISTS MANAGEMENT                                         |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  📋 Customize Smart Lists (Reorder & Toggle Visibility)    │ |
|  │     Today (Locked) • Tomorrow • Upcoming • All • Completed │ |
|  │     [ Edit Smart Lists → ]                                 │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  TASK ERGONOMICS & GESTURES                                     |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  👈 Left Swipe Action: [ Move to Trash ▾ ]                 │ |
|  │  👉 Right Swipe Action: [ Reschedule Tomorrow ▾ ]           │ |
|  │  ✂️ Remove Parsed NLP Tokens from Title: [ ON ]            │ |
|  │  🗑️ Trash Auto-Purge Duration: [ 30 Days ▾ ]                │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  FLUXFOXUS (FF) INTER-APP BRIDGE                                |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  ● FluxFoxus Active (IPC Synced)         [ Test Sync 🔄 ]   │ |
|  │    Target Package: com.fluxfoxus                           │ |
|  │                                                            │ |
|  │  Auto-Sync Focus Blocks: [ ON ]                            │ |
|  │  Schedules FF focus blocks on timed task creation/update   │ |
|  │                                                            │ |
|  │  Reschedule Sync Delay Buffer: [ 30 Seconds ]              │ |
|  │  ──●──────────────────────────────────────                 │ |
|  │  Min: 15s • Current: 30s • Max: 5 min                      │ |
|  │  Prevents sync bursts while dragging blocks on calendar    │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  NOTIFICATIONS & BACKUP                                         |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  🔔 Notification Settings (Agenda, Reminders, Channels)    │ |
|  │  💾 Backup & Export (SCR-13 — JSON & SQLite Snapshots)     │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  ABOUT                                                          |
|  FluxDone v2.0.0 • Native Android Kotlin • Built for Ari        |
+-----------------------------------------------------------------+
```

---

### 3.2 Key Settings Modules & User Interactions

#### 1. Account & Profile Section
* Displays the active local user profile (*"Ari"*).
* Shows Google Account status row: In Phase 2, this row is clearly labeled as an architectural placeholder:  
  *"Local Storage Only (Offline) • Google Account & Cloud Sync coming in Phase 4"*.
* Tapping opens an informational modal explaining Phase 4 multi-device sync, ensuring zero ambiguity.

#### 2. Task Ergonomics & Swipe Gesture Pickers
* **Left Swipe Picker:**
  * `[ Move to Trash ]` (Default — soft-deletes with 5s undo snackbar)
  * `[ Mark Won't Do ]` (Marks skipped, skips recurring instance)
  * `[ Complete Task ]` (Marks complete with subtask invariant)
* **Right Swipe Picker:**
  * `[ Reschedule Tomorrow ]` (Default — shifts due date by +1 day)
  * `[ Mark Done ]` (Marks complete with subtask invariant)
  * `[ Snooze 1 Hour ]` (Shifts start/due time by +60 minutes)
* **Trash Retention Duration:**
  * Dropdown selector: `7 Days`, `14 Days`, **`30 Days (Default)`**, `60 Days`, `Never (Manual Only)`.

#### 3. FluxFoxus Inter-App Bridge Cockpit
* **Dynamic Package Detection:**
  * Queries `packageManager.getPackageInfo("com.fluxfoxus", 0)` on screen resume:
    * **Installed:** Displays bold green badge `● FluxFoxus Active (IPC Synced)`.
    * **Not Installed:** Displays gray badge `○ FluxFoxus Not Installed (Offline Mode)`.
* **Diagnostics Ping Button (`[ Test Sync 🔄 ]`):**
  * Dispatches an explicit test broadcast:
    `com.flux.action.FOCUS_BLOCK_SYNC` with `action = "TEST_PING"`.
  * Shows Toast / Snackbar: *"Diagnostic broadcast dispatched to FluxFoxus"*.
* **Reschedule Sync Delay Slider:**
  * Slider with snap points: `15s`, `30s`, `45s`, `1m`, `2m`, `3m`, `5m`.
  * User can fine-tune between **15 seconds (minimum)** and **5 minutes (maximum)**.
  * Allows Ari to freely drag and adjust study blocks on the Calendar timeline without triggering premature focus blocks in FluxFoxus.

---

## 4. Native IPC Bridge Implementation (`FocusBlockBroadcastSender.kt`)

### 4.1 Sender Architecture & Rate-Limited Debounce Pipeline

```kotlin
package com.flux.done.core.ipc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.flux.done.core.database.TaskEntity
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

enum class IpcAction {
    CREATE,
    UPDATE,
    DELETE,
    TEST_PING
}

@Serializable
data class FocusBlockIpcPayload(
    val version: Int = 1,
    val action: String,
    val taskId: String,
    val taskName: String,
    val startTimeEpochMs: Long?,
    val endTimeEpochMs: Long?,
    val durationMinutes: Int?,
    val listId: String?,
    val colorHex: String?,
    val presetId: String? = null
)

class FocusBlockBroadcastSender(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    companion object {
        const val ACTION_FOCUS_BLOCK_SYNC = "com.flux.action.FOCUS_BLOCK_SYNC"
        const val TARGET_PACKAGE = "com.fluxfoxus"
        const val EXTRA_VERSION = "EXTRA_VERSION"
        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_END_TIME = "EXTRA_END_TIME"
        const val EXTRA_DURATION_MIN = "EXTRA_DURATION_MIN"
        const val EXTRA_LIST_ID = "EXTRA_LIST_ID"
        const val EXTRA_COLOR_HEX = "EXTRA_COLOR_HEX"
        const val EXTRA_PAYLOAD_JSON = "EXTRA_PAYLOAD_JSON"
    }

    private val pendingSyncJobs = ConcurrentHashMap<String, Job>()
    private val json = Json { ignoreUnknownKeys = true }

    // Check if FluxFoxus is installed on the device
    fun isFluxFoxusInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TARGET_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Immediate Broadcast (Used for Creation, Deletion, and Test Ping)
    fun dispatchImmediate(task: TaskEntity, action: IpcAction, colorHex: String? = null) {
        // Cancel any pending debounced sync for this task
        pendingSyncJobs.remove(task.id)?.cancel()
        sendBroadcastIntent(task, action, colorHex)
    }

    // Debounced Broadcast (Used for Calendar Drag-to-Reschedule)
    fun scheduleDebouncedRescheduleSync(
        task: TaskEntity,
        delaySeconds: Int,
        colorHex: String? = null
    ) {
        // Cancel existing pending job for this task to restart debounce timer
        pendingSyncJobs.remove(task.id)?.cancel()

        val clampedDelay = delaySeconds.coerceIn(15, 300) // Strictly 15s to 5 mins
        val job = coroutineScope.launch {
            delay(clampedDelay * 1000L)
            sendBroadcastIntent(task, IpcAction.UPDATE, colorHex)
            pendingSyncJobs.remove(task.id)
        }
        pendingSyncJobs[task.id] = job
    }

    private fun sendBroadcastIntent(task: TaskEntity, action: IpcAction, colorHex: String?) {
        val payload = FocusBlockIpcPayload(
            version = 1,
            action = action.name,
            taskId = task.id,
            taskName = task.title,
            startTimeEpochMs = task.start_time,
            endTimeEpochMs = task.end_time,
            durationMinutes = task.duration_minutes,
            listId = task.list_id,
            colorHex = colorHex
        )

        val intent = Intent(ACTION_FOCUS_BLOCK_SYNC).apply {
            setPackage(TARGET_PACKAGE)
            putExtra(EXTRA_VERSION, 1)
            putExtra(EXTRA_ACTION, action.name)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_NAME, task.title)
            putExtra(EXTRA_START_TIME, task.start_time ?: 0L)
            putExtra(EXTRA_END_TIME, task.end_time ?: 0L)
            putExtra(EXTRA_DURATION_MIN, task.duration_minutes ?: 0)
            putExtra(EXTRA_LIST_ID, task.list_id)
            putExtra(EXTRA_COLOR_HEX, colorHex ?: "")
            putExtra(EXTRA_PAYLOAD_JSON, json.encodeToString(FocusBlockIpcPayload.serializer(), payload))
        }

        context.sendBroadcast(intent)
    }

    // Test Diagnostics Ping
    fun dispatchTestPing() {
        val intent = Intent(ACTION_FOCUS_BLOCK_SYNC).apply {
            setPackage(TARGET_PACKAGE)
            putExtra(EXTRA_VERSION, 1)
            putExtra(EXTRA_ACTION, IpcAction.TEST_PING.name)
            putExtra(EXTRA_TASK_ID, "test-diagnostic-ping")
            putExtra(EXTRA_TASK_NAME, "FluxDone Diagnostic Ping")
        }
        context.sendBroadcast(intent)
    }
}
```

---

### 4.2 Integration into `TaskRepository.kt`

```kotlin
// In TaskRepositoryImpl:
override suspend fun updateTaskTimeBounds(taskId: String, newStart: Long, newEnd: Long, duration: Int) {
    taskDao.updateTimeBounds(taskId, newStart, newEnd, duration, System.currentTimeMillis())
    
    val task = taskDao.getTaskById(taskId) ?: return
    val autoSyncEnabled = settingsRepository.isIpcAutoSyncEnabled()
    if (autoSyncEnabled && focusBlockSender.isFluxFoxusInstalled()) {
        val delaySecs = settingsRepository.getIpcSyncDelaySeconds()
        val list = listDao.getListById(task.list_id)
        focusBlockSender.scheduleDebouncedRescheduleSync(task, delaySecs, list?.color_hex)
    }
}

override suspend fun deleteTask(taskId: String) {
    val task = taskDao.getTaskById(taskId)
    taskDao.softDeleteTask(taskId, System.currentTimeMillis())
    
    // Deletions dispatch immediately without delay to cancel focus blocks in FF
    if (task != null && task.start_time != null && settingsRepository.isIpcAutoSyncEnabled()) {
        focusBlockSender.dispatchImmediate(task, IpcAction.DELETE)
    }
}
```

---

## 5. MVI Architecture & DataStore Keys

### 5.1 Settings DataStore Keys (`SettingsPreferencesKeys.kt`)

```kotlin
package com.flux.done.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsPreferencesKeys {
    val THEME_MODE = stringPreferencesKey("setting_theme_mode") // "SYSTEM", "DARK", "LIGHT"
    val FIRST_DAY_OF_WEEK = stringPreferencesKey("setting_first_day_of_week") // "MONDAY", "SUNDAY"
    
    // Ergonomics
    val SWIPE_LEFT_ACTION = stringPreferencesKey("setting_swipe_left_action") // "TRASH", "WONT_DO", "COMPLETE"
    val SWIPE_RIGHT_ACTION = stringPreferencesKey("setting_swipe_right_action") // "TOMORROW", "DONE", "SNOOZE_1H"
    val STRIP_NLP_TOKENS = booleanPreferencesKey("setting_strip_nlp_tokens") // default: true
    val TRASH_RETENTION_DAYS = intPreferencesKey("setting_trash_retention_days") // 7, 14, 30, 60, -1 (Never)

    // Inter-App IPC Bridge
    val IPC_AUTO_SYNC_ENABLED = booleanPreferencesKey("setting_ipc_auto_sync_enabled") // default: true
    val IPC_SYNC_DELAY_SECONDS = intPreferencesKey("setting_ipc_sync_delay_seconds") // 15 to 300, default: 30
}
```

### 5.2 Settings MVI State (`SettingsViewModel.kt`)

```kotlin
package com.flux.done.features.settings

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val firstDayOfWeek: String = "MONDAY",
    val swipeLeftAction: String = "TRASH",
    val swipeRightAction: String = "TOMORROW",
    val stripNlpTokens: Boolean = true,
    val trashRetentionDays: Int = 30,
    val isFluxFoxusInstalled: Boolean = false,
    val isIpcAutoSyncEnabled: Boolean = true,
    val ipcSyncDelaySeconds: Int = 30,
    val showAccountInfoDialog: Boolean = false,
    val feedbackSnackbar: String? = null
)

sealed interface SettingsIntent {
    data class UpdateThemeMode(val mode: String) : SettingsIntent
    data class UpdateFirstDayOfWeek(val day: String) : SettingsIntent
    data class UpdateSwipeLeftAction(val action: String) : SettingsIntent
    data class UpdateSwipeRightAction(val action: String) : SettingsIntent
    data class SetStripNlpTokens(val strip: Boolean) : SettingsIntent
    data class SetTrashRetentionDays(val days: Int) : SettingsIntent
    data class SetIpcAutoSyncEnabled(val enabled: Boolean) : SettingsIntent
    data class SetIpcSyncDelaySeconds(val seconds: Int) : SettingsIntent
    object TriggerTestPing : SettingsIntent
    object ShowAccountDialog : SettingsIntent
    object DismissAccountDialog : SettingsIntent
    object DismissSnackbar : SettingsIntent
}
```

---

## 6. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **Rapid Calendar Block Dragging** | If Ari drags a block 3 times within 10 seconds, `scheduleDebouncedRescheduleSync` cancels the prior coroutines. Exactly **one** `UPDATE` broadcast is dispatched after the configured delay (e.g. 30s) stabilizes. |
| **Task Deleted During Pending Reschedule Delay** | If a task is moved and then immediately deleted before the 30s delay expires, `dispatchImmediate(DELETE)` cancels the pending debounced update and broadcasts `DELETE` instantly. |
| **FluxFoxus Not Installed** | `isFluxFoxusInstalled()` returns `false`. Settings renders gray offline badge; `FocusBlockBroadcastSender` suppresses broadcasts to avoid OS intent dispatch overhead. |
| **Account Section Scope** | Clearly documented as the landing zone for Phase 4. Displays user's offline name (*"Ari"*) with informational dialog stating that Google Sign-In and cloud multi-device synchronization arrive in Phase 4. Zero dead code. |
| **Trash Retention Duration Set to Never** | If set to `-1` (Never), the background auto-purge query in `TrashDao` is bypassed, keeping trashed items until Ari manually taps "Empty Trash". |
| **IPC Rate Limiting on Receiver (FF)** | Handled symmetrically: `FdIntegrationReceiver` in FluxFoxus enforces burst throttling and deduplicates consecutive payloads sharing the same `taskId` and timestamps. |

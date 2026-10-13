# 🔄 Topic FD-P4-2: Bidirectional Multi-Device Sync with FluxFoxus Specification

**Document ID:** `FD_P4_CROSS_DEVICE_FF_FD_SYNC_SPEC`  
**Phase:** Phase 4 (Cloud Sync, Cross-Device Ecosystem & Advanced Notes)  
**Target Package:** `com.flux.done.integration.ff` (Android) & `com.flux.done.desktop.integration` (Linux)  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Room 2.7+ KMP + Local IPC Broadcasts + Cloud Sync Relay)  
**Target Platforms:** Android Phone & Tablet (`com.fluxdone`), Ubuntu Linux Workstation (`com.fluxdone.desktop`)  
**Prerequisites:** [`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md), [`FD_P4_CLOUD_SYNC_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%204/FD_P4_CLOUD_SYNC_ENGINE_SPEC.md), & [`IPC_SPEC_FD_FF_NATIVE.md`](file:///home/hp/Flux_Prompts/prompts_v2/01_INTEGRATION_SYNC/IPC_SPEC_FD_FF_NATIVE.md)  

---

## 1. Overview & Architectural Philosophy

Within the Flux ecosystem, **FluxDone (FD)** is the *Architect* (organizing tasks, syllabi, and daily time-blocks), while **FluxFoxus (FF)** is the *Enforcer* (locking distractions and driving deep academic focus). 

On a single device, they communicate via local Android IPC and Linux DBus. In Phase 4, this bridge extends across Ari's **entire multi-device ecosystem**:
1. **Unified Scheduling:** Scheduling a timed study block in FluxDone (on Phone or Linux Desktop) automatically registers a scheduled focus session in FluxFoxus on **all active paired devices**.
2. **Strict Automatic Start Invariant:** Focus blocks are **not optional suggestions**. When the scheduled start time arrives, FluxFoxus **automatically engages** the focus session (preceded by a 60-second warning countdown), eliminating procrastination.
3. **Session Completion $\implies$ Task Auto-Complete:** When a focus block finishes in FluxFoxus, FluxDone automatically marks the linked task complete (`is_completed = 1`), atomizes completion to all child subtasks (**Mandatory Subtask Cascade**), and synchronizes the completed state across all devices.
4. **Early Quit Audit Tracking:** If a focus session is interrupted or aborted early in FluxFoxus, the linked task in FluxDone remains active (`is_completed = 0`), and an audit log note is appended to the task so Ari can reschedule or resume.

---

## 2. Multi-Device IPC Topology

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                          CROSS-DEVICE FD <-> FF ECOSYSTEM TOPOLOGY                          |
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                             |
|   ┌─────────────────────────────────────────┐     ┌─────────────────────────────────────┐   |
|   │             ANDROID PHONE               │     │           UBUNTU WORKSTATION        │   |
|   │ ┌─────────────┐         ┌─────────────┐ │     │ ┌─────────────┐     ┌─────────────┐ │   |
|   │ │  FluxDone   │◄─ Local ┤  FluxFoxus  │ │     │ │  FluxDone   │◄─DBus┤  FluxFoxus  │ │   |
|   │ │  (Client)   │   IPC   │  (Enforcer) │ │     │ │  (Desktop)  │     │  (Daemon)   │ │   |
|   │ └──────┬──────┘         └─────────────┘ │     │ └──────┬──────┘     └─────────────┘ │   |
|   └────────┼────────────────────────────────┘     └────────┼────────────────────────────┘   |
|            │                                               │                                |
|            │   [ Differential Sync / Manifest Relay ]      │                                |
|            ▼                                               ▼                                |
|   +─────────────────────────────────────────────────────────────────────────────────────+   |
|   |                  GOOGLE DRIVE HIDDEN APP DATA FOLDER (`appDataFolder`)              |   |
|   |  • Relays `focus_block_schedule` & `session_completion_events` across devices        |   |
|   |  • Monotonic timestamping prevents double-completion and stale updates              |   |
|   +─────────────────────────────────────────────────────────────────────────────────────+   |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```

---

## 3. The Scheduled Focus Block & Automatic Start Pipeline

### 3.1 Strict Automatic Start Invariant

When Ari schedules a timed task in FluxDone with explicit start and end times (e.g. `start_time = 16:00`, `end_time = 17:30`, title: *"Olympiad Geometry"*):
1. **No Manual Opt-In Required:** The focus session does **not** wait for Ari to tap an acceptance button. Discipline enforcement requires zero friction to start and high friction to quit.
2. **The 60-Second Grace Warning:** Exactly 60 seconds before the scheduled start time ($15:59:00$), both Android and Linux emit an urgent audible chime and Heads-Up Notification:
   > *"🎯 Focus Block Starting in 60 seconds: 'Olympiad Geometry' (90 min). Save your work and prepare."*
3. **Hard Lock Engagement:** At $16:00:00$, FluxFoxus automatically locks distractions, engages the active preset whitelist, and starts the timer.

### 3.2 Broadcast Contract: `FOCUS_BLOCK_SYNC`

Dispatched by FluxDone whenever a timed task is created, rescheduled, or moved on the calendar timeline (debounced by 15s to 5m per [`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md)):

```kotlin
package com.flux.done.integration.ff

import android.content.Context
import android.content.Intent

object FluxFoxusBroadcastDispatcher {
    const val ACTION_FOCUS_BLOCK_SYNC = "com.flux.action.FOCUS_BLOCK_SYNC"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_START_TIME_EPOCH = "start_time_epoch"
    const val EXTRA_END_TIME_EPOCH = "end_time_epoch"
    const val EXTRA_DOMAIN_TAG = "domain_tag"
    const val EXTRA_PRESET_NAME = "preset_name"

    fun broadcastScheduledBlock(
        context: Context,
        taskId: String,
        title: String,
        startEpochMs: Long,
        endEpochMs: Long,
        domainTag: String?,
        presetName: String = "Academic Deep Work"
    ) {
        val intent = Intent(ACTION_FOCUS_BLOCK_SYNC).apply {
            setPackage("com.fluxfoxus")
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_START_TIME_EPOCH, startEpochMs)
            putExtra(EXTRA_END_TIME_EPOCH, endEpochMs)
            putExtra(EXTRA_DOMAIN_TAG, domainTag)
            putExtra(EXTRA_PRESET_NAME, presetName)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        context.sendBroadcast(intent)
    }
}
```

---

## 4. Session Completion & Mandatory Subtask Cascade Pipeline

### 4.1 Successful Session Completion Flow (Decision 1.A)

When a linked focus block successfully finishes in FluxFoxus (e.g. 90 full minutes elapsed):
1. **FF Broadcast Dispatched:** FluxFoxus broadcasts `com.flux.action.FOCUS_SESSION_COMPLETED`.
2. **FluxDone Ingestion:** FluxDone receives the broadcast via `FocusSessionCompletedReceiver`.
3. **Atomic Task Completion:**
   * `tasks.is_completed = 1`
   * `tasks.completed_at = System.currentTimeMillis()`
4. **Mandatory Subtask Cascade:** All child subtasks belonging to `task_id` are atomically set to `is_completed = 1`.
5. **Audit Metadata Appended:** A formatted completion stamp is prepended to the task notes:
   ```markdown
   > [!NOTE]
   > ✅ **Focus Session Completed:** 90m deep work session logged via FluxFoxus on 13 Oct 2026, 17:30.
   ```
6. **Cross-Device Sync Propagation:** The updated task is marked `sync_state = 'DIRTY_MUTATED'` and pushed to Google Drive `appDataFolder` during the next sync cycle, automatically completing the task on all paired devices.

### 4.2 Implementation Specification (`FocusSessionCompletedReceiver.kt`)

```kotlin
package com.flux.done.integration.ff

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flux.done.database.dao.TaskDao
import com.flux.done.sync.cloud.DebouncedSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusSessionCompletedReceiver(
    private val taskDao: TaskDao,
    private val syncScheduler: DebouncedSyncScheduler
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.flux.action.FOCUS_SESSION_COMPLETED") return

        val taskId = intent.getStringExtra("task_id") ?: return
        val durationMinutes = intent.getIntExtra("duration_minutes", 0)

        CoroutineScope(Dispatchers.IO).launch {
            // Atomic check-off with Mandatory Subtask Cascade
            taskDao.completeTaskWithSubtaskCascade(
                taskId = taskId,
                completedAtUtc = System.currentTimeMillis(),
                auditNote = "\n> [!NOTE]\n> ✅ **Focus Session Completed:** ${durationMinutes}m deep work logged via FluxFoxus.\n"
            )
            // Trigger 45s cloud sync countdown
            syncScheduler.onMutationOccurred()
        }
    }
}
```

---

## 5. Early Quits & Incomplete Sessions Pipeline (Decision 2.A)

Academic plans do not always go smoothly. If Ari emergency-unlocks, pauses past break limits, or quits the session prematurely:

1. **FF Broadcast Dispatched:** FluxFoxus broadcasts `com.flux.action.FOCUS_SESSION_ABORTED`.
2. **Task State Preserved:** FluxDone strictly keeps the task **active** (`is_completed = 0`). It is **not** checked off.
3. **Audit Reflection Note Appended:** An audit note detailing the actual time achieved is appended to the task's notes:
   ```markdown
   > [!WARNING]
   > ⚠️ **Focus Block Incomplete:** 22m of 90m completed. Session terminated prematurely on 13 Oct 2026, 16:22.
   ```
4. **UI Attention Badge:** On SCR-01 (Task Stream) and SCR-04 (Calendar Timeline), the task badge indicates an incomplete session (`⚠️ 22/90m`), alerting Ari during the 9:00 PM End-of-Day Review to either reschedule or complete the remaining work.

---

## 6. Linux Desktop DBus Bridge (`FluxFoxusDBusBridge.kt`)

On Ubuntu Linux workstations, inter-process communication between `fluxdone.desktop` and `fluxfoxus-daemon.service` utilizes the system/session DBus interface:

### 6.1 DBus Interface Definition

```kotlin
package com.flux.done.desktop.integration

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface

@DBusInterfaceName("org.flux.foxus.FocusService")
interface FluxFoxusDaemonInterface : DBusInterface {
    fun ScheduleFocusBlock(
        taskId: String,
        taskTitle: String,
        startEpochMs: Long,
        endEpochMs: Long,
        domainTag: String,
        presetName: String
    )

    fun CancelScheduledBlock(taskId: String)
}
```

### 6.2 Linux DBus Signal Handler

```kotlin
package com.flux.done.desktop.integration

import com.flux.done.database.dao.TaskDao
import com.flux.done.sync.cloud.DebouncedSyncScheduler
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal

class LinuxFocusSessionCompletedSignal(
    path: String,
    val taskId: String,
    val durationMinutes: Int
) : DBusSignal(path)

class DesktopDBusBridgeManager(
    private val sessionConnection: DBusConnection,
    private val taskDao: TaskDao,
    private val syncScheduler: DebouncedSyncScheduler
) {
    fun registerSignalListeners() {
        sessionConnection.addSigHandler(
            LinuxFocusSessionCompletedSignal::class.java,
            DBusSigHandler { signal ->
                taskDao.completeTaskWithSubtaskCascade(
                    taskId = signal.taskId,
                    completedAtUtc = System.currentTimeMillis(),
                    auditNote = "\n> [!NOTE]\n> ✅ **Desktop Focus Session Completed:** ${signal.durationMinutes}m deep work.\n"
                )
                syncScheduler.onMutationOccurred()
            }
        )
    }
}
```

---

## 7. Active Discipline Dominance on the Desktop Timeline

While a linked focus session is actively running in FluxFoxus on any paired device:

1. **Timeline Block Lock:** On the 24-hour Canvas Timeline (SCR-04 / `DesktopWeekTimelineScreen`), the active task block is **locked against mouse dragging, resizing, or deletion**.
2. **Visual Status Styling:** The active block renders an animated pulsing border (`#1565C0` 2dp stroke) with an active focus pill (`[ ⏳ Deep Work In Progress ]`).
3. **Task Inspector Read-Only:** Notes and checklist subtasks remain viewable, but modifying the task title or due date is disabled until the session concludes.

---

## 8. Verification & Testing Contracts

1. **Automatic Start Verification Test:** Create a task in FluxDone scheduled for $T + 120\text{s}$. Verify that at $T + 60\text{s}$, the 60-second warning heads-up chime fires. At $T + 120\text{s}$, verify that FluxFoxus automatically enters full-screen lock mode without requiring any screen taps or button clicks.
2. **Session Completion Cascade Test:** Link a task with 4 child subtasks to a 90m focus session. Trigger completion in FF. Verify in FluxDone SQLite that:
   - `tasks.is_completed = 1`
   - All 4 `subtasks.is_completed = 1`
   - Task notes contain the markdown block `✅ **Focus Session Completed:** 90m deep work`.
3. **Early Quit Audit Note Test:** Start a 90m session. Abort after 15m. Query FluxDone database; verify `tasks.is_completed = 0` and task notes contain `⚠️ **Focus Block Incomplete:** 15m of 90m completed`.
4. **Cross-Device Relay Test:** Schedule a block on Ubuntu Desktop. Trigger Google Drive cloud sync. Open Android Phone; verify the scheduled session appears in FluxFoxus AlarmManager queue within the scheduled timeframe.

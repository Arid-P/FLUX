# 🛎️ Topic FD-P3-4: Desktop System Tray, Background Daemon & Floating Quick Add Specification

**Document ID:** `FD_P3_SYSTEM_TRAY_GLOBAL_ADD_SPEC`  
**Phase:** Phase 3 (Ubuntu Linux Desktop Companion via Compose Multiplatform)  
**Target Package:** `com.flux.done.desktop.tray`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Compose Multiplatform 1.6+ + AppIndicator / DBus Native Integration)  
**Target OS:** Ubuntu Linux Workstation (GNOME 42+, X11 & Wayland, JVM 17+)  
**Prerequisites:** [`FD_P3_KEYBOARD_SHORTCUTS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%203/FD_P3_KEYBOARD_SHORTCUTS_SPEC.md) & [`FD_P3_SHARED_DATABASE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%203/FD_P3_SHARED_DATABASE_SPEC.md)  

---

## 1. Overview & Core Philosophy

On an Ubuntu Linux workstation, productivity software must seamlessly blend into the desktop environment. Re-opening a heavyweight application every time a thought or task occurs creates friction.

**Topic FD-P3-4 establishes the Desktop System Tray, Background Lifecycle Daemon, and Floating Quick Add Popup for FluxDone.**

### Core Architectural Decisions & Invariants:
1. **Ubuntu GNOME AppIndicator Tray:** Resident system tray icon in the GNOME top bar showing real-time pending task counts for Today.
2. **Window Close Behavior (`[ ✕ ]` Minimizes to Tray):** Clicking `[ ✕ ]` hides the main window into the background while keeping the global summon hotkey (`Ctrl + Alt + Space`) instantly responsive.
3. **The Strict Suspend / Lock Invariant (Zero-Footprint Guarantee):**
   > *"On suspend or similar state (user has to unlock the laptop) then it is off, no process and nothing."*
   The instant GNOME screen lock or system suspend is signaled over DBus, FluxDone executes a synchronous SQLite WAL checkpoint, unregisters all native handles, and **immediately terminates the process (`System.exit(0)`)**. While your laptop is locked or asleep, **literally zero CPU, zero battery, and zero processes exist**.
4. **Floating Quick Add Popup:** Undecorated, centered 560dp floating capture bar powered by the Phase 2 NLP tokenizer ([`FD_P2_QUICK_CAPTURE_NLP_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_QUICK_CAPTURE_NLP_SPEC.md)).

---

## 2. Desktop System Tray Specification (`FluxDoneTrayService.kt`)

### 2.1 GNOME AppIndicator Integration Architecture

On modern Ubuntu (GNOME Shell), standard AWT `SystemTray` is frequently deprecated or hidden. FluxDone utilizes `libayatana-appindicator3` via JNA to bind natively to the GNOME Top Bar notification area:

```
+─────────────────────────────────────────────────────────────────────────────+
| Ubuntu Top Bar:  [ Activities ]         14:30           [ 🌐 ] [ 🛎️ (5) ] [ ⏻ ] |
+─────────────────────────────────────────────────────────────────────────────+
                                                                  │
                                                     Right Click  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 📥 FluxDone — 5 tasks pending today                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│ 🖥️  Open Main Workspace                                      Ctrl + 1       │
│ ➕  Quick Add Task...                                         Ctrl+Alt+Space │
├─────────────────────────────────────────────────────────────────────────────┤
│ 📅  Today's Agenda                                           ►              │
│     ├── [✓] Angle Chasing Lemma (Done)                                      │
│     ├── [ ] Solve 5 Incenter Problems (16:00)                               │
│     ├── [ ] Simson Line Proof                                               │
│     ├── [ ] Revise Incenter Formulas                                        │
│     └── [ ] Cyclics Drill                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│ ⚙️  Preferences...                                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ ✕   Quit FluxDone (Clean Flush & Exit)                       Ctrl + Q       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Dynamic Badge Counter Rendering

The tray icon dynamically reflects Ari's remaining academic workload for Today:
* **Pending Count $> 0$:** Renders high-contrast dark-mode tray glyph with a distinct numeric badge counter (e.g. `5`).
* **All Done ($0$ Pending):** Changes icon to a serene monochrome checkmark glyph (`ic_tray_all_done.png`).
* **Overdue Present:** Changes badge color to Urgent Accent `#D90429`.

```kotlin
package com.flux.done.desktop.tray

import com.flux.done.database.dao.TaskDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrayBadgeManager(private val taskDao: TaskDao) {
    fun observePendingCountForToday(): Flow<TrayBadgeState> {
        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        return taskDao.observeIncompleteCountForDate(todayEpochDay).map { count ->
            when {
                count == 0 -> TrayBadgeState.AllDone
                else -> TrayBadgeState.Pending(count = count)
            }
        }
    }
}

sealed interface TrayBadgeState {
    data object AllDone : TrayBadgeState
    data class Pending(val count: Int) : TrayBadgeState
}
```

---

## 3. The Strict Suspend & Screen-Lock Terminate Invariant

To guarantee that FluxDone consumes **zero CPU, zero battery, and leaves no running process** when you step away from your workstation or close your laptop lid, FluxDone implements a **Native DBus Session Monitor**.

### 3.1 DBus Signal Architecture

```
[ Ubuntu GNOME Session Bus / System Bus ]
                    │
   ┌────────────────┴────────────────┐
   ▼                                 ▼
Signal: `PrepareForSleep(true)`   Signal: `ActiveChanged(true)`
(org.freedesktop.login1.Manager)   (org.gnome.ScreenSaver)
   │                                 │
   └────────────────┬────────────────┘
                    ▼
[ FluxDone Native DBus Listener ]
                    │
   1. Checkpoint SQLite WAL: PRAGMA wal_checkpoint(TRUNCATE);
   2. Dispose AppIndicator Tray & Unregister Hotkeys
   3. Close Room Database Connection
   4. Invoke System.exit(0)
                    ▼
  ★ RESULT: Process completely terminated in `ps aux`
            Zero battery, zero RAM, zero background footprint.
```

### 3.2 Implementation Specification (`DesktopSessionLockMonitor.kt`)

```kotlin
package com.flux.done.desktop.tray

import com.flux.done.database.FluxDoneDatabase
import com.flux.done.database.desktop.DatabaseShutdownManager
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import kotlin.system.exitProcess

class DesktopSessionLockMonitor(
    private val database: FluxDoneDatabase,
    private val onDisposeTray: () -> Unit
) {
    private var sessionConnection: DBusConnection? = null
    private var systemConnection: DBusConnection? = null

    fun startMonitoring() {
        try {
            // 1. Monitor GNOME ScreenSaver Lock (Session Bus)
            sessionConnection = DBusConnectionBuilder.forSessionBus().build().apply {
                addSigHandler(GnomeScreenSaverActiveChanged::class.java, DBusSigHandler { signal ->
                    if (signal.isActive) {
                        executeEmergencyShutdown("Screen Locked by User")
                    }
                })
            }

            // 2. Monitor Systemd Suspend / Sleep (System Bus)
            systemConnection = DBusConnectionBuilder.forSystemBus().build().apply {
                addSigHandler(SystemdPrepareForSleep::class.java, DBusSigHandler { signal ->
                    if (signal.isSuspending) {
                        executeEmergencyShutdown("System Suspend / Sleep Initiated")
                    }
                })
            }
        } catch (_: Exception) {
            // Fallback for non-standard DBus environments
        }
    }

    private fun executeEmergencyShutdown(reason: String) {
        // Synchronously flush SQLite WAL and close database cleanly
        DatabaseShutdownManager.checkpointAndClose(database)
        onDisposeTray()
        
        sessionConnection?.disconnect()
        systemConnection?.disconnect()

        // Terminate JVM process completely - ZERO residual footprint
        exitProcess(0)
    }
}

// DBus Signal Bindings
class GnomeScreenSaverActiveChanged(path: String, val isActive: Boolean) : DBusSignal(path)
class SystemdPrepareForSleep(path: String, val isSuspending: Boolean) : DBusSignal(path)
```

---

## 4. Floating Quick Add Popup Specification (`QuickAddFloatingWindow.kt`)

Summoned anywhere in the OS via `Ctrl + Alt + Space` or tray context menu.

### 4.1 Visual Wireframe & Anatomy

```
+-----------------------------------------------------------------------------------+
|  ➕ QUICK CAPTURE                                                          [ ✕ ]  |
+-----------------------------------------------------------------------------------+
| [ Solve 5 Incenter Problems today 16:00 !High #Geometry                         ] |
+-----------------------------------------------------------------------------------+
| NLP Tokens: [ 📅 Today ] [ ⏰ 16:00 ] [ 🔴 High Priority ] [ 📁 Olympiad/Geometry ]|
+-----------------------------------------------------------------------------------+
| [ Esc ] Cancel            [ Shift + Enter ] Save & Add Another     [ Enter ] Save |
+-----------------------------------------------------------------------------------+
```

### 4.2 Window Properties & Ergonomics

* **Window Style:** Undecorated, rounded floating card (border radius `12dp`, surface `#1B263B` Oxford Slate, border `1dp` `#415A77` Steel Blue).
* **Dimensions:** Fixed `560dp` width $\times$ `160dp` height.
* **Placement:** Automatically centered on the currently active monitor where the mouse cursor resides.
* **Always-On-Top:** `window.isAlwaysOnTop = true` during capture; steals focus immediately to the input text field.
* **Focus Dismissal:** Clicking outside the window bounds or pressing `Escape` immediately dismisses the popup without committing changes.
* **NLP Pipeline Integration:** Ingests the Phase 2 tokenizer ([`FD_P2_QUICK_CAPTURE_NLP_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_QUICK_CAPTURE_NLP_SPEC.md)). Token pills appear in real-time beneath the input bar as Ari types.
* **Commit Actions:**
  * `Enter`: Commits task to Room database, dispatches IPC sync to FluxFoxus if timed, and closes popup.
  * `Shift + Enter`: Commits task, clears text field, keeps popup open with focus ready for next item.

---

## 5. Desktop Application Lifecycle Controller

```kotlin
package com.flux.done.desktop.tray

import androidx.compose.ui.window.WindowState
import com.flux.done.database.FluxDoneDatabase
import com.flux.done.database.desktop.DatabaseShutdownManager
import kotlin.system.exitProcess

class DesktopLifecycleController(
    private val windowState: WindowState,
    private val database: FluxDoneDatabase,
    private val onDisposeTray: () -> Unit
) {
    fun handleWindowCloseRequest() {
        // [ ✕ ] Button Clicked: Hide window to tray, keeping daemon active
        windowState.isVisible = false
    }

    fun restoreMainWindow() {
        windowState.isVisible = true
        windowState.isMinimized = false
    }

    fun requestApplicationExit() {
        // Clean user exit (Ctrl+Q or Tray -> Quit)
        DatabaseShutdownManager.checkpointAndClose(database)
        onDisposeTray()
        exitProcess(0)
    }
}
```

---

## 6. Verification & Testing Contracts

1. **System Close Minimization Test:** Click `[ ✕ ]` on the main window. Verify that the window disappears from the dock/taskbar, but the tray icon remains visible and `Ctrl + Alt + Space` opens the Quick Add popup.
2. **The Zero-Footprint Suspend Test:** 
   * Launch FluxDone on Ubuntu.
   * Lock the screen via `Super + L` or simulate DBus signal `dbus-send --session --type=signal /org/gnome/ScreenSaver org.gnome.ScreenSaver.ActiveChanged boolean:true`.
   * From a separate SSH/tty terminal, run `pgrep -f fluxdone`.
   * **Verification:** Process ID does not exist; exit code is 0; database WAL file is cleanly truncated.
3. **Floating Quick Add NLP Test:** Press `Ctrl + Alt + Space`. Type `"Revise Olympiad Chapter 4 tomorrow 10:00 !High"`. Press `Enter`. Query Room database; verify task created with `due_date = Tomorrow`, `start_time = 10:00`, `priority = 1`.

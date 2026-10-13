# ⌨️ Topic FD-P3-2: Keyboard-First Navigation & Desktop Shortcuts Specification

**Document ID:** `FD_P3_KEYBOARD_SHORTCUTS_SPEC`  
**Phase:** Phase 3 (Ubuntu Linux Desktop Companion via Compose Multiplatform)  
**Target Package:** `com.flux.done.desktop.keyboard`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Compose Multiplatform 1.6+ + Skiko KeyEvent Dispatcher)  
**Target OS:** Ubuntu Linux Workstation (GNOME X11 & Wayland, JVM 17+)  
**Prerequisites:** [`FD_P3_DESKTOP_UI_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%203/FD_P3_DESKTOP_UI_SPEC.md)  

---

## 1. Overview & Architectural Philosophy

Intensive academic studying and problem-solving at a workstation requires zero-friction input. Reaching for a mouse breaks focus. FluxDone Desktop implements a **keyboard-first navigation engine** modeled after professional developer tools and desktop ergonomics.

### Core Architectural Decisions:
1. **Modifier-Driven Safety (Option B):** All primary shortcuts use unambiguous modifier keys (`Ctrl`, `Alt`, `Shift`) or standard non-character keys (`Enter`, `Tab`, `Delete`, `Escape`). Typing inside task titles, search bars, or markdown notes is **never** intercepted or corrupted by modal keybindings.
2. **Global Desktop Summon (`Ctrl + Alt + Space`):** System-wide hotkey registered via GNOME Desktop Portal / X11 Keybinder. Summons the lightweight Quick Add floating bar or toggles the main window into focus from anywhere in the OS.
3. **Dual Calendar View Access (`Alt + C` & `Ctrl + 2`):** Direct switching between Task Stream and Calendar Timeline while strictly preserving universal `Ctrl + C` (Copy) functionality across all text editors.
4. **Mandatory Subtask Cascade (`Alt + X` / `Ctrl + Enter`):** Toggling parent task completion automatically cascades completion to all child subtasks (`is_completed = 1`), preserving core database integrity.
5. **Contextual Focus Pipeline:** Clear priority hierarchy between Active Text Field Focus, Task Row Selection, and Global Window Key Dispatch.

---

## 2. Complete Desktop Keyboard Shortcuts Matrix

### 2.1 Global System & Application Control

| Shortcut | Scope | Action | Technical Dispatch |
|---|---|---|---|
| `Ctrl + Alt + Space` | OS Global | Summon Quick Add Bar / Focus Main Window | DBus Global Shortcuts Portal / X11 Keybinder |
| `Ctrl + Q` | App Window | Quit FluxDone (Clean WAL flush & Exit) | `DesktopLifecycleController.requestQuit()` |
| `Ctrl + W` | App Window | Minimize Window to System Tray | `WindowState.isMinimized = true` |
| `Ctrl + ?` / `F1` | App Window | Toggle Keyboard Shortcuts Cheat-Sheet Modal | `KeyboardDialogState.showModal = true` |
| `Escape` | App Context | Defocus text input $\to$ Close Inspector $\to$ Dismiss Modal | FocusManager hierarchical clear |

### 2.2 Views, Panes & Tab Navigation

| Shortcut | Scope | Action | Technical Dispatch |
|---|---|---|---|
| `Ctrl + 1` | Main Workspace | Switch to **Tasks Stream View** (Pane 2 active) | `NavigationController.selectTab(WorkspaceTab.TASKS)` |
| `Ctrl + 2` | Main Workspace | Switch to **Calendar Timeline View** (Pane 2 full 7-day) | `NavigationController.selectTab(WorkspaceTab.CALENDAR)` |
| `Ctrl + 3` | Main Workspace | Switch to **Habits Matrix View** (Monthly grid) | `NavigationController.selectTab(WorkspaceTab.HABITS)` |
| `Alt + C` | Main Workspace | Secondary shortcut: Toggle Calendar Timeline in Pane 3 | `InspectorModeController.toggleTimeline()` |
| `Ctrl + T` | Main Workspace | Advance to **Next Workspace Tab** (Browser ergonomics) | `NavigationController.nextTab()` |
| `Ctrl + Shift + T`| Main Workspace | Return to **Previous Workspace Tab** | `NavigationController.previousTab()` |
| `Ctrl + \` | Main Workspace | Toggle Left Navigation Sidebar (Pane 1 collapse) | `PaneVisibilityController.toggleSidebar()` |
| `Alt + \` | Main Workspace | Toggle Right Inspector (Pane 3 collapse) | `PaneVisibilityController.toggleInspector()` |
| `Ctrl + F` / `Ctrl + K` | Main Workspace | Focus Universal Quick Search Bar | `SearchInputFocusRequester.requestFocus()` |

### 2.3 Task Stream Navigation & Selection

| Shortcut | Scope | Action | Technical Dispatch |
|---|---|---|---|
| `Alt + J` / `Down Arrow` | Pane 2 (Tasks) | Move selection to **Next Task** in list | `TaskSelectionManager.selectNext()` |
| `Alt + K` / `Up Arrow` | Pane 2 (Tasks) | Move selection to **Previous Task** in list | `TaskSelectionManager.selectPrevious()` |
| `Home` | Pane 2 (Tasks) | Jump selection to **First Task** in section | `TaskSelectionManager.selectFirst()` |
| `End` | Pane 2 (Tasks) | Jump selection to **Last Task** in section | `TaskSelectionManager.selectLast()` |
| `Alt + Right Arrow` | Main Workspace | Shift focus: Pane 1 $\to$ Pane 2 $\to$ Pane 3 | `FocusNavigator.focusNextPane()` |
| `Alt + Left Arrow` | Main Workspace | Shift focus: Pane 3 $\to$ Pane 2 $\to$ Pane 1 | `FocusNavigator.focusPreviousPane()` |

### 2.4 Task Operations & Scheduling

| Shortcut | Scope | Action | Invariant / Technical Dispatch |
|---|---|---|---|
| `Ctrl + N` | Pane 2 (Tasks) | Create **New Task** in active list/section | Focuses inline Quick Add field |
| `Alt + X` / `Ctrl + Enter` | Selected Task | **Toggle Complete / Active** | **Mandatory Subtask Cascade**: atomizes completion to all child subtasks |
| `Space` | Selected Task | **Toggle Complete** (Only when row is focused & not editing text) | Executes `ToggleTaskCompletion(taskId)` |
| `Enter` | Selected Task | Open & focus **Task Detail Inspector** (Pane 3) | Switches Inspector to Detail mode & focuses notes |
| `Ctrl + Alt + T` | Selected Task | Schedule task for **Today** (`due_date = Today`) | Updates Room DB with today's epoch |
| `Ctrl + Alt + M` | Selected Task | Schedule task for **Tomorrow** (`due_date = Tomorrow`) | Updates Room DB with tomorrow's epoch |
| `Tab` | Selected Task | **Indent Task** (Demote to Subtask under above item) | `TaskHierarchyManager.indentSelected()` |
| `Shift + Tab` | Selected Task | **Outdent Subtask** (Promote to top-level Task) | `TaskHierarchyManager.outdentSelected()` |
| `Delete` / `Backspace` | Selected Task | Move task to **Trash** | Soft deletes with 5-second Undo floating snackbar |
| `Ctrl + Z` | Main Workspace | **Undo** last deletion or check-off | Reverts last transaction within 5s grace window |
| `Alt + 1` | Selected Task | Set Priority: **High (!High, Red)** | `UpdateTaskPriority(Priority.HIGH)` |
| `Alt + 2` | Selected Task | Set Priority: **Medium (!Med, Orange)** | `UpdateTaskPriority(Priority.MEDIUM)` |
| `Alt + 3` | Selected Task | Set Priority: **Low (!Low, Blue)** | `UpdateTaskPriority(Priority.LOW)` |
| `Alt + 0` | Selected Task | Clear Priority: **None (Gray)** | `UpdateTaskPriority(Priority.NONE)` |

---

## 3. Keyboard Event Dispatch Pipeline in Compose Multiplatform

### 3.1 Event Interception Architecture

Compose Multiplatform for Desktop intercepts key events at the window root level using `onPreviewKeyEvent`. To prevent text inputs from being hijacked, the dispatcher operates in a strict 3-tier chain:

```
[ Window Root: onPreviewKeyEvent ]
           │
           ▼
[ Is Event Modifier-Driven Global Shortcut? (Ctrl+Q, Ctrl+1, Ctrl+Alt+Space) ] ──► YES ──► Handle Immediately
           │ NO
           ▼
[ Does Active Child have Text Focus? (TextField, OutlinedTextField, LiveNotes) ]
      ├─────► YES: Forward raw key to TextField (Space, Enter, Letters, Backspace)
      │            * EXCEPTION: Escape immediately defocuses and restores Nav mode
      │            * EXCEPTION: Tab inside title advances to Due Date picker
      ▼ NO
[ Selected Task Row Context ]
      ├─────► Alt+J / Alt+K / Up / Down: Move row cursor
      ├─────► Alt+X / Ctrl+Enter / Space: Toggle Complete (Cascade)
      ├─────► Enter: Expand to Inspector
      ├─────► Ctrl+Alt+T / Ctrl+Alt+M: Reschedule
      ├─────► Delete: Soft Delete
      └─────► Tab / Shift+Tab: Indent / Outdent
```

### 3.2 Implementation Specification (`DesktopKeyDispatcher.kt`)

```kotlin
package com.flux.done.desktop.keyboard

import androidx.compose.ui.input.key.*
import com.flux.done.desktop.ui.mvi.DesktopAction
import com.flux.done.desktop.ui.mvi.DesktopViewModel

class DesktopKeyDispatcher(
    private val viewModel: DesktopViewModel,
    private val onToggleSidebar: () -> Unit,
    private val onToggleInspector: () -> Unit
) {
    fun handlePreviewKeyEvent(
        event: KeyEvent,
        isTextInputFocused: Boolean
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        // 1. Universal Escape Key (Defocus & Dismiss)
        if (event.key == Key.Escape) {
            viewModel.onAction(DesktopAction.DismissActiveOverlayOrDefocus)
            return true
        }

        // 2. Global Shortcuts (Always active even if text input has focus)
        if (event.isCtrlPressed && event.isAltPressed && event.key == Key.Spacebar) {
            viewModel.onAction(DesktopAction.ToggleQuickAddFloatingWindow)
            return true
        }
        if (event.isCtrlPressed && event.key == Key.Q) {
            viewModel.onAction(DesktopAction.RequestApplicationExit)
            return true
        }
        if (event.isCtrlPressed && event.key == Key.W) {
            viewModel.onAction(DesktopAction.MinimizeToTray)
            return true
        }
        if (event.isCtrlPressed && (event.key == Key.F || event.key == Key.K)) {
            viewModel.onAction(DesktopAction.FocusGlobalSearch)
            return true
        }
        if (event.isCtrlPressed && event.key == Key.Z) {
            viewModel.onAction(DesktopAction.UndoLastAction)
            return true
        }

        // View Navigation (Ctrl+1, Ctrl+2, Ctrl+3, Ctrl+T, Ctrl+Shift+T, Alt+C)
        if (event.isCtrlPressed && event.key == Key.One) {
            viewModel.onAction(DesktopAction.SwitchWorkspaceView(0))
            return true
        }
        if ((event.isCtrlPressed && event.key == Key.Two) || (event.isAltPressed && event.key == Key.C)) {
            viewModel.onAction(DesktopAction.SwitchWorkspaceView(1)) // Calendar Timeline
            return true
        }
        if (event.isCtrlPressed && event.key == Key.Three) {
            viewModel.onAction(DesktopAction.SwitchWorkspaceView(2)) // Habits Matrix
            return true
        }
        if (event.isCtrlPressed && event.isShiftPressed && event.key == Key.T) {
            viewModel.onAction(DesktopAction.PreviousWorkspaceTab)
            return true
        }
        if (event.isCtrlPressed && event.key == Key.T) {
            viewModel.onAction(DesktopAction.NextWorkspaceTab)
            return true
        }
        if (event.isCtrlPressed && event.key == Key.Backslash) {
            onToggleSidebar()
            return true
        }
        if (event.isAltPressed && event.key == Key.Backslash) {
            onToggleInspector()
            return true
        }

        // 3. IF TEXT INPUT IS FOCUSED -> Let TextField consume characters, spaces, and edits!
        if (isTextInputFocused) {
            return false // Let Compose BasicTextField handle raw typing
        }

        // 4. TASK STREAM NAVIGATION & SELECTION (Only when NOT editing text)
        return when {
            (event.isAltPressed && event.key == Key.J) || event.key == Key.DirectionDown -> {
                viewModel.onAction(DesktopAction.SelectNextTask)
                true
            }
            (event.isAltPressed && event.key == Key.K) || event.key == Key.DirectionUp -> {
                viewModel.onAction(DesktopAction.SelectPreviousTask)
                true
            }
            event.isCtrlPressed && event.key == Key.N -> {
                viewModel.onAction(DesktopAction.CreateNewTaskInline)
                true
            }
            (event.isAltPressed && event.key == Key.X) || (event.isCtrlPressed && event.key == Key.Enter) || event.key == Key.Spacebar -> {
                viewModel.onAction(DesktopAction.ToggleSelectedTaskCompletionWithCascade)
                true
            }
            event.key == Key.Enter -> {
                viewModel.onAction(DesktopAction.OpenTaskDetailsInInspector)
                true
            }
            event.isCtrlPressed && event.isAltPressed && event.key == Key.T -> {
                viewModel.onAction(DesktopAction.RescheduleSelectedTaskToday)
                true
            }
            event.isCtrlPressed && event.isAltPressed && event.key == Key.M -> {
                viewModel.onAction(DesktopAction.RescheduleSelectedTaskTomorrow)
                true
            }
            event.isShiftPressed && event.key == Key.Tab -> {
                viewModel.onAction(DesktopAction.OutdentSelectedTask)
                true
            }
            event.key == Key.Tab -> {
                viewModel.onAction(DesktopAction.IndentSelectedTask)
                true
            }
            event.key == Key.Delete || event.key == Key.Backspace -> {
                viewModel.onAction(DesktopAction.TrashSelectedTask)
                true
            }
            event.isAltPressed && event.key == Key.One -> {
                viewModel.onAction(DesktopAction.SetPriority(1)) // High
                true
            }
            event.isAltPressed && event.key == Key.Two -> {
                viewModel.onAction(DesktopAction.SetPriority(2)) // Med
                true
            }
            event.isAltPressed && event.key == Key.Three -> {
                viewModel.onAction(DesktopAction.SetPriority(3)) // Low
                true
            }
            event.isAltPressed && event.key == Key.Zero -> {
                viewModel.onAction(DesktopAction.SetPriority(0)) // None
                true
            }
            else -> false
        }
    }
}
```

---

## 4. The Mandatory Subtask Cascade Mechanics

When `Alt+X`, `Ctrl+Enter`, or `Space` is pressed on a selected task, the action dispatches to `ToggleTaskCompletionWithCascade`. The transactional guarantees are:

```
Selected Task (Task Entity)
       │
       ▼ [ Check Current Status ]
   ┌───┴───────────────────────────────┐
   │                                   │
[ If Currently Active (0) ]    [ If Currently Complete (1) ]
   │                                   │
   ▼                                   ▼
Atomic Transaction:             Atomic Transaction:
1. `tasks.is_completed = 1`     1. `tasks.is_completed = 0`
2. `tasks.completed_at = now`   2. `tasks.completed_at = null`
3. `UPDATE subtasks             3. Subtasks remain completed or
   SET is_completed = 1            revive optionally based on
   WHERE task_id = :id`            parent configuration
4. Trigger IPC Sync to FF       4. Trigger IPC Sync to FF
5. Show 5s Undo Snackbar        5. Show 5s Undo Snackbar
```

---

## 5. Global Hotkey Registration on Ubuntu Linux

To support `Ctrl + Alt + Space` globally when FluxDone is minimized or in the background:

1. **Wayland Compatibility (GNOME 42+):** Register via XDG Desktop Portal `org.freedesktop.portal.GlobalShortcuts` over DBus.
2. **X11 Fallback:** Register using standard `XGrabKey` via `libkeybinder-3.0` JNA bindings.
3. **Behavior on Hotkey Trigger:**
   * If the main window is hidden $\to$ Display centered floating Quick Add popup window (undecorated, 560dp width).
   * If the main window is minimized $\to$ Restore window and bring to front (`window.toFront()`).
   * If the main window is focused $\to$ Hide window back to system tray.

---

## 6. Keyboard Shortcuts Cheat-Sheet Modal (`Ctrl + ?`)

Pressing `Ctrl + ?` or clicking the Help icon renders a semi-transparent dark overlay dialog displaying organized shortcut categories:

```
+-----------------------------------------------------------------------------------+
|  ⌨️ FLUXDONE KEYBOARD SHORTCUTS COCKPIT                                    [ Esc ] |
+-----------------------------------------------------------------------------------+
| NAVIGATION                 ACTIONS                       VIEWS                    |
| Alt + J / ↓   Next Task    Ctrl + N       New Task       Ctrl + 1    Tasks Stream |
| Alt + K / ↑   Prev Task    Alt + X / ␣    Toggle Done    Ctrl + 2    Timeline     |
| Alt + → / ←   Pane Switch  Enter          Edit Details   Alt + C     Timeline     |
| Home / End    List Bounds  Del            Move to Trash  Ctrl + 3    Habit Matrix |
|                            Ctrl + Z       Undo (5s)      Ctrl + T    Next Tab     |
| SCHEDULING & PRIORITY      Tab            Indent Subtask Ctrl + ⇧ + T Prev Tab    |
| Ctrl+Alt+T    Today        Shift + Tab    Outdent Task   Ctrl + \    Toggle Left  |
| Ctrl+Alt+M    Tomorrow     Alt + 1..3     Priority       Alt + \     Toggle Right |
+-----------------------------------------------------------------------------------+
```

---

## 7. Verification & Testing Contracts

1. **Text Editing Exemption Test:** Focus a `BasicTextField` in the Notes editor and type `"j k n x c today"`. Verify that zero tasks are navigated, selected, or marked complete, and all typed characters appear in the text buffer.
2. **Mandatory Cascade Test:** Select a task with 3 child subtasks. Press `Alt + X`. Verify in SQLite that both the parent `tasks` record and all 3 `subtasks` records have `is_completed = 1`.
3. **Non-Conflicting Calendar View Test:** Select notes text and press `Ctrl + C`. Verify the text is copied to the system clipboard. Press `Alt + C` or `Ctrl + 2`. Verify the view switches immediately to the Calendar Timeline.
4. **Undo Reversibility Test:** Select a task, press `Delete`, then immediately press `Ctrl + Z`. Verify the task is restored to active state with zero data loss.

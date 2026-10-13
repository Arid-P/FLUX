# 🖥️ Topic FD-P3-1: Compose Multiplatform Desktop 3-Pane UI Specification

**Document ID:** `FD_P3_DESKTOP_UI_SPEC`  
**Phase:** Phase 3 (Ubuntu Linux Desktop Companion via Compose Multiplatform)  
**Target Package:** `com.flux.done.desktop.ui`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + JetBrains Compose Multiplatform 1.6+ + Skiko Canvas + StateFlow)  
**Target OS:** Ubuntu Linux Workstation (GNOME X11 / Wayland, JVM 17+)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

When studying at a workstation, mobile interfaces feel constrained. Intensive academic workflows—such as solving multi-step Olympiad geometry problem sheets or reviewing syllabus sections—demand a high-density, wide-screen workspace.

**Topic FD-P3-1 establishes the Compose Multiplatform Desktop 3-Pane Workspace for FluxDone.**  
It replicates TickTick's desktop ergonomics while stripping out web bloat:
* **High-Density 3-Pane Layout:**
  * **Pane 1 (Left Sidebar):** Collapsible navigation tree with system Smart Lists, custom Folders & Lists, and section sub-nodes.
  * **Pane 2 (Center Stream):** Primary task action stream with sticky section dividers, compact mouse-optimized rows (`44dp`), and instant search.
  * **Pane 3 (Right Inspector):** Dual-mode contextual inspector. Displays the **24-Hour Day Timeline Inspector by default on startup**, switching automatically to the **Task Detail Inspector** whenever a task row is clicked.
* **Draggable Dividers & Layout Persistence (Option 2.A):** Draggable splitter handles allow fine-grained resizing between panes. Widths are persisted in local preferences across app restarts.
* **Adaptive Breakpoint Engine (Option 3.A):** If the window drops below `1024dp` (such as snapping to half-screen on a 1080p display), the UI collapses automatically into an adaptive **2-Pane layout** with an overlay navigation drawer.
* **Ubuntu Linux Native Polish:** Custom client-side window controls (CSD) adhering to GNOME desktop ergonomics, fractional DPI scaling (100%, 125%, 150%, 200%) via Skiko, and deep dark theme integration (`#0D1B2A` Abyssal Navy, `#1B263B` Oxford Slate, `#415A77` Steel Blue).

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Design Tokens** | [`FLUX_DESIGN_TOKENS.md`](file:///home/hp/Flux_Prompts/documentation/00_CORE/FLUX_DESIGN_TOKENS.md) | Maps curated 12-swatch academic domain palette, typography, and dark theme colors. |
| **Hierarchy Baseline** | [`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md) | Renders the 4-tier tree: Folders $\to$ Lists $\to$ Sections $\to$ Tasks in Pane 1 and Pane 2. |
| **Task Detail & Notes** | [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) | Powers the Task Detail Inspector in Pane 3 with live markdown and subtask synchronization. |
| **Calendar Timeline** | [`FD_P1_CALENDAR_TIMELINE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_CALENDAR_TIMELINE_SPEC.md) | Powers the Day Timeline Inspector in Pane 3 with 24-hour canvas and drag-to-reschedule. |
| **Shared Desktop DB** | `FD_P3_SHARED_DATABASE_SPEC.md` | Ingests the shared local SQLite database located in `~/.local/share/fluxdone/fluxdone.db`. |

---

## 3. UI Specifications & 3-Pane Anatomy

### 3.1 Desktop Workspace Wireframe (`DesktopWorkspaceScreen.kt`)

```
+-----------------------------------------------------------------------------------------------------------------------+
|  FLUXDONE DESKTOP — [Ari Workstation]                                                            [ — ] [ □ ] [ ✕ ]    |
+---------------------+-------------------------------------------------------+-----------------------------------------+
| PANE 1: NAVIGATION  | PANE 2: TASK STREAM (Center List)                     | PANE 3: INSPECTOR (Dual Mode)           |
| Width: 260dp        | Width: Flexible (Fill Remaining)                      | Width: 380dp                            |
+---------------------+-------------------------------------------------------+-----------------------------------------+
| [🔍 Quick Search ]  | 📐 OLYMPIAD GEOMETRY                    🔍  [ + Add ] | [ 📝 Details ]    [ 📅 Timeline (Active)]|
|                     | ----------------------------------------------------- | --------------------------------------- |
| SMART LISTS         | ▼ TRIANGLES (3)                                       | ALL-DAY STRIP (2 tasks)                 |
| 📅 Today        [5] | [✓] Angle Chasing Lemma (Done)       #1565C0 14:00    | [ Revise Incenter Formulas ]            |
| 🌅 Tomorrow     [2] | [ ] Solve 5 Incenter Problems   !High #1565C0 16:00 ◄ | ======================================= |
| 📆 Upcoming    [14] | [ ] Simson Line Proof           !Med  #1565C0       | 14:00 [✓] Angle Chasing (Done)          |
| 📋 All Active  [28] |                                                       | 15:00                                   |
| ✓  Completed        | ▼ CIRCLES (2)                                         | 16:00 ┌───────────────────────────────┐ |
|                     | [ ] Power of a Point Problems   !High #1565C0         |       │ Solve 5 Incenter Problems     │ |
| FOLDERS & LISTS     | [ ] Cyclic Quadrilaterals Drill !Med  #1565C0         | 17:00 │ 16:00 - 17:30 (90m)   !High   │ |
| ▼ Olympiad Prep (6) |                                                       |       └───────────────────────────────┘ |
|   • Number Theory   |                                                       | 18:00 ------------------- [ 18:15 ● ] - |
|   • Geometry (Sel)  |                                                       | 19:00                                   |
|   • Combinatorics   |                                                       | 20:00                                   |
| ▶ JEE Foundation(3) |                                                       |                                         |
| ------------------- |                                                       |                                         |
| 🗑️ Trash (3) ⚙️ Set |                                                       |                                         |
+---------------------+-------------------------------------------------------+-----------------------------------------+
```

---

### 3.2 Detailed Pane Specifications

#### Pane 1: Left Navigation Sidebar (`DesktopNavSidebar.kt`)
* **Default Width:** `260dp` (resizable between `200dp` and `360dp`). Collapsible with `Ctrl+B`.
* **Top Header:** User profile indicator (*"Ari"*), app badge, and Quick Search shortcut (`Ctrl+K` / `Ctrl+P`).
* **System Smart Lists:** Interactive rows for `Today`, `Tomorrow`, `Upcoming`, `All Active`, and `Completed` with dynamic task counts. Tapping changes the active view in Pane 2.
* **Hierarchical Tree View:**
  * Folders expand/collapse with smooth chevron animations.
  * Lists display colored domain swatches (`10dp` circle), title, and active task count.
  * Section sub-nodes: Lists with sections reveal their internal sections in the tree for direct 1-tap filtering.
* **Footer Row:** Quick links to **Trash** (SCR-12) displaying active items count, and **Settings** (SCR-09).

#### Pane 2: Center Task Action Stream (`DesktopTaskStream.kt`)
* **Flexible Layout:** Takes up all remaining available window space between Pane 1 and Pane 3.
* **Stream Header:** Displays active List or Smart List title, breadcrumb hierarchy (`Olympiad Prep / Geometry`), sort dropdown (`Date`, `Priority`, `Alphabetical`), and `[ + Add Task ]` button.
* **Quick Add Input Bar:** Pinned directly above the task list (`Enter to submit`). Supports Smart Recognition NLP inline parsing.
* **High-Density Task Rows (`44dp` height):**
  * **Leading Checkbox:** Custom tactile checkbox with spring-bounce animation.
  * **Title:** 14sp semibold text, struck through on completion.
  * **Pill Badges:** Priority flag pill (`!High` Crimson `#E53935`), list domain badge, and due date/time caption.
  * **Selection Cursor (`◄`):** The currently focused task (via mouse click or keyboard `j`/`k`) displays an active accent outline and subtle surface highlight.
  * **Hover Actions:** Hovering over a row reveals quick mouse shortcuts on the trailing edge: `[ 📅 Reschedule ]`, `[ 📁 Move ]`, `[ 🗑️ Trash ]`.
* **Sticky Section Headers:** Collapsible section bars dividing tasks (e.g. *Triangles*, *Circles*).

#### Pane 3: Right Inspector Pane (`DesktopInspectorPane.kt` — Dual Mode)
* **Default Width:** `380dp` (resizable between `300dp` and `500dp`). Collapsible with `Ctrl+\` or close button `[ ✕ ]`.
* **Top Mode Switcher Tabs:**
  ```
  [ 📝 Task Detail ]          [ 📅 Day Timeline (Default) ]
  ```
* **Mode A: Task Detail Inspector:**
  * Active when user clicks any task row in Pane 2 or taps the `Details` tab.
  * Large unframed Title field with enter-to-commit.
  * Metadata bar: List selector dropdown, Section assignment, Priority flag picker.
  * Date & Time controls: Start Time, End Time, Duration calculator, Recurrence picker.
  * **Live Markdown Notes Editor:** Supports live formatted rich text with clickable web links.
  * **Subtasks Checklist:** Enter-to-add subtasks with bidirectional parent completion synchronization.
* **Mode B: Day Timeline Inspector (Default on Startup — Option 1.A):**
  * Provides an always-visible, live 24-hour Canvas Timeline for Today (or the date of the selected task).
  * Houses the **Dynamic All-Day top strip** for untimed obligations.
  * Full interactive canvas: Tapping empty time slots opens quick task creation pre-filled with the slot; dragging blocks reschedules with **strict duration preservation**.
  * Red "Now" line auto-advancing every 60 seconds.

---

### 3.3 Draggable Dividers & Layout Persistence (Option 2.A)

Between Pane 1 and Pane 2, and between Pane 2 and Pane 3, interactive 4dp splitter dividers are rendered:

```kotlin
@Composable
fun DesktopVerticalSplitter(
    onDrag: (deltaPx: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> onDrag(delta) }
            )
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}
```

* **Persistence:** On drag completion, `sidebarWidthDp` and `inspectorWidthDp` are written asynchronously to Desktop UI Preferences (`~/.config/fluxdone/desktop_ui_prefs.json`).
* **Restoration:** On app launch, saved widths are restored, preserving the user's customized workstation layout.

---

### 3.4 Adaptive Responsive Breakpoint Engine (Option 3.A)

When the desktop window is resized or tiled across monitors:

| Window Width Breakpoint | Active Layout Architecture | Behavioral Rules |
|---|---|---|
| **$\ge 1200\text{dp}$** | **Full 3-Pane Expanded** | All 3 panes visible side-by-side. Sidebar (`260dp`), Center List (`Flex`), Inspector (`380dp`). |
| **$1024\text{dp} \le W < 1200\text{dp}$** | **3-Pane Compact** | Sidebar automatically compresses to `220dp`; Inspector to `320dp`; Center List occupies remaining space. |
| **$< 1024\text{dp}$** | **2-Pane Responsive Mode** | **Sidebar collapses into an overlay drawer** (toggled via `Ctrl+B` or hamburger button). Center List and Right Inspector share the screen. |
| **$< 768\text{dp}$** | **Single Pane with Floating Inspector** | Ultra-compact mode: Center List takes full width. Clicking a task opens the Inspector as a slide-over modal drawer. |

---

## 4. Technical Architecture & State Management

### 4.1 Desktop Workspace MVI State (`DesktopWorkspaceViewModel.kt`)

```kotlin
package com.flux.done.desktop.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flux.done.core.database.TaskEntity
import com.flux.done.core.database.TaskListEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class InspectorMode {
    TASK_DETAIL,
    DAY_TIMELINE
}

data class DesktopWorkspaceUiState(
    val sidebarWidth: Dp = 260.dp,
    val inspectorWidth: Dp = 380.dp,
    val isSidebarCollapsed: Boolean = false,
    val isInspectorCollapsed: Boolean = false,
    val activeInspectorMode: InspectorMode = InspectorMode.DAY_TIMELINE, // Default per Option 1.A
    val activeListId: String = "today",
    val selectedTaskId: String? = null,
    val selectedTask: TaskEntity? = null,
    val tasksInActiveList: List<TaskEntity> = emptyList(),
    val searchQuery: String = "",
    val isNarrowWindow: Boolean = false // True if width < 1024dp
)

sealed interface DesktopWorkspaceIntent {
    data class ResizeSidebar(val newWidth: Dp) : DesktopWorkspaceIntent
    data class ResizeInspector(val newWidth: Dp) : DesktopWorkspaceIntent
    object ToggleSidebar : DesktopWorkspaceIntent
    object ToggleInspector : DesktopWorkspaceIntent
    data class SwitchInspectorMode(val mode: InspectorMode) : DesktopWorkspaceIntent
    data class SelectList(val listId: String) : DesktopWorkspaceIntent
    data class SelectTask(val task: TaskEntity) : DesktopWorkspaceIntent
    object ClearTaskSelection : DesktopWorkspaceIntent
    data class UpdateSearchQuery(val query: String) : DesktopWorkspaceIntent
    data class WindowSizeChanged(val widthDp: Dp) : DesktopWorkspaceIntent
}
```

---

### 4.2 Desktop Main Application Entry Point (`DesktopMain.kt`)

```kotlin
package com.flux.done.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.flux.done.core.designsystem.FluxTheme
import com.flux.done.desktop.ui.DesktopWorkspaceScreen

fun main() = application {
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "FluxDone — Academic Workspace",
        icon = null // Set app icon bitmap
    ) {
        FluxTheme(darkTheme = true) {
            DesktopWorkspaceScreen(
                windowWidth = windowState.size.width
            )
        }
    }
}
```

---

## 5. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **No Task Selected on Launch** | In accordance with Option 1.A, Pane 3 renders the **Day Timeline Inspector** by default, immediately displaying Today's time-blocking schedule. |
| **Rapid Switching between Task Details and Timeline** | Tapping a task in Pane 2 switches Pane 3 to `TASK_DETAIL`. The user can tap the `[ 📅 Timeline ]` tab at any time to inspect the day's schedule without losing task selection. |
| **Window Resized to Half-Screen (Snap to 960px)** | The layout detects `width < 1024dp`, automatically collapses the left navigation sidebar into an overlay drawer, and expands the center task stream and inspector to occupy the entire half-screen cleanly. |
| **Splitter Drag Clamping** | Sidebar width is clamped strictly between `200dp` and `360dp`; Inspector width is clamped strictly between `300dp` and `500dp`. User cannot accidentally collapse a pane to `0dp` by dragging. |
| **Ubuntu Fractional Scaling (125% / 150%)** | Layout uses density-independent pixels (`Dp`); Skiko renders crisp vectors and subpixel text without blurry font rasterization on Ubuntu GNOME. |
| **Selected Task Deleted from Another Device / List** | If `selectedTaskId` is deleted, Pane 3 gracefully falls back to `DAY_TIMELINE` mode with an informational notification: *"Selected task was removed"*. |

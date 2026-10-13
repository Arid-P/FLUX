# ⚡ Topic FD-P2-2: Batch Operations & Multi-Select Engine Specification

**Document ID:** `FD_P2_BATCH_OPERATIONS_SPEC`  
**Phase:** Phase 2 (Advanced Ergonomics, Settings & Inter-App IPC)  
**Target Package:** `com.flux.done.features.batch`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room `@Transaction` + StateFlow)  
**Screen References:** SCR-01 (Main Task List), SCR-05 (Smart Lists)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

When managing extensive study backlogs (such as dozens of Olympiad problem sets, mock tests, or chapter revision items), moving or rescheduling tasks individually introduces unacceptable friction.

**Topic FD-P2-2 establishes the Batch Operations & Multi-Select Engine for FluxDone.**  
It enables rapid bulk curation through an intuitive contextual interface:
* **Dual Activation Gestures:** Triggered by long-pressing any task card or by tapping the Multi-Select action button in the Top App Bar.
* **Contextual Top App Bar:** Transforms into a selection header showing real-time selection counts, `[ Select All ]` / `[ Deselect All ]` toggle, and `[ ✕ Exit ]`.
* **Docked Bottom Action Dock:** Provides 1-tap bulk actions: **Date Shift**, **Move to List/Section**, **Priority Set**, **Batch Complete**, and **Batch Trash**.
* **System-Wide Subtask Invariant:** Completing a task in batch mode **strictly and atomically marks all its child subtasks complete** (`is_completed = 1`), matching single-task completion behavior across the entire app.
* **2-Step List & Section Relocation:** When moving tasks in bulk, Ari first selects the target List, then optionally chooses a specific Section within that list (or places tasks in the unsectioned root pool).
* **Unified Recurring Series Scope Dialog:** If a batch selection includes recurring tasks and bulk delete is triggered, a single modal resolves scope for all recurring items (*This instance only*, *This and future*, or *All tasks in series*).
* **Atomic Room SQLite Transactions:** Every batch operation executes inside `@Transaction` blocks, guaranteeing zero partial writes or orphaned records.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Hierarchy Baseline** | [`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md) | Bulk move reassigns `list_id` and nullable `section_id` with cascading constraints. |
| **Task Management** | [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) | Reaffirms the core invariant: completing parent completes all subtasks; unchecking subtasks revives parent. |
| **Recurrence Engine** | [`FD_P1_RECURRENCE_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_RECURRENCE_ENGINE_SPEC.md) | Applies 3-choice scope resolution to recurring tasks during batch deletions or date shifts. |
| **Trash Architecture** | [`FD_P1_SMART_LISTS_AND_TRASH_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_SMART_LISTS_AND_TRASH_SPEC.md) | Batch trash marks `is_deleted = 1` and logs `deleted_from_list_id` for 30-day retention and restore. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Contextual Selection Mode Layout (SCR-01 & SCR-05)

When multi-select mode is activated:

```
+-----------------------------------------------------------------+
|  [ ✕ ]  3 Selected                      [ Select All ]    ⋮    | <-- Contextual Top Bar
+-----------------------------------------------------------------+
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [✓]  Solve 5 Diophantine Equations               !High     │ | <-- Selected Task Card
|  │      📅 Today 4:00 PM • [Number Theory #2E7D32]            │ |
|  └────────────────────────────────────────────────────────────┘ |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [✓]  Review Incircle Lemma                       !Med      │ | <-- Selected Task Card
|  │      📅 Today 6:00 PM • [Geometry #1565C0]                 │ |
|  └────────────────────────────────────────────────────────────┘ |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [ ]  Read Thermodynamics Notes                   !None     │ | <-- Unselected Task Card
|  │      📅 Oct 15 • [Physics #576481]                         │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
+=================================================================+
|  [ 📅 Date ]  [ 📁 Move ]  [ 🚩 Priority ]  [ ✓ Done ]  [ 🗑️ ]  | <-- Docked Bottom Bar
+-----------------------------------------------------------------+
```

#### Selection Interactions:
* **Activation:**
  1. Long-press any task card for 400ms $\to$ delivers light haptic tick and enters Multi-Select Mode with that task selected.
  2. Tap the `[ ≡✓ ]` Multi-Select button in the Top App Bar $\to$ enters Multi-Select Mode with 0 items selected.
* **Tapping Cards in Multi-Select Mode:**
  * Tapping anywhere on a task card toggles its selection state (`selectedTaskIds.contains(id)`).
  * Checkbox on left transforms from completion checkmark into a blue circular selection check.
* **Exit Mode:**
  * Tapping `[ ✕ ]` or pressing system Back exits Multi-Select Mode and clears `selectedTaskIds`.

---

### 3.2 Docked Bottom Action Strip Operations

The bottom docked action strip provides 5 core bulk operations:

#### Action 1: 📅 Batch Date Shift Modal (`BatchDateShiftSheet.kt`)
Tapping `[ 📅 Date ]` opens a compact bottom modal:
```
+-----------------------------------------------------------------+
|  RESCHEDULE 3 TASKS                                             |
+-----------------------------------------------------------------+
|  [ ⚡ Today ]         [ 🌅 Tomorrow ]     [ ⏩ +1 Day Shift ]    |
|  [ 📅 Next Week (+7d) ]                   [ 🕒 Pick Custom Date ]|
+-----------------------------------------------------------------+
```
* **Relative `+1 Day` Shift:** Shifts each selected task's existing due date forward by exactly 24 hours, preserving individual staggered due dates across the batch.
* **Absolute Date Setting (*Today*, *Tomorrow*, *Custom*):** Updates all selected tasks to the target calendar date.

#### Action 2: 📁 2-Step List & Section Relocation (`BatchMoveSheet.kt`)
Tapping `[ 📁 Move ]` opens a 2-step destination sheet:
```
STEP 1: SELECT DESTINATION LIST
+-----------------------------------------------------------------+
|  MOVE 3 TASKS TO:                                               |
+-----------------------------------------------------------------+
|  📥 Inbox                                                       |
|  📐 Olympiad Geometry (#1565C0)                                 |
|  ⚛️ PW Neev Diamond (#576481)                                   |
+-----------------------------------------------------------------+

STEP 2: SELECT SECTION (IF TARGET LIST IS SECTIONED)
+-----------------------------------------------------------------+
|  PW NEEV DIAMOND — SELECT SECTION:                              |
+-----------------------------------------------------------------+
|  ( ) Root / Unsectioned                                         |
|  (•) Physics                                                    |
|  ( ) Chemistry                                                  |
|  ( ) Mathematics                                                |
|                                                     [ Confirm ] |
+-----------------------------------------------------------------+
```
* If the target list has no sections, selection finishes immediately in Step 1.
* If the target list has sections, Step 2 presents the section radio options (defaulting to the first section or Root).

#### Action 3: 🚩 Batch Priority Picker
Tapping `[ 🚩 Priority ]` displays a popup menu with the 4 priority flags:
* `[ 🚩 High ]` (`priority = 3`, `#E53935`)
* `[ 🚩 Medium ]` (`priority = 2`, `#FB8C00`)
* `[ 🚩 Low ]` (`priority = 1`, `#1565C0`)
* `[ 🏳️ None ]` (`priority = 0`)

#### Action 4: ✓ Batch Complete (System-Wide Subtask Invariant)
Tapping `[ ✓ Done ]`:
* Atomically marks all selected tasks complete:
  `UPDATE tasks SET is_completed = 1, completed_at = :now WHERE id IN (:taskIds)`
* **MANDATORY INVARIANT:** Atomically marks all subtasks belonging to the selected tasks as completed too:
  `UPDATE subtasks SET is_completed = 1 WHERE task_id IN (:taskIds)`
* Plays completion spring animation, dismisses selection mode, and shows a Snackbar:
  *"3 tasks completed"* with `[ Undo ]`.

#### Action 5: 🗑️ Batch Trash & Recurring Series Scope
Tapping `[ 🗑️ Trash ]`:
1. **Recurring Tasks Check:**
   * If **zero** recurring tasks are in the selection: Immediately moves all tasks to Trash with 30-day retention and 5s Undo snackbar.
   * If **one or more** recurring tasks are present, displays the **Batch Recurrence Scope Modal**:
     ```
     +-------------------------------------------------------+
     |  Delete 3 Tasks (Including Recurring Tasks)?          |
     +-------------------------------------------------------+
     |  Some selected tasks are recurring series.            |
     |  Apply delete scope to recurring tasks:               |
     |                                                       |
     |  (•) This instance only                               |
     |  ( ) This and future occurrences                      |
     |  ( ) All tasks in the series                          |
     |                                                       |
     |  [ Move to Trash ]                       [ Cancel ]   |
     +-------------------------------------------------------+
     ```
2. Sets `is_deleted = 1`, `deleted_at = System.currentTimeMillis()`, and preserves `deleted_from_list_id` and `deleted_from_section_id`.

---

## 4. Technical Architecture & Database Transactions

All batch mutations execute within atomic Room SQLite `@Transaction` methods in `TaskDao.kt`:

### 4.1 Room SQLite DAO Queries (`TaskBatchDao.kt`)

```kotlin
package com.flux.done.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TaskBatchDao {

    // --- BATCH COMPLETE WITH SUBTASK INVARIANT ---
    @Query("UPDATE tasks SET is_completed = 1, completed_at = :now, updated_at = :now WHERE id IN (:taskIds)")
    suspend fun markTasksCompleted(taskIds: List<String>, now: Long)

    @Query("UPDATE subtasks SET is_completed = 1 WHERE task_id IN (:taskIds)")
    suspend fun markSubtasksCompletedForTasks(taskIds: List<String>)

    @Transaction
    suspend fun executeBatchComplete(taskIds: List<String>, now: Long) {
        markTasksCompleted(taskIds, now)
        markSubtasksCompletedForTasks(taskIds)
    }

    // --- BATCH RESCHEDULE (ABSOLUTE) ---
    @Query("UPDATE tasks SET due_date = :newDueDateMs, updated_at = :now WHERE id IN (:taskIds)")
    suspend fun executeBatchReschedule(taskIds: List<String>, newDueDateMs: Long?, now: Long)

    // --- BATCH RESCHEDULE (+1 DAY RELATIVE SHIFT) ---
    @Query("""
        UPDATE tasks 
        SET due_date = CASE 
            WHEN due_date IS NOT NULL THEN due_date + :shiftMs 
            ELSE :defaultDueDateMs 
        END,
        updated_at = :now 
        WHERE id IN (:taskIds)
    """)
    suspend fun executeBatchRelativeDateShift(taskIds: List<String>, shiftMs: Long, defaultDueDateMs: Long, now: Long)

    // --- BATCH MOVE TO LIST & SECTION ---
    @Query("UPDATE tasks SET list_id = :targetListId, section_id = :targetSectionId, updated_at = :now WHERE id IN (:taskIds)")
    suspend fun executeBatchMove(taskIds: List<String>, targetListId: String, targetSectionId: String?, now: Long)

    // --- BATCH PRIORITY UPDATE ---
    @Query("UPDATE tasks SET priority = :priority, updated_at = :now WHERE id IN (:taskIds)")
    suspend fun executeBatchPriority(taskIds: List<String>, priority: Int, now: Long)

    // --- BATCH SOFT-DELETE TO TRASH ---
    @Query("""
        UPDATE tasks 
        SET is_deleted = 1, 
            deleted_at = :now, 
            deleted_from_list_id = list_id, 
            deleted_from_section_id = section_id, 
            updated_at = :now 
        WHERE id IN (:taskIds)
    """)
    suspend fun executeBatchTrash(taskIds: List<String>, now: Long)

    // --- BATCH RESTORE FROM TRASH (UNDO) ---
    @Query("UPDATE tasks SET is_deleted = 0, deleted_at = NULL, updated_at = :now WHERE id IN (:taskIds)")
    suspend fun executeBatchRestore(taskIds: List<String>, now: Long)
}
```

---

## 5. MVI Architecture & State Management

### 5.1 Batch Selection MVI State (`TaskBatchViewModel.kt`)

```kotlin
package com.flux.done.features.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.done.core.database.TaskBatchDao
import com.flux.done.core.database.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatchSelectionUiState(
    val isMultiSelectActive: Boolean = false,
    val selectedTaskIds: Set<String> = emptySet(),
    val totalSelectableCount: Int = 0,
    val isDateShiftSheetOpen: Boolean = false,
    val isMoveSheetOpen: Boolean = false,
    val isPriorityPickerOpen: Boolean = false,
    val isRecurrenceDeleteModalOpen: Boolean = false,
    val hasRecurringTasksInSelection: Boolean = false,
    val undoSnackbarMessage: String? = null
)

sealed interface BatchSelectionIntent {
    data class EnterMultiSelect(val initialTaskId: String?) : BatchSelectionIntent
    data class ToggleTaskSelection(val taskId: String) : BatchSelectionIntent
    object SelectAll : BatchSelectionIntent
    object DeselectAll : BatchSelectionIntent
    object ExitMultiSelect : BatchSelectionIntent
    
    // Actions
    object OpenDateShiftSheet : BatchSelectionIntent
    data class ApplyBatchDate(val dueDateMs: Long?) : BatchSelectionIntent
    object ApplyBatchPlusOneDayShift : BatchSelectionIntent
    
    object OpenMoveSheet : BatchSelectionIntent
    data class ApplyBatchMove(val targetListId: String, val targetSectionId: String?) : BatchSelectionIntent
    
    data class ApplyBatchPriority(val priority: Int) : BatchSelectionIntent
    
    object ExecuteBatchComplete : BatchSelectionIntent
    object RequestBatchTrash : BatchSelectionIntent
    data class ConfirmBatchTrash(val recurrenceScope: RecurrenceScope) : BatchSelectionIntent
    
    object ExecuteUndoLastAction : BatchSelectionIntent
    object DismissSnackbar : BatchSelectionIntent
}

enum class RecurrenceScope {
    THIS_INSTANCE_ONLY,
    THIS_AND_FUTURE,
    ALL_IN_SERIES
}
```

---

## 6. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **Subtask Completion Invariant** | Whenever a task is completed in batch mode, all associated subtasks in `subtasks` table are updated to `is_completed = 1` within the same `@Transaction`. Zero lingering uncompleted subtasks remain. |
| **All Tasks Deselected While in Multi-Select Mode** | If Ari unchecks the last selected task, Multi-Select Mode **remains active** with `0 Selected`, allowing him to select other cards or tap `Select All` without jarring UI resets. Tapping `[ ✕ ]` exits cleanly. |
| **Batch Move into Unsectioned List** | If the target list has `is_sectioned = false`, `section_id` is automatically set to `null` for all moved tasks. |
| **Relative `+1 Day` Shift on Date-Less Tasks** | If a task in the selection has `due_date = null`, the `+1 Day` shift sets its due date to tomorrow (`startOfTomorrowMs`). Tasks with existing due dates advance by +24 hours. |
| **Undo 5-Second Snackbar** | Tapping `[ Undo ]` on the batch delete snackbar re-executes `executeBatchRestore(taskIds)`, returning all tasks back to their original lists with active status. |
| **Mass Select Performance (100+ tasks)** | Selection state is maintained as an immutable `Set<String>` of task IDs; rendering is optimized with Compose `key(task.id)` to prevent unnecessary list recompositions. |

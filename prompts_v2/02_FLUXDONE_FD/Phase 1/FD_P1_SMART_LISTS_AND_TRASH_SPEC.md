# 📋 Topic FD-P1-6: Smart Lists, Filter Engine & Trash Specification

**Document ID:** `FD_P1_SMART_LISTS_AND_TRASH_SPEC`  
**Phase:** Phase 1 (Core Task, Hierarchy & Habit Foundation)  
**Target Package:** `com.flux.done.features.smartlists` & `com.flux.done.features.trash`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + DataStore)  
**Screen References:** SCR-01 (Task List), SCR-05 (Smart Lists Engine), SCR-08 (Side Drawer), SCR-12 (Trash Screen)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari's academic routine, obligations are distributed across multiple subjects (Number Theory, Geometry, Combinatorics, JEE Physics, Chemistry, etc.). Navigating into separate domain lists just to find what needs to be solved before nightfall introduces friction.

**Smart Lists** provide unified, auto-filtered aggregation lenses that dynamically compute views over the entire task repository without altering task ownership:
* **`Today`:** All tasks due today, with a prioritized **Overdue** batch and an interactive **One-by-One Overdue Rescheduling Modal**.
* **`Tomorrow`:** Rapid preview and prep for the next study day.
* **`Upcoming`:** Forward-looking horizon grouped into Next 7 Days and Later.
* **`All`:** Complete active inventory across all folders and lists.
* **`Completed`:** Dedicated archive categorized strictly into **Today**, **Yesterday**, **Previous 7 Days**, and **Older (Before 7 Days)**, equipped with a live search filter.
* **Visibility Customization:** Dual entry points (Drawer Edit button and Settings) allowing Ari to toggle visibility and reorder smart list items.
* **Soft-Delete Trash (SCR-12):** 30-day retention countdown, 1-tap restore with safe list fallback, and permanent purge safety gates.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Hierarchy & Domains** | [`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md) | Displays parent list badge and 12-swatch domain color on task rows. |
| **Task Management & Notes** | [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) | Checkbox completion animations, swipe-to-trash, swipe-to-tomorrow, and subtask sync. |
| **Recurrence Engine** | [`FD_P1_RECURRENCE_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_RECURRENCE_ENGINE_SPEC.md) | Completing recurring tasks in smart lists spawns next occurrence in target list. |
| **Settings & IPC Bridge (Phase 2)** | [`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md) | Smart list visibility settings persisted in DataStore; shared with Phase 2 Settings screen. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Smart Lists Navigation Drawer Integration (SCR-08)

The top section of the side navigation drawer displays the enabled Smart Lists with live badge counters:

```
+-----------------------------------------------------------------+
|  FLUXDONE                                                 [ ✎ ] | <-- Quick Edit Smart Lists
+-----------------------------------------------------------------+
|  📅  Today                                             [ 5 ]    | <-- Amber pill if overdue exists
|  🌅  Tomorrow                                          [ 3 ]    |
|  📆  Upcoming                                         [ 12 ]    |
|  📋  All Active                                       [ 28 ]    |
|  ✓   Completed                                                  |
+-----------------------------------------------------------------+
|  FOLDERS & LISTS                                          [ + ] |
|  ▶ 📐 Olympiad Prep (6)                                         |
|  ▶ ⚛️ JEE Foundation (3)                                         |
+-----------------------------------------------------------------+
|  🗑️  Trash (30-day auto-purge)                         [ 2 ]    |
|  ⚙️  Settings                                                    |
+-----------------------------------------------------------------+
```

#### Dual Entry Points for Smart List Visibility Customization:
1. **Drawer Shortcut:** Tapping the `[ ✎ ]` (Edit) icon at the header of the Smart Lists section opens `SmartListConfigSheet`.
2. **Settings Screen:** Navigating to `Settings -> Smart Lists` opens the identical configuration surface.

---

### 3.2 Today View & Overdue Rescheduling Flow (SCR-05 / SCR-01)

When tasks have `due_date < startOfToday`, they are hoisted to an **Overdue** card batch above Today's tasks:

```
+-----------------------------------------------------------------+
|  ←  Today                                        🔍   ⋮        |
|     Tuesday, October 13 • 5 tasks pending                       |
+-----------------------------------------------------------------+
|  ⚠️ OVERDUE (3)                    [ 📅 Reschedule Overdue ]     |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [ ] Solve 5 Diophantine Equations               !High      │ |
|  │     Due: Yesterday • [Number Theory #2E7D32]               │ |
|  │ [ ] Review Incircle Lemma (Geometry)           !Med       │ |
|  │     Due: Oct 11 • [Geometry #1565C0]                       │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  TODAY'S SCHEDULE                                               |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [ ] Thermodynamics Problem Set 4               !High      │ |
|  │     16:00 - 17:30 • [Physics #576481]                      │ |
|  │ [ ] Combinatorics Double Counting              !Med       │ |
|  │     18:00 - 19:30 • [Combinatorics #43A047]                │ |
|  └────────────────────────────────────────────────────────────┘ |
+-----------------------------------------------------------------+
```

#### Dedicated One-by-One Overdue Reschedule Modal (`RescheduleOverdueSheet.kt`):
Tapping `[ 📅 Reschedule Overdue ]` opens a focused triage sheet that walks through overdue tasks sequentially:

```
+-----------------------------------------------------------------+
|  RESCHEDULE OVERDUE TASKS                            (1 of 3)   |
+-----------------------------------------------------------------+
|  Task: Solve 5 Diophantine Equations                            |
|  List: [ Number Theory #2E7D32 ]      Priority: !High           |
|  Originally Due: Oct 12 (Yesterday)                             |
|                                                                 |
|  Select New Date:                                               |
|  [ ⚡ Today ]       [ 🌅 Tomorrow ]     [ 📅 Next Week (+7d) ]   |
|  [ 🕒 Pick Custom Date & Time ]         [ 🚫 Won't Do / Trash ] |
|                                                                 |
|  -------------------------------------------------------------  |
|  [ ⏩ Skip for Now ]                    [ Shift All to Today ]  |
+-----------------------------------------------------------------+
```
* **Step-by-Step Triage:** Tapping any date button immediately updates the task's `due_date`, plays a light tick haptic, and slides in task `(2 of 3)`.
* **Safe Exit:** User can dismiss the sheet at any time; completed adjustments are committed immediately.
* **Batch Fallback:** Tapping `[ Shift All to Today ]` updates all remaining overdue items in an atomic Room transaction.

---

### 3.3 Completed Smart List with 4 Date Groupings (SCR-05)

The **Completed** smart list displays past finished work, organized into the **4 mandatory chronological buckets**:

```
+-----------------------------------------------------------------+
|  ←  Completed                                                   |
|  [ 🔍 Search completed tasks...                               ] |
+-----------------------------------------------------------------+
|  ▼ TODAY (3 completed)                                          |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [✓] Rotational Dynamics JEE Adv (Done 14:15)    #576481   │ |
|  │ [✓] Read Inversion Geometry Ch 3 (Done 11:30)    #1565C0   │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  ▼ YESTERDAY (2 completed)                                      |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [✓] Combinatorics Graph Theory Set (Done 21:00)  #43A047   │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  ▼ PREVIOUS 7 DAYS (8 completed)                                |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [✓] Functional Equations Test 1 (Oct 10)         #FB8C00   │ |
|  │ [✓] Organic Chemistry Carbonyls (Oct 08)         #576481   │ |
|  └────────────────────────────────────────────────────────────┘ |
|                                                                 |
|  ▼ OLDER (24 completed)                                         |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ [✓] Modular Arithmetic Induction (Sep 28)        #2E7D32   │ |
|  └────────────────────────────────────────────────────────────┘ |
+-----------------------------------------------------------------+
```

* **Interactive Uncomplete:** Tapping the checked `[✓]` box unchecks the task, animates it out, sets `is_completed = 0, completed_at = null`, and restores it to its active parent list and section.
* **Live Search Filter:** As Ari types in the search bar, tasks across all 4 buckets filter in real-time by title and notes content.

---

### 3.4 Soft-Delete Trash Screen (`TrashScreen.kt` — SCR-12)

Soft-deleted items are safely housed in Trash with a transparent 30-day countdown before purge:

```
+-----------------------------------------------------------------+
|  ←  Trash (3 items)                        [ Empty Trash 🗑️ ]   |
|  Items in Trash are permanently deleted after 30 days.          |
+-----------------------------------------------------------------+
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ Mock Test 2 Scratch Notes                                  │ |
|  │ Deleted from: Olympiad Tests • 28 days left                │ |
|  │                                  [ Restore ]  [ Delete 🗑️ ] │ |
|  └────────────────────────────────────────────────────────────┘ |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │ Old Physics Chapter 1 Homework                             │ |
|  │ Deleted from: Neev Diamond • 12 days left                  │ |
|  │                                  [ Restore ]  [ Delete 🗑️ ] │ |
|  └────────────────────────────────────────────────────────────┘ |
+-----------------------------------------------------------------+
```

#### Restore Lineage Contract:
1. When user taps `[ Restore ]`:
   * Checks if `deleted_from_list_id` still exists in `task_lists`.
   * If yes: Sets `is_deleted = 0`, `deleted_at = null`, `list_id = deleted_from_list_id`. If `deleted_from_section_id` exists in `sections`, restores section assignment; otherwise sets `section_id = null`.
   * If no (parent list was deleted): Safely sets `list_id = "inbox"` and `section_id = null`.
   * Displays Snackbar: *"Restored to [Target List Name]"* with quick `[ View ]` action.

#### Permanent Purge Safety Gates:
* **Single Item Hard Delete:** Shows confirmation dialog: *"Delete Permanently? This cannot be undone."* $\to$ Executes hard delete.
* **Empty Trash:** Shows warning alert dialog: *"Empty Trash? All items will be permanently erased from your device."* with red confirmation button.

---

## 4. Technical Architecture & Data Model

### 4.1 Room SQLite Schema Updates

To support safe restoration lineage and auto-purge countdowns, the `tasks` and `habits` tables track deletion metadata:

```sql
-- tasks table deletion fields
ALTER TABLE tasks ADD COLUMN deleted_from_list_id TEXT DEFAULT NULL;
ALTER TABLE tasks ADD COLUMN deleted_from_section_id TEXT DEFAULT NULL;
ALTER TABLE tasks ADD COLUMN deleted_at INTEGER DEFAULT NULL;
```

### 4.2 DataStore Preferences Contract for Smart Lists Visibility

```kotlin
package com.flux.done.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SmartListPreferencesKeys {
    val SHOW_TODAY = booleanPreferencesKey("smart_list_show_today")         // Always true (locked)
    val SHOW_TOMORROW = booleanPreferencesKey("smart_list_show_tomorrow")   // Default true
    val SHOW_UPCOMING = booleanPreferencesKey("smart_list_show_upcoming")   // Default true
    val SHOW_ALL = booleanPreferencesKey("smart_list_show_all")             // Default true
    val SHOW_COMPLETED = booleanPreferencesKey("smart_list_show_completed") // Default true
    val SMART_LISTS_ORDER = stringPreferencesKey("smart_lists_order")       // CSV: "TODAY,TOMORROW,UPCOMING,ALL,COMPLETED"
}

enum class SmartListType(val title: String, val iconRes: String) {
    TODAY("Today", "ic_today"),
    TOMORROW("Tomorrow", "ic_tomorrow"),
    UPCOMING("Upcoming", "ic_upcoming"),
    ALL("All", "ic_all_tasks"),
    COMPLETED("Completed", "ic_completed")
}
```

---

### 4.3 Room SQLite DAO Queries (`SmartListsDao.kt` & `TrashDao.kt`)

```kotlin
package com.flux.done.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartListsDao {

    // --- TODAY & OVERDUE ---
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND is_completed = 0 
          AND due_date IS NOT NULL 
          AND due_date < :startOfTodayMs 
        ORDER BY due_date ASC, priority DESC
    """)
    fun observeOverdueTasks(startOfTodayMs: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND is_completed = 0 
          AND due_date >= :startOfTodayMs 
          AND due_date <= :endOfTodayMs 
        ORDER BY start_time ASC, priority DESC, created_at ASC
    """)
    fun observeTodayTasks(startOfTodayMs: Long, endOfTodayMs: Long): Flow<List<TaskEntity>>

    // --- TOMORROW ---
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND is_completed = 0 
          AND due_date >= :startOfTomorrowMs 
          AND due_date <= :endOfTomorrowMs 
        ORDER BY start_time ASC, priority DESC
    """)
    fun observeTomorrowTasks(startOfTomorrowMs: Long, endOfTomorrowMs: Long): Flow<List<TaskEntity>>

    // --- UPCOMING ---
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND is_completed = 0 
          AND due_date > :endOfTodayMs 
        ORDER BY due_date ASC, priority DESC
    """)
    fun observeUpcomingTasks(endOfTodayMs: Long): Flow<List<TaskEntity>>

    // --- ALL ACTIVE ---
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND is_completed = 0 
        ORDER BY priority DESC, created_at DESC
    """)
    fun observeAllActiveTasks(): Flow<List<TaskEntity>>

    // --- COMPLETED (WITH 4 CHRONOLOGICAL BUCKETS) ---
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND is_completed = 1 
          AND (:query IS NULL OR :query = '' OR title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY completed_at DESC
    """)
    fun observeCompletedTasks(query: String?): Flow<List<TaskEntity>>

    // Batch Overdue Reschedule
    @Query("UPDATE tasks SET due_date = :newDueDateMs, updated_at = :now WHERE id IN (:taskIds)")
    suspend fun batchRescheduleTasks(taskIds: List<String>, newDueDateMs: Long, now: Long)
}

@Dao
interface TrashDao {

    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 1 
        ORDER BY deleted_at DESC
    """)
    fun observeTrashTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET is_deleted = 0, deleted_at = NULL, list_id = :targetListId, section_id = :targetSectionId WHERE id = :taskId")
    suspend fun restoreTask(taskId: String, targetListId: String, targetSectionId: String?)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun permanentlyDeleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE is_deleted = 1")
    suspend fun emptyTrash()

    // 30-Day Auto Purge Query (Called by WorkManager or App Launch)
    @Query("DELETE FROM tasks WHERE is_deleted = 1 AND deleted_at < :cutoffMs")
    suspend fun purgeExpiredTrash(cutoffMs: Long): Int
}
```

---

## 5. MVI Architecture & State Management

### 5.1 Smart Lists MVI State (`SmartListsViewModel.kt`)

```kotlin
package com.flux.done.features.smartlists

data class SmartListsUiState(
    val currentType: SmartListType = SmartListType.TODAY,
    val overdueTasks: List<TaskEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val tomorrowTasks: List<TaskEntity> = emptyList(),
    val upcomingSections: Map<String, List<TaskEntity>> = emptyMap(),
    val allTasks: List<TaskEntity> = emptyList(),
    val completedBuckets: CompletedBuckets = CompletedBuckets(),
    val searchQuery: String = "",
    val isRescheduleSheetOpen: Boolean = false,
    val currentRescheduleIndex: Int = 0,
    val isLoading: Boolean = false
)

data class CompletedBuckets(
    val today: List<TaskEntity> = emptyList(),
    val yesterday: List<TaskEntity> = emptyList(),
    val previous7Days: List<TaskEntity> = emptyList(),
    val older: List<TaskEntity> = emptyList()
)

sealed interface SmartListsIntent {
    data class SwitchSmartList(val type: SmartListType) : SmartListsIntent
    data class ToggleTaskComplete(val task: TaskEntity) : SmartListsIntent
    data class UpdateSearchQuery(val query: String) : SmartListsIntent
    object OpenRescheduleSheet : SmartListsIntent
    object CloseRescheduleSheet : SmartListsIntent
    data class RescheduleTask(val taskId: String, val newDueDateMs: Long) : SmartListsIntent
    data class MoveToTrash(val taskId: String) : SmartListsIntent
    object ShiftAllOverdueToToday : SmartListsIntent
}
```

### 5.2 Trash MVI State (`TrashViewModel.kt`)

```kotlin
package com.flux.done.features.trash

data class TrashedTaskItem(
    val task: TaskEntity,
    val originalListName: String,
    val daysRemaining: Int
)

data class TrashUiState(
    val items: List<TrashedTaskItem> = emptyList(),
    val isConfirmEmptyDialogOpen: Boolean = false,
    val pendingDeleteTaskId: String? = null,
    val messageSnackbar: String? = null
)

sealed interface TrashIntent {
    data class RestoreTask(val taskId: String) : TrashIntent
    data class ConfirmPermanentDelete(val taskId: String) : TrashIntent
    object ExecutePermanentDelete : TrashIntent
    object ShowEmptyTrashConfirmation : TrashIntent
    object ExecuteEmptyTrash : TrashIntent
    object DismissDialog : TrashIntent
    object ClearSnackbar : TrashIntent
}
```

---

## 6. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **Parent List Deleted During Trash Retention** | If a task was deleted from a list that was subsequently permanently destroyed, restoring the task reassigns it to the default `inbox` list with `section_id = null`. Task is never orphaned or lost. |
| **Overdue Task Rescheduled via Stepper** | Stepper advances to `(i + 1) of N`. If user reaches end of overdue list, the `RescheduleOverdueSheet` closes automatically with a celebratory *"All overdue tasks rescheduled!"* message. |
| **Completed Tasks Unchecked** | Tapping checkmark in Completed view sets `is_completed = 0` and `completed_at = null`. Task immediately leaves Completed and returns to active smart lists and its source list. |
| **Trash 30-Day Auto-Purge Calculation** | Cutoff calculation uses exact epoch difference: `cutoffMs = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)`. Checked on each app cold start and scheduled daily via `WorkManager`. |
| **All-Day vs Timed Tasks in Today Smart List** | Timed tasks sort at the top chronologically by `start_time ASC`. Untimed date-only tasks appear in an "Anytime Today" group beneath timed items. |

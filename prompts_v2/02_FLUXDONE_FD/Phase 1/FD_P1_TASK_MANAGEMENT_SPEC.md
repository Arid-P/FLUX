# 📝 Topic FD-P1-2: Task Management, Detail Sheet, Subtasks & Markdown Notes Specification

**Document ID:** `FD_P1_TASK_MANAGEMENT_SPEC`  
**Phase:** Phase 1 (Core Task, Hierarchy & Habit Foundation)  
**Target Package:** `com.flux.done.features.tasks`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + StateFlow)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

Tasks are the fundamental atomic units of FluxDone. In an intensive academic and Olympiad workflow, task interaction must be tactile, friction-free, and immediate:
* **Tactile Check-offs:** Checking a task triggers a subtle spring-bounce scale animation, strikes through the title, and waits 400ms before sliding into the completed group (allowing instant re-tap if checked accidentally).
* **Automatic Subtask Completion:** When all subtasks are finished, the parent task completes automatically.
* **Live Markdown Notes:** Notes render rich text formatting (bold, italics, bullet points, monospace code blocks) live as the user types without requiring clunky edit/preview toggle tabs.
* **Rapid Swipe Actions:** Swipe Left deletes to Trash (with 5-second Undo snackbar); Swipe Right shifts due date to Tomorrow.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Hierarchy Baseline** | [`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md) | Tasks bind to `list_id` and optional `section_id`. Inherits list domain color. |
| **Calendar View** | `FD_P1_CALENDAR_TIMELINE_SPEC.md` | Tasks with non-null `start_time` and `end_time` render as time-blocks on the timeline. |
| **Recurrence Engine** | `FD_P1_RECURRENCE_ENGINE_SPEC.md` | Completing a recurring task auto-spawns the next instance per RRULE specification. |
| **FF IPC Bridge (Phase 2)** | [`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md) | In Phase 2, mutating timed tasks broadcasts `com.flux.action.FOCUS_BLOCK_SYNC` to FluxFoxus per [`IPC_SPEC_FD_FF_NATIVE.md`](file:///home/hp/Flux_Prompts/prompts_v2/01_INTEGRATION_SYNC/IPC_SPEC_FD_FF_NATIVE.md). |

---

## 3. UI Specifications & Interaction Design

### 3.1 Task Card Component (`TaskCard.kt` — SCR-01 & SCR-05)

A high-density card rendered inside lists, sections, and smart views:

```
+-----------------------------------------------------------------+
|  [○]  Solve 5 Hard Combinatorics Problems                  [🚩] |
|       📅 Today 4:00 PM - 5:30 PM • 3/5 subtasks • 📋 Comb       |
+-----------------------------------------------------------------+
```

#### Visual Elements & Design Tokens:
* **Checkbox:** Custom 22dp rounded square (4dp corner radius). Unchecked has a 1.5dp border tinted with the list's `color_hex`.
  * **Completion Animation:** Tapping triggers a spring scale-bounce (`scale(0.85f) → scale(1.15f) → scale(1.0f)`), fills the checkbox with list color, draws a white checkmark vector, strikes through the task title, and delivers subtle haptic feedback.
  * **400ms Grace Delay:** The task remains visible in place for 400ms before animating smoothly into the Collapsed/Completed group.
* **Title Text:** 16sp medium typography (`MaterialTheme.typography.bodyLarge`).
* **Metadata Caption Row:** 12sp secondary text displaying:
  * Scheduled time span (e.g. `4:00 PM - 5:30 PM`) or due date.
  * Subtask progress pill (e.g. `3/5 subtasks`).
  * Domain badge: Small rounded capsule showing list name in list color (visible in aggregated views like *Today*).
* **Priority Flag:** Right-aligned icon:
  * High: Crimson Red `#E53935` (Filled Flag)
  * Medium: Warm Amber `#FB8C00` (Filled Flag)
  * Low: Royal Blue `#1565C0` (Filled Flag)
  * None: Transparent (no icon displayed)
* **Pinned State:** If `is_pinned == true`, renders a subtle pin icon `#778D7A` and sorts to the top of the group.

#### Swipe Gestures:
* **Phase 1 Actions (Fixed Defaults):**
  * **Swipe Left (Threshold 40%):** Reveals Crimson Red background (`#E53935`) with trash icon. Releasing soft-deletes the task to Trash (`is_deleted = 1`) and shows an immediate 5-second Undo Snackbar:  
    *"Task moved to Trash"* `[ UNDO ]`.
  * **Swipe Right (Threshold 40%):** Reveals Royal Blue background (`#1565C0`) with calendar icon. Releasing shifts `due_date` to Tomorrow (+24 hours), preserving existing time duration.
* **Phase 2 Customization Roadmap:** In Phase 2, a dedicated Settings section will allow user customization of both swipe left and swipe right gestures (selecting from Delete, Reschedule Tomorrow, Mark Completed, Pin, or None).

---

### 3.2 Task Detail Sheet (`TaskDetailSheet.kt` — SCR-02)

Rendered as an expandable full-height modal bottom sheet or dedicated navigation screen:

```
+-----------------------------------------------------------------+
|  [ < Back ]               [ 📌 Pin ]   [ 🚩 Priority ]   [ ⋮ ]  |
+-----------------------------------------------------------------+
|  Solve 5 Hard Combinatorics Problems                            |
|                                                                 |
|  [ 📋 Combinatorics ▾ ]              [ 📁 Triangles & Cycles ▾ ]|
|  -------------------------------------------------------------  |
|  📅 Due Date: Today, Oct 13                                     |
|  ⏰ Time: 4:00 PM  →  5:30 PM  (90 mins)                        |
|  🔁 Repeat: Mon, Wed, Fri                                       |
|  -------------------------------------------------------------  |
|  SUBTASKS (3/5)                                                 |
|  [✓] Problem 1: Invariance Principle                            |
|  [✓] Problem 2: Pigeonhole with area                            |
|  [✓] Problem 3: Graph vertices coloring                         |
|  [○] Problem 4: Double counting pairs                           |
|  [○] Problem 5: Extremal principle setup                        |
|  [ + Add subtask... ]                                           |
|  -------------------------------------------------------------  |
|  NOTES                                                          |
|  Focus on *extremal principle* and bounds.                      |
|  - Check sheet notes: https://contest.math.org                  |
|  - `n(n-1)/2` pairs minimum                                    |
+-----------------------------------------------------------------+
```

#### Key Functional Sections:
1. **Title Input:** Unframed auto-expanding `TextField` supporting up to 3 lines before internal scrolling.
2. **List & Section Selectors:** Dropdown pills displaying list color dot and section name. Changing list clears section if incompatible.
3. **Time & Recurrence Controls:**
   * Due Date pill: Quick selectors (`Today`, `Tomorrow`, `Next Week`, `Pick Date`).
   * Start & End Time pickers: Auto-computes `duration_minutes` in real time.
   * Recurrence picker: Displays human-readable recurrence description (opens RRULE dialog from Topic FD-P1-4).
4. **Subtasks Checklist Engine (Strictly Lightweight):**
   * Subtasks are **purely lightweight checklist items**: title and checkbox only. Zero subtask dates, zero subtask reminders.
   * Enter key in an empty subtask field automatically creates the next subtask.
   * Drag handle allows reordering via `sort_order`.
5. **Live Markdown Notes Editor & Clickable Links:**
   * Formats headers (`# H1`), bold (`**bold**`), italic (`*italic*`), bullet points (`- list`), and inline code (`` `code` ``) dynamically as the user types.
   * **Clickable Web Links:** Any `http://` or `https://` URL is rendered as a clickable link that opens directly in the system browser via Android Intent.
   * *Phase 4 Roadmap:* Extended markdown capabilities (LaTeX math equations, tables) will expand on this foundation in Phase 4.
6. **Task Duplication Engine (`[ Duplicate Task ]` in Overflow Menu):**
   * Invoked via top-bar overflow menu `[ ⋮ ]` → `[ Duplicate Task ]`.
   * **Deep Copy Invariant:**
     * A brand-new task UUID is generated.
     * The title becomes `"${originalTitle} (copy)"` (e.g. *"Physics Assignment (copy)"*).
     * Deep-copies all subtasks with newly generated subtask UUIDs, preserving their respective completion states.
     * Copies notes, list, section, priority, due date, start/end times, and duration.
     * Resets completion status on the new copy: `is_completed = false`, `completed_at = null`.

---

### 3.3 Quick Task Creation Sheet (`QuickTaskSheet.kt` — SCR-03)

Docked directly above the software keyboard for rapid 3-second task entry:

```
+-----------------------------------------------------------------+
|  What needs to be done?                                         |
|  [ 📅 Today ]  [ ⏰ 4:00 PM ]  [ 🚩 None ]  [ 📋 Combinatorics ] |
|                                                           [ ➤ ] |
+-----------------------------------------------------------------+
|                      [ Software Keyboard ]                      |
+-----------------------------------------------------------------+
```
* **Auto-Focus:** Title field gains immediate keyboard focus upon sheet presentation.
* **Contextual Defaults:** Pre-populates with current list and current calendar time slot if summoned from Calendar View.
* **Submit Action:** Tapping `[ ➤ ]` (or pressing Enter) saves task atomically to SQLite and dismisses the sheet.

---

### 3.4 Date-Only Tasks vs. Timed Tasks & FluxFoxus (FF) IPC Separation

FluxDone explicitly supports two classes of tasks:
1. **Date-Only Tasks (`due_date != null`, `start_time == null`, `end_time == null`):**
   * Represents tasks that must be done on or by a certain day without a locked time window (e.g. *"Submit chemistry lab record today"*).
   * **FF IPC Isolation:** Date-only tasks remain **strictly local to FluxDone**. They do **NOT** broadcast a Focus Block sync to FluxFoxus, because FluxFoxus enforces discrete start/end focus sessions.
2. **Timed Tasks (`start_time != null`, `end_time != null`):**
   * Represents calendar study blocks (e.g. *"4:00 PM – 5:30 PM: IOQM Geometry"*).
   * **FF IPC Synchronization:** Mutating a timed task immediately broadcasts `com.flux.action.FOCUS_BLOCK_SYNC` to FluxFoxus to schedule or update an auto-focus block (`FF_P2_SCHEDULED_SESSIONS_SPEC.md`).

---

## 4. Subtask & Parent Task Automatic Synchronization Rules

To ensure predictable behavior without manual overhead:

### 4.1 Upward Propagation (Subtasks → Parent Task)
1. **Auto-Completion Rule:**  
   When all subtasks of a task are marked as `is_completed = true`:
   * The parent task **automatically completes** (`is_completed = 1`, `completed_at = now`).
   * It triggers the completion animation and slides into the Completed section.
2. **Auto-Uncompletion Rule:**  
   If a completed parent task has any of its subtasks unchecked:
   * The parent task **automatically reverts to active** (`is_completed = 0`, `completed_at = null`).

### 4.2 Downward Propagation (Parent Task → Subtasks)
1. **Manual Parent Check-off:**  
   If the user manually checks off the parent task directly while some subtasks are still incomplete:
   * **All remaining incomplete subtasks are automatically marked completed**.
2. **Manual Parent Uncheck:**  
   If the user unchecks a previously completed parent task:
   * The parent becomes active; its subtasks retain their individual completed states so progress is never lost.

---

## 5. Room Database Schema

### 5.1 Entity: `TaskEntity`

```kotlin
package com.flux.done.features.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flux.done.features.hierarchy.data.entity.SectionEntity
import com.flux.done.features.hierarchy.data.entity.TaskListEntity

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("list_id"),
        Index("section_id"),
        Index("due_date"),
        Index("start_time"),
        Index("is_completed"),
        Index("is_deleted")
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "list_id")
    val listId: String,

    @ColumnInfo(name = "section_id")
    val sectionId: String? = null,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "start_time")
    val startTime: Long? = null, // UTC Epoch ms

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null, // UTC Epoch ms

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int? = null,

    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null, // UTC Epoch ms (Day boundary or exact due)

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "priority")
    val priority: Int = 0, // 0=None, 1=Low, 2=Medium, 3=High

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null, // RFC 5545 JSON RRULE

    @ColumnInfo(name = "recurrence_series_id")
    val recurrenceSeriesId: String? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false, // Soft-delete for Trash

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "deleted_from_list_id")
    val deletedFromListId: String? = null,

    @ColumnInfo(name = "deleted_from_section_id")
    val deletedFromSectionId: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

### 5.2 Entity: `SubtaskEntity`

```kotlin
package com.flux.done.features.tasks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("task_id"),
        Index("sort_order")
    ]
)
data class SubtaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 6. Room DAO Specifications (`TaskDao.kt`)

```kotlin
package com.flux.done.features.tasks.data.dao

import androidx.room.*
import com.flux.done.features.tasks.data.entity.SubtaskEntity
import com.flux.done.features.tasks.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

data class TaskWithSubtasks(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    val subtasks: List<SubtaskEntity>
)

@Dao
interface TaskDao {

    // --- Task Observation ---
    @Transaction
    @Query("SELECT * FROM tasks WHERE list_id = :listId AND is_deleted = 0 ORDER BY is_pinned DESC, is_completed ASC, sort_order ASC, created_at DESC")
    fun observeTasksForList(listId: String): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE section_id = :sectionId AND is_deleted = 0 ORDER BY is_pinned DESC, is_completed ASC, sort_order ASC")
    fun observeTasksForSection(sectionId: String): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun observeTaskById(taskId: String): Flow<TaskWithSubtasks?>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    // --- Task Mutations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET is_completed = :completed, completed_at = :completedAt WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: String, completed: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET is_deleted = 1, deleted_at = :deletedAt, deleted_from_list_id = list_id, deleted_from_section_id = section_id WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: String, deletedAt: Long)

    @Query("UPDATE tasks SET is_deleted = 0, deleted_at = NULL WHERE id = :taskId")
    suspend fun restoreTask(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun permanentlyDeleteTask(taskId: String)

    // --- Subtasks ---
    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY sort_order ASC")
    fun observeSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Query("UPDATE subtasks SET is_completed = :completed WHERE id = :subtaskId")
    suspend fun setSubtaskCompleted(subtaskId: String, completed: Boolean)

    @Query("UPDATE subtasks SET is_completed = :completed WHERE task_id = :taskId")
    suspend fun setAllSubtasksForTaskCompleted(taskId: String, completed: Boolean)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: String)
}
```

---

## 7. Edge Cases & Defensive Engineering Guardrails

To prevent implementation ambiguity and ensure rock-solid stability:

### Edge Case 1: Empty Subtask Clean Up
* **Scenario:** User taps `[ + Add subtask ]`, types nothing, and closes the keyboard or taps away.
* **Invariant:** The ViewModel filters out zero-length or whitespace-only subtasks before database commit, preventing blank phantom checklist rows.

### Edge Case 2: Subtask Deletion Triggering Parent Completion
* **Scenario:** A task has 2 subtasks: Subtask 1 is completed (`✓`), Subtask 2 is incomplete (`○`). The user deletes Subtask 2.
* **Invariant:** Now 100% of the remaining subtasks (1 of 1) are complete. The repository triggers the upward propagation rule, **automatically marking the parent task completed**.

### Edge Case 3: Undo Snackbar Race Conditions
* **Scenario:** User swipes to delete Task A, immediately taps `[ UNDO ]`, but already swiped to delete Task B.
* **Invariant:** The Snackbar action captures the exact `taskId` of the deleted item in its closure. Invoking Undo targets strictly `taskId = "t1"`, atomically flipping `is_deleted = 0` without affecting Task B.

### Edge Case 4: Duration Computation Discrepancy
* **Scenario:** User selects `start_time = 11:30 PM` and `end_time = 01:00 AM` (crosses midnight).
* **Invariant:** Duration calculator checks:  
  `if (endTime < startTime) endTime += 24.hours`. Computed `duration_minutes` correctly evaluates to 90 minutes instead of a negative number.

---

## 8. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class TaskRepositorySyncTest {

    private lateinit var db: FluxDoneDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxDoneDatabase::class.java).build()
        taskDao = db.taskDao()
        repository = TaskRepositoryImpl(taskDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun checkingLastSubtask_autoCompletesParentTask() = runTest {
        // Given: A task with 2 subtasks, 1 already completed
        val task = createSampleTask("task-1", "Solve Geometry Set")
        taskDao.insertTask(task)
        val s1 = SubtaskEntity("s1", "task-1", "Problem 1", isCompleted = true, sortOrder = 0)
        val s2 = SubtaskEntity("s2", "task-1", "Problem 2", isCompleted = false, sortOrder = 1)
        taskDao.insertSubtask(s1)
        taskDao.insertSubtask(s2)

        // When: s2 is marked completed via repository
        repository.setSubtaskCompleted("s2", true)

        // Then: Parent task is automatically completed in Room
        val updatedTask = taskDao.getTaskById("task-1")
        assertNotNull(updatedTask)
        assertTrue(updatedTask!!.isCompleted)
        assertNotNull(updatedTask.completedAt)
    }

    @Test
    fun uncheckingSubtask_revertsCompletedParentToActive() = runTest {
        // Given: Completed task with 2 completed subtasks
        val task = createSampleTask("task-1", "Review Notes", isCompleted = true)
        taskDao.insertTask(task)
        taskDao.insertSubtask(SubtaskEntity("s1", "task-1", "Part 1", isCompleted = true, sortOrder = 0))
        taskDao.insertSubtask(SubtaskEntity("s2", "task-1", "Part 2", isCompleted = true, sortOrder = 1))

        // When: Subtask s1 is unchecked
        repository.setSubtaskCompleted("s1", false)

        // Then: Parent task automatically reverts to active
        val updatedTask = taskDao.getTaskById("task-1")
        assertFalse(updatedTask!!.isCompleted)
        assertNull(updatedTask.completedAt)
    }

    @Test
    fun checkingParentTask_completesAllRemainingSubtasks() = runTest {
        val task = createSampleTask("task-1", "Homework", isCompleted = false)
        taskDao.insertTask(task)
        taskDao.insertSubtask(SubtaskEntity("s1", "task-1", "Q1", isCompleted = true, sortOrder = 0))
        taskDao.insertSubtask(SubtaskEntity("s2", "task-1", "Q2", isCompleted = false, sortOrder = 1))

        // When: User manually checks off parent task
        repository.setTaskCompleted("task-1", true)

        // Then: Subtask s2 is automatically marked completed
        val subtasks = taskDao.observeSubtasksForTask("task-1").first()
        assertTrue(subtasks.all { it.isCompleted })
    }
}
```

---

## 9. Deliverables & Acceptance Checklist

- [ ] **Tactile Checkbox & 400ms Delay:** Checkbox scale-bounce animation, title strike-through, haptic feedback, and 400ms grace period before moving to completed group.
- [ ] **Bidirectional Subtask Sync:** Checking last subtask auto-completes parent; unchecking subtask uncompletes parent; checking parent completes all subtasks.
- [ ] **Live Markdown Notes:** Notes editor renders formatting (bold, italic, bullets, code) live as typed.
- [ ] **Swipe Gestures:** Swipe Left soft-deletes to Trash with 5s Undo snackbar; Swipe Right reschedules due date to Tomorrow.
- [ ] **Task Detail Sheet (SCR-02):** Unframed title field, list/section selectors, date/time pickers, subtasks reordering, and live notes.
- [ ] **Quick Add Sheet (SCR-03):** Docked above keyboard with instant auto-focus and 1-tap save.
- [ ] **Cross-Midnight Duration Math:** Start/end times spanning past midnight compute positive minutes correctly.
- [ ] **Zero-Mock Architecture:** Backed by real Room DAOs, MVI ViewModels, and unit test suite.

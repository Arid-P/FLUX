# 📅 Topic FD-P1-3: Calendar View & Timeline-Tapping Time Blocking Specification

**Document ID:** `FD_P1_CALENDAR_TIMELINE_SPEC`  
**Phase:** Phase 1 (Core Task, Hierarchy & Habit Foundation)  
**Target Package:** `com.flux.done.features.calendar`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose Custom Layout / Canvas + Room DAO + StateFlow)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari’s academic workflow, the Calendar View (SCR-04) is **not a passive agenda viewer**. It is the **primary interactive planning canvas** where study blocks are visually mapped into open time gaps between lectures, problem-solving, and revision.

### 1.1 Phase 1 View Modes
* **1-Day View:** High-density, single-day vertical schedule (ideal for daily execution).
* **3-Day View:** Default multi-day planning view (yesterday, today, tomorrow) allowing rapid gap identification.
* *(Note: 7-Day Full Week view and external Google Calendar overlay are explicitly slated for Phase 3).*

---

## 2. Layout Architecture & Viewport Anatomy

The Calendar View uses a fixed-viewport custom Compose layout consisting of two coordinated vertical sections:

```
+-----------------------------------------------------------------+
|  [ < Oct 2026 ▾ ]         [ 1-Day | 3-Day ]         [ TODAY ]   |
+-----------------------------------------------------------------+
|  ALL-DAY STRIP (Date-Only Tasks)                                |
|  [ Submit Chemistry Record ]                                    |
|  [ Revise Number Theory Theorem 4 ]                             |
+=================================================================+
|  24-HOUR SCROLLABLE TIMELINE GRID                               |
|  08:00                                                          |
|  09:00                                                          |
|  10:00 ┌──────────────────────────────────────────────────────┐ |
|        │ IOQM Geometry: Triangles                   (90m)     │ |
|  11:00 │ 10:00 AM - 11:30 AM                        [✓]       │ |
|        └──────────────────────────────────────────────────────┘ |
|  12:00 ---------------------------------- [ NOW: 12:15 PM ● ]-- |
|  01:00                                                          |
|  02:00 ┌───────────────────────────┬──────────────────────────┐ |
|        │ Physics Assignment (55m)  │ Chemistry Review (55m)   │ |
|  03:00 │ 02:10 PM - 03:05 PM   [○] │ 02:10 PM - 03:05 PM  [○] │ |
|        └───────────────────────────┴──────────────────────────┘ |
+-----------------------------------------------------------------+
```

### 2.1 Dynamic All-Day Strip (Date-Only Tasks)
Tasks with a `due_date` but no specific start/end times (`start_time == null`) do not belong on the hourly grid. They are displayed in the **All-Day Strip** pinned directly above the timeline:

* **Dynamic Height Allocation:**
  * 0 date-only tasks: Strip height collapses to `0.dp`.
  * 1 date-only task: Height equals `24.dp` (compact chip).
  * $N$ date-only tasks: Expands to fit $N$ chips cleanly (up to 3 visible chips before internal scrolling).
* **Viewport Preservation:**
  * The timeline parent occupies the available screen height.
  * As the All-Day strip expands, the 24-hour scrollable timeline underneath dynamically shrinks its viewport height so the entire screen never pushes bottom navigation tabs out of view.

---

### 2.2 24-Hour Timeline Grid Specs
* **Hour Height:** Exactly 64dp per hour (`TimelineHourHeight = 64.dp`). Total scrollable canvas height = `1536.dp` ($24 \times 64$).
* **Dividers:**
  * Solid hairline divider every 60 minutes (`1.dp`, `color = borderSubtle`).
  * Dashed/dotted divider every 30 minutes (`0.5.dp`).
* **Moving "Now" Indicator Line:**
  * A bold horizontal Red/Coral line (`#E53935`, 2dp thickness) anchored at the exact current minute:  
    $$Y_{\text{now}} = (\text{currentHour} \times 64\text{dp}) + \left(\frac{\text{currentMinute}}{60} \times 64\text{dp}\right)$$
  * Features a circular 6dp dot on the left axis and auto-advances every 60 seconds via `LaunchedEffect(Unit)`.

---

## 3. Timeline-Tapping Creation Flow (Primary Workflow)

Creating a study block directly on the timeline requires minimal friction:

```
[ Tap Empty Slot (e.g. 4:10 PM) ]
                 │
                 ▼
[ Snap Start Time to Nearest 15m (4:15 PM) ]
                 │
                 ▼
[ Render Translucent Ghost Block (30% opacity, dashed border, default 60m) ]
                 │
                 ▼
[ Optional: Drag Ghost Bottom Edge to Set Duration ]
                 │
                 ▼
[ Quick Task Sheet (SCR-03) Slides Up with Start/End Times Pre-Filled ]
                 │
                 ▼
[ Enter Title & Tap Submit ]
                 │
                 ▼
[ Ghost Block Replaced by Solid Colored Task Block ]
```

1. **Tap Detection & Snapping:** Tapping an empty space on the grid snaps the start time to the nearest 15-minute slot (`:00`, `:15`, `:30`, `:45`).
2. **Ghost Block:** A dashed placeholder appears immediately in the active theme color (`30% opacity`). Default height is 60 minutes (`64.dp`).
3. **Optional Edge-Drag:** Dragging the bottom edge of the ghost block allows setting an exact custom end time before confirming.
4. **Instant Commit:** Submitting the Quick Task Sheet converts the ghost block into an active task in Room SQLite and renders the solid colored block.

---

## 4. Drag-to-Reschedule: Strict Duration Preservation

When rescheduling existing study blocks, **duration must remain completely unchanged**:

### 4.1 The Duration Preservation Rule
* **The Invariant:**  
  $$\text{Task Duration} = \text{endTime} - \text{startTime} \equiv \text{CONSTANT}$$
* **Behavior:**
  * When long-pressing and dragging a 55-minute task, the block moves as a solid unit.
  * The drop location sets the new `startTime`.
  * The new end time is calculated strictly as:  
    $$\text{newEndTime} = \text{newStartTime} + \text{originalDuration}$$
  * **Zero Duration Snapping:** A 55-minute task **never** snaps to 60 minutes or 45 minutes. Its 55-minute length is preserved to the exact second.
* **Cross-Day Dragging:** In 3-Day view, dragging horizontally past the column boundary shifts the task to the adjacent day while preserving its exact time and duration.

---

## 5. Completed Tasks on the Timeline

Checking off tasks on the timeline must preserve visual history without cluttering focus:

* **Visual State for Completed Blocks:**
  * **50% Opacity:** The entire block fades to `alpha = 0.5f`.
  * **Strike-Through Title:** The task title is struck through with a horizontal line.
  * **Checked Checkbox:** The inline 18dp checkbox renders with list-color fill and white checkmark.
* **Preserved Position:** The block **remains in its scheduled time slot**. It does **not** disappear or move to the bottom, allowing Ari to visually audit where time was spent throughout the day.
* **Reversible:** Tapping the checkbox again immediately un-completes the task and restores full 100% opacity.

---

## 6. Overlap & Collision Resolution Math

When multiple tasks are scheduled in the same time window (e.g. concurrent classes or overlapping revision blocks):

```
+-----------------------------------------------------------------+
|  Column Width = AvailableDayWidth / TotalOverlappingTasks       |
|                                                                 |
|  ┌───────────────────────────┬──────────────────────────┐       |
|  │ Block A (Left: 0%)        │ Block B (Left: 50%)      │       |
|  │ Width: 50%                │ Width: 50%               │       |
|  └───────────────────────────┴──────────────────────────┘       |
+-----------------------------------------------------------------+
```

### 6.1 Deterministic Overlap Algorithm
```kotlin
fun layoutTimelineColumns(tasks: List<TaskEntity>, dayWidth: Dp): List<PositionedTaskBlock> {
    // 1. Sort by start_time ascending, then duration descending
    val sorted = tasks.sortedWith(compareBy({ it.startTime }, { -(it.durationMinutes ?: 0) }))
    
    val clusters = mutableListOf<MutableList<TaskEntity>>()
    var currentCluster = mutableListOf<TaskEntity>()
    var clusterEnd = 0L

    for (task in sorted) {
        if (currentCluster.isEmpty() || task.startTime!! < clusterEnd) {
            currentCluster.add(task)
            clusterEnd = maxOf(clusterEnd, task.endTime ?: (task.startTime!! + 3600_000L))
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(task)
            clusterEnd = task.endTime ?: (task.startTime!! + 3600_000L)
        }
    }
    if (currentCluster.isNotEmpty()) clusters.add(currentCluster)

    // 2. Assign horizontal slots within each collision cluster
    return clusters.flatMap { cluster ->
        val columns = mutableListOf<MutableList<TaskEntity>>()
        for (task in cluster) {
            var placed = false
            for (col in columns) {
                if (col.last().endTime!! <= task.startTime!!) {
                    col.add(task)
                    placed = true
                    break
                }
            }
            if (!placed) columns.add(mutableListOf(task))
        }

        val totalCols = columns.size
        columns.flatMapIndexed { colIndex, colTasks ->
            colTasks.map { task ->
                PositionedTaskBlock(
                    task = task,
                    horizontalOffset = (dayWidth / totalCols) * colIndex,
                    blockWidth = dayWidth / totalCols
                )
            }
        }
    }
}
```

---

## 7. Room Database Queries for Calendar (`CalendarDao.kt`)

```kotlin
package com.flux.done.features.calendar.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.flux.done.features.tasks.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {

    // 1. Timed tasks in range (start_time and end_time NOT NULL)
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND start_time IS NOT NULL 
          AND end_time IS NOT NULL
          AND start_time < :rangeEndUtc 
          AND end_time > :rangeStartUtc
        ORDER BY start_time ASC
    """)
    fun observeTimedTasksInRange(rangeStartUtc: Long, rangeEndUtc: Long): Flow<List<TaskEntity>>

    // 2. Date-only tasks in range (due_date falls on this date, start_time IS NULL)
    @Query("""
        SELECT * FROM tasks 
        WHERE is_deleted = 0 
          AND start_time IS NULL 
          AND due_date >= :dayStartUtc 
          AND due_date < :dayEndUtc
        ORDER BY priority DESC, created_at ASC
    """)
    fun observeDateOnlyTasksForDay(dayStartUtc: Long, dayEndUtc: Long): Flow<List<TaskEntity>>

    // 3. Drag-to-reschedule atomic update
    @Query("""
        UPDATE tasks 
        SET start_time = :newStartTime, 
            end_time = :newEndTime, 
            due_date = :newDueDate, 
            updated_at = :updatedAt 
        WHERE id = :taskId
    """)
    suspend fun updateTaskSchedule(
        taskId: String,
        newStartTime: Long,
        newEndTime: Long,
        newDueDate: Long,
        updatedAt: Long
    )
}
```

---

## 8. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class CalendarTimelineEngineTest {

    private lateinit var db: FluxDoneDatabase
    private lateinit var calendarDao: CalendarDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxDoneDatabase::class.java).build()
        calendarDao = db.calendarDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun dragToReschedule_strictlyPreservesOriginalDuration() = runTest {
        // Given: 55-minute task from 10:00 AM to 10:55 AM (duration = 3300_000 ms)
        val initialStart = 1791810000000L // 10:00 AM
        val durationMs = 55 * 60 * 1000L
        val initialEnd = initialStart + durationMs // 10:55 AM
        
        val task = createSampleTask("task-1", "Physics", startTime = initialStart, endTime = initialEnd)
        db.taskDao().insertTask(task)

        // When: Dragged to 2:10 PM (14:10)
        val newStart = 1791825000000L
        val calculatedEnd = newStart + durationMs
        calendarDao.updateTaskSchedule("task-1", newStart, calculatedEnd, newStart, System.currentTimeMillis())

        // Then: New duration in Room is still exactly 55 minutes
        val updated = db.taskDao().getTaskById("task-1")
        assertNotNull(updated)
        assertEquals(newStart, updated!!.startTime)
        assertEquals(calculatedEnd, updated.endTime)
        assertEquals(durationMs, updated.endTime!! - updated.startTime!!)
    }

    @Test
    fun completedTask_retainsPositionWithCompletedStatus() = runTest {
        val task = createSampleTask("task-1", "Olympiad Math", isCompleted = false)
        db.taskDao().insertTask(task)

        db.taskDao().setTaskCompleted("task-1", true, System.currentTimeMillis())

        val timedTasks = calendarDao.observeTimedTasksInRange(0, Long.MAX_VALUE).first()
        assertEquals(1, timedTasks.size)
        assertTrue(timedTasks.first().isCompleted)
    }
}
```

---

## 9. Deliverables & Acceptance Checklist

- [ ] **1-Day & 3-Day Views:** Smooth horizontal paging and vertical scrolling across 24 hours.
- [ ] **Dynamic All-Day Strip:** Pinned date-only tasks dynamically size the strip (0 to 3+ chips) while preserving the total viewport height.
- [ ] **Timeline-Tapping Creation:** Tapping empty slot snaps to 15m, renders translucent ghost block with edge-drag duration control, and opens Quick Task Sheet.
- [ ] **Strict Duration Preservation on Drag:** Moving a task preserves its exact duration without snapping to 15m multiples.
- [ ] **Completed Tasks at 50% Opacity:** Struck-through title, checked checkbox, and anchored position preserved on the canvas.
- [ ] **Moving "Now" Line:** Red/Coral indicator line with circular dot auto-advances every 60 seconds.
- [ ] **Equal Split Overlap Math:** Side-by-side equal column layout for overlapping study blocks.
- [ ] **Zero-Mock Database Binding:** Live Room SQLite queries via `CalendarDao`.

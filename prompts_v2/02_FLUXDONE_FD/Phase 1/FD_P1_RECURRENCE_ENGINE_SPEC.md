# 🔁 Topic FD-P1-4: RRULE Recurrence & Schedule Engine Specification

**Document ID:** `FD_P1_RECURRENCE_ENGINE_SPEC`  
**Phase:** Phase 1 (Core Task, Hierarchy & Habit Foundation)  
**Target Package:** `com.flux.done.features.recurrence`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Room DAO + StateFlow + RFC 5545 RRULE Parser Engine)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari's study and contest preparation, tasks frequently repeat on strict recurring cadences:
* Daily revision blocks (e.g. *Daily Number Theory Problem*).
* Fixed coaching batches (e.g. *Physics Lecture every Mon, Wed, Fri at 4:00 PM*).
* Bi-weekly tests (e.g. *Full IOQM Mock Test every 2nd Saturday*).

**Topic FD-P1-4 establishes the RFC 5545 RRULE Recurrence Engine for FluxDone.**  
It enables flexible repeating patterns, seamless historical completion tracking (completing an instance keeps its historical log and auto-spawns the next instance), and TickTick-parity 3-choice scope dialogs when editing or deleting recurring tasks.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Task Management** | [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) | Tasks store `recurrence_rule` string and `recurrence_series_id`. Subtasks are cloned fresh for each spawned instance. |
| **Calendar View** | [`FD_P1_CALENDAR_TIMELINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_CALENDAR_TIMELINE_SPEC.md) | Timed recurring tasks project their start/end time blocks onto future calendar dates. |
| **IPC Bridge (Phase 2)** | [`FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_SETTINGS_AND_IPC_BRIDGE_SPEC.md) | In Phase 2, when a recurring timed instance becomes active, it coordinates focus sessions with FluxFoxus per [`IPC_SPEC_FD_FF_NATIVE.md`](file:///home/hp/Flux_Prompts/prompts_v2/01_INTEGRATION_SYNC/IPC_SPEC_FD_FF_NATIVE.md). |

---

## 3. Recurrence Patterns & RFC 5545 String Format

Recurrence rules are stored as standard RFC 5545 strings in `tasks.recurrence_rule`:

```
Pattern Type               RRULE String Representation
-------------------------------------------------------------------------
1. Daily                   RRULE:FREQ=DAILY;INTERVAL=1
2. Weekdays                RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
3. Specific Days of Week   RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR
4. Monthly (By Date)       RRULE:FREQ=MONTHLY;BYMONTHDAY=15
5. Monthly (By Day)        RRULE:FREQ=MONTHLY;BYDAY=2SA (2nd Saturday)
6. Yearly                  RRULE:FREQ=YEARLY;INTERVAL=1
7. Custom Interval         RRULE:FREQ=DAILY;INTERVAL=3 (Every 3 days)
                           RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=SU (Every 2 wks)
-------------------------------------------------------------------------
```

### 3.1 Recurrence Picker Dialog (`RecurrencePickerDialog.kt`)
Summoned from the Task Detail Sheet (SCR-02):

```
+-------------------------------------------------------+
|  ━  Repeat Schedule                                   |
+-------------------------------------------------------+
|  ( ) Does not repeat                                  |
|  ( ) Daily                                            |
|  ( ) Weekdays (Mon - Fri)                             |
|  (•) Weekly                                           |
|      Days: [M] [T] [W] [T] [F] [S] [S]                |
|            •MO     •WE     •FR                        |
|  ( ) Monthly                                          |
|  ( ) Yearly                                           |
|  ( ) Custom... (Every N days/weeks/months)            |
|                                                       |
|  End Condition:                                       |
|  [•] Forever   [ ] Until date   [ ] Count (N times)   |
|                                                       |
|  +-------------------------------------------------+  |
|  |                    [ Save ]                     |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

---

## 4. Lifecycle & Instance Generation Engine

### 4.1 Completing a Recurring Task (Option A — Preserving History)
When a recurring task instance is marked as **Completed** (`is_completed = true`):
1. **Historical Preservation:**  
   The current task record remains marked completed in SQLite (`is_completed = 1`, `completed_at = now`). It stays on today's list and on the Calendar timeline (rendered at 50% opacity with strike-through title).
2. **Immediate Generation of Next Instance:**  
   The `RecurrenceEngine` calculates the next scheduled date/time:
   $$\text{NextDueDate} = \text{calculateNextOccurrence}(\text{currentDueDate}, \text{rruleString})$$
3. **Cloning Invariants:**  
   The new task instance is committed to Room with:
   * Brand new UUID `id`.
   * Identical `recurrence_series_id` (links it to the recurring series).
   * Identical `title`, `notes`, `list_id`, `section_id`, `priority`, and `recurrence_rule`.
   * New `due_date = NextDueDate`.
   * If timed task: `start_time = NextStartTime`, `end_time = NextEndTime` (duration strictly preserved).
   * **Fresh Subtasks:** Deep copies all subtasks with new UUIDs and resets their status: `is_completed = false`.
   * Clean state: `is_completed = false`, `completed_at = null`, `wont_do = false`.

---

### 4.2 Skipping / "Won't Do" on a Recurring Task
If the user cannot complete today's task (e.g. holiday or contest reschedule):
1. The user selects **Won't Do / Skip** from the task menu.
2. The current task is updated: `wont_do = 1`, `completed_at = null`.
3. The engine **immediately generates the next upcoming instance** automatically.
4. **Zero Penalty:** The future recurrence chain continues completely uninterrupted.

---

### 4.3 Overdue Catch-Up Rule (Preventing Backlog Flooding)
* **Problem:** A user sets a daily recurring task, closes the app, and returns 5 days later. Without a catch-up guard, checking off the task would spawn 4 overdue backlog tasks consecutively.
* **The Rule:**  
  When an overdue recurring task is completed:
  $$\text{NextDate} = \text{calculateNextOccurrence}(\text{scheduledDate}, \text{rrule})$$
  $$\text{if } (\text{NextDate} < \text{todayStart}) \implies \text{NextDate} = \text{calculateNextFrom}(\text{todayStart}, \text{rrule})$$
  The engine always advances the next instance to the **first upcoming valid date starting from today**, preventing phantom backlog spam.

---

## 5. Scope Selection Dialogs (TickTick Parity)

When modifying or deleting an instance of an active recurring series, the app prompts for explicit user intent:

### 5.1 Edit Scope Dialog
When editing title, time, priority, or list on a recurring task:

```
+-------------------------------------------------------+
|  ━  Edit Recurring Task                               |
+-------------------------------------------------------+
|  This task repeats. Which tasks should be updated?    |
|                                                       |
|  [ 1. This task only ]                                |
|  Detaches this instance; leaves future tasks as is.   |
|                                                       |
|  [ 2. This and future tasks ]                         |
|  Updates this task and all upcoming instances.        |
|                                                       |
|  [ 3. All tasks in series ]                           |
|  Updates the entire template across all instances.    |
|                                                       |
|  +-------------------------------------------------+  |
|  |                   [ Cancel ]                    |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

* **Option 1 (`This task only`):**  
  Detaches this instance (`recurrence_rule = null`). Future scheduled instances continue adhering to the original series.
* **Option 2 (`This and future tasks`):**  
  Terminates recurrence on prior instances (`UNTIL = yesterday`). Generates a new `recurrence_series_id` starting from this instance with the updated attributes.
* **Option 3 (`All tasks in series`):**  
  Updates all non-completed tasks sharing the `recurrence_series_id`.

---

### 5.2 Delete Scope Dialog
When deleting a recurring task:

```
+-------------------------------------------------------+
|  ━  Delete Recurring Task                             |
+-------------------------------------------------------+
|  How would you like to delete this recurring task?    |
|                                                       |
|  [ 1. Delete this task only ]                         |
|  Moves this instance to Trash. Next instance spawns.  |
|                                                       |
|  [ 2. Delete this and future tasks ]                  |
|  Stops recurrence; preserves past completed history.  |
|                                                       |
|  [ 3. Delete all tasks in series ]                    |
|  Moves all instances (past and future) to Trash.      |
|                                                       |
|  +-------------------------------------------------+  |
|  |                   [ Cancel ]                    |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

* **Option 1 (`Delete this task only`):**  
  Moves the current instance to Trash (`is_deleted = 1`). Immediately spawns the next future instance so the recurrence series continues unbroken.
* **Option 2 (`Delete this and future tasks`):**  
  Soft-deletes current and any future instances to Trash. Keeps past completed tasks intact.
* **Option 3 (`Delete all tasks in series`):**  
  Soft-deletes every task sharing the `recurrence_series_id` to Trash.

---

## 6. Algorithm & Pure Kotlin Engine (`RecurrenceEngine.kt`)

```kotlin
package com.flux.done.features.recurrence.domain

import java.time.*
import java.time.temporal.TemporalAdjusters

object RecurrenceEngine {

    /**
     * Calculates the next occurrence epoch ms based on RFC 5545 RRULE string.
     */
    fun calculateNextOccurrence(
        currentEpochMs: Long,
        rruleString: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val currentDateTime = Instant.ofEpochMilli(currentEpochMs).atZone(zoneId)
        val params = parseRrule(rruleString)

        val freq = params["FREQ"] ?: "DAILY"
        val interval = params["INTERVAL"]?.toIntOrNull() ?: 1
        val byDay = params["BYDAY"]?.split(",") ?: emptyList()

        var nextDateTime = when (freq) {
            "DAILY" -> currentDateTime.plusDays(interval.toLong())
            
            "WEEKLY" -> {
                if (byDay.isNotEmpty()) {
                    findNextMatchingDayOfWeek(currentDateTime, byDay, interval)
                } else {
                    currentDateTime.plusWeeks(interval.toLong())
                }
            }
            
            "MONTHLY" -> {
                val byMonthDay = params["BYMONTHDAY"]?.toIntOrNull()
                if (byMonthDay != null) {
                    val candidateMonth = currentDateTime.plusMonths(interval.toLong())
                    val safeDay = minOf(byMonthDay, candidateMonth.toLocalDate().lengthOfMonth())
                    candidateMonth.withDayOfMonth(safeDay)
                } else {
                    currentDateTime.plusMonths(interval.toLong())
                }
            }
            
            "YEARLY" -> currentDateTime.plusYears(interval.toLong())
            else -> currentDateTime.plusDays(1)
        }

        return nextDateTime.toInstant().toEpochMilli()
    }

    private fun findNextMatchingDayOfWeek(
        current: ZonedDateTime,
        byDay: List<String>,
        interval: Int
    ): ZonedDateTime {
        val targetDays = byDay.map { parseDayOfWeek(it) }
        var candidate = current.plusDays(1)
        
        // Search up to 7 days ahead for next day in same week
        for (i in 1..7) {
            if (targetDays.contains(candidate.dayOfWeek)) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        
        // If wrapped, jump forward by interval weeks
        return current.plusWeeks(interval.toLong()).with(TemporalAdjusters.nextOrSame(targetDays.first()))
    }

    private fun parseDayOfWeek(dayStr: String): DayOfWeek = when (dayStr.takeLast(2)) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> DayOfWeek.MONDAY
    }

    private fun parseRrule(rrule: String): Map<String, String> {
        return rrule.removePrefix("RRULE:")
            .split(";")
            .mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
    }
}
```

---

## 7. Edge Cases & Defensive Engineering Guardrails

To prevent implementation ambiguity and ensure total algorithmic reliability:

### Edge Case 1: Month-End Clipping (31st Day Invariant)
* **Scenario:** A task is set to repeat monthly on the 31st. In February (28/29 days) or April (30 days), there is no 31st.
* **Invariant:** The engine uses safe month clipping (`minOf(targetDay, candidateMonth.lengthOfMonth())`). February 2026 evaluates to Feb 28; April evaluates to April 30. When May arrives, it resumes on May 31.

### Edge Case 2: Timezone & Daylight Saving Time (DST) Preservation
* **Scenario:** A study block is scheduled for 4:00 PM. A DST shift occurs between instances.
* **Invariant:** Calculation operates on `ZonedDateTime` anchored to `ZoneId.systemDefault()`. The wall-clock hour (`16:00`) remains strictly identical across time transitions.

### Edge Case 3: Subtask Cloned State Hygiene
* **Scenario:** The user checked 4 of 5 subtasks on today's recurring task before marking the parent complete.
* **Invariant:** When the next instance spawns, **all subtasks reset to `is_completed = false`**. Each cloned subtask receives a brand-new unique UUID, preventing foreign key conflicts with the completed historical instance.

---

## 8. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class RecurrenceEngineUnitTest {

    @Test
    fun weeklySpecificDays_calculatesCorrectNextOccurrence() {
        // Given: Tuesday Oct 13, 2026. Recurrence: Mon, Wed, Fri (MO,WE,FR)
        val zoneId = ZoneId.of("UTC")
        val tuesdayEpoch = LocalDate.of(2026, 10, 13).atTime(16, 0).atZone(zoneId).toInstant().toEpochMilli()
        val rrule = "RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR"

        // When: Next occurrence calculated
        val nextEpoch = RecurrenceEngine.calculateNextOccurrence(tuesdayEpoch, rrule, zoneId)
        val nextDate = Instant.ofEpochMilli(nextEpoch).atZone(zoneId)

        // Then: Should be Wednesday Oct 14, 2026 at 16:00
        assertEquals(DayOfWeek.WEDNESDAY, nextDate.dayOfWeek)
        assertEquals(14, nextDate.dayOfMonth)
        assertEquals(16, nextDate.hour)
    }

    @Test
    fun monthEndClipping_handlesFebruaryProperly() {
        val zoneId = ZoneId.of("UTC")
        val jan31Epoch = LocalDate.of(2026, 1, 31).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val rrule = "RRULE:FREQ=MONTHLY;BYMONTHDAY=31;INTERVAL=1"

        val nextEpoch = RecurrenceEngine.calculateNextOccurrence(jan31Epoch, rrule, zoneId)
        val nextDate = Instant.ofEpochMilli(nextEpoch).atZone(zoneId)

        // 2026 is non-leap year -> Feb has 28 days
        assertEquals(Month.FEBRUARY, nextDate.month)
        assertEquals(28, nextDate.dayOfMonth)
    }
}
```

---

## 9. Deliverables & Acceptance Checklist

- [ ] **RFC 5545 RRULE Parser & Generator:** Full support for Daily, Weekdays, Weekly on specific days, Monthly by date/day, Yearly, and custom $N$-interval patterns.
- [ ] **History-Preserving Completion (Option A):** Completed instance remains in SQLite history; next instance immediately spawns with fresh uncompleted subtasks.
- [ ] **Skip / Won't Do Support:** Skipping today's task leaves future recurrence schedule completely intact.
- [ ] **Overdue Catch-Up Protection:** Completing long-overdue tasks jumps to the upcoming date, preventing backlog flooding.
- [ ] **Edit Scope Dialog:** 3 choices (`This task only`, `This and future`, `All tasks in series`) with detachment and series splitting logic.
- [ ] **Delete Scope Dialog:** 3 choices (`Delete this task only`, `Delete this and future`, `Delete all in series`).
- [ ] **Safe Month-End & DST Math:** Safe day clipping for short months and wall-clock time preservation across time transitions.
- [ ] **Zero-Mock Pure Kotlin Engine:** Fully deterministic, test-verified recurrence logic.

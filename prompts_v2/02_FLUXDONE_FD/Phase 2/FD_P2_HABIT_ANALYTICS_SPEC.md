# 📊 Topic FD-P2-3: Habit Matrix Analytics & Consistency Heatmaps Specification

**Document ID:** `FD_P2_HABIT_ANALYTICS_SPEC`  
**Phase:** Phase 2 (Advanced Ergonomics, Settings & Inter-App IPC)  
**Target Package:** `com.flux.done.features.habits.analytics`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose Custom Canvas / Matrix Grid + Room SQLite DAOs + StateFlow)  
**Screen References:** SCR-06 (Habits Grid & Analytics View), SCR-07 (Habit Detail Analytics)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari’s intensive academic routine, tracking daily habits is only half the equation; sustaining long-term momentum requires clear visual reflection on discipline and consistency over time.

**Topic FD-P2-3 establishes the Habit Matrix Analytics & Consistency Heatmap Engine for FluxDone.**  
It provides both high-level cross-habit visibility and granular single-habit insights:
* **Calendar Month Horizon (Option 1.B):** The cross-habit heatmap renders a unified calendar month view (1st to last day of the month) with intuitive `<` `>` month switchers.
* **Natural Lifecycle Inclusion:** Because analytics are scoped by Calendar Month, any habit active during that month naturally displays its rows. When viewing past months, historical habits appear in their true chronological context without artificial "archived" toggles or clutter.
* **Proportional Color Intensity:** Fully achieved goals render in 100% solid habit color; partial numeric progress renders at proportional opacity (e.g. 2/4 problems = 50% alpha); unfulfilled scheduled days render in muted surface slate.
* **Rest Day Representation ("Border with Dot"):** For specific-days habits, non-scheduled days render with a **subtle border and a small centered dot (`·`)**, visually and mathematically distinguishing valid rest days from missed days.
* **Fair Completion Rate Engine:** Rest days are excluded from the denominator when calculating monthly consistency rates ($\text{Rate} = \frac{\text{Days Met}}{\text{Scheduled Days}} \times 100\%$).
* **Per-Habit Deep Dive:** Dedicated metrics cockpit comparing Current Streak vs All-Time Best Streak, Weekly Distribution Bar Charts (Mon–Sun), and single-habit calendar heatmaps.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Habit Tracker Baseline** | [`FD_P1_HABIT_TRACKER_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_HABIT_TRACKER_SPEC.md) | Ingests `habits` (`target_count`, `increment_count`, `scheduled_days_mask`, `color_hex`) and `habit_completions` records. |
| **Design Tokens** | [`FLUX_DESIGN_TOKENS.md`](file:///home/hp/Flux_Prompts/prompts_v2/00_CORE/FLUX_DESIGN_TOKENS.md) | Maps habit colors, background surface slate (`#1B263B`), and alpha blending tiers. |
| **Backup & Export** | `FD_P2_BACKUP_EXPORT_SPEC.md` | Historical `habit_completions` rows export losslessly to JSON and restore into analytics without loss. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Cross-Habit Matrix Heatmap (`HabitMatrixScreen.kt` — SCR-06)

Rendered either as an expandable section in the Habits tab or via a dedicated "Analytics" view toggle:

```
+-----------------------------------------------------------------+
|  HABIT CONSISTENCY MATRIX              [ < ] OCTOBER 2026 [ > ] |
+-----------------------------------------------------------------+
|  HABIT               1  2  3  4  5  6  7  8  9 10 11 12 13 14 ...|
|  -------------------------------------------------------------- |
|  [💡] IOQM 12 Probs  ■  ■  ■  ■  ■  ■  ■  ■  ■  ■  ◩  ■  ■  ·  |
|  [📚] Physics Rev    ■  ■  ·  ■  ·  ■  ·  ■  ■  ·  ■  ·  ■  ·  |
|  [⚡] Flashcards     ■  ■  ■  ■  □  ■  ■  ■  ■  ■  ■  ■  ■  □  |
+-----------------------------------------------------------------+
|  CELL LEGEND:                                                   |
|  ■ 100% Target Met    ◩ Partial (Proportional)    □ Missed Day  |
|  · Valid Rest Day (Border with Dot)              ◌ Future Day  |
+-----------------------------------------------------------------+
```

#### Visual Cell Invariants:
1. **100% Target Met (`■`):**
   * Solid habit swatch color (`color_hex`) at 100% opacity.
   * Subtle rounded corners (`4dp`).
2. **Partial Progress (`◩`):**
   * Rendered in the habit's color with proportional alpha:
     $$\alpha = \max\left(0.25f, \frac{\text{completed\_count}}{\text{target\_count}}\right)$$
   * Example: 6 / 12 problems = 50% opacity.
3. **Missed Day (`□`):**
   * Scheduled day where `completed_count == 0` and date is in the past.
   * Rendered in dark muted surface slate (`#1B263B` / `surfaceVariant`) with no accent color fill.
4. **Rest Day (`·` — Border with Dot):**
   * Scheduled off per `scheduled_days_mask` (e.g., Sunday on a Mon–Sat habit).
   * Rendered with a subtle 1dp border and a centered dot (`·`) in muted sage (`#778D7A`).
   * Clearly informs Ari that this day did **not** break his streak.
5. **Future Days (`◌`):**
   * Days after `LocalDate.now()` in the current month render as hollow, low-contrast ghost outlines.

#### Interactive Cell Tooltip Sheet:
Tapping any cell displays a lightweight modal sheet or floating tooltip:
```
+-------------------------------------------------------+
|  [💡] Solve 12 Hard IOQM Problems                     |
|  Tuesday, October 13, 2026                            |
|                                                       |
|  Progress: 12 / 12 (Target Met)                       |
|  Status: Streak Maintained 🔥 18 Days                 |
|  Completed at: 18:45 PM                               |
+-------------------------------------------------------+
```

---

### 3.2 Single-Habit Deep Dive Sheet (`HabitAnalyticsSheet.kt` — SCR-07)

Tapping a habit title row in the matrix opens the single-habit analytics cockpit:

```
+-----------------------------------------------------------------+
|  ←  HABIT ANALYTICS: IOQM 12 Problems                   [ ⚙️ ]  |
+-----------------------------------------------------------------+
|  ┌──────────────────────────────┬─────────────────────────────┐ |
|  │  CURRENT STREAK              │  ALL-TIME BEST STREAK       │ |
|  │  🔥 18 Days                  │  🏆 42 Days                 │ |
|  ├──────────────────────────────┼─────────────────────────────┤ |
|  │  MONTHLY CONSISTENCY         │  TOTAL COMPLETIONS          │ |
|  │  📈 92% (23/25 days)         │  🎯 142 Times               │ |
|  └──────────────────────────────┴─────────────────────────────┘ |
|                                                                 |
|  WEEKLY DISTRIBUTION (Mon - Sun)                                |
|  100% │  █    █    █    █    █    █    ·                        |
|   50% │  █    █    █    █    █    █    ·                        |
|       └───────────────────────────────────                      |
|         Mon  Tue  Wed  Thu  Fri  Sat  Sun(Rest)                 |
|                                                                 |
|  MONTHLY CALENDAR GRID (October 2026)                           |
|   M   T   W   T   F   S   S                                     |
|  [●] [●] [●] [●] [●] [●] [·]   <-- Connected streak glow        |
|  [●] [●] [●] [●] [◐] [●] [·]                                    |
|  [●] [●] [●] [ ] [ ] [ ] [ ]                                    |
+-----------------------------------------------------------------+
```

#### Key Single-Habit Metrics:
* **Monthly Consistency Rate Formula:**
  $$\text{Consistency Rate} = \frac{\text{Count of Days Met in Month}}{\text{Total Scheduled Days in Month (excluding Rest Days)}} \times 100\%$$
* **Weekly Distribution Bar Chart:** Vertical bars indicating total completions achieved on each day of the week, exposing study consistency patterns.
* **Connected Streak Glow:** Days that form part of an unbroken streak render connected horizontal pill bridges behind the date nodes.

---

## 4. Technical Architecture & Data Calculations

### 4.1 Room SQLite DAO Queries (`HabitAnalyticsDao.kt`)

```kotlin
package com.flux.done.core.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class HabitWithCompletions(
    val habit: HabitEntity,
    val completions: List<HabitCompletionEntity>
)

@Dao
interface HabitAnalyticsDao {

    // Fetch active habits and their completions for a given calendar month ('YYYY-MM-01' to 'YYYY-MM-31')
    @Query("""
        SELECT * FROM habit_completions 
        WHERE completion_date BETWEEN :startOfMonth AND :endOfMonth
        ORDER BY completion_date ASC
    """)
    fun observeMonthCompletions(startOfMonth: String, endOfMonth: String): Flow<List<HabitCompletionEntity>>

    // Fetch all habits that were either created on or before the month end, and not deleted
    @Query("""
        SELECT * FROM habits 
        WHERE is_deleted = 0 
          AND created_at <= :monthEndEpochMs
        ORDER BY created_at ASC
    """)
    fun observeHabitsForMonth(monthEndEpochMs: Long): Flow<List<HabitEntity>>

    // All-time completions for a specific habit
    @Query("""
        SELECT * FROM habit_completions 
        WHERE habit_id = :habitId 
        ORDER BY completion_date ASC
    """)
    fun observeAllCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>
}
```

---

### 4.2 Matrix Calculation Engine (`HabitMatrixCalculator.kt`)

```kotlin
package com.flux.done.features.habits.analytics

import com.flux.done.core.database.HabitCompletionEntity
import com.flux.done.core.database.HabitEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class CellStatus {
    TARGET_MET,
    PARTIAL,
    MISSED,
    REST_DAY,
    FUTURE
}

data class MatrixCell(
    val date: LocalDate,
    val status: CellStatus,
    val progressRatio: Float,
    val completedCount: Int,
    val targetCount: Int
)

data class HabitMatrixRow(
    val habit: HabitEntity,
    val cells: List<MatrixCell>,
    val monthlyCompletionRate: Float,
    val daysCompleted: Int,
    val totalScheduledDays: Int
)

object HabitMatrixCalculator {

    fun buildMatrixRows(
        yearMonth: YearMonth,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): List<HabitMatrixRow> {
        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now()
        val completionsByHabitAndDate = completions.associateBy { Pair(it.habit_id, it.completion_date) }

        return habits.map { habit ->
            val cells = mutableListOf<MatrixCell>()
            var daysMet = 0
            var scheduledDaysCount = 0

            for (day in 1..daysInMonth) {
                val date = yearMonth.atDay(day)
                val dateStr = date.toString()
                val isScheduled = isHabitScheduledOnDay(habit.scheduled_days_mask, date.dayOfWeek)
                val completion = completionsByHabitAndDate[Pair(habit.id, dateStr)]
                val completedCount = completion?.completed_count ?: 0

                val cell = when {
                    date.isAfter(today) -> MatrixCell(date, CellStatus.FUTURE, 0f, 0, habit.target_count)
                    !isScheduled -> MatrixCell(date, CellStatus.REST_DAY, 0f, 0, habit.target_count)
                    else -> {
                        scheduledDaysCount++
                        val ratio = (completedCount.toFloat() / habit.target_count).coerceIn(0f, 1f)
                        if (ratio >= 1.0f) {
                            daysMet++
                            MatrixCell(date, CellStatus.TARGET_MET, 1f, completedCount, habit.target_count)
                        } else if (ratio > 0f) {
                            MatrixCell(date, CellStatus.PARTIAL, ratio, completedCount, habit.target_count)
                        } else {
                            MatrixCell(date, CellStatus.MISSED, 0f, 0, habit.target_count)
                        }
                    }
                }
                cells.add(cell)
            }

            val rate = if (scheduledDaysCount > 0) (daysMet.toFloat() / scheduledDaysCount) * 100f else 0f
            HabitMatrixRow(
                habit = habit,
                cells = cells,
                monthlyCompletionRate = rate,
                daysCompleted = daysMet,
                totalScheduledDays = scheduledDaysCount
            )
        }
    }

    private fun isHabitScheduledOnDay(mask: Int, dayOfWeek: DayOfWeek): Boolean {
        val bit = when (dayOfWeek) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 8
            DayOfWeek.FRIDAY -> 16
            DayOfWeek.SATURDAY -> 32
            DayOfWeek.SUNDAY -> 64
        }
        return (mask and bit) != 0
    }
}
```

---

## 5. MVI Architecture & State Management

### 5.1 Habit Analytics MVI State (`HabitAnalyticsViewModel.kt`)

```kotlin
package com.flux.done.features.habits.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flux.done.core.database.HabitEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

data class HabitAnalyticsUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val matrixRows: List<HabitMatrixRow> = emptyList(),
    val selectedCellInfo: MatrixCellTooltipInfo? = null,
    val selectedHabitDetail: HabitDetailAnalytics? = null,
    val isLoading: Boolean = false
)

data class MatrixCellTooltipInfo(
    val habitTitle: String,
    val dateString: String,
    val status: CellStatus,
    val completedCount: Int,
    val targetCount: Int
)

data class HabitDetailAnalytics(
    val habit: HabitEntity,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalAllTimeCompletions: Int,
    val monthlyConsistencyRate: Float,
    val weeklyDistribution: Map<java.time.DayOfWeek, Int>
)

sealed interface HabitAnalyticsIntent {
    object NextMonth : HabitAnalyticsIntent
    object PreviousMonth : HabitAnalyticsIntent
    data class JumpToMonth(val yearMonth: YearMonth) : HabitAnalyticsIntent
    data class SelectCell(val habit: HabitEntity, val cell: MatrixCell) : HabitAnalyticsIntent
    object DismissTooltip : HabitAnalyticsIntent
    data class OpenHabitDetail(val habitId: String) : HabitAnalyticsIntent
    object CloseHabitDetail : HabitAnalyticsIntent
}
```

---

## 6. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **Varying Month Lengths (28, 29, 30, 31 days)** | The matrix grid dynamically sizes its column count using `yearMonth.lengthOfMonth()`. February renders 28 columns (or 29 in leap years); zero trailing phantom columns are drawn. |
| **Rest Days Never Penalize Consistency** | In `HabitMatrixCalculator`, rest days increment neither `daysMet` nor `scheduledDaysCount`. A student completing 6 out of 6 scheduled days (with Sunday resting) earns 100% consistency. |
| **Natural Lifecycle Scoping for Archived Habits** | When navigating backwards in time (e.g. to August 2026), habits created before that month are displayed. When navigating to the present, habits archived previously simply do not populate rows if no completions exist for that month. |
| **Numeric Target Overshoot Protection** | If database logs `completed_count > target_count` (prevented by Phase 1 hard ceiling), the matrix clamps `ratio` strictly to `1.0f`. |
| **Cross-Month Streak Preservation** | When computing streaks across month boundaries, streaks are calculated continuously from the all-time sequence, ensuring streak counters never artificially reset to 0 on the 1st of a new month. |

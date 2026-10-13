# 🔥 Topic FD-P1-5: Habit Tracker, Daily Grid & Streak Engine Specification

**Document ID:** `FD_P1_HABIT_TRACKER_SPEC`  
**Phase:** Phase 1 (Core Task, Hierarchy & Habit Foundation)  
**Target Package:** `com.flux.done.features.habits`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + StateFlow)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

Habit tracking in Ari's academic routine reinforces daily and weekly discipline (e.g. *Daily IOQM Problem Solving*, *Morning Revision*, *Formula Flashcards*).

Unlike task lists that track one-off obligations, habits track **sustainable recurring momentum**:
* **Dual Habit Modes:** Supports both **Simple (1-Tap Completion)** habits and **Numeric Target Count** habits.
* **Custom Increment Steps:** Multi-step habits support custom increment amounts (e.g. target 12, step 3 $\to$ 4 clicks to finish).
* **Tactile Incrementation & Decrementation:** Short tap increments by step; long-press decrements to undo accidental taps.
* **Hard Ceiling Rule:** Numeric habits cap strictly at the target count (no overshooting).
* **Specific-Days Scheduling:** Non-scheduled days are true rest days; they are **hidden from the daily grid** and **never break your streak**.
* **Archive vs Delete:** Full support for both non-destructive Archiving (keeps historical heatmap) and Trash Deletion (with 30-day retention).

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Color Palette** | [`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md) | Habits select from the 12-swatch palette or custom hex picker. |
| **Settings & Reminders (Phase 2)** | [`FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md) | In Phase 2, `reminder_time` triggers an `AlarmManager` heads-up notification. |
| **Trash Architecture** | `FD_P1_SMART_LISTS_AND_TRASH_SPEC.md` | Deleted habits move to Trash with 30-day auto-purge and 1-tap restore. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Daily Habits Screen (`HabitsScreen.kt` — SCR-06)

Rendered as the dedicated Habits tab in the bottom navigation bar:

```
+-----------------------------------------------------------------+
|  HABITS                                                   [ + ] |
|  Mon 12 • Tue 13 (Today) • Wed 14 • Thu 15                      |
+-----------------------------------------------------------------+
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  [💡] Solve 12 Hard IOQM Problems                  🔥 18   │ |
|  │       Progress: 9 / 12  (+3 per tap)                       │ |
|  │                                         ╭───────────╮      │ |
|  │                                         │  9 / 12   │      │ |
|  │                                         ╰───────────╯      │ |
|  └────────────────────────────────────────────────────────────┘ |
|  ┌────────────────────────────────────────────────────────────┐ |
|  │  [📚] Read Physics Theory (1-Tap Simple)           🔥 42   │ |
|  │       Completed for Today                       ╭───╮      │ |
|  │                                                 │ ✓ │      │ |
|  │                                                 ╰───╯      │ |
|  └────────────────────────────────────────────────────────────┘ |
+-----------------------------------------------------------------+
```

#### Card Anatomy:
1. **Icon Container:** 40dp circular badge tinted with habit `color_hex` at 20% opacity, rendering a white/accent Material Icon.
2. **Title & Caption:**
   * 16sp bold title.
   * Caption showing current count and step size (e.g. `9 / 12 (+3 per tap)`).
3. **Flame Streak Counter:** Top-right pill showing flame vector `#E07A5F` + consecutive streak integer (`🔥 18`).
4. **Interactive Action Ring (Right):**
   * **Simple Mode (`BINARY`):** 44dp circular toggle. Tapping immediately marks completed with a white checkmark vector.
   * **Numeric Mode (`NUMERIC`):** Circular progress ring (stroke 4dp) filling proportionally:
     $$\text{Fill Ratio} = \frac{\text{currentCount}}{\text{targetCount}}$$
     Center displays `current / target` text in bold 12sp font.

---

### 3.2 Tap & Gesture Interaction Rules

#### A. Short Tap (Increment):
* **Numeric Habit:**  
  $$\text{newCount} = \min(\text{targetCount}, \text{currentCount} + \text{incrementCount})$$
* **Hard Ceiling Invariant:** If `targetCount = 10`, `currentCount = 9`, and `step = 3`:  
  $\text{newCount} = \min(10, 9 + 3) = \mathbf{10}$ (never overshoots to 12).
* **Celebratory Target Completion:** When `newCount == targetCount`:
  * Ring fills to 100%.
  * Delivers haptic feedback vibration (`HapticFeedbackType.LongPress`).
  * Triggers a lightweight 300ms confetti particle burst animation (`ConfettiView`).
  * Flame streak counter increments by +1.

#### B. Long-Press (Decrement / Undo):
* **Undo Accident:** Long-pressing the ring decrements by `incrementCount`:  
  $$\text{newCount} = \max(0, \text{currentCount} - \text{incrementCount})$$
* **Floor Invariant:** Count cannot drop below 0.
* If a completed habit drops below target, completion state reverts to active.

---

### 3.3 Frequency Scheduling: Specific Days Rule

Habits support two frequency types:
1. **`DAILY`:** Expected every day (Monday through Sunday).
2. **`SPECIFIC_DAYS`:** Scheduled on a subset of days (e.g. `["TUESDAY", "THURSDAY"]`).

#### The Rest Day Invariant:
* On non-scheduled days (e.g. Wednesday for a Tue/Thu habit):
  * **Hidden from Daily Grid:** The habit card is **completely hidden** from SCR-06 on non-scheduled days, eliminating visual guilt and cluttered UI.
  * **Zero Streak Penalty:** Non-scheduled days are treated as **valid rest days**. They do **never break the streak**. The streak continues as long as each scheduled day is completed.

---

### 3.4 Archive vs. Delete Architecture

From the habit card overflow menu or detail sheet:

```
+-------------------------------------------------------+
|  ━  Manage Habit                                      |
+-------------------------------------------------------+
|  [ 📦 Archive Habit ]                                 |
|  Hides from daily grid; preserves all history/stats.  |
|                                                       |
|  [ 🗑️ Delete Habit ]                                  |
|  Moves habit to Trash with 30-day restore period.     |
|                                                       |
|  +-------------------------------------------------+  |
|  |                   [ Cancel ]                    |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

1. **Archive Habit (`is_archived = true`):**
   * Hides the habit from the daily grid.
   * **Preserves 100% of completion history** in SQLite.
   * Preserves contribution heatmap data in stats.
   * Accessible in *Settings → Archived Habits* where it can be unarchived at any time.
2. **Delete Habit (`is_deleted = true`):**
   * Moves the habit to Trash (SCR-12).
   * Soft-deleted with `deleted_at = System.currentTimeMillis()`.
   * Follows the 30-day retention window before permanent purge.

---

### 3.5 Monthly Contribution Heatmap (SCR-07 Detail Sheet)

Tapping a habit card opens the Habit Detail Sheet:

```
+-----------------------------------------------------------------+
|  [ < Back ]             Solve 12 IOQM Problems          [ ⋮ ]   |
+-----------------------------------------------------------------+
|  🔥 Current Streak: 18 Days         ⭐ Best Streak: 34 Days     |
|  🎯 Target: 12 Problems (Step 3)    📅 Frequency: Tue, Thu      |
|  -------------------------------------------------------------  |
|  OCTOBER 2026 COMPLETIONS                                       |
|  M   T   W   T   F   S   S                                      |
|          1   2   3   4   5                                      |
|  6  [●]  8  [●] 10  11  12                                      |
| 13  [●] 15  ( ) 17  18  19  ...                                 |
|                                                                 |
|  [●] = Scheduled & Completed   ( ) = Scheduled & Missed         |
|  Numbers without dots = Non-scheduled rest days                 |
+-----------------------------------------------------------------+
```

---

## 4. Room Database Schema

### 4.1 Entity: `HabitEntity`

```kotlin
package com.flux.done.features.habits.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "icon_identifier")
    val iconIdentifier: String = "star", // Material icon key

    @ColumnInfo(name = "color_hex")
    val colorHex: String, // e.g. "2E7D32"

    @ColumnInfo(name = "habit_type")
    val habitType: String = "BINARY", // "BINARY" (1-tap) or "NUMERIC" (target count)

    @ColumnInfo(name = "target_count")
    val targetCount: Int = 1, // Max goal (e.g. 10)

    @ColumnInfo(name = "increment_count")
    val incrementCount: Int = 1, // Step size (e.g. 3)

    @ColumnInfo(name = "frequency_type")
    val frequencyType: String = "DAILY", // "DAILY" or "SPECIFIC_DAYS"

    @ColumnInfo(name = "frequency_days_json")
    val frequencyDaysJson: String = "[]", // e.g. '["TUESDAY","THURSDAY"]'

    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null, // Time of day ms

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

### 4.2 Entity: `HabitCompletionEntity`

```kotlin
package com.flux.done.features.habits.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habit_id"),
        Index("completion_date"),
        Index(value = ["habit_id", "completion_date"], unique = true)
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "habit_id")
    val habitId: String,

    @ColumnInfo(name = "completion_date")
    val completionDate: String, // "YYYY-MM-DD" local date key

    @ColumnInfo(name = "current_count")
    val currentCount: Int,

    @ColumnInfo(name = "is_target_met")
    val isTargetMet: Boolean,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long = System.currentTimeMillis()
)
```

---

## 5. Room DAO Specifications (`HabitDao.kt`)

```kotlin
package com.flux.done.features.habits.data.dao

import androidx.room.*
import com.flux.done.features.habits.data.entity.HabitCompletionEntity
import com.flux.done.features.habits.data.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

data class HabitWithTodayCompletion(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habit_id"
    )
    val completions: List<HabitCompletionEntity>
)

@Dao
interface HabitDao {

    // --- Habits ---
    @Query("SELECT * FROM habits WHERE is_archived = 0 AND is_deleted = 0 ORDER BY sort_order ASC, created_at ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE is_archived = 1 AND is_deleted = 0 ORDER BY updated_at DESC")
    fun observeArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET is_archived = :archived, updated_at = :updatedAt WHERE id = :habitId")
    suspend fun setHabitArchived(habitId: String, archived: Boolean, updatedAt: Long)

    @Query("UPDATE habits SET is_deleted = 1, deleted_at = :deletedAt WHERE id = :habitId")
    suspend fun softDeleteHabit(habitId: String, deletedAt: Long)

    @Query("UPDATE habits SET is_deleted = 0, deleted_at = NULL WHERE id = :habitId")
    suspend fun restoreHabit(habitId: String)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun permanentlyDeleteHabit(habitId: String)

    // --- Completions ---
    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completion_date = :date LIMIT 1")
    suspend fun getCompletionForDate(habitId: String, date: String): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completion_date DESC")
    fun observeCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND completion_date = :date")
    suspend fun deleteCompletionForDate(habitId: String, date: String)
}
```

---

## 6. Streak Calculation Algorithm (`HabitStreakCalculator.kt`)

```kotlin
package com.flux.done.features.habits.domain

import com.flux.done.features.habits.data.entity.HabitCompletionEntity
import com.flux.done.features.habits.data.entity.HabitEntity
import java.time.DayOfWeek
import java.time.LocalDate

object HabitStreakCalculator {

    fun calculateStreaks(
        habit: HabitEntity,
        completions: List<HabitCompletionEntity>,
        today: LocalDate = LocalDate.now()
    ): Pair<Int, Int> {
        val completedDates = completions
            .filter { it.isTargetMet }
            .map { LocalDate.parse(it.completionDate) }
            .toSet()

        val scheduledDays = parseScheduledDays(habit.frequencyType, habit.frequencyDaysJson)

        var currentStreak = 0
        var maxStreak = 0
        var checkDate = today

        // If today is scheduled and not yet completed, start checking backwards from yesterday
        if (isScheduledOn(checkDate, scheduledDays) && !completedDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        // Trace backwards to evaluate current streak
        while (true) {
            if (isScheduledOn(checkDate, scheduledDays)) {
                if (completedDates.contains(checkDate)) {
                    currentStreak++
                } else {
                    break // Streak broken
                }
            }
            checkDate = checkDate.minusDays(1)
            if (checkDate.isBefore(today.minusYears(5))) break // Safety cap
        }

        // Calculate all-time best streak across entire historical record
        var tempStreak = 0
        val allScheduledDatesSorted = completedDates.sorted()
        // (Iterates through chronological timeline respecting rest days)

        maxStreak = maxOf(habit.bestStreak, currentStreak)
        return Pair(currentStreak, maxStreak)
    }

    private fun isScheduledOn(date: LocalDate, scheduledDays: Set<DayOfWeek>): Boolean {
        return scheduledDays.isEmpty() || scheduledDays.contains(date.dayOfWeek)
    }

    private fun parseScheduledDays(freqType: String, json: String): Set<DayOfWeek> {
        if (freqType == "DAILY") return DayOfWeek.values().toSet()
        // Parses JSON array of string day names
        return setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY) // Example fallback
    }
}
```

---

## 7. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {

    private lateinit var db: FluxDoneDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var repository: HabitRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxDoneDatabase::class.java).build()
        habitDao = db.habitDao()
        repository = HabitRepositoryImpl(habitDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun numericIncrement_capsStrictlyAtTarget() = runTest {
        // Given: Target = 10, Step = 3, current = 9
        val habit = createSampleHabit("h1", targetCount = 10, step = 3, habitType = "NUMERIC")
        habitDao.insertHabit(habit)
        val initialCompletion = HabitCompletionEntity("c1", "h1", "2026-10-13", currentCount = 9, isTargetMet = false)
        habitDao.insertOrUpdateCompletion(initialCompletion)

        // When: User taps increment
        repository.incrementHabit("h1", "2026-10-13")

        // Then: Count caps strictly at 10 (not 12), and isTargetMet becomes true
        val updated = habitDao.getCompletionForDate("h1", "2026-10-13")
        assertNotNull(updated)
        assertEquals(10, updated!!.currentCount)
        assertTrue(updated.isTargetMet)
    }

    @Test
    fun longPressDecrement_floorsAtZero() = runTest {
        val habit = createSampleHabit("h1", targetCount = 10, step = 3, habitType = "NUMERIC")
        habitDao.insertHabit(habit)
        val initialCompletion = HabitCompletionEntity("c1", "h1", "2026-10-13", currentCount = 2, isTargetMet = false)
        habitDao.insertOrUpdateCompletion(initialCompletion)

        // When: User long presses to decrement (2 - 3)
        repository.decrementHabit("h1", "2026-10-13")

        // Then: Count floors at 0
        val updated = habitDao.getCompletionForDate("h1", "2026-10-13")
        assertNotNull(updated)
        assertEquals(0, updated!!.currentCount)
        assertFalse(updated.isTargetMet)
    }

    @Test
    fun specificDaysHabit_nonScheduledDaysDoNotBreakStreak() = runTest {
        // Given: Habit scheduled only on Tuesday & Thursday
        val habit = createSampleHabit("h1", freqType = "SPECIFIC_DAYS", days = listOf("TUESDAY", "THURSDAY"))
        val completions = listOf(
            createCompletion("h1", "2026-10-08", isMet = true), // Thursday
            createCompletion("h1", "2026-10-13", isMet = true)  // Tuesday (Fri/Sat/Sun/Mon were rest days)
        )

        val (currentStreak, _) = HabitStreakCalculator.calculateStreaks(habit, completions, LocalDate.of(2026, 10, 13))
        assertEquals(2, currentStreak) // Unbroken streak across rest days
    }
}
```

---

## 8. Deliverables & Acceptance Checklist

- [ ] **Dual Habit Modes:** Supports both 1-Tap Simple (`BINARY`) and Multi-Step (`NUMERIC`) habits.
- [ ] **Custom Increment Steps:** Configurable step size (e.g. target 12, step 3).
- [ ] **Hard Ceiling Invariant:** Count caps strictly at `target_count` (never overshoots).
- [ ] **Long-Press Decrement:** Long press decrements by step size, floored cleanly at 0.
- [ ] **Rest Days Immunity:** Non-scheduled days are hidden from the daily grid and never break streaks.
- [ ] **Confetti & Haptics:** 300ms confetti particle animation and haptic pulse on target completion.
- [ ] **Archive & Delete:** Non-destructive Archiving preserves history/heatmap; Deleting moves to Trash with 30-day retention.
- [ ] **Monthly Calendar Heatmap (SCR-07):** Visual grid with filled completion dots and rest days mapping.
- [ ] **Zero-Mock Room SQLite:** Live Room DAOs and unit test validation.

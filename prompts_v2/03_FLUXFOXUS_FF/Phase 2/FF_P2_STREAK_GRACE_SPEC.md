# 🛡️ Topic FF-P2-4: Stacking Streak Grace & Historical Calendar Specification

**Document ID:** `FF_P2_STREAK_GRACE_SPEC`  
**Phase:** Phase 2 (Hardened Enforcement & Advanced Scheduling)  
**Target Package:** `com.flux.focus.features.streaks`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + WorkManager Midnight Engine)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Long-term habit consistency faces the **catastrophic demoralization cliff**: a user with a 90-day streak who suffers an unavoidable illness, emergency, or travel day without phone access loses their streak overnight. Resetting to 0 frequently causes complete abandonment of the app.

Phase 1 established the Daily Streak Engine with Sunday Relief Days. **Phase 2 introduces Stacking Grace Shields & The Historical Streak Calendar (`/streaks/history`)**, transforming streak protection into an earned, stackable buffer that shields an entire 24-hour day without hiding missed days.

### 1.1 The Stacking & Whole-Day Shield Invariants
1. **Earned Stacking Shields:** Users earn **+1 Grace Shield on the 1st of every calendar month**. Unused shields **accumulate and roll over** across months (e.g. 3 consistent months = 3 banked shields).
2. **Whole-Day Immunity:** Consuming a shield protects the **entire 24-hour calendar day**. Once a shield is applied to a day, all remaining scheduled or manual session targets for that date are covered; the user never consumes multiple shields on the same day.
3. **Unforgiving Transparency:** Shielded days are never silently erased or disguised as completed work. They are permanently marked as **`GRACE_CONSUMED` (🛡️)** in the audit history.

---

## 2. Dependencies & Phase 1 Integration

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Streak Engine Baseline** | [`FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md) | Extends `streaks` Room table; integrates Sunday relief day freeze logic with shield consumption. |
| **Stop Focusing Friction** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Updates modal quit warning: informs user if quitting will consume an available shield or wipe the streak to 0. |
| **Home Dashboard Header** | `Tab.Home` Executive Cockpit | Streak pill displays live fire count and shield badge: e.g. `[🔥 47 · 🛡️ 2]`. Tapping opens `/streaks/history`. |

---

## 3. The Stacking Shield & Evaluation Engine

### 3.1 Monthly Accumulation Logic
* **Grant Frequency:** On the 1st day of each month at 00:01 AM, `DailyStreakWorker` checks `last_shield_granted_month`.
* **Stacking Rule:** If current month (`YYYY-MM`) != `last_shield_granted_month`:
  $$\text{accumulated\_grace\_shields} \leftarrow \text{accumulated\_grace\_shields} + 1$$
  $$\text{last\_shield\_granted\_month} \leftarrow \text{currentMonth}$$
* **Default Maximum Bank Cap:** Configurable in settings, default capped at **5 shields maximum** to prevent indefinite neglect while rewarding long-term discipline.

---

### 3.2 Midnight Automated Evaluation Protocol (`DailyStreakWorker`)

Every night at **00:01 AM**, a non-dismissible WorkManager periodic worker evaluates the preceding calendar day (`yesterday`):

```
                                [ Midnight Evaluation (00:01 AM) ]
                                                |
                                                v
                              Was yesterday a designated Relief Day?
                                      (e.g., Sunday Relief)
                                       /              \
                                     YES              NO
                                     /                  \
                        [ Streak Freezes Neutrally ]     v
                        (No reset, no increment)    Were target sessions completed?
                                                     /              \
                                                   YES              NO
                                                   /                  \
                                     [ Streak +1 ]                     v
                                                              Do you have >= 1 Grace Shield?
                                                                 /                   \
                                                               YES                   NO
                                                               /                       \
                                        [ Auto-Consume 1 Shield ]             [ Streak Resets to 0 ]
                                        - Shields: N -> N - 1                 - Current streak = 0
                                        - Streak preserved (N days)           - Status: BROKEN
                                        - Status: GRACE_CONSUMED
                                        - Shields today for 24h
```

---

### 3.3 Mid-Session Stop Modal Awareness (Whole-Day Shielding)

When the user triggers the quit modal during an active session (`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`):

1. **If Today Already Has an Active Shield (or Target Met Previously):**
   * Modal Warning: *"You have already completed a session or activated a shield today. Quitting now will save your elapsed time without affecting your streak."*
2. **If Shields Are Available (`accumulated_grace_shields >= 1`):**
   * Modal Warning:  
     > *"Quitting now will consume 1 Grace Shield (🛡️). Your streak will remain protected at **[N] days**, and today will be covered for all remaining sessions. (Shields remaining: **[N-1]**)."*
   * Streak Display: `[🔥 18 days → 18 days 🛡️]`
3. **If Zero Shields Available (`accumulated_grace_shields == 0`):**
   * Modal Warning:  
     > *"⚠️ ZERO SHIELDS AVAILABLE. Quitting now will immediately reset your streak to 0."*
   * Streak Display: `[🔥 18 days → 0 days]`

---

## 4. Dedicated Streak Calendar & Audit UI (`/streaks/history`)

Accessible by tapping the Streak Pill on the Home Dashboard header or inside `Tab.Stats`.

```
+-------------------------------------------------------+
|  ←  Streak History & Audit                            |
+-------------------------------------------------------+
|                                                       |
|   CURRENT STREAK            BEST STREAK     SHIELDS   |
|      🔥 47 days              🏆 92 days       🛡️ 2    |
|                                                       |
|  +-------------------------------------------------+  |
|  |  <               October 2026                 >  |  |
|  |                                                 |  |
|  |   M    T    W    T    F    S    S               |  |
|  |             1    2    3    4    5               |  |
|  |  [🟢] [🟢] [🟢] [🟢] [🟢] [🟢] [💤]             |  |
|  |   6    7    8    9   10   11   12               |  |
|  |  [🟢] [🟢] [🛡️] [🟢] [🟢] [🟢] [💤]             |  |
|  |  13   14   15   16   17   18   19               |  |
|  |  [🟢] [🟢] [●]                                  |  |
|  +-------------------------------------------------+  |
|                                                       |
|  MONTH AUDIT LEGEND:                                  |
|  🟢 Session Target Completed (11 days)                |
|  🛡️ Grace Shield Consumed (Oct 8 - Preserved)         |
|  💤 Sunday Relief Day (2 days)                        |
|  🔴 Broken / Reset (0 days)                           |
|                                                       |
|  SHIELD INVENTORY:                                    |
|  +-------------------------------------------------+  |
|  | 🛡️ 2 Active Shields Available                   |  |
|  | Next earned: Nov 1, 2026 (+1 Shield)            |  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

---

## 5. Database Schema & Room Entities

### 5.1 Updated `streaks` Entity

```kotlin
package com.flux.focus.features.streaks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Singleton

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak")
    val bestStreak: Int = 0,

    @ColumnInfo(name = "last_completed_date")
    val lastCompletedDate: String? = null, // "YYYY-MM-DD"

    @ColumnInfo(name = "relief_days_json")
    val reliefDaysJson: String = "[\"SUNDAY\"]",

    // PHASE 2 SHIELD FIELDS:
    @ColumnInfo(name = "accumulated_grace_shields")
    val accumulatedGraceShields: Int = 1, // Starts with 1 introductory shield

    @ColumnInfo(name = "shields_used_lifetime")
    val shieldsUsedLifetime: Int = 0,

    @ColumnInfo(name = "last_shield_granted_month")
    val lastShieldGrantedMonth: String, // "YYYY-MM"

    @ColumnInfo(name = "last_shield_used_date")
    val lastShieldUsedDate: String? = null, // "YYYY-MM-DD"

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
```

### 5.2 Historical Audit Entity (`streak_day_history`)

```kotlin
package com.flux.focus.features.streaks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak_day_history",
    indices = [Index("date")]
)
data class StreakDayHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "status")
    val status: String, // "MET", "RELIEF_DAY", "GRACE_CONSUMED", "BROKEN"

    @ColumnInfo(name = "sessions_completed_count")
    val sessionsCompletedCount: Int = 0,

    @ColumnInfo(name = "focus_duration_minutes")
    val focusDurationMinutes: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
```

### 5.3 Room DAO (`StreakDao.kt`)

```kotlin
package com.flux.focus.features.streaks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flux.focus.features.streaks.data.entity.StreakDayHistoryEntity
import com.flux.focus.features.streaks.data.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Query("SELECT * FROM streaks WHERE id = 1")
    fun observeStreak(): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks WHERE id = 1")
    suspend fun getStreak(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordDayHistory(history: StreakDayHistoryEntity)

    @Query("SELECT * FROM streak_day_history WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun observeMonthHistory(monthPrefix: String): Flow<List<StreakDayHistoryEntity>> // e.g. "2026-10"

    @Query("SELECT * FROM streak_day_history WHERE date = :date LIMIT 1")
    suspend fun getDayHistory(date: String): StreakDayHistoryEntity?
}
```

---

## 6. Jetpack Compose UI Specifications

### 6.1 Color & Typography Tokens (Holst Theme)

| Token | Hex | Element / Usage |
|---|---|---|
| `SurfaceCard` | `#1B263B` | History calendar sheet, summary metric boxes |
| `SurfaceBorder` | `#415A77` | Calendar grid borders, disabled cells |
| `AccentSage` | `#778D7A` | Completed day cells (`[🟢]`), primary headers |
| `PrimarySand` | `#D4C4A8` | Shield cell highlights (`[🛡️]`), milestone trophies |
| `TextLight` | `#F4F1DE` | Day numbers, streak counters |
| `TextMuted` | `#A0AAB2` | Relief day labels, subtitle descriptions |
| `DangerCoral` | `#E07A5F` | Broken streak day cells (`[🔴]`) |

### 6.2 Responsive Streak Header Pill (with Live Shields)

```kotlin
@Composable
fun StreakHeaderPill(
    currentStreak: Int,
    availableShields: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1B263B),
        border = BorderStroke(1.dp, Color(0xFF415A77))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (currentStreak > 0) "🔥 $currentStreak" else "💤 0",
                color = if (currentStreak > 0) Color(0xFFD4C4A8) else Color(0xFFA0AAB2),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )

            if (availableShields > 0) {
                Text(
                    text = "· 🛡️ $availableShields",
                    color = Color(0xFF778D7A),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
```

---

## 7. Verification & Branch-Testing Isolation Workflow

### 7.1 Zero-Mock Automated & Manual Test Cases
1. **Stacking Accumulation Test:**
   * User has 1 shield on Oct 31.
   * Advance simulated system time to Nov 1 (00:01 AM).
   * **Verify:** `accumulated_grace_shields` becomes `2`, `last_shield_granted_month = "2026-11"`.
2. **Whole-Day Immunity Test:**
   * User quits mid-session at 10:00 AM; shield is consumed (`shields = 1 -> 0`).
   * Start 2nd session at 3:00 PM and quit mid-way.
   * **Verify:** Stop modal recognizes today is already protected; no additional shield is consumed, streak remains at 47 days.
3. **Midnight Auto-Consumption Test:**
   * On Wednesday (non-relief day), 0 sessions completed.
   * Advance system clock across midnight to 00:01 AM.
   * **Verify:** `DailyStreakWorker` consumes 1 shield, logs `status = 'GRACE_CONSUMED'` for Wednesday, and streak remains intact.
4. **Zero-Shield Reset Test:**
   * With 0 shields available, complete 0 sessions on Thursday.
   * Advance across midnight.
   * **Verify:** Streak resets to 0, logged as `status = 'BROKEN'`.

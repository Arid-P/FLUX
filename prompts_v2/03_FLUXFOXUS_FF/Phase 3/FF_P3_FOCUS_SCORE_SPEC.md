# 🧠 Topic FF-P3-1: Focus Score Engine Specification

**Document ID:** `FF_P3_FOCUS_SCORE_SPEC`  
**Phase:** Phase 3 (Intelligence, Lock Screen Presence & Session Ergonomics)  
**Target Package:** `com.flux.focus.features.score`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + WorkManager Midnight Calculation)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Quantitative tracking alone (e.g. "Focused for 3 hours") is inherently misleading. A student can keep a timer running while taking frequent unearned pauses, checking Instagram between sessions, and repeatedly quitting halfway through difficult problems.

The **Focus Score** collapses an entire day's focus behavior into an objective, glanceable **0–100 daily integer**. It evaluates not just desk time, but the discipline, restraint, and grit demonstrated across the day.

### 1.1 Core Principles
* **100% Quantitative & Objective:** The score is calculated strictly from empirical Room and UsageStats records. No arbitrary feelings or subjective modifiers inflate or deflate the number.
* **100% Offline & Local:** Computed entirely on-device by local Room queries; zero cloud dependencies, zero external tracking.
* **Balanced Marks System (100 Max Points):** Divided into 4 distinct performance categories that sum to 100 marks.
* **Configurable for Advanced Users:** Default weights (40 / 25 / 20 / 15) can be customized via interactive sliders in Settings to match individual study strategies.

---

## 2. Dependencies & Upstream Integration

| Component | Upstream Specification | Data Contract / Input Metric |
|---|---|---|
| **Focus Time Tracking** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Sum of `actual_duration_seconds` across completed sessions today. |
| **Break Enforcement** | [`FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md) | `breaks_taken` vs preset `break_count` allowed. |
| **App Limits** | [`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md) | Actual app usage from `UsageStatsManager` vs `app_limits.daily_budget_minutes`. |
| **Session Completion** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Completed sessions (`status = 'COMPLETED'`) vs Quit sessions (`status = 'QUIT_PREMATURELY'`). |
| **Sunday Relief Days** | [`FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md) | Sunday is an off/relief day; score freezes neutrally without penalty. |

---

## 3. The 4 Quantitative Component Calculations

The day's total score is the sum of four sub-scores:

$$\text{Daily Focus Score} = \text{round}\left(S_{\text{time}} + S_{\text{break}} + S_{\text{limit}} + S_{\text{completion}}\right)$$

Where each component score $S_i$ is computed from its percentage achievement ($0.0 \text{ to } 1.0$) multiplied by its configured weight $W_i$:

```
+-----------------------------------------------------------------------------------+
|  1. FOCUS TIME VS DAILY TARGET (Default Weight: 40 Marks)                         |
|     Target: User-configured daily focus goal (Default: 120 minutes / 2 hours).     |
|                                                                                   |
|     Achievement Ratio: R_time = clamp(actualFocusMinutes / targetMinutes, 0.0, 1.0)|
|     Score: S_time = R_time × W_time                                               |
|     - Example: 90 mins focused out of 120m target (75%) -> 0.75 × 40 = 30.0 marks. |
+-----------------------------------------------------------------------------------+
|  2. BREAK DISCIPLINE (Default Weight: 25 Marks)                                   |
|     Measures whether the user stayed within their planned preset breaks.          |
|                                                                                   |
|     Total Allowed Breaks = sum(preset.break_count across all today's sessions)     |
|     Total Breaks Taken   = sum(session.breaks_taken across all today's sessions)   |
|                                                                                   |
|     - If Total Allowed Breaks == 0: R_break = 1.0 (Zero breaks = perfect purity)  |
|     - If Total Breaks Taken <= Allowed: R_break = 1.0 (Full 25 marks awarded)     |
|     - If Breaks Taken > Allowed:                                                  |
|       R_break = clamp(1.0 - ((breaksTaken - breaksAllowed) / breaksAllowed), 0.0) |
|     Score: S_break = R_break × W_break                                            |
+-----------------------------------------------------------------------------------+
|  3. APP LIMIT COMPLIANCE (Default Weight: 20 Marks)                               |
|     Evaluates adherence to daily app limits on distracting apps (Tab.Limits).     |
|                                                                                   |
|     For each app with an active daily limit:                                      |
|       overageMinutes = max(0, actualUsageMinutes - dailyBudgetMinutes)            |
|       appCompliance  = clamp(1.0 - (overageMinutes / dailyBudgetMinutes), 0.0, 1.0)|
|                                                                                   |
|     R_limit = average(appCompliance across all limited apps)                      |
|     - If no apps have limits configured: R_limit = 1.0 (Neutral 100%)             |
|     Score: S_limit = R_limit × W_limit                                            |
+-----------------------------------------------------------------------------------+
|  4. SESSION COMPLETION RATE (Default Weight: 15 Marks)                            |
|     Measures resilience against rage-quitting during sessions.                    |
|                                                                                   |
|     R_completion = completedCount / (completedCount + quitPrematurelyCount)       |
|     - If zero sessions were started today: R_completion = 1.0 (Neutral 100%)      |
|     Score: S_completion = R_completion × W_completion                            |
|     - Example: 2 finished, 1 quit -> 2 / 3 (66.7%) -> 0.667 × 15 = 10.0 marks.    |
+-----------------------------------------------------------------------------------+
```

---

## 4. Advanced Component Weight Customization

For advanced users who want to emphasize specific disciplines (e.g. Olympiad problem solvers who value long unbroken blocks over strict app limits):

### 4.1 Settings UI (`Settings → Focus Score → Component Weights`)
* **Location:** Accessible under an **"Advanced Configuration"** expandable disclosure.
* **4 Linked Sliders (Step: 5 Marks):**
  * Focus Time Target ($W_{\text{time}}$): `0 – 100` (Default: `40`)
  * Break Discipline ($W_{\text{break}}$): `0 – 100` (Default: `25`)
  * App Limit Compliance ($W_{\text{limit}}$): `0 – 100` (Default: `20`)
  * Session Completion ($W_{\text{completion}}$): `0 – 100` (Default: `15`)
* **Strict Sum-to-100 Constraint:**
  * The sum of all four sliders is strictly locked to **100**:
    $$W_{\text{time}} + W_{\text{break}} + W_{\text{limit}} + W_{\text{completion}} = 100$$
  * When the user drags one slider up or down, the remaining three sliders auto-balance proportionally:
    $$\Delta = 100 - \text{newSliderValue} - \sum \text{otherSliders}$$
  * A persistent **[ Reset to Defaults (40 / 25 / 20 / 15) ]** button restores standard calibration.

---

## 5. The Weekly Rhythm & Checkpoint System

### 5.1 Week Structure (Monday through Saturday)
* **Daily Computation:**
  * WorkManager evaluates the finalized score at **00:01 AM** for the previous day.
  * `Tab.Stats` computes and previews the **Live Running Score** in real-time throughout the day.
* **Sunday Relief Day:**
  * Sunday is an official rest day.
  * No Focus Score is computed or demanded for Sunday.
  * The weekly report graph shows Sunday as a restful pause (`💤 Relief`).

### 5.2 User-Configured Checkpoint Days
* **Purpose:** Mid-week progress pulses that prevent users from discovering poor study habits only when the week is already lost.
* **Setting:** `Settings → Focus Score → Checkpoint Days` (Multi-select Mon–Fri; Saturday is permanent).
* **Default Checkpoints:** **Wednesday & Saturday**.
* **9:00 PM Checkpoint Notification:**
  On configured checkpoint days at 9:00 PM, a High-Priority notification is pushed:
  > *"📊 Focus Checkpoint: Your average score for [Mon–Wed] is 84 / 100."*
  * Tapping the notification opens `Tab.Stats` with the detailed component breakdown.
* **Saturday Night Final Rollup:**
  Calculates the definitive **Weekly Focus Index** (unweighted average of Monday through Saturday daily scores).

---

## 6. Database Schema & Data Models

### 6.1 `daily_focus_scores` Room Entity

```kotlin
package com.flux.focus.features.score.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_focus_scores",
    indices = [Index("date")]
)
data class DailyFocusScoreEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "total_score")
    val totalScore: Int, // 0 to 100

    @ColumnInfo(name = "time_score")
    val timeScore: Float,

    @ColumnInfo(name = "break_score")
    val breakScore: Float,

    @ColumnInfo(name = "limit_score")
    val limitScore: Float,

    @ColumnInfo(name = "completion_score")
    val completionScore: Float,

    @ColumnInfo(name = "focus_minutes_actual")
    val focusMinutesActual: Int,

    @ColumnInfo(name = "target_minutes")
    val targetMinutes: Int,

    @ColumnInfo(name = "is_checkpoint_day")
    val isCheckpointDay: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

### 6.2 DataStore Preferences (`FocusScorePreferenceKeys.kt`)

```kotlin
package com.flux.focus.features.score.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object FocusScorePreferenceKeys {
    val DAILY_FOCUS_TARGET_MINUTES = intPreferencesKey("daily_focus_target_minutes") // Default: 120
    val WEIGHT_TIME = intPreferencesKey("weight_time")               // Default: 40
    val WEIGHT_BREAK = intPreferencesKey("weight_break")             // Default: 25
    val WEIGHT_LIMIT = intPreferencesKey("weight_limit")             // Default: 20
    val WEIGHT_COMPLETION = intPreferencesKey("weight_completion")   // Default: 15
    val CHECKPOINT_DAYS = stringSetPreferencesKey("checkpoint_days") // Default: ["WEDNESDAY", "SATURDAY"]
}
```

---

## 7. Jetpack Compose UI Specifications (`Tab.Stats`)

### 7.1 Circular Focus Score Gauge Composable

```kotlin
@Composable
fun FocusScoreCard(
    totalScore: Int,
    timeScore: Float,
    maxTimeScore: Int,
    breakScore: Float,
    maxBreakScore: Int,
    limitScore: Float,
    maxLimitScore: Int,
    completionScore: Float,
    maxCompletionScore: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
        border = BorderStroke(1.dp, Color(0xFF415A77)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DAILY FOCUS SCORE",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFA0AAB2),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Score Dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                CircularProgressIndicator(
                    progress = { totalScore / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF778D7A),
                    trackColor = Color(0xFF0D1B2A),
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalScore",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFFD4C4A8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getScoreTierLabel(totalScore),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF4F1DE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 Breakdown Rows
            ScoreBreakdownRow(
                icon = "⏱️",
                label = "Focus Time Goal",
                score = timeScore,
                maxScore = maxTimeScore
            )
            ScoreBreakdownRow(
                icon = "☕",
                label = "Break Discipline",
                score = breakScore,
                maxScore = maxBreakScore
            )
            ScoreBreakdownRow(
                icon = "🚫",
                label = "App Limit Compliance",
                score = limitScore,
                maxScore = maxLimitScore
            )
            ScoreBreakdownRow(
                icon = "🎯",
                label = "Completion Rate",
                score = completionScore,
                maxScore = maxCompletionScore
            )
        }
    }
}

private fun getScoreTierLabel(score: Int): String = when {
    score >= 85 -> "DEEP FLOW"
    score >= 70 -> "SOLID FOCUS"
    score >= 50 -> "DRIFTING"
    else -> "UNFOCUSED"
}
```

### 7.2 Holst Design Tokens Applied

| Element | Token / Color | Value / Usage |
|---|---|---|
| Card Background | `SurfaceCard` | `#1B263B` (Oxford Slate) |
| Gauge Background Track | `BackgroundPrimary` | `#0D1B2A` (Abyssal Navy) |
| Active Score Arc | `AccentSage` | `#778D7A` (Muted Sage Green) |
| Central Score Number | `PrimarySand` | `#D4C4A8` (Warm Sand Khaki) |
| Primary Typography | `TextLight` | `#F4F1DE` (Eggshell Cream) |
| Category Subtitles | `TextMuted` | `#A0AAB2` (Muted Steel Subtext) |

---

## 8. Verification & Branch-Testing Isolation Workflow

### 8.1 Zero-Mock Automated & Manual Test Cases
1. **Mathematical Accuracy Test:**
   * Simulate: Target = 120m, Actual = 90m (30/40), 0 extra breaks (25/25), 1 app limit over by 10% (18/20), 2/2 sessions completed (15/15).
   * **Verify:** Calculated score is exactly $30 + 25 + 18 + 15 = \mathbf{88}$.
2. **Neutral Zero-Activity Test:**
   * On day with zero sessions started and zero limit breaches:
   * **Verify:** Score remains neutral 100 on breaks, limits, and completion; only penalizes time ratio.
3. **Advanced Slider Rebalancing Test:**
   * Drag $W_{\text{time}}$ from 40 to 70 (+30).
   * **Verify:** Remaining 60 points distribute proportionally across breaks, limits, and completion; total remains exactly 100.
   * Tap "Reset to Defaults" -> Verify weights restore to 40 / 25 / 20 / 15.
4. **Checkpoint Notification Alarm Test:**
   * Configure Wednesday as Checkpoint Day.
   * Trigger simulated alarm at Wednesday 9:00 PM.
   * **Verify:** Checkpoint notification posts with exact average score for Monday through Wednesday.

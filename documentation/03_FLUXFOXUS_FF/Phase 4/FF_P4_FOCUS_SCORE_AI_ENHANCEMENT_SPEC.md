# 🎯 Topic FF-P4-1: Focus Score 5th Component & AI Task Alignment Specification

**Document ID:** `FF_P4_FOCUS_SCORE_AI_ENHANCEMENT_SPEC`  
**Phase:** Phase 4 (Cross-Platform Daemon, Group Limits & Habit Breaking)  
**Target Package:** `com.flux.focus.features.score.enhancement`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + IPC Inter-App Contract + Google Gemini Kotlin SDK)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Phase 3 established the 4-component quantitative Focus Score (Time 40, Breaks 25, Limits 20, Completion 15). While those metrics measure behavioral discipline, they do not verify whether that focus was directed toward planned goals.

**Phase 4 introduces the 5th Component: Task Alignment & Completion (5 Marks)**. It bridges FluxFoxus focus time with FluxDone planned tasks, introducing contextual AI reasoning for missed or rescheduled tasks.

### 1.1 The 100-Mark Allocation
The score retains its strict 100-mark sum constraint, recalibrated with the 5th component:

```
Component                       Phase 3 Weight    Phase 4 Default
-----------------------------------------------------------------
1. Focus Time vs Target               40 marks       40 marks
2. Break Discipline                   25 marks       20 marks
3. App Limit Compliance               20 marks       20 marks
4. Session Completion Rate            15 marks       15 marks
5. Task Alignment (NEW)               -- marks        5 marks
-----------------------------------------------------------------
TOTAL MAXIMUM SCORE                  100 marks      100 marks
```

---

## 2. Dependencies & Cross-App Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Base Focus Score** | [`FF_P3_FOCUS_SCORE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%203/FF_P3_FOCUS_SCORE_SPEC.md) | Extends `daily_focus_scores` Room entity and `FocusScoreCard` composable. |
| **FD ↔ FF IPC Bridge** | `IPC_SPEC_FD_FF_NATIVE.md` | Queries `getDayTaskSummary` via explicit intent / IPC receiver. |
| **AI Intelligence Hub** | [`FF_P3_AI_COACHING_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%203/FF_P3_AI_COACHING_SPEC.md) | Uses configured Gemini 1.5 Flash client if `ai_master_enabled == true`. |

---

## 3. Calculation Mechanics: The 5th Component

### 3.1 Mode A: Standalone Mode (FluxDone Not Installed or 0 Tasks Scheduled)
* **Neutral 100% Score:** If FluxFoxus is operated standalone, or if the user planned 0 tasks for today in FluxDone:
  $$S_{\text{task}} = 1.0 \times 5 = \mathbf{5.0 / 5\text{ marks}}$$
* **Principle:** Users are never penalized for not using FluxDone or having an untracked day.

---

### 3.2 Mode B: Connected Mode (Deterministic Formula)
Active when FluxDone returns task data, but AI is OFF or no contextual notes exist:
* **Input Data:**
  * `totalScheduled`: Number of tasks with start/end times planned for today.
  * `completed`: Tasks marked completed (`is_completed = true`).
  * `wontDo`: Tasks marked with "Won't Do" status.
  * `incomplete`: Tasks unfinished and not marked "Won't Do".
* **Formula:**
  $$\text{Adjusted Total} = \text{totalScheduled} - \text{wontDo}$$
  $$R_{\text{task}} = \text{clamp}\left(\frac{\text{completed}}{\text{Adjusted Total}}, 0.0, 1.0\right)$$
  $$S_{\text{task}} = R_{\text{task}} \times 5.0$$
* Won't Do tasks with zero context are excluded from both numerator and denominator.

---

### 3.3 Mode C: AI Contextual Evaluation (When Notes / Reasons Exist)
Active when:
1. `ai_master_enabled == true` and valid Gemini API key is present.
2. At least one task is `incomplete` or marked `wontDo` **AND** the user recorded end-of-day notes or reasons in FluxDone.

#### Analytical Evaluation Prompt:
```
System: You are an analytical academic evaluator for FluxFoxus. 
Evaluate the user's task completion component (0 to 100 scale) based on their actual study context.
If unfinished tasks were legitimately deprioritized because an existing deep work block required extra time (e.g. difficult Olympiad problem or exam prep), award full or near-full credit.
If incomplete tasks have no valid academic explanation, penalize proportionally.
Respond ONLY with valid JSON:
{
  "score": integer (0 to 100),
  "reasoning": "string (concise explanation, max 40 words)"
}

Input:
- Scheduled Tasks Today: {totalScheduled}
- Completed: {completed}
- Won't Do: {wontDo}
- Unfinished: {incomplete}
- User Notes/Context: "{notesText}"
```

#### Final Component Score:
$$S_{\text{task}} = \frac{\text{AI Score}}{100} \times 5.0$$
* The returned 40-word `reasoning` string is stored in Room and displayed directly in the UI.

---

## 4. UI Specifications (`Tab.Stats`)

### 4.1 Updated Focus Score Breakdown

A 5th row is added to `FocusScoreCard`:

```
+-------------------------------------------------------+
|  DAILY FOCUS SCORE                                    |
|                     ╭─────────╮                       |
|                   ╭─╯   92    ╰─╮   DEEP FLOW         |
|                     ╰─────────╯     (Top 10% Week)    |
|                  [ 92 / 100 Points ]                  |
|                                                       |
|  SCORE BREAKDOWN:                                     |
|  +-------------------------------------------------+  |
|  | ⏱️ Focus Time Goal:      38.0 / 40  (114m / 120m) |  |
|  | ☕ Break Discipline:      20.0 / 20  (0 unearned)  |  |
|  | 🚫 App Limit Compliance:  18.0 / 20  (1 app over)  |  |
|  | 🎯 Completion Rate:       11.5 / 15  (3/4 finished)|  |
|  | 📋 Task Alignment:         4.5 / 5   (90% aligned) |  |
|  +-------------------------------------------------+  |
|                                                       |
|  [ 💡 Coach Rationale ▾ ]                             |
|  +-------------------------------------------------+  |
|  | "Task component: 90/100. Incomplete physics set |  |
|  | was justified by extending your Olympiad Math   |  |
|  | block. Excellent prioritization."               |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

* **Interactive Disclosure:** Tapping `[ 💡 Coach Rationale ▾ ]` smoothly expands the AI explanation card styled in Oxford Slate `#1B263B` with Muted Sage border `#778D7A`.

---

## 5. Database Schema & Data Models

### 5.1 Updated `daily_focus_scores` Room Entity

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
    val timeScore: Float, // Max 40

    @ColumnInfo(name = "break_score")
    val breakScore: Float, // Max 20

    @ColumnInfo(name = "limit_score")
    val limitScore: Float, // Max 20

    @ColumnInfo(name = "completion_score")
    val completionScore: Float, // Max 15

    // PHASE 4 ENHANCEMENTS:
    @ColumnInfo(name = "task_score")
    val taskScore: Float = 5.0f, // Max 5

    @ColumnInfo(name = "task_component_reasoning")
    val taskComponentReasoning: String? = null,

    @ColumnInfo(name = "score_finalized")
    val scoreFinalized: Boolean = false,

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

### 5.2 Settings DataStore Key

```kotlin
package com.flux.focus.features.score.data.preferences

import androidx.datastore.preferences.core.intPreferencesKey

object FocusScoreP4PreferenceKeys {
    val WEIGHT_TASK = intPreferencesKey("weight_task") // Default: 5
}
```

---

## 6. Verification & Branch-Testing Isolation Workflow

### 6.1 Zero-Mock Automated & Manual Test Cases
1. **Standalone Independence Test:**
   * Run score calculation on a device with FluxDone uninstalled.
   * **Verify:** `task_score` automatically evaluates to `5.0 / 5` (neutral full credit); total score sums accurately without errors.
2. **Deterministic Task Math Test:**
   * Mock IPC task response: 4 scheduled, 3 completed, 0 wontDo, 1 incomplete.
   * **Verify:** $R_{\text{task}} = 3 / 4 = 0.75 \implies \text{task\_score} = 0.75 \times 5.0 = \mathbf{3.75}$.
3. **AI Contextual Reasoning Test:**
   * Input: 1 incomplete task with note *"Olympiad contest round ran 90 mins over time"*.
   * **Verify:** Gemini AI awards $\ge 90\%$ credit, populates `task_component_reasoning`, and the UI displays the expandable rationale card.

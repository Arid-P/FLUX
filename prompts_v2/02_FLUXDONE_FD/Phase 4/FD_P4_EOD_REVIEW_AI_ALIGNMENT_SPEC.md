# 🌙 Topic FD-P4-4: End-of-Day Review & AI Task Alignment Generator Specification

**Document ID:** `FD_P4_EOD_REVIEW_AI_ALIGNMENT_SPEC`  
**Phase:** Phase 4 (Cloud Sync, Cross-Device Ecosystem & Advanced Notes)  
**Target Package:** `com.flux.done.review.eod`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Compose Multiplatform 1.6+ + Room 2.7+ KMP + Local Heuristic AI Engine)  
**Target Platforms:** Android Phone & Tablet (`com.fluxdone`), Ubuntu Linux Workstation (`com.fluxdone.desktop`)  
**Prerequisites:** [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md), [`FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%202/FD_P2_NOTIFICATIONS_REMINDERS_SPEC.md), & [`FF_P3_FOCUS_SCORE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_FOCUS_SCORE_SPEC.md)  

---

## 1. Overview & Philosophical Role

High-performance academics requires closing psychological open loops at the end of each day. Leaving unfinished tasks floating as overdue guilt induces cognitive fatigue and harms sleep quality.

**Topic FD-P4-4 establishes the Nightly 9:00 PM End-of-Day (EOD) Review Sheet & AI Task Alignment Generator.**

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                         NIGHTLY 9:00 PM REVIEW & ALIGNMENT PIPELINE                         |
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                             |
|   [ 21:00 Alarm / Notification: "🌙 Time for your Daily Review" ]                           |
|                                   │                                                         |
|                                   ▼                                                         |
|   ┌─────────────────────────────────────────────────────────────────────────────────────┐   |
|   │                       FLUXDONE: DAILY REVIEW MODAL (SCR-11)                         │   |
|   │  1. Celebrate Completed: 5 tasks done, 180 min deep work logged in FluxFoxus        │   |
|   │  2. Rapid Triage Incomplete: [ 🌅 Tomorrow ] [ 📆 Reschedule ] [ 🚫 Won't Do ]      │   |
|   │  3. Reflection Input: "What went well? What blocked momentum?"                     │   |
|   └───────────────────────────────────┬─────────────────────────────────────────────────┘   |
|                                       │                                                     |
|                                       ├─────────────────────────────┐                       |
|                                       ▼                             ▼                       |
|   ┌───────────────────────────────────────────┐   ┌─────────────────────────────────────┐   |
|   │   FLUXFOXUS 5TH FOCUS SCORE COMPONENT     │   │      AI TASK ALIGNMENT GENERATOR    │   |
|   │   Broadcast: `EOD_REVIEW_COMPLETED`       │   │  Suggests Tomorrow's schedule:      │   |
|   │   Awards up to 15 Points:                 │   │  • High-friction Olympiad: 09:00 AM │   |
|   │   • EOD Review completed on time (+5)     │   │  • Syllabus Review: 15:00 PM        │   |
|   │   • Zero unaddressed tasks left (+5)      │   │  • [ 1-Tap Accept Schedule ]        │   |
|   │   • >=70% scheduled focus achieved (+5)   │   └─────────────────────────────────────┘   |
|   └───────────────────────────────────────────┘                                             |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```

---

## 2. Nightly 9:00 PM Review Sheet Specification

### 2.1 Trigger & Notification Mechanics

* **Scheduled Time:** `21:00` (9:00 PM, user-configurable in Settings between 20:00 and 23:00).
* **Notification Shade Action:**
  * Channel: `channel_agenda` (High priority, audible gentle chime).
  * Text: *"🌙 Time for your Daily Review. Wrap up today's 3 remaining tasks and prepare for tomorrow."*
  * Action Button: `[ Open Review ]` $\to$ launches `DailyReviewActivity` / Desktop Modal.

### 2.2 Wireframe & Anatomy (`DailyReviewSheet.kt`)

```
+-----------------------------------------------------------------------------------+
|  🌙 END-OF-DAY REVIEW — 13 OCT 2026                                        [ ✕ ]  |
+-----------------------------------------------------------------------------------+
| TODAY'S MOMENTUM                                                                  |
| [ ✓ 4 Tasks Completed ]      [ ⏱️ 180m Deep Focus (FF) ]     [ 🔥 18 Day Streak ] |
+-----------------------------------------------------------------------------------+
| INCOMPLETE TASKS TRIAGE (2 Remaining)                                             |
|                                                                                   |
| 1. Solve 5 Incenter Problems (!High, Olympiad Geometry)                           |
|    [ 🌅 Tomorrow ]   [ 📆 Pick Date ]   [ 🚫 Won't Do ]   [ 🗑️ Trash ]           |
|                                                                                   |
| 2. Simson Line Proof (!Med, Olympiad Geometry)                                    |
|    [ 🌅 Tomorrow ]   [ 📆 Pick Date ]   [ 🚫 Won't Do ]   [ 🗑️ Trash ]           |
+-----------------------------------------------------------------------------------+
| DAILY ACADEMIC REFLECTION                                                         |
| [ Understood incenter lemmas; got stuck on Problem 4 angle chasing diagram...   ] |
+-----------------------------------------------------------------------------------+
| [ ✨ AI Align Tomorrow's Schedule ]                     [ Complete Review (✓) ]   |
+-----------------------------------------------------------------------------------+
```

### 2.3 Rapid Triage Actions

For each incomplete task, Ari can resolve the status in a single tap:
1. `[ 🌅 Tomorrow ]`: Moves `due_date` to tomorrow with same priority and list.
2. `[ 📆 Pick Date ]`: Opens quick date picker for future scheduling.
3. `[ 🚫 Won't Do ]`: Marks as skipped; advances recurrence rule if recurring; preserves record for analytics.
4. `[ 🗑️ Trash ]`: Soft deletes with 30-day trash retention.

---

## 3. Feeding the FluxFoxus 5th Focus Score Component

In FluxFoxus ([`FF_P3_FOCUS_SCORE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_FOCUS_SCORE_SPEC.md)), the Daily Focus Score (0–100) incorporates five pillars:
1. Base Duration Score (30 pts)
2. Intensity & Pause Score (20 pts)
3. Distraction Resistance Score (20 pts)
4. Streak Momentum Score (15 pts)
5. **Task Alignment & Daily Review Score (15 pts)**

### 3.1 Scoring Criteria & IPC Broadcast

When the review sheet is submitted, FluxDone evaluates Ari's discipline and broadcasts the result to FluxFoxus:

| Sub-Criteria | Max Points | Evaluation Condition |
|---|:---:|---|
| **EOD Review Timeliness** | `5 pts` | Submitted between 20:00 and 23:59 on the same calendar day. |
| **Zero Floating Overdue** | `5 pts` | All scheduled tasks for today were resolved (completed, rescheduled, or marked won't-do). |
| **Scheduled Focus Alignment**| `5 pts` | Logged focus session minutes $\ge 70\%$ of scheduled study block minutes. |
| **Total Alignment Points** | **`15 pts`** | Ingested directly into the daily 100-point composite score. |

```kotlin
package com.flux.done.review.eod

import android.content.Context
import android.content.Intent

object EODReviewBroadcastDispatcher {
    const val ACTION_EOD_REVIEW_COMPLETED = "com.flux.action.EOD_REVIEW_COMPLETED"

    fun broadcastReviewCompleted(
        context: Context,
        alignmentPoints: Int, // 0 to 15
        reflectionText: String,
        tasksCompletedCount: Int,
        tasksRescheduledCount: Int
    ) {
        val intent = Intent(ACTION_EOD_REVIEW_COMPLETED).apply {
            setPackage("com.fluxfoxus")
            putExtra("alignment_points", alignmentPoints)
            putExtra("reflection_text", reflectionText)
            putExtra("tasks_completed", tasksCompletedCount)
            putExtra("tasks_rescheduled", tasksRescheduledCount)
            putExtra("timestamp_utc", System.currentTimeMillis())
        }
        context.sendBroadcast(intent)
    }
}
```

---

## 4. AI Academic Task Alignment Generator

Clicking `[ ✨ AI Align Tomorrow's Schedule ]` invokes the local heuristic/AI alignment engine:

### 4.1 Scheduling Heuristic Algorithm

1. **Chronotype & Peak Hours:** High-priority, high-cognitive load tasks (`!High`, e.g. *Olympiad Geometry*, *Advanced Calculus*) are scheduled during Ari's proven peak focus window (**09:00 AM – 11:30 AM** based on historical FluxFoxus session momentum).
2. **Buffer Spacing:** Injects a mandatory 15-minute cognitive rest buffer between intense study blocks.
3. **Rescheduled Incomplete Tasks:** Prioritized at the start of study blocks to prevent conceptual debt.
4. **Output Preview:** Presents an interactive timeline preview card:
   ```
   ✨ Suggested Tomorrow Schedule:
   • 09:00 - 10:30 (90m) [!High] Solve 5 Incenter Problems (Rescheduled)
   • 10:45 - 11:45 (60m) [!Med]  Simson Line Proof (Rescheduled)
   • 15:00 - 16:30 (90m) [!High] Mock Contest Problem Set 2
   [ ✓ Accept & Apply to Timeline ]
   ```
5. **1-Tap Application:** Clicking `[ ✓ Accept & Apply ]` sets `start_time` and `end_time` on all tasks in SQLite and dispatches `com.flux.action.FOCUS_BLOCK_SYNC` to pre-arm FluxFoxus for tomorrow!

---

## 5. Verification & Testing Contracts

1. **21:00 Review Alarm Test:** Fast-forward system clock to 21:00:00. Verify high-priority heads-up notification fires on Android and GNOME desktop.
2. **Zero Floating Overdue Enforcement:** Open review with 2 incomplete tasks. Reschedule one to tomorrow and mark the other as `Won't Do`. Verify that today's pending count drops to 0.
3. **Focus Score IPC Relay Test:** Complete review with full compliance. Inspect broadcast intent received by FluxFoxus; verify `alignment_points == 15`.
4. **AI Schedule Application Test:** Generate suggested schedule and tap `Accept`. Query `tasks` table; verify start and end times are updated and `FOCUS_BLOCK_SYNC` is broadcast to FF.

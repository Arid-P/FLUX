# FF Phase 4 — Feature 1: Focus Score Enhancement

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Phase 4 enhances the Focus Score (FF P3 F1) by adding a 5th component derived from FD task completion data. A hybrid scoring model is used: if the user has provided context (Notes or Won't Do flags in FD) for incomplete tasks, AI uses that context to produce an adjusted score. If no context exists, a raw formula computes the component.

This feature requires FD P4 (Won't Do task status) and P4.FF (IPC bridge) to be built and stable before implementation begins.

---

## 2. P3/P4 References

| Reference | Location |
|---|---|
| Focus Score — base implementation | `FF_P3_F1_focus-score.md` |
| Post-session reflection | `FF_P2_F5_post-session-reflection.md` |
| AI toggle + API key | `FF_P2_F3_ai-break-negotiation.md` §3.1 |
| Won't Do task status | `FD_P2_AMENDMENT_wont-do-status.md` |
| Notes (FD P3 F6) | `FD_P3_F6_notes.md` |
| FD ↔ FF IPC | FLUX_CONTEXT.md §5 |
| P4.FF IPC sub-version | Separate IPC spec |

---

## 3. New 5th Component: Task Completion

### 3.1 Data Fetched from FD (via P4.FF IPC)

FF queries FD at midnight score computation time for:

```dart
// Method: 'getDayTaskSummary'
// Arguments: { 'date': int }  // Unix ms for today midnight UTC

// FD response:
{
  'totalScheduled': int,       // Tasks with today's date that had start_time + end_time
  'completed': int,            // is_completed = 1
  'wontDo': int,               // is_wont_do = 1
  'incomplete': int,           // neither completed nor won't do
  'notes': List<String>,       // text content of all Notes dated today
}
```

### 3.2 Raw Formula (no context / AI off)

```
wont_do_adjusted_total = totalScheduled - wontDo
task_completion_component = clamp(completed / wont_do_adjusted_total, 0.0, 1.0) × 100
If wont_do_adjusted_total = 0: component = 100 (no scheduled tasks = neutral)
```

Won't Do tasks are excluded from both numerator and denominator — they are treated as if the tasks never existed for scoring purposes when no context is provided.

### 3.3 AI Hybrid Scoring (AI on + context exists)

**Trigger conditions (all must be true):**
- AI toggle is ON (FF P2 F3 §3.1)
- Valid API key present
- At least one of: notes exist for today OR wontDo > 0

**API request:**

```
System: You are a focus score evaluator. Based on the user's day data, 
evaluate the task completion component of their focus score (0–100).
Be fair — consider the context provided. A genuine reason for incomplete 
tasks should result in a higher score than unexplained incompletions.
Won't Do tasks with no context should be treated as incomplete.
Respond ONLY with valid JSON:
{
  "score": integer (0–100),
  "reasoning": "string (max 60 words)"
}

User:
Scheduled tasks today: [N]
Completed: [N]
Won't Do: [N]  
Incomplete: [N]
Context/notes for today: [notes text joined, or "None provided"]
```

**Response handling:**
- `score` replaces the raw formula component
- `reasoning` stored alongside the daily score record for user to view
- API timeout (10s) or error → silent fallback to raw formula
- Result stored in `daily_focus_scores` table with new `task_component_reasoning` column

### 3.4 Updated Score Formula

The base P3 formula (4 components) gains a 5th:

```
focus_score = round(
  (component_time × weight_time) +
  (component_break × weight_break) +
  (component_limit × weight_limit) +
  (component_completion × weight_completion) +
  (component_task × weight_task)
)
```

Default weights adjusted to accommodate 5th component:

| Component | P3 Default | P4 Default |
|---|---|---|
| Session time vs target | 40% | 35% |
| Break discipline | 25% | 20% |
| App limit compliance | 20% | 20% |
| Session completion rate | 15% | 15% |
| **Task completion (NEW)** | — | **10%** |

User-adjustable weights (P3 F1 §3.4) still apply — all 5 sliders sum to 100.

---

## 4. End-of-Day Prompt (FD-side)

The prompt is handled entirely by FD, not FF. FD evaluates:

1. Last scheduled task end time today
2. If gap ≥ 1 hour before a natural cutoff (~10 PM): FD sends a notification at ~1 hour after last task — *"Any notes for today? They'll help improve your Focus Score."*
3. If gap ≥ 2 hours: no prompt — notification only (no modal)
4. If no gap: no prompt, no notification. User fills manually next day if desired

**Retroactive score update:**
- A periodic WorkManager task runs every 2 hours until midnight checking for new notes/context added for today
- If new context found after initial score computation: score is recomputed with AI (if available) and updated
- Flag: `score_finalized` (boolean) on `daily_focus_scores` — set to `true` at midnight. After finalization, retroactive updates are no longer applied

---

## 5. Database Schema Changes

### 5.1 Modified Table: daily_focus_scores

```sql
ALTER TABLE daily_focus_scores ADD COLUMN component_task INTEGER NOT NULL DEFAULT 100;
ALTER TABLE daily_focus_scores ADD COLUMN task_component_reasoning TEXT;  -- AI reasoning, nullable
ALTER TABLE daily_focus_scores ADD COLUMN score_finalized INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daily_focus_scores ADD COLUMN weight_task INTEGER NOT NULL DEFAULT 10;
```

### 5.2 Updated Settings (Hive)

```
focus_score_weight_task: int  -- default 10
// Existing weights adjusted: time=35, break=20, limit=20, completion=15, task=10
```

---

## 6. Module Boundary

Extensions to existing P3 modules:

```
features/
└── focus_score/
    ├── data/
    │   └── focus_score_repository_impl.dart  ← EXTENDED (5th component, AI call)
    ├── domain/
    │   └── use_cases/
    │       └── compute_focus_score.dart       ← EXTENDED (task component logic)
    └── presentation/
        └── focus_score_screen.dart            ← EXTENDED (5th component row, reasoning text)

features/
└── fd_integration/
    └── data/
        └── method_channel_service.dart        ← EXTENDED (getDayTaskSummary — P4.FF)
```

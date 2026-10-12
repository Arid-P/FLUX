# FD Phase 3 — Feature 5: Subtask Progress on List View

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

When a task has subtasks, its card in SCR-01 (Task List View) displays a small subtask progress indicator showing completed vs total subtasks. This gives the user at-a-glance progress visibility without opening the task detail sheet.

This is a purely additive change to the existing task card component. No new screens, no new routes, no new data — subtask data already exists in the P1 schema.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Task card component — SCR-01 | `ui_SCR-01_list-view.md` |
| Subtasks feature | PRD v2 §5, FD-05 |
| Subtasks table schema | TRD v2 §5 (subtasks table) |
| Task Detail Sheet — subtask display | `ui_SCR-02_task-detail-sheet.md` |

---

## 3. Feature Specification

### 3.1 Indicator Placement

The subtask progress indicator appears in the **metadata row** of the task card in SCR-01 — the same row that shows due date, reminder, recurrence, and priority flag chips.

Position: leftmost item in the metadata row, before other metadata chips.

### 3.2 Visual Specification

| Property | Value |
|---|---|
| Icon | Subtask icon (stacked lines / checklist variant — from `ui_ICONS_icon-system.md`) |
| Icon size | 12dp (micro/badge size — consistent with metadata inline icons) |
| Text | `[completed]/[total]` — e.g. `2/5` |
| Font size | 12sp (metadata size) |
| Font weight | Regular |
| Color — incomplete | `ThemeTokens.secondaryText` (same as all metadata) |
| Color — all complete | `ThemeTokens.primary` (Electric Indigo / app primary) |
| Gap between icon and text | 4dp |

### 3.3 Visibility Rule

- Only shown when `total subtasks ≥ 1`
- Hidden entirely when task has no subtasks — no empty placeholder
- Shown in all list contexts: regular lists, smart lists (Today, Tomorrow, Upcoming, All), search results

### 3.4 All Complete State

When `completed == total`:
- Icon and text color switch to `ThemeTokens.primary`
- No other visual change — no strikethrough, no animation on the card itself
- The individual task completion (main checkbox) is separate from subtask completion — a task with all subtasks done is not automatically marked complete

### 3.5 Tap Behaviour

Tapping the subtask progress indicator has **no independent tap action**. The entire task card tap opens SCR-02 (Task Detail Sheet) as normal. The indicator is display-only.

---

## 4. Data

No new data or queries needed. The subtasks table already exists in P1 schema. The task card widget reads:

```dart
// Already available from ITaskRepository
final int totalSubtasks = task.subtasks.length;
final int completedSubtasks = task.subtasks.where((s) => s.isCompleted).count();
```

If subtasks are lazy-loaded (not included in the list query by default), the list query must be extended to include subtask counts per task. Two options:

**Option A — Extend list query (preferred):**
```sql
SELECT t.*, 
  COUNT(s.id) as subtask_total,
  SUM(s.is_completed) as subtask_completed
FROM tasks t
LEFT JOIN subtasks s ON s.task_id = t.id
WHERE [existing filters]
GROUP BY t.id
```

**Option B — Separate count query per task:** Not recommended — O(n) queries for n tasks causes list jank.

Option A is the correct approach. The list query is extended once, subtask counts are included in the task card data model.

---

## 5. Data Model Change

`TaskCardData` (or equivalent list view model) gains two new fields:

```dart
// Added to task list view model in Phase 3
final int subtaskTotal;      // 0 if no subtasks
final int subtaskCompleted;  // 0 if no subtasks
```

These are computed at query time (SQL COUNT/SUM) — not stored separately.

---

## 6. Module Boundary

Modifications to existing P1 modules only — no new module needed:

```
features/
└── tasks/
    ├── data/
    │   └── task_dao.dart              ← MODIFIED (extend list query with subtask counts)
    ├── domain/
    │   └── task_card_data.dart        ← MODIFIED (2 new fields)
    └── presentation/
        └── task_card.dart             ← MODIFIED (subtask progress indicator in metadata row)
```

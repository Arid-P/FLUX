# FD Phase 2 — Amendment: Won't Do Task Status

**Version:** 1.0  
**Phase:** 2 (Amendment)  
**Status:** Locked  
**Author:** Ari  
**Amends:** PRD v2, TRD v2, `ui_SCR-01_list-view.md`, `ui_SCR-02_task-detail-sheet.md`

---

## 1. Overview

Won't Do is a third task resolution state alongside Complete and Incomplete. When a user decides they will not do a task (intentionally skipping it rather than completing it), they mark it as Won't Do. This is a deliberate, permanent decision — distinct from a task simply being overdue or incomplete.

This feature exists in TickTick and was inadvertently omitted from FD P1/P2 specs. It is added here as a P2 amendment.

---

## 2. Amended References

| Document | Section Amended |
|---|---|
| PRD v2 §5 — Feature Table | New feature FD-WD added |
| TRD v2 §5 — tasks table | New column `is_wont_do` |
| `ui_SCR-01_list-view.md` | Task card checkbox interaction |
| `ui_SCR-02_task-detail-sheet.md` | Task status display |
| Smart Lists | New "Won't Do" smart list added |

---

## 3. Feature Specification

### 3.1 Marking a Task as Won't Do

**Trigger:** Long-press on the task checkbox (in SCR-01 task card or SCR-02 task detail sheet)

A small popup menu appears with two options:
- ✓ **Complete**
- ✕ **Won't Do**

Normal single-tap on checkbox: marks Complete as before (P1 behaviour unchanged).

### 3.2 Won't Do Visual Treatment

| Element | Value |
|---|---|
| Checkbox state | ✕ icon inside checkbox, filled with muted color (`ThemeTokens.secondaryText` at 60% opacity) |
| Task title | Strikethrough, muted color (`ThemeTokens.secondaryText`) |
| Card opacity | 0.7 — same as completed tasks |
| Metadata | Unchanged |

Distinct from completed tasks which use a ✓ checkmark in the list color.

### 3.3 Reversing Won't Do

Won't Do is reversible. Long-press the ✕ checkbox → popup menu:
- ↩ **Mark Incomplete** — restores task to active state
- ✓ **Complete** — marks as completed instead

### 3.4 Won't Do Smart List

A new smart list added to the side drawer alongside Today, Tomorrow, Upcoming, All, Completed, Trash:

| Smart List | Query |
|---|---|
| **Won't Do** | All tasks where `is_wont_do = 1` AND `is_trashed = 0` |

- Sorted by task date descending (most recently marked first)
- Same card format as Completed smart list
- Can restore tasks to incomplete from this list (long-press checkbox → Mark Incomplete)

### 3.5 Behaviour in Other Smart Lists

| Smart List | Won't Do tasks included? |
|---|---|
| Today | ❌ Excluded |
| Tomorrow | ❌ Excluded |
| Upcoming | ❌ Excluded |
| All | ❌ Excluded |
| Completed | ❌ Excluded (has its own list) |
| Won't Do | ✅ Only here |
| Trash | ✅ If trashed |

### 3.6 Won't Do + Trash

Won't Do tasks can be deleted (soft-deleted to Trash) the same as any task. In Trash they appear with the ✕ visual treatment. Restoring from Trash preserves the Won't Do state.

### 3.7 Won't Do + Recurring Tasks

When a recurring task instance is marked Won't Do:
- Same scope dialog as completion: **This task only** / **This and future** / **All in series**
- Only the selected instances are marked Won't Do
- Future ungenerated instances are unaffected unless "All in series" is selected

### 3.8 Won't Do + FF P4.FF Integration

Won't Do tasks are treated as **incomplete without a reason** by default in the FF Focus Score computation (FF P4). If the user has created a Note (FD P3 F6) for the same date, FF reads that note as context for AI scoring. No separate reason field — Notes handle this entirely.

---

## 4. Database Schema Change

### 4.1 Modified Table: tasks

```sql
ALTER TABLE tasks ADD COLUMN is_wont_do INTEGER NOT NULL DEFAULT 0;
ALTER TABLE tasks ADD COLUMN wont_do_at INTEGER;  -- Unix ms timestamp, nullable
```

`is_wont_do = 1` + `wont_do_at` set when marked. Both reset to `0` / `NULL` on restore to incomplete.

### 4.2 Smart List Query

```sql
-- Won't Do smart list
SELECT * FROM tasks
WHERE is_wont_do = 1
  AND is_trashed = 0
ORDER BY wont_do_at DESC
```

---

## 5. Data Model Change

```dart
// Added to Task entity (TRD v2 §3 — domain layer)
final bool isWontDo;
final DateTime? wontDoAt;
```

---

## 6. Module Boundary

Modifications to existing P1/P2 modules only — no new module:

```
features/
└── tasks/
    ├── data/
    │   └── task_dao.dart              ← MODIFIED (new columns, Won't Do query)
    ├── domain/
    │   └── task.dart                  ← MODIFIED (new fields)
    └── presentation/
        ├── task_card.dart             ← MODIFIED (long-press checkbox popup, ✕ state)
        └── task_detail_sheet.dart     ← MODIFIED (long-press checkbox popup, ✕ state)

features/
└── smart_lists/
    └── domain/
        └── smart_list_query_service.dart  ← MODIFIED (Won't Do query + exclusion from others)

features/
└── navigation/
    └── presentation/
        └── app_drawer.dart            ← MODIFIED (Won't Do smart list entry)
```

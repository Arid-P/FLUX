# FD Phase 4 — Feature 6: Linked Tasks

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Linked Tasks allow the user to define one-directional dependency chains between tasks: Task A blocks Task B, Task B blocks Task C, and so on. A blocked task is visually flagged and cannot be marked complete until all tasks blocking it are completed. This is useful for study sequences where steps must be done in order.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Task Detail Sheet — SCR-02 | `ui_SCR-02_task-detail-sheet.md` |
| Task List View — SCR-01 | `ui_SCR-01_list-view.md` |
| Tasks schema | TRD v2 §5 |
| Task completion — FD-02 | PRD v2 §5 |
| Trash — soft delete | PRD v2 §5, FD-03 |

---

## 3. Dependency Model

### 3.1 Direction

One-directional only. Task A **blocks** Task B means:
- Task B cannot be completed until Task A is completed
- Task A has no awareness of Task B (it is the blocker, not the blocked)

### 3.2 Chain Support

Chains of arbitrary length are supported: A → B → C → D

- A blocks B, B blocks C, C blocks D
- D cannot be completed until C is done
- C cannot be completed until B is done
- B cannot be completed until A is done
- A can be completed freely

### 3.3 No Cycles

Circular dependencies are not allowed (A → B → A). FD enforces this at link creation time by checking if the target task already exists anywhere in the source task's dependency chain.

### 3.4 Cross-List Links

Tasks in different lists can be linked. Dependency is not scoped to a single list.

### 3.5 No Branching Limit

A task can block multiple tasks (A → B and A → C). A task can also be blocked by multiple tasks (A → C and B → C — both A and B must be completed before C).

---

## 4. Visual Treatment

### 4.1 Task Card in SCR-01

When a task is **blocked** (has incomplete blockers):

| Element | Value |
|---|---|
| Chain icon | Small chain link icon (🔗, 12dp) in metadata row, leftmost position |
| Blocked count | "[N] blocker[s]" label next to icon — e.g. "🔗 2 blockers" |
| Checkbox | Greyed out (opacity 0.4). Tapping shows inline error (see §5.2) |
| Card opacity | Slightly reduced (0.85) to signal non-actionable state |

When a task **blocks others** (is a blocker):

| Element | Value |
|---|---|
| Chain icon | Small chain link icon in metadata row |
| Label | "🔗 Blocks [N]" |
| No opacity reduction | Blocker tasks are fully actionable |

### 4.2 Task Detail Sheet — SCR-02

New section in SCR-02: **Dependencies**

**Blocked by (if any):**
```
Blocked by
┌─────────────────────────────────┐
│ 🔗 [Task title]    [list color] │  ← tappable, opens that task's SCR-02
│    [list name] · [due date]     │
└─────────────────────────────────┘
  + Add blocker
```

**Blocks (if any):**
```
Blocks
┌─────────────────────────────────┐
│ 🔗 [Task title]    [list color] │
│    [list name] · [due date]     │
└─────────────────────────────────┘
  + Add dependent
```

Both sections are collapsible. Hidden entirely if no dependencies exist (not shown as empty sections).

---

## 5. Interactions

### 5.1 Adding a Dependency

**Entry point:** SCR-02 → Dependencies section → "+ Add blocker" or "+ Add dependent"

Opens a task search bottom sheet:
- Search field (searches task titles across all lists)
- Results list: task title + list color dot + list name + due date
- Tapping a result: creates the link
- Cycle detection runs before link is created — if cycle detected: inline error *"Cannot link: would create a circular dependency"*, link not created

### 5.2 Completing a Blocked Task

If user taps the checkbox of a blocked task (in SCR-01 or SCR-02):
- Completion is **blocked**
- Inline snackbar: *"Complete [blocker task name] first"* (if 1 blocker)
- Or: *"[N] tasks must be completed first"* (if multiple blockers)
- Snackbar has an action: *"View"* → opens SCR-02 of the first incomplete blocker

### 5.3 Removing a Dependency

SCR-02 → Dependencies section → long-press a linked task row → *"Remove link"* option.

Or: swipe left on a linked task row in the Dependencies section → delete icon.

### 5.4 Deleting a Task with Dependencies

When a task with dependencies is deleted (soft-deleted to Trash):
- All links involving this task are **removed** — no orphaned dependencies
- Tasks that were blocked by this task become unblocked automatically
- Tasks that this task was blocking: this task is removed from their blocker list

When a task is restored from Trash:
- Links are **not restored** — they were permanently removed on deletion
- Restored task has no dependencies

---

## 6. Data Model

```dart
@freezed
class TaskLink with _$TaskLink {
  const factory TaskLink({
    required int id,
    required int blockerTaskId,    // The task that must be done first
    required int blockedTaskId,    // The task that is waiting
    required DateTime createdAt,
  }) = _TaskLink;
}
```

---

## 7. Database Schema

### 7.1 New Table: task_links

```sql
CREATE TABLE task_links (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  blocker_task_id INTEGER NOT NULL,
  blocked_task_id INTEGER NOT NULL,
  created_at INTEGER NOT NULL,
  FOREIGN KEY (blocker_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
  FOREIGN KEY (blocked_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
  UNIQUE (blocker_task_id, blocked_task_id)
);

CREATE INDEX idx_task_links_blocker ON task_links(blocker_task_id);
CREATE INDEX idx_task_links_blocked ON task_links(blocked_task_id);
```

`ON DELETE CASCADE` ensures links are automatically removed when either task is hard-deleted (purged from Trash).

### 7.2 Soft Delete Handling

On soft delete (`is_trashed = 1`): links are **manually deleted** by FD (not by CASCADE, since the row still exists). This is handled in the `deleteTask` use case.

### 7.3 Cycle Detection Query

```dart
// Before creating link: blockerTaskId → blockedTaskId
// Check if blockedTaskId already reaches blockerTaskId through existing links
Future<bool> wouldCreateCycle(int blockerTaskId, int blockedTaskId) async {
  // BFS/DFS from blockedTaskId following blocker_task_id chains
  // If blockerTaskId is reachable: cycle exists → return true
  final visited = <int>{};
  final queue = [blockedTaskId];
  
  while (queue.isNotEmpty) {
    final current = queue.removeAt(0);
    if (current == blockerTaskId) return true;
    if (visited.contains(current)) continue;
    visited.add(current);
    
    final blockers = await getBlockersOf(current);
    queue.addAll(blockers.map((l) => l.blockerTaskId));
  }
  return false;
}
```

### 7.4 Is Blocked Query

```dart
// Task is blocked if it has at least one incomplete blocker
Future<bool> isTaskBlocked(int taskId) async {
  final result = await db.rawQuery('''
    SELECT COUNT(*) as count FROM task_links tl
    JOIN tasks t ON t.id = tl.blocker_task_id
    WHERE tl.blocked_task_id = ?
      AND t.is_completed = 0
      AND t.is_trashed = 0
  ''', [taskId]);
  return (result.first['count'] as int) > 0;
}
```

---

## 8. Routing

No new routes. Dependency management is inline in SCR-02 via bottom sheets.

---

## 9. Module Boundary

**Owned by:** `tasks/` module (extended from P1)

```
features/
└── tasks/
    ├── data/
    │   ├── task_link_dao.dart               ← NEW
    │   └── task_repository_impl.dart        ← EXTENDED (cycle detection, blocked check)
    ├── domain/
    │   ├── task_link.dart                   ← NEW model
    │   └── use_cases/
    │       ├── link_tasks.dart              ← NEW
    │       ├── unlink_tasks.dart            ← NEW
    │       └── check_task_blocked.dart      ← NEW
    └── presentation/
        ├── task_card.dart                   ← MODIFIED (chain icon + blocked state)
        ├── task_detail_sheet.dart           ← MODIFIED (Dependencies section)
        └── task_link_search_sheet.dart      ← NEW
```

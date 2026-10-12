# FD Phase 3 — Feature 4: Task Templates

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Task Templates are reusable task configurations. The user defines a template once — storing everything a task can have except date and time — and stamps it into new tasks on demand. This eliminates repetitive setup for recurring task types (e.g. a standard "Mock Test" task with a fixed list, priority, description structure, and subtasks).

---

## 2. P1 References

| Reference | Location |
|---|---|
| Task Creation Sheet (SCR-03) | `ui_SCR-03_task-creation-sheet.md` |
| Task entity and fields | TRD v2 §5 (tasks table) |
| FAB behaviour — task creation | PRD v2 §5, SCR-01 |
| Subtasks | PRD v2 §5, FD-05 |
| Rich text description | PRD v2 §5, FD-04 |
| Recurrence rules | PRD v2 §5, FD-07 |

---

## 3. What a Template Stores

A template stores all task fields **except** `task_date`, `start_time`, and `end_time`. The user fills those in when creating from the template.

| Field | Stored in Template | Notes |
|---|---|---|
| Title | ✅ | Can be a base title the user modifies at creation time |
| List | ✅ | If the list is deleted, template falls back to default list |
| Section | ✅ | If section deleted, falls back to no section |
| Priority | ✅ | None / Low / Medium / High |
| Recurrence rule | ✅ | Full recurrence config |
| Reminder | ✅ | Relative reminder (e.g. "30 mins before") — absolute times not stored |
| Estimated duration | ✅ | In minutes |
| Description (rich text) | ✅ | Full markdown-formatted description |
| Subtasks | ✅ | List of subtask titles (all unchecked when stamped) |
| Task date | ❌ | User sets at creation time |
| Start time | ❌ | User sets at creation time |
| End time | ❌ | User sets at creation time |

---

## 4. Access Point

**Entry point:** Long-press on the FAB (Floating Action Button) in SCR-01 (Task List screen).

Long-press FAB → small popup menu appears with two options:
- **"New Task"** (same as normal FAB tap)
- **"New from Template"** → opens Template Picker bottom sheet

This keeps the standard FAB tap behaviour unchanged. Long-press reveals the additional option.

---

## 5. Template Picker

A bottom sheet showing all saved templates.

**Layout:**
- Title: "Choose Template"
- Search field at top (filters by template name)
- Template list: each row shows template name + list color dot + list name + priority indicator (if set)
- "Manage Templates" link at bottom → navigates to Templates Management screen
- Tap any template row → opens SCR-03 (Task Creation Sheet) pre-filled with template values

---

## 6. Creation from Template

When the user selects a template:
1. SCR-03 opens with all template fields pre-filled
2. The title field is pre-filled and focused — user can edit or accept as-is
3. Date, start time, end time fields are empty — user must fill these (or leave untimed)
4. All other fields are pre-filled from template but individually editable
5. Subtasks are pre-populated (all unchecked)
6. User taps submit → task created normally. Template is not modified

---

## 7. Template Management Screen

**Route:** `/settings/templates`  
**Access:** Settings → General → Templates, OR "Manage Templates" link in Template Picker

**Layout:**
- List of all saved templates (name, list, priority chips)
- FAB: "New Template"
- Long-press template row: context menu → Edit / Duplicate / Delete
- Tap template row: opens template detail (read-only with Edit button)
- No hard limit on number of templates (JSON objects are small — storage is negligible)
- Empty state: *"No templates yet. Long-press the + button to create your first task from a template."*

---

## 8. Template Creation / Edit

**Entry:** Templates Management FAB or Edit from long-press

Opens a full-screen template editor. Identical layout to SCR-03 (Task Creation Sheet) with the following differences:
- Top app bar title: "New Template" / "Edit Template"
- Date, start time, end time fields are **absent** (not shown — templates never store time)
- Template name field added at top (separate from task title, required, max 64 chars)
- Submit button label: "Save Template"

All other SCR-03 fields behave identically.

---

## 9. Data Model

```dart
@freezed
class TaskTemplate with _$TaskTemplate {
  const factory TaskTemplate({
    required String id,                    // UUID
    required String name,                  // Template name, max 64 chars
    required String taskTitle,             // Pre-fill value for task title
    String? listId,                        // null = default list
    String? sectionId,
    required TaskPriority priority,        // none / low / medium / high
    String? recurrenceRule,                // JSON string, same as tasks table
    String? reminderOffset,                // e.g. "30m", "1h", "1d" — relative only
    int? estimatedDurationMinutes,
    String? description,                   // Rich text markdown string
    required List<String> subtaskTitles,   // Ordered list
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _TaskTemplate;
}
```

---

## 10. Database Schema

### 10.1 New Table: task_templates

```sql
CREATE TABLE task_templates (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  task_title TEXT NOT NULL DEFAULT '',
  list_id TEXT,
  section_id TEXT,
  priority INTEGER NOT NULL DEFAULT 0,
  recurrence_rule TEXT,
  reminder_offset TEXT,
  estimated_duration_minutes INTEGER,
  description TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

### 10.2 New Table: task_template_subtasks

```sql
CREATE TABLE task_template_subtasks (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  template_id TEXT NOT NULL,
  title TEXT NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (template_id) REFERENCES task_templates(id) ON DELETE CASCADE
);
```

---

## 11. Routing

```dart
GoRoute(
  path: '/settings/templates',
  builder: (_, __) => const TaskTemplatesScreen(),
),
GoRoute(
  path: '/settings/templates/new',
  builder: (_, __) => const TaskTemplateEditorScreen(),
),
GoRoute(
  path: '/settings/templates/:id/edit',
  builder: (_, state) => TaskTemplateEditorScreen(
    templateId: state.pathParameters['id'],
  ),
),
```

---

## 12. Module Boundary

**New module:** `templates/`

```
features/
└── templates/
    ├── data/
    │   ├── task_template_dao.dart
    │   └── template_repository_impl.dart
    ├── domain/
    │   ├── task_template.dart
    │   └── use_cases/
    │       ├── create_template.dart
    │       ├── stamp_template.dart          ← Converts template to task creation prefill
    │       └── delete_template.dart
    └── presentation/
        ├── template_picker_sheet.dart       ← Bottom sheet from FAB long-press
        ├── task_templates_screen.dart       ← Management screen
        └── task_template_editor_screen.dart ← Create / Edit
```

Modifications to existing modules:
- `tasks/presentation/task_list_screen.dart` — FAB long-press handler
- `settings/presentation/` — Templates link under General section

# FD Phase 3 — Feature 6: Notes

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Notes are informational entries that live inside lists and sections alongside tasks. They have a title, a rich-text description, and a date — but no checkbox, no completion state, no reminder, no recurrence, and no time. They exist purely to store and display information in context with related tasks.

A note is not a task. It does not affect smart lists, completion counts, or any task-based statistics.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Task List View — SCR-01 | `ui_SCR-01_list-view.md` |
| Task Detail Sheet — SCR-02 | `ui_SCR-02_task-detail-sheet.md` |
| Task Creation Sheet — SCR-03 | `ui_SCR-03_task-creation-sheet.md` |
| Rich text description | PRD v2 §5, FD-04 |
| Lists and sections | PRD v2 §5, FD-09, FD-10 |
| Side drawer — list navigation | `ui_SCR-08_side-drawer.md` |
| Trash — soft delete | PRD v2 §5, FD-03 |

---

## 3. What a Note Is

| Property | Value |
|---|---|
| Title | Required, plain text, max 255 chars |
| Description | Optional, rich text (same markdown rendering as task description — PRD v2 FD-04) |
| Date | Required. Date only — no time component |
| List | Required. Assigned to exactly one list |
| Section | Optional. Assigned to a section within the list |
| Checkbox | ❌ None |
| Completion state | ❌ None |
| Priority | ❌ None |
| Reminder | ❌ None |
| Recurrence | ❌ None |
| Start time / End time | ❌ None |
| Subtasks | ❌ None |
| Trash / soft delete | ✅ Same as tasks — `is_trashed` flag, 30-day auto-purge |

---

## 4. Visual Treatment in List View (SCR-01)

Notes appear inline within their list/section, sorted by date alongside tasks.

### 4.1 Note Card

Identical to a task card with the following differences:

| Element | Task Card | Note Card |
|---|---|---|
| Checkbox | ✅ Present | ❌ Absent |
| Left edge | List color dot or nothing | Small note icon (document/page icon, 16dp, `ThemeTokens.secondaryText`) replaces the checkbox position |
| Title | Task title | Note title |
| Metadata row | Date, reminder, recurrence chips | Date chip only |
| Right side | List color dot + list name | List color dot + list name (identical) |
| Swipe right | Complete | ❌ No action |
| Swipe left | Delete (trash) | Delete (trash) — same as task |

### 4.2 Note Icon

The note icon occupies the same left-side position as the task checkbox, maintaining consistent left-edge alignment across all card types in the list. Size: 20dp visual, 40dp touch target (same as checkbox).

Tapping the note icon has no action — it is decorative/semantic only.

### 4.3 Distinguishability

The absence of a checkbox and the presence of the note icon is sufficient visual distinction. No additional background color, border, or badge is applied to note cards.

---

## 5. Note Detail Sheet

Tap a note card → opens Note Detail Sheet (modal bottom sheet, same pattern as SCR-02).

### 5.1 Layout

```
─────────────────────────────────
  [Note icon]  [Title — editable inline]     [⋮]
  
  📅 [Date chip — tappable]
  📁 [List chip — tappable]  [Section chip if set]
  
  ─────────────────────────────────
  [Rich text description — editable]
  [Rich text toolbar when focused]
─────────────────────────────────
```

### 5.2 Overflow Menu (⋮)
- Duplicate note
- Move to list
- Delete (soft-delete to Trash)

### 5.3 Editing
All fields editable inline — same pattern as SCR-02. No separate edit mode. Changes auto-saved on sheet close.

---

## 6. Note Creation

### 6.1 Entry Points

**From SCR-01 (List View):**
FAB long-press → popup menu gains a third option:
- "New Task"
- "New from Template"
- **"New Note"** ← new in P3

**From SCR-03 equivalent:**
A Note Creation Sheet opens (same structure as Task Creation Sheet but with note-specific fields only — title, description, date, list, section).

### 6.2 Note Creation Sheet Fields

| Field | Required | Notes |
|---|---|---|
| Title | Yes | Plain text, max 255 chars |
| Date | Yes | Date picker. Defaults to today |
| List | Yes | Pre-filled with currently active list |
| Section | No | Dropdown of sections in selected list |
| Description | No | Rich text editor, same as task description |

No time, no reminder, no recurrence, no priority, no subtasks.

---

## 7. Smart Lists Behaviour

Notes are **excluded** from all smart lists (Today, Tomorrow, Upcoming, All, Completed, Trash — except Trash).

- **Today smart list:** shows only tasks due today. Notes not included
- **Trash smart list:** notes soft-deleted to trash appear in Trash alongside trashed tasks
- **All smart list:** tasks only, no notes
- **Search:** notes ARE included in search results (title + description text search)

Notes only appear when navigating directly to their assigned list/section via the side drawer.

---

## 8. Notes and Calendar View

Notes with a date do **not** appear in SCR-04 (Calendar View). Calendar View is time-block oriented — notes have no time component and do not render as blocks. This is consistent with untimed tasks which also do not appear on the timeline.

---

## 9. Data Model

```dart
@freezed
class Note with _$Note {
  const factory Note({
    required int id,
    required String title,
    String? description,          // Rich text markdown string
    required DateTime date,       // Date only — time component ignored
    required int listId,
    int? sectionId,
    required bool isTrashed,
    DateTime? trashedAt,
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _Note;
}
```

---

## 10. Database Schema

### 10.1 New Table: notes

```sql
CREATE TABLE notes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  description TEXT,
  date INTEGER NOT NULL,          -- Unix ms for date (time component = midnight UTC)
  list_id INTEGER NOT NULL,
  section_id INTEGER,
  is_trashed INTEGER NOT NULL DEFAULT 0,
  trashed_at INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
  FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE SET NULL
);
```

---

## 11. Routing

```dart
GoRoute(
  path: '/note/:id',
  builder: (_, state) => NoteDetailSheet(
    noteId: int.parse(state.pathParameters['id']!),
  ),
),
GoRoute(
  path: '/note/create',
  builder: (_, __) => const NoteCreationSheet(),
),
```

---

## 12. Trash Integration

Notes follow the exact same soft-delete and restore flow as tasks (PRD v2 §5, FD-03):
- Delete → `is_trashed = 1`, `trashed_at = now()`
- Auto-purge after 30 days (same WorkManager task as task purge)
- SCR-12 (Trash Screen) shows trashed notes alongside trashed tasks
- Note cards in Trash: same visual as list view note card (no checkbox, note icon)
- Restore: swipe right or tap restore icon — restores to original list/section
- If original list was deleted: restores to default list (same rule as tasks — PRD v2 §2.2)

---

## 13. Module Boundary

**New module:** `notes/`

```
features/
└── notes/
    ├── data/
    │   ├── note_dao.dart
    │   └── note_repository_impl.dart
    ├── domain/
    │   ├── note.dart
    │   └── use_cases/
    │       ├── create_note.dart
    │       ├── update_note.dart
    │       └── delete_note.dart
    └── presentation/
        ├── note_card.dart               ← Reused in SCR-01 and SCR-12
        ├── note_detail_sheet.dart
        └── note_creation_sheet.dart
```

Modifications to existing P1 modules:
- `tasks/presentation/task_list_screen.dart` — FAB long-press third option, note cards rendered inline
- `smart_lists/` — notes excluded from all smart list queries
- `tasks/presentation/trash_screen.dart` (SCR-12) — note cards in trash
- `core/search/` — notes included in search results

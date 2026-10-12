# FD Phase 3 — Feature 3: Data Import

**Version:** 1.0  
**Phase:** 3  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Data Import allows the user to import folders, lists, tasks, subtasks, and notes into FluxDone from a structured JSON file. This is the primary migration path from TickTick (or any other task manager) into FD. The user manually creates the JSON file following FD's defined schema, imports it, reviews a summary, and confirms.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Folders, Lists, Sections schema | TRD v2 §5 |
| Tasks schema | TRD v2 §5 |
| Subtasks schema | TRD v2 §5 |
| Notes schema | FD P3 F6 — Notes |
| Trash — soft delete | PRD v2 §5, FD-03 |
| Settings screen | `ui_SCR-09_settings.md` |
| Default list fallback | PRD v2 §2.2 |

---

## 3. Import JSON Schema

### 3.1 File Structure

```json
{
  "_comment": "FluxDone Import File. See schema documentation at: [link]. This file contains folders, lists, tasks, subtasks, and notes for import into FluxDone. Each entity has a unique 'id' (string) and a 'name'/'title' field. Tasks and notes are nested under their parent list. Subtasks are nested under their parent task. Dates are ISO 8601 strings (YYYY-MM-DD). Times are HH:mm (24-hour). This file can be read by AI assistants to understand your task history.",
  "export_version": "1.0",
  "exported_at": "2026-03-21T00:00:00Z",
  "folders": [
    {
      "id": "folder-001",
      "name": "Math Olympiad",
      "sort_order": 0,
      "lists": [
        {
          "id": "list-001",
          "name": "Geometry",
          "color": "#1565C0",
          "sort_order": 0,
          "sections": [
            {
              "id": "section-001",
              "name": "Triangles",
              "sort_order": 0
            }
          ],
          "tasks": [
            {
              "id": "task-001",
              "title": "Solve Exercise 3.4",
              "description": "Complete all parts of exercise 3.4 from the textbook.",
              "date": "2026-03-22",
              "start_time": "15:00",
              "end_time": "17:00",
              "priority": 2,
              "is_completed": false,
              "completed_at": null,
              "recurrence_rule": null,
              "reminder_offset": "30m",
              "estimated_duration_minutes": 120,
              "section_id": "section-001",
              "subtasks": [
                {
                  "id": "subtask-001",
                  "title": "Part (a)",
                  "is_completed": false,
                  "sort_order": 0
                }
              ]
            }
          ],
          "notes": [
            {
              "id": "note-001",
              "title": "Key Theorems",
              "description": "## Pythagoras\n...",
              "date": "2026-03-20",
              "section_id": null
            }
          ]
        }
      ]
    }
  ]
}
```

### 3.2 Field Definitions

**Folder:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String | Yes | User-defined unique ID. Used for conflict matching |
| `name` | String | Yes | Folder name |
| `sort_order` | Integer | No | Default 0 |

**List:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String | Yes | User-defined unique ID |
| `name` | String | Yes | List name |
| `color` | String | No | Hex color `#RRGGBB`. Default: `#2E7D32` |
| `sort_order` | Integer | No | Default 0 |

**Section:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String | Yes | |
| `name` | String | Yes | |
| `sort_order` | Integer | No | Default 0 |

**Task:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String | Yes | |
| `title` | String | Yes | |
| `description` | String | No | Rich text markdown |
| `date` | String | No | ISO date `YYYY-MM-DD` |
| `start_time` | String | No | `HH:mm` 24h |
| `end_time` | String | No | `HH:mm` 24h |
| `priority` | Integer | No | 0=none, 1=low, 2=medium, 3=high |
| `is_completed` | Boolean | No | Default false |
| `completed_at` | String | No | ISO datetime or null |
| `recurrence_rule` | String | No | JSON string matching FD recurrence format |
| `reminder_offset` | String | No | e.g. `"30m"`, `"1h"`, `"1d"` |
| `estimated_duration_minutes` | Integer | No | |
| `section_id` | String | No | Must match a section `id` in the same list |

**Subtask:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String | Yes | |
| `title` | String | Yes | |
| `is_completed` | Boolean | No | Default false |
| `sort_order` | Integer | No | Default 0 |

**Note:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `id` | String | Yes | |
| `title` | String | Yes | |
| `description` | String | No | Rich text markdown |
| `date` | String | Yes | ISO date `YYYY-MM-DD` |
| `section_id` | String | No | |

---

## 4. Validation Rules

Before showing the summary screen, FD validates the entire file:

| Rule | Error behaviour |
|---|---|
| Valid JSON syntax | Hard fail — import blocked, error shown |
| `export_version` present | Warning only — import proceeds |
| All required fields present per entity | Hard fail per entity — entity skipped, logged in summary |
| `color` is valid `#RRGGBB` hex | Fallback to `#2E7D32`, warning in summary |
| `date` is valid ISO date | Entity skipped, logged in summary |
| `start_time` / `end_time` valid `HH:mm` | Field ignored, warning in summary |
| `section_id` references valid section in same list | Field ignored, warning in summary |
| File size | Max 10 MB — hard fail above this |

---

## 5. Conflict Resolution

### 5.1 Folders and Lists — Match by Name

- If a folder with the same name already exists in FD: imported lists are added into the existing folder
- If a list with the same name already exists in the matched folder: imported tasks and notes are added into the existing list
- New sections in an existing list are created if they don't exist by name
- Existing sections matched by name — tasks assigned to them accordingly

### 5.2 Tasks and Notes — Side-by-Side Conflict Resolution

A conflict occurs when an imported task has the same `id` as an existing FD task.

For each conflict:
- Full-screen conflict resolution card shown (part of the summary flow)
- Left side: existing FD task details
- Right side: imported task details
- Three options: **Keep existing** / **Use imported** / **Keep both** (imports as a new task with a new auto-generated ID)

Same logic applies to notes with conflicting IDs.

---

## 6. Import Flow

### 6.1 Entry Point

Settings → Data → Import

### 6.2 Step 1 — File Selection

- Android file picker opens, filtered to `.json` files
- User selects file
- FD reads and validates (§4)
- If hard validation fail: error bottom sheet with specific reason. Import blocked

### 6.3 Step 2 — Summary Screen

Full-screen summary before any data is written:

```
─────────────────────────────────
  Import Summary

  📁 3 folders
  📋 12 lists
  ✅ 847 tasks (23 completed)
  📝 14 notes
  ⚠️  3 conflicts requiring resolution
  ⚠️  2 warnings (see details)

  [Resolve Conflicts →]        ← only shown if conflicts exist
  
  [Cancel]        [Import →]   ← Import disabled until conflicts resolved
─────────────────────────────────
```

### 6.4 Step 3 — Conflict Resolution (if any)

Each conflict shown as a card — user picks Keep existing / Use imported / Keep both. Progress indicator: "2 of 3 resolved."

### 6.5 Step 4 — Import Execution

On confirm:
- Full-screen non-dismissible progress overlay (same pattern as Google Drive restore — SCR-13)
- Progress: "Importing folders… Importing lists… Importing tasks…"
- On completion: success snackbar — "Import complete. 847 tasks imported."
- App navigates to side drawer (lists visible immediately)

### 6.6 Failure During Import

If a write error occurs mid-import:
- Import stops
- All successfully written data is rolled back (SQLite transaction — atomic import)
- Error screen shown with option to retry

---

## 7. Module Boundary

**New module:** `data_portability/`

```
features/
└── data_portability/
    ├── data/
    │   ├── import_repository_impl.dart
    │   └── export_repository_impl.dart    ← shared module with F4
    ├── domain/
    │   ├── import_file_schema.dart        ← JSON model definitions
    │   └── use_cases/
    │       ├── validate_import_file.dart
    │       ├── parse_import_file.dart
    │       └── execute_import.dart
    └── presentation/
        ├── import_screen.dart
        ├── import_summary_screen.dart
        └── import_conflict_card.dart
```

Modifications to existing modules:
- `settings/presentation/` — new Data section with Import entry point

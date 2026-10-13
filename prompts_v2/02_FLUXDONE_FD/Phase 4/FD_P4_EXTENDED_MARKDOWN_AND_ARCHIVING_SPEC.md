# 📝 Topic FD-P4-3: Extended Academic Markdown & Large Database Archiving Specification

**Document ID:** `FD_P4_EXTENDED_MARKDOWN_AND_ARCHIVING_SPEC`  
**Phase:** Phase 4 (Cloud Sync, Cross-Device Ecosystem & Advanced Notes)  
**Target Package:** `com.flux.done.notes.markdown` & `com.flux.done.database.archive`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Compose Multiplatform 1.6+ + KaTeX Vector Parser + Room 2.7+ KMP)  
**Target Platforms:** Android Phone & Tablet (`com.fluxdone`), Ubuntu Linux Workstation (`com.fluxdone.desktop`)  
**Prerequisites:** [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) & [`FD_P3_SHARED_DATABASE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%203/FD_P3_SHARED_DATABASE_SPEC.md)  

---

## 1. Overview & Architectural Philosophy

Intensive academic studying—such as preparing for Olympiad geometry, advanced calculus, or algorithm competitions—demands notes capabilities far beyond simple plain text. When reviewing problem sheets, Ari must write and render mathematical equations, reference structured tables, and format key theorems with callout boxes.

Simultaneously, as thousands of tasks and subtasks accumulate over months of preparation, the active database must remain blazing fast. 

**Topic FD-P4-3 establishes:**
1. **Extended Academic Markdown Engine:** Rich rendering of LaTeX math (`$...$` and `$$...$$`), Markdown tables, and GitHub-style callouts (`> [!NOTE]`, `> [!TIP]`, `> [!WARNING]`).
2. **Large Database Archiving Engine:** Background migration of tasks completed $>90$ days ago into a dedicated archive partition (`archived_tasks`), ensuring the active database stays lean and responsive.

---

## 2. Extended Markdown Syntax & KaTeX Math Engine

### 2.1 Supported Markdown Syntax Matrix

| Syntax Feature | Markdown Syntax Example | Visual Rendering Contract |
|---|---|---|
| **Inline LaTeX Math** | `$E = mc^2$` or `$\angle ABC = 90^\circ$` | High-resolution vector math span rendered inline within body text. |
| **Display Block Math**| `$$\sum_{i=1}^n i = \frac{n(n+1)}{2}$$` | Centered display equation block with dark-mode vector rendering. |
| **Markdown Tables** | `\| A \| B \|`<br>`\|---|---\|`<br>`\| 1 \| 2 \|` | Alternating zebra-striped rows, border `#415A77`, horizontal scroll on mobile. |
| **Callout: Note** | `> [!NOTE]`<br>`> Definition here` | Blue accent border `#1565C0`, light blue tinted background, info icon. |
| **Callout: Tip** | `> [!TIP]`<br>`> Short lemma proof` | Green accent border `#2E7D32`, green tinted background, bulb icon. |
| **Callout: Warning**| `> [!WARNING]`<br>`> Edge case failure` | Amber border `#F57C00`, warning icon. |
| **Callout: Caution**| `> [!CAUTION]`<br>`> Formula pitfall` | Red border `#D90429`, shield alert icon. |
| **Code Blocks** | ` ```kotlin ... ``` ` | Monospace syntax-highlighted block with a 1-tap `[ Copy ]` button. |

### 2.2 Vector Math Parser Architecture (`KaTeXMathRenderer.kt`)

Rather than embedding heavyweight webviews on mobile and desktop, FluxDone utilizes a lightweight client-side KaTeX AST parser:
* **Android:** Renders LaTeX nodes into native Android `Spannable` / Jetpack Compose `AnnotatedString` using canvas path drawing for mathematical symbols ($\int, \sum, \sqrt{}, \frac{a}{b}$).
* **Desktop (Compose Multiplatform):** Renders math blocks directly onto Skiko canvas surfaces using anti-aliased font vector glyphs with full dark-mode palette compliance (`#E0E1DD` math text, `#1B263B` surface).

```kotlin
package com.flux.done.notes.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
expect fun RenderMarkdownContent(
    markdownText: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFFE0E1DD),
    onLinkClick: (String) -> Unit
)
```

---

## 3. Large Database Archiving Engine (>90 Days)

### 3.1 Performance Benchmark Thresholds

To maintain instantaneous responsiveness as Ari's database scales to $10,000+$ historical tasks:

| Performance Metric | Active Database Target | Degradation Threshold (Trigger Archive) |
|---|---|---|
| **Application Cold Start** | $< 1.0\text{ second}$ | $> 1.5\text{ seconds}$ |
| **Timeline Scroll Framerate** | Solid 60 FPS ($16.6\text{ms}$ budget) | Dropped frames during multi-day scroll |
| **Task Check-Off Latency** | $< 16\text{ms}$ (1 frame instant check) | $> 30\text{ms}$ |
| **Memory Footprint** | $< 45\text{ MB}$ JVM / Heap | $> 90\text{ MB}$ |

### 3.2 Dedicated Archive Table Schema (`archived_tasks`)

Completed tasks older than 90 days are moved from `tasks` to `archived_tasks` within `fluxdone.db` (Option A):

```sql
CREATE TABLE IF NOT EXISTS archived_tasks (
    id TEXT PRIMARY KEY NOT NULL,
    user_id TEXT NOT NULL,
    folder_id TEXT,
    list_id TEXT NOT NULL,
    section_id TEXT,
    title TEXT NOT NULL,
    notes TEXT,
    priority INTEGER NOT NULL,
    due_date INTEGER,
    start_time INTEGER,
    end_time INTEGER,
    completed_at INTEGER NOT NULL,
    archived_at_utc INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    domain_tag TEXT,
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_archived_completed_at ON archived_tasks(completed_at);
CREATE INDEX IF NOT EXISTS idx_archived_title ON archived_tasks(title);
```

### 3.3 Background Archiving Worker (`DatabaseArchivingWorker.kt`)

Runs weekly at 03:00 AM (or on idle workstation time) using WorkManager on Android and a scheduled Coroutine timer on Desktop Linux:

```kotlin
package com.flux.done.database.archive

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ArchiveDao {

    @Transaction
    suspend fun archiveOldCompletedTasks(cutoffTimestampMs: Long): Int {
        // 1. Copy tasks completed >90 days ago into archived_tasks
        val count = copyToArchive(cutoffTimestampMs)
        if (count > 0) {
            // 2. Remove archived tasks from primary active table
            deleteArchivedFromActive(cutoffTimestampMs)
            // 3. Clean up orphaned subtasks
            deleteOrphanedSubtasks()
        }
        return count
    }

    @Query("""
        INSERT OR REPLACE INTO archived_tasks (
            id, user_id, folder_id, list_id, section_id, title, notes,
            priority, due_date, start_time, end_time, completed_at,
            archived_at_utc, domain_tag, metadata_json
        )
        SELECT 
            id, user_id, folder_id, list_id, section_id, title, notes,
            priority, due_date, start_time, end_time, completed_at,
            (strftime('%s', 'now') * 1000), domain_tag, field_timestamps_json
        FROM tasks
        WHERE is_completed = 1 AND completed_at < :cutoffTimestampMs
    """)
    suspend fun copyToArchive(cutoffTimestampMs: Long): Int

    @Query("DELETE FROM tasks WHERE is_completed = 1 AND completed_at < :cutoffTimestampMs")
    suspend fun deleteArchivedFromActive(cutoffTimestampMs: Long): Int

    @Query("DELETE FROM subtasks WHERE task_id NOT IN (SELECT id FROM tasks)")
    suspend fun deleteOrphanedSubtasks(): Int
}
```

---

## 4. Archive Search & Restoration

Archived tasks are hidden from daily task streams and calendar views to keep them clutter-free, but they are fully searchable:

```
+-----------------------------------------------------------------------------------+
|  🔍 UNIVERSAL SEARCH                                                              |
+-----------------------------------------------------------------------------------+
| [ Search: "Incenter Proof"                                                      ] |
+-----------------------------------------------------------------------------------+
| Filters: [ All ] [ Active Tasks ] [ 📦 Include Archive (Active) ]                 |
+-----------------------------------------------------------------------------------+
| ACTIVE RESULTS (0)                                                                |
| No active tasks found matching "Incenter Proof".                                  |
|                                                                                   |
| ARCHIVE RESULTS (1)                                                               |
| 📦 [✓] Solve 5 Incenter Problems    Completed: 14 Jul 2026   [ ↩️ Restore ]      |
+-----------------------------------------------------------------------------------+
```

* **1-Tap Restoration:** Clicking `[ ↩️ Restore ]` moves the archived task back to `tasks` as an active or completed task, restoring its checklist subtasks.

---

## 5. Verification & Testing Contracts

1. **LaTeX Math Rendering Test:** Input note with `$\alpha + \beta = 180^\circ$` and `$$\sqrt{x^2 + y^2}$$`. Verify that KaTeX parser outputs vector equation spans without throwing parsing exceptions.
2. **Callout Visual Hierarchy Test:** Render notes with `> [!TIP]`. Verify left border has 3dp stroke `#2E7D32` and background has 10% opacity green tint.
3. **90-Day Archiving Threshold Test:** Insert 10 tasks completed 100 days ago and 5 tasks completed 10 days ago. Execute `ArchiveDao.archiveOldCompletedTasks()`. Verify that exactly 10 tasks are moved to `archived_tasks`, and the 5 recent tasks remain in `tasks`.
4. **Performance Benchmark Test:** Populate `archived_tasks` with 20,000 records. Execute cold start and timeline scroll. Verify cold start remains $<1.0\text{s}$ and 60 FPS is maintained.

# 📁 Topic FD-P1-1: Lists, Folders, Sections & Hierarchy Specification

**Document ID:** `FD_P1_LISTS_FOLDERS_DOMAINS_SPEC`  
**Phase:** Phase 1 (Core Task, Hierarchy & Habit Foundation)  
**Target Package:** `com.flux.done.features.hierarchy`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + StateFlow)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

FluxDone is designed as an ultra-fast, offline-first academic task manager replacing TickTick. In an intensive study workflow, tasks cannot simply sit in a single flat backlog; they require structured organization that adapts seamlessly from high-level domains down to daily sub-topics.

### 1.1 The Organizational Hierarchy
The hierarchy follows a strict, intuitive 4-tier tree:

```
Folders → Lists → Sections → Tasks
```

* **Folders:** Top-level collapsible containers in the navigation drawer (e.g. *Olympiad Prep*, *School*, *Projects*).
* **Lists:** Primary organizational units. A list can belong to a folder or exist as an independent unfiled list (e.g. *Geometry*, *Physics*, *Personal*). Each list holds a custom hex color.
* **Sections:** Optional internal grouping dividers inside a list (e.g. *Triangles*, *Circles*, *Coordinate Geometry* inside *Geometry*).
* **Tasks:** Discrete actionable study items residing inside a list (and optionally inside a specific section).

---

## 2. Default First-Launch Seeding (Zero Bloat Tutorial Structure)

To avoid cognitive overload while demonstrating full capability, FluxDone boots on first launch with **zero bloated lists** and a minimal, purposeful tutorial layout:

```
Navigation Drawer (First Launch):
├── 📥 Inbox (Default Fallback List — Unfiled)
└── 📁 Tutorial & Guides (Folder)
    ├── 📋 Getting Started (Sectioned List)
    │   ├── ▼ Basics
    │   │   ├── ☐ Tap checkbox to complete task
    │   │   └── ☐ Swipe right to reschedule for tomorrow
    │   └── ▼ Advanced
    │       └── ☐ Tap Calendar tab to time-block on the timeline
    └── 📝 Quick Notes (Unsectioned List)
        └── ☐ A flat list without sections for quick, simple tasks
```

* **Inbox (`is_default = true`):** Permanent default container for unfiled quick-add tasks.
* **Getting Started (Sectioned):** Demonstrates collapsible sticky headers, task grouping, and core gestures.
* **Quick Notes (Unsectioned):** Demonstrates the ultra-clean, minimal flat task view.
* **Full User Ownership:** Once the user understands the layout, they can freely edit, rename, or delete the tutorial folder and its lists.

---

## 3. Sectioned vs Unsectioned List Modes

A central principle of FluxDone is that **sections are optional**:

### 3.1 Unsectioned View (Default Flat Mode)
* Used for simple lists (e.g. *Reading List*, *Quick Errand*), where sections would be unnecessary visual clutter.
* Tasks render directly as a flat vertical list of task cards with zero section headers.
* A subtle `[ + Add Section ]` button appears at the very bottom of the task list (or inside the top overflow menu).

### 3.2 Sectioned View (Grouped Mode)
* Active as soon as at least one section is created in the list.
* **Sticky Section Headers:** Rendered with semibold typography, task count, and collapse caret:
  ```
  [ ▼ PHYSICS  (4) ]                                     [ + ]
  ```
* Tapping the caret collapses or expands that section's task cards.
* Tapping `[ + ]` on a section header immediately opens Quick Task Creation pre-assigned to that section.
* Tasks that do not belong to any section sit cleanly at the top in a default unsectioned group.

---

## 4. Deletion Architecture & Trash Rules

Data loss must be strictly prevented, but list and section management must remain fast and low-friction:

### 4.1 Deleting a List
1. User long-presses a list in the side drawer or taps **Delete List** in list settings.
2. **Confirmation Dialog:**
   ```
   +-------------------------------------------------------+
   |  Delete List "Algebra"?                               |
   +-------------------------------------------------------+
   |  This will move 12 tasks inside to Trash.             |
   |  You can restore them within 30 days.                 |
   |                                                       |
   |  +-------------------------------------------------+  |
   |  |        [ Delete & Move to Trash ]               |  |
   |  +-------------------------------------------------+  |
   |  |                [ Cancel ]                       |  |
   |  +-------------------------------------------------+  |
   +-------------------------------------------------------+
   ```
3. **Behavior:**
   * The list record is removed from `task_lists`.
   * All tasks inside are marked as `is_deleted = 1` and moved to **Trash (SCR-12)**.
   * Tasks remain in Trash for **30 days by default** (configurable in Settings) before permanent auto-purge.
   * The tasks are **never** immediately wiped from SQLite.

### 4.2 Deleting a Section (Crucial Safety Rule)
* When a user deletes a section:
  * **All tasks within that section are moved to Trash** (`is_deleted = 1`).
  * A confirmation dialog explicitly clarifies:  
    *"Delete section 'Physics'? All 5 tasks in this section will be moved to Trash."*
  * This matches list deletion behavior and prevents accidental orphaned tasks.

### 4.3 Deleting a Folder
When deleting a folder, the user is offered two clear choices:
1. **Ungroup Lists (Default):** Deletes the folder container; all member lists become unfiled root lists (`folder_id = NULL`). Zero tasks are deleted.
2. **Delete Folder and Contents:** Moves all tasks across all member lists into Trash, and deletes the lists and folder.

### 4.4 The "Last Remaining List" Invariant
* The app requires at least one list to exist so incoming tasks (e.g. quick capture, IPC focus sync) always have a valid target.
* The default **Inbox** cannot be deleted if it is the only list left.
* If a user tries to delete the final list, a modal informs them:  
  *"Cannot delete the only remaining list. Create another list first."*

---

## 5. Curated Color Palette & Custom Hex Picker

Each list has an identifiable color used for:
* Circular color swatch in the side drawer (12dp circle).
* 3dp left border on task cards in aggregated views (Today, Tomorrow, All).
* Background fill color of calendar timeline time-blocks (SCR-04).

### 5.1 The 12 Signature Preset Swatches
A curated 12-color grid presented in a 2×6 or 3×4 swatch picker:

| Color Swatch | Hex Code | Visual Tone |
|---|---|---|
| Swatch 1 | `#2E7D32` | Deep Forest Green |
| Swatch 2 | `#1565C0` | Royal Indigo Blue |
| Swatch 3 | `#43A047` | Fresh Leaf Green |
| Swatch 4 | `#FB8C00` | Warm Amber Orange |
| Swatch 5 | `#E64A19` | Bold Crimson Orange |
| Swatch 6 | `#E53935` | Vibrant Academic Red |
| Swatch 7 | `#576481` | Slate Indigo |
| Swatch 8 | `#7B1FA2` | Deep Vivid Purple |
| Swatch 9 | `#00838F` | Deep Marine Teal |
| Swatch 10 | `#F57F17` | Golden Mustard |
| Swatch 11 | `#5E35B1` | Violet Cobalt |
| Swatch 12 | `#546E7A` | Neutral Blue Grey |

### 5.2 Custom Hex Input Field
* Below the 12 swatches, a clean text input with `#` prefix allows manual entry of any 6-character hex code.
* Live 32dp preview circle updates dynamically as the user types.

---

## 6. Room Database Schema

### 6.1 Entity: `FolderEntity`
```kotlin
package com.flux.done.features.hierarchy.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_expanded")
    val isExpanded: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 6.2 Entity: `TaskListEntity`
```kotlin
package com.flux.done.features.hierarchy.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_lists",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("folder_id"),
        Index("sort_order")
    ]
)
data class TaskListEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "folder_id")
    val folderId: String? = null, // Nullable: allows unfiled lists

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String, // e.g. "2E7D32" (stored without '#')

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false, // Exactly one list has isDefault = true

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 6.3 Entity: `SectionEntity`
```kotlin
package com.flux.done.features.hierarchy.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("list_id"),
        Index("sort_order")
    ]
)
data class SectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "list_id")
    val listId: String, // FK to task_lists

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_collapsed")
    val isCollapsed: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

---

## 7. DAO Interfaces & Relational Queries

### 7.1 Relational Models
```kotlin
data class FolderWithLists(
    @Embedded val folder: FolderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "folder_id"
    )
    val lists: List<TaskListEntity>
)

data class ListWithSectionsAndTaskCount(
    val listId: String,
    val listName: String,
    val colorHex: String,
    val folderId: String?,
    val isDefault: Boolean,
    val activeTaskCount: Int
)
```

### 7.2 Hierarchy DAO (`HierarchyDao.kt`)
```kotlin
package com.flux.done.features.hierarchy.data.dao

import androidx.room.*
import com.flux.done.features.hierarchy.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HierarchyDao {

    // --- Folders ---
    @Query("SELECT * FROM folders ORDER BY sort_order ASC, created_at ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Transaction
    @Query("SELECT * FROM folders ORDER BY sort_order ASC")
    fun observeFoldersWithLists(): Flow<List<FolderWithLists>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: String)

    @Query("UPDATE folders SET is_expanded = :isExpanded WHERE id = :folderId")
    suspend fun setFolderExpanded(folderId: String, isExpanded: Boolean)

    // --- Lists ---
    @Query("SELECT * FROM task_lists WHERE folder_id IS NULL ORDER BY sort_order ASC")
    fun observeUnfiledLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :listId")
    suspend fun getListById(listId: String): TaskListEntity?

    @Query("SELECT * FROM task_lists WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultList(): TaskListEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TaskListEntity)

    @Update
    suspend fun updateList(list: TaskListEntity)

    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteList(listId: String)

    @Query("SELECT COUNT(*) FROM task_lists")
    suspend fun getTotalListCount(): Int

    // --- Sections ---
    @Query("SELECT * FROM sections WHERE list_id = :listId ORDER BY sort_order ASC")
    fun observeSectionsForList(listId: String): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Query("DELETE FROM sections WHERE id = :sectionId")
    suspend fun deleteSection(sectionId: String)

    @Query("UPDATE sections SET is_collapsed = :isCollapsed WHERE id = :sectionId")
    suspend fun setSectionCollapsed(sectionId: String, isCollapsed: Boolean)
}
```

---

## 8. Edge Cases & Defensive Engineering Guardrails

To prevent ambiguity or subtle bugs during implementation, the following edge cases are strictly codified:

### Edge Case 1: Restoring a Task whose Parent Section or List was Deleted
* **Scenario:** A user deletes a section or list (moving its tasks to Trash). Later, they tap "Restore" on one of those tasks in Trash.
* **Invariant:**
  * If the task's original section no longer exists, but the list exists: the task restores to the **unsectioned group** of that list.
  * If the task's original list no longer exists: the task restores to the **default Inbox list**.
  * A snackbar informs the user: *"Restored to Inbox (original list was deleted)"*.

### Edge Case 2: Drag-and-Drop Reordering Collisions
* **Scenario:** The user reorders lists within a folder or sections within a list.
* **Invariant:** Reordering must not use fractional floats (`sort_order = 1.5`). The repository executes a batch re-indexing transaction updating integer `sort_order` (`0, 1, 2, 3...`) in an atomic Room SQLite transaction:
  ```kotlin
  @Transaction
  suspend fun reorderLists(orderedListIds: List<String>) {
      orderedListIds.forEachIndexed { index, id ->
          updateListSortOrder(id, index)
      }
  }
  ```

### Edge Case 3: Moving a Task from a Sectioned List to an Unsectioned List
* **Scenario:** A task in section *"Physics"* is moved to an unsectioned list *"Personal"*.
* **Invariant:** The repository automatically sets `task.section_id = null`. Setting a `section_id` belonging to a different list is prevented by foreign key constraints.

### Edge Case 4: Side Drawer Folder Collapse Persistence
* **Scenario:** The user collapses a folder with 10 lists and closes the app.
* **Invariant:** The collapsed state must not reset on app restart. The `is_expanded` column on `FolderEntity` persists this directly in SQLite.

---

## 9. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class HierarchyRepositoryTest {

    private lateinit var db: FluxDoneDatabase
    private lateinit var hierarchyDao: HierarchyDao
    private lateinit var taskDao: TaskDao
    private lateinit var repository: HierarchyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxDoneDatabase::class.java).build()
        hierarchyDao = db.hierarchyDao()
        taskDao = db.taskDao()
        repository = HierarchyRepositoryImpl(hierarchyDao, taskDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun deletingSection_movesAllContainedTasksToTrash() = runTest {
        // Given: A list with 1 section containing 3 tasks
        val list = createSampleList("list-1", "Physics")
        val section = createSampleSection("sec-1", "list-1", "Mechanics")
        hierarchyDao.insertList(list)
        hierarchyDao.insertSection(section)
        taskDao.insertTasks(listOf(
            createSampleTask("t1", "list-1", "sec-1"),
            createSampleTask("t2", "list-1", "sec-1"),
            createSampleTask("t3", "list-1", "sec-1")
        ))

        // When: Section is deleted
        repository.deleteSection("sec-1")

        // Then: Section is gone from SQLite
        val remainingSections = hierarchyDao.observeSectionsForList("list-1").first()
        assertTrue(remainingSections.isEmpty())

        // And: All 3 tasks are marked as is_deleted = 1 (in Trash)
        val activeTasks = taskDao.getActiveTasksForList("list-1").first()
        assertTrue(activeTasks.isEmpty())

        val trashedTasks = taskDao.getTrashedTasks().first()
        assertEquals(3, trashedTasks.size)
    }

    @Test
    fun lastRemainingList_cannotBeDeleted() = runTest {
        // Given: Exactly 1 list in database
        val defaultList = createSampleList("inbox-1", "Inbox", isDefault = true)
        hierarchyDao.insertList(defaultList)

        // When & Then: Attempting deletion throws IllegalStateException
        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.deleteList("inbox-1") }
        }
    }

    @Test
    fun unfiledList_canExistWithoutFolder() = runTest {
        val unfiledList = createSampleList("list-2", "Independent Notes", folderId = null)
        hierarchyDao.insertList(unfiledList)

        val unfiled = hierarchyDao.observeUnfiledLists().first()
        assertEquals(1, unfiled.size)
        assertEquals("Independent Notes", unfiled.first().name)
    }
}
```

---

## 10. Deliverables & Acceptance Checklist

- [ ] **4-Tier Hierarchy Implemented:** Clean relational flow from Folders → Lists → Sections → Tasks.
- [ ] **First-Launch Seeding:** Seeds exactly 1 default Inbox + 1 Tutorial Folder (1 sectioned list with tutorial tasks, 1 unsectioned flat list). Zero artificial bloat.
- [ ] **Sectioned & Unsectioned Support:** Lists dynamically render flat vertical cards when unsectioned, or sticky collapsible header groups when sectioned.
- [ ] **Section Deletion to Trash:** Deleting a section soft-deletes all its tasks to Trash (never orphaned, never permanently wiped).
- [ ] **List Deletion to Trash:** Deleting a list soft-deletes its tasks to Trash with 30-day retention and restore capability.
- [ ] **Last Remaining List Protection:** Database refuses deletion of the final remaining list in the app.
- [ ] **12-Swatch Palette + Hex Input:** Curated palette includes all 7 signature colors (`#2E7D32`, `#1565C0`, `#43A047`, `#FB8C00`, `#E64A19`, `#E53935`, `#576481`) plus 5 accents and 6-character hex input field.
- [ ] **Room Relational DAOs:** Reactive `Flow<FolderWithLists>` and atomic integer re-indexing transactions.
- [ ] **Zero-Mock Architecture:** Backed by real SQLite tables, Room DAOs, and MVI ViewModels.

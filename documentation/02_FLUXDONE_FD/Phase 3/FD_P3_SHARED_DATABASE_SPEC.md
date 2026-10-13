# 🗄️ Topic FD-P3-3: Multiplatform Shared Database & Local File Storage Specification

**Document ID:** `FD_P3_SHARED_DATABASE_SPEC`  
**Phase:** Phase 3 (Ubuntu Linux Desktop Companion via Compose Multiplatform)  
**Target Package:** `com.flux.done.database` (Shared Multiplatform Module)  
**Architecture:** Room 2.7+ Kotlin Multiplatform (KMP) + Bundled SQLite Driver + Coroutines Flow  
**Target OS:** Ubuntu Linux Workstation (JVM 17+, Linux x86_64 / arm64) & Android (API 26+)  
**Prerequisites:** [`FD_TRD_KOTLIN.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_TRD_KOTLIN.md) & [`GMP_FD_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/GMP_FD_BUILD.md)  

---

## 1. Overview & Architectural Scope

A split architecture between mobile and desktop that uses different database libraries (e.g. Room on Android vs. SQLDelight on Desktop) introduces significant risks of schema drift, query discrepancies, and double-maintenance overhead.

**Topic FD-P3-3 establishes the Shared Multiplatform Database Layer powered by official Room 2.7+ Kotlin Multiplatform (KMP).**

```
+─────────────────────────────────────────────────────────────────────────────+
|                    SHARED KMP MODULE: :core:database                        |
+─────────────────────────────────────────────────────────────────────────────+
|  Entities: FolderEntity, ListEntity, SectionEntity, TaskEntity, Subtask...  |
|  DAOs: FolderDao, ListDao, SectionDao, TaskDao, HabitDao, SettingsDao       |
|  Converters: InstantConverter, RecurrenceRuleConverter, PriorityConverter   |
|  Database Class: @Database(entities = [...], version = 1)                   |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
┌──────────────────────────────────────┐  ┌───────────────────────────────────┐
│     ANDROID TARGET (:app:android)    │  │    DESKTOP LINUX (:app:desktop)   │
├──────────────────────────────────────┤  ├───────────────────────────────────┤
│ • Platform Context: Android Context  │  │ • Storage: ~/.local/share/fluxdone│
│ • Storage: /data/data/.../databases  │  │ • Driver: BundledSQLiteDriver()   │
│ • Driver: AndroidSQLiteDriver()      │  │ • Security: POSIX chmod 600       │
│ • WAL Mode & Room InvalidationTracker│  │ • Watcher: java.nio WatchService  │
└──────────────────────────────────────┘  └───────────────────────────────────┘
```

---

## 2. Linux Storage Path & XDG File System Standards

On Ubuntu Linux, the database must never be placed haphazardly in the home directory or `/tmp`. It strictly adheres to the **XDG Base Directory Specification**:

### 2.1 File System Paths

| Resource | Path on Ubuntu Linux | Permissions | Purpose |
|---|---|---|---|
| **Primary SQLite Database** | `~/.local/share/fluxdone/fluxdone.db` | `0600` (`rw-------`) | Main database file |
| **Write-Ahead Log (WAL)** | `~/.local/share/fluxdone/fluxdone.db-wal` | `0600` (`rw-------`) | High-speed concurrent WAL journal |
| **Shared Memory Index (SHM)**| `~/.local/share/fluxdone/fluxdone.db-shm` | `0600` (`rw-------`) | SQLite WAL shared memory index |
| **Directory Container** | `~/.local/share/fluxdone/` | `0700` (`rwx------`) | Application data sandbox |
| **UI Preferences** | `~/.config/fluxdone/desktop_ui_prefs.json` | `0600` (`rw-------`) | Splitter widths, window bounds |

### 2.2 Directory Initialization & POSIX Security

```kotlin
package com.flux.done.database.desktop

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

object DesktopDatabaseStorageProvider {
    fun getDatabaseFile(): File {
        val xdgDataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.local/share")
        
        val directory = File(xdgDataHome, "fluxdone")
        if (!directory.exists()) {
            directory.mkdirs()
            try {
                val permissions = PosixFilePermissions.fromString("rwx------")
                Files.setPosixFilePermissions(directory.toPath(), permissions)
            } catch (_: UnsupportedOperationException) {
                // Fallback for non-POSIX file systems
            }
        }
        
        val dbFile = File(directory, "fluxdone.db")
        if (dbFile.exists()) {
            try {
                val permissions = PosixFilePermissions.fromString("rw-------")
                Files.setPosixFilePermissions(dbFile.toPath(), permissions)
            } catch (_: UnsupportedOperationException) { }
        }
        return dbFile
    }
}
```

---

## 3. Database Factory & Room KMP Configuration

### 3.1 Multiplatform Room Database Class

```kotlin
package com.flux.done.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.flux.done.database.converter.DatabaseTypeConverters
import com.flux.done.database.dao.*
import com.flux.done.database.entity.*

@Database(
    entities = [
        FolderEntity::class,
        ListEntity::class,
        SectionEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        SettingsKvEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseTypeConverters::class)
@ConstructedBy(FluxDoneDatabaseConstructor::class)
abstract class FluxDoneDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun listDao(): ListDao
    abstract fun sectionDao(): SectionDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun tagDao(): TagDao
    abstract fun settingsDao(): SettingsDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FluxDoneDatabaseConstructor : RoomDatabaseConstructor<FluxDoneDatabase> {
    override fun initialize(): FluxDoneDatabase
}
```

### 3.2 Desktop JVM Database Builder (`DesktopDatabaseBuilder.kt`)

```kotlin
package com.flux.done.database.desktop

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.flux.done.database.FluxDoneDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

object DesktopDatabaseBuilder {
    fun build(dbFile: File = DesktopDatabaseStorageProvider.getDatabaseFile()): FluxDoneDatabase {
        return Room.databaseBuilder<FluxDoneDatabase>(
            name = dbFile.absolutePath,
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onOpen(connection: androidx.sqlite.SQLiteConnection) {
                // Non-negotiable Pragmas for desktop integrity & high concurrency
                connection.execSQL("PRAGMA foreign_keys = ON;")
                connection.execSQL("PRAGMA journal_mode = WAL;")
                connection.execSQL("PRAGMA synchronous = NORMAL;")
                connection.execSQL("PRAGMA busy_timeout = 5000;")
            }
        })
        .build()
    }
}
```

---

## 4. SQLite Pragmas & High-Performance Desktop Profile

When running on an Ubuntu workstation with NVMe/SSD storage, the SQLite engine is configured for maximum concurrent throughput:

1. **`PRAGMA journal_mode = WAL;` (Write-Ahead Logging):**
   * Concurrent readers never block writers, and writers never block readers.
   * Background queries (such as calendar timeline render and live search) execute smoothly while background task insertions or batch moves take place.
2. **`PRAGMA synchronous = NORMAL;`:**
   * In WAL mode, `NORMAL` guarantees full ACID safety across application crashes without forcing a physical disk platter flush on every single write, reducing write latency to $<1\text{ms}$.
3. **`PRAGMA foreign_keys = ON;`:**
   * Strictly enforced on every database connection open. Deleting a `FolderEntity` or `ListEntity` cleanly executes cascading constraints defined in Room entities.
4. **`PRAGMA busy_timeout = 5000;`:**
   * Prevents `SQLiteDatabaseLockedException` by allowing up to 5000ms for busy write transactions to complete.

---

## 5. Clean Checkpoint & Flush on Suspend / Termination

To satisfy the non-negotiable **Suspend / Lock Invariant** (zero background process and zero orphaned journal files), FluxDone executes a complete synchronous checkpoint on shutdown:

```kotlin
package com.flux.done.database.desktop

import com.flux.done.database.FluxDoneDatabase

object DatabaseShutdownManager {
    fun checkpointAndClose(database: FluxDoneDatabase) {
        try {
            // Force WAL journal to truncate and write back all frames into main .db
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE);")
        } catch (_: Exception) {
            // Log fallback
        } finally {
            database.close()
        }
    }
}
```

---

## 6. Local File System Watcher (`DatabaseFileWatcher.kt`)

If the user runs a manual restore script or a future sync daemon modifies `~/.local/share/fluxdone/fluxdone.db` externally, FluxDone Desktop must detect the modification without requiring an application restart.

### 6.1 Implementation Specification

```kotlin
package com.flux.done.database.desktop

import com.flux.done.database.FluxDoneDatabase
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.*

class DatabaseFileWatcher(
    private val dbFile: File,
    private val database: FluxDoneDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var watchJob: Job? = null

    fun startWatching(onExternalModification: () -> Unit) {
        val path = dbFile.parentFile.toPath()
        watchJob = scope.launch {
            val watchService = FileSystems.getDefault().newWatchService()
            path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            )

            while (isActive) {
                val key = watchService.take()
                for (event in key.pollEvents()) {
                    val changed = event.context() as? Path
                    if (changed?.toString() == dbFile.name) {
                        // Invalidate Room cache to notify active StateFlow collectors
                        database.invalidationTracker.refreshVersionsSync()
                        withContext(Dispatchers.Main) {
                            onExternalModification()
                        }
                    }
                }
                if (!key.reset()) break
            }
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
    }
}
```

---

## 7. Verification & Testing Contracts

1. **Multiplatform Schema Parity Test:** Execute the Room schema generator on both Android and Desktop. Compare the generated SQLite DDL schemas byte-for-byte; verify zero discrepancies.
2. **WAL Concurrency Test:** Launch 50 simultaneous background read queries while executing an atomic batch completion of 20 tasks. Verify zero `SQLiteDatabaseLockedException` errors and latency $<15\text{ms}$.
3. **Truncate Checkpoint Test:** Mutate tasks, then invoke `DatabaseShutdownManager.checkpointAndClose()`. Verify using `ls -la ~/.local/share/fluxdone/` that `fluxdone.db-wal` has size 0 bytes or is cleanly merged.
4. **POSIX Permission Test:** Delete the directory and run `DesktopDatabaseStorageProvider.getDatabaseFile()`. Inspect permissions using `stat -c "%a" ~/.local/share/fluxdone/fluxdone.db`; verify permissions equal `600`.

# 🚀 GMP — Phase 3 Grand Master Prompt: FluxDone (FD) Desktop Build Instructions

**Version:** 3.0 (Native Compose Multiplatform Desktop Linux Master Build Specification)  
**Role:** Autonomous AI Senior Desktop Systems & Compose Multiplatform Engineer  
**Target:** Ubuntu Linux Workstation (`com.fluxdone.desktop`, Compose Multiplatform / Skiko, JVM 17+) & Shared KMP Module (`com.flux.done.database`, Room 2.7+ KMP)  
**Prerequisites:** Phase 1 Complete Suite ([`GMP_FD_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/GMP_FD_BUILD.md)), Phase 2 Complete Suite ([`GMP_FD_P2_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/GMP_FD_P2_BUILD.md))  

---

## 0. Non-Negotiable Operational Rules & Invariants

1. **Zero Flutter / Zero Dart:** 100% Native Kotlin & JetBrains Compose Multiplatform (1.6+) on Ubuntu Linux (GNOME X11 / Wayland, JVM 17+).
2. **Zero Mocks in Production Code:** All desktop components must bind to real Linux filesystem storage (`~/.local/share/fluxdone/fluxdone.db`), real Room KMP DAOs, real DBus session buses, and real system utilities (`xdg-open`). Hardcoded dummy lists or mock data are strictly prohibited in `main`.
3. **The Mandatory Subtask Cascade:** Marking a parent task complete (via keyboard `Alt+X`/`Ctrl+Enter`/`Space`, mouse click, or tray menu) **strictly and atomically marks all child subtasks complete** (`is_completed = 1`). Unchecking any subtask revives the parent task (`is_completed = 0`).
4. **The Strict Suspend / Lock Terminate Invariant (Zero-Footprint Guarantee):**
   - FluxDone registers native DBus listeners for system suspend (`org.freedesktop.login1.Manager.PrepareForSleep`) and GNOME screen lock (`org.gnome.ScreenSaver.ActiveChanged`).
   - The instant either signal fires, the application flushes the SQLite WAL (`PRAGMA wal_checkpoint(TRUNCATE);`), disposes the tray icon, and **terminates immediately via `exitProcess(0)`**.
   - While the user's laptop is locked or asleep, **literally zero CPU, zero battery, and zero processes exist** in `ps aux`.
5. **The Universal Magnetic Day Snapping Invariant:**
   - In **all calendar formats** (1-Day, 3-Day, and 7-Day), horizontal scrolling is fluid and continuous during trackpad/mouse dragging.
   - The instant interaction ceases (finger lifted or mouse released), the canvas **magnetically snaps to whole-day boundaries**. At rest, exactly 1, 3, or 7 full day columns are displayed. Zero fractional days are ever cut off at rest.
6. **The Keyboard Safety Invariant (Modifier-Driven):**
   - All navigation and execution shortcuts use unambiguous modifiers (`Ctrl`, `Alt`, `Shift`) or non-character keys (`Enter`, `Tab`, `Delete`, `Escape`). Typing inside task titles, search bars, or markdown notes is **never** intercepted or swallowed.
7. **Read-Only Google Calendar Overlay Invariant:**
   - Google Calendar events render strictly with a **1.5dp outlined muted border** (never filled), cached locally in SQLite table `gcal_events_cache`.
   - Google Calendar blocks are strictly non-editable and non-draggable; clicking opens the event URL in the default Linux browser (`xdg-open`).
8. **Autonomous Execution Order:** Implement Phase 3 features strictly in sequence:
   - Module 1: Compose Multiplatform Desktop 3-Pane UI (`FD_P3_DESKTOP_UI_SPEC.md`)
   - Module 2: Keyboard-First Navigation & Desktop Shortcuts (`FD_P3_KEYBOARD_SHORTCUTS_SPEC.md`)
   - Module 3: Multiplatform Shared Database & Local File Storage (`FD_P3_SHARED_DATABASE_SPEC.md`)
   - Module 4: Desktop System Tray, Background Daemon & Floating Quick Add (`FD_P3_SYSTEM_TRAY_GLOBAL_ADD_SPEC.md`)
   - Module 5: Desktop Timeline Canvas, 7-Day Week View & Google Calendar Overlay (`FD_P3_TIMELINE_EXTENSIONS_AND_GCAL_SPEC.md`)

---

## 1. Module 1: Compose Multiplatform Desktop 3-Pane Workspace UI

* **Specification:** [`FD_P3_DESKTOP_UI_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%203/FD_P3_DESKTOP_UI_SPEC.md)
* **Tasks:**
  1. Build `DesktopWorkspaceScreen.kt` using Compose Multiplatform with a 3-pane layout:
     - **Pane 1 (Left Sidebar, 260dp default):** Navigation tree for Folders, Lists, Sections, and Smart Lists.
     - **Pane 2 (Center Stream, Flex Fill):** High-density task list with sticky section headers, quick add field, and hover actions.
     - **Pane 3 (Right Inspector, 380dp default):** Dual-mode contextual panel: **Day Timeline Inspector active by default on startup**, switching automatically to **Task Detail Inspector** when a task row is clicked.
  2. Implement Draggable Splitter Handles between panes with width persistence to `~/.config/fluxdone/desktop_ui_prefs.json`.
  3. Implement Adaptive 2-Pane Breakpoint: Below `1024dp`, automatically collapse Pane 1 into an overlay navigation drawer.
  4. Implement Ubuntu GNOME CSD window header with custom minimize, maximize, and close controls.
  5. Apply deep dark theme tokens (`#0D1B2A` Abyssal Navy, `#1B263B` Oxford Slate, `#415A77` Steel Blue) with fractional DPI scaling support.

---

## 2. Module 2: Keyboard-First Navigation & Desktop Shortcuts

* **Specification:** [`FD_P3_KEYBOARD_SHORTCUTS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%203/FD_P3_KEYBOARD_SHORTCUTS_SPEC.md)
* **Tasks:**
  1. Implement `DesktopKeyDispatcher.kt` using `Modifier.onPreviewKeyEvent` with strict 3-tier precedence: Global Shortcuts $\to$ Active Text Field $\to$ Task Selection.
  2. Bind task stream navigation: `Alt + J` / `Down Arrow` (next), `Alt + K` / `Up Arrow` (prev).
  3. Bind task completion: `Alt + X` / `Ctrl + Enter` (and `Space` when row is focused), enforcing the **Mandatory Subtask Cascade**.
  4. Bind scheduling: `Ctrl + Alt + T` (Today), `Ctrl + Alt + M` (Tomorrow).
  5. Bind view switching: `Ctrl + 1` (Tasks), `Ctrl + 2` / `Alt + C` (Calendar Timeline), `Ctrl + 3` (Habits), `Ctrl + T` (Next Tab), `Ctrl + Shift + T` (Prev Tab).
  6. Bind hierarchy operations: `Tab` (Indent to subtask), `Shift + Tab` (Outdent).
  7. Bind editing & deletion: `Enter` (open details in Inspector), `Delete` (trash with 5s Undo via `Ctrl + Z`).
  8. Register OS-wide global summon hotkey: `Ctrl + Alt + Space` via DBus Global Shortcuts / X11 Keybinder.
  9. Build Keyboard Shortcuts Cheat-Sheet Modal (`Ctrl + ?`).

---

## 3. Module 3: Multiplatform Shared Database & Local File Storage

* **Specification:** [`FD_P3_SHARED_DATABASE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%203/FD_P3_SHARED_DATABASE_SPEC.md)
* **Tasks:**
  1. Standardize on **Room 2.7+ Kotlin Multiplatform** in the shared `:core:database` module with `BundledSQLiteDriver()`.
  2. Configure storage path on Linux adhering to XDG specs: `~/.local/share/fluxdone/fluxdone.db` with POSIX permissions `0600` (`rw-------`) and directory `0700` (`rwx------`).
  3. Enforce high-performance desktop SQLite Pragmas:
     - `PRAGMA foreign_keys = ON;`
     - `PRAGMA journal_mode = WAL;`
     - `PRAGMA synchronous = NORMAL;`
     - `PRAGMA busy_timeout = 5000;`
  4. Implement `DatabaseShutdownManager.kt` executing synchronous `PRAGMA wal_checkpoint(TRUNCATE);` and clean close.
  5. Build `DatabaseFileWatcher.kt` using `java.nio.file.WatchService` to invalidate Room cache upon external database modifications.

---

## 4. Module 4: Desktop System Tray, Background Daemon & Floating Quick Add

* **Specification:** [`FD_P3_SYSTEM_TRAY_GLOBAL_ADD_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%203/FD_P3_SYSTEM_TRAY_GLOBAL_ADD_SPEC.md)
* **Tasks:**
  1. Implement `FluxDoneTrayService.kt` integrating native Ubuntu AppIndicator (`libayatana-appindicator3` via JNA).
  2. Implement dynamic tray badge counter reflecting incomplete tasks for Today (`[ 5 ]`), switching to checkmark when all are completed.
  3. Build tray right-click context menu: Pending count header, Open Workspace (`Ctrl+1`), Quick Add (`Ctrl+Alt+Space`), Today's Agenda submenu with 1-click checkoff, Preferences, and Quit (`Ctrl+Q`).
  4. Intercept window close `[ ✕ ]`: Minimize window to system tray while keeping the daemon and global hotkeys active.
  5. Implement `DesktopSessionLockMonitor.kt` over DBus: On `PrepareForSleep` (suspend) or `ActiveChanged` (screen lock), cleanly checkpoint WAL and invoke `System.exit(0)` to guarantee **zero running processes while asleep/locked**.
  6. Build `QuickAddFloatingWindow.kt`: Centered, undecorated 560dp popup window with full Phase 2 NLP tokenizer support.

---

## 5. Module 5: Desktop Timeline Canvas, 7-Day Week View & Google Calendar Overlay

* **Specification:** [`FD_P3_TIMELINE_EXTENSIONS_AND_GCAL_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%203/FD_P3_TIMELINE_EXTENSIONS_AND_GCAL_SPEC.md)
* **Tasks:**
  1. Implement `TimelineDaySnapFlingBehavior.kt` providing continuous horizontal drag with **Magnetic Day Snapping on gesture release** across all 1-Day, 3-Day, and 7-Day formats.
  2. Build `DesktopWeekTimelineScreen.kt`: 7 equal day columns (Monday through Sunday) fitting desktop width, with sticky vertical 24-hour hour ruler, "Now" red line ticking every 60s, and expandable All-Day strip.
  3. Implement Mouse Wheel & Keyboard Zoom: `Ctrl + Mouse Wheel` smoothly adjusts time slot heights (60m: 24dp, 30m: 40dp default, 15m: 64dp), with presets `Ctrl + +`, `Ctrl + -`, and `Ctrl + 0`.
  4. Create `gcal_events_cache` SQLite table in Room database.
  5. Render Google Calendar overlay blocks: 1.5dp outlined muted borders, non-draggable/read-only, underneath FD task blocks, with 1-click `xdg-open` browser launching.
  6. Implement background 15-minute polling and offline resilience with header sync indicator.

---

## 6. Build & Verification Checklist

- [ ] `DesktopWorkspaceScreen` boots on Ubuntu Linux (GNOME) rendering all 3 panes with responsive splitters.
- [ ] Typing inside any text input never triggers keyboard navigation or task completion.
- [ ] Pressing `Alt + X` or `Ctrl + Enter` toggles task completion and cascades completion to all child subtasks.
- [ ] Database is created at `~/.local/share/fluxdone/fluxdone.db` with permissions `600` and journal mode `WAL`.
- [ ] Locking the desktop (`Super + L`) or suspending immediately terminates the process; `pgrep -f fluxdone` returns empty.
- [ ] In 7-Day Week View, dragging horizontally and releasing magnetically snaps to full day columns with zero fractional days at rest.
- [ ] `Ctrl + Mouse Wheel` zooms timeline vertically, and `Ctrl + 0` resets to 30m default.
- [ ] Google Calendar events render as 1.5dp outlined blocks; clicking opens event URL in system browser.

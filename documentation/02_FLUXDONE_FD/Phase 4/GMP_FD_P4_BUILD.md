# 🚀 GMP — Phase 4 Grand Master Prompt: FluxDone (FD) Build Instructions

**Version:** 4.0 (Native Android Kotlin & Desktop Linux Phase 4 Master Build Specification)  
**Role:** Autonomous AI Senior Systems, Multiplatform & Cloud Sync Engineer  
**Target:** Android Application (`com.fluxdone`, API 26+) & Ubuntu Linux Desktop (`com.fluxdone.desktop`, Compose Multiplatform / Skiko, JVM 17+) & Shared KMP Modules  
**Prerequisites:** Phase 1 Complete Suite ([`GMP_FD_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/GMP_FD_BUILD.md)), Phase 2 Complete Suite ([`GMP_FD_P2_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%202/GMP_FD_P2_BUILD.md)), & Phase 3 Desktop Suite ([`GMP_FD_P3_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%203/GMP_FD_P3_BUILD.md))  

---

## 0. Non-Negotiable Operational Rules & Invariants

1. **Zero Flutter / Zero Dart:** 100% Native Kotlin (Jetpack Compose on Android & JetBrains Compose Multiplatform on Ubuntu Desktop).
2. **Zero Mocks in Production Code:** All components must bind to real Room KMP DAOs, real Google Drive REST APIs, real DBus session buses, real KaTeX math engines, and real IPC broadcasts. Hardcoded dummy data is strictly prohibited in `main`.
3. **The $0 Free-Forever Cloud Invariant:**
   - Multi-device sync operates exclusively via Google Drive's hidden `appDataFolder` (`https://www.googleapis.com/auth/drive.appdata`).
   - Zero third-party cloud database subscriptions. Zero risk of project sleep or inactivity pausing.
4. **The Strict 45-Second Debounce Engine:**
   - Any local mutation arms a 45-second countdown timer.
   - Any new edit within 45 seconds resets the timer.
   - Upload fires only after 45 quiet seconds (with immediate bypass on app close, window focus, or manual "Sync Now").
5. **Field-Level Last-Write-Wins (LWW) Conflict Resolution:**
   - Merge conflicts are resolved at the attribute level via `field_timestamps_json`.
   - Non-overlapping edits (e.g. title edited on Phone, due date edited on Desktop) are both preserved without data loss.
6. **The Automatic Focus Start Invariant:**
   - Scheduled study blocks in FluxFoxus start automatically at the scheduled time (preceded by a 60-second warning chime), requiring zero manual confirmation.
7. **Session Completion $\implies$ Task Auto-Complete with Subtask Cascade:**
   - Finishing a focus block in FluxFoxus atomically marks the linked task complete (`is_completed = 1`) and cascades completion to all child subtasks.
8. **Early Quit Audit Tracking:**
   - Aborted or incomplete focus sessions keep the task active (`is_completed = 0`) and append an audit reflection note to task notes.
9. **Extended Markdown with KaTeX Vector Math:**
   - Renders LaTeX equations (`$...$` and `$$...$$`), Markdown tables, and callouts (`> [!NOTE]`, `> [!TIP]`, `> [!WARNING]`) natively with dark-mode compliance.
10. **Large Database Archiving Engine (>90 Days):**
    - Tasks completed $>90$ days ago are moved to `archived_tasks` to guarantee cold start $<1.0\text{s}$, 60 FPS timeline, and $<16\text{ms}$ check-off.
11. **Nightly 21:00 EOD Review Sheet:**
    - Triages incomplete tasks and broadcasts `com.flux.action.EOD_REVIEW_COMPLETED`, awarding up to 15 points to the FluxFoxus 5th Focus Score Component.
12. **Autonomous Execution Order:** Implement Phase 4 modules strictly in sequence:
    - Module 1: Cross-Device Cloud Sync Engine (`FD_P4_CLOUD_SYNC_ENGINE_SPEC.md`)
    - Module 2: Bidirectional Multi-Device Sync with FluxFoxus (`FD_P4_CROSS_DEVICE_FF_FD_SYNC_SPEC.md`)
    - Module 3: Extended Academic Markdown & Large Database Archiving (`FD_P4_EXTENDED_MARKDOWN_AND_ARCHIVING_SPEC.md`)
    - Module 4: End-of-Day Review & AI Task Alignment Generator (`FD_P4_EOD_REVIEW_AI_ALIGNMENT_SPEC.md`)

---

## 1. Module 1: Cross-Device Cloud Sync Engine & LWW Conflict Resolution

* **Specification:** [`FD_P4_CLOUD_SYNC_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%204/FD_P4_CLOUD_SYNC_ENGINE_SPEC.md)
* **Tasks:**
  1. Implement `GoogleDriveAppDataSyncAdapter.kt` using Ktor HTTP Client targeting the hidden `appDataFolder` API.
  2. Implement `DebouncedSyncScheduler.kt` with a strict 45-second debounce countdown timer.
  3. Augment Room schema with `updated_at_utc`, `is_deleted`, `deleted_at_utc`, `sync_state`, and `field_timestamps_json`.
  4. Implement `FieldLevelConflictResolver.kt` executing attribute-level Last-Write-Wins merging.
  5. Enforce 30-day tombstone replication and garbage collection.
  6. Provide `CloudSyncAdapter` pluggable interface ready for future Phase 5 Cloudflare D1 dual-sync.

---

## 2. Module 2: Bidirectional Multi-Device Sync with FluxFoxus

* **Specification:** [`FD_P4_CROSS_DEVICE_FF_FD_SYNC_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%204/FD_P4_CROSS_DEVICE_FF_FD_SYNC_SPEC.md)
* **Tasks:**
  1. Implement `FluxFoxusBroadcastDispatcher.kt` broadcasting `com.flux.action.FOCUS_BLOCK_SYNC` for timed tasks across Android and Linux DBus.
  2. Enforce the Automatic Start Invariant: Scheduled sessions auto-engage at the exact scheduled start time with a 60-second warning countdown.
  3. Implement `FocusSessionCompletedReceiver.kt`: Automatically marks linked task complete and cascades completion to all subtasks with audit metadata.
  4. Implement Early Quit Handler: Appends audit note (`⚠️ Focus Block Incomplete: X of Y min`) while keeping task active.
  5. Enforce Active Discipline Dominance: Locks active task blocks on the calendar timeline against dragging, resizing, or deletion during active focus.

---

## 3. Module 3: Extended Academic Markdown & Large Database Archiving

* **Specification:** [`FD_P4_EXTENDED_MARKDOWN_AND_ARCHIVING_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%204/FD_P4_EXTENDED_MARKDOWN_AND_ARCHIVING_SPEC.md)
* **Tasks:**
  1. Implement `RenderMarkdownContent.kt` with client-side KaTeX AST parser rendering LaTeX inline (`$...$`) and display (`$$...$$`) vector equations.
  2. Render Markdown tables with horizontal scroll and zebra striping.
  3. Render GitHub-style callouts (`> [!NOTE]`, `> [!TIP]`, `> [!WARNING]`, `> [!CAUTION]`) with matching theme borders and icons.
  4. Create `archived_tasks` SQLite table in `fluxdone.db`.
  5. Implement `ArchiveDao.kt` migrating completed tasks older than 90 days in atomic `@Transaction` blocks.
  6. Integrate `[ Include Archive ]` toggle in universal search.

---

## 4. Module 4: End-of-Day Review & AI Task Alignment Generator

* **Specification:** [`FD_P4_EOD_REVIEW_AI_ALIGNMENT_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%204/FD_P4_EOD_REVIEW_AI_ALIGNMENT_SPEC.md)
* **Tasks:**
  1. Schedule daily 21:00 review notification via `AlarmManager` / Linux desktop cron.
  2. Build `DailyReviewSheet.kt` (SCR-11) displaying completed summary, incomplete task triage, and reflection input.
  3. Provide 1-tap triage buttons: `[ 🌅 Tomorrow ]`, `[ 📆 Pick Date ]`, `[ 🚫 Won't Do ]`, `[ 🗑️ Trash ]`.
  4. Implement `EODReviewBroadcastDispatcher.kt`: Broadcasts `com.flux.action.EOD_REVIEW_COMPLETED` awarding up to 15 points to FluxFoxus 5th Focus Score Component.
  5. Build `AITaskAlignmentEngine.kt`: Generates suggested tomorrow schedule prioritizing high-cognitive load tasks during morning peak hours (09:00 - 11:30) with 1-tap acceptance.

---

## 5. Build & Verification Checklist

- [ ] Google Drive `appDataFolder` sync functions with zero third-party cloud hosting; mutations trigger after exactly 45 quiet seconds.
- [ ] Offline edits on phone (title) and desktop (due date) merge gracefully via Field-Level LWW without data loss.
- [ ] Scheduled focus block in FluxDone automatically engages in FluxFoxus at scheduled time with 60-second warning chime.
- [ ] Completing a focus session in FluxFoxus auto-marks linked task complete in FluxDone and checks off all child subtasks.
- [ ] Prematurely aborted focus session leaves task active and appends audit reflection note.
- [ ] LaTeX math equations render as sharp vector typography in notes across Android and Desktop Skiko.
- [ ] Tasks completed $>90$ days ago are moved to `archived_tasks`; cold start remains $<1.0\text{s}$ with 20k records.
- [ ] Submitting 21:00 EOD review broadcasts 15 alignment points to FluxFoxus and schedules tomorrow's aligned blocks.

# GMP — Grand Master Prompt: FluxDone (FD) Build Instructions

**Version:** 2.0 (Native Android Kotlin Master Build Specification)  
**Role:** Autonomous AI Senior Android System & UI Engineer  
**Target:** Android Application (`com.fluxdone`, Jetpack Compose) & Ubuntu Linux Desktop (`com.fluxdone.desktop`, Compose Multiplatform / JetBrains Compose)  
**Prerequisites:** Read `FLUX_SYSTEM_CONTEXT.md`, `FLUX_DESIGN_TOKENS.md`, `FD_PRD_V2.md`, `FD_TRD_KOTLIN.md`, `FD_UI_SPECIFICATIONS.md`, and all Phase 1 Specs (`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`, `FD_P1_TASK_MANAGEMENT_SPEC.md`, `FD_P1_CALENDAR_TIMELINE_SPEC.md`, `FD_P1_RECURRENCE_ENGINE_SPEC.md`, `FD_P1_HABIT_TRACKER_SPEC.md`, `FD_P1_SMART_LISTS_AND_TRASH_SPEC.md`).

---

## 0. Non-Negotiable Operational Rules & Invariants

1. **Zero Flutter / Zero Dart:** You are strictly forbidden from writing Dart, using Flutter SDK, or referencing Flutter plugins. FluxDone is 100% **Native Kotlin (Jetpack Compose on Android & JetBrains Compose Multiplatform on Desktop)**.
2. **Zero Brittle Code-Gen Loops:** Do not use `build_runner`. All code generation must be standard Kotlin Symbol Processing (**KSP**) running natively through Gradle.
3. **4-Tier Structural Hierarchy:** Maintain the strict tree invariant:
   $$\text{Folders} \longrightarrow \text{Lists} \longrightarrow \text{Sections} \longrightarrow \text{Tasks}$$
   - Sectioned lists display sticky section headers with task counters and collapse toggles.
   - Unsectioned lists render clean, uncluttered task cards without artificial sections.
   - Deleting a list or section soft-deletes tasks to Trash with 30-day retention.
4. **Tactile Checkbox & Subtask Invariants:**
   - Checkbox tap triggers a scale-bounce animation (`1.0 -> 0.85 -> 1.1 -> 1.0`), applies title strike-through, and delays for 400ms before removing the item from the active view.
   - Bidirectional subtask sync: Completing all subtasks completes the parent; checking the parent marks all subtasks complete; unchecking any subtask revives the parent.
   - Subtasks are strictly lightweight checklist items (title and checkbox only; zero subtask dates or reminders).
5. **Calendar Timeline Tapping & Strict Duration Preservation:**
   - Calendar View (SCR-04) supports tapping empty grid slots to open Quick Task Creation (SCR-03) pre-filled with the selected 15-minute start and end time.
   - Dragging a task block vertically to reschedule **strictly preserves its exact duration** (e.g. a 55-minute task remains exactly 55 minutes; start time snaps to 15m intervals, duration never rounds).
   - Dynamic All-Day top strip houses date-only tasks without shrinking the visible 24-hour timeline height.
   - Current time indicated by a bold red "Now" line auto-advancing every 60 seconds.
6. **RFC 5545 Recurrence Engine (Option A Lifecycle):**
   - Completing a recurring task instance preserves that row in the historical completed list and timeline, and immediately spawns the next occurrence with fresh uncompleted subtasks.
   - Deleting offers 3 choices: *This task only*, *This and future*, *All tasks in series*.
   - Overdue catch-up protection: Tasks never create duplicate backlogged instances.
7. **Dual Habit Modes & Hard Ceiling Rule:**
   - Supports both Simple 1-Tap (`BINARY`) and Multi-Step (`NUMERIC`) habits.
   - Numeric habits support custom increment steps (e.g. target 12, step 3).
   - Hard Ceiling: Numeric habits cap strictly at `target_count` (no overshooting).
   - Short tap increments by step size; long-press decrements by step size (floored at 0).
   - Specific-days habits: Non-scheduled days are rest days; they are hidden from the daily grid and **never break streaks**.
8. **Smart Lists & Soft-Delete Trash (SCR-12):**
   - `Today` smart list features a prioritized Overdue section with a dedicated **One-by-One Overdue Rescheduling Modal** (`RescheduleOverdueSheet`).
   - `Completed` smart list categorizes tasks into 4 exact chronological buckets: **Today**, **Yesterday**, **Previous 7 Days**, and **Older (Before 7 Days)**, equipped with a real-time search bar.
   - Smart list visibility can be customized via both Navigation Drawer (`[ ✎ ]` shortcut) and Settings.
   - Soft-delete Trash preserves 30-day retention with days-remaining counters, 1-tap restore (with fallback to `Inbox` if parent list was deleted), and permanent purge safety dialogs.

---

## 1. Project Initialization & Gradle Setup

### 1.1 Project Structure
Initialize an Android Gradle project with package name `com.fluxdone`:
```
fluxdone/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/fluxdone/
│           ├── core/
│           │   ├── database/        // Room Database, DAOs, Entities, TypeConverters
│           │   ├── datastore/       // SmartListPreferences, AppPreferences
│           │   ├── designsystem/    // FluxTheme, Color (12 swatches), Type, Components
│           │   ├── recurrence/      // RRuleParser, NextOccurrenceCalculator
│           │   └── util/            // Date/Time formatters, CoroutineDispatchers
│           ├── domain/              // Use cases, Domain models
│           └── features/
│               ├── hierarchy/       // Folders, Lists, Sections management (SCR-10, SCR-11)
│               ├── tasklist/        // Task list view, Sections, Sticky headers (SCR-01)
│               ├── taskdetail/      // Task detail sheet & Quick creation (SCR-02, SCR-03)
│               ├── calendar/        // Custom Canvas 24h Timeline (SCR-04)
│               ├── habits/          // Habit tracker grid & editor (SCR-06, SCR-07)
│               ├── smartlists/      // Today, Tomorrow, Upcoming, All, Completed (SCR-05)
│               ├── trash/           // Soft-delete Trash & Auto-Purge (SCR-12)
│               └── navigation/      // Navigation Drawer (SCR-08), NavHost
├── build.gradle.kts
└── settings.gradle.kts
```

### 1.2 `app/build.gradle.kts` Standard Dependencies
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fluxdone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fluxdone"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation & Lifecycle
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Room Database with KSP
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager (Trash Auto-Purge & Midnight Habit Resets)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Kotlinx Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
```

---

## 2. Implementation Execution Sequence

Execute the implementation in strict dependency order across all 6 core modules:

### Module 1: Core Design System & Tokens ([`FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_LISTS_FOLDERS_DOMAINS_SPEC.md))
1. Implement `FluxTheme.kt`, `Color.kt`, and `Type.kt` matching `FLUX_DESIGN_TOKENS.md`.
2. Configure both Light and Dark theme palettes.
3. Codify the curated 12-swatch palette including the 7 academic colors:
   - Olympiad Emerald (`#2E7D32`), Royal Azure (`#1565C0`), Forest Green (`#43A047`), Amber Gold (`#FB8C00`), Vermillion Orange (`#E64A19`), Crimson Red (`#E53935`), Deep Slate (`#576481`), plus Amethyst Purple, Vibrant Teal, Rose Pink, Electric Indigo, Neutral Charcoal, and custom hex input.
4. Build reusable UI components: `FluxCheckbox`, `FluxPriorityPill`, `AcademicDomainBadge`, `DynamicAllDayStrip`.

### Module 2: Room SQLite Database Layer & Pre-Seeding
1. Implement Room Entities with full foreign keys and cascading rules:
   - `FolderEntity` (`id`, `name`, `sort_order`, `is_deleted`)
   - `TaskListEntity` (`id`, `folder_id`, `name`, `color_hex`, `is_sectioned`, `is_system`, `sort_order`, `is_deleted`)
   - `SectionEntity` (`id`, `list_id`, `name`, `sort_order`, `is_deleted`)
   - `TaskEntity` (`id`, `list_id`, `section_id`, `title`, `notes`, `start_time`, `end_time`, `duration_minutes`, `due_date`, `is_completed`, `completed_at`, `priority`, `recurrence_rule`, `recurrence_series_id`, `is_deleted`, `deleted_at`, `deleted_from_list_id`, `deleted_from_section_id`)
   - `SubtaskEntity` (`id`, `task_id`, `title`, `is_completed`, `sort_order`)
   - `HabitEntity` (`id`, `title`, `habit_type`, `target_count`, `increment_count`, `color_hex`, `icon_identifier`, `frequency_type`, `scheduled_days_mask`, `current_streak`, `best_streak`, `is_archived`, `is_deleted`, `deleted_at`)
   - `HabitCompletionEntity` (`id`, `habit_id`, `completion_date`, `completed_count`, `is_target_met`, `completed_at`)
2. Define DAOs with reactive Flow queries (`FolderDao`, `TaskListDao`, `SectionDao`, `TaskDao`, `SubtaskDao`, `HabitDao`, `SmartListsDao`, `TrashDao`).
3. **Database Pre-Seeding Invariant:** On initial creation, seed:
   - Default `Inbox` list (unsectioned, default blue).
   - Default `Olympiad Prep` folder containing:
     - 1 sectioned list: `Neev Diamond` (Sections: *Physics*, *Chemistry*, *Math*).
     - 1 unsectioned list: `Combinatorics`.

### Module 3: Task Management, Detail Sheet & Notes ([`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md))
1. Build `TaskCard` Composable with:
   - Scale-bounce tactile checkbox with 400ms completion delay.
   - Priority indicator flag / border.
   - Domain color pill.
   - Fixed swipe actions: Swipe Left = Move to Trash with 5s Undo snackbar; Swipe Right = Quick reschedule to Tomorrow.
2. Build `TaskDetailSheet` (SCR-02):
   - Unframed title input + live markdown preview notes field with clickable web links.
   - Subtasks checklist with bidirectional parent completion synchronization.
   - Date-only vs Timed task selector.
   - Deep task duplication action (`"${title} (copy)"` with duplicated subtasks).
3. Build `QuickCreationSheet` (SCR-03):
   - Keyboard auto-focus, date pill, list tag pill, enter-to-submit.

### Module 4: Calendar Timeline View ([`FD_P1_CALENDAR_TIMELINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_CALENDAR_TIMELINE_SPEC.md))
1. Build custom Canvas 24-hour timeline (`TimelineCanvas.kt` — SCR-04):
   - 1-Day and 3-Day responsive viewports.
   - 64dp vertical grid per hour with 15m hairline guides.
   - Dynamic All-Day top strip housing date-only tasks (preserves total 24h viewport height).
   - Red "Now" line auto-advancing every 60 seconds.
2. Timeline-Tapping Creation Flow:
   - Tapping an empty slot renders a highlighted ghost block (default 30m).
   - Bottom edge drag-handle adjusts duration before confirming creation.
3. Drag-to-Reschedule Gesture:
   - Long-press to lift time-block.
   - Vertical drag snaps start time to 15m intervals while **strictly preserving original duration**.
   - Completed tasks remain rendered at 50% opacity with strike-through title.

### Module 5: RRULE Recurrence Engine ([`FD_P1_RECURRENCE_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_RECURRENCE_ENGINE_SPEC.md))
1. Implement RFC 5545 parser and generator (`RRuleEngine.kt`):
   - Daily, weekdays, weekly on specific days (Mon/Wed/Fri), and custom intervals ($N$ days/weeks/months).
2. Option A Lifecycle:
   - Marking recurring task complete stamps `is_completed = 1, completed_at = now`.
   - Immediately generates next uncompleted occurrence with fresh uncompleted subtasks.
3. Recurrence Delete Dialog:
   - Displays 3-choice modal: *This task only*, *This and future*, *All tasks in series*.
4. Overdue catch-up protection: Generates next instance from current time forward.

### Module 6: Habit Tracker & Streak Engine ([`FD_P1_HABIT_TRACKER_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_HABIT_TRACKER_SPEC.md))
1. Build `HabitsScreen` (SCR-06):
   - Daily habit cards with circular progress rings and flame streak badges.
   - Dual modes: 1-Tap Simple (`BINARY`) and Multi-Step Numeric (`NUMERIC`).
   - Custom increment step sizes (e.g. target 12, step 3).
   - Hard Ceiling: Strictly capped at target count; no overshooting.
   - Short tap increments; long-press decrements floored at 0.
   - Celebratory confetti particle explosion upon reaching target.
2. Specific-Days Scheduling & Rest Day Invariant:
   - Non-scheduled days are hidden from daily grid and count as rest days that **never break streaks**.
3. Archive vs Trash:
   - Dedicated Archive view preserves historical completion logs.
   - Soft-delete Trash with 30-day auto-purge.

### Module 7: Smart Lists, Filter Engine & Trash ([`FD_P1_SMART_LISTS_AND_TRASH_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_SMART_LISTS_AND_TRASH_SPEC.md))
1. Implement System Smart Lists (SCR-05):
   - `Today`: Displays Overdue batch at top with high-contrast alert badge.
   - `RescheduleOverdueSheet`: Dedicated modal to triage overdue tasks **one-by-one** (options: Today, Tomorrow, Next Week, Pick Date, Won't Do/Trash, or Batch Shift All).
   - `Tomorrow`, `Upcoming`, and `All Active`.
   - `Completed`: Categorized strictly into 4 date buckets: **Today**, **Yesterday**, **Previous 7 Days**, and **Older (Before 7 Days)**, with live search bar.
2. Smart List Visibility Management:
   - Accessible via Navigation Drawer `[ ✎ ]` shortcut and Settings.
   - Toggle visibility and reorder items; persisted via DataStore Preferences.
3. Soft-Delete Trash Screen (SCR-12):
   - 30-day retention countdown badges (`X days left`).
   - 1-tap Restore: Restores to original list/section; falls back safely to `Inbox` if parent list was deleted.
   - Individual hard delete with confirmation; Empty Trash danger alert.
   - Background periodic auto-purge worker removing items expired past 30 days.

---

## 3. Verification & Quality Gates

Before declaring Phase 1 complete, execute and pass all verification checks:
```bash
# 1. Compile and run unit tests for Room DAOs, RRULE engine, and Habit Streaks
./gradlew testDebugUnitTest

# 2. Build debug APK
./gradlew assembleDebug

# 3. Verify APK exists and has zero manifest, resource, or dependency conflicts
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Zero Hardcoded Data & Branch-Testing Isolation Mandate

> [!CAUTION]
> **STRICT BAN ON HARDCODED MOCKS IN PRODUCTION CODE:**
> AI coding agents are strictly prohibited from embedding hardcoded dummy strings, static lists (e.g. `listOf(...)`), fake task items, or mocked calendar events inside `@Composable` screens, ViewModels, or Repositories. All data must dynamically originate from live Room SQLite DAOs or DataStore preferences.

**Branch-Testing Isolation Workflow:**
- If test stubs or static fixtures are needed for isolated visual or algorithmic verification:
  1. Check out an isolated testing branch (`git checkout -b test/feature-name`).
  2. Implement temporary fixtures only on that isolated branch.
  3. Verify execution.
  4. If successful: completely revert all hardcoded data, wire the component to live Room DAOs / dynamic repositories, and verify with live data before merging into `main`.
  5. If failed: revert to clean state, diagnose dynamic flow, re-branch and repeat.
  6. Zero mock or stub literals are ever permitted to remain in `main`.

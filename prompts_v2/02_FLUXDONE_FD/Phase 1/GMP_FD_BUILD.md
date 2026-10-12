# GMP — Grand Master Prompt: FluxDone (FD) Build Instructions

**Version:** 2.0 (Kotlin + Jetpack Compose Master Build Specification)  
**Role:** Autonomous AI Senior Android Engineer  
**Target:** Android Application (`com.fluxdone`) & Compose Desktop  
**Prerequisites:** Read `FLUX_SYSTEM_CONTEXT.md`, `FLUX_DESIGN_TOKENS.md`, `FD_PRD_V2.md`, `FD_TRD_KOTLIN.md`, and `FD_UI_SPECIFICATIONS.md`.

---

## 0. Non-Negotiable Operational Rules

1. **Zero Flutter / Zero Dart:** You are strictly forbidden from writing Dart, using Flutter SDK, or creating `pubspec.yaml`. The entire project is 100% **Native Kotlin and Jetpack Compose**.
2. **Zero Code-Generation CLI Loops:** Do not use `build_runner`. All code generation must be standard Kotlin Symbol Processing (**KSP**) running natively through Gradle.
3. **Exact Visual & Theme Match:** Adhere strictly to the 7 locked academic domain colors in `FLUX_DESIGN_TOKENS.md`. Do not improvise hex values.
4. **Calendar Tapping is Primary:** The Calendar View (SCR-04) must support instant slot tapping to open the Quick Task Creation bottom sheet (SCR-03) pre-filled with the selected 15-minute start and end time.
5. **IPC Broadcast Guarantee:** Every time a task with `start_time` and `end_time` is created, updated, or deleted, dispatch the explicit native Android Intent broadcast `com.flux.action.FOCUS_BLOCK_SYNC` targeting `com.fluxfoxus`.

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
│           │   ├── designsystem/    // FluxTheme, Color, Type, Components
│           │   ├── ipc/             // FocusBlockBroadcastSender
│           │   └── util/            // Date/Time formatters, CoroutineDispatchers
│           ├── domain/              // Use cases, Domain models
│           └── features/
│               ├── tasklist/        // SCR-01, SCR-05, SCR-12
│               ├── taskdetail/      // SCR-02, SCR-03
│               ├── calendar/        // SCR-04 (Custom Canvas Timeline)
│               ├── habits/          // SCR-06, SCR-07
│               ├── navigation/      // SCR-08 (Side Drawer), NavHost
│               └── settings/        // SCR-09, SCR-13 (Backup & Export)
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
        kotlinCompilerExtensionVersion = "1.5.8" // Or matching Kotlin version compiler
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

    // Room Database with KSP
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Kotlinx Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
```

---

## 2. Implementation Execution Sequence

Execute the implementation in strict dependency order:

### Phase 1: Core Design System & Tokens
1. Implement `FluxTheme.kt`, `Color.kt`, and `Type.kt` matching `FLUX_DESIGN_TOKENS.md`.
2. Configure both Light and Dark theme palettes.
3. Build atomic UI components: `FluxCheckbox`, `FluxPriorityPill`, `AcademicDomainBadge`.

### Phase 2: Database Layer (Room SQLite)
1. Implement Room Entities: `FolderEntity`, `TaskListEntity`, `SectionEntity`, `TaskEntity`, `SubtaskEntity`, `HabitEntity`, `HabitCompletionEntity`.
2. Define DAOs with reactive Flow queries (`@Query("SELECT * FROM tasks WHERE is_deleted = 0 ...") fun observeTodayTasks(): Flow<List<TaskWithDetails>>`).
3. Seed the 7 locked academic domain lists upon initial database creation.

### Phase 3: IPC Broadcast Gateway
1. Implement `FocusBlockBroadcastSender`:
   - Builds explicit intent with action `com.flux.action.FOCUS_BLOCK_SYNC`.
   - Targets package `com.fluxfoxus`.
   - Packs task parameters and action type (`CREATE`, `UPDATE`, `DELETE`).
2. Wire sender into `TaskRepository.insertTimedTask()`, `updateTimedTask()`, and `deleteTask()`.

### Phase 4: UI Screens & ViewModels
1. **SCR-01 & SCR-05 (Task Lists):** Implement sticky headers for sections, swipeable task rows, and filter tabs.
2. **SCR-03 & SCR-02 (Task Creation & Detail):** Modal sheets with date-time pickers and subtasks.
3. **SCR-04 (Calendar Timeline View):** Build custom Canvas timeline layout with 24-hour vertical grid, 15-minute slot snapping, and tap-to-create gestures.
4. **SCR-06 & SCR-07 (Habits):** Daily habit completion rings, streak counters, and celebratory haptics.
5. **SCR-08, SCR-09, SCR-12, SCR-13:** Side drawer, settings, trash restore, and local JSON export/import.

---

## 3. Verification & Quality Gates

Before declaring the build complete, execute and pass all verification checks:
```bash
# 1. Compile and run unit tests
./gradlew testDebugUnitTest

# 2. Build debug APK
./gradlew assembleDebug

# 3. Verify APK exists and has zero manifest or resource conflicts
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


# GMP — Grand Master Prompt: FluxFoxus (FF) Build Instructions

**Version:** 2.0 (Native Android Kotlin Master Build Specification)  
**Role:** Autonomous AI Senior Android System & UI Engineer  
**Target:** Android Application (`com.fluxfoxus`) & Ubuntu Linux Daemon  
**Prerequisites:** Read `FLUX_SYSTEM_CONTEXT.md`, `FLUX_DESIGN_TOKENS.md`, `IPC_SPEC_FD_FF_NATIVE.md`, `FF_PRD_V2.md`, `FF_TRD_KOTLIN.md`, and `FF_UI_SPECIFICATIONS.md`.

---

## 0. Non-Negotiable Operational Rules

1. **Zero Flutter / Zero Dart:** You are strictly forbidden from writing Dart, using Flutter SDK, or referencing Flutter plugins. FluxFoxus is 100% **Native Android Kotlin**.
2. **Strict Holst (Холст) Theming:** Sourced directly from `Theme/Холст.jpeg`. All UI components, dialogs, and overlays must strictly map to the Holst palette:
   - `#0D1B2A` (Abyssal Navy Background), `#1B263B` (Oxford Slate Surface), `#415A77` (Muted Steel Blue Border), `#778D7A` (Muted Sage Green Accent/Breaks), `#D4C4A8` (Warm Sand Khaki Primary Action), `#F4F1DE` (Eggshell Cream Text).
   - *Purples, cyans, and oversaturated neon colors are strictly forbidden.*
3. **No Brittle Code-Gen Loops:** Do not use `build_runner` or external CLI generators. Use **Kotlin Symbol Processing (KSP)** natively via Gradle.
4. **Enforcement Services are First-Class:**
   - App interception must use a native `AccessibilityService`.
   - Timer countdowns must run inside a persistent `ForegroundService`.
   - Intervention popups must use Android `WindowManager` system overlays (`TYPE_APPLICATION_OVERLAY`).
5. **IPC Receiver Implementation:** Implement `FdIntegrationReceiver` to automatically ingest scheduled study blocks broadcast by FluxDone (`com.flux.action.FOCUS_BLOCK_SYNC`).

---

## 1. Project Initialization & Gradle Setup

### 1.1 Project Structure
Initialize an Android Gradle project with package name `com.fluxfoxus`:
```
fluxfoxus/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/xml/accessibility_service_config.xml
│       └── java/com/fluxfoxus/
│           ├── core/
│           │   ├── database/        // Room Database, DAOs, Entities
│           │   ├── designsystem/    // Holst Theme, Typography, FlipClock Composable
│           │   ├── ipc/             // FdIntegrationReceiver (goAsync)
│           │   └── service/         // FocusAccessibilityService, FocusTimerService, OverlayManager
│           ├── domain/              // Use cases, Presets, Friction formula calculators
│           └── features/
│               ├── home/            // Home dashboard & streak display
│               ├── focus/           // Mechanical Flip Clock screen & Stop Modal
│               ├── presets/         // Preset CRUD & YouTube Whitelist
│               ├── limits/          // App Limits & Extra Time tokens
│               ├── stats/           // UsageStatsManager queries & charts
│               └── widget/          // Jetpack Glance 4-segment donut ring
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
    namespace = "com.fluxfoxus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fluxfoxus"
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

    // Navigation, Lifecycle & DataStore
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Room Database with KSP
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Jetpack Glance (Widgets) & WorkManager
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
```

---

## 2. Implementation Execution Sequence

### Phase 1: Holst (Холст) Design System & Flip Clock
1. Implement `FluxFoxusTheme.kt` mapping exclusively to Holst hex tokens from `Theme/Холст.jpeg`.
2. Build the custom `@Composable FlipClock` using `JetBrains Mono` tabular figures, split flap cards in `#1B263B`, and `#D4C4A8` hairline dividers.
3. Build the `StopFocusingModal` with locked delay timers.

### Phase 2: Room SQLite Database Layer
1. Implement Entities: `PresetEntity`, `PresetAppRuleEntity`, `FocusSessionEntity`, `AppLimitEntity`, `StreakEntity`.
2. Implement DAOs with reactive Kotlin `Flow` queries.
3. Implement `DataStore` repository for last-used preset ID and channel whitelist.

### Phase 3: Android System Services
1. **`FocusTimerService` (Foreground Service):**
   - Implements countdown ticker with `SupervisorJob()` coroutine scope.
   - Manages ongoing notification with remaining `HH:MM`.
   - Exposes `StateFlow<TimerState>` for UI observation.
2. **`FocusAccessibilityService`:**
   - Detects `TYPE_WINDOW_STATE_CHANGED`.
   - Checks against active preset blocklist; redirects to Home launcher if blocked.
   - Reads YouTube channel name nodes for Whitelist compliance.
3. **`FdIntegrationReceiver` (IPC Gateway):**
   - Receives `com.flux.action.FOCUS_BLOCK_SYNC`.
   - Calls `goAsync()` to persist scheduled session in Room DB without blocking the main thread.

### Phase 4: UI Screens & ViewModels
1. **Home Screen:** Preset card, streak counter (`🔥`), and primary CTA.
2. **Focus Screen:** Mechanical flip clock, break management, and friction quit dialog.
3. **Presets & Limits Screens:** Preset configuration forms, app search, and slider budgets.
4. **Stats Screen:** Query `UsageStatsManager` and render screen time breakdown.
5. **Widget:** Implement Glance 4-segment donut ring widget.

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

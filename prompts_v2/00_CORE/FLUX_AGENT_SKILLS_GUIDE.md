# Flux Multi-Agent Toolchain & Skills Guide (v2.0)

**Version:** 2.0 (Kotlin + Jetpack Compose Toolchain)  
**Target Environment:** Android SDK 34 + Ubuntu Linux Desktop (Compose Multiplatform)  
**Purpose:** Guide for AI coding agents (Antigravity / Claude) to configure environment, build projects, and avoid architectural traps.

---

## 1. Environment & Prerequisites on Ubuntu Linux

Ari works on an **Ubuntu Linux (LTS)** machine. Every autonomous agent executing build commands or project setup must verify and use the following toolchain:

### 1.1 JDK (Java Development Kit)
- **Required Version:** OpenJDK 17 or OpenJDK 21 LTS
- **Verification:**
  ```bash
  java -version
  javac -version
  ```
- **Environment Variable:**
  ```bash
  export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
  ```

### 1.2 Android SDK & Command-Line Tools
- **Android Target SDK:** API 34 (Android 14)
- **Android Min SDK:** API 26 (Android 8.0 Oreo) — *mandatory for `UsageStatsManager` and Notification Channels*
- **Android SDK Location:** Typically `~/android-sdk` or `~/Android/Sdk`
- **Environment Variables:**
  ```bash
  export ANDROID_HOME="$HOME/android-sdk"
  export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
  ```

---

## 2. Standardized Gradle Build Configuration (Kotlin DSL)

All Flux projects MUST use **Gradle with Kotlin DSL (`build.gradle.kts`)**. Do not generate legacy Groovy `build.gradle` files.

### 2.1 Approved Architecture Libraries

| Role | Standard Approved Library | Version Guidance | Reason / Notes |
| :--- | :--- | :--- | :--- |
| **UI Framework** | Jetpack Compose (Compose BOM) | Latest stable BOM (2024.x+) | Declarative, reactive, unified with Desktop |
| **State Management** | Android Jetpack `ViewModel` + `StateFlow` | Standard Lifecycle 2.8+ | Zero boilerplate, native coroutines |
| **Local Database** | Android Jetpack `Room` with **KSP** | Room 2.6+ (`androidx.room`) | Type-safe SQLite, compile-time query verification |
| **Desktop DB** | `SQLDelight` or Room 2.7+ (multiplatform) | Latest stable | Shared relational SQLite on Ubuntu |
| **Serialization** | `kotlinx.serialization` | 1.6+ | Pure Kotlin, no reflection or code-gen bloat |
| **Asynchronous** | Kotlin Coroutines & Flow | 1.8+ | Native structured concurrency |
| **Dependency Injection** | `Hilt` or `Koin` | Latest stable | Simple, lightweight dependency graphs |
| **Navigation** | `androidx.navigation:navigation-compose` | 2.7+ | Type-safe declarative Compose navigation |
| **System Overlays & Usage** | Native Android APIs directly | Standard Android Framework | No third-party wrappers; use direct Android SDK |

---

## 3. Strict Rules for AI Coding Agents

> [!CAUTION]
> **Zero Flutter / Zero Dart Policy:** Under no circumstances should an agent suggest, create, or reference Flutter SDK, `pubspec.yaml`, `pub`, `flutter_bloc`, `riverpod`, or `build_runner`. The entire project has permanently transitioned to Kotlin.

### Rule 1: No Brittle Code Generation
- Do NOT introduce libraries requiring external CLI generation loops (like Flutter's `build_runner`).
- Use **Kotlin Symbol Processing (KSP)** for Room and Hilt, which executes natively during normal Gradle compilation (`./gradlew assembleDebug`).

### Rule 2: Strict Feature Modularization
The codebase must be structured by feature, not by technical layers:
```
com.fluxdone / com.fluxfoxus
├── core/
│   ├── designsystem/     // Theme tokens, Typography, Icons, Reusable Atoms
│   ├── database/         // Room Database instance, migrations, base DAOs
│   ├── ipc/              // BroadcastReceivers, Intent filters, sync contracts
│   └── util/             // Coroutine dispatchers, formatters, date utilities
└── features/
    ├── feature_a/        // UI screens, ViewModels, Domain Use Cases, Repositories
    └── feature_b/
```

### Rule 3: Deterministic Terminal Execution
When verifying builds, agents must run:
```bash
# Verify compilation and run unit tests:
./gradlew testDebugUnitTest

# Assemble APK for mobile testing:
./gradlew assembleDebug

# For Ubuntu Desktop builds (Compose Multiplatform):
./gradlew run
```

### Rule 4: Preserving Offline Integrity
Network calls must never be embedded directly inside local UI repositories. All data operations hit the local Room SQLite database first. Cloud sync (Phase 2) is strictly a separate background synchronization layer.

### Rule 5: Zero Hardcoded Data Policy & Branch-Testing Isolation Workflow
> [!IMPORTANT]
> **Strict Anti-Mocking Guardrail:** Under no circumstances should production application code (`@Composable` screens, ViewModels, Repositories, DAOs, Services) contain hardcoded dummy strings, static lists (e.g. `listOf("Olympiad Math", ...)`), fake metric literals (e.g. `"18 Days"`, `"2h 53m"`), or stubbed database responses. AI coding agents have a notorious tendency to insert hardcoded mocks to bypass data plumbing—this is strictly forbidden.

All UI screens must reactively collect state from genuine Kotlin `StateFlow` emitted by Room DAOs, DataStore preferences, Android `UsageStatsManager`, or native OS broadcast streams.

**The Branch-Testing Isolation Workflow:**
If hardcoded test fixtures or mock stubs are required for isolated visual or algorithmic verification:
1. **Branch Out:** Check out an isolated testing branch (`git checkout -b test/feature-name`).
2. **Inject Test Fixture:** Apply temporary static fixtures only on that isolated branch.
3. **Verify Execution:** Run unit tests or visual inspection.
4. **Clean & Wire Dynamic Flow:**
   - **If successful:** Completely remove/revert all hardcoded data, wire the component to the live Room DAO / Repository pipeline, verify with dynamic data, and only then merge into `main`.
   - **If failed:** Revert to the clean unmocked state, diagnose the dynamic flow logic, re-branch, and repeat.
5. **Zero Mocks in Main:** No mock/stub literals are ever permitted to be committed into `main`.

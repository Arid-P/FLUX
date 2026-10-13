# 🚀 Topic SEC-04: AI Development Workflow & Native APK Delivery Protocol

**Document ID:** `SEC_04_DEVELOPMENT_AND_APK_DELIVERY_PROTOCOL`  
**Phase:** Security, Authentication & System Validation Suite  
**Role:** Autonomous Senior Android & Multiplatform Lead Architect  
**Applicability:** Universal across both **FluxDone (FD)** and **FluxFoxus (FF)** Development Repositories  

---

## 1. Core Operating Principles

This specification defines the **non-negotiable operating protocol** for AI development agents implementing the Flux App Family (`FluxDone` and `FluxFoxus`). It guarantees architectural cohesion, rigorous automated testing, and a frictionless review workflow for the user.

---

## 2. Non-Negotiable Development Directives

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                         AI DEVELOPMENT EXECUTION PIPELINE                                   |
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                             |
|   1. PHASE-WIDE PRE-PLANNING INVARIANT                                                      |
|      -> Formulate the complete implementation plan for the ENTIRE phase at once             |
|      -> Specify automated unit, DAO, and UI tests at EVERY step (NO review pauses)          |
|                                │                                                            |
|                                ▼                                                            |
|   2. AUTONOMOUS IMPLEMENTATION & TESTING                                                    |
|      -> Implement modules sequentially per documentation/ specifications                   |
|      -> Execute `./gradlew test` and `./gradlew connectedCheck` at each milestone           |
|                                │                                                            |
|                                ▼                                                            |
|   3. NATIVE APK BUILD & DIRECT CURL DELIVERY                                                |
|      -> Build production debug APK: `./gradlew assembleDebug`                               |
|      -> Provide direct `curl` command to download .apk onto user's laptop                   |
|      -> STRICTLY ZERO WEB REVIEWS OR WEB-BASED PREVIEWS                                     |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```

### Directive 1: Phase-Wide Pre-Planning Invariant
* **Before writing any production code for a phase**, the AI developer agent must generate a complete, end-to-end plan for the **entire phase at once**.
* The plan must include:
  1. Complete dependency order and file tree.
  2. Data models, Room migrations, and DAOs.
  3. MVI ViewModel states, intents, and screen components.
  4. **Automated testing contracts at each step:** Unit tests for business logic, in-memory Room DAO tests for data persistence, and Compose UI tests.
* The agent must **not** pause for user review between individual steps within a phase; it plans the whole phase, executes the plan, and verifies tests at each step.

### Directive 2: Canonical Documentation Path Mapping (`documentation/` $\to$ `documentation/`)
* In the active development repository where implementation occurs, the specification directory is named **`documentation/`** (matching the content of `documentation/` in the prompts repository).
* **Path Translation Rule:** Any specification reference to `documentation/XX_DIR/FILE.md` maps directly to `documentation/XX_DIR/FILE.md` in the development repository.
* The AI must strictly read from and adhere to `documentation/` for all architectural contracts, tokens, and invariants.

### Directive 3: Zero Web Reviews & Native APK Delivery Invariant
* **Strictly Zero Web-Based Reviews:** The AI must **never** launch web preview servers, browser mockups, or web-based simulators to review Android mobile features.
* **Physical Device Review via Direct APK Delivery:**
  * After developing and passing all automated test suites for a phase, the AI must compile the native Android APK:
    ```bash
    ./gradlew assembleDebug
    ```
  * The AI must then make the compiled APK easily downloadable by the user and provide a **clear, copy-pasteable `curl` command** so the user can download the `.apk` directly onto their laptop:
    ```bash
    # Example APK Delivery Command provided by AI to user:
    curl -o fluxdone-phase1.apk http://localhost:8080/fluxdone-debug.apk
    ```
  * Or provide the exact absolute path to the generated APK on the local filesystem:
    ```bash
    # Direct local path:
    app/build/outputs/apk/debug/app-debug.apk
    ```

---

## 3. Automated Testing Gates at Each Development Step

At every stage of the phase plan, the AI must implement and run automated tests before proceeding to the next step:

```
Step 1: Domain & Model Layer    ──► Run Unit Tests (100% Pass)
Step 2: Room SQLite DAOs        ──► Run Room In-Memory Tests (100% Pass)
Step 3: Repositories & MVI      ──► Run ViewModel StateFlow Tests (100% Pass)
Step 4: Jetpack Compose UI      ──► Run Compose Component Tests (100% Pass)
Step 5: Full Phase Integration  ──► Build APK & Provide curl Download
```

### 3.1 Test Requirements by Layer

1. **Room DAO Tests (`androidx.room.testing`):**
   * Verifies cascading deletions, constraints, and atomic transactions.
   * **Mandatory Subtask Cascade Test:** Verifies that completing a parent task atomically sets all child subtasks to `is_completed = 1`.
2. **ViewModel StateFlow Tests (`kotlinx.coroutines.test`):**
   * Verifies MVI state transitions using `StandardTestDispatcher` and `Turbine`.
3. **Boundary Validation Tests:**
   * Verifies that invalid inputs, malicious LaTeX, or out-of-bounds timestamps are rejected before reaching SQLite.

---

## 4. Final Phase Review Checklist for User

When presenting the completed phase to the user, the AI must summarize:

1. **Phase Deliverables Summary:** List of implemented screens, DAOs, services, and features.
2. **Automated Test Results:** Total unit and instrumented tests passed (e.g. `48 / 48 tests passed`).
3. **APK Download `curl` Command:** The exact command allowing the user to download the compiled `.apk` to their laptop for physical testing.
4. **Physical Device Smoke-Test Checklist:** 3 to 5 clear manual steps for the user to try on their phone.

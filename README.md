# Flux App Family — Architecture, Specifications & Development Suite

**Target Platforms:** Native Android (API 26+) & Ubuntu Linux Workstation (JVM 17+, Compose Multiplatform)  
**Role:** Canonical Master Architecture & Developer Prompt Specifications  
**Repository Branch Strategy:**  
- **`main`:** Development repository containing `documentation/`, `Theme/`, and future application source code.
- **`discussion`:** Pure documentation & planning branch preserving the complete Table of Contents ([`CURRENT_TOC.md`](file:///home/hp/Flux_Prompts/CURRENT_TOC.md)) for future feature discussions (Phase 5 Web Portal, P_Theme).

---

## 📁 Repository Directory Structure

```
├── documentation/                     # Canonical Multi-Agent Architecture Specifications
│   ├── 00_CORE/                       # Global guardrails, system context & design tokens
│   │   ├── FLUX_SYSTEM_CONTEXT.md
│   │   ├── FLUX_DESIGN_TOKENS.md
│   │   └── FLUX_AGENT_SKILLS_GUIDE.md
│   │
│   ├── 01_INTEGRATION_SYNC/           # Cross-device & inter-app contracts
│   │   ├── IPC_SPEC_FD_FF_NATIVE.md
│   │   ├── LINUX_UBUNTU_DAEMON_SPEC.md
│   │   └── CENTRAL_SYNC_ROADMAP.md
│   │
│   ├── 02_FLUXDONE_FD/                # FluxDone: Task & Habit Management Suite
│   │   ├── Phase 1/                   # Mobile MVP Foundation (6 Specs + PRD/TRD/UI/GMP)
│   │   ├── Phase 2/                   # Power Features & Core System (6 Specs + GMP)
│   │   ├── Phase 3/                   # Ubuntu Linux Desktop Companion (5 Specs + GMP)
│   │   └── Phase 4/                   # Cloud Sync, Ecosystem & AI Notes (4 Specs + GMP)
│   │
│   ├── 03_FLUXFOXUS_FF/               # FluxFoxus: Discipline & Focus Enforcer Suite
│   │   ├── Phase 1/                   # Core Session Engine & Presets (7 Specs + PRD/TRD/UI/GMP)
│   │   ├── Phase 2/                   # Hardened Anti-Bypass & Schedules (5 Specs + GMP)
│   │   ├── Phase 3/                   # Focus Score & Session Ergonomics (5 Specs + GMP)
│   │   └── Phase 4/                   # Cross-Device Daemon & Smart Limits (5 Specs + GMP)
│   │
│   └── 04_SECURITY_AND_VALIDATION/    # Security, Validation & AI Development Protocol
│       ├── SEC_01_SECURE_AUTHENTICATION_AND_IDENTITY_SPEC.md
│       ├── SEC_02_CRYPTOGRAPHIC_SECURITY_AND_DATA_ENCRYPTION_SPEC.md
│       ├── SEC_03_INPUT_VALIDATION_AND_DATA_DEFENSE_SPEC.md
│       └── SEC_04_DEVELOPMENT_AND_APK_DELIVERY_PROTOCOL.md
│
├── Theme/                             # Theme Palette Photographic References
│   ├── Холст.jpeg                     # Source palette for FluxFoxus Holst dark theme
│   ├── Rustic Medley.jpeg             # Alternative rustic palette
│   ├── starla _ colour palette.jpeg   # Vibrant accent palette
│   ├── How to use a lot of color...   # High-density swatch reference
│   └── _.jpeg                         # Contrast swatch reference
│
└── CURRENT_TOC.md                     # Master Status & Phase Table of Contents
```

---

## 🛠️ Non-Negotiable AI Developer Directives

Before writing code for any phase, the AI developer agent must strictly follow [`SEC_04_DEVELOPMENT_AND_APK_DELIVERY_PROTOCOL.md`](file:///home/hp/Flux_Prompts/documentation/04_SECURITY_AND_VALIDATION/SEC_04_DEVELOPMENT_AND_APK_DELIVERY_PROTOCOL.md):

1. **Phase Pre-Planning Invariant:** Plan the entire phase at once with automated testing specifications at each step (unit, in-memory Room DAO, and UI tests). Do not pause for intermediate reviews.
2. **Path Mapping Rule:** All specifications live under `documentation/`.
3. **Zero Web Reviews & Native APK Delivery Invariant:** Strictly zero web previews or browser simulators. Upon phase completion and automated testing, compile the native Android APK (`./gradlew assembleDebug`) and provide the user with a direct, copy-pasteable `curl` command to download the `.apk` directly to their laptop for physical device testing.
4. **Zero Flutter / Zero Dart / Zero Mocks:** 100% native Kotlin (Jetpack Compose & Compose Multiplatform). Zero mock data in production `main`.

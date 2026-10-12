# Flux App Family — Architecture & Prompt Suite (v2.0)

**Version:** 2.0 (Native Kotlin + Jetpack Compose Overhaul)  
**Target Hardware:** Android (Mobile) & Ubuntu Linux (Desktop Workstation)  
**Author:** Ari  
**Status:** Canonical Multi-Agent Specification Suite

---

## Directory Overview & Reading Order for AI Agents

To guarantee successful execution and zero architectural drift, AI coding agents must read files in the exact sequence outlined below:

```
prompts_v2/
├── 00_CORE/                       # 1. Foundation & Global Guardrails
│   ├── FLUX_SYSTEM_CONTEXT.md      # Master context: Ari persona, dual-device setup, architectural pivot
│   ├── FLUX_DESIGN_TOKENS.md       # Authoritative design tokens (Rustic Medley + 7 Academic Domain Colors)
│   └── FLUX_AGENT_SKILLS_GUIDE.md  # Toolchain, JDK 17/21, Android SDK 34, KSP, and Gradle rules
│
├── 01_INTEGRATION_SYNC/           # 2. Cross-Device & Inter-App Contracts
│   ├── IPC_SPEC_FD_FF_NATIVE.md    # Android Explicit Intent & BroadcastReceiver IPC contract
│   ├── LINUX_UBUNTU_DAEMON_SPEC.md # FluxFoxus Ubuntu Linux daemon & Compose Desktop architecture
│   └── CENTRAL_SYNC_ROADMAP.md     # Phase 2 Central DB (Supabase/Postgres) & Web Portal sync
│
├── 02_FLUXDONE_FD/                # 3. FluxDone (Task & Habit Management)
│   ├── FD_PRD_V2.md                # Product requirements (TickTick parity, calendar timeline tapping)
│   ├── FD_TRD_KOTLIN.md            # Technical requirements (Compose UI, Room SQLite, StateFlow)
│   ├── FD_UI_SPECIFICATIONS.md     # Screen specs SCR-01 to SCR-13
│   └── GMP_FD_BUILD.md             # Grand Master Prompt for building FluxDone
│
└── 03_FLUXFOXUS_FF/               # 4. FluxFoxus (Discipline & Focus Enforcer)
    ├── FF_PRD_V2.md                # Product requirements (Regain replacement, presets, flip clock)
    ├── FF_TRD_KOTLIN.md            # Technical requirements (Accessibility, UsageStats, Overlays)
    ├── FF_UI_SPECIFICATIONS.md     # Screen specs in Rustic Medley palette
    └── GMP_FF_BUILD.md             # Grand Master Prompt for building FluxFoxus
```

---

## Core Architectural Guardrails for AI Agents

1. **Zero Flutter / Zero Dart:** The entire suite is strictly native **Kotlin, Jetpack Compose, and Compose Multiplatform**.
2. **Zero `build_runner`:** Code generation is handled natively via **KSP (Kotlin Symbol Processing)** in Gradle.
3. **Offline-First Room Storage:** Local SQLite operations never rely on cloud availability.
4. **Authoritative Design System:**
   - **FluxFoxus:** Strictly **Rustic Medley** (`#13191F`, `#2B2F2E`, `#594C3D`, `#906D4B`, `#CA9C68`). Prohibits purples, blues, cyans.
   - **FluxDone:** The **7 locked academic domain colors** (`#2E7D32`, `#1565C0`, `#43A047`, `#FB8C00`, `#E64A19`, `#E53935`, `#576481`) must be used for Olympiad and JEE lists.

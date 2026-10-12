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
│   ├── FLUX_DESIGN_TOKENS.md       # Authoritative design tokens (Holst Theme + 7 Academic Domain Colors)
│   └── FLUX_AGENT_SKILLS_GUIDE.md  # Toolchain, JDK 17/21, Android SDK 34, KSP, and Gradle rules
│
├── 01_INTEGRATION_SYNC/           # 2. Cross-Device & Inter-App Contracts
│   ├── IPC_SPEC_FD_FF_NATIVE.md    # Android Explicit Intent & BroadcastReceiver IPC contract
│   ├── LINUX_UBUNTU_DAEMON_SPEC.md # FluxFoxus Ubuntu Linux daemon & Compose Desktop architecture
│   └── CENTRAL_SYNC_ROADMAP.md     # Phase 2 Central DB (Supabase/Postgres) & Web Portal sync
│
├── 02_FLUXDONE_FD/                # 3. FluxDone (Task & Habit Management)
│   └── Phase 1/                   # Phase 1 Core Specs
│       ├── FD_PRD_V2.md            # Product requirements (TickTick parity, calendar timeline tapping)
│       ├── FD_TRD_KOTLIN.md        # Technical requirements (Compose UI, Room SQLite, StateFlow)
│       ├── FD_UI_SPECIFICATIONS.md # Screen specs SCR-01 to SCR-13
│       └── GMP_FD_BUILD.md         # Grand Master Prompt for building FluxDone
│
└── 03_FLUXFOXUS_FF/               # 4. FluxFoxus (Discipline & Focus Enforcer)
    ├── Phase 1/                   # Phase 1 Core Specs (100% Complete)
    │   ├── FF_PRD_V2.md            # Product requirements (Regain replacement, presets, flip clock)
    │   ├── FF_TRD_KOTLIN.md        # Technical requirements (Accessibility, UsageStats, Overlays)
    │   ├── FF_UI_SPECIFICATIONS.md # Screen specs in Holst palette (5 bottom tabs)
    │   ├── FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md # Home Dashboard, 24h Momentum & Daily Streak Engine
    │   ├── FF_P1_PRESETS_AND_FOCUS_SPEC.md # Presets list, Floating Island selector & Standalone Planner
    │   ├── FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md # 3D Flip Clock, 2-row controls, Break mode & completion
    │   ├── FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md # App Limits, 5/10/20/40m Overlay & YouTube Study Mode
    │   ├── FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md # Stats Screen, App Detail Page, Focus Ratio & Glance Widget
    │   ├── FF_P1_PRESET_EDITOR_AND_WHITELISTS_SPEC.md # Preset Editor, Multi-State Filtering & Whitelists
    │   ├── FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md # Permissions Checklist, OEM Guides & System Health
    │   └── GMP_FF_BUILD.md         # Grand Master Prompt for building FluxFoxus P1
    │
    ├── Phase 2/                   # Phase 2 Hardened Enforcement Specs (100% Complete)
    │   ├── FF_P2_UNINSTALL_PROTECTION_SPEC.md # Device Admin & Anti-Tamper Guard (Topic FF-P2-1)
    │   ├── FF_P2_SCHEDULED_SESSIONS_SPEC.md   # Exact Alarms & Automatic Start Calendar (Topic FF-P2-2)
    │   ├── FF_P2_BREAK_NEGOTIATION_SPEC.md    # AI Break Negotiation & Stamina Protocol (Topic FF-P2-3)
    │   ├── FF_P2_STREAK_GRACE_SPEC.md         # Stacking Grace Shields & Calendar Audit (Topic FF-P2-4)
    │   ├── FF_P2_POST_SESSION_REFLECTION_SPEC.md # 3-Tile Flow Rating & Notification Actions (Topic FF-P2-5)
    │   └── GMP_FF_P2_BUILD.md                 # Grand Master Prompt for building FluxFoxus P2
    │
    ├── Phase 3/                   # Phase 3 Intelligence & Session Ergonomics (100% Complete)
    │   ├── FF_P3_FOCUS_SCORE_SPEC.md          # 0-100 Daily Focus Score Engine (Topic FF-P3-1)
    │   ├── FF_P3_SESSION_TEMPLATES_SPEC.md    # Session Blueprints & Auto-Scheduling (Topic FF-P3-2)
    │   ├── FF_P3_LOCK_SCREEN_STATUS_SPEC.md   # Live Lock Screen Ambient Chronometer (Topic FF-P3-3)
    │   ├── FF_P3_AI_COACHING_SPEC.md          # AI Focus Coaching & Granular Settings Hub (Topic FF-P3-4)
    │   ├── FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC.md # YouTube Shorts Guard, WakeLock & Polish (Topic FF-P3-5)
    │   └── GMP_FF_P3_BUILD.md                 # Grand Master Prompt for building FluxFoxus P3
    │
    └── Phase 4/                   # Phase 4 Daemon, Group Limits & Habit Breaking (100% Complete)
        ├── FF_P4_FOCUS_SCORE_AI_ENHANCEMENT_SPEC.md # Focus Score 5th Component & AI Task Alignment (Topic FF-P4-1)
        ├── FF_P4_PRE_OPEN_FRICTION_SPEC.md          # Pre-Open Habit Friction Screen & Breathing Timer (Topic FF-P4-2)
        ├── FF_P4_CATEGORY_LIMITS_SPEC.md            # Category Pools & Shared Intervention (Topic FF-P4-3)
        ├── FF_P4_SMART_LIMIT_SUGGESTIONS_SPEC.md    # Smart Usage Anomaly Detection & Sunday Report (Topic FF-P4-4)
        ├── FF_P4_CROSS_DEVICE_SYNC_SPEC.md          # Cross-Device Sync Engine & Linux Daemon (Topic FF-P4-5)
        └── GMP_FF_P4_BUILD.md                       # Grand Master Prompt for building FluxFoxus P4
```

---

## Core Architectural Guardrails for AI Agents

1. **Zero Flutter / Zero Dart:** The entire suite is strictly native **Kotlin, Jetpack Compose, and Compose Multiplatform**.
2. **Zero `build_runner`:** Code generation is handled natively via **KSP (Kotlin Symbol Processing)** in Gradle.
3. **Offline-First Room Storage:** Local SQLite operations never rely on cloud availability.
4. **Authoritative Design System:**
   - **FluxFoxus:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`). Sourced directly from `Theme/Холст.jpeg`.
   - **FluxDone:** The **7 locked academic domain colors** (`#2E7D32`, `#1565C0`, `#43A047`, `#FB8C00`, `#E64A19`, `#E53935`, `#576481`) must be used for Olympiad and JEE lists.

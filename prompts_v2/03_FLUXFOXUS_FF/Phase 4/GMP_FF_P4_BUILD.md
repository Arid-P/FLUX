# 🚀 GMP — Phase 4 Grand Master Prompt: FluxFoxus (FF) Build Instructions

**Version:** 4.0 (Native Android Kotlin + Compose Multiplatform Linux Phase 4 Master Build Specification)  
**Role:** Autonomous AI Senior Android System, Desktop & Security Engineer  
**Target:** Android Application (`com.flux.focus`) + Linux Desktop Companion & System Daemon (`com.flux.focus.desktop`)  
**Prerequisites:**  
- Phase 1 Complete Suite ([`GMP_FF_BUILD.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/GMP_FF_BUILD.md))  
- Phase 2 Complete Suite ([`GMP_FF_P2_BUILD.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%202/GMP_FF_P2_BUILD.md))  
- Phase 3 Complete Suite ([`GMP_FF_P3_BUILD.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/GMP_FF_P3_BUILD.md))  

---

## 0. Non-Negotiable Operational Rules

1. **Zero Flutter / Zero Dart:** 100% Native Android Kotlin (Kotlin 2.x + Jetpack Compose + Room + Hilt) for Android; Compose Multiplatform for Desktop on Ubuntu Linux.
2. **Zero Mocks in Production:** All components must bind to genuine system services (`AccessibilityService`, `UsageStatsManager`, `WorkManager`, systemd, D-Bus, Room DAOs, DataStore). Stubs and fake data are strictly forbidden in production code. Use the Branch-Testing Isolation Workflow for testing.
3. **Strict Holst (Холст) Dark Palette:** Sourced directly from `Theme/Холст.jpeg`:
   * `#0D1B2A` (Abyssal Navy Background), `#1B263B` (Oxford Slate Surface), `#415A77` (Muted Steel Blue Border), `#778D7A` (Muted Sage Green Accent), `#D4C4A8` (Warm Sand Khaki Primary Action), `#F4F1DE` (Eggshell Cream Text), `#E07A5F` (Coral Danger).
4. **Autonomous Execution Order:** Implement Phase 4 modules sequentially:
   - Module 1: Focus Score 5th Component & AI Task Alignment (`FF_P4_FOCUS_SCORE_AI_ENHANCEMENT_SPEC.md`)
   - Module 2: Pre-Open Habit Friction Screen (`FF_P4_PRE_OPEN_FRICTION_SPEC.md`)
   - Module 3: Category & Group Limits (`FF_P4_CATEGORY_LIMITS_SPEC.md`)
   - Module 4: Smart Usage Anomaly Detection & Limit Suggestions (`FF_P4_SMART_LIMIT_SUGGESTIONS_SPEC.md`)
   - Module 5: Cross-Device Sync Engine & Linux Focus Daemon (`FF_P4_CROSS_DEVICE_SYNC_SPEC.md`)

---

## 1. Module 1: Focus Score 5th Component & AI Task Alignment

* **Specification:** [`FF_P4_FOCUS_SCORE_AI_ENHANCEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%204/FF_P4_FOCUS_SCORE_AI_ENHANCEMENT_SPEC.md)
* **Tasks:**
  1. Recalibrate Focus Score weights: Time 40, Breaks 20, Limits 20, Completion 15, Task Alignment 5 (Sum = 100).
  2. Implement Standalone Mode fallback (default neutral 5.0/5.0 marks when FluxDone is not installed or 0 tasks planned).
  3. Implement IPC Bridge query to FluxDone for planned vs completed tasks.
  4. Implement Gemini AI contextual evaluator for incomplete/Won't Do tasks with notes; store 40-word analytical reasoning in `daily_focus_scores`.
  5. Update `FocusScoreCard` UI on `Tab.Stats` with the 5th component progress bar and expandable AI explanation card.

---

## 2. Module 2: Pre-Open Habit Friction Screen

* **Specification:** [`FF_P4_PRE_OPEN_FRICTION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%204/FF_P4_PRE_OPEN_FRICTION_SPEC.md)
* **Tasks:**
  1. Implement `PreOpenFrictionOverlayActivity` triggered by `FocusAccessibilityService` on target app launch before daily budget exhaustion.
  2. Build expanding/contracting mindful breathing circle animation (4s inhale / 4s exhale) in Compose.
  3. Implement 5-second countdown timer with asymmetric buttons: `[ ↩️ Step Back ]` is active at second 1; `[ Open App ]` remains disabled and locked until second 0.
  4. Implement Sequential Handover: If daily budget is already exceeded, skip friction and immediately show 5/10/20/40m intervention overlay.
  5. Add Settings toggle in `Settings → Habit Friction → Pre-Open Breathing Screen` (ON by default).

---

## 3. Module 3: Category & Group Limits

* **Specification:** [`FF_P4_CATEGORY_LIMITS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%204/FF_P4_CATEGORY_LIMITS_SPEC.md)
* **Tasks:**
  1. Create `category_limits` Room entity and DAO.
  2. Enforce Strict Mutual Exclusivity: An app has either an Individual Limit or is a Category Pool Member.
  3. Build Mutual Exclusivity Migration Dialog when adding an individually-limited app into a Category Pool.
  4. Implement Real-Time Aggregated Enforcement in `FocusAccessibilityService` summing daily minutes across all category member apps.
  5. Build Shared Category Cards and Extra Time Token logic (unlocks all member apps during token window) on `Tab.Limits`.

---

## 4. Module 4: Smart Usage Anomaly Detection & Limit Suggestions

* **Specification:** [`FF_P4_SMART_LIMIT_SUGGESTIONS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%204/FF_P4_SMART_LIMIT_SUGGESTIONS_SPEC.md)
* **Tasks:**
  1. Create `smart_limit_suggestions` Room entity and DAO.
  2. Implement Deterministic Tightening Rule (usage $\le 60\%$ on 14+ of 30 days) and Loosening Rule (breaches on 10+ of 14 days, non-distracting only).
  3. Enforce Discipline-First priority order (Tighten strictly before Loosen) and cap output at top 3 suggestions.
  4. Enforce 14-day anti-nagging cooldown per package.
  5. Schedule `SundayWeeklyReportWorker` at Sunday 00:05 AM to populate suggestions.
  6. Build `SmartLimitSuggestionsSection` in Sunday Weekly Report on `Tab.Stats` with 1-tap `[ Apply ]` and `[ Dismiss ]`.
  7. Register `FEATURE_SMART_LIMIT_SUGGESTIONS` toggle in `/settings/ai`.

---

## 5. Module 5: Cross-Device Sync Engine & Linux Focus Daemon

* **Specification:** [`FF_P4_CROSS_DEVICE_SYNC_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%204/FF_P4_CROSS_DEVICE_SYNC_SPEC.md)
* **Tasks:**
  1. Build Linux systemd service `fluxfoxus-daemon` with dual-tier `/etc/hosts` backup (`/etc/hosts.flux.orig` baseline + `/etc/hosts.flux.bak` pre-session backup).
  2. Implement demarcated `FLUXFOXUS_SESSION_BLOCK_...` injection and automated post-session DNS flush (`resolvectl flush-caches`).
  3. Implement D-Bus / window manager desktop app guard for Steam, Discord, and distracting desktop apps.
  4. Implement Google Account authentication via Android Credential Manager and Linux OAuth 2.0 PKCE loopback receiver.
  5. Build Hybrid Sync engine: Low-latency mDNS LAN broadcast (<20ms) + Google Firebase Realtime Cloud listener (<300ms) with Active Discipline Dominance.
  6. Build Compose Multiplatform Desktop Focus HUD with floating timer, break controls, and Holst dark theme styling.

---

## 6. Build Verification Gate
Execute complete test suite across Android and Linux desktop companions:
```bash
# Android verification
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# Desktop companion verification
./gradlew :desktop:test
./gradlew :desktop:packageDeb
```
Verify zero lint warnings, zero mock dependencies in production modules, and pristine `/etc/hosts` restoration upon test tear-down.

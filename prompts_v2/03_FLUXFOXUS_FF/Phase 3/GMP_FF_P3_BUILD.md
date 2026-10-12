# 🚀 GMP — Phase 3 Grand Master Prompt: FluxFoxus (FF) Build Instructions

**Version:** 3.0 (Native Android Kotlin Phase 3 Master Build Specification)  
**Role:** Autonomous AI Senior Android System & UI Engineer  
**Target:** Android Application (`com.flux.focus`)  
**Prerequisites:** Phase 1 Complete Suite ([`GMP_FF_BUILD.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/GMP_FF_BUILD.md)), Phase 2 Complete Suite ([`GMP_FF_P2_BUILD.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%202/GMP_FF_P2_BUILD.md))  

---

## 0. Non-Negotiable Operational Rules

1. **Zero Flutter / Zero Dart:** 100% Native Android Kotlin (Kotlin 2.x + Jetpack Compose + Room + Hilt + Coroutines/StateFlow).
2. **Zero Mocks in Production:** All components must bind to genuine Android system services (`UsageStatsManager`, `NotificationManager`, `PowerManager`, Room DAOs, DataStore). Stubs and fake data are forbidden in production code. Use the Branch-Testing Isolation Workflow for testing.
3. **Strict Holst (Холст) Dark Palette:** Sourced directly from `Theme/Холст.jpeg`:
   * `#0D1B2A` (Abyssal Navy Background), `#1B263B` (Oxford Slate Surface), `#415A77` (Muted Steel Blue Border), `#778D7A` (Muted Sage Green Accent), `#D4C4A8` (Warm Sand Khaki Primary Action), `#F4F1DE` (Eggshell Cream Text), `#E07A5F` (Coral Danger).
4. **Autonomous Execution Order:** Implement Phase 3 modules sequentially:
   - Module 1: Focus Score Engine (`FF_P3_FOCUS_SCORE_SPEC.md`)
   - Module 2: Session Templates & Auto-Scheduling (`FF_P3_SESSION_TEMPLATES_SPEC.md`)
   - Module 3: Lock Screen Live Session Status (`FF_P3_LOCK_SCREEN_STATUS_SPEC.md`)
   - Module 4: AI Coaching & Granular AI Hub (`FF_P3_AI_COACHING_SPEC.md`)
   - Module 5: Edge Cases, Battery Optimizations & Polish (`FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC.md`)

---

## 1. Module 1: Focus Score Engine (0–100 Daily Metric)

* **Specification:** [`FF_P3_FOCUS_SCORE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_FOCUS_SCORE_SPEC.md)
* **Tasks:**
  1. Create `daily_focus_scores` Room entity and DAO.
  2. Implement mathematical formula combining: Time Goal (40%), Break Discipline (25%), Limit Compliance (20%), Completion Rate (15%).
  3. Implement Advanced Customization Sliders in Settings with live sum-to-100 rebalancing algorithm.
  4. Implement Checkpoint Days system (Wed & Sat default) with 9:00 PM rollup notifications.
  5. Build `FocusScoreCard` Composable with circular gauge in Holst theme on `Tab.Stats`.

---

## 2. Module 2: Session Templates & Auto-Scheduling

* **Specification:** [`FF_P3_SESSION_TEMPLATES_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_SESSION_TEMPLATES_SPEC.md)
* **Tasks:**
  1. Create `session_templates` Room entity and DAO.
  2. Seed database with 3 starter templates (`Pomodoro Sprint`, `Deep Focus Block`, `Olympiad Marathon`).
  3. Implement deterministic calendar gap-finding math (`AutoScheduleEngine.kt`) with configurable 15m buffer.
  4. Add top Segmented Control to Planner: `[ Timeline ]` vs `[ Templates ]`.
  5. Build Proposed Schedule Review sheet with inline time adjustment and 1-tap confirmation.

---

## 3. Module 3: Lock Screen Live Focus Session Status

* **Specification:** [`FF_P3_LOCK_SCREEN_STATUS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_LOCK_SCREEN_STATUS_SPEC.md)
* **Tasks:**
  1. Create `res/layout/notification_focus_lock_screen.xml` using `android.widget.Chronometer` for zero-CPU countdown.
  2. Implement `FocusLockScreenNotificationManager` with `NotificationCompat.VISIBILITY_PUBLIC` and `CATEGORY_STOPWATCH`.
  3. Build 2 distinct visual states: Active Session vs Break Countdown.
  4. Enforce Asymmetric Action Security: Non-destructive breaks execute without unlock; Stop Session requires biometric/PIN unlock and routes to the 15–25s quit gate.
  5. Add Settings toggle in `Settings → Sessions → Lock Screen Display` (ON by default).

---

## 4. Module 4: AI Coaching & Granular Settings Hub

* **Specification:** [`FF_P3_AI_COACHING_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_AI_COACHING_SPEC.md)
* **Tasks:**
  1. Build `/settings/ai` Hub with master toggle + 5 independent feature toggles (`FEATURE_BREAK_NEGOTIATION`, `FEATURE_SESSION_DEBRIEF`, `FEATURE_PRESET_SUGGESTER`, `FEATURE_STREAK_COACH`, `FEATURE_DIFFICULTY_TUNER`).
  2. Implement Touchpoint A: Analytical Session Debrief with 1 follow-up question limit.
  3. Implement Touchpoint B: AI Preset Suggester background worker at 06:00 AM + Home recommendation chip.
  4. Implement Touchpoint C: Streak Risk Coach notification at 6:00 PM when streak is endangered.
  5. Implement Touchpoint D: Difficulty Auto-Tuner monitor triggering after 5 consecutive negotiation outcomes.

---

## 5. Module 5: Edge Cases, Battery Optimizations & System Polish

* **Specification:** [`FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_POLISH_AND_OPTIMIZATIONS_SPEC.md)
* **Tasks:**
  1. Implement YouTube Shorts accessibility tree inspection: Allow whitelisted channels, block unapproved Shorts within 800ms.
  2. Anchor all timer clocks to `SystemClock.elapsedRealtime()` for complete timezone and DST immunity.
  3. Implement Boot State `UsageStatsManager` re-querying in `BootCompletedReceiver`.
  4. Anchor `DailyStreakWorker` midnight calculations to session completion epoch timestamps to prevent flex-window drift.
  5. Implement `FocusWakeLockManager` with strict partial WakeLock lifecycle hygiene.
  6. Verify performance benchmarks: Cold start < 1.5s, timer drift < 50ms, Focus Score computation < 50ms.

---

## 6. Build Verification Gate
Execute complete test suite before concluding Phase 3:
```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```
Verify zero lint warnings and zero mock dependencies in production modules.

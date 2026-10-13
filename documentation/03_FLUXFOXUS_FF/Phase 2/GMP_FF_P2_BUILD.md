# 🚀 GMP — Phase 2 Grand Master Prompt: FluxFoxus (FF) Build Instructions

**Version:** 2.0 (Native Android Kotlin Phase 2 Master Build Specification)  
**Role:** Autonomous AI Senior Android System & UI Engineer  
**Target:** Android Application (`com.flux.focus`)  
**Prerequisites:** Phase 1 Complete Suite ([`GMP_FF_BUILD.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/GMP_FF_BUILD.md))  

---

## 0. Non-Negotiable Operational Rules

1. **Zero Flutter / Zero Dart:** 100% Native Android Kotlin (Kotlin 2.x + Jetpack Compose + Room + Hilt + Coroutines/StateFlow).
2. **Zero Mocks in Production:** All components must bind to real Android APIs (`DevicePolicyManager`, `AlarmManager`, `WorkManager`, Room DAOs, DataStore). Hardcoded strings, stubbed lists, and mock streaks are strictly banned in production code. Use the Branch-Testing Isolation Workflow for testing.
3. **Strict Holst (Холст) Dark Palette:**
   * `#0D1B2A` (Abyssal Navy Background), `#1B263B` (Oxford Slate Surface), `#415A77` (Muted Steel Blue Border), `#778D7A` (Muted Sage Green Accent), `#D4C4A8` (Warm Sand Khaki Primary Action), `#F4F1DE` (Eggshell Cream Text), `#E07A5F` (Coral Danger).
4. **Autonomous Execution Order:** Implement Phase 2 features sequentially:
   - Module 1: Device Administrator & Anti-Tamper Guard (`FF_P2_UNINSTALL_PROTECTION_SPEC.md`)
   - Module 2: Advanced Scheduled Sessions & Exact Alarms (`FF_P2_SCHEDULED_SESSIONS_SPEC.md`)
   - Module 3: AI Break Negotiation & Stamina Protocol (`FF_P2_BREAK_NEGOTIATION_SPEC.md`)
   - Module 4: Stacking Streak Grace & Calendar Audit (`FF_P2_STREAK_GRACE_SPEC.md`)
   - Module 5: Post-Session Flow Rating (`FF_P2_POST_SESSION_REFLECTION_SPEC.md`)

---

## 1. Module 1: Device Administrator & Anti-Tamper Protection

* **Specification:** [`FF_P2_UNINSTALL_PROTECTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_UNINSTALL_PROTECTION_SPEC.md)
* **Tasks:**
  1. Create `FluxFoxusDeviceAdminReceiver: DeviceAdminReceiver` binding `android.permission.BIND_DEVICE_ADMIN`.
  2. Create `res/xml/device_admin_policies.xml` declaring zero destructive policies.
  3. Implement `SecurityRepository` with Jetpack DataStore binding.
  4. Build `/settings/security` UI with `Uninstall Protection` toggle.
  5. Implement `UninstallProtectionModal` with 10-second asymmetric countdown: `[ Keep Protection ]` is always free; `[ Remove Protection ]` locked until 0s.
  6. Enforce Mid-Session Inviolability: Block deactivation while an active focus session is running.
  7. Register as Shield #5 (Optional) on the interactive Onboarding Checklist.

---

## 2. Module 2: Advanced Scheduled Sessions & RRULE Calendar Engine

* **Specification:** [`FF_P2_SCHEDULED_SESSIONS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_SCHEDULED_SESSIONS_SPEC.md)
* **Tasks:**
  1. Extend `focus_sessions` Room table with `session_origin = 'SCHEDULED'`, `series_id`, `recurrence_rule`, and `status = 'PLANNED'`.
  2. Implement `ScheduledSessionAlarmReceiver` leveraging `AlarmManager.setExactAndAllowWhileIdle()`.
  3. **Guaranteed Automatic Start:** Auto-start `FocusSessionService` on the second; lock apps via Accessibility immediately.
  4. Implement Catch-Up on Boot (`BootCompletedReceiver`) with configurable threshold (`Settings → Sessions → Boot Resume Threshold`, default 15m).
  5. Implement Smooth Handover: Convert ongoing manual sessions to scheduled sessions at the trigger minute.
  6. Build `/planner/session/create` bottom sheet with optional 15-minute heads-up toggle (no skip button) and non-blocking overlap warning.
  7. Implement Recurrence Scopes Dialog (This only / This and following / All in series).

---

## 3. Module 3: Friction Break Extension & AI Negotiation

* **Specification:** [`FF_P2_BREAK_NEGOTIATION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_BREAK_NEGOTIATION_SPEC.md)
* **Tasks:**
  1. Extend Focus Screen controls: When preset breaks reach 0, transform button into `[ Negotiate Break 🤖 ]` (or `[ Emergency Break ⚡ ]`).
  2. Integrate Google Gemini Kotlin SDK (`gemini-1.5-flash`) with API key in `EncryptedSharedPreferences`.
  3. Enforce maximum 3-round negotiation turn cap.
  4. Implement Room entity `ai_break_negotiations` and DAO.
  5. Build Deterministic Offline Fallback ("The Stamina Ratio Protocol"):
     - Continuous Focus $\ge 45$m.
     - Total Break Ratio $\le 20\%$.
     - Max 2 emergency breaks per session (hard-capped at 5 minutes).
  6. Build `AIBreakNegotiationSheet` composable with Holst theme styling.

---

## 4. Module 4: Stacking Streak Grace & Historical Calendar

* **Specification:** [`FF_P2_STREAK_GRACE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_STREAK_GRACE_SPEC.md)
* **Tasks:**
  1. Extend `streaks` Room table with `accumulated_grace_shields`, `last_shield_granted_month`, `last_shield_used_date`.
  2. Implement `DailyStreakWorker` running at 00:01 AM:
     - On the 1st of each month: `accumulated_grace_shields += 1` (stacking roll-over).
     - At midnight: Auto-consume 1 shield if yesterday missed target (covers entire 24h day).
     - Streak freezes neutrally on Sunday Relief Days.
  3. Create `streak_day_history` Room entity for daily audit records.
  4. Update Quit Friction Modal: Inform user if quitting will consume an available shield or wipe streak to 0.
  5. Build `/streaks/history` full-screen sheet with monthly calendar grid, Holst color legend, and live shield counter pill `[🔥 18 · 🛡️ 2]`.

---

## 5. Module 5: Post-Session Flow Rating

* **Specification:** [`FF_P2_POST_SESSION_REFLECTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_POST_SESSION_REFLECTION_SPEC.md)
* **Tasks:**
  1. Add `reflection_rating: String?` to `focus_sessions` table (`LOW`, `MEDIUM`, `HIGH`, or `null`).
  2. Build `PostSessionReflectionSheet` composable: 1-tap rating tiles (`😴 Low`, `😐 Medium`, `🔥 High`) + `[ Skip → ]`.
  3. Trigger sheet immediately upon natural session completion (ON by default in settings).
  4. Implement `SessionReflectionReceiver` to handle background completion notification actions (`[ 😴 Low ]` | `[ 😐 Med ]` | `[ 🔥 High ]`).
  5. Display rating badges on Planner history cards and aggregate in weekly review.

---

## 6. Build Verification Gate
Run clean compile and instrumentation tests before concluding Phase 2:
```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```
Verify zero lint warnings and zero mock dependencies in production modules.

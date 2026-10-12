# GMP — FluxFoxus (FF) Build Instructions for Antigravity

**Read this entire document before writing a single line of code.**

**Prerequisite: FluxDone (FD) must be fully built and the MethodChannel IPC bridge on FD's side must be complete before starting FF.**

---

## 0. Before You Start

You have been provided the following files. Read ALL of them:

- `FLUX_CONTEXT.md` — project context, app family overview, FD↔FF integration
- `FF_PRD_v1.0.md` — full product requirements
- `FF_TRD_v1.0.md` — full technical requirements, database schema, architecture
- `ui_design_system.md` — complete FF design system (dark-only, Electric Indigo, Soft Cyan)
- `ui_navigation.md` — bottom navigation structure (5 tabs)
- `ui_home.md` — Home screen
- `ui_focus_session.md` — Focus session, break window, stop modal
- `ui_preset.md` — Preset creation flow, channel whitelist
- `ui_app_limits.md` — App Limits (Blocks tab)
- `ui_planner.md` — Planner screen
- `ui_usage_stats.md` — Usage Stats screen
- `ui_distracted_intervention.md` — 2/5/10/20 intervention window, YouTube Study Mode
- `ui_widget.md` — Home screen widget

**Every UI spec file is locked. The app must match the specs exactly. No deviations without explicit approval from Ari.**

---

## 1. Environment Setup

Flutter and Android SDK must already be installed from the FD build. Verify:

```bash
flutter doctor -v
# All Android items should be green
```

---

## 2. Project Initialization

```bash
# Create the Flutter project
flutter create --org com.fluxfoxus --project-name fluxfoxus fluxfoxus
cd fluxfoxus

# Set minimum Android SDK to API 26 in android/app/build.gradle:
# minSdkVersion 26
# targetSdkVersion 34
```

---

## 3. Install All Dependencies

```bash
flutter pub add \
  flutter_riverpod \
  riverpod_annotation \
  sqflite \
  hive \
  hive_flutter \
  flutter_secure_storage \
  go_router \
  flutter_local_notifications \
  workmanager \
  flutter_foreground_task \
  android_alarm_manager_plus \
  fl_chart \
  google_fonts \
  flutter_svg \
  home_widget \
  permission_handler \
  rxdart \
  freezed_annotation \
  json_annotation \
  uuid \
  intl \
  path_provider

flutter pub add --dev \
  build_runner \
  riverpod_generator \
  freezed \
  json_serializable \
  hive_generator \
  mocktail

# Run code generation:
dart run build_runner build --delete-conflicting-outputs
```

---

## 4. Architecture Rules (Non-Negotiable)

### 4.1 Design System — Dark Only
FF is dark-only in Phase 1. Use the exact color values from `ui_design_system.md`. The primary palette:
- Background: `#0F172A`
- Surface: `#1E293B`
- Primary: `#6366F1` (Electric Indigo)
- Accent: `#22D3EE` (Soft Cyan)
- Text: `#F8FAFC` (Ghost White)

Never deviate from these values without instruction.

### 4.2 Modular Architecture
Each feature module (focus_timer, app_limits, streaks, planner, youtube_study_mode, fd_integration, widget) must be independently functional. A crash or data corruption in one module must not affect others. Modules communicate via the internal rxdart event bus only.

### 4.3 Offline-First
All features work without internet. The FD MethodChannel is local IPC — no network required.

### 4.4 Riverpod State Management
FF uses Riverpod (v2+) with code generation. Each feature has its own provider family. No global state singletons.

### 4.5 Storage Split
- **SQLite (sqflite):** Sessions, presets, session history, streak records, study channels
- **Hive:** User preferences (minimumSessionsPerDay, lastUsedPresetId), app categorization map, app metadata cache

---

## 5. Critical Android Permissions

Add to `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />

<service android:name=".FluxFoxusAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
  <intent-filter>
    <action android:name="android.accessibilityservice.AccessibilityService" />
  </intent-filter>
  <meta-data android:name="android.accessibilityservice"
      android:resource="@xml/accessibility_service_config" />
</service>
```

Create `android/app/src/main/res/xml/accessibility_service_config.xml`:
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100"
    android:packageNames="" />
```

---

## 6. Build Order (Follow Strictly)

Build in this exact order. Test each step before proceeding.

### Phase 1-A: Foundation

**Step 1: Design System + Theme**
- Create `lib/core/theme/` with all FF color tokens from `ui_design_system.md`
- Dark-only ThemeData
- All typography scales (type_display through type_micro)
- Spacing constants (base 4px)
- Border radius constants
- Component styles (cards, buttons, pills, toggles)

**Step 2: Database + Hive + DI**
- SQLite: all tables from TRD Section 4 (presets, preset_app_restrictions, focus_sessions, app_limits, streak_records, study_channels)
- Hive: boxes for preferences, app_categories, app_metadata
- get_it DI registration

**Step 3: Navigation Shell**
- go_router with all routes from TRD Section 6
- Floating pill bottom navigation bar per `ui_navigation.md` (5 tabs: Home, Usage, Focus, Planner, Block)
- Tab groups: [Usage+Focus] left pair, [Planner+Block] right pair, Home standalone
- Bottom nav hidden during active focus session

**Step 4: Permissions Onboarding Flow**
- First launch: permission request screens in order:
  1. POST_NOTIFICATIONS (standard runtime)
  2. PACKAGE_USAGE_STATS (opens system settings)
  3. SYSTEM_ALERT_WINDOW (opens system settings)
  4. ACCESSIBILITY_SERVICE (opens accessibility settings)
- Plain-language explanation screen before each system settings redirect

### Phase 1-B: Core Focus System

**Step 5: Preset System**
- Presets CRUD (SQLite)
- Preset creation screen per `ui_preset.md`
- Emoji picker bottom sheet
- Break configuration steppers (0–6 breaks, 1–15 min)
- App restrictions by category (4 categories, collapsible)
- YouTube mode: 3-way radio (Block/Allow/Study Mode)
- Last-used preset as default
- 60-hour confirmation nudge

**Step 6: Focus Timer + Foreground Service**
- Flutter Foreground Service via flutter_foreground_task
- 3 timer modes: Countdown (counts down), Stopwatch (counts up), Open-Ended (counts up, no end)
- Timer state: `startTimestamp` + `totalElapsedBeforePause` delta calculation (not tick-based — avoids drift)
- Timer state persisted to Hive every 5 seconds (survives app kill)
- Mechanical flip clock UI per `ui_focus_session.md`:
  - Two large rounded-square cards stacked vertically
  - HH (top card), MM (bottom card)
  - Ghost White digits in focus mode, Soft Cyan in break mode
  - AnimatedSwitcher with custom clip flip animation (300ms)
  - Horizontal split line creating mechanical flip illusion

**Step 7: Break System**
- Break countdown (Soft Cyan palette)
- Break count decrement
- Break exhaustion: greyscale Break button, non-interactive
- 0 breaks configured: Break button absent entirely
- End Break Early button
- Break end: vibration + notification + auto-return to focus session

**Step 8: Stop Focusing Modal**
- Both buttons disabled for 15–25 seconds (formula-driven wait)
- Wait time formula per TRD Section 5.3.3:
  - streak_contribution + usage_contribution, clamped 15–25
  - Productive app time excluded from usage calculation
- "Quit Session ([countdown])" text counting down live
- On Keep Going: modal closes, session resumes
- On Quit Session: streak resets to 0, session logged as manually stopped

**Step 9: Focus Session Screen**
- Full-screen takeover, bottom nav hidden
- Header: preset name pill (Electric Indigo) + session time range / "BREAK" label
- Flip clock (Steps 6–7)
- Bottom controls: Stop Focusing (left, ~50% width) + Break pill (center, Soft Cyan outline) + Pause/Play circle (right, Electric Indigo)
- Session completion (Countdown → 00:00): vibration + notification + auto-navigate to home
- Per `ui_focus_session.md` exactly

**Step 10: Streaks**
- StreakRecord table
- Daily evaluation via WorkManager at 11:59 PM
- Streak neutral days (minimum = 0)
- Streak reset on Quit Session confirmation
- Wait time formula using streak length + distracting app usage ratio

### Phase 1-C: App Blocking

**Step 11: Accessibility Service + App Detection**
- Kotlin-side AccessibilityService (FluxFoxusAccessibilityService)
- Monitor TYPE_WINDOW_STATE_CHANGED events
- Extract packageName from AccessibilityEvent
- Flutter MethodChannel bridge to Dart side
- Block enforcement during sessions: redirect to FF if distracting app opened
- App limit enforcement: show overlay when daily limit reached

**Step 12: UsageStats Integration**
- Kotlin-side UsageStatsManager wrapper
- MethodChannel: `com.fluxfoxus/usage_stats`
- `getUsageStats(startTime, endTime)` returning per-app usage in milliseconds
- 7-day rolling average calculation, stored in Hive
- App categorization: Productive / Semi-Productive / Distracting / Others
- FF ships with predefined categorization for common apps (Instagram, YouTube, TikTok = Distracting; Gmail, Maps = Semi-Productive; etc.)

**Step 13: App Limits (Blocks Tab — SCR-05 in FF)**
- App limits CRUD per `ui_app_limits.md`
- Daily limit range: 15 min to (avg daily usage + 1 hour)
- Extra time sessions: 0–6 sessions, 5/10/15 min chips, hard cap 60 min total
- Per-app streak tracking
- Turn Off Block flow: streak warning sheet → 3-second countdown → duration picker → pause record
- "Paused till [date]" badge

**Step 14: 2/5/10/20 Intervention Window**
- SYSTEM_ALERT_WINDOW overlay
- FlutterOverlayWindow or native Android WindowManager
- Blurred background (BackdropFilter + ImageFilter.blur)
- App info row (icon + daily limit text) + streak counter
- Progress bar (Soft Cyan → Electric Indigo gradient)
- 2×2 time option grid (2/5/10/20 min)
- "Exit [App Name]" button
- Wait periods on re-show: 2min→5s, 5min→10s, 10min→10s, 20min→15s
- Does NOT affect streaks
- Per `ui_distracted_intervention.md` exactly

### Phase 1-D: YouTube Study Mode

**Step 15: YouTube Study Mode**
- Channel whitelist (study_channels table)
- Accessibility Service: detect YouTube channel name from window content
- Non-whitelisted: overlay message + "Got it" / "Add to Study List"
- Channel addition: confirmation sentence flow
  - ≤25 channels: 5 short sentences pool
  - >25 channels: ~50 longer sentences pool
  - No same sentence twice in a row
- Edge grabber per `ui_distracted_intervention.md` Section 3.5:
  - Right edge, vertically centered, ~32×80dp
  - Electric Indigo pill with FF aperture logo
  - Tap to expand: Midnight Slate panel + "YouTube Study Mode" label + Soft Cyan toggle + ×

**Step 16: Channel Whitelist Screen**
- Per `ui_preset.md` Sections 11.1–11.5
- Search bar, channel list with remove buttons
- Add channel bottom sheet with sentence confirmation input

### Phase 1-E: Tracking + Planner

**Step 17: Screen Time Tracking + Usage Stats Screen**
- Per `ui_usage_stats.md`
- 3 tabs: Today (area chart), Daily (stacked bar, tap-to-highlight), Weekly (multi-week bars)
- fl_chart: smooth Bézier stacked area chart + stacked bar charts
- Layer order (bottom to top): Productive → Semi-Productive → Distracting → Others
- 2×2 category legend grid
- App list with category pills and usage time
- Weekly report: Sunday notification + dismissible summary card on Weekly tab

**Step 18: Planner Screen**
- Per `ui_planner.md`
- Horizontal day strip (Mon–Sun), scrollable
- Streak bar
- Stats row (Focus + Usage)
- Session cards: emoji + name + preset pill + source badge (FF/FD) + time range + sync icon + duration label
- Completed session: opacity 0.6, teal left border, checkmark
- Active session: live elapsed time, "Live" Soft Cyan label
- FAB: "+ Add Preset" → opens Preset creation screen

**Step 19: Home Screen**
- Per `ui_home.md`
- Header: FF aperture icon + "FluxFoxus" + two pills (Focused avg + Weekly avg)
- Momentum Card: area chart (last 24h) + 2×2 legend grid
- Session Card: today's focus time + preset selector (dropdown chevron) + session details rows
- "Start Focusing" button (Electric Indigo pill)
- Active session state: "Resume Session" in Soft Cyan
- Upcoming session banner (within 15 minutes)

### Phase 1-F: Widget + Notifications

**Step 20: Home Screen Widget**
- Per `ui_widget.md`
- home_widget package + Android XML layout (res/layout/ff_widget.xml)
- 4-segment donut ring (fl_chart), category legend, total time, Live Focus indicator
- WorkManager periodic refresh every 15 minutes
- Immediate update on session start/end
- Distracting dot: add 1px #94A3B8 border (barely visible against #1E293B background)

**Step 21: Notifications**
- Android notification channels per TRD Section 5.7.1
- 15-minute heads-up: android_alarm_manager_plus, scheduled when session created
- Session complete notification
- Weekly report: WorkManager on Sundays at 8:00 AM
- Break end: vibration + notification

### Phase 1-G: FluxFoxus Bridge (FD ↔ FF IPC)

**Step 22: MethodChannel IPC — FF Side**

This step consumes the bridge that FD already built. FF listens for incoming calls and also calls FD.

**FF receives from FD (FocusBlockRequest):**
```dart
const fdChannel = MethodChannel('com.fluxfoxus/fd_integration');

void setupFDIntegration() {
  fdChannel.setMethodCallHandler((call) async {
    switch (call.method) {
      case 'focusBlockRequest':
        final request = FocusBlockRequest.fromMap(call.arguments);
        await fdIntegrationService.handle(request);
    }
  });
}
```

**FocusBlockRequest handler:**
- CREATE: get last-used preset → create FocusSession with status=pending, source=fd, scheduledStart/End from request
- UPDATE: find session by fdTaskId → update scheduledStart, scheduledEnd, plannedDuration
- DELETE: find session by fdTaskId → delete it
- If no preset exists on CREATE: skip silently (no crash, no user notification)

**FF sends to FD (createTask):**
When a session is started from FF Home (not from FD), FF creates a "Focus Session" task in FD:
```dart
await fdChannel.invokeMethod('createTask', {
  'name': 'Focus Session',
  'description': 'Preset: ${preset.name}\nMode: ${session.mode.name}\n'
                 'Duration: ${session.plannedDuration?.inMinutes ?? "Open-ended"} min\n'
                 'Started: ${session.actualStart?.toIso8601String()}',
  'listSection': 'FF',
});
```

**FF reads FD task lists (for preset application to FD lists):**
```dart
final lists = await fdChannel.invokeMethod<List>('getTaskLists');
```

**Graceful handling:**
- If FD is not installed: all MethodChannel calls must fail silently with try/catch
- Never crash FF because FD is absent

---

## 7. Testing Requirements

After every step, verify on a physical Android device with both apps installed:

- All Accessibility Service features require a physical device (not emulator)
- UsageStats features require actual app usage data
- App blocking enforcement tested by actually opening Instagram/YouTube
- MethodChannel IPC: create a timed task in FD → verify FF receives FocusBlockRequest
- Start session from FF Home → verify "Focus Session" task appears in FD's FF section

---

## 8. Artifacts to Generate

After each major step:
1. Screenshot of completed screen
2. Browser recording for any interactive flow (tap-to-create, app blocking, etc.)
3. Any deviations from UI spec (should be none)
4. Any blockers

---

## 9. What NOT to Build in Phase 1

- iOS or Web targets
- Cloud sync or backup
- Multiplayer / social features
- Built-in focus music
- AI companion or AI-generated insights
- Lock screen widgets
- Notification-based task creation in FD from FF

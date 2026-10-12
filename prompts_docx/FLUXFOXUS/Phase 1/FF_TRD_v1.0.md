# FluxFoxus (FF) — Technical Requirements Document
**Version:** 1.0  
**Date:** March 2026  
**Platform:** Android (Phase 1)  
**Framework:** Flutter (Dart)  
**References:** FF_PRD_v1.0.md, ui_design_system.md, all ui_*.md files

---

## 1. Technology Stack

### 1.1 Core Framework
- **Language:** Dart
- **Framework:** Flutter (latest stable channel)
- **Min Android SDK:** API 26 (Android 8.0 Oreo) — required for UsageStatsManager granularity
- **Target Android SDK:** API 34 (Android 14)

### 1.2 Local Storage
| Store | Library | Purpose |
|---|---|---|
| Relational data | `sqflite` | Sessions, presets, session history, streak records |
| Key-value store | `hive` + `hive_flutter` | User preferences, app categorization map, channel whitelist, last-used preset ID |
| Secure storage | `flutter_secure_storage` | Any sensitive config (future-proofing) |

### 1.3 State Management
- **`riverpod`** (v2+) — provider-based state management
- Each feature domain has its own provider family
- No global state singletons

### 1.4 Background Processing
- **`workmanager`** — periodic background tasks (widget refresh, streak evaluation)
- **`flutter_foreground_task`** — foreground service for active focus timer
- **`android_alarm_manager_plus`** — precise scheduling for session start notifications

### 1.5 Inter-App Communication (FF ↔ FD)
- **`flutter_platform_channel`** (MethodChannel) — primary IPC with FluxDone
- Android Intent filter registration for FocusBlockRequest
- Fallback: Android broadcast receiver for FD events

### 1.6 Permissions & System APIs
- **`usage_stats`** (platform channel wrapper) — Android UsageStatsManager
- **`accessibility_service`** (platform channel) — app foreground detection, redirect enforcement
- **`system_alert_window`** (platform channel) — overlay for 2/5/10/20 window
- **`permission_handler`** — runtime permission requests

### 1.7 Notifications
- **`flutter_local_notifications`** — session heads-up (15 min), break end, session complete, weekly report

### 1.8 UI / Charts
- **`fl_chart`** — area charts, stacked bar charts, donut ring
- **`google_fonts`** — Inter font
- **`flutter_svg`** — FF aperture icon rendering
- **`home_widget`** — Android home screen widget

### 1.9 Navigation
- **`go_router`** — declarative routing with deep link support

### 1.10 Utilities
- **`freezed`** + **`json_serializable`** — immutable data models + JSON serialization
- **`drift`** (optional alternative to sqflite) — if complex SQL queries needed
- **`rxdart`** — stream composition for real-time timer and live session updates

---

## 2. Architecture

### 2.1 Pattern
**Clean Architecture** with feature-based folder structure:

```
lib/
├── core/
│   ├── constants/
│   ├── theme/
│   ├── router/
│   ├── database/
│   └── utils/
├── features/
│   ├── home/
│   ├── focus_timer/
│   ├── breaks/
│   ├── streaks/
│   ├── app_limits/
│   ├── presets/
│   ├── planner/
│   ├── usage_stats/
│   ├── youtube_study_mode/
│   ├── fd_integration/
│   └── widget/
└── shared/
    ├── models/
    ├── repositories/
    ├── widgets/
    └── services/
```

### 2.2 Layer Separation (per feature)
```
Presentation Layer  →  Riverpod Notifier / StateNotifier
Domain Layer        →  Use Cases / Interactors
Data Layer          →  Repository interfaces + implementations
                        (SQLite / Hive / UsageStats / MethodChannel)
```

### 2.3 Event Bus
- Internal event bus using `rxdart` Subject streams
- Events: SessionStarted, SessionEnded, SessionPaused, BreakStarted, BreakEnded, AppLimitHit, StreakUpdated, FDSyncReceived
- Modules subscribe to relevant events only
- No direct module-to-module dependencies

---

## 3. Data Models

### 3.1 Preset
```dart
@freezed
class Preset with _$Preset {
  const factory Preset({
    required String id,           // UUID
    required String name,         // Max 64 chars
    required String emoji,        // Single emoji character
    required int breakCount,      // 0–6
    required int breakDurationMinutes, // 1–15
    required Map<String, bool> appRestrictions, // packageName → isBlocked
    required YoutubeMode youtubeMode, // BLOCK / ALLOW / STUDY_MODE
    String? description,
    required DateTime createdAt,
    required DateTime updatedAt,
  }) = _Preset;
}

enum YoutubeMode { block, allow, studyMode }
```

### 3.2 FocusSession
```dart
@freezed
class FocusSession with _$FocusSession {
  const factory FocusSession({
    required String id,
    required String presetId,
    String? fdTaskId,             // null if FF-native session
    required String sessionName,
    required TimerMode mode,      // countdown / stopwatch / openEnded
    DateTime? scheduledStart,
    DateTime? scheduledEnd,
    Duration? plannedDuration,
    DateTime? actualStart,
    DateTime? actualEnd,
    Duration? actualFocusDuration,
    required SessionStatus status, // pending / active / completed / stopped / skipped
    required int breaksUsed,
    required SessionSource source, // ff / fd
    required DateTime createdAt,
  }) = _FocusSession;
}

enum TimerMode { countdown, stopwatch, openEnded }
enum SessionStatus { pending, active, completed, stopped, skipped }
enum SessionSource { ff, fd }
```

### 3.3 AppLimit
```dart
@freezed
class AppLimit with _$AppLimit {
  const factory AppLimit({
    required String packageName,
    required String appName,
    required AppCategory category,
    required Duration dailyLimit,         // 15 mins to avg+1hr
    required int extraSessionCount,       // 0–6
    required Duration extraSessionDuration, // per session: 5/10/15 min
    required bool isActive,
    DateTime? pausedUntil,               // null if not paused
    required int streakDays,
    required DateTime createdAt,
  }) = _AppLimit;
}

enum AppCategory { productive, semiProductive, distracting, others }
```

### 3.4 StreakRecord
```dart
@freezed
class StreakRecord with _$StreakRecord {
  const factory StreakRecord({
    required int currentStreak,
    required int longestStreak,
    required int minimumSessionsPerDay, // 0 = streak neutral
    required DateTime lastStreakDate,
    required DateTime updatedAt,
  }) = _StreakRecord;
}
```

### 3.5 StudyChannel
```dart
@freezed
class StudyChannel with _$StudyChannel {
  const factory StudyChannel({
    required String id,
    required String channelName,
    String? channelUrl,
    required DateTime addedAt,
  }) = _StudyChannel;
}
```

### 3.6 AppUsageRecord
```dart
@freezed
class AppUsageRecord with _$AppUsageRecord {
  const factory AppUsageRecord({
    required String packageName,
    required DateTime date,
    required Duration usageDuration,
    required AppCategory category,
  }) = _AppUsageRecord;
}
```

### 3.7 FocusBlockRequest (IPC)
```dart
@freezed
class FocusBlockRequest with _$FocusBlockRequest {
  const factory FocusBlockRequest({
    required String taskId,
    required String taskName,
    DateTime? startTime,
    DateTime? endTime,
    Duration? duration,
    required String listId,
    String? sectionId,
    required FocusBlockAction action,
  }) = _FocusBlockRequest;
}

enum FocusBlockAction { create, update, delete }
```

---

## 4. Database Schema (SQLite via sqflite)

### 4.1 Table: presets
```sql
CREATE TABLE presets (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  emoji TEXT NOT NULL DEFAULT '⏳',
  break_count INTEGER NOT NULL DEFAULT 0,
  break_duration_minutes INTEGER NOT NULL DEFAULT 5,
  youtube_mode TEXT NOT NULL DEFAULT 'study_mode',
  description TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
```

### 4.2 Table: preset_app_restrictions
```sql
CREATE TABLE preset_app_restrictions (
  preset_id TEXT NOT NULL,
  package_name TEXT NOT NULL,
  is_blocked INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (preset_id, package_name),
  FOREIGN KEY (preset_id) REFERENCES presets(id) ON DELETE CASCADE
);
```

### 4.3 Table: focus_sessions
```sql
CREATE TABLE focus_sessions (
  id TEXT PRIMARY KEY,
  preset_id TEXT NOT NULL,
  fd_task_id TEXT,
  session_name TEXT NOT NULL,
  mode TEXT NOT NULL,
  scheduled_start INTEGER,
  scheduled_end INTEGER,
  planned_duration_seconds INTEGER,
  actual_start INTEGER,
  actual_end INTEGER,
  actual_focus_seconds INTEGER,
  status TEXT NOT NULL DEFAULT 'pending',
  breaks_used INTEGER NOT NULL DEFAULT 0,
  source TEXT NOT NULL DEFAULT 'ff',
  created_at INTEGER NOT NULL,
  FOREIGN KEY (preset_id) REFERENCES presets(id)
);
```

### 4.4 Table: app_limits
```sql
CREATE TABLE app_limits (
  package_name TEXT PRIMARY KEY,
  app_name TEXT NOT NULL,
  category TEXT NOT NULL,
  daily_limit_seconds INTEGER NOT NULL,
  extra_session_count INTEGER NOT NULL DEFAULT 0,
  extra_session_duration_seconds INTEGER NOT NULL DEFAULT 300,
  is_active INTEGER NOT NULL DEFAULT 1,
  paused_until INTEGER,
  streak_days INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
```

### 4.5 Table: streak_records
```sql
CREATE TABLE streak_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  current_streak INTEGER NOT NULL DEFAULT 0,
  longest_streak INTEGER NOT NULL DEFAULT 0,
  minimum_sessions_per_day INTEGER NOT NULL DEFAULT 1,
  last_streak_date INTEGER,
  updated_at INTEGER NOT NULL
);
```

### 4.6 Table: study_channels
```sql
CREATE TABLE study_channels (
  id TEXT PRIMARY KEY,
  channel_name TEXT NOT NULL,
  channel_url TEXT,
  added_at INTEGER NOT NULL
);
```

### 4.7 Hive Boxes
| Box name | Type | Contents |
|---|---|---|
| `preferences` | Map | minimumSessionsPerDay, lastUsedPresetId, sessionStartConfirmAfterHours (60) |
| `app_categories` | Map<String, String> | packageName → category override |
| `app_metadata` | Map<String, Map> | packageName → {displayName, iconPath, systemCategory} |

---

## 5. Feature Technical Specifications

### 5.1 Focus Timer

#### 5.1.1 Foreground Service
- Implemented as an Android Foreground Service via `flutter_foreground_task`
- Service persists when app is backgrounded
- Notification: persistent notification showing elapsed/remaining time
- Timer uses `DateTime.now()` delta calculation (not tick-based) to avoid drift

#### 5.1.2 Timer Accuracy
- Timer state stored as: `startTimestamp` + `totalElapsedBeforePause`
- On resume: `elapsed = DateTime.now() - startTimestamp + totalElapsedBeforePause`
- This ensures accuracy even if the service is briefly killed and restarted

#### 5.1.3 Flip Clock Animation
- Two `AnimatedSwitcher` widgets with custom flip transition
- Custom `PageRouteBuilder`-style clip animation: top half slides up (old digit), bottom half slides down (new digit)
- Transition duration: 300ms
- Font: tabular numerals to prevent width shifting

#### 5.1.4 Timer Modes
```dart
sealed class TimerState {}
class CountdownState extends TimerState {
  final Duration remaining;
  final Duration total;
}
class StopwatchState extends TimerState {
  final Duration elapsed;
}
class OpenEndedState extends TimerState {
  final Duration elapsed;
}
```

#### 5.1.5 Session Persistence on App Kill
- Timer state written to Hive every 5 seconds while active
- On app restart: check Hive for active session, restore if found
- If app was killed mid-session: session resumes with correct elapsed time

---

### 5.2 App Blocking & Accessibility Service

#### 5.2.1 Foreground App Detection
- Android Accessibility Service monitors `TYPE_WINDOW_STATE_CHANGED` events
- On event: read `packageName` from `AccessibilityEvent`
- Compare against blocked app list (presets + app limits)

#### 5.2.2 Block Enforcement — During Session
```
On foreground app change:
  if (activeSession && app.category == DISTRACTING && !studyModeOverride):
    launchApp(FF_PACKAGE_NAME)  // redirect to FF
```

#### 5.2.3 Block Enforcement — App Limits (Outside Session)
```
On foreground app change:
  if (appHasActiveLimit(packageName)):
    if (dailyUsage >= dailyLimit && extraSessions == 0):
      showOverlay(InterventionWindow)
    elif (dailyUsage >= dailyLimit && extraSessions > 0):
      showOverlay(InterventionWindow with extra time options)
    elif (inExtraSession && extraSessionTimeUp):
      showOverlay(InterventionWindow with wait timer)
```

#### 5.2.4 Overlay Implementation
- `SYSTEM_ALERT_WINDOW` permission
- Flutter overlay entry using `FlutterOverlayWindow` plugin or native Android `WindowManager`
- Overlay renders the 2/5/10/20 intervention widget
- Blurred background: `BackdropFilter` with `ImageFilter.blur`

#### 5.2.5 YouTube Study Mode Detection
- Accessibility Service monitors URL changes inside YouTube (via `AccessibilityNodeInfo` content description or window title)
- Channel name extracted from window content
- Compared against `study_channels` Hive box
- If no match: overlay triggered with non-whitelisted channel message

---

### 5.3 Streak Engine

#### 5.3.1 Daily Evaluation
- Evaluated via WorkManager task scheduled for 11:59 PM each day
- Logic:
```dart
Future<void> evaluateDailyStreak() async {
  final record = await streakRepository.get();
  final todaySessions = await sessionRepository.getCompletedForDate(DateTime.now());
  final minimum = record.minimumSessionsPerDay;
  
  if (minimum == 0) return; // streak neutral day
  
  final scheduledForToday = await sessionRepository.getScheduledForDate(DateTime.now());
  final required = scheduledForToday.length < minimum 
      ? scheduledForToday.length  // all scheduled sessions must be complete
      : minimum;
  
  if (todaySessions.length >= required) {
    await streakRepository.increment();
  } else {
    // streak not broken unless user actively stopped — silent non-increment
  }
}
```

#### 5.3.2 Streak Reset (on Quit Session)
```dart
Future<void> onQuitSession() async {
  await streakRepository.reset(); // sets currentStreak = 0
  await sessionRepository.markStopped(sessionId);
}
```

#### 5.3.3 Wait Time Calculation
```dart
int calculateWaitSeconds({
  required int streakDays,
  required Duration todayDistractingUsage,
  required Duration avgDistractingUsage,
}) {
  final weights = _getWeights(streakDays);
  
  final usageRatio = avgDistractingUsage.inSeconds == 0 
      ? 1.0 
      : todayDistractingUsage.inSeconds / avgDistractingUsage.inSeconds;
  
  final streakScore = weights.streak * 25;
  final usageScore = weights.usage * 25 * usageRatio.clamp(0.0, 1.0);
  
  return (streakScore + usageScore).clamp(15.0, 25.0).round();
}

({double streak, double usage}) _getWeights(int days) {
  if (days < 10) return (streak: 0.55, usage: 0.45);
  if (days < 21) return (streak: 0.45, usage: 0.55);
  if (days < 30) return (streak: 0.40, usage: 0.60);
  if (days < 45) return (streak: 0.25, usage: 0.75);
  if (days < 60) return (streak: 0.15, usage: 0.85);
  if (days < 100) return (streak: 0.10, usage: 0.90);
  return (streak: 0.00, usage: 1.00);
}
```

---

### 5.4 UsageStats Integration

#### 5.4.1 Platform Channel
```dart
// Dart side
const channel = MethodChannel('com.fluxfoxus/usage_stats');

Future<List<AppUsageRecord>> getUsageForDate(DateTime date) async {
  final result = await channel.invokeMethod<List>('getUsageStats', {
    'startTime': date.millisecondsSinceEpoch,
    'endTime': date.add(const Duration(days: 1)).millisecondsSinceEpoch,
  });
  return result!.map((e) => AppUsageRecord.fromMap(e)).toList();
}
```

#### 5.4.2 Android Kotlin Side
```kotlin
// MainActivity.kt or FluxFoxusPlugin.kt
val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

fun getUsageStats(startTime: Long, endTime: Long): List<Map<String, Any>> {
  val stats = usageStatsManager.queryUsageStats(
    UsageStatsManager.INTERVAL_DAILY,
    startTime,
    endTime
  )
  return stats.map { stat ->
    mapOf(
      "packageName" to stat.packageName,
      "totalTimeInForeground" to stat.totalTimeInForeground,
      "lastTimeUsed" to stat.lastTimeUsed
    )
  }
}
```

#### 5.4.3 Average Calculation
- Rolling 7-day average per app
- Stored in Hive after each daily query
- Used for: app limit range enforcement, wait time formula, widget display

---

### 5.5 FluxDone Integration

#### 5.5.1 MethodChannel Registration
```dart
// FF side — listens for incoming FocusBlockRequests
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

#### 5.5.2 FocusBlockRequest Handler
```dart
Future<void> handle(FocusBlockRequest request) async {
  switch (request.action) {
    case FocusBlockAction.create:
      final preset = await presetRepository.getLastUsed();
      if (preset == null) return; // no preset, skip silently
      await sessionRepository.create(FocusSession(
        id: uuid(),
        presetId: preset.id,
        fdTaskId: request.taskId,
        sessionName: request.taskName,
        mode: TimerMode.countdown,
        scheduledStart: request.startTime,
        scheduledEnd: request.endTime,
        plannedDuration: request.duration,
        status: SessionStatus.pending,
        source: SessionSource.fd,
        createdAt: DateTime.now(),
        breaksUsed: 0,
      ));
    case FocusBlockAction.update:
      await sessionRepository.updateByFdTaskId(request.taskId, request);
    case FocusBlockAction.delete:
      await sessionRepository.deleteByFdTaskId(request.taskId);
  }
}
```

#### 5.5.3 "Focus Session" Task Creation in FD
When user starts a session from FF (not from FD):
```dart
Future<void> createFDTask(FocusSession session, Preset preset) async {
  await fdChannel.invokeMethod('createTask', {
    'name': 'Focus Session',
    'description': 'Preset: ${preset.name}\n'
                   'Mode: ${session.mode.name}\n'
                   'Duration: ${session.plannedDuration?.inMinutes ?? "Open-ended"} min\n'
                   'Started: ${session.actualStart?.toIso8601String()}',
    'listSection': 'FF',  // FF section in default list
  });
}
```

---

### 5.6 Home Screen Widget

#### 5.6.1 Implementation
- `home_widget` Flutter package
- Widget layout defined in `res/layout/ff_widget.xml` (Android XML)
- Data passed from Flutter to native via `HomeWidget.saveWidgetData`

#### 5.6.2 Widget Data Contract
```dart
Future<void> updateWidget() async {
  final usage = await usageRepository.getTodayTotals();
  
  await HomeWidget.saveWidgetData('total_usage', usage.total.inMinutes);
  await HomeWidget.saveWidgetData('productive_minutes', usage.productive.inMinutes);
  await HomeWidget.saveWidgetData('semi_productive_minutes', usage.semiProductive.inMinutes);
  await HomeWidget.saveWidgetData('distracting_minutes', usage.distracting.inMinutes);
  await HomeWidget.saveWidgetData('others_minutes', usage.others.inMinutes);
  await HomeWidget.saveWidgetData('is_live_focus', activeSession != null);
  
  await HomeWidget.updateWidget(
    name: 'FluxFoxusWidget',
    androidName: 'FluxFoxusWidgetProvider',
  );
}
```

#### 5.6.3 WorkManager Task for Widget Refresh
```dart
Workmanager().registerPeriodicTask(
  'widget_refresh',
  'widgetRefreshTask',
  frequency: const Duration(minutes: 15),
  constraints: Constraints(networkType: NetworkType.not_required),
);
```

---

### 5.7 Notifications

#### 5.7.1 Notification Channels (Android)
| Channel ID | Name | Importance | Purpose |
|---|---|---|---|
| `focus_session` | Focus Session | HIGH | Session start heads-up, break end |
| `session_complete` | Session Complete | DEFAULT | Countdown complete |
| `weekly_report` | Weekly Report | DEFAULT | Sunday report |
| `app_limit` | App Limit | LOW | Persistent timer in App Limits foreground service |

#### 5.7.2 Scheduled Notifications
- 15-minute heads-up: scheduled via `android_alarm_manager_plus` when session is created
- Sunday report: WorkManager periodic task on Sundays at 8:00 AM

---

### 5.8 App Limits — Extra Time Session Logic

```dart
class AppLimitEnforcer {
  // Called when app comes to foreground
  Future<void> onAppOpened(String packageName) async {
    final limit = await appLimitRepository.get(packageName);
    if (limit == null || !limit.isActive) return;
    if (limit.pausedUntil != null && DateTime.now().isBefore(limit.pausedUntil!)) return;
    
    final todayUsage = await usageRepository.getTodayUsage(packageName);
    
    if (todayUsage >= limit.dailyLimit) {
      final extraSessionsLeft = await extraSessionRepository.getRemainingForToday(packageName);
      
      if (extraSessionsLeft > 0) {
        overlayService.show(InterventionOverlay(
          packageName: packageName,
          hasExtraTime: true,
          extraSessionDuration: limit.extraSessionDuration,
        ));
      } else {
        overlayService.show(InterventionOverlay(
          packageName: packageName,
          hasExtraTime: false,
        ));
      }
    }
  }
}
```

---

### 5.9 YouTube Study Mode — Channel Detection

#### 5.9.1 Detection Strategy
- Accessibility Service monitors `AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED`
- Reads `AccessibilityNodeInfo` tree to find channel name
- YouTube channel name typically appears in video metadata or channel header node
- Fallback: monitor page title via `getWindows()` content description

#### 5.9.2 Channel Matching
```dart
bool isChannelAllowed(String detectedChannelName) {
  final channels = studyChannelRepository.getAll();
  return channels.any((ch) => 
    ch.channelName.toLowerCase() == detectedChannelName.toLowerCase()
  );
}
```

#### 5.9.3 Confirmation Sentence Selection
```dart
String getConfirmationSentence() {
  final totalChannels = studyChannelRepository.count();
  final pool = totalChannels <= 25 ? _shortSentences : _longSentences;
  final last = preferences.get('lastConfirmationSentence');
  String sentence;
  do {
    sentence = pool[Random().nextInt(pool.length)];
  } while (sentence == last && pool.length > 1);
  preferences.set('lastConfirmationSentence', sentence);
  return sentence;
}

// Short pool (5 sentences, channels 1–25)
final _shortSentences = [
  "I am adding a productive channel.",
  "This channel helps me study better.",
  "I choose to learn from this channel.",
  "This is a study channel I trust.",
  "I am building my study list.",
];

// Long pool (~50 sentences, channels 26+)
// [Populated with varied longer sentences during implementation]
```

---

### 5.10 Preset 60-Hour Confirmation Nudge

```dart
Future<bool> shouldShowPresetConfirmation() async {
  final lastSessionTime = await sessionRepository.getLastSessionEndTime();
  if (lastSessionTime == null) return false;
  
  final gap = DateTime.now().difference(lastSessionTime);
  return gap.inHours >= 60;
}
```

If true: before starting a session from home screen, show a bottom sheet:
- "It's been [X] days since your last session."
- "You're about to start with: [Preset Name]"
- "Continue" button (Electric Indigo) / "Change Preset" link

---

## 6. Routing (go_router)

```dart
final router = GoRouter(
  routes: [
    GoRoute(path: '/', builder: (_, __) => const HomeScreen()),
    GoRoute(path: '/usage', builder: (_, __) => const UsageStatsScreen()),
    GoRoute(path: '/focus', builder: (_, __) => const FocusStartScreen()),
    GoRoute(path: '/focus/session', builder: (_, __) => const FocusSessionScreen()),
    GoRoute(path: '/planner', builder: (_, __) => const PlannerScreen()),
    GoRoute(path: '/planner/preset/create', builder: (_, __) => const PresetCreateScreen()),
    GoRoute(
      path: '/planner/preset/:id/edit',
      builder: (_, state) => PresetEditScreen(presetId: state.pathParameters['id']!),
    ),
    GoRoute(
      path: '/planner/preset/:id/channels',
      builder: (_, state) => ChannelWhitelistScreen(presetId: state.pathParameters['id']!),
    ),
    GoRoute(path: '/blocks', builder: (_, __) => const AppLimitsScreen()),
  ],
);
```

---

## 7. Permissions Flow

### 7.1 Permission Request Order (first launch)
1. `POST_NOTIFICATIONS` — standard runtime permission
2. `PACKAGE_USAGE_STATS` — opens system Settings screen (cannot be granted in-app)
3. `SYSTEM_ALERT_WINDOW` — opens system Settings screen
4. `ACCESSIBILITY_SERVICE` — opens Accessibility Settings screen

Each permission explained to user before opening Settings with a plain-language explanation screen.

### 7.2 Permission Check on Each Launch
```dart
Future<void> checkCriticalPermissions() async {
  if (!await UsageStatsPermission.isGranted) {
    navigateTo('/onboarding/usage_stats_permission');
  }
  if (!await AccessibilityPermission.isGranted) {
    navigateTo('/onboarding/accessibility_permission');
  }
}
```

---

## 8. Testing Strategy

### 8.1 Unit Tests
- All repository implementations
- Streak evaluation logic
- Wait time formula
- FocusBlockRequest handler
- Channel matching logic
- Extra time session enforcement logic

### 8.2 Widget Tests
- Flip clock display and animation
- Category pill rendering
- Session card states (pending/active/completed)
- Intervention overlay

### 8.3 Integration Tests
- Full focus session flow (start → break → end)
- FD → FF sync (mock FD MethodChannel)
- App limit enforcement (mock UsageStats)
- Streak increment/reset

### 8.4 Manual Testing Checklist
- All 5 timer modes × 3 scenarios (normal end / manual stop / break exhaustion)
- App limit enforcement with each extra time configuration
- YouTube Study Mode channel add flow (first 25 channels, after 25)
- 60-hour preset nudge
- Widget data accuracy vs in-app data

---

## 9. Build & Release

### 9.1 Flavors
- `debug` — local development, verbose logging, no obfuscation
- `release` — ProGuard/R8 obfuscation, no debug logs

### 9.2 Code Generation
Run before building:
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

### 9.3 Android Manifest Key Entries
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service android:name=".FluxFoxusAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
  <intent-filter>
    <action android:name="android.accessibilityservice.AccessibilityService" />
  </intent-filter>
  <meta-data android:name="android.accessibilityservice"
      android:resource="@xml/accessibility_service_config" />
</service>
```

### 9.4 accessibility_service_config.xml
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100"
    android:packageNames="" />
```

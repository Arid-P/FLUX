# FluxFoxus (FF) — Technical Requirements Document (TRD v2.0 Kotlin)

**Version:** 2.0 (Native Android System Architecture)  
**Date:** March 2026 / v2 Overhaul  
**Target Platform:** Android (API 26+ / Target API 34)  
**Author:** Ari

---

## 1. System Technology Stack

| Layer | Native Android Technology | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 1.9.20+ / 2.0+ | Modern type safety, Coroutines, Flow |
| **UI Framework** | Jetpack Compose (Compose BOM) | Dark-only Holst (Холст) UI, custom flip-clock canvas |
| **Local Storage** | Android Jetpack `Room` (via KSP) | Presets, sessions, streaks, app limit rules |
| **Preferences** | Android Jetpack `DataStore` | Last-used preset, user configurations, whitelist |
| **Screen Time** | `android.app.usage.UsageStatsManager` | System screen time metrics & category categorization |
| **App Enforcement**| Android `AccessibilityService` | Real-time foreground app interception & YouTube channel inspection |
| **Overlays** | `android.view.WindowManager` (Overlay) | 5/10/20/40 intervention window (`TYPE_APPLICATION_OVERLAY`) |
| **Timer Engine** | Android `ForegroundService` | Persistent timer service with ongoing notification |
| **Background Tasks**| `WorkManager` & `AlarmManager` | Daily streak evaluation, widget refresh, precise scheduled start alarms |
| **Widgets** | Android Jetpack `Glance` | 4-segment screen time donut ring home screen widget |
| **Inter-App IPC** | `BroadcastReceiver` (`goAsync()`) | Receives `com.flux.action.FOCUS_BLOCK_SYNC` from FluxDone |

---

## 2. Android Manifest Permissions & Capabilities

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.fluxfoxus">

    <!-- Screen Time & Foreground Detection -->
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />
    
    <!-- System Overlays (2/5/10/20 Intervention & Stop Modal) -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    
    <!-- Foreground Service for Focus Timer -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <!-- Exact Alarms for Scheduled Sessions -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application ...>
        <!-- Accessibility Service for App Interception -->
        <service
            android:name=".core.service.FocusAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- Timer Foreground Service -->
        <service
            android:name=".core.service.FocusTimerService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />

        <!-- Inter-App Broadcast Receiver for FluxDone -->
        <receiver
            android:name=".core.ipc.FdIntegrationReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="com.flux.action.FOCUS_BLOCK_SYNC" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

---

## 3. Database Schema (Room SQLite v2)

### 3.1 Table: `presets`
```sql
CREATE TABLE presets (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL DEFAULT '⏳',
    break_count INTEGER NOT NULL DEFAULT 2,          -- 0 to 6
    break_duration_minutes INTEGER NOT NULL DEFAULT 5, -- 1 to 15
    youtube_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
    browser_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
    description TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```

### 3.2 Table: `preset_app_rules`
```sql
CREATE TABLE preset_app_rules (
    preset_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    rule_mode TEXT NOT NULL DEFAULT 'BLOCKED', -- 'BLOCKED', 'ALLOWED', 'STUDY_MODE'
    PRIMARY KEY(preset_id, package_name),
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE CASCADE
);
```

### 3.3 Table: `focus_sessions`
```sql
CREATE TABLE focus_sessions (
    id TEXT PRIMARY KEY NOT NULL,
    session_origin TEXT NOT NULL DEFAULT 'MANUAL',    -- 'MANUAL' (impulse), 'SCHEDULED' (calendar), or 'TASK_SYNC' (FD synced)
    preset_id TEXT NOT NULL,                          -- FK to presets(id)
    session_title TEXT NOT NULL,                      -- Custom title or fallback to preset name
    linked_task_id TEXT,                              -- Nullable UUID from FluxDone (if synced)
    timer_mode TEXT NOT NULL,                         -- 'COUNTDOWN', 'STOPWATCH', 'FROM_TO'
    start_time INTEGER NOT NULL,                      -- UTC epoch ms
    end_time INTEGER,                                 -- Nullable UTC epoch ms (computed or actual)
    actual_start_time INTEGER,                        -- Nullable UTC epoch ms (when foreground service actually began)
    target_duration_minutes INTEGER NOT NULL,
    actual_duration_seconds INTEGER NOT NULL DEFAULT 0,
    breaks_taken INTEGER NOT NULL DEFAULT 0,
    breaks_total_seconds INTEGER NOT NULL DEFAULT 0,
    recurrence_rule TEXT,                             -- Nullable RRULE JSON
    series_id TEXT,                                   -- Nullable UUID linking recurring instances
    status TEXT NOT NULL,                             -- 'PLANNED', 'ACTIVE', 'PAUSED_BREAK', 'COMPLETED', 'QUIT_PREMATURELY', 'SKIPPED', 'EXPIRED_ON_BOOT'
    reflection_rating TEXT,                           -- Nullable: 'LOW', 'MEDIUM', 'HIGH', or NULL (P2)
    penalty_delay_applied_seconds INTEGER NOT NULL DEFAULT 0,
    headsup_notification_armed INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE CASCADE
);
```

### 3.4 Table: `app_limits`
```sql
CREATE TABLE app_limits (
    package_name TEXT PRIMARY KEY NOT NULL,
    app_label TEXT NOT NULL,
    daily_budget_minutes INTEGER NOT NULL,
    extra_time_passes_used INTEGER NOT NULL DEFAULT 0, -- Max 3 per day
    is_enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL
);
```

### 3.5 Table: `streaks`
```sql
CREATE TABLE streaks (
    id INTEGER PRIMARY KEY CHECK (id = 1), -- Single row record
    current_streak INTEGER NOT NULL DEFAULT 0,
    best_streak INTEGER NOT NULL DEFAULT 0,
    last_completed_date TEXT NOT NULL,                  -- 'YYYY-MM-DD'
    relief_days_json TEXT NOT NULL DEFAULT '["SUNDAY"]', -- Relief/rest days
    accumulated_grace_shields INTEGER NOT NULL DEFAULT 1, -- Phase 2 Stacking Grace Shields
    shields_used_lifetime INTEGER NOT NULL DEFAULT 0,
    last_shield_granted_month TEXT NOT NULL DEFAULT '2026-10', -- 'YYYY-MM'
    last_shield_used_date TEXT,                         -- 'YYYY-MM-DD'
    updated_at INTEGER NOT NULL
);
```

### 3.6 Table: `app_limit_streaks`
```sql
CREATE TABLE app_limit_streaks (
    package_name TEXT PRIMARY KEY NOT NULL,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_successful_date TEXT NOT NULL,     -- 'YYYY-MM-DD'
    updated_at INTEGER NOT NULL
);
```

### 3.7 Table: `app_category_overrides`
```sql
CREATE TABLE app_category_overrides (
    package_name TEXT PRIMARY KEY NOT NULL,
    app_label TEXT NOT NULL,
    category TEXT NOT NULL, -- 'PRODUCTIVE', 'SEMI_PRODUCTIVE', 'DISTRACTING', 'OTHERS'
    updated_at INTEGER NOT NULL
);
```

### 3.8 Table: `global_content_whitelists`
```sql
CREATE TABLE global_content_whitelists (
    id TEXT PRIMARY KEY NOT NULL,
    app_type TEXT NOT NULL,          -- 'YOUTUBE_CHANNEL' or 'BROWSER_DOMAIN'
    target_identifier TEXT NOT NULL,  -- e.g. '@3blue1brown' or 'desmos.com'
    display_label TEXT NOT NULL,      -- e.g. '3Blue1Brown' or 'Desmos Graphing'
    created_at INTEGER NOT NULL
);
```

### 3.9 Table: `ai_break_negotiations` (Phase 2)
```sql
CREATE TABLE ai_break_negotiations (
    id TEXT PRIMARY KEY NOT NULL,
    session_id TEXT NOT NULL,
    engine_type TEXT NOT NULL,                     -- 'GEMINI_AI' or 'STAMINA_PROTOCOL'
    rounds_count INTEGER NOT NULL,
    conversation_transcript_json TEXT NOT NULL,
    granted INTEGER NOT NULL,                      -- 0 or 1
    granted_duration_minutes INTEGER,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(session_id) REFERENCES focus_sessions(id) ON DELETE CASCADE
);
```

### 3.10 Table: `streak_day_history` (Phase 2)
```sql
CREATE TABLE streak_day_history (
    date TEXT PRIMARY KEY NOT NULL,                -- 'YYYY-MM-DD'
    status TEXT NOT NULL,                          -- 'MET', 'RELIEF_DAY', 'GRACE_CONSUMED', 'BROKEN'
    sessions_completed_count INTEGER NOT NULL DEFAULT 0,
    focus_duration_minutes INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);
```

### 3.11 Table: `daily_focus_scores` (Phase 3)
```sql
CREATE TABLE daily_focus_scores (
    date TEXT PRIMARY KEY NOT NULL,                -- 'YYYY-MM-DD'
    total_score INTEGER NOT NULL,                  -- 0 to 100
    time_score REAL NOT NULL,
    break_score REAL NOT NULL,
    limit_score REAL NOT NULL,
    completion_score REAL NOT NULL,
    focus_minutes_actual INTEGER NOT NULL,
    target_minutes INTEGER NOT NULL,
    is_checkpoint_day INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);
```

### 3.12 Table: `session_templates` (Phase 3)
```sql
CREATE TABLE session_templates (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    preset_id TEXT NOT NULL,
    target_duration_minutes INTEGER NOT NULL,
    preferred_start_time TEXT,                     -- 'HH:mm'
    preferred_end_time TEXT,                       -- 'HH:mm'
    description TEXT,
    is_system_starter INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE RESTRICT
);
```

---

## 4. Enforcement Architecture

### 4.1 Accessibility Enforcement Engine
`FocusAccessibilityService` inspects window transitions via `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`:
1. Reads `event.packageName`.
2. Checks if an active focus session is in progress:
   - If `packageName` is marked blocked in active Preset, immediately redirects user to Home Launcher (`Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)`), and displays a HUD notification: *"Focus Session Active: Instagram is blocked."*
3. If no session is active, checks `app_limits`:
   - If today's usage exceeds `daily_budget_minutes`, launches the 5/10/20/40 intervention overlay window.
4. **Multi-State Content Inspection (YouTube & Web Browsers):**
   - When `packageName == "com.google.android.youtube"` in `STUDY_MODE`:
     - Inspects video description / channel name nodes. If channel is not found in `global_content_whitelists`, triggers the friction overlay.
   - When `packageName in BROWSER_PACKAGES` in `STUDY_MODE`:
     - Inspects URL / address bar text. If active domain is not found in `global_content_whitelists`, redirects to a local focus blocked splash screen.

### 4.2 Foreground Timer Service
- Running during active focus and breaks.
- Posts a high-priority ongoing notification showing remaining time (`HH:MM`) and active mode.
- Holds `PARTIAL_WAKE_LOCK` to ensure Android Doze mode does not pause countdown timers.
- Exposes a Kotlin `StateFlow<SessionState>` that Compose UI observes in real time.

---

## 5. Usage Stats Aggregation & Jetpack Glance Widget Engine

### 5.1 Reactive Stats Pipeline
```kotlin
@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCategoryDao: AppCategoryDao,
    private val focusSessionDao: FocusSessionDao
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Reactive flow combining Android system stats with Room user overrides
    fun getDailyStatsFlow(startEpoch: Long, endEpoch: Long): Flow<DailyStatsAggregate> {
        return appCategoryDao.observeAllOverrides().map { overridesMap ->
            val rawStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startEpoch,
                endEpoch
            )
            val focusSessions = focusSessionDao.getSessionsBetweenSync(startEpoch, endEpoch)
            buildDailyAggregate(rawStats, overridesMap, focusSessions)
        }
    }
}
```

### 5.2 Jetpack Glance Home Screen Widget (`GlanceAppWidget`)
- **Component:** `FocusDonutWidget : GlanceAppWidget()`
- **Layout:**
  - Compact 2×2: Canvas-drawn 4-segment arc ring using Holst color tokens (`#778D7A`, `#D4C4A8`, `#E07A5F`, `#415A77`), center screen time text, and top focus ratio badge.
  - Large 4×2: Left half Donut Ring, Right half Quick Launcher CTA (`actionStartActivity<MainActivity>()` invoking the last-used preset).
- **Background Refresh:**
  - `GlanceUpdateWorker` scheduled via `WorkManager` every 15 minutes.
  - Reactive trigger on `app_category_overrides` modification or `FocusTimerService` session completion.

---

## 6. Permissions Architecture & System Health Pipeline

### 6.1 `PermissionChecker` Engine
```kotlin
@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isNotificationGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

    fun isUsageStatsGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isOverlayGranted(): Boolean = Settings.canDrawOverlays(context)

    fun isAccessibilityGranted(): Boolean {
        val expectedComponentName = ComponentName(context, FocusAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponentName, ignoreCase = true)) return true
        }
        return false
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun areMandatoryShieldsActive(): Boolean =
        isNotificationGranted() && isUsageStatsGranted() && isOverlayGranted() && isAccessibilityGranted()
}
```

### 6.2 Lifecycle Auto-Detection on Resume
- `OnboardingViewModel` listens to Compose `ON_RESUME` events.
- System Settings intent launches invoke `startActivity()`.
- Returning from system settings triggers `refreshPermissionStates()`, instantly converting `[ Enable ➔ ]` into `[ Granted 🟢 ]` with zero manual refresh gestures.



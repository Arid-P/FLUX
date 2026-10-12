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
| **Overlays** | `android.view.WindowManager` (Overlay) | 2/5/10/20 intervention window (`TYPE_APPLICATION_OVERLAY`) |
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
    break_count INTEGER NOT NULL DEFAULT 2,     -- 0 to 6
    break_duration_minutes INTEGER NOT NULL DEFAULT 5, -- 1 to 15
    youtube_mode TEXT NOT NULL DEFAULT 'STUDY_MODE', -- 'BLOCK', 'ALLOW', 'STUDY_MODE'
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
    is_blocked INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY(preset_id, package_name),
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE CASCADE
);
```

### 3.3 Table: `focus_sessions`
```sql
CREATE TABLE focus_sessions (
    id TEXT PRIMARY KEY NOT NULL,
    preset_id TEXT,             -- Nullable FK to presets(id)
    linked_task_id TEXT,        -- Nullable UUID from FluxDone
    task_name TEXT,             -- Display title from FD
    timer_mode TEXT NOT NULL,   -- 'COUNTDOWN', 'STOPWATCH', 'OPEN_ENDED'
    start_time INTEGER NOT NULL,-- UTC epoch ms
    end_time INTEGER,           -- Nullable UTC epoch ms
    target_duration_minutes INTEGER NOT NULL,
    actual_duration_seconds INTEGER NOT NULL DEFAULT 0,
    breaks_taken INTEGER NOT NULL DEFAULT 0,
    breaks_total_seconds INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL,       -- 'SCHEDULED', 'ACTIVE', 'PAUSED_BREAK', 'COMPLETED', 'QUIT_PREMATURELY'
    penalty_delay_applied_seconds INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(preset_id) REFERENCES presets(id) ON DELETE SET NULL
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
    last_completed_date TEXT NOT NULL,      -- 'YYYY-MM-DD'
    updated_at INTEGER NOT NULL
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
   - If today's usage exceeds `daily_budget_minutes`, launches the 2/5/10/20 intervention overlay window.
4. **YouTube Window Inspection:**
   - When `packageName == "com.google.android.youtube"`:
   - Uses `AccessibilityNodeInfo` to inspect video description / channel name nodes.
   - If channel is not found in Whitelist, triggers friction overlay.

### 4.2 Foreground Timer Service
- Running during active focus and breaks.
- Posts a high-priority ongoing notification showing remaining time (`HH:MM`) and active mode.
- Holds `PARTIAL_WAKE_LOCK` to ensure Android Doze mode does not pause countdown timers.
- Exposes a Kotlin `StateFlow<SessionState>` that Compose UI observes in real time.

# FluxFoxus (FF) — Phase 1: Permission Onboarding & Service Reliability Specification

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** First-Launch Onboarding Wizard (`/onboarding/wizard`), Interactive Permissions Checklist, Real-Time Lifecycle Auto-Detection, OEM Battery Killer Guides, and Ambient System Health Recovery Monitor  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`, `#E07A5F`)

---

## 1. Architectural Strategy & Philosophy

Unlike standard passive productivity tools, **FluxFoxus is an enforcement suite**. It actively intercepts distracting applications, monitors screen time consumption, renders system-level overlay timers, and runs background foreground services during multi-hour deep work sessions.

To function reliably on modern Android (API 33/34+):
1. **Zero Cold-Start Friction:** Permissions are presented not as bureaucratic hurdles, but as **"Activating Focus Shields"** with plain-language academic rationales.
2. **Single-Page Interactive Checklist:** All permissions live on a unified scrollable checklist screen. Ari does not need to swipe through a tedious multi-page carousel.
3. **Zero-Latency Lifecycle Auto-Detection:** Using Android Jetpack Compose `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)`, the instant Ari toggles an OS permission in System Settings and returns to FF, the card updates to **`[ Granted 🟢 ]`** with haptic feedback.
4. **OEM Background Survival:** Tailored manufacturer detection (`Build.MANUFACTURER`) provides targeted guidance against aggressive background service killers (Samsung OneUI, Xiaomi MIUI/HyperOS, OnePlus OxygenOS).
5. **Continuous System Health Monitor:** If Android OS silently revokes an accessibility service after a phone reboot, an ambient health banner appears on `Tab.Home` to restore protection with one tap.

---

## 2. The 5 Focus Shields Matrix

| Shield # | Android Permission | Type | Scope | User-Facing Rationale | System Action Intent |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `POST_NOTIFICATIONS` | Runtime Prompt | **Mandatory** | Displays ongoing focus timers, break alerts, and 15m session warnings. | `ActivityResultContracts.RequestPermission()` |
| **2** | `PACKAGE_USAGE_STATS` | Special App Access | **Mandatory** | Powers the 24h Momentum chart, Donut widget, and daily App Limit tracking. | `Settings.ACTION_USAGE_ACCESS_SETTINGS` |
| **3** | `SYSTEM_ALERT_WINDOW` | Special App Access | **Mandatory** | Displays the 5/10/20/40m Intervention Overlay and YouTube Edge Grabber. | `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (`package:com.flux.focus`) |
| **4** | `BIND_ACCESSIBILITY_SERVICE`| Accessibility Service | **Mandatory** | **Core Interceptor:** Closes blacklisted apps and checks YouTube/Browser whitelists. | `Settings.ACTION_ACCESSIBILITY_SETTINGS` |
| **5** | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Power Exception | **Optional** | Prevents Android Doze mode and OEM cleaners from killing active timers. | `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |

---

## 3. UI Flow: The Onboarding Wizard (`/onboarding/wizard`)

### 3.1 Step 1: Welcome & Mission Screen
- **Hero Icon:** FF Aperture Emblem (64×64dp) in Warm Sand Khaki (`#D4C4A8`).
- **Heading:** *"Forge Unbreakable Focus"* (`#F4F1DE`, 24sp bold).
- **Sub-label:** *"FluxFoxus enforces hard digital boundaries for your Olympiad and JEE study blocks. 100% offline, private, and local."*
- **Primary CTA:** Wide pill button *"Configure Focus Shields ➔"* in Warm Sand `#D4C4A8`.

---

### 3.2 Step 2: The Interactive Shields Checklist

A scrollable container presenting all 5 shield cards:

```
+-------------------------------------------------------+
|  FLUXFOXUS                                     Step 2/3|
|  🛡️ Activate Focus Shields                            |
|  Enable the required Android shields to enforce deep  |
|  focus blocks. All data stays local on your device.   |
+-------------------------------------------------------+
|  1. NOTIFICATIONS                                     |
|  +-------------------------------------------------+  |
|  |  🔔 Focus Timers & Break Alerts     [Granted 🟢]|  |
|  |  Ensures timer chimes and break warnings alert. |  |
|  +-------------------------------------------------+  |
|                                                       |
|  2. SCREEN TIME & USAGE ACCESS                        |
|  +-------------------------------------------------+  |
|  |  📊 24h Momentum & App Limits       [Enable ➔]  |  |
|  |  Allows FF to calculate daily study analytics.  |  |
|  +-------------------------------------------------+  |
|                                                       |
|  3. DISPLAY OVER OTHER APPS                           |
|  +-------------------------------------------------+  |
|  |  🪟 Distraction Intervention Window  [Enable ➔]  |  |
|  |  Allows FF to present the 5/10/20/40m timer.    |  |
|  +-------------------------------------------------+  |
|                                                       |
|  4. ACCESSIBILITY ENFORCEMENT SERVICE                 |
|  +-------------------------------------------------+  |
|  |  🔒 Core Focus Interceptor          [Enable ➔]  |  |
|  |  Instantly closes blacklisted apps & checks     |  |
|  |  YouTube study whitelist.                       |  |
|  +-------------------------------------------------+  |
|                                                       |
|  5. UNRESTRICTED BATTERY               [Optional]     |
|  +-------------------------------------------------+  |
|  |  ⚡ Prevent Background Timer Kills   [Enable ➔]  |  |
|  |  Keeps multi-hour sessions immune to OS sleep.  |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
|  [     Continue to Dashboard (All Shields Ready)   ]  |
+-------------------------------------------------------+
```

#### Card Visual States & Behaviors:
- **Card Container:** Oxford Slate `#1B263B`, border 1dp `#415A77`, corner radius 12dp, padding 16dp.
- **Pending State:**
  - Status Button: `[ Enable ➔ ]` in Warm Sand `#D4C4A8` text on muted surface `#1B263B` with border `#415A77`.
  - Tapping immediately opens the targeted Android System Settings screen.
- **Granted State:**
  - Status Pill: `[ Granted 🟢 ]` in Muted Sage Green (`#778D7A`) background with Eggshell Cream text (`#F4F1DE`).
  - Card border shifts to subtle Sage Green (`#778D7A`).
- **Primary Continue CTA:**
  - Pinned full-width pill button at screen bottom.
  - **Gating Logic:** Remains disabled, greyed out (`opacity 0.4`), and non-interactive until Shields 1, 2, 3, and 4 are marked `[ Granted 🟢 ]`.
  - Shield 5 (Battery Optimization) is marked `[ Optional ]` and does not block progression.

> [!NOTE]
> **Phase 2 Forward Integration:** Phase 2 introduces **Shield #6: Uninstall Guard (Device Admin)** ([`FF_P2_UNINSTALL_PROTECTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_UNINSTALL_PROTECTION_SPEC.md)), which appears directly below Shield #5 as an additional Optional shield.

---

### 3.3 Step 3: OEM Device Killer Survival Guide

Before landing on Home, FF inspects `Build.MANUFACTURER` to present device-tailored instructions preventing silent background termination:

```
+-------------------------------------------------------+
|  FLUXFOXUS                                     Step 3/3|
|  ⚡ OEM Background Protection                          |
|  Detected: Samsung OneUI                              |
+-------------------------------------------------------+
|  Samsung devices aggressively sleep background apps.  |
|  Follow these 2 quick steps to prevent timer drops:   |
|                                                       |
|  +-------------------------------------------------+  |
|  |  1. Lock FluxFoxus in Recent Apps               |  |
|  |     Open app switcher, tap FF icon, select      |  |
|  |     "Lock this app" (🔒).                       |  |
|  +-------------------------------------------------+  |
|  |  2. Add to "Never Sleeping Apps"                |  |
|  |     Settings ➔ Battery ➔ Background usage limits|  |
|  |     ➔ Never auto-sleeping apps ➔ Add FF.        |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
|  [       All Shields Active — Enter FluxFoxus      ]  |
+-------------------------------------------------------+
```

#### Supported Manufacturer Profiles:
1. **Samsung (OneUI):**
   - Guide: Pin in Recent Apps + Add to "Never Auto-Sleeping Apps" in Device Care.
2. **Xiaomi / Poco / Redmi (MIUI & HyperOS):**
   - Guide: Enable "Autostart" in App Info + Set Battery Saver to "No Restrictions".
3. **OnePlus / Realme / Oppo (ColorOS / OxygenOS):**
   - Guide: Disable "Auto-Optimize" + Enable "Allow background activity".
4. **Google Pixel / Motorola / Stock Android:**
   - Guide: Set App Battery Usage to "Unrestricted".

---

## 4. Technical Architecture: Lifecycle Auto-Detection

### 4.1 Permission State Observer (Jetpack Compose)
Zero manual refresh buttons. The UI automatically evaluates permission states whenever the activity regains focus:

```kotlin
@Composable
fun PermissionsOnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-evaluate permissions every time user returns from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionsChecklistContent(
        state = state,
        onRequestPermission = { shield -> viewModel.launchPermissionIntent(shield) },
        onContinue = onComplete
    )
}
```

### 4.2 Permission Checking Logic
```kotlin
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
}
```

---

## 5. Post-Onboarding: Continuous System Health Recovery

If Android OS revokes a permission following a reboot, system update, or manual user toggle:
1. `PermissionChecker` evaluates shields on every app launch and whenever `Tab.Home` becomes visible.
2. If **Accessibility Service** or **Usage Access** is revoked:
   - An ambient warning banner appears at the top of `Tab.Home`:
     ```
     +-------------------------------------------------------+
     |  ⚠️ Focus Shields Compromised                         |
     |  Accessibility service was disabled by system update. |
     |  [ Restore Protection ➔ ]                             |
     +-------------------------------------------------------+
     ```
   - Tapping `[ Restore Protection ➔ ]` opens the checklist directly to the disabled shield.
   - Starting a focus session is temporarily gated with a prompt: *"Accessibility permission is required to enforce app restrictions."*

---

## 6. Strict Holst UI Color Token Lock

| UI Component | Token Name | Hex Value |
| :--- | :--- | :--- |
| **Screen Background** | `color_background` | `#0D1B2A` |
| **Shield Card Surfaces** | `color_surface` | `#1B263B` |
| **Borders & Dividers** | `color_border` | `#415A77` |
| **Action CTA / Enable Button** | `color_primary` (Warm Sand) | `#D4C4A8` |
| **Granted Pill / Shield Active** | `color_accent` (Muted Sage) | `#778D7A` |
| **Compromised Shield Alert** | `color_warning` (Terracotta Coral) | `#E07A5F` |
| **Primary High-Contrast Text** | `color_text_primary` | `#F4F1DE` |
| **Muted Rationales & Explanations** | `color_text_muted` | `#8E9CAE` |

# 🛡️ Topic FF-P2-1: Device Administrator & Anti-Tamper Protection Specification

**Document ID:** `FF_P2_UNINSTALL_PROTECTION_SPEC`  
**Phase:** Phase 2 (Hardened Enforcement & Advanced Scheduling)  
**Target Package:** `com.flux.focus.features.security`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Android `DevicePolicyManager` + Jetpack DataStore)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Uninstall Protection introduces deliberate friction against impulsive app removal. Under moments of acute distraction, cognitive resistance, or frustration with an active focus block or app limit, users frequently attempt to uninstall the enforcement app in autopilot.

### 1.1 Non-Destructive Friction Mechanic
* **Friction Over Impossibility:** Android's security sandbox guarantees that root commands (`adb pm uninstall`) or full factory resets will always bypass app-level locks. This is expected and accepted.
* **Google Play Policy Compliance:** FluxFoxus registers as a Device Administrator strictly to prevent trivial uninstallation. It requests **zero destructive policies** (no remote wipe, no lock screen password enforcement, no camera disabling).
* **Asymmetric Friction Rule:** Consistent with the FluxFoxus core design principles established in Phase 1, canceling the removal (`[ Keep Protection ]`) is **always free and instant**, while disabling protection (`[ Remove Protection ]`) enforces a mandatory **10-second friction countdown**.

---

## 2. Dependencies & Phase 1 Integration

| Component | Upstream Dependency | Contract / Integration |
|---|---|---|
| **Onboarding Shield #6** | [`FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_PERMISSIONS_AND_ONBOARDING_SPEC.md) | Registers as the 6th shield (following Battery Optimization) on the interactive Permissions Checklist. Explicitly marked as **Optional**. |
| **Active Session Guard** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | `SessionRepository.observeActiveSession()` supplies live session state. Disabling protection is **strictly blocked** while a session is active. |
| **Streak Engine Integrity** | [`FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_HOME_DASHBOARD_AND_STREAKS_SPEC.md) | Removing protection does **NOT** reset the streak to 0. The streak remains intact. |
| **Settings Navigation** | `Tab.Home` Header Settings Gear | Navigates to `/settings/security` section containing the protection toggle. |

---

## 3. Android Device Administration Architecture

### 3.1 Device Admin Manifest Declaration
The receiver binds `android.permission.BIND_DEVICE_ADMIN` and responds to activation and deactivation request intents:

```xml
<!-- AndroidManifest.xml -->
<receiver
    android:name="com.flux.focus.features.security.receiver.FluxFoxusDeviceAdminReceiver"
    android:label="@string/app_name"
    android:description="@string/device_admin_description"
    android:permission="android.permission.BIND_DEVICE_ADMIN"
    android:exported="true">
    <meta-data
        android:name="android.app.device_admin"
        android:resource="@xml/device_admin_policies" />
    <intent-filter>
        <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
        <action android:name="android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED" />
        <action android:name="android.app.action.DEVICE_ADMIN_DISABLED" />
    </intent-filter>
</receiver>
```

### 3.2 Non-Destructive Policies Declaration (`device_admin_policies.xml`)
To guarantee total user trust and zero friction during Google Play Store review, the `<uses-policies>` block contains no destructive authority:

```xml
<!-- res/xml/device_admin_policies.xml -->
<device-admin xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-policies>
        <!-- Zero destructive policies requested. 
             Device admin registration is leveraged solely for OS uninstall blocking. -->
    </uses-policies>
</device-admin>
```

### 3.3 Receiver Implementation (`FluxFoxusDeviceAdminReceiver.kt`)
The receiver reacts to OS-level admin events and returns custom friction text when the user attempts deactivation through Android System Settings:

```kotlin
package com.flux.focus.features.security.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.flux.focus.R
import com.flux.focus.features.security.domain.repository.SecurityRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class FluxFoxusDeviceAdminReceiver : DeviceAdminReceiver() {

    @Inject
    lateinit var securityRepository: SecurityRepository

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        runBlocking {
            securityRepository.setDeviceAdminActive(true)
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        runBlocking {
            securityRepository.setDeviceAdminActive(false)
        }
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Shown by Android OS inside the system deactivation prompt
        return context.getString(R.string.device_admin_deactivation_os_warning)
    }
}
```

---

## 4. Feature Specifications & Behavioral Rules

### 4.1 Onboarding Flow Integration
* **Placement:** Positioned as **Shield #6 (Uninstall Guard)** on the interactive permissions screen (`/onboarding/permissions`), placed directly below Shield #5 (Battery Optimization).
* **Optional Flag:** Marked with an `OPTIONAL` tag in Secondary Text (`#A0AAB2`). Users can proceed through onboarding without enabling it.
* **Grant Action:** Tapping `[ Enable ➔ ]` launches the system enrollment intent:
  ```kotlin
  val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
      putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
      putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, context.getString(R.string.device_admin_prompt_explanation))
  }
  context.startActivity(intent)
  ```
* **Auto-Detection:** On `ON_RESUME`, `devicePolicyManager.isAdminActive(componentName)` updates the checkbox immediately to checked (`✅`).

---

### 4.2 In-App Settings & Security Hub (`/settings/security`)

```
+-------------------------------------------------------+
|  ←  Security & Anti-Tamper                            |
+-------------------------------------------------------+
|                                                       |
|  UNINSTALL PROTECTION                                 |
|  +-------------------------------------------------+  |
|  | 🛡️ Uninstall Guard             [  ACTIVE  ] (●) |  |
|  | Prevents impulsive uninstallation of FluxFoxus  |  |
|  +-------------------------------------------------+  |
|                                                       |
|  When active, Android prevents app removal. To        |
|  uninstall, you must complete a 10s cooldown.         |
|                                                       |
+-------------------------------------------------------+
```

* **Toggle Control:**
  * When `OFF` → Tapping opens the standard OS `ACTION_ADD_DEVICE_ADMIN` screen.
  * When `ON` → Tapping initiates the **Friction Deactivation Flow** (§4.4).
* **Status Pill:**
  * Active: Background `#778D7A` (Sage), Text `#F4F1DE`, Label: `ACTIVE`.
  * Inactive: Background `#415A77`, Text `#A0AAB2`, Label: `INACTIVE`.

---

### 4.3 Mid-Session Inviolability Rule (Locked During Focus)
* **Invariant:** If a Focus Session is in `Active` or `Break` state, uninstall protection **cannot be turned off**.
* **Behavior:**
  * Tapping the toggle while a session is running does **not** open the 10-second friction modal.
  * An immediate Warning Snackbar is displayed:
    > *"Cannot remove protection during an active focus session. Complete or end your session first."*
  * The toggle remains locked in the `ON` state.

---

### 4.4 In-App Revocation Friction Modal

When the user attempts to toggle Uninstall Protection off outside of an active focus session:

```
+-------------------------------------------------------+
|                                                       |
|                     ⚠️                                |
|          Remove Uninstall Guard?                      |
|                                                       |
|  This allows FluxFoxus to be uninstalled from your    |
|  device. Your streak will be preserved, but your      |
|  focus environment will no longer be protected.       |
|                                                       |
|                 [ 10s Cooldown ]                      |
|                                                       |
|  +-------------------------------------------------+  |
|  |           [ Keep Protection ]                   |  |
|  +-------------------------------------------------+  |
|  |           [ Remove Protection (8s) ]            |  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

#### Modal Interaction Rules:
1. **Back Tap Disabled:** The Android system back gesture and outside scrim taps are ignored during the countdown.
2. **`[ Keep Protection ]` Button (Primary):**
   * Background: Accent Sage `#778D7A` or Sand `#D4C4A8`.
   * State: **Always enabled and free at 0s**.
   * Action: Closes modal immediately with zero state change.
3. **`[ Remove Protection ]` Button (Secondary):**
   * Default State: Disabled (`alpha = 0.38`), Background `#415A77`.
   * Label: Dynamic countdown: `"Remove Protection (10s)"` → `"Remove Protection (9s)"` → ... → `"Remove Protection"`.
   * Activated State (at 0s): Enabled, Background Coral Red `#E07A5F`, Text `#F4F1DE`.
   * Action:
     * Calls `devicePolicyManager.removeActiveAdmin(componentName)`.
     * Updates DataStore `uninstall_protection_enabled = false`.
     * Emits success toast: *"Uninstall protection deactivated."*
     * Dismisses modal.

---

### 4.5 Accessibility Boundary (OS Settings Integrity)
* **Design Decision:** FluxFoxus accessibility service **does not** aggressively intercept or inject clicks to block Android OS `Settings -> Apps -> FluxFoxus -> Force Stop`.
* **Rationale:** Device Admin already greys out the `Uninstall` button at the OS level. Hijacking the system settings app creates fragile OS-dependent regressions, accessibility store rejections, and battery drains. Friction remains centered on the Device Admin revocation hook.

---

## 5. Data & State Storage Architecture

### 5.1 Jetpack DataStore Schema
Uninstall protection state is maintained in `SecurityPreferences`:

```kotlin
package com.flux.focus.features.security.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

object SecurityPreferenceKeys {
    val UNINSTALL_PROTECTION_ENABLED = booleanPreferencesKey("uninstall_protection_enabled")
    val UNINSTALL_PROTECTION_BANNER_DISMISSED = booleanPreferencesKey("uninstall_protection_banner_dismissed")
}
```

### 5.2 Domain Repository Interface

```kotlin
package com.flux.focus.features.security.domain.repository

import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    fun observeIsDeviceAdminActive(): Flow<Boolean>
    suspend fun setDeviceAdminActive(isActive: Boolean)
    fun isDeviceAdminEnrolled(): Boolean
    suspend fun revokeDeviceAdmin()
}
```

### 5.3 Repository Implementation (`SecurityRepositoryImpl.kt`)

```kotlin
package com.flux.focus.features.security.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.flux.focus.features.security.data.preferences.SecurityPreferenceKeys
import com.flux.focus.features.security.domain.repository.SecurityRepository
import com.flux.focus.features.security.receiver.FluxFoxusDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SecurityRepository {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, FluxFoxusDeviceAdminReceiver::class.java)

    override fun observeIsDeviceAdminActive(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            val stored = prefs[SecurityPreferenceKeys.UNINSTALL_PROTECTION_ENABLED] ?: false
            stored && dpm.isAdminActive(adminComponent)
        }
    }

    override suspend fun setDeviceAdminActive(isActive: Boolean) {
        dataStore.edit { prefs ->
            prefs[SecurityPreferenceKeys.UNINSTALL_PROTECTION_ENABLED] = isActive
        }
    }

    override fun isDeviceAdminEnrolled(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    override suspend fun revokeDeviceAdmin() {
        if (dpm.isAdminActive(adminComponent)) {
            dpm.removeActiveAdmin(adminComponent)
        }
        setDeviceAdminActive(false)
    }
}
```

---

## 6. Jetpack Compose UI Specifications

### 6.1 Color & Typography Tokens (Holst Theme)

| Token | Hex | Usage |
|---|---|---|
| `SurfaceCard` | `#1B263B` | Security settings card container |
| `SurfaceBorder` | `#415A77` | Card borders, disabled button background |
| `AccentSage` | `#778D7A` | Active status pill, `[ Keep Protection ]` primary button |
| `PrimarySand` | `#D4C4A8` | Key labels and focus headers |
| `TextLight` | `#F4F1DE` | Primary modal body text and button typography |
| `TextMuted` | `#A0AAB2` | Subtitle descriptions and inactive status |
| `DangerCoral` | `#E07A5F` | Activated `[ Remove Protection ]` warning button |

### 6.2 UninstallProtectionModal Composable Structure

```kotlin
@Composable
fun UninstallProtectionModal(
    onDismissRequest: () -> Unit,
    onConfirmRemove: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    Dialog(
        onDismissRequest = { /* Block dismiss */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
            border = BorderStroke(1.dp, Color(0xFF415A77)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛡️",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Remove Uninstall Guard?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF4F1DE),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This allows FluxFoxus to be uninstalled from your device. Your streak will be preserved, but your focus environment will no longer be protected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA0AAB2),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Keep Protection (Always Free)
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF778D7A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Keep Protection", color = Color(0xFFF4F1DE), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Remove Protection (10s Friction Gate)
                Button(
                    onClick = { if (remainingSeconds == 0) onConfirmRemove() },
                    enabled = remainingSeconds == 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE07A5F),
                        disabledContainerColor = Color(0xFF415A77).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val label = if (remainingSeconds > 0) "Remove Protection (${remainingSeconds}s)" else "Remove Protection"
                    Text(label, color = if (remainingSeconds == 0) Color(0xFFF4F1DE) else Color(0xFFA0AAB2))
                }
            }
        }
    }
}
```

---

## 7. Zero-Mock Testing & Verification Workflow

### 7.1 Verification on Real Android Device / Emulator
1. **Enrollment Check:**
   * Go to Onboarding or `/settings/security`.
   * Enable Device Admin.
   * Verify in OS Settings: `Settings -> Apps -> FluxFoxus` -> The **Uninstall** button is disabled / greyed out.
2. **Mid-Session Tamper Test:**
   * Start a 25-minute focus session on `Tab.Focus`.
   * Navigate to `/settings/security`.
   * Tap the toggle.
   * Verify: Toggle is locked, snackbar *"Cannot remove protection during an active focus session"* appears.
3. **Friction Gate Countdown Test:**
   * Ensure session is stopped.
   * Tap toggle to remove protection.
   * Verify: `[ Keep Protection ]` is clickable at 10s and cancels immediately.
   * Verify: `[ Remove Protection ]` is disabled and ticks down from 10 to 0.
   * Confirm removal at 0s -> Verify `dpm.isAdminActive` returns false, OS Uninstall button becomes active.
   * Verify: Streak counter on `Tab.Home` remains completely unaffected.

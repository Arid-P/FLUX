# 🧘 Topic FF-P4-2: Pre-Open Habit Friction Screen Specification

**Document ID:** `FF_P4_PRE_OPEN_FRICTION_SPEC`  
**Phase:** Phase 4 (Cross-Platform Daemon, Group Limits & Habit Breaking)  
**Target Package:** `com.flux.focus.features.limits.friction`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Android Accessibility Window Interceptor + Compose System Window Overlay)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

The vast majority of digital distraction does not stem from deliberate intention—it is triggered by unconscious, automatic muscle memory. A student unlocks their phone to check a physics formula or answer an urgent call, but an autopilot reflex taps Instagram, YouTube, or Reddit before their conscious mind can intervene.

**Pre-Open Habit Friction** intercepts the launch of distracting apps with a calm, full-screen breathing pause. By enforcing a brief 5-second mindful cooldown before the app renders, the automatic loop is disrupted, allowing conscious decision-making to reclaim control.

### 1.1 The Asymmetric Free-Exit Principle
* **`[ ↩️ Step Back ]` is ALWAYS FREE & UNLOCKED:** Available from the very first millisecond. If the user decides not to proceed, they can tap `Step Back` instantly to return to their home launcher with zero penalty.
* **`[ Open App ]` is STRICTLY LOCKED:** Disabled and non-interactive until the countdown reaches `0s`.

---

## 2. Dependencies & Sequential Handover

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Accessibility Interceptor** | [`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md) | Detects `TYPE_WINDOW_STATE_CHANGED` package transitions. |
| **App Categories** | [`FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md) | Triggers on apps categorized as `DISTRACTING`. |
| **Intervention Window (5/10/20/40m)** | [`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md) | Sequential: If user completes pre-open friction and the app is over its daily budget, the intervention window immediately appears. |
| **Active Session Immunity** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | During active focus sessions, preset blacklists block apps 100%; Pre-Open Friction does not trigger during sessions. |

---

## 3. Trigger Conditions & Configuration

Pre-Open Friction displays when **ALL** of the following conditions are met:
1. Global Setting `pre_open_friction_enabled == true` (**ON by default** on fresh install).
2. The opened application belongs to the `DISTRACTING` category.
3. Per-app friction is enabled for that package (`pre_open_friction_enabled == 1`).
4. **No active focus session is currently running** in `FocusTimerService`.

### 3.1 Settings & Controls
* **Global Location:** `Settings → App Limits → Pre-Open Habit Friction`
  * Toggle: `Enable Pre-Open Friction` (Default: **ON**).
  * Cooldown Duration: Segmented chips: `[ 3s ] | [ 5s (Default) ] | [ 10s ]`.
* **Per-App Exclusion:**
  * Located inside `AppDetailScreen` (`/limits/app/{packageName}`):
  * Toggle: `Pre-Open Friction for this App` (Default: **ON**).
  * Allows users to exempt specific apps (e.g. YouTube if used primarily for lectures) while retaining friction on social feeds.

---

## 4. Full-Screen Overlay UX & Interaction Flow

Rendered via Android `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` and `FLAG_LAYOUT_IN_SCREEN`:

```
+-------------------------------------------------------+
|                                                       |
|                     [ 📱 App Icon ]                   |
|                        Instagram                      |
|                                                       |
|                                                       |
|                     ╭─────────╮                       |
|                   ╭─╯         ╰─╮                     |
|                  │   (BREATHE)   │                    |
|                   ╰─╮         ╭─╯                     |
|                     ╰─────────╯                       |
|                                                       |
|                    Opening in 4s...                   |
|                                                       |
|                                                       |
|  +-------------------------------------------------+  |
|  |           [ ↩️ Step Back (Don't Open) ]          |  |
|  +-------------------------------------------------+  |
|  |           [ Open App (Locked 4s) ]              |  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

### 4.1 Visual & Animation Specifications
* **Scrim Background:** 95% opacity Abyssal Navy (`#F20D1B2A`). The target app behind the scrim is obscured to remove visual dopamine triggers.
* **Expanding Breathing Circle:** A calm circular pulse rendered in Sage Green `#778D7A` at 25% opacity, expanding smoothly from 48dp to 180dp over the configured countdown seconds using Compose `animateFloatAsState`.
* **Asymmetric Action Handling:**
  * **`[ ↩️ Step Back (Don't Open) ]` (Primary Action):**
    * Background: Sage Green `#778D7A`, Text `#F4F1DE`, Bold.
    * **Always clickable instantly at second 1**.
    * Action: Dismisses overlay and routes user to Android Home Launcher (`Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)`). The distracting app is closed.
  * **`[ Open App ]` (Friction Gate):**
    * Disabled state: Background `#415A77` (`alpha = 0.38`), Text `#A0AAB2`, Non-interactive.
    * Countdown label: *"Open App (5s)"* → *"Open App (4s)"* → ... → *"Open App"*.
    * Activated state (at 0s): Enabled, Background Oxford Slate `#1B263B` with border `#778D7A`, Text `#F4F1DE`.
    * Action: Dismisses overlay, allowing the application to open.

---

## 5. Sequential Handover Flowchart

```
                 [ User Taps Distracting App Icon ]
                                 |
                                 v
               Is an Active Focus Session Running?
                      /                     \
                    YES                      NO
                    /                         \
       [ Instant Focus Block ]                 v
       (Preset Blacklist Cuts In)    Is Pre-Open Friction ON?
                                           /          \
                                         YES           NO
                                         /              \
                          [ Pre-Open Friction 5s ]       v
                          (Breathing Overlay Appears)   [ Check Daily Budget ]
                               /              \
                        [ Step Back ]     [ Wait 5s & Open ]
                              |                   |
                     (Returns to Home)            v
                                        Is Daily Budget Exceeded?
                                              /          \
                                            YES           NO
                                            /              \
                             [ 5/10/20/40m Overlay ]    [ App Opens Freely ]
```

---

## 6. Database Schema & Data Models

### 6.1 Schema Extension on `app_limits` Room Entity

```kotlin
// Added to AppLimitEntity:
@ColumnInfo(name = "pre_open_friction_enabled")
val preOpenFrictionEnabled: Boolean = true // Default: true
```

### 6.2 DataStore Preferences (`FrictionPreferenceKeys.kt`)

```kotlin
package com.flux.focus.features.limits.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object FrictionPreferenceKeys {
    val PRE_OPEN_FRICTION_ENABLED = booleanPreferencesKey("pre_open_friction_enabled") // Default: true
    val PRE_OPEN_FRICTION_DURATION_SECONDS = intPreferencesKey("pre_open_friction_duration_seconds") // Default: 5
}
```

---

## 7. Compose Overlay Composable (`PreOpenFrictionOverlay.kt`)

```kotlin
@Composable
fun PreOpenFrictionOverlay(
    appLabel: String,
    appIcon: Drawable?,
    totalSeconds: Int = 5,
    onStepBack: () -> Unit,
    onProceed: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20D1B2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Identity
            Text(
                text = appLabel,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF4F1DE),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Breathing Ring Visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .background(Color(0xFF778D7A).copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, Color(0xFF778D7A), CircleShape)
                )
                Text(
                    text = if (remainingSeconds > 0) "${remainingSeconds}s" else "Ready",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFD4C4A8),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Take a mindful breath before opening.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA0AAB2)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Step Back Button (Always Free & Unlocked)
            Button(
                onClick = onStepBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF778D7A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "↩️ Step Back (Don't Open)",
                    color = Color(0xFFF4F1DE),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proceed Button (Locked Until 0s)
            Button(
                onClick = { if (remainingSeconds == 0) onProceed() },
                enabled = remainingSeconds == 0,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B263B),
                    disabledContainerColor = Color(0xFF415A77).copy(alpha = 0.38f)
                ),
                border = BorderStroke(1.dp, if (remainingSeconds == 0) Color(0xFF778D7A) else Color.Transparent),
                shape = RoundedCornerShape(10.dp)
            ) {
                val label = if (remainingSeconds > 0) "Open App (${remainingSeconds}s)" else "Open App"
                Text(
                    text = label,
                    color = if (remainingSeconds == 0) Color(0xFFF4F1DE) else Color(0xFFA0AAB2)
                )
            }
        }
    }
}
```

---

## 8. Verification & Branch-Testing Isolation Workflow

### 8.1 Zero-Mock Automated & Manual Test Cases
1. **Asymmetric Free Exit Test:**
   * Launch Instagram.
   * Verify Pre-Open Friction overlay displays immediately.
   * Tap `[ ↩️ Step Back ]` at second 4.
   * **Verify:** Overlay immediately closes, device lands on Android Home screen, Instagram is not displayed.
2. **Locked Proceed Test:**
   * Launch Instagram.
   * Verify `[ Open App ]` is disabled and ticks down 5s → 4s → ... → 0s.
   * Tap before 0s -> Verify zero action.
   * At 0s, tap `[ Open App ]` -> Verify Instagram opens.
3. **Sequential Handover Test:**
   * Configure Instagram daily limit = 30m, already consumed 35m.
   * Launch Instagram -> Complete 5s Pre-Open Friction -> Tap `[ Open App ]`.
   * **Verify:** Instagram does not open freely; Phase 1 intervention overlay (`5m | 10m | 20m | 40m`) immediately cuts in.

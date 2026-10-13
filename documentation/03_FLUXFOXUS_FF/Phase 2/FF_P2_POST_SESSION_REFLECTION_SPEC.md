# 📝 Topic FF-P2-5: Post-Session Flow Rating Specification

**Document ID:** `FF_P2_POST_SESSION_REFLECTION_SPEC`  
**Phase:** Phase 2 (Hardened Enforcement & Advanced Scheduling)  
**Target Package:** `com.flux.focus.features.reflection`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + Android Notification Action Intents)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Quantitative time tracking (e.g. "Focused for 120 minutes") fails to capture cognitive quality. A user can sit in front of a textbook for two hours while daydreaming or battle through intense, unbroken flow for 45 minutes.

**Post-Session Flow Rating** captures instant subjective focus quality via a frictionless 1-tap interaction immediately upon session completion.

### 1.1 The Frictionless 1-Tap Rule
* **Zero Form-Filling:** No lengthy surveys, no mandatory sliders, no mandatory text fields.
* **Instant Submission:** Tapping a rating tile immediately records the value to Room and navigates to the summary screen.
* **Explicit Skip Option:** If the user does not want to rate, an unambiguous `[ Skip → ]` button logs `null` without penalizing metrics.

---

## 2. Dependencies & Phase 1 Integration

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Session Completion Trigger** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Fires when countdown timer hits 00:00:00 or when user completes an open-ended session. |
| **Room Session Entity** | [`FF_P1_PRESETS_AND_FOCUS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESETS_AND_FOCUS_SPEC.md) | Stores `reflection_rating: String?` on `focus_sessions` table (`LOW`, `MEDIUM`, `HIGH`, or `null`). |
| **Planner Timeline Cards** | [`FF_P2_SCHEDULED_SESSIONS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_SCHEDULED_SESSIONS_SPEC.md) | Completed session cards display a subtle rating badge (`🔥`, `😐`, `😴`). |
| **Weekly Review & Stats** | [`FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md) | Aggregates weekly focus quality score across completed sessions. |

---

## 3. Feature Specifications & Interaction Flow

### 3.1 Trigger Conditions & Default State
* **Default State:** **ON by default** in `Settings → Sessions → Post-Session Reflection`.
* **Triggers When:**
  * Countdown timer reaches natural completion.
  * Stopwatch / Open-ended session is manually finished via `Finish Session`.
* **Does NOT Trigger When:**
  * Session is prematurely quit or abandoned via the quit friction gate.

---

### 3.2 In-App Rating Bottom Sheet UI

Appears immediately after completion chime:

```
+-------------------------------------------------------+
|  ━  Session Complete ✓                                |
+-------------------------------------------------------+
|  Deep Research & Drafting · 1h 45m Focused            |
|                                                       |
|  How focused were you?                                |
|                                                       |
|  +-------------+   +-------------+   +-------------+  |
|  |   😴 Low    |   |  😐 Medium  |   |   🔥 High   |  |
|  +-------------+   +-------------+   +-------------+  |
|                                                       |
|                                            [ Skip → ] |
+-------------------------------------------------------+
```

#### Interaction Rules:
1. **Modal Inviolability:** Backdrop clicks outside the sheet and Android back gestures are blocked during display. User must tap one of the 3 tiles or `[ Skip → ]`.
2. **Instant Action:**
   * Tapping `😴 Low`: Saves `LOW` to Room DB, dismisses sheet immediately.
   * Tapping `😐 Medium`: Saves `MEDIUM` to Room DB, dismisses sheet immediately.
   * Tapping `🔥 High`: Saves `HIGH` to Room DB, dismisses sheet immediately.
   * Tapping `[ Skip → ]`: Saves `null` to Room DB, dismisses sheet immediately.

---

### 3.3 Background Notification Fallback

If the session completes while the device is locked or the user is outside FluxFoxus:
* High-Priority Completion Notification fires.
* Notification adds 3 direct inline action buttons:
  * `[ 😴 Low ]` | `[ 😐 Medium ]` | `[ 🔥 High ]`
* **Broadcast Intent Handling:**
  Tapping any notification action triggers `SessionReflectionReceiver`, which updates Room DAO directly in the background and clears the notification tray without requiring an activity launch.

---

## 4. Database Schema & Room Entities

### 4.1 Schema Update on `focus_sessions` Table

```kotlin
// Added to FocusSessionEntity:
@ColumnInfo(name = "reflection_rating")
val reflectionRating: String? = null // "LOW", "MEDIUM", "HIGH", or null if skipped
```

### 4.2 Room DAO Method (`FocusSessionDao.kt`)

```kotlin
@Query("UPDATE focus_sessions SET reflection_rating = :rating, updated_at = :updatedAt WHERE id = :sessionId")
suspend fun updateSessionReflection(sessionId: String, rating: String?, updatedAt: Long = System.currentTimeMillis())
```

### 4.3 Notification Receiver (`SessionReflectionReceiver.kt`)

```kotlin
package com.flux.focus.features.reflection.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flux.focus.features.reflection.domain.repository.ReflectionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionReflectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reflectionRepository: ReflectionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra("EXTRA_SESSION_ID") ?: return
        val rating = intent.getStringExtra("EXTRA_RATING") // "LOW", "MEDIUM", "HIGH"

        CoroutineScope(Dispatchers.IO).launch {
            reflectionRepository.recordRating(sessionId, rating)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(sessionId.hashCode())
    }
}
```

---

## 5. Jetpack Compose UI Specifications

### 5.1 PostSessionReflectionSheet Composable

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostSessionReflectionSheet(
    sessionTitle: String,
    durationFormatted: String,
    onRatingSelected: (rating: String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { /* Prevent dismiss on outside tap */ },
        containerColor = Color(0xFF1B263B),
        scrimColor = Color(0xCC0D1B2A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Session Complete ✓",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF778D7A),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$sessionTitle · $durationFormatted Focused",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA0AAB2)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "How focused were you?",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF4F1DE),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The 3 Rating Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RatingTile(
                    emoji = "😴",
                    label = "Low",
                    modifier = Modifier.weight(1f),
                    onClick = { onRatingSelected("LOW") }
                )
                RatingTile(
                    emoji = "😐",
                    label = "Medium",
                    modifier = Modifier.weight(1f),
                    onClick = { onRatingSelected("MEDIUM") }
                )
                RatingTile(
                    emoji = "🔥",
                    label = "High",
                    modifier = Modifier.weight(1f),
                    onClick = { onRatingSelected("HIGH") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Skip Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = { onRatingSelected(null) }
                ) {
                    Text(
                        text = "Skip →",
                        color = Color(0xFFA0AAB2),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RatingTile(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D1B2A),
        border = BorderStroke(1.dp, Color(0xFF415A77))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFF4F1DE),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
```

### 5.2 Holst Design Tokens

| Token | Hex | Element / Usage |
|---|---|---|
| Sheet Surface | `SurfaceCard` (`#1B263B`) | Bottom sheet container |
| Tile Background | `BackgroundPrimary` (`#0D1B2A`) | Unselected rating tile background |
| Tile Border | `SurfaceBorder` (`#415A77`) | Tile stroke |
| Tile Text | `TextLight` (`#F4F1DE`) | Tile labels |
| Accent Header | `AccentSage` (`#778D7A`) | "Session Complete ✓" header |
| Muted Controls | `TextMuted` (`#A0AAB2`) | Duration subtitle, `[ Skip → ]` button |

---

## 6. Verification & Testing Workflow

### 6.1 Zero-Mock Automated & Manual Test Cases
1. **Interactive Tap Test:**
   * Start a 1-minute countdown session.
   * Upon completion, verify bottom sheet appears.
   * Tap `🔥 High`.
   * **Verify:** Sheet immediately dismisses, Room DB record for the session has `reflection_rating = "HIGH"`.
2. **Skip Test:**
   * Complete session, tap `[ Skip → ]`.
   * **Verify:** Sheet dismisses, Room DB record has `reflection_rating = null`.
3. **Background Notification Action Test:**
   * Run session with app minimized.
   * On completion, examine notification banner on lock screen.
   * Tap inline `[ 😐 Medium ]` action button.
   * **Verify:** Notification dismisses, `SessionReflectionReceiver` writes `MEDIUM` to Room DB without opening the app UI.

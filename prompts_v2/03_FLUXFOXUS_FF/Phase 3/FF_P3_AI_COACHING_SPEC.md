# 🤖 Topic FF-P3-4: AI Focus Coaching & Granular Settings Hub Specification

**Document ID:** `FF_P3_AI_COACHING_SPEC`  
**Phase:** Phase 3 (Intelligence, Lock Screen Presence & Session Ergonomics)  
**Target Package:** `com.flux.focus.features.ai`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Google Gemini Kotlin SDK + Android Keystore + Jetpack DataStore + WorkManager)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

AI in FluxFoxus is not a conversational companion or motivational cheerleader. In alignment with the Flux core philosophy (discipline over motivation), the AI acts as an **analytical, data-driven academic coach**. It examines raw study metrics, identifies fatigue and distraction patterns, and delivers concise, objective observations.

### 1.1 Non-Negotiable Granular Toggle Rule
* **Master vs. Granular Control:** In addition to a master `Enable AI Features` switch, **every individual AI touchpoint has its own independent toggle in Settings**.
* Users maintain total autonomy to keep AI Break Negotiation enabled while disabling Streak Coaching, or enable Session Debriefs while disabling Preset Suggestions.
* If a feature toggle is `OFF`, that feature’s UI elements, background workers, and API requests are completely suppressed.

---

## 2. Granular AI Settings Hub (`/settings/ai`)

Located in App Settings under the **AI Intelligence** section:

```
+-------------------------------------------------------+
|  ←  AI Intelligence & Coaching                       |
+-------------------------------------------------------+
|                                                       |
|  MASTER CONFIGURATION                                 |
|  +-------------------------------------------------+  |
|  | Enable AI Features               [ ON ] (●)     |  |
|  | Gemini 1.5 Flash · API Key: [••••••••••••] (🔒) |  |
|  +-------------------------------------------------+  |
|                                                       |
|  INDIVIDUAL FEATURE TOGGLES                           |
|  +-------------------------------------------------+  |
|  | 1. Break Negotiation             [ ON ] (●)     |  |
|  |    Negotiate emergency breaks when presets run out|  |
|  +-------------------------------------------------+  |
|  | 2. Post-Session Debrief          [ ON ] (●)     |  |
|  |    "How did I do?" analysis after completed blocks|  |
|  +-------------------------------------------------+  |
|  | 3. Preset Suggester              [ ON ] (●)     |  |
|  |    Home screen contextual study recommendations  |  |
|  +-------------------------------------------------+  |
|  | 4. Streak Risk Coach             [ ON ] (●)     |  |
|  |    Tailored evening alerts when streak is at risk |  |
|  +-------------------------------------------------+  |
|  | 5. Negotiation Auto-Tuner        [ ON ] (●)     |  |
|  |    Difficulty adjustments based on win/loss streaks|  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

---

## 3. The 4 Phase 3 Intelligence Touchpoints

### 3.1 Touchpoint A: AI Session Debrief (ASD)
* **Trigger:** Appears on the session summary screen after natural completion (following the Flow Rating sheet from Phase 2).
* **Entry Point:** Secondary text link: *"How did I do? →"* (only shown if `ai_feature_session_debrief == true`).
* **Debrief Bottom Sheet:**
  * Header: *"[Deep Research] · 1h 45m · Flow: 🔥 High"*
  * AI Observation: Maximum 80 words. Strictly analytical. Highlights focus-to-break ratio and time-of-day stamina.
  * Follow-Up Interaction: **Allows exactly 1 follow-up question**. After the AI replies once, the input field permanently locks with label: *"One follow-up per session"*.
* **Prompt Contract:**
  ```
  System: You are an analytical, data-driven study coach. Provide a concise, objective observation (max 80 words). Do not give generic cheerleading or empty praise. Focus on focus-to-break pacing and stamina.
  Input: Session: {name}, Duration: {duration}, Breaks: {breaksTaken}/{breaksAllowed}, Flow Rating: {rating}, Time: {startHour}.
  ```

---

### 3.2 Touchpoint B: AI Preset Suggester (APS)
* **Trigger:** Daily WorkManager background task at **06:00 AM** analyzes the last 14 days of session history.
* **UI Surface:** A subtle, outlined recommendation chip directly below the Mode Selector on `Tab.Home`:
  ```
  [ 💡 Suggested: ⚡ Deep Work (Your best focus happens at 10:00 AM) ]  [×]
  ```
* **Behavior:**
  * Tapping chip pre-loads the suggested preset into the focus launcher.
  * Tapping `[×]` dismisses the suggestion for 24 hours.
  * Suppressed if `ai_feature_preset_suggester == false` or if fewer than 5 lifetime sessions exist.

---

### 3.3 Touchpoint C: AI Streak Risk Coach (ASC)
* **Trigger:** WorkManager evaluates daily progress at **6:00 PM** on non-relief days (Monday–Saturday).
* **Risk Conditions (All Must Be True):**
  1. `ai_feature_streak_coach == true`.
  2. Current streak $\ge 1$ day.
  3. Zero completed sessions logged today.
  4. Today is not a designated relief day (e.g. Sunday).
  5. Current available grace shields $== 0$ OR streak $\le 3$ days.
* **Delivery:** A single High-Priority push notification:
  * Title: *"Streak at risk 🔥"*
  * Body: Tailored, analytical nudge (max 60 words):  
    > *"Your 47-day streak is on the line tonight. You average 50 minutes of focus on Tuesday evenings. Launch 1 session before midnight to keep your momentum."*
  * Action: `[ Start Focusing ]` opens `Tab.Focus`. Max 1 alert per day.

---

### 3.4 Touchpoint D: AI Difficulty Auto-Tuner (ADA)
* **Trigger:** Evaluated automatically after every AI Break Negotiation resolves (`FF_P2_BREAK_NEGOTIATION_SPEC.md`).
* **Condition:**
  * If user **wins 5 consecutive negotiations** at current difficulty: Suggests increasing difficulty (e.g. Medium → Hard).
  * If user **loses 5 consecutive negotiations**: Suggests decreasing difficulty (e.g. Hard → Medium).
* **UI Modal:**
  * Container: Surface Card `#1B263B`, Border `#778D7A`.
  * Title: *"Break Difficulty Review"*
  * Rationale: *"You have won 5 consecutive break negotiations on Medium difficulty. Your discipline has matured. We recommend upgrading to Hard difficulty."*
  * Actions: `[ Update to Hard ]` (Primary) | `[ Keep Current ]` (Secondary).

---

## 4. Jetpack DataStore Preferences Schema

All feature flags are persisted in `AIFeaturePreferences`:

```kotlin
package com.flux.focus.features.ai.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AIFeaturePreferenceKeys {
    // Master Switches
    val AI_MASTER_ENABLED = booleanPreferencesKey("ai_master_enabled") // Default: false
    val GEMINI_API_KEY_CONFIGURED = booleanPreferencesKey("gemini_api_key_configured")

    // Granular Touchpoint Toggles
    val FEATURE_BREAK_NEGOTIATION = booleanPreferencesKey("ai_feature_break_negotiation") // Default: true
    val FEATURE_SESSION_DEBRIEF = booleanPreferencesKey("ai_feature_session_debrief")       // Default: true
    val FEATURE_PRESET_SUGGESTER = booleanPreferencesKey("ai_feature_preset_suggester")     // Default: true
    val FEATURE_STREAK_COACH = booleanPreferencesKey("ai_feature_streak_coach")             // Default: true
    val FEATURE_DIFFICULTY_TUNER = booleanPreferencesKey("ai_feature_difficulty_tuner")     // Default: true

    // Cache Keys
    val APS_SUGGESTED_PRESET_ID = stringPreferencesKey("aps_suggested_preset_id")
    val APS_SUGGESTION_REASON = stringPreferencesKey("aps_suggestion_reason")
    val APS_DISMISSED_TIMESTAMP = stringPreferencesKey("aps_dismissed_timestamp")
}
```

---

## 5. Domain Repository Interface

```kotlin
package com.flux.focus.features.ai.domain.repository

import kotlinx.coroutines.flow.Flow

interface AICoachingRepository {
    fun observeIsFeatureEnabled(key: String): Flow<Boolean>
    suspend fun setFeatureEnabled(key: String, enabled: Boolean)
    
    suspend fun generateSessionDebrief(sessionId: String): Result<String>
    suspend fun answerDebriefFollowUp(sessionId: String, followUpQuestion: String): Result<String>
    
    suspend fun computePresetSuggestion(): Result<PresetSuggestion?>
    suspend fun dismissPresetSuggestion()
    
    suspend fun evaluateStreakRiskAndNotify()
    suspend fun checkDifficultyAdjustmentTrigger(): DifficultyRecommendation?
}
```

---

## 6. Jetpack Compose UI Specifications (`SessionDebriefSheet.kt`)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDebriefSheet(
    sessionTitle: String,
    durationFormatted: String,
    ratingEmoji: String?,
    debriefText: String?,
    followUpAnswer: String?,
    isLoading: Boolean,
    onSendFollowUp: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var followUpInput by remember { mutableStateOf("") }
    var followUpSent by remember { mutableStateOf(followUpAnswer != null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1B263B),
        scrimColor = Color(0xCC0D1B2A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analytical Debrief",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF4F1DE),
                    fontWeight = FontWeight.Bold
                )
                if (ratingEmoji != null) {
                    Text(text = ratingEmoji, style = MaterialTheme.typography.titleMedium)
                }
            }

            Text(
                text = "$sessionTitle · $durationFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA0AAB2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Color(0xFF778D7A)
                )
            } else if (debriefText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D1B2A),
                    border = BorderStroke(1.dp, Color(0xFF415A77))
                ) {
                    Text(
                        text = debriefText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF4F1DE),
                        lineHeight = 20.sp
                    )
                }
            }

            if (followUpAnswer != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B263B),
                    border = BorderStroke(1.dp, Color(0xFF778D7A))
                ) {
                    Text(
                        text = followUpAnswer,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD4C4A8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1 Follow-Up Input Row
            if (!followUpSent && debriefText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = followUpInput,
                        onValueChange = { followUpInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask 1 follow-up question...", color = Color(0xFFA0AAB2)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF778D7A),
                            unfocusedBorderColor = Color(0xFF415A77),
                            focusedTextColor = Color(0xFFF4F1DE)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (followUpInput.isNotBlank()) {
                                onSendFollowUp(followUpInput)
                                followUpSent = true
                            }
                        }
                    ) {
                        Text("➔", color = Color(0xFFD4C4A8), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else if (followUpSent) {
                Text(
                    text = "One follow-up per session completed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA0AAB2),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
```

---

## 7. Verification & Branch-Testing Isolation Workflow

### 7.1 Zero-Mock Automated & Manual Test Cases
1. **Granular Toggle Suppression Test:**
   * Toggle `FEATURE_SESSION_DEBRIEF` to OFF.
   * Complete focus session.
   * **Verify:** Session complete screen does NOT show the *"How did I do? →"* button.
2. **Turn-Cap Inviolability Test (ASD):**
   * Ask initial debrief, ask 1 follow-up question.
   * **Verify:** Input row disappears, replaced with *"One follow-up per session completed"*; no further API calls permitted.
3. **Streak Risk Notification Guard Test (ASC):**
   * Set simulated clock to 6:00 PM, 0 sessions completed today, 0 grace shields remaining.
   * **Verify:** WorkManager fires push notification with analytical nudge.
   * Complete 1 session at 6:30 PM.
   * **Verify:** Subsequent worker runs that evening do not resend the notification.

# 🤖 Topic FF-P2-3: Friction Break Extension & AI Negotiation Specification

**Document ID:** `FF_P2_BREAK_NEGOTIATION_SPEC`  
**Phase:** Phase 2 (Hardened Enforcement & Advanced Scheduling)  
**Target Package:** `com.flux.focus.features.breaks`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + Google Gemini Kotlin SDK + Local Stamina Protocol)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

In traditional timer apps, taking breaks is frictionless and unconstrained: when fatigue strikes or an urge to scroll appears, users repeatedly hit "Pause" or "Take Break" until the focus block loses all discipline.

Phase 1 established fixed preset breaks (e.g. 2 breaks of 5 minutes each). **Phase 2 introduces the AI Break Negotiation & Stamina Protocol**, which converts break extensions beyond preset limits into a disciplined friction gate.

### 1.1 The Exhaustion Unlock Progression
1. **Tier 1 (Preset Discipline):** The user consumes their allotted preset breaks with zero friction.
2. **Tier 2 (AI / Algorithmic Negotiation):** When preset breaks reach **0 (exhausted)**, the break button transforms from disabled into an active negotiation portal:
   * **Online with API Key:** `[ Negotiate Break 🤖 ]` → Interactive AI Coach evaluates fatigue vs. escape.
   * **Offline or No API Key:** `[ Emergency Break ⚡ ]` → Strict mathematical Stamina Ratio Protocol.

---

## 2. Dependencies & Phase 1 Integration

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Break Controls Layout** | [`FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_FLIP_CLOCK_AND_SESSION_SPEC.md) | Row 1: `[ Take Break (75%) ]` + `[ Pause ⏸ (25%) ]`. When preset breaks hit 0, `[ Take Break ]` morphs into `[ Negotiate Break 🤖 ]`. |
| **Session Room Entity** | [`FF_P1_PRESETS_AND_FOCUS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESETS_AND_FOCUS_SPEC.md) | `focus_sessions` tracks `breaks_taken`, `breaks_total_seconds`, and links foreign key to `ai_break_negotiations`. |
| **Anti-Tamper Lock** | [`FF_P2_UNINSTALL_PROTECTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_UNINSTALL_PROTECTION_SPEC.md) | Active session inviolability holds during breaks; focus restrictions immediately re-engage when break expires. |

---

## 3. Online AI Negotiation Architecture (Google Gemini)

### 3.1 Client SDK & Security Model
* **Official SDK:** `com.google.ai.client.generativeai:generativeai` targeting `gemini-1.5-flash` for sub-second streaming and structured JSON evaluation.
* **Encrypted Storage:** Gemini API Key is stored in Android Keystore via `EncryptedSharedPreferences`, never in plaintext SQLite or Room.

### 3.2 Structured System Prompt Contract
The model is constrained to act as an objective, empathetic but firm discipline coach. It returns strict JSON:

```
You are a firm, disciplined, yet understanding focus guardian for FluxFoxus.
A user has exhausted their pre-planned breaks and is requesting an emergency break.
Evaluate their stated reason, elapsed focus duration, and target session length.

Input Context:
- Session Title: {sessionTitle}
- Elapsed Focus Time: {elapsedMinutes}m / {targetMinutes}m
- Preset Breaks Exhausted: {presetBreakCount}
- Requested Break Duration: {requestedMinutes}m
- User Stated Reason: "{userReason}"
- Negotiation Round: {currentRound} / 3

Rules:
1. Short, vague, or impulsive reasons ("tired", "bored", "just want to check phone") must be DENIED.
2. Legitimate physical or cognitive fatigue (headache, bathroom, water, stretching after 40+ min focus) may be GRANTED or COUNTERED with a shorter duration (e.g. counter 10m request with 5m).
3. If currentRound == 3 and not earned, you must firmly DENY.
4. Response MUST be valid JSON only. Max 60 words in message.

JSON Response Schema:
{
  "message": "Direct, conversational feedback to user",
  "granted": true/false,
  "approved_duration_minutes": integer or null
}
```

### 3.3 Negotiation Rules & Anti-Distraction Turn Cap
* **Anti-Procrastination Turn Cap:** Capped at **maximum 3 rounds**.
  * Round 1: Initial request & AI response.
  * Round 2: Counter-argument / clarification.
  * Round 3: Final appeal. If denied on Round 3, negotiation terminates with zero break.
* **Outcome Handlers:**
  * **Granted:** Sheet auto-dismisses after 1.5 seconds; break timer activates immediately for `approved_duration_minutes`.
  * **Denied:** Sheet presents AI rationale and provides two buttons: `[ Accept & Keep Focusing ]` (primary) or `[ Appeal (Round N/3) ]`.

---

## 4. Offline Fallback: The Stamina Ratio Protocol

When device is offline, network fails, or no API key is provided, FluxFoxus activates the deterministic **Stamina Ratio Protocol**.

```
+-----------------------------------------------------------------------------------+
|                        The 3 Inflexible Mathematical Gates                        |
+-----------------------------------------------------------------------------------+
|  1. Continuous Stamina Gate:                                                      |
|     continuousFocusMinutes >= 45m (or >= 75% of target if target < 45m).          |
|                                                                                   |
|  2. Break-to-Focus Ratio Gate (4:1 Rule):                                         |
|     totalBreakSeconds / totalElapsedSeconds <= 0.20 (max 20% total break ratio).  |
|                                                                                   |
|  3. Emergency Frequency Cap:                                                      |
|     emergencyOfflineBreaksUsed < 2 per session (Max 2 emergency breaks).          |
+-----------------------------------------------------------------------------------+
```

### 4.1 Strict Cap on Offline Breaks
* Duration is **hard-capped at 5 minutes** (no 10m or 15m options allowed offline).
* Maximum **2 emergency offline breaks** per session.

### 4.2 Real-Time Stamina Checklist UI
Tapping `[ Emergency Break ⚡ ]` displays live status with exact time remaining:

```
+-------------------------------------------------------+
|  ⚡ Emergency Break Protocol                          |
+-------------------------------------------------------+
|  Status: Offline Guard (Deterministic Rule Engine)     |
|                                                       |
|  REQUIREMENTS FOR 5-MINUTE BREAK:                     |
|  [❌] Continuous Focus: 31m / 45m required            |
|  [✅] Total Break Ratio: 12% / 20% max                |
|  [✅] Emergency Breaks: 0 / 2 used                    |
|                                                       |
|  Push through another 14 minutes of continuous focus  |
|  to unlock your next 5-minute break.                  |
|                                                       |
|  +-------------------------------------------------+  |
|  |           [ Return to Focus ]                   |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

---

## 5. Database Schema & Room Entities

### 5.1 `ai_break_negotiations` Entity

```kotlin
package com.flux.focus.features.breaks.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flux.focus.core.database.entity.FocusSessionEntity

@Entity(
    tableName = "ai_break_negotiations",
    foreignKeys = [
        ForeignKey(
            entity = FocusSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("session_id"),
        Index("created_at")
    ]
)
data class AIBreakNegotiationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "engine_type")
    val engineType: String, // "GEMINI_AI" or "STAMINA_PROTOCOL"

    @ColumnInfo(name = "rounds_count")
    val roundsCount: Int,

    @ColumnInfo(name = "conversation_transcript_json")
    val conversationTranscriptJson: String, // JSON array of messages

    @ColumnInfo(name = "granted")
    val granted: Boolean,

    @ColumnInfo(name = "granted_duration_minutes")
    val grantedDurationMinutes: Int?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
```

### 5.2 Room DAO Interface

```kotlin
package com.flux.focus.features.breaks.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flux.focus.features.breaks.data.entity.AIBreakNegotiationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIBreakNegotiationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNegotiation(entity: AIBreakNegotiationEntity)

    @Query("SELECT * FROM ai_break_negotiations WHERE session_id = :sessionId ORDER BY created_at ASC")
    fun observeNegotiationsForSession(sessionId: String): Flow<List<AIBreakNegotiationEntity>>

    @Query("SELECT COUNT(*) FROM ai_break_negotiations WHERE session_id = :sessionId AND engine_type = 'STAMINA_PROTOCOL' AND granted = 1")
    suspend fun getOfflineEmergencyBreaksUsedCount(sessionId: String): Int
}
```

---

## 6. Jetpack Compose UI Specifications

### 6.1 Negotiation Chat Bottom Sheet (`AIBreakNegotiationSheet.kt`)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBreakNegotiationSheet(
    sessionTitle: String,
    elapsedMinutes: Int,
    currentRound: Int,
    chatMessages: List<NegotiationChatMessage>,
    isEvaluating: Boolean,
    onSendMessage: (reason: String, requestedMinutes: Int) -> Unit,
    onAcceptCounter: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1B263B),
        scrimColor = Color(0xCC0D1B2A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Break Negotiation",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF4F1DE),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$sessionTitle · Focused: ${elapsedMinutes}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0AAB2)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF415A77).copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Round $currentRound / 3",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD4C4A8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chat Messages Thread
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageBubble(message)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Row: Requested Duration Chips + Reason Field
            if (currentRound <= 3 && !isEvaluating) {
                NegotiationInputArea(onSendMessage = onSendMessage)
            }
        }
    }
}
```

### 6.2 Holst Theme Design Tokens

| UI Element | Token / Color | Usage |
|---|---|---|
| Background Card | `SurfaceCard` (`#1B263B`) | Bottom sheet background |
| AI Message Bubble | `SurfaceCard` with `AccentSage` Border | Guardian feedback (`#1B263B` / `#778D7A`) |
| User Message Bubble | `SurfaceBorder` (`#415A77`) | User argument bubble |
| Send / Primary CTA | `AccentSage` (`#778D7A`) | Send reason button |
| Denied Alert | `DangerCoral` (`#E07A5F`) | Strict refusal banner |
| Duration Chips | `PrimarySand` (`#D4C4A8`) | `[ 5m ]`, `[ 10m ]`, `[ 15m ]` selection |

---

## 7. Verification & Branch-Testing Isolation Workflow

### 7.1 Zero-Mock Automated & Manual Test Cases
1. **Button Transformation Test:**
   * Start session with 1 preset break.
   * Take 1 preset break.
   * **Verify:** As soon as break completes, button displays `[ Negotiate Break 🤖 ]`.
2. **Turn Cap Enforcement Test:**
   * Enter vague reasons ("I want to look at phone") for 3 consecutive rounds.
   * **Verify:** On 3rd denial, input field locks, message reads: *"Maximum negotiation rounds reached. Break denied. Return to your focus session."*
3. **Stamina Protocol Verification Test:**
   * Disconnect network / disable API key.
   * Tap button at 20m elapsed focus.
   * **Verify:** Stamina modal shows `Continuous Focus: 20m / 45m [❌]`, claim button disabled.
   * Simulate 45m elapsed focus with 0 breaks taken.
   * **Verify:** All 3 checklist gates turn green `[✅]`, `[ Claim 5-Minute Break ]` enables.
   * Claim 1st break, resume session, claim 2nd break at 95m.
   * **Verify:** After 2nd emergency break, button displays permanently locked state.

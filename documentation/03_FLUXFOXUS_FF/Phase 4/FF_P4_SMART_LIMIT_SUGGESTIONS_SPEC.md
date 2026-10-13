# 📈 Topic FF-P4-4: Smart Usage Anomaly Detection & Limit Suggestions Specification

**Document ID:** `FF_P4_SMART_LIMIT_SUGGESTIONS_SPEC`  
**Phase:** Phase 4 (Cross-Platform Daemon, Group Limits & Habit Breaking)  
**Target Package:** `com.flux.focus.features.limits.suggestions`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + UsageStatsManager + WorkManager + Google Gemini Kotlin SDK)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Static application limits frequently fall out of sync with real behavioral progress:
1. **Redundantly Loose Limits:** A user initially sets a 60-minute limit on Instagram. Over 30 days of discipline, their actual daily usage drops to an average of 18 minutes. The 60-minute ceiling remains as a dangerous safety net, allowing accidental relapse spikes without friction.
2. **Chronically Frustrating Limits:** A user sets an overly ambitious 10-minute limit on a tool app they need for communication. The limit triggers every day by 11:00 AM, forcing continuous overrides, habituating the user to ignoring interventions, or creating learned helplessness.

**Topic FF-P4-4 introduces Smart Usage Anomaly Detection & Limit Suggestions.**  
The system monitors rolling 14-day and 30-day usage metrics entirely on-device, identifies statistical misalignments between current budgets and actual habits, and generates high-impact, actionable limit adjustments.

### 1.1 Core Behavioral Principles
* **Discipline-First Priority:** Tighten suggestions **strictly take priority** over loosen suggestions. When multiple candidates qualify, the engine always surfaces reductions in distracting time before considering budget increases.
* **Sunday Weekly Report Only (Zero Mid-Week Spam):** Suggestions are never delivered via intrusive popups or mid-week push notifications. They are compiled at Sunday midnight and presented calmly in the **Sunday Weekly Report** on `Tab.Stats` during natural weekly reflection.
* **Strict Anti-Nagging Cooldown:** Once an application or category pool receives a suggestion (whether applied or dismissed), it enters a **14-day cooldown period** during which no new suggestions can be generated for that target.
* **Maximum 3 Suggestions per Week:** To avoid cognitive overload, the engine caps output at 3 recommendations per weekly report.
* **Granular Settings Control:** Governed by `FEATURE_SMART_LIMIT_SUGGESTIONS` in `/settings/ai`, honoring the universal Flux rule that every AI/statistical feature can be independently toggled.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **App Limits Baseline** | [`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md) | Applying a suggestion executes an atomic update to `app_limits.daily_budget_minutes` via `AppLimitDao`. |
| **Category Limits** | [`FF_P4_CATEGORY_LIMITS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%204/FF_P4_CATEGORY_LIMITS_SPEC.md) | Supports category pools; updating a category suggestion modifies `category_limits.daily_budget_minutes`. |
| **AI Settings Hub** | [`FF_P3_AI_COACHING_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%203/FF_P3_AI_COACHING_SPEC.md) | Integrates with `AIFeaturePreferences` and the `/settings/ai` toggle screen. |
| **Usage Stats Tracking** | [`FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md) | Aggregates daily usage minutes per package from Android `UsageStatsManager` / Room caches. |

---

## 3. Anomaly Detection Algorithms & Mathematical Models

All evaluation executes on a background coroutine via `SundayWeeklyReportWorker`. The engine evaluates two deterministic rules:

### 3.1 Rule A: Habit Tightening (High Priority — Discipline First)

Identifies apps where the user has outgrown their limit and consistently uses significantly less than allocated.

#### Qualification Criteria:
An app or category pool qualifies for a **TIGHTEN** suggestion if:
1. **Sufficient History:** At least 21 days of usage stats exist in the rolling 30-day window.
2. **Chronic Under-Utilization:** On at least **14 of the last 30 days**, actual usage was $\le 60\%$ of the current daily budget:
   $$\text{DaysWithUsage}(\text{actual} \le 0.60 \times B_{\text{current}}) \ge 14$$
3. **Average Deficit:** The 30-day average usage $\bar{U}_{30}$ is $\le 65\%$ of the budget:
   $$\bar{U}_{30} \le 0.65 \times B_{\text{current}}$$
4. **Current Budget Minimum:** $B_{\text{current}} \ge 25\text{ minutes}$ (limits of 15m or 20m cannot be tightened further).

#### Suggested Budget Calculation:
The tightened budget provides a realistic 25% buffer over the user's actual 30-day average, rounded to the nearest 5 minutes:
$$B_{\text{suggested}} = \max\left(15, \text{roundToNearest5}\left(\bar{U}_{30} \times 1.25\right)\right)$$
* **Reduction Constraint:** $B_{\text{suggested}}$ must reduce the current budget by at least 10 minutes ($B_{\text{current}} - B_{\text{suggested}} \ge 10$). If the calculated reduction is $< 10\text{m}$, the suggestion is discarded.

*Example:*  
Current Limit: $60\text{m}$.  
30-Day Average Usage: $22\text{m}$ (under $36\text{m}$ on 19 days).  
$\text{Buffer Calculation} = 22 \times 1.25 = 27.5\text{m} \to \text{rounded} = 30\text{m}$.  
Reduction: $60\text{m} - 30\text{m} = 30\text{m}$ (Valid $\ge 10\text{m}$).  
Result: **Tighten from 60m to 30m (-30m)**.

---

### 3.2 Rule B: Habit Loosening / Chronic Friction (Low Priority)

Identifies apps where an unrealistic limit triggers constant friction and overrides, signaling an unsustainable setting that undermines user trust in the app.

#### Qualification Criteria:
An app qualifies for a **LOOSEN** suggestion if:
1. **Chronic Over-Run:** The user triggered the intervention overlay on at least **10 of the last 14 days**:
   $$\text{BreachDays}_{14} \ge 10$$
2. **Category Safeguard:**  
   * Apps categorized as `DISTRACTING` (e.g. Instagram, TikTok, YouTube Shorts) are **strictly disqualified from auto-loosening** unless the user has manually exempted them. For distracting apps, chronic breaches suggest addiction, not limit insufficiency—the app continues strict enforcement.
   * Loosening is permitted **only** for `SEMI_PRODUCTIVE`, `PRODUCTIVE`, or unclassified utility packages (e.g. Slack, WhatsApp, Gmail, Maps).
3. **Current Budget Ceiling:** $B_{\text{current}} \le 120\text{ minutes}$.

#### Suggested Budget Calculation:
Loosening is intentionally conservative (+15 minutes maximum) to prevent binge opening:
$$B_{\text{suggested}} = \min\left(180, B_{\text{current}} + 15\right)$$

*Example:*  
Current Limit on Slack: $30\text{m}$ (`SEMI_PRODUCTIVE`).  
Breached on 12 of the last 14 days due to work communications.  
Result: **Adjust from 30m to 45m (+15m)**.

---

### 3.3 Priority Sorting & Selection Pipeline

When `SundayWeeklyReportWorker` evaluates all tracked apps and categories:

```
[ All Tracked Packages & Categories ]
                 │
                 ▼
 ┌──────────────────────────────────────────┐
 │ Filter: Cooldown Check                   │
 │ Exclude if suggested in last 14 days     │
 └──────────────────────────────────────────┘
                 │
                 ▼
 ┌──────────────────────────────────────────┐
 │ Evaluate Tightening & Loosening Rules    │
 └──────────────────────────────────────────┘
                 │
                 ▼
 ┌────────────────────────────────────────────────────────┐
 │ Sort Candidates:                                       │
 │ 1. All TIGHTEN candidates first                        │
 │    (Ranked descending by minute reduction: B_cur - B_s) │
 │ 2. All LOOSEN candidates second                        │
 │    (Ranked descending by breach frequency: BreachDays)  │
 └────────────────────────────────────────────────────────┘
                 │
                 ▼
 ┌──────────────────────────────────────────┐
 │ Cap Selection:                           │
 │ Take TOP 3 candidates                    │
 └──────────────────────────────────────────┘
                 │
                 ▼
 ┌──────────────────────────────────────────┐
 │ Enrich Rationale (Gemini AI or Template) │
 └──────────────────────────────────────────┘
                 │
                 ▼
 [ Insert into smart_limit_suggestions Room ]
```

---

## 4. Analytical Rationale Engine (Hybrid AI / Offline)

Suggestions include a concise, evidence-based rationale (max 40 words).

### 4.1 Mode A: Gemini 1.5 Flash Enrichment (When AI Master & Key Active)
If `AI_MASTER_ENABLED == true` and `GEMINI_API_KEY_CONFIGURED == true`:
* A single batch prompt queries Gemini with anonymized stats:
```
System: You are an analytical behavioral data auditor for FluxFoxus. 
Write a single objective, non-preachy sentence (max 35 words) explaining why this app limit adjustment is recommended based on the user's data.

Input:
- App: {appLabel}
- Suggestion: {suggestionType} (from {currentBudget}m to {suggestedBudget}m)
- 30-Day Avg Usage: {avgUsage}m
- Low Usage Days: {lowUsageDays}/30
- Breach Days: {breachDays}/14

Return JSON:
{
  "rationale": "string"
}
```

### 4.2 Mode B: Deterministic Offline Templates (Default / Air-Gapped)
If AI is disabled or offline, deterministic templates format the rationale instantly with zero latency or external dependencies:

* **Tighten Template:**
  > *"You used less than {60% threshold}m on {lowUsageDays} of the last 30 days (averaging {avgUsage}m). Tightening to {suggestedBudget}m locks in your habit gains."*

* **Loosen Template:**
  > *"You hit your {currentBudget}m limit on {breachDays} of the last 14 days. A 15m adjustment gives breathing room for essential communication while maintaining boundaries."*

---

## 5. Room Database Architecture

### 5.1 Entity: `SmartLimitSuggestionEntity`

```kotlin
package com.flux.focus.features.limits.suggestions.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "smart_limit_suggestions",
    indices = [
        Index(value = ["target_identifier"]),
        Index(value = ["week_start_date"]),
        Index(value = ["status"])
    ]
)
data class SmartLimitSuggestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "target_type")
    val targetType: String, // 'INDIVIDUAL_APP' or 'CATEGORY_POOL'

    @ColumnInfo(name = "target_identifier")
    val targetIdentifier: String, // Package name or Category name (e.g. "DISTRACTING")

    @ColumnInfo(name = "target_label")
    val targetLabel: String, // Display label e.g. "Instagram" or "Distracting Apps"

    @ColumnInfo(name = "suggestion_type")
    val suggestionType: String, // 'TIGHTEN' or 'LOOSEN'

    @ColumnInfo(name = "current_budget_minutes")
    val currentBudgetMinutes: Int,

    @ColumnInfo(name = "suggested_budget_minutes")
    val suggestedBudgetMinutes: Int,

    @ColumnInfo(name = "rationale")
    val rationale: String, // Max 40 words

    @ColumnInfo(name = "status")
    val status: String, // 'PENDING', 'APPLIED', 'DISMISSED', 'EXPIRED'

    @ColumnInfo(name = "week_start_date")
    val weekStartDate: String, // 'YYYY-MM-DD' (Sunday date)

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "actioned_at")
    val actionedAt: Long? = null
)
```

### 5.2 DAO: `SmartLimitSuggestionDao`

```kotlin
package com.flux.focus.features.limits.suggestions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flux.focus.features.limits.suggestions.data.entity.SmartLimitSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartLimitSuggestionDao {

    @Query("SELECT * FROM smart_limit_suggestions WHERE week_start_date = :weekStartDate AND status = 'PENDING' ORDER BY suggestion_type DESC, (current_budget_minutes - suggested_budget_minutes) DESC")
    fun getPendingSuggestionsForWeek(weekStartDate: String): Flow<List<SmartLimitSuggestionEntity>>

    @Query("SELECT * FROM smart_limit_suggestions WHERE week_start_date = :weekStartDate ORDER BY created_at DESC")
    fun getAllSuggestionsForWeek(weekStartDate: String): Flow<List<SmartLimitSuggestionEntity>>

    @Query("SELECT MAX(created_at) FROM smart_limit_suggestions WHERE target_identifier = :targetIdentifier")
    suspend fun getLastSuggestionTimestamp(targetIdentifier: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: List<SmartLimitSuggestionEntity>)

    @Query("UPDATE smart_limit_suggestions SET status = :newStatus, actioned_at = :actionedAt WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String, actionedAt: Long)

    @Query("UPDATE smart_limit_suggestions SET status = 'EXPIRED' WHERE status = 'PENDING' AND week_start_date < :currentWeekStartDate")
    suspend fun expireOldPendingSuggestions(currentWeekStartDate: String)
}
```

---

## 6. WorkManager Scheduling & Execution Flow

### 6.1 Worker Contract
* **Class:** `SundayWeeklyReportWorker`
* **Trigger:** Periodic WorkManager scheduled every **Sunday at 00:05 AM**.
* **Constraints:** `BatteryNotLow`, `DeviceIdle` (preferred).

### 6.2 Execution Lifecycle:
```kotlin
class SundayWeeklyReportWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val suggestionEngine: SmartLimitSuggestionEngine,
    private val suggestionDao: SmartLimitSuggestionDao,
    private val aiPreferences: AIFeaturePreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val isFeatureEnabled = aiPreferences.smartLimitSuggestionsEnabled.first()
        if (!isFeatureEnabled) {
            return Result.success()
        }

        val todayDate = getCurrentDateString() // e.g. "2026-10-18" (Sunday)
        
        // 1. Expire stale pending suggestions from prior weeks
        suggestionDao.expireOldPendingSuggestions(todayDate)

        // 2. Generate new suggestions based on 30-day usage stats
        val suggestions = suggestionEngine.evaluateWeeklyAnomalies(todayDate)

        // 3. Persist new suggestions
        if (suggestions.isNotEmpty()) {
            suggestionDao.insertSuggestions(suggestions)
        }

        return Result.success()
    }
}
```

---

## 7. UI Specifications (Sunday Weekly Report on `Tab.Stats`)

Suggestions are integrated directly into the Weekly Report screen on `Tab.Stats`.

### 7.1 Component Layout: `SmartLimitSuggestionsSection`

```
+-------------------------------------------------------+
|  WEEKLY LIMIT RECOMMENDATIONS (SUNDAY REVIEW)        |
+-------------------------------------------------------+
|  [ 📉 TIGHTEN ] Instagram                             |
|  Current: 60m  →  Suggested: 35m  (-25m)              |
|                                                       |
|  "You used under 36m on 21 of the last 30 days        |
|  (averaging 24m). Tightening to 35m locks in          |
|  your habit gains."                                   |
|                                                       |
|  +------------------------+  +---------------------+  |
|  |   [ Apply 35m Limit ]  |  |    [ Dismiss ]      |  |
|  +------------------------+  +---------------------+  |
+-------------------------------------------------------+
|  [ 📉 TIGHTEN ] Reddit                                |
|  Current: 45m  →  Suggested: 25m  (-20m)              |
|  ...                                                  |
+-------------------------------------------------------+
```

### 7.2 Holst Design Tokens for Suggestions

| Element | Color Token | Hex Code | Description |
|---|---|---|---|
| Card Container | `HolstTheme.colors.surfaceCard` | `#1B263B` | Solid elevated surface card. |
| Border | `HolstTheme.colors.borderSubtle` | `#415A77` | 1dp border stroke. |
| Tighten Badge Background | `HolstTheme.colors.accentSage.copy(alpha = 0.2f)` | `rgba(119, 141, 122, 0.2)` | Soft sage tint. |
| Tighten Badge Text | `HolstTheme.colors.accentSage` | `#778D7A` | Academic sage text (`[ 📉 TIGHTEN ]`). |
| Loosen Badge Background | `HolstTheme.colors.primarySand.copy(alpha = 0.2f)` | `rgba(212, 196, 168, 0.2)` | Soft sand tint. |
| Loosen Badge Text | `HolstTheme.colors.primarySand` | `#D4C4A8` | Warm sand text (`[ ⚖️ ADJUST ]`). |
| App Label Text | `HolstTheme.colors.textPrimary` | `#F4F1DE` | Bold 16sp title. |
| Rationale Text | `HolstTheme.colors.textSecondary` | `#A0AAB2` | 13sp body copy, max 40 words. |
| `[ Apply Limit ]` Button | Background `#778D7A`, Text `#0D1B2A` | `#778D7A` | High-contrast filled primary button. |
| `[ Dismiss ]` Button | Outlined `#415A77`, Text `#F4F1DE` | `#415A77` | Subtle outlined button. |

### 7.3 Action Handlers

#### When User Taps `[ Apply Limit ]`:
1. **Database Update:**  
   * If `targetType == 'INDIVIDUAL_APP'`: Updates `app_limits.daily_budget_minutes = suggestedBudgetMinutes`.
   * If `targetType == 'CATEGORY_POOL'`: Updates `category_limits.daily_budget_minutes = suggestedBudgetMinutes`.
2. **Suggestion Record:** Updates suggestion status to `'APPLIED'` and `actioned_at = System.currentTimeMillis()`.
3. **User Feedback:** Animated card collapse + Snackbar:  
   *"Instagram limit updated to 35m. Streak criteria will evaluate against the new budget starting tomorrow."*

#### When User Taps `[ Dismiss ]`:
1. **Suggestion Record:** Updates suggestion status to `'DISMISSED'` and `actioned_at = System.currentTimeMillis()`.
2. **Cooldown Start:** Enforces the 14-day silence window for that package.
3. **Card Dismissal:** Card fades out cleanly with `AnimatedVisibility(shrinkVertically)`.

#### Zero State (No Suggestions This Week):
When all limits are healthy and no candidates qualify:
```
+-------------------------------------------------------+
|  WEEKLY LIMIT RECOMMENDATIONS                         |
+-------------------------------------------------------+
|  ✓ All app limits align with your current habits.     |
|  No adjustments needed this week. Keep up the rhythm! |
+-------------------------------------------------------+
```

---

## 8. Settings & Privacy Architecture

### 8.1 DataStore Key Registration
In `AIFeaturePreferenceKeys`:
```kotlin
val FEATURE_SMART_LIMIT_SUGGESTIONS = booleanPreferencesKey("ai_feature_smart_limit_suggestions") // Default: true
```

### 8.2 UI Entry in `/settings/ai`
Located under the **Behavioral Optimization** section of AI Settings:
```
+-------------------------------------------------------+
|  Smart Limit Suggestions                        [ON]  |
|  Evaluates 30-day usage patterns and recommends       |
|  tightening loose limits in your Sunday Weekly        |
|  Report. Strictly discipline-first.                   |
+-------------------------------------------------------+
```

### 8.3 Privacy Contract: 100% On-Device Anomaly Detection
* **Zero Cloud Usage Logs:** Raw app package usage times, timestamps, and browsing activities **never leave the local device**.
* **Local Math:** The 14-day and 30-day statistical anomaly algorithms run entirely within the native Kotlin WorkManager process.
* **Anonymized AI Processing:** If Gemini AI is active for rationale generation, only anonymized aggregates (e.g. *"Daily limit: 60m, 30-day average: 22m"*) are passed to the model—never private telemetry.

---

## 9. Verification & Anti-Mocking Test Specifications

```kotlin
@RunWith(AndroidJUnit4::class)
class SmartLimitSuggestionEngineTest {

    private lateinit var db: FluxDatabase
    private lateinit var suggestionDao: SmartLimitSuggestionDao
    private lateinit var appLimitDao: AppLimitDao
    private lateinit var engine: SmartLimitSuggestionEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxDatabase::class.java).build()
        suggestionDao = db.smartLimitSuggestionDao()
        appLimitDao = db.appLimitDao()
        engine = SmartLimitSuggestionEngine(suggestionDao, appLimitDao, FakeUsageStatsRepository())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tightenSuggestion_triggered_whenUsageBelow60PercentFor14Days() = runTest {
        // Given: 60m limit on Instagram, actual 30-day average is 20m, 18 days under 36m
        val candidates = engine.evaluateWeeklyAnomalies("2026-10-18")
        
        val instagram = candidates.find { it.targetIdentifier == "com.instagram.android" }
        assertNotNull(instagram)
        assertEquals("TIGHTEN", instagram!!.suggestionType)
        assertEquals(60, instagram.currentBudgetMinutes)
        assertEquals(25, instagram.suggestedBudgetMinutes) // 20 * 1.25 = 25m
    }

    @Test
    fun disciplineFirst_tightenSuggestionsPrioritizedOverLoosen() = runTest {
        // Given: 2 tighten candidates and 2 loosen candidates
        val candidates = engine.evaluateWeeklyAnomalies("2026-10-18")
        
        // Assert: Cap at 3, with TIGHTEN always appearing before LOOSEN
        assertTrue(candidates.size <= 3)
        val types = candidates.map { it.suggestionType }
        if (types.contains("LOOSEN")) {
            val firstLoosenIndex = types.indexOf("LOOSEN")
            val lastTightenIndex = types.lastIndexOf("TIGHTEN")
            if (lastTightenIndex != -1) {
                assertTrue(lastTightenIndex < firstLoosenIndex)
            }
        }
    }

    @Test
    fun cooldownEnforced_packageExcludedIfSuggestedWithin14Days() = runTest {
        // Given: Prior suggestion created 7 days ago
        suggestionDao.insertSuggestions(listOf(createSampleSuggestion("com.reddit.frontpage", daysAgo = 7)))

        val candidates = engine.evaluateWeeklyAnomalies("2026-10-18")
        assertNull(candidates.find { it.targetIdentifier == "com.reddit.frontpage" })
    }

    @Test
    fun applyingSuggestion_updatesAppLimitDirectlyInDatabase() = runTest {
        val suggestion = createSampleSuggestion("com.instagram.android", suggestedBudget = 30)
        suggestionDao.insertSuggestions(listOf(suggestion))

        engine.applySuggestion(suggestion.id)

        val updatedLimit = appLimitDao.getLimitForPackage("com.instagram.android").first()
        assertEquals(30, updatedLimit.dailyBudgetMinutes)
        
        val updatedSuggestion = suggestionDao.getAllSuggestionsForWeek(suggestion.weekStartDate).first().first()
        assertEquals("APPLIED", updatedSuggestion.status)
    }
}
```

---

## 10. Deliverables & Acceptance Checklist

- [ ] **Deterministic Math Engine:** Statistical tightening triggers only when $\ge 14$ of 30 days are $\le 60\%$ of budget, with 25% safety buffer.
- [ ] **Discipline-First Order:** Tighten suggestions strictly precede loosen suggestions in rank order.
- [ ] **Sunday Weekly Delivery:** Worker fires exclusively on Sunday midnight; UI presents suggestions exclusively on `Tab.Stats` Weekly Report.
- [ ] **14-Day Anti-Nagging Cooldown:** Cooldown enforced on every package regardless of whether applied or dismissed.
- [ ] **Max 3 Suggestions:** Caps output at top 3 actionable cards.
- [ ] **Settings Integration:** Toggle `FEATURE_SMART_LIMIT_SUGGESTIONS` in `/settings/ai` properly halts worker generation when disabled.
- [ ] **Theme Compliance:** Built with Holst Dark Palette tokens (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`).
- [ ] **Zero-Mock Architecture:** Backed by real Room DAOs, DataStore Preferences, and Android `UsageStatsManager`.

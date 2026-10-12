# 📦 Topic FF-P4-3: Category & Group Limits Specification

**Document ID:** `FF_P4_CATEGORY_LIMITS_SPEC`  
**Phase:** Phase 4 (Cross-Platform Daemon, Group Limits & Habit Breaking)  
**Target Package:** `com.flux.focus.features.limits.category`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + UsageStats Aggregation Engine)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Individual app limits (e.g. 30m Instagram, 30m Reddit, 30m Twitter) suffer from **behavioral substitution**: when the user’s Instagram limit is exhausted, their habit loop simply shifts to Reddit, and then to Twitter, enabling up to 90 minutes of aggregate doomscrolling.

**Category & Group Limits** resolve this by pooling time across groups of apps. A single shared budget covers the entire category; when the shared pool is exhausted, the intervention overlay immediately engages across all member apps.

### 1.1 Strict Mutual Exclusivity Rule
* **One Rule per Package:** An application is tracked **either under an Individual Limit OR as a member of a Category Limit—never both**.
* This eliminates conflicting overlays, double-counting bugs, and ambiguous extra-time pass allocation.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **App Limits Baseline** | [`FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_APP_LIMITS_AND_INTERVENTION_SPEC.md) | Category limits trigger the identical 5/10/20/40m intervention overlay when the shared pool is breached. |
| **App Categorization** | [`FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%201/FF_P1_USAGE_STATS_AND_WIDGET_SPEC.md) | Maps packages to categories (`DISTRACTING`, `SEMI_PRODUCTIVE`, `PRODUCTIVE`, `OTHERS`). |
| **Focus Score Engine** | [`FF_P3_FOCUS_SCORE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/03_FLUXFOXUS_FF/Phase%203/FF_P3_FOCUS_SCORE_SPEC.md) | Category pool compliance feeds into the 20-mark App Limit Compliance component. |

---

## 3. Feature Specifications & Behavioral Rules

### 3.1 Category Limit Configuration
* **User-Initiated Creation:** Category pools are created strictly on-demand by the user (no forced defaults).
* **Configurable Attributes:**
  * Target Category: `DISTRACTING`, `SEMI_PRODUCTIVE`, `ENTERTAINMENT`, or `SOCIAL`.
  * Shared Daily Budget: Time picker (`15m` to `8 hours`, step `15m`).
  * Shared Extra Time Passes: Max 3 tokens per day (`5m | 10m | 15m`).

---

### 3.2 Mutual Exclusivity Migration Dialog

When a user enables or creates a Category Pool that includes apps currently holding individual limits:

```
+-------------------------------------------------------+
|  ━  Convert to Shared Category Pool?                  |
+-------------------------------------------------------+
|  The following apps currently have individual limits: |
|                                                       |
|  • Instagram (30m budget)                             |
|  • Reddit (30m budget)                                |
|                                                       |
|  Adding them to the shared 45m Distracting Pool will  |
|  remove their individual limits to avoid double       |
|  enforcement.                                         |
|                                                       |
|  +-------------------------------------------------+  |
|  |     [ Move to Shared Pool (Recommended) ]       |  |
|  +-------------------------------------------------+  |
|  |     [ Keep Individual Limits & Cancel ]         |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

* **Action `[ Move to Shared Pool ]`:**
  * Removes the individual limits from `app_limits`.
  * Assigns apps to `limit_type = 'CATEGORY_MEMBER'`.
  * Activates the shared category budget.
* **Action `[ Keep Individual Limits ]`:**
  * Cancels the migration; individual limits remain completely untouched.

---

### 3.3 Real-Time Aggregated Enforcement

When the user launches an app whose `limit_type == 'CATEGORY_MEMBER'`:
1. `FocusAccessibilityService` checks the app's category.
2. It queries `UsageStatsManager` for the sum of usage across all member apps in that category today:
   $$\text{Total Category Usage} = \sum_{p \in \text{CategoryApps}} \text{actualUsageMinutes}(p)$$
3. **Threshold Evaluation:**
   * If $\text{Total Category Usage} \ge \text{categoryDailyBudgetMinutes}$:
   * The **5/10/20/40m Intervention Overlay** immediately cuts in.
4. **Shared Extra Time Token Rule:**
   * If an extra time token (e.g. 10m) is redeemed, it unlocks the **entire category** for that 10-minute window, allowing the user to check whichever member app they need before re-enforcing the block.

---

## 4. UI Restructuring on `Tab.Limits`

The Limits screen is cleanly bifurcated into two sections:

```
+-------------------------------------------------------+
|  LIMITS & BUDGETS                               [ + ] |
+-------------------------------------------------------+
|                                                       |
|  SHARED CATEGORY POOLS                                |
|  +-------------------------------------------------+  |
|  | 🚫 Distracting Apps Pool         38m / 45m used |  |
|  | [████████████████████░░░]        7 mins left    |  |
|  | 4 Apps: Instagram, Reddit, X, TikTok             |  |
|  +-------------------------------------------------+  |
|                                                       |
|  INDIVIDUAL APP LIMITS                                |
|  +-------------------------------------------------+  |
|  | 📺 YouTube (Study Mode)          12m / 30m used |  |
|  | [████████░░░░░░░░░░░░░░░]        18 mins left   |  |
|  +-------------------------------------------------+  |
|  | 🎮 Chess.com                     20m / 20m used |  |
|  | [███████████████████████]        Exhausted      |  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

---

## 5. Database Schema & Room Entities

### 5.1 New Table: `category_limits`

```kotlin
package com.flux.focus.features.limits.category.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_limits")
data class CategoryLimitEntity(
    @PrimaryKey
    @ColumnInfo(name = "category")
    val category: String, // "DISTRACTING", "SEMI_PRODUCTIVE", "ENTERTAINMENT", "SOCIAL"

    @ColumnInfo(name = "daily_budget_minutes")
    val dailyBudgetMinutes: Int,

    @ColumnInfo(name = "extra_time_passes_used")
    val extraTimePassesUsed: Int = 0, // Max 3 per day

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

### 5.2 Schema Modification to `app_limits`

```kotlin
// Added to AppLimitEntity:
@ColumnInfo(name = "limit_type")
val limitType: String = "INDIVIDUAL" // "INDIVIDUAL" or "CATEGORY_MEMBER"
```

### 5.3 Category Limit DAO (`CategoryLimitDao.kt`)

```kotlin
package com.flux.focus.features.limits.category.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flux.focus.features.limits.category.data.entity.CategoryLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryLimitDao {

    @Query("SELECT * FROM category_limits WHERE is_enabled = 1")
    fun observeActiveCategoryLimits(): Flow<List<CategoryLimitEntity>>

    @Query("SELECT * FROM category_limits WHERE category = :category LIMIT 1")
    suspend fun getCategoryLimit(category: String): CategoryLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(categoryLimit: CategoryLimitEntity)

    @Query("UPDATE category_limits SET extra_time_passes_used = extra_time_passes_used + 1 WHERE category = :category")
    suspend fun incrementExtraTimePass(category: String)

    @Query("UPDATE category_limits SET extra_time_passes_used = 0")
    suspend fun resetDailyPasses()
}
```

---

## 6. Jetpack Compose UI Specifications (`CategoryPoolCard.kt`)

```kotlin
@Composable
fun CategoryPoolCard(
    categoryTitle: String,
    categoryIcon: String,
    usedMinutes: Int,
    budgetMinutes: Int,
    memberAppsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingMinutes = maxOf(0, budgetMinutes - usedMinutes)
    val progress = (usedMinutes.toFloat() / budgetMinutes.toFloat()).coerceIn(0f, 1f)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
        border = BorderStroke(1.dp, Color(0xFF415A77)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = categoryIcon, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$categoryTitle Pool",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF4F1DE),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${usedMinutes}m / ${budgetMinutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD4C4A8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) Color(0xFFE07A5F) else Color(0xFF778D7A),
                trackColor = Color(0xFF0D1B2A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$memberAppsCount Member Apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0AAB2)
                )
                Text(
                    text = if (remainingMinutes > 0) "$remainingMinutes mins left" else "Pool Exhausted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remainingMinutes > 0) Color(0xFF778D7A) else Color(0xFFE07A5F),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

---

## 7. Verification & Branch-Testing Isolation Workflow

### 7.1 Zero-Mock Automated & Manual Test Cases
1. **Migration Handover Test:**
   * Create individual limits for Instagram (30m) and Reddit (30m).
   * Create a 45m Distracting Category Pool.
   * Confirm migration dialog: `[ Move to Shared Pool ]`.
   * **Verify:** Individual limits removed from `app_limits`; Instagram and Reddit have `limit_type = 'CATEGORY_MEMBER'`.
2. **Aggregated Depletion Test:**
   * Category budget = 45m.
   * Simulate UsageStats: Instagram = 25m, Reddit = 20m (Total = 45m).
   * Launch Twitter (0m used individually).
   * **Verify:** 5/10/20/40m intervention overlay triggers immediately on Twitter because the shared 45m pool is exhausted.
3. **Shared Token Redemption Test:**
   * Pool exhausted -> Redeem 10m extra time token on Instagram.
   * Launch Reddit within the 10-minute window.
   * **Verify:** Reddit opens smoothly without intervention until the 10m pass expires.

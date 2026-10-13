# FluxFoxus (FF) — Phase 1: Stats Screen, App Detail Page & Home Screen Widget Specification

**Version:** 2.0 (Native Android Kotlin + Jetpack Compose)  
**Phase:** Phase 1 Core  
**Scope:** Stats Screen (`Tab.Stats`), Daily vs Weekly Trends, Focus vs Screen Time Ratio, App Usage List, Dedicated `AppDetailScreen`, Category Overrides, and Jetpack Glance Donut Widget  
**Theme:** Strictly **Holst (Холст)** (`#0D1B2A`, `#1B263B`, `#415A77`, `#778D7A`, `#D4C4A8`, `#F4F1DE`, `#E07A5F`)

---

## 1. Architectural Overview & Design Philosophy

For an academic Olympiad competitor, screen time is not inherently evil—**unconscious, fragmented, and passive consumption is**. The device is Ari's primary textbook and problem-solving console, but also a dangerous vector for distraction.

FluxFoxus solves this by:
1. **Separating Deep Work from Casual Screen Time:** Tracking the **Focus vs Screen Time Ratio**.
2. **Two-Tiered Analytics (`Tab.Stats`):** Offering seamless switching between **`Daily`** tactical tracking and **`Weekly Trends`** strategic discipline reviews.
3. **App Usage Lists with Direct Deep Dives:** Every app row in `Tab.Stats` and `Tab.Limits` is interactive, navigating directly to a dedicated **App Detail Page (`AppDetailScreen`)**.
4. **User-Controlled App Categorization:** Giving Ari total authority to classify apps into 4 categories (`Productive`, `Semi-Productive`, `Distracting`, `Others`) with instant, zero-latency reactive recalculation across the entire app.
5. **The Flagship Home Screen Widget:** A 4-segment Donut Ring built with Jetpack Glance providing passive ambient accountability without opening the app.

---

## 2. Core Metrics: Focus vs Screen Time Ratio

### 2.1 The Mathematical Model
$$\text{Focus Time} = \sum (\text{Actual elapsed duration of completed or active FF focus sessions today})$$
$$\text{Total Screen Time} = \sum (\text{Foreground app duration from Android } \texttt{UsageStatsManager} \text{ today})$$
$$\text{Casual Screen Time} = \max(0, \text{Total Screen Time} - \text{Focus Time})$$

$$\text{Focus Ratio} = \frac{\text{Focus Time}}{\max(1\text{ min}, \text{Casual Screen Time})}$$

- **Format:** Displayed as a primary multiplier ratio: e.g. **`2.1x`** (or **`2.1 : 1`**).
- **Secondary Sub-label:** *"3h 15m Deep Focus vs 1h 32m Casual Screen"*.
- **Edge Case (Zero Casual Time):** If Casual Screen Time is $0$, the ratio caps at **`10.0x+`** with the badge **"Pure Flow ⚡"**.

### 2.2 Discipline Benchmark Tiers
| Focus Ratio | Discipline Tier | UI Color & Badge | Meaning |
| :--- | :--- | :--- | :--- |
| **$< 0.5x$** | **Attention Leak** | Terracotta Coral (`#E07A5F`) | Casual usage dominates; focus sessions neglected. |
| **$0.5x - 1.0x$** | **Developing Rhythm** | Warm Sand (`#D4C4A8`) | Balanced, but non-focus screen time exceeds deep focus. |
| **$1.0x - 2.0x$** | **Deep Work** | Muted Sage Green (`#778D7A`)| Focus time surpasses all casual phone usage combined. |
| **$> 2.0x$** | **Monk Mode** | Eggshell Cream + Sage Glow (`#F4F1DE`) | Device is primarily functioning as an academic workstation. |

---

## 3. Stats Screen (`Tab.Stats`) Architecture

Accessible via the 5th bottom navigation tab.

### 3.1 Top Segmented Navigation
At the top of `Tab.Stats`, a custom Holst Segmented Pill Switcher allows instant toggling:
```
+-------------------------------------------------------+
|  STATS                                         [🏷️ All]|
|             +-----------------------+                 |
|             |   Daily  |  Weekly    |                 |
|             +-----------------------+                 |
+-------------------------------------------------------+
```
- **Surface:** Container `#1B263B` with border `#415A77`, pill height 36dp.
- **Active Pill:** Warm Sand `#D4C4A8` background with dark navy text `#0D1B2A`, bold 13sp.
- **Inactive Pill:** Slate Blue-Grey `#8E9CAE` text.

---

### 3.2 View A: The `Daily` Tab

```
+-------------------------------------------------------+
|  [⚡ HERO FOCUS RATIO CARD]                            |
|  Focus / Screen Ratio: 2.1x           [Monk Mode ⚡]  |
|  Deep Focus: 3h 15m   |   Casual Screen: 1h 32m       |
|  [===========================Sage========|==Coral==]  |
+-------------------------------------------------------+
|  [7-DAY STACKED BAR CHART - TODAY HIGHLIGHTED]        |
|  8h|                                                  |
|  6h|            [█]                                   |
|  4h|--[█]---[█]-[█]-[█]-[█]---------------- 4h Target |
|  2h|  [█]   [█] [█] [█] [█]   [█]   [█]               |
|  0h+-----------------------------------               |
|       M     T   W   T   F     S     S* (Today)        |
+-------------------------------------------------------+
|  [CATEGORY BREAKDOWN CHIPS]                           |
|  [🟢 Productive 2h 45m]      [🟡 Semi-Prod 1h 12m]    |
|  [🔴 Distracting 0h 50m]     [⚪ Others 0h 00m]       |
+-------------------------------------------------------+
|  TODAY'S APPS (14)                     [Sort: Time ▾] |
|  +-------------------------------------------------+  |
|  | [📘] Notewise                 1h 45m (36%)   🟢  |  |
|  +-------------------------------------------------+  |
|  | [⏳] FluxFoxus                1h 05m (22%)   🟢  |  |
|  +-------------------------------------------------+  |
|  | [🧮] Calculator               0h 25m  (9%)   🟡  |  |
|  +-------------------------------------------------+  |
|  | [▶️] YouTube                  0h 22m  (8%)   🔴  |  |
|  +-------------------------------------------------+  |
|  | [🌐] Chrome                   0h 15m  (5%)   🔴  |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

1. **Hero Focus Ratio Card:**
   - Background `#1B263B`, border 1dp `#415A77`, padding 16dp.
   - Ratio multiplier text: `2.1x` in Eggshell Cream `#F4F1DE` (28sp bold).
   - Horizontal dual-split progress bar: Sage Green `#778D7A` (focus share) vs Terracotta Coral `#E07A5F` (casual share).
2. **7-Day Stacked Bar Chart (Contextual):**
   - Renders the rolling 7 days.
   - Each bar is composed of stacked segments: Productive (Sage), Semi-Productive (Sand), Distracting (Coral), Others (Steel Blue).
   - **Today's Bar:** Visually framed with a 1.5dp Warm Sand `#D4C4A8` outline or subtle glow.
   - Target Line: Dashed horizontal line indicating daily focus target (e.g. 4 hours).
3. **Category Summary Filter Chips:**
   - 4 horizontal pills.
   - Tapping any chip filters the app list below to only show apps in that category. Tapping again clears filter.
4. **Today's App Usage List:**
   - Ranked descending by foreground screen time today.
   - Each app row displays: App Icon (36×36dp), App Title (`#F4F1DE`), Usage Duration (e.g. `1h 45m`), Percentage of today's screen time, and Category Indicator Dot (🟢, 🟡, 🔴, ⚪).
   - **Interactive Navigation:** Tapping any row opens the **`AppDetailScreen`** for that app in `Daily` mode.

---

### 3.3 View B: The `Weekly Trends` Tab

```
+-------------------------------------------------------+
|  [WEEK-OVER-WEEK REPORT HERO CARD]                    |
|  Weekly Focus: 18h 40m        (+3h 15m / +21% ▲)      |
|  Distracting:   3h 20m        (-1h 45m / -34% ▼)      |
|  Focus Ratio:   1.2x ➔ 1.9x   (+0.7x Discipline Gain) |
|  Discipline Grade: [ A - Monk Flow ]                  |
+-------------------------------------------------------+
|  [WEEKLY STACKED USAGE CHART]   < Oct 05 - Oct 11 >   |
|  8h|                                                  |
|  6h|            [█]                                   |
|  4h|--[█]---[█]-[█]-[█]-[█]---------------- Avg: 4h02m|
|  2h|  [█]   [█] [█] [█] [█]   [█]   [█]               |
|  0h+-----------------------------------               |
|       M     T   W   T   F     S     S                 |
+-------------------------------------------------------+
|  WEEKLY CATEGORY DISTRIBUTION                         |
|  🟢 Productive:     18h 40m (66%)                     |
|  🟡 Semi-Prod:       6h 14m (22%)                     |
|  🔴 Distracting:     3h 20m (12%)                     |
+-------------------------------------------------------+
|  TOP APPS THIS WEEK                    [Sort: Time ▾] |
|  +-------------------------------------------------+  |
|  | [📘] Notewise                 12h 10m (Avg 1h44m) |  |
|  +-------------------------------------------------+  |
|  | [⏳] FluxFoxus                 6h 30m (Avg 0h55m) |  |
|  +-------------------------------------------------+  |
|  | [▶️] YouTube                   2h 45m (Avg 0h23m) |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

1. **Week-over-Week Report Hero Card:**
   - Compares the active week with the previous 7-day period.
   - Shows green Sage indicators for positive trends (increase in focus time, decrease in distracting time, increase in focus ratio).
   - Generates an academic discipline grade (`A+`, `A`, `B`, etc.) summarizing consistency.
2. **Weekly Stacked Usage Chart:**
   - Displays all 7 days with full category stacking.
   - Average Daily Screen Time line displayed as a horizontal dashed line.
   - Week navigation arrows (`< Prev Week` / `Next Week >`).
3. **Weekly App Usage List:**
   - Aggregated total time spent across the week.
   - Displays total weekly time and average daily minutes.
   - **Interactive Navigation:** Tapping any app row opens the **`AppDetailScreen`** in `Weekly` mode.

---

## 4. The Dedicated App Detail Page (`AppDetailScreen`)

The App Detail Page is a unified destination opened whenever an app is tapped from `Tab.Stats` (Daily or Weekly) or `Tab.Limits`. It combines usage analytics, category assignment, and block configurations.

```
+-------------------------------------------------------+
|  [←]  [📘 Notewise]                            [Help] |
+-------------------------------------------------------+
|                       [Daily ▾]                       |
|                                                       |
|  [<]                   6h 34m                     [>] |
|                      Sun, 11 Oct                      |
|                                                       |
|   8h |                                                |
|   4h |         [█]             [█]                    |
|   0h +--[ ]----[█]--[ ]--[ ]---[█]---[█]--------------|
|          M      T    W    T     F     S     S (Sage)  |
|                                                       |
|  Usage Category                                       |
|  +-------------------------------------------------+  |
|  |  🏷️ Category                    🟢 Productive ▾  |  |
|  +-------------------------------------------------+  |
|                                                       |
|  Manage Blocks                                        |
|  +-------------------------------------------------+  |
|  |  [📘] Notewise limit                               |  |
|  |       Add block / 45m limit                        |  |
|  +-------------------------------------------------+  |
|                                                       |
|  [🔒 We care about your privacy. All data stays local] |
+-------------------------------------------------------+
```

### 4.1 Header & Mode Selector
- **App Bar:** Back button `←`, App Icon (32×32dp), App Label (Eggshell Cream `#F4F1DE`), and contextual Help button.
- **Period Dropdown / Pill:**
  - `Daily ▾`: Inspects a single day.
  - `Weekly ▾`: Inspects the 7-day aggregate period.

---

### 4.2 Hero Usage Metric & 7-Day App Bar Chart

#### Mode 1: `Daily ▾` Mode
- **Date Navigation:** `[<] Sun, 11 Oct [>]` with date picker support.
- **Hero Time:** Prominent usage duration for that specific day: e.g. **`6h 34m`** (Eggshell Cream `#F4F1DE`, 32sp bold).
- **7-Day Bar Chart:**
  - X-Axis: `M`, `T`, `W`, `T`, `F`, `S`, `S`.
  - **Selected Day Bar:** Highlighted in the app's current category color (Sage Green `#778D7A` for Productive, Terracotta Coral `#E07A5F` for Distracting, Sand `#D4C4A8` for Semi-Productive).
  - **Other 6 Days:** Rendered in Muted Steel Blue `#415A77` to provide context while keeping visual focus on the selected day.

#### Mode 2: `Weekly ▾` Mode
- **Date Navigation:** `[<] 05 Oct – 11 Oct [>]`.
- **Hero Time:** Average daily usage for the week: e.g. **`2h 57m`** (with subtext *"Total: 20h 39m"*).
- **Delta Pill:**
  - If usage decreased: `[- 12% vs last week]` in Sage Green `#778D7A` (for distracting apps) or neutral.
  - If usage increased: `[+ 70% vs last week]` in Terracotta Coral `#E07A5F` (for distracting apps).
- **7-Day Bar Chart:** All active days for the week are rendered in the app's category color.

---

### 4.3 Usage Category Assignment Card

- Card container: Oxford Slate `#1B263B`, 1dp border `#415A77`, corner radius 12dp.
- Left side: Tag icon `Icons.Default.Label` + *"Category"* text (`#F4F1DE`, 15sp).
- Right side: Dropdown selector pill:
  - `🟢 Productive ▾`
  - `🟡 Semi-Productive ▾`
  - `🔴 Distracting ▾`
  - `⚪ Others ▾`
- **Category Selection Modal / Bottom Sheet:**
  Tapping opens a compact bottom sheet displaying all 4 categories with descriptions:
  1. 🟢 **Productive:** Academic tools, study readers, Notewise, FD.
  2. 🟡 **Semi-Productive:** System utilities, calculator, calendar, communication.
  3. 🔴 **Distracting:** Social media, entertainment video, algorithmic feeds, games.
  4. ⚪ **Others:** System background, launchers, unclassified apps.
- **Instant Reactive Recalculation:**
  - Selecting a category immediately executes a Room `INSERT OR REPLACE` into `app_category_overrides`.
  - The bar chart on `AppDetailScreen` immediately shifts to the new category color.
  - Room's reactive Kotlin `Flow` emits the updated state, causing `Tab.Stats`, `Tab.Limits`, and the Home Screen Widget to re-bucket all statistics in real time without screen refresh or reload flicker.

---

### 4.4 Manage Blocks & Granular Restrictions Section

- Direct bridge between Stats and Limits:
  1. **Daily App Limit Card:**
     - Displays current limit status: e.g. `[App Icon] Notewise limit: Add block` or `45m daily limit (Active 🟢)`.
     - Tapping opens the time budget editor (from `Tab.Limits`) or toggles the block.
  2. **Granular Feature Block Cards (Where applicable):**
     - For Instagram: `[Reels Icon] IG Reels: + Add block` (Accessibility service detects and blocks the Reels tab/feed).
     - For YouTube: `[Shorts Icon] YT Shorts: + Add block` / `YouTube Study Mode`.
- **Privacy Lock Footer:**
  - Subtle lock icon `Icons.Default.Lock` + text: *"We care about your privacy. All usage statistics remain strictly on your device."*

---

## 5. Home Screen Widget: The 4-Category Donut Ring (Jetpack Glance)

The Donut Ring is specifically engineered for the Android Home Screen Widget using **Jetpack Glance (`androidx.glance`)**.

```
+-------------------------------------------------------+
|  FLUXFOXUS                           [ ⚡ Focus: 2.1x ]|
|                    . - ~ - .                          |
|                  /  🟢 Sage  \                        |
|                 :  [ 4h 12m ] :    [ ▶ Start Focus ]  |
|                 :  [ Total  ] :    Last: Math Olympiad|
|                  \ 🔴 Coral 🟡/                       |
|                    . - ~ - .                          |
+-------------------------------------------------------+
```

### 5.1 Widget Variants
1. **Compact Donut (2×2 Widget):**
   - Centered 4-segment Donut Ring.
   - Holst color segments: Productive (`#778D7A`), Semi-Productive (`#D4C4A8`), Distracting (`#E07A5F`), Others (`#415A77`).
   - Donut Center: Total Screen Time (`4h 12m`) in bold Eggshell Cream `#F4F1DE`.
   - Top Header: Current Focus Ratio (`2.1x ⚡`).
2. **Dashboard & Quick Launcher (4×2 Widget):**
   - Left half: 4-segment Donut Ring with center time and ratio badge.
   - Right half: Quick Session Launcher button (`#D4C4A8` Sand CTA):
     - Displays last-used preset name (e.g. *"▶ Start Olympiad Math"*).
     - Tapping launches the foreground focus session directly from the home screen.

### 5.2 Widget Update Cycle
- Uses Android `WorkManager` with a periodic 15-minute background worker.
- Immediately triggered to update whenever:
  - A focus session completes or starts.
  - An app category override is changed.
  - Device screen turns on (via `ACTION_SCREEN_ON` broadcast receiver).

---

## 6. Technical Implementation & Data Schema

### 6.1 Database Schema (Room SQLite)

```sql
-- Table: app_category_overrides
CREATE TABLE app_category_overrides (
    package_name TEXT PRIMARY KEY NOT NULL,
    app_label TEXT NOT NULL,
    category TEXT NOT NULL, -- 'PRODUCTIVE', 'SEMI_PRODUCTIVE', 'DISTRACTING', 'OTHERS'
    updated_at INTEGER NOT NULL
);
```

### 6.2 Default Category Fallback Strategy
If no user override exists in `app_category_overrides`:
1. Query Android `ApplicationInfo.category` from `PackageManager`:
   - `CATEGORY_PRODUCTIVITY`, `CATEGORY_EDUCATION` $\rightarrow$ `PRODUCTIVE`
   - `CATEGORY_MAPS`, `CATEGORY_IMAGE`, `CATEGORY_AUDIO` $\rightarrow$ `SEMI_PRODUCTIVE`
   - `CATEGORY_GAME`, `CATEGORY_SOCIAL`, `CATEGORY_NEWS` $\rightarrow$ `DISTRACTING`
   - `CATEGORY_UNDEFINED` $\rightarrow$ `OTHERS`
2. Once the user modifies an app's category on `AppDetailScreen`, the override persists permanently in Room DB.

### 6.3 State Management & Reactive Recalculation Flow
```kotlin
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val categoryRepository: AppCategoryRepository
) : ViewModel() {

    // Combines Android UsageStats with Room focus sessions and category overrides
    val statsUiState: StateFlow<StatsUiState> = combine(
        usageStatsRepository.getRawUsageStatsFlow(),
        focusSessionRepository.getCompletedSessionsTodayFlow(),
        categoryRepository.getAllCategoryOverridesFlow()
    ) { rawUsage, focusSessions, overrides ->
        aggregateStats(rawUsage, focusSessions, overrides)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState.Loading
    )
}
```
*Zero polling loops. Any change to `app_category_overrides` immediately emits down the flow, recalculating both `Tab.Stats` and `AppDetailScreen` instantly.*

---

## 7. Strict Holst UI Color Token Lock

| UI Element | Token / Color | Hex Value |
| :--- | :--- | :--- |
| **Screen Background** | `color_background` | `#0D1B2A` |
| **Card Surfaces** | `color_surface` | `#1B263B` |
| **Borders & Dividers** | `color_border` | `#415A77` |
| **Productive Category / Chart Accent** | `color_accent` (Sage) | `#778D7A` |
| **Semi-Productive Category / Action CTA** | `color_primary` (Sand) | `#D4C4A8` |
| **Distracting Category / Warning Deltas** | `color_warning` (Terracotta Coral) | `#E07A5F` |
| **Others Category / Inactive Chart Bars**| `color_muted_bar` (Steel Blue) | `#415A77` |
| **Primary High-Contrast Text** | `color_text_primary` | `#F4F1DE` |
| **Muted Labels & Timestamps** | `color_text_muted` | `#8E9CAE` |

---

## 8. Cross-Reference & Navigation Rules

1. **`Tab.Stats` App List Click:** Navigates to `AppDetailScreen(packageName, initialMode = Daily or Weekly)`.
2. **`Tab.Limits` App List Click:** Navigates to `AppDetailScreen(packageName, initialMode = Daily)`.
3. **App Category Change:** Writes to Room DB and updates `Tab.Stats`, `Tab.Limits`, and Glance widget via reactive `Flow`.
4. **App Limit Change:** Tapping "Add block / Edit limit" inside `AppDetailScreen` updates Room DB `app_limits` table and reflects back in `Tab.Limits`.

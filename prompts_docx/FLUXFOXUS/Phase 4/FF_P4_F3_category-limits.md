# FF Phase 4 — Feature 3: Category Limits

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

Category Limits allow the user to set a shared daily time budget across all apps in a category. When the category budget is exhausted, the 2/5/10/20 intervention window appears for any app in that category on next open — regardless of individual app limit status.

Per-app limits and category limits are **mutually exclusive**. An app is either tracked individually or under its category — never both.

---

## 2. P1 References

| Reference | Location |
|---|---|
| App categories (4 types) | FF_PRD_v1_0.md §3.6.6, §3.9.2 |
| App Limits — enforcement | FF_PRD_v1_0.md §3.6 |
| 2/5/10/20 Intervention Window | FF_PRD_v1_0.md §3.7.7 |
| AppLimit data model | FF_TRD_v1_0.md §3.3 |
| app_limits table | FF_TRD_v1_0.md §4.4 |
| Screen Time Tracking — UsageStats | FF_PRD_v1_0.md §3.9 |

---

## 3. Mutual Exclusivity Rule

### 3.1 Core Rule
An app cannot simultaneously have an individual limit AND be tracked under a category limit. One or the other.

### 3.2 Switching an App from Individual → Category

When user enables a category limit for a category that contains apps with individual limits:

- Bottom sheet appears: *"[N] apps in [Category] have individual limits: [app names]. Moving them to the category limit will remove their individual limits."*
- **"Move to Category"** button: removes individual limits, apps fall under category tracking
- **"Keep Individual"** button: those specific apps retain individual limits and are excluded from category tracking. Only apps without individual limits join the category

### 3.3 Switching an App from Category → Individual

When user creates an individual limit for an app that's under a category limit:

- Inline warning in App Limits creation flow: *"[App name] is covered by the [Category] category limit. Adding an individual limit will remove it from the category."*
- User confirms → app removed from category tracking, individual limit created
- Category limit continues for remaining apps in category

### 3.4 Storage Flag

```sql
-- Modified app_limits table
ALTER TABLE app_limits ADD COLUMN limit_type TEXT NOT NULL DEFAULT 'individual';
-- Values: 'individual' | 'category'
```

Apps with `limit_type = 'category'` are excluded from individual enforcement and tracked against their category budget only.

---

## 4. Feature Specification

### 4.1 Category Limit Configuration

**Location:** Settings → App Limits → Category Limits section (new section above the per-app list)

One row per category:

| Element | Detail |
|---|---|
| Category name | e.g. "Distracting" |
| Category color dot | Matches existing category color coding |
| App count | "4 apps" — number of apps currently tracked under this category |
| Time limit | Current limit or "No limit" |
| Toggle | ON/OFF per category |

Tapping a category row → Category Limit detail screen:
- Time picker: daily budget (15 min – 12 hours)
- App list: all apps in category, each with a toggle to include/exclude from category tracking
- Excluded apps shown with "Individual limit" badge if they have one, or "No limit" if they don't

### 4.2 Enforcement

**Tracking:** FF sums UsageStats data for all apps in the category with `limit_type = 'category'` every time a distracting app is opened (same real-time check as individual app limits).

**When category budget is hit:**
- The 2/5/10/20 intervention window appears for the app the user just opened
- This applies to ALL apps in the category with `limit_type = 'category'` — not just the one that tipped over the budget
- Extra time sessions (per-category, not per-app) — see §4.3

**When individual app limit is hit (app has `limit_type = 'individual'`):**
- Normal P1 App Limits enforcement (PRD §3.6.4)
- Category limit is not involved

### 4.3 Category Extra Time Sessions

Category limits have their own extra time session configuration (separate from per-app extra time):
- Count: 0–6 sessions
- Duration per session: 5 / 10 / 15 min chips
- Hard cap: total extra time ≤ 60 min (same rule as per-app — PRD §3.6.3)
- Extra time applies to the entire category — all apps in the category share the extra time window

### 4.4 Daily Reset

Category usage counters reset at midnight — same WorkManager task as individual app limit resets.

---

## 5. Display in App Limits Screen

The main App Limits screen is restructured in P4:

**Before P4 (P1 structure):**
- Flat list of apps with individual limits

**P4 structure:**
- **Category Limits** section at top (collapsible)
  - One row per category that has a limit configured
  - Shows category name + budget + usage today (e.g. "Distracting — 45m / 60m")
- **Individual Limits** section below
  - Same as P1 — apps with individual limits

Apps under category limits do NOT appear in the Individual Limits section. Clean separation.

---

## 6. Data Model

### 6.1 New Model: CategoryLimit

```dart
@freezed
class CategoryLimit with _$CategoryLimit {
  const factory CategoryLimit({
    required AppCategory category,
    required Duration dailyLimit,
    required int extraSessionCount,        // 0–6
    required Duration extraSessionDuration, // 5/10/15 min
    required bool isActive,
    required DateTime createdAt,
  }) = _CategoryLimit;
}
```

### 6.2 Daily Category Usage (computed, not stored)

Category usage is computed at enforcement time from `AppUsageRecord` data (already tracked in P1):

```dart
Duration getCategoryUsageToday(AppCategory category) {
  final today = DateTime.now().startOfDay;
  return appUsageRepository
    .getRecordsForDate(today)
    .where((r) => r.category == category && r.limitType == 'category')
    .fold(Duration.zero, (sum, r) => sum + r.usageDuration);
}
```

---

## 7. Database Schema

### 7.1 New Table: category_limits

```sql
CREATE TABLE category_limits (
  category TEXT PRIMARY KEY,              -- 'productive' | 'semi_productive' | 'distracting' | 'others'
  daily_limit_seconds INTEGER NOT NULL,
  extra_session_count INTEGER NOT NULL DEFAULT 0,
  extra_session_duration_seconds INTEGER NOT NULL DEFAULT 300,
  is_active INTEGER NOT NULL DEFAULT 1,
  created_at INTEGER NOT NULL
);
```

### 7.2 Modified Table: app_limits

```sql
ALTER TABLE app_limits ADD COLUMN limit_type TEXT NOT NULL DEFAULT 'individual';
```

---

## 8. Routing

```dart
GoRoute(
  path: '/blocks/category/:category',
  builder: (_, state) => CategoryLimitDetailScreen(
    category: state.pathParameters['category']!,
  ),
),
```

---

## 9. Module Boundary

**Owned by:** `app_limits/` module (extended from P1)

```
features/
└── app_limits/
    ├── data/
    │   ├── category_limit_dao.dart           ← NEW
    │   └── app_limit_repository_impl.dart    ← EXTENDED (limit_type, mutual exclusivity)
    ├── domain/
    │   ├── category_limit.dart               ← NEW model
    │   └── use_cases/
    │       ├── create_category_limit.dart    ← NEW
    │       ├── get_category_usage.dart       ← NEW
    │       └── enforce_category_limit.dart   ← NEW
    └── presentation/
        ├── app_limits_screen.dart            ← MODIFIED (Category Limits section)
        ├── category_limit_detail_screen.dart ← NEW
        └── mutual_exclusivity_sheet.dart     ← NEW
```

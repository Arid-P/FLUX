# FF Phase 4 — Feature 4: App Limit Smart Suggestions

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

App Limit Smart Suggestions analyses the user's actual daily app usage patterns over a rolling 30-day window and suggests adjustments to their app limits (tighten or loosen) when consistent patterns are detected. Suggestions are surfaced exclusively in the Sunday weekly report — no mid-week prompts, no separate notifications.

No AI involved. Pure statistical analysis on local UsageStats data.

---

## 2. P1 References

| Reference | Location |
|---|---|
| App Limits — configuration | FF_PRD_v1_0.md §3.6 |
| AppUsageRecord data model | FF_TRD_v1_0.md §3.6 |
| Weekly Report | FF_PRD_v1_0.md §3.11 |
| Extra time sessions | FF_PRD_v1_0.md §3.6.3 |
| Category Limits | FF_P4_F3_category-limits.md |

---

## 3. Suggestion Logic

### 3.1 Tighten Suggestion

**Trigger:** App's average daily usage ≤ 60% of its current limit for 14+ of the last 30 days.

**Suggested new limit:** Rounded up to nearest 5-minute interval above the 30-day average.

**Example:** Limit = 30 min. Average usage = 18 min/day for 16 of last 30 days. Suggested new limit = 20 min.

### 3.2 Loosen Suggestion

**Trigger:** App's daily limit was hit (or extra time sessions were used) on 10+ of the last 14 days.

**Suggested new limit:** Current limit + 15 minutes, rounded to nearest 5-minute interval.

**Example:** Limit = 30 min, hit on 11 of last 14 days. Suggested new limit = 45 min.

### 3.3 Priority

- Maximum **3 suggestions** per weekly report — ranked by strongest signal (most days triggering the condition)
- One suggestion per app maximum per report cycle
- Tighten suggestions take priority over loosen (discipline-first philosophy)
- Same app cannot appear in consecutive weekly reports — 2-week cooldown per app after a suggestion is shown (whether acted on or dismissed)

### 3.4 Category Limits

Same logic applies to category limits — if category usage is consistently below 60% of the category budget or consistently hitting the budget, a category-level suggestion is generated. Shown as a single suggestion for the category, not per-app within it.

---

## 4. Weekly Report Integration

The Sunday weekly report (FF_PRD_v1_0.md §3.11) gains a new **"Limit Suggestions"** section at the bottom of the report, after existing content.

### 4.1 Suggestion Card

Each suggestion is a card within the report:

```
─────────────────────────────────
  [App icon]  Instagram
  
  💡 Consider tightening your limit
  
  Current limit: 30 min/day
  Your average: 18 min/day (last 30 days)
  Suggested: 20 min/day
  
  [Dismiss]        [Apply: 20 min]
─────────────────────────────────
```

- **"Apply [X] min"** button: updates the limit immediately. Snackbar: *"Instagram limit updated to 20 min."*
- **"Dismiss"** button: dismisses this suggestion. 2-week cooldown starts
- Cards are stacked vertically if multiple suggestions exist (max 3)
- If no suggestions: section is hidden entirely — no empty state shown

### 4.2 In-App Report Access

The weekly report is accessible in the Usage Stats screen (Weekly tab) on Sundays (FF_PRD_v1_0.md §3.11.3). Suggestions appear in the same in-app summary card.

---

## 5. Computation

Suggestions are computed as part of the Sunday weekly report generation WorkManager task. No separate background task needed.

```dart
List<LimitSuggestion> computeSuggestions() {
  final suggestions = <LimitSuggestion>[];
  final thirtyDaysAgo = DateTime.now().subtract(const Duration(days: 30));
  final fourteenDaysAgo = DateTime.now().subtract(const Duration(days: 14));

  for (final limit in appLimitRepository.getAllActive()) {
    // Skip if in cooldown
    if (suggestionRepository.isInCooldown(limit.packageName)) continue;

    final records = usageRepository.getRecords(limit.packageName, thirtyDaysAgo);
    final avg = records.averageDailyUsage;
    final hitCount14d = records.last14Days.where((r) => r.hitLimit).length;

    if (avg <= limit.dailyLimit * 0.6 &&
        records.daysWithUsage >= 14) {
      suggestions.add(LimitSuggestion.tighten(
        packageName: limit.packageName,
        currentLimit: limit.dailyLimit,
        suggestedLimit: _roundUp5Min(avg),
        triggerDays: records.daysWithUsage,
      ));
    } else if (hitCount14d >= 10) {
      suggestions.add(LimitSuggestion.loosen(
        packageName: limit.packageName,
        currentLimit: limit.dailyLimit,
        suggestedLimit: _roundUp5Min(limit.dailyLimit + const Duration(minutes: 15)),
        triggerDays: hitCount14d,
      ));
    }
  }

  // Sort by signal strength, take top 3
  return suggestions
    .sortedByDescending((s) => s.triggerDays)
    .take(3)
    .toList();
}
```

---

## 6. Data Model

```dart
@freezed
class LimitSuggestion with _$LimitSuggestion {
  const factory LimitSuggestion({
    required String packageName,          // null for category suggestions
    AppCategory? category,                // null for per-app suggestions
    required SuggestionType type,         // tighten / loosen
    required Duration currentLimit,
    required Duration suggestedLimit,
    required int triggerDays,
    required DateTime generatedAt,
    required bool isDismissed,
    DateTime? dismissedAt,
    DateTime? appliedAt,
  }) = _LimitSuggestion;
}

enum SuggestionType { tighten, loosen }
```

---

## 7. Database Schema

### 7.1 New Table: limit_suggestions

```sql
CREATE TABLE limit_suggestions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  package_name TEXT,                    -- null for category suggestions
  category TEXT,                        -- null for per-app suggestions
  type TEXT NOT NULL,                   -- 'tighten' | 'loosen'
  current_limit_seconds INTEGER NOT NULL,
  suggested_limit_seconds INTEGER NOT NULL,
  trigger_days INTEGER NOT NULL,
  generated_at INTEGER NOT NULL,
  is_dismissed INTEGER NOT NULL DEFAULT 0,
  dismissed_at INTEGER,
  applied_at INTEGER
);
```

---

## 8. Module Boundary

**Owned by:** `app_limits/` module (extended from P1/P4 F3)

```
features/
└── app_limits/
    ├── data/
    │   ├── limit_suggestion_dao.dart         ← NEW
    │   └── app_limit_repository_impl.dart    ← EXTENDED (suggestion computation)
    ├── domain/
    │   ├── limit_suggestion.dart             ← NEW model
    │   └── use_cases/
    │       ├── compute_limit_suggestions.dart ← NEW
    │       └── apply_limit_suggestion.dart    ← NEW
    └── presentation/
        └── limit_suggestion_card.dart         ← NEW (used in weekly report)
```

Modifications to existing modules:
- `usage_stats/presentation/` — weekly report summary card extended with Limit Suggestions section

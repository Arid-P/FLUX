# FD Phase 4 — Feature 2: Statistics Screen

**Version:** 1.0  
**Phase:** 4  
**Status:** Locked  
**Author:** Ari  

---

## 1. Overview

A dedicated Statistics screen with two tabs — Tasks and Habits — providing longitudinal insights derived entirely from existing FD local data. No network required. All metrics are computed from the SQLite database.

---

## 2. P1 References

| Reference | Location |
|---|---|
| Tasks schema | TRD v2 §5 |
| Habits schema | TRD v2 §5 |
| Habit completion history | PRD v2 §5, FD-11 |
| Habit Detail Screen — SCR-06 | `ui_SCR-06-07_habit-detail-creation.md` |
| Navigation — bottom nav (4 tabs) | PRD v2 §8.5 |

---

## 3. Navigation

**Route:** `/statistics`  
**Access:** Bottom navigation bar — replaces or adds a 5th tab.

Since PRD v2 §8.5 defines 4 bottom nav tabs (Tasks, Calendar, Habits, Settings), Statistics needs a home. Options at implementation time:
- Add as 5th tab (bottom nav becomes 5 items)
- Access from Settings → Statistics
- Access from Habits screen header action

**Decision deferred to UI spec phase.** The route and screen are fully specced here; placement in navigation is a UI decision for a future `ui_SCR-statistics.md` file.

---

## 4. Tasks Tab

### 4.1 Time Range Selector

A segmented control at the top of the tab: **7D / 30D / 90D / All**. All metrics below update based on the selected range. Default: 30D.

### 4.2 Metrics

**1. Completion Rate**
- Definition: (completed tasks / total created tasks) × 100 in selected range
- Display: large percentage number + circular progress ring
- Toggle: Day / Week / Month — switches the granularity of the trend line chart below

**2. Total Tasks Created vs Completed (All Time)**
- Two numbers side by side: "Created: [N]" and "Completed: [M]"
- Shown regardless of time range selector (always all-time)
- Sub-label: "Since [first task creation date]"

**3. Average Tasks Completed Per Day**
- Rolling average within selected range
- Display: single number + "tasks/day" label

**4. Peak Productivity Hours**
- Bar chart (fl_chart): 24 bars representing hours 0–23
- Bar height = number of tasks completed in that hour (based on `completed_at` timestamp)
- Top 3 peak hours highlighted in app primary color, rest in secondary color
- X-axis: hour labels (6AM, 9AM, 12PM, 3PM, 6PM, 9PM)

**5. Overdue Rate**
- Definition: tasks completed after their due date OR still incomplete and past due date, as % of all tasks in range
- Display: percentage + small trend indicator (↑↓ vs previous period)

**6. Per-List Breakdown**
- Scrollable list of all lists
- Each row: list color dot + list name + completion rate % + total tasks count
- Sorted by total task count descending
- Tapping a list row: filters all metrics above to that list only (with a "Filtered: [list name] ×" chip at top)

**7. Longest Completion Streak**
- Definition: longest consecutive calendar days with at least 1 task completed
- Display: flame icon + "[N] days" + date range of the streak
- Also shows current streak if active

### 4.3 Chart Library
All charts use `fl_chart` (already in FD tech stack via FF — confirmed available).

---

## 5. Habits Tab

### 5.1 Layout

Scrollable screen. No time range selector at tab level — each metric has its own range context.

### 5.2 Metrics

**1. Cross-Habit Summary Heatmap**
- GitHub-style contribution grid
- Rows = each habit (habit name label on left)
- Columns = days (last 30 days, scrollable left to extend)
- Cell color: grey (not completed) → habit color at full opacity (fully completed, i.e. met daily target)
- Partial completion: habit color at proportional opacity (e.g. 1/3 completions = 33% opacity)
- Tapping a cell: shows tooltip — "[habit name] — [date] — [N]/[target] completions"

**2. Per-Habit Cards**
Scrollable list of cards, one per habit.

Each card contains:
- Habit name + habit icon + habit color
- **Completion rate:** % of days the daily target was met (last 30 days)
- **Current streak:** flame icon + "[N] days"
- **Longest streak:** "[N] days" + date range
- **Total completions:** all-time count
- **Monthly heatmap:** mini calendar grid (current month) — same cell coloring as summary heatmap but single-habit
- **Happiest moment:** the date with the highest consecutive streak momentum — shown as "[date] — [N] day streak peak"
- **Saddest moment:** the most recent date the streak was broken (reset to 0) — shown as "[date] — streak ended after [N] days"

---

## 6. Data Queries

All computed at query time, not stored. Computed on tab open with a loading indicator for large datasets.

### 6.1 Key Queries

```sql
-- Completion rate in range
SELECT 
  COUNT(*) as total,
  SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) as completed
FROM tasks
WHERE task_date BETWEEN :start AND :end
  AND is_trashed = 0

-- Peak productivity hours
SELECT 
  strftime('%H', datetime(completed_at/1000, 'unixepoch', 'localtime')) as hour,
  COUNT(*) as count
FROM tasks
WHERE is_completed = 1
  AND completed_at BETWEEN :start AND :end
GROUP BY hour
ORDER BY hour

-- Per-list breakdown
SELECT 
  l.id, l.name, l.color_hex,
  COUNT(t.id) as total,
  SUM(t.is_completed) as completed
FROM lists l
LEFT JOIN tasks t ON t.list_id = l.id
  AND t.task_date BETWEEN :start AND :end
  AND t.is_trashed = 0
GROUP BY l.id
ORDER BY total DESC

-- Habit completion rate (last 30 days)
SELECT 
  h.id, h.name, h.target_count,
  COUNT(hc.id) as total_completions,
  COUNT(DISTINCT hc.completion_date) as days_with_any_completion
FROM habits h
LEFT JOIN habit_completions hc ON hc.habit_id = h.id
  AND hc.completion_date >= :thirtyDaysAgo
GROUP BY h.id
```

---

## 7. Routing

```dart
GoRoute(
  path: '/statistics',
  builder: (_, __) => const StatisticsScreen(),
),
```

---

## 8. Module Boundary

**New module:** `statistics/`

```
features/
└── statistics/
    ├── data/
    │   ├── task_stats_dao.dart
    │   ├── habit_stats_dao.dart
    │   └── statistics_repository_impl.dart
    ├── domain/
    │   ├── task_statistics.dart
    │   ├── habit_statistics.dart
    │   └── use_cases/
    │       ├── get_task_statistics.dart
    │       └── get_habit_statistics.dart
    └── presentation/
        ├── statistics_screen.dart
        ├── tasks_tab.dart
        ├── habits_tab.dart
        ├── peak_hours_chart.dart
        ├── per_list_breakdown.dart
        ├── habit_summary_heatmap.dart
        └── habit_stat_card.dart
```

No existing P1 modules modified except navigation (placement TBD at UI spec phase).

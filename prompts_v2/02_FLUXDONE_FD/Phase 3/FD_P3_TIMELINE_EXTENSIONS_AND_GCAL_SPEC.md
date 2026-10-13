# 📅 Topic FD-P3-5: Desktop Timeline Canvas, 7-Day Week View & Google Calendar Overlay Specification

**Document ID:** `FD_P3_TIMELINE_EXTENSIONS_AND_GCAL_SPEC`  
**Phase:** Phase 3 (Ubuntu Linux Desktop Companion via Compose Multiplatform)  
**Target Package:** `com.flux.done.desktop.timeline`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Compose Multiplatform 1.6+ + Skiko Canvas Rendering)  
**Target OS:** Ubuntu Linux Workstation (GNOME X11 & Wayland, JVM 17+)  
**Prerequisites:** [`FD_P1_CALENDAR_TIMELINE_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%201/FD_P1_CALENDAR_TIMELINE_SPEC.md) & [`FD_P3_DESKTOP_UI_SPEC.md`](file:///home/hp/Flux_Prompts/prompts_v2/02_FLUXDONE_FD/Phase%203/FD_P3_DESKTOP_UI_SPEC.md)  

---

## 1. Overview & Architectural Philosophy

Widescreen desktop monitors enable an unconstrained, macro-level perspective of an entire academic week. Rather than flipping through single-day mobile cards, Ari requires an expansive, high-density **7-Day Full Week Time-Blocking Canvas**.

### Core Capabilities Established in FD-P3-5:
1. **Unified Continuous Drag with Magnetic Day Snapping (Universal across 1-Day, 3-Day & 7-Day):**
   * Smooth, continuous horizontal trackpad/mouse panning with fluid sub-pixel interpolation.
   * **The At-Rest Invariant:** The moment user interaction ceases (finger lifted from trackpad or mouse drag released), the canvas **magnetically snaps to whole-day boundaries**. Zero fractional days are ever cut off at rest.
2. **7-Day Full Week Canvas:** Widescreen column matrix (Monday through Sunday) fitting the desktop canvas width, with sticky vertical 24-hour time ruler and All-Day strip.
3. **Smooth Wheel & Keyboard Timeline Zoom:** `Ctrl + Mouse Wheel` smoothly adjusts vertical time scale between 15m, 30m, and 60m grid cell heights, supported by `Ctrl + +`, `Ctrl + -`, and `Ctrl + 0` keyboard presets.
4. **Read-Only Google Calendar Overlay:** External calendar events (school lectures, exams, birthdays) render as **1.5dp outlined muted blocks** on top of the timeline. Cached locally in SQLite (`gcal_events_cache`) for instant offline display, with 1-click `xdg-open` browser deep linking.

---

## 2. The Universal Drag & Magnetic Day Snapping Engine

Across **all calendar formats** (1-Day, 3-Day, and 7-Day), navigation supports both chevron step-jumps (`<` / `>`) and freeform continuous horizontal dragging.

```
+───────────────────────────────────────────────────────────────────────────────────+
| USER DRAGGING / FLINGING (Active Interaction)                                      |
| -> Smooth continuous sub-pixel offset: Fractional days permitted (e.g. +1.4 days) |
+───────────────────────────────────────────────────────────────────────────────────+
                                          │
                    [ User lifts finger / releases mouse drag ]
                                          │
                                          ▼
+───────────────────────────────────────────────────────────────────────────────────+
| MAGNETIC DAY SNAPPING (Spring / Tween Animation)                                  |
| -> Snaps forward/backward to nearest integer day boundary:                        |
|    TargetOffset = round(CurrentOffset / DayColumnWidth) * DayColumnWidth          |
+───────────────────────────────────────────────────────────────────────────────────+
                                          │
                                          ▼
+───────────────────────────────────────────────────────────────────────────────────+
| AT-REST STATE (Zero interaction)                                                  |
| -> Exactly 1, 3, or 7 FULL day columns displayed. Zero truncated or half days!    |
+───────────────────────────────────────────────────────────────────────────────────+
```

### 2.1 Implementation Specification (`TimelineDaySnapFlingBehavior.kt`)

```kotlin
package com.flux.done.desktop.timeline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import kotlin.math.roundToInt

class TimelineDaySnapFlingBehavior(
    private val getDayColumnWidthPx: () -> Float,
    private val getCurrentDayOffset: () -> Float,
    private val onSnapComplete: (targetDayIndex: Int) -> Unit
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val columnWidth = getDayColumnWidthPx()
        if (columnWidth <= 0f) return initialVelocity

        val currentOffset = getCurrentDayOffset()
        
        // Project resting position based on kinetic velocity
        val projectedOffset = currentOffset + (initialVelocity * 0.15f)
        val targetDayIndex = (projectedOffset / columnWidth).roundToInt()
        val targetOffset = targetDayIndex * columnWidth

        var lastValue = currentOffset
        val animatable = Animatable(currentOffset)

        animatable.animateTo(
            targetValue = targetOffset,
            initialVelocity = initialVelocity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) {
            val delta = value - lastValue
            scrollBy(delta)
            lastValue = value
        }

        onSnapComplete(targetDayIndex)
        return 0f
    }
}
```

---

## 3. Desktop 7-Day Full Week Canvas Specification

### 3.1 7-Day Canvas Wireframe (`DesktopWeekTimelineScreen.kt`)

```
+-------------------------------------------------------------------------------------------------------------------------------+
|  [ < Prev Week ]  [ Today (13 Oct) ]  [ Next Week > ]    [ 1 Day | 3 Day | 7 Day (Active) ]    [ Zoom: 30m ] [ 🔄 GCal Sync ]  |
+-------+------------------+------------------+------------------+------------------+------------------+-------------+----------+
| GMT+5 | MON 12 OCT       | TUE 13 OCT (Now) | WED 14 OCT       | THU 15 OCT       | FRI 16 OCT       | SAT 17 OCT  | SUN 18   |
+-------+------------------+------------------+------------------+------------------+------------------+-------------+----------+
| ALL   | [ Revise Geo ]   | [ Mock Exam 1 ]  | [ Rest Day · ]   |                  | [ Physics Lab ]  |             |          |
| DAY   |                  | [ Read Article ] |                  |                  |                  |             |          |
+=======+==================+==================+==================+==================+==================+=============+==========+
| 08:00 |                  |                  |                  |                  |                  |             |          |
| 09:00 |                  | ┌──────────────┐ |                  |                  |                  |             |          |
|       |                  | │ Trig Review  │ |                  |                  |                  |             |          |
| 10:00 | ┌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┐ | └──────────────┘ |                  |                  |                  |             |          |
|       | ┆ School Math  ┆ |                  |                  |                  |                  |             |          |
| 11:00 | ┆ (GCal Read)  ┆ |                  |                  |                  |                  |             |          |
|       | └╌╌╌╌╌╌╌╌╌╌╌╌╌╌┘ |                  |                  |                  |                  |             |          |
| 12:00 |                  |                  |                  |                  |                  |             |          |
| 13:00 |                  |                  |                  |                  |                  |             |          |
| 14:00 |                  |                  |                  |                  |                  |             |          |
|       | ┌──────────────┐ | ────────● NOW    |                  |                  |                  |             |          |
| 15:00 | │ Angle Chasing│ | ┌──────────────┐ |                  |                  |                  |             |          |
| 16:00 | │ Incenter Lem │ | │ Solve 5 Prob │ |                  |                  |                  |             |          |
|       | └──────────────┘ │ 16:00 - 17:30  │ |                  |                  |                  |             |          |
| 17:00 |                  | └──────────────┘ |                  |                  |                  |             |          |
+-------+------------------+------------------+------------------+------------------+------------------+-------------+----------+
```

### 3.2 Responsive 7-Column Layout & All-Day Strip

1. **Width Distribution:** In 7-Day mode, the center canvas divides available horizontal space into 7 equal columns after reserving `56dp` for the left sticky hour ruler. Minimum column width is `120dp`; on narrower displays ($<900\text{dp}$), the canvas horizontally scrolls with magnetic snapping to single-day boundaries.
2. **Current Day Column Accent:** The active day (Today) features a subtle primary tint background (`#1565C0` at 6% opacity) and an accent date pill in the header.
3. **Current Time Indicator ("Now" Red Line):** A live horizontal line (`#D90429` 2dp stroke) spanning the current day column with a 6dp circular head, ticking every 60 seconds.
4. **All-Day Task Strip:** Sticky container below the day headers. Automatically expands up to 3 rows of all-day chips. If $>3$ tasks exist on a day, shows `"+N more"` clickable badge expanding an overlay list.

---

## 4. Desktop Mouse Wheel & Keyboard Zoom Engine

Academic schedules vary in density. A student planning 15-minute pomodoros requires high vertical spacing, while a macro-planner prefers dense hourly blocks.

### 4.1 Zoom Levels & Spatial Dimensions

| Zoom Level | Slot Height (30-min block) | 1-Hour Height | Pixels per Minute | Use Case |
|---|---|---|---|---|
| **Compact (60m)** | `24dp` | `48dp` | `0.8 dp/min` | Bird's-eye full day overview without scrolling |
| **Normal (30m - Default)** | `40dp` | `80dp` | `1.33 dp/min` | Standard daily time-blocking |
| **Detailed (15m)** | `64dp` | `128dp` | `2.13 dp/min` | Fine-grained Olympiad problem-by-problem schedule |

### 4.2 Zoom Gesture & Shortcut Bindings

* **`Ctrl + Mouse Wheel Scroll`:** Smoothly adjusts `pixelsPerMinute` between `0.6` and `3.0`.
* **`Ctrl + +` / `Ctrl + =`:** Steps zoom in to the next discrete preset (`60m -> 30m -> 15m`).
* **`Ctrl + -`:** Steps zoom out (`15m -> 30m -> 60m`).
* **`Ctrl + 0`:** Instantly resets to default `30m` scale (`40dp` per 30m slot).
* **Scroll Position Anchoring:** When zooming via mouse wheel, the time slot under the mouse cursor remains stationary as the canvas expands/contracts around it.

---

## 5. Read-Only Google Calendar Overlay Engine

External commitments must be visible on the timeline to prevent double-booking study hours, but external events must not pollute the primary tasks table.

### 5.1 Local SQLite Cache Table (`gcal_events_cache`)

Added to the multiplatform Room schema in `fluxdone.db`:

```sql
CREATE TABLE IF NOT EXISTS gcal_events_cache (
    id TEXT PRIMARY KEY NOT NULL,
    calendar_id TEXT NOT NULL,
    title TEXT NOT NULL,
    start_time_epoch INTEGER NOT NULL,
    end_time_epoch INTEGER NOT NULL,
    is_all_day INTEGER NOT NULL DEFAULT 0,
    color_hex TEXT,
    event_url TEXT NOT NULL,
    etag TEXT NOT NULL,
    last_synced_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gcal_time_range 
ON gcal_events_cache(start_time_epoch, end_time_epoch);
```

### 5.2 Visual Rendering & Overlap Invariants

1. **Outlined Muted Style:** Google Calendar events render strictly with a **1.5dp solid outline** (using Google event color or `#9E9E9E` muted gray) with a semi-transparent or clear interior. A discrete `[ 📅 GCal ]` micro-icon appears in the top-right corner.
2. **FD Priority Invariant (Z-Index Layering):** FluxDone task blocks **always render above** Google Calendar overlay blocks.
3. **Column Overlap Separation:**
   * When multiple FD tasks overlap, they split into side-by-side columns.
   * When an FD task overlaps a Google Calendar event, the FD task renders in its primary column and the Google Calendar block renders slightly offset or underneath with 50% opacity, ensuring Ari's tasks remain the dominant visual focus.
4. **Interaction Contract (Read-Only):**
   * Dragging, resizing, or checking off Google Calendar blocks is strictly disabled.
   * Clicking a Google Calendar block invokes the native Linux browser launcher:
     ```kotlin
     ProcessBuilder("xdg-open", event.eventUrl).start()
     ```
5. **Background Sync & Offline Resilience:**
   * Fetches the visible week range on app startup and every 15 minutes.
   * If network connection is unavailable, events render seamlessly from `gcal_events_cache` without blocking or throwing errors. A discreet offline dot is displayed in the calendar header.

---

## 6. MVI State & Event Contracts

```kotlin
package com.flux.done.desktop.timeline.mvi

import java.time.LocalDate

data class DesktopTimelineState(
    val viewMode: TimelineViewMode = TimelineViewMode.WEEK_7_DAY,
    val anchorDate: LocalDate = LocalDate.now(),
    val zoomSlotHeightDp: Float = 40f, // 30-min slot default
    val dayColumnWidthPx: Float = 0f,
    val horizontalScrollOffsetPx: Float = 0f,
    val isDragging: Boolean = false,
    val tasksByDate: Map<LocalDate, List<TimelineTaskItem>> = emptyMap(),
    val gcalEventsByDate: Map<LocalDate, List<GCalOverlayItem>> = emptyMap(),
    val isGcalConnected: Boolean = false,
    val isGcalSyncing: Boolean = false,
    val selectedTask: TimelineTaskItem? = null
)

enum class TimelineViewMode(val daysCount: Int) {
    DAY_1(1),
    DAY_3(3),
    WEEK_7_DAY(7)
}

sealed interface DesktopTimelineAction {
    data class SwitchViewMode(val mode: TimelineViewMode) : DesktopTimelineAction
    data class OnHorizontalDragDelta(val deltaPx: Float) : DesktopTimelineAction
    data object OnDragReleased : DesktopTimelineAction
    data class StepDays(val deltaDays: Int) : DesktopTimelineAction
    data class ZoomDelta(val deltaY: Float, val anchorMinute: Int) : DesktopTimelineAction
    data object ResetZoom : DesktopTimelineAction
    data class SelectTask(val taskId: String) : DesktopTimelineAction
    data class OpenGCalEventInBrowser(val url: String) : DesktopTimelineAction
    data object TriggerGCalSync : DesktopTimelineAction
}
```

---

## 7. Verification & Testing Contracts

1. **Magnetic Day Snapping Test:** In 7-Day view, drag the timeline horizontally by `0.45 * dayWidth`. Release drag. Verify that the animation smoothly glides back to index 0. Drag by `0.6 * dayWidth` and release; verify it snaps forward to index 1.
2. **At-Rest Invariant Test:** Ensure that after every gesture or step button click, the left-most visible column exactly aligns with the start of a calendar day, displaying zero clipped/fractional day boundaries.
3. **Zoom Scale Invariant Test:** Set zoom to 15m. Verify the vertical distance for 1 hour is `128dp`. Verify that pressing `Ctrl + 0` resets 1 hour to `80dp`.
4. **Google Calendar Read-Only Test:** Populate `gcal_events_cache` with a mock school lecture. Click the block. Verify that `xdg-open` is invoked with the event URL and zero modification mutations are dispatched to the Room database.

# 📋 Topic FF-P3-2: Session Templates & Auto-Scheduling Specification

**Document ID:** `FF_P3_SESSION_TEMPLATES_SPEC`  
**Phase:** Phase 3 (Intelligence, Lock Screen Presence & Session Ergonomics)  
**Target Package:** `com.flux.focus.features.templates`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Jetpack Compose + Room DAO + Deterministic Interval Math Engine)  
**Theme Context:** Holst (Холст) Dark Palette  

---

## 1. Overview & Core Philosophy

Manually creating focus sessions date-by-date is tedious. When setting up a multi-day study schedule, users often give up or manually squeeze sessions into sub-optimal hours.

**Session Templates** decouple the blueprint of a session from a specific calendar date. **Auto-Scheduling** uses pure deterministic interval math (zero AI hallucinations) to scan calendar gaps and stamp concrete scheduled sessions into the user's planner with guaranteed automatic start.

### 1.1 Core Principles
* **Templates are Date-Agnostic Blueprints:** Define *how long* and *what rules*, not *when*.
* **Deterministic Gap Math:** Pure local set-interval math scans for unoccupied calendar blocks; 100% predictable, testable, and offline.
* **Reviewable Proposal Before Stamping:** The engine never blindly writes to the calendar; it presents a clean proposed schedule that the user can inspect, tweak, and approve with 1 tap.
* **Pre-Populated Starter Templates:** FluxFoxus ships with 3 default templates on fresh install to get the user focusing immediately.

---

## 2. Dependencies & Phase 2 Integration

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Scheduled Sessions Engine** | [`FF_P2_SCHEDULED_SESSIONS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%202/FF_P2_SCHEDULED_SESSIONS_SPEC.md) | Auto-scheduled proposals are stamped directly into `focus_sessions` with `session_origin = 'SCHEDULED'`, `status = 'PLANNED'`, and exact `AlarmManager` alarms. |
| **Presets System** | [`FF_P1_PRESETS_AND_FOCUS_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/03_FLUXFOXUS_FF/Phase%201/FF_P1_PRESETS_AND_FOCUS_SPEC.md) | Templates link foreign key to `presets.id` to inherit whitelists/blacklists and break durations. |
| **Standalone Planner UI** | `Tab.Focus` Planner Screen | Planner gains a top Segmented Control: `[ Timeline ]` vs `[ Templates ]`. |
| **FluxDone IPC (Optional)** | `com.flux.action.FOCUS_BLOCK_SYNC` | If FluxDone is installed, auto-scheduler queries existing timed task blocks to avoid collisions. |

---

## 3. Pre-Populated Starter Templates

On initial install, `DatabaseCallback` populates the Room database with 3 standard templates:

| Starter Template | Emoji | Target Duration | Linked Preset | Preferred Window |
|---|:---:|---|---|---|
| **Pomodoro Sprint** | ⚡ | 25 minutes | Default Preset (1 break, 5m) | Any time |
| **Deep Focus Block** | 🎯 | 60 minutes | Deep Work (2 breaks, 5m) | 08:00 AM – 02:00 PM |
| **Olympiad Marathon** | 🏆 | 120 minutes | Study Mode (2 breaks, 10m) | 02:00 PM – 09:00 PM |

*Users have full authority to edit, duplicate, or delete these starter templates at any time.*

---

## 4. Deterministic Auto-Scheduling Algorithm

### 4.1 Input Parameters
* `template`: Selected `SessionTemplateEntity`.
* `targetCount`: Number of sessions requested ($N \in [1, 7]$).
* `dateRange`: Start date to End date (default: Today to Today + 6 days).
* `bufferMinutes`: Minimum buffer before and after each session (default: **15 minutes**).
* `respectPreferredWindow`: Boolean flag.

### 4.2 Gap-Scanning Math (`AutoScheduleEngine.kt`)

```
For each calendar day in dateRange:
  1. Determine Day Bounds:
     dayStart = max(preferredWindow.start, 06:00 AM)
     dayEnd   = min(preferredWindow.end, 11:00 PM)

  2. Query Existing Busy Blocks:
     busyBlocks = focusSessionsDao.getSessionsBetween(dayStart, dayEnd)
                  + fdIntegrationRepository.getTimedTasks(dayStart, dayEnd)
     sortedBlocks = busyBlocks.sortedBy { it.startTime }

  3. Scan Open Gaps:
     For each interval between consecutive sortedBlocks:
       gapDuration = nextBlock.startTime - currentBlock.endTime
       requiredDuration = targetDurationMinutes + (2 × bufferMinutes)

       If gapDuration >= requiredDuration:
         candidateStart = currentBlock.endTime + bufferMinutes
         candidateEnd   = candidateStart + targetDurationMinutes
         Add to candidatesList

  4. Prioritize & Select:
     Prioritize candidates that fall squarely within template.preferredWindow.
     Select top N candidates matching targetCount.
```

If fewer than $N$ slots are found, the UI transparently informs the user:  
> *"Found 2 open slots (requested 3). Expand date range or adjust preferred time window."*

---

## 5. UI & Interaction Flow

### 5.1 Planner Screen Navigation (`Tab.Focus` Planner)

```
+-------------------------------------------------------+
|  PLANNER                                        [ + ] |
|  +-------------------------------------------------+  |
|  |     [ Timeline ]      |     [ Templates (3) ]   |  |
|  +-------------------------------------------------+  |
|                                                       |
|  MY BLUEPRINTS                                        |
|  +-------------------------------------------------+  |
|  | ⚡ Pomodoro Sprint                   25 mins     |  |
|  | Linked: Default · 1 Break (5m)                  |  |
|  | [ Quick Schedule ➔ ]                            |  |
|  +-------------------------------------------------+  |
|  +-------------------------------------------------+  |
|  | 🎯 Deep Focus Block                 60 mins     |  |
|  | Linked: Deep Work · 2 Breaks (5m)               |  |
|  | Preferred: 08:00 AM - 02:00 PM                  |  |
|  | [ Quick Schedule ➔ ]                            |  |
|  +-------------------------------------------------+  |
|                                                       |
+-------------------------------------------------------+
```

### 5.2 Auto-Schedule Configuration Sheet

Tapping `[ Quick Schedule ➔ ]` opens the parameter sheet:
* **Template Title:** Header displaying name and duration pill.
* **Session Stepper:** `[ - ] 3 Sessions [ + ]` (Range: 1–7).
* **Horizon Picker:** `Next 3 Days | Next 7 Days (Default) | Custom Range`.
* **Preferred Window Toggle:** Switch: `Respect Preferred Hours (08:00 AM – 02:00 PM) [ ON ]`.
* **Action:** `[ Find Available Slots ➔ ]` runs local gap math in <10ms.

---

### 5.3 Proposed Schedule Preview Sheet

```
+-------------------------------------------------------+
|  ━  Proposed Schedule Review                          |
+-------------------------------------------------------+
|  Template: 🎯 Deep Focus Block (60m)                  |
|  Found 3 optimal slots with 15m buffer:               |
|                                                       |
|  +-------------------------------------------------+  |
|  | Tue, Oct 13   09:30 AM – 10:30 AM     [ Edit ✏️ ]|  |
|  +-------------------------------------------------+  |
|  | Wed, Oct 14   10:00 AM – 11:00 AM     [ Edit ✏️ ]|  |
|  +-------------------------------------------------+  |
|  | Fri, Oct 16   08:30 AM – 09:30 AM     [ Edit ✏️ ]|  |
|  +-------------------------------------------------+  |
|                                                       |
|  Each session will start automatically on the dot.    |
|                                                       |
|  +-------------------------------------------------+  |
|  |          [ Confirm & Add to Planner ]           |  |
|  +-------------------------------------------------+  |
+-------------------------------------------------------+
```

* **Inline Editing:** Tapping `[ Edit ✏️ ]` opens a standard time-picker to shift an individual slot if desired.
* **Confirmation:** Tapping `[ Confirm & Add to Planner ]` commits all sessions to Room DB, schedules exact `AlarmManager` alarms, and navigates back to the Planner timeline.

---

## 6. Database Schema & Room Entities

### 6.1 `session_templates` Room Entity

```kotlin
package com.flux.focus.features.templates.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flux.focus.core.database.entity.PresetEntity

@Entity(
    tableName = "session_templates",
    foreignKeys = [
        ForeignKey(
            entity = PresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("preset_id"),
        Index("created_at")
    ]
)
data class SessionTemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "preset_id")
    val presetId: String,

    @ColumnInfo(name = "target_duration_minutes")
    val targetDurationMinutes: Int,

    @ColumnInfo(name = "preferred_start_time")
    val preferredStartTime: String? = null, // "HH:mm" (24h format)

    @ColumnInfo(name = "preferred_end_time")
    val preferredEndTime: String? = null,   // "HH:mm" (24h format)

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "is_system_starter")
    val isSystemStarter: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 6.2 Room DAO Interface (`SessionTemplateDao.kt`)

```kotlin
package com.flux.focus.features.templates.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flux.focus.features.templates.data.entity.SessionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionTemplateDao {

    @Query("SELECT * FROM session_templates ORDER BY created_at ASC")
    fun observeAllTemplates(): Flow<List<SessionTemplateEntity>>

    @Query("SELECT * FROM session_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): SessionTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SessionTemplateEntity)

    @Update
    suspend fun updateTemplate(template: SessionTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: SessionTemplateEntity)
}
```

---

## 7. Jetpack Compose UI Specifications

### 7.1 Template Card Composable (`TemplateItemCard.kt`)

```kotlin
@Composable
fun TemplateItemCard(
    template: SessionTemplateEntity,
    presetName: String,
    presetEmoji: String,
    onQuickScheduleClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF4F1DE),
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0D1B2A),
                    border = BorderStroke(1.dp, Color(0xFF415A77))
                ) {
                    Text(
                        text = "${template.targetDurationMinutes}m",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD4C4A8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$presetEmoji Linked: $presetName",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA0AAB2)
            )

            if (template.preferredStartTime != null && template.preferredEndTime != null) {
                Text(
                    text = "Preferred: ${template.preferredStartTime} – ${template.preferredEndTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0AAB2)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onQuickScheduleClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF778D7A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Quick Schedule ➔",
                    color = Color(0xFFF4F1DE),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

### 7.2 Holst Design Tokens Applied

| Element | Token / Color | Usage |
|---|---|---|
| Card Background | `SurfaceCard` (`#1B263B`) | Template container surface |
| Card Border | `SurfaceBorder` (`#415A77`) | Border stroke and dividers |
| Action Button | `AccentSage` (`#778D7A`) | `[ Quick Schedule ➔ ]` CTA |
| Duration Pill | `PrimarySand` (`#D4C4A8`) | Duration highlight pill (`25m`, `60m`) |
| Primary Typography | `TextLight` (`#F4F1DE`) | Template titles, button labels |
| Subtext | `TextMuted` (`#A0AAB2`) | Linked preset subtitle, window hint |

---

## 8. Verification & Branch-Testing Isolation Workflow

### 8.1 Zero-Mock Automated & Manual Test Cases
1. **Starter Seed Verification:**
   * On fresh install / first launch:
   * **Verify:** `session_templates` table has exactly 3 records (`Pomodoro Sprint`, `Deep Focus Block`, `Olympiad Marathon`).
2. **Gap Finding Edge Cases:**
   * Day has existing session from 10:00 AM to 12:00 PM.
   * Auto-schedule 60m session with 15m buffer.
   * **Verify:** Slot `10:30 AM – 11:30 AM` is NOT proposed (collision). Next proposed slot is at least 15m after 12:00 PM (e.g. `12:15 PM – 01:15 PM`).
3. **Stamping Integrity Test:**
   * Propose 3 sessions, confirm preview.
   * **Verify:** 3 new records inserted into `focus_sessions` with `status = 'PLANNED'` and exact `AlarmManager` alarms registered.

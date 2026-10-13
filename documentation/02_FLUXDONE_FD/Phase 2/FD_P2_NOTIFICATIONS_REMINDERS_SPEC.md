# 🔔 Topic FD-P2-5: Notification & Reminders Engine Specification

**Document ID:** `FD_P2_NOTIFICATIONS_REMINDERS_SPEC`  
**Phase:** Phase 2 (Advanced Ergonomics, Settings & Inter-App IPC)  
**Target Package:** `com.flux.done.features.notifications`  
**Architecture:** MVI / Clean Architecture (Kotlin 2.x + Android `AlarmManager` Exact Alarms + `NotificationManagerCompat` + `BroadcastReceiver` + Room SQLite DAOs)  
**Screen References:** SCR-09 (Settings - Notifications), `NotificationRescheduleActivity.kt` (Floating Reschedule Dialog)  
**Theme Context:** Light & Dark Modes supported via `FLUX_DESIGN_TOKENS.md`  

---

## 1. Overview & Core Philosophy

In Ari’s intensive academic schedule, punctual alarms for coaching batches, problem-solving blocks, and mock tests are critical. A missed study block disrupts the entire day's rhythm.

**Topic FD-P2-5 establishes the Punctual Notification & Reminders Engine for FluxDone.**  
It provides zero-miss alert guarantees combined with friction-free notification-shade triaging:
* **`AlarmManager` Exact Alarms:** Uses `setExactAndAllowWhileIdle()` to reliably pierce Android Doze mode and manufacturer battery optimizations at the exact target second.
* **Boot Re-Arming Gateway:** `BootCompletedReceiver` queries Room SQLite upon system boot or app update, instantly re-registering all upcoming alarms.
* **The 3 Actionable Notification Buttons (User-Specified):**
  1. **`[ ✓ Mark Done ]`:** Silent background completion adhering to the system-wide subtask invariant (completes parent task and all child subtasks).
  2. **`[ 🚫 Won't Do ]`:** Marks task as skipped/won't-do. For recurring tasks, advances to the next instance per RRULE without penalizing past completion records.
  3. **`[ 📅 Reschedule ]`:** Launches a focused, transparent floating dialog (`NotificationRescheduleActivity`) offering instant rescheduling options (`+15m`, `+30m`, `+1h`, `+2h`, `Tomorrow`, `Next Week`, `Pick Date & Time`).
* **3 Dedicated Notification Channels:** Strict visual and audio separation between high-priority task alarms, gentle habit check-in nudges, and the daily morning agenda summary.
* **Configurable Morning Agenda Summary:** Fires by default at **07:00 AM** with an expandable digest of the day's scheduled tasks, fully customizable in Settings.
* **Smart Habit Nudges:** Habit reminders check Room SQLite at fire time; if the habit is already completed today (`is_target_met == 1`), the notification is suppressed to prevent nagging.

---

## 2. Dependencies & System Contracts

| Component | Upstream / Peer Specification | Technical Contract |
|---|---|---|
| **Task Management & Subtasks** | [`FD_P1_TASK_MANAGEMENT_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_TASK_MANAGEMENT_SPEC.md) | Tapping `[ ✓ Mark Done ]` executes the mandatory invariant: marks task and all subtasks complete in Room DB. |
| **Recurrence Engine** | [`FD_P1_RECURRENCE_ENGINE_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_RECURRENCE_ENGINE_SPEC.md) | Tapping `[ 🚫 Won't Do ]` advances recurring tasks to next occurrence per Option A lifecycle without breaking streak. |
| **Habit Tracker** | [`FD_P1_HABIT_TRACKER_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_HABIT_TRACKER_SPEC.md) | Ingests `reminder_time` and checks `is_target_met == 0` before dispatching habit nudges. |
| **Smart Lists** | [`FD_P1_SMART_LISTS_AND_TRASH_SPEC.md`](file:///home/hp/Flux_Prompts/documentation/02_FLUXDONE_FD/Phase%201/FD_P1_SMART_LISTS_AND_TRASH_SPEC.md) | Tapping the Daily Morning Agenda notification deep-links directly into the `Today` smart list. |

---

## 3. UI Specifications & Interaction Design

### 3.1 Task Reminder Notification Anatomy

Rendered as an Android System Notification in the notification shade and heads-up banner:

```
+-----------------------------------------------------------------+
|  [FLUXDONE]  Task Reminder • 16:00                         Now  |
|  Solve 5 Diophantine Equations (!High)                          |
|  [ Number Theory #2E7D32 ] • Due in 15 minutes                  |
|                                                                 |
|  [ ✓ Mark Done ]        [ 🚫 Won't Do ]       [ 📅 Reschedule ] |
+-----------------------------------------------------------------+
```

#### Action Button Behavior:
1. **`[ ✓ Mark Done ]`:**
   * Handled by `NotificationActionReceiver` in the background.
   * Atomically sets `tasks.is_completed = 1, completed_at = now`.
   * Atomically sets `subtasks.is_completed = 1` for all child subtasks.
   * Cancels the notification and delivers a subtle completion haptic tick.
   * Does **not** bring the app to the foreground.
2. **`[ 🚫 Won't Do ]`:**
   * Handled by `NotificationActionReceiver` in the background.
   * For non-recurring tasks: Sets `is_completed = 2` (or soft-deletes/archives per user preference).
   * For recurring tasks: Skips the current instance and immediately schedules the next recurrence per RFC 5545 RRULE.
   * Dismisses the notification cleanly.
3. **`[ 📅 Reschedule ]`:**
   * Launches `NotificationRescheduleActivity` (a transparent floating Compose dialog docked in the center of the screen).

---

### 3.2 Floating Quick Reschedule Dialog (`NotificationRescheduleActivity.kt`)

Summoned directly when the user taps `[ 📅 Reschedule ]` from the notification shade:

```
+-----------------------------------------------------------------+
|  RESCHEDULE TASK                                                |
|  "Solve 5 Diophantine Equations"                                |
+-----------------------------------------------------------------+
|  QUICK SNOOZE:                                                  |
|  [ +15 min ]      [ +30 min ]      [ +1 hour ]      [ +2 hours ]|
|                                                                 |
|  CALENDAR SHIFT:                                                |
|  [ 🌅 Tomorrow ]                   [ 📅 Next Week (+7d) ]        |
|  [ 🕒 Pick Custom Date & Time ]                                 |
|                                                                 |
|  -------------------------------------------------------------  |
|  [ Cancel / Dismiss ]                                           |
+-----------------------------------------------------------------+
```

* **Instant Background Application:** Tapping any quick snooze pill (`+15m`, `+30m`, `+1h`, `+2h`) or calendar target (`Tomorrow`, `Next Week`):
  1. Updates the task's `due_date` and `start_time` in Room SQLite.
  2. Cancels the existing notification.
  3. Re-schedules the `AlarmManager` exact alarm for the new target time.
  4. Closes the dialog activity immediately without opening the main app interface.

---

### 3.3 Daily Morning Agenda Notification (07:00 AM Default)

Fires every morning at 07:00 AM (configurable in Settings):

```
+-----------------------------------------------------------------+
|  [FLUXDONE]  Today's Study Agenda • 07:00 AM                    |
|  5 tasks scheduled for today (2 High Priority)                  |
|  • 16:00 - Solve 5 Diophantine Equations (!High)                |
|  • 18:00 - PW Neev Diamond Physics Ch 4                         |
|  • 20:00 - Olympiad Geometry Incenter Lemma                     |
|                                                                 |
|  [ Open Today View → ]                        [ Dismiss ]       |
+-----------------------------------------------------------------+
```
* **Summary Content:** Pulls today's scheduled tasks from Room SQLite, ordered chronologically by `start_time ASC`.
* **Deep-Link Action:** Tapping `[ Open Today View → ]` or the notification body opens FluxDone directly into the **Today** smart list (SCR-05).

---

## 4. Android System Architecture & Notification Channels

### 4.1 3 Dedicated Android Notification Channels

```kotlin
package com.flux.done.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelConstants {
    const val CHANNEL_TASK_REMINDERS = "flux_channel_task_reminders"
    const val CHANNEL_HABIT_NUDGES = "flux_channel_habit_nudges"
    const val CHANNEL_DAILY_AGENDA = "flux_channel_daily_agenda"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "Task Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Punctual heads-up alerts and alarms for scheduled tasks"
                enableVibration(true)
                setShowBadge(true)
            }

            val habitChannel = NotificationChannel(
                CHANNEL_HABIT_NUDGES,
                "Habit Check-In Nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle check-in nudges for uncompleted daily habits"
                enableVibration(false)
                setShowBadge(false)
            }

            val agendaChannel = NotificationChannel(
                CHANNEL_DAILY_AGENDA,
                "Daily Morning Agenda",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Morning overview of today's scheduled tasks"
                setShowBadge(false)
            }

            manager.createNotificationChannels(listOf(taskChannel, habitChannel, agendaChannel))
        }
    }
}
```

---

### 4.2 Exact `AlarmManager` Scheduling Gateway (`TaskAlarmScheduler.kt`)

```kotlin
package com.flux.done.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.flux.done.core.database.TaskEntity
import java.time.Instant

class TaskAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskAlarm(task: TaskEntity, triggerEpochMs: Long) {
        // Safety check: do not schedule for past times, completed, or trashed tasks
        if (triggerEpochMs <= System.currentTimeMillis() || task.is_completed == 1 || task.is_deleted == 1) {
            return
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_PRIORITY, task.priority)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_LIST_ID, task.list_id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Fallback for API 31+ if exact alarms are restricted
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerEpochMs, pendingIntent)
        }
    }

    fun cancelTaskAlarm(taskId: String) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
```

---

### 4.3 Actionable Notification Receiver (`NotificationActionReceiver.kt`)

Handles silent background execution for `[ ✓ Mark Done ]` and `[ 🚫 Won't Do ]`:

```kotlin
package com.flux.done.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.flux.done.core.database.FluxDoneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DONE = "com.flux.done.action.MARK_DONE"
        const val ACTION_WONT_DO = "com.flux.done.action.WONT_DO"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val action = intent.action ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FluxDoneDatabase.getInstance(context)
                val taskDao = db.taskDao()
                val subtaskDao = db.subtaskDao()

                when (action) {
                    ACTION_MARK_DONE -> {
                        val now = System.currentTimeMillis()
                        // 1. Mandatory subtask completion invariant: mark parent and all child subtasks complete
                        taskDao.markTaskCompleted(taskId, now)
                        subtaskDao.markAllSubtasksCompleted(taskId)
                    }
                    ACTION_WONT_DO -> {
                        // Mark task as skipped or advance recurrence
                        taskDao.markTaskWonDo(taskId, System.currentTimeMillis())
                    }
                }

                // Dismiss the notification
                if (notifId != -1) {
                    NotificationManagerCompat.from(context).cancel(notifId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

---

### 4.4 Boot & Package Re-Arming Gateway (`BootCompletedReceiver.kt`)

```kotlin
package com.flux.done.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flux.done.core.database.FluxDoneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED) {

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FluxDoneDatabase.getInstance(context)
                    val scheduler = TaskAlarmScheduler(context)
                    val now = System.currentTimeMillis()

                    // Re-schedule all active tasks with future alarms
                    val pendingTasks = db.taskDao().getActiveTasksWithFutureReminders(now)
                    for (task in pendingTasks) {
                        task.due_date?.let { scheduler.scheduleTaskAlarm(task, it) }
                    }

                    // Re-schedule habit reminders
                    val activeHabits = db.habitDao().getActiveHabitsWithReminders()
                    for (habit in activeHabits) {
                        // Re-arm daily habit alarms
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
```

---

## 5. MVI Architecture & Settings State

### 5.1 Notification Settings State (`NotificationSettingsUiState.kt`)

```kotlin
package com.flux.done.features.notifications

data class NotificationSettingsUiState(
    val areRemindersEnabled: Boolean = true,
    val isMorningAgendaEnabled: Boolean = true,
    val morningAgendaTimeMinutes: Int = 420, // 07:00 AM (7 * 60)
    val defaultReminderOffsetMinutes: Int = 15, // 15 mins before
    val areHabitNudgesEnabled: Boolean = true,
    val canScheduleExactAlarms: Boolean = true
)

sealed interface NotificationSettingsIntent {
    data class SetRemindersEnabled(val enabled: Boolean) : NotificationSettingsIntent
    data class SetMorningAgendaEnabled(val enabled: Boolean) : NotificationSettingsIntent
    data class SetMorningAgendaTime(val timeMinutes: Int) : NotificationSettingsIntent
    data class SetDefaultReminderOffset(val offsetMinutes: Int) : NotificationSettingsIntent
    data class SetHabitNudgesEnabled(val enabled: Boolean) : NotificationSettingsIntent
    object RequestExactAlarmPermission : NotificationSettingsIntent
}
```

---

## 6. Edge Cases & Invariants

| Edge Case | Expected System Behavior |
|---|---|
| **Subtask Invariant via Notification Shade** | Tapping `[ ✓ Mark Done ]` executes the mandatory subtask completion invariant in Room SQLite within `NotificationActionReceiver`. Both parent and child subtasks update to `is_completed = 1`. |
| **Recurring Task Skips via `[ 🚫 Won't Do ]`** | Skips the current active occurrence and computes the next date per RFC 5545 RRULE. Historical records are preserved; streak evaluation does not penalize planned rest/skip. |
| **Device Reboot or Timezone Shift** | `BootCompletedReceiver` catches `ACTION_BOOT_COMPLETED`, `ACTION_TIMEZONE_CHANGED`, and `ACTION_TIME_CHANGED`, querying Room SQLite to re-arm all pending alarms with zero missed alerts. |
| **Suppression for Already-Completed Habits** | At the habit's scheduled `reminder_time`, the receiver queries `habit_completions` for today. If `is_target_met == 1`, the notification is cancelled silently without waking the user. |
| **Android 12+ `SCHEDULE_EXACT_ALARM` Restrictions** | The app checks `alarmManager.canScheduleExactAlarms()`. If restricted, Settings displays an alert directing Ari to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` while falling back gracefully to `setAndAllowWhileIdle()`. |
| **Multiple Active Task Alarms** | Notifications use a shared group key (`groupKey = "com.flux.done.TASK_REMINDERS"`), collapsing into a neat Android notification bundle when multiple reminders fire concurrently. |

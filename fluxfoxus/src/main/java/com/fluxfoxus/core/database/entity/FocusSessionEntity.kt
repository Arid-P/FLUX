package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Focus Session Entity
 * Matches Table: focus_sessions in TRD v2.0 Section 3.3
 */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["preset_id"]),
        Index(value = ["start_time"]),
        Index(value = ["status"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "session_origin", defaultValue = "'MANUAL'")
    val sessionOrigin: String = "MANUAL", // 'MANUAL', 'SCHEDULED', 'TASK_SYNC'

    @ColumnInfo(name = "preset_id")
    val presetId: String,

    @ColumnInfo(name = "session_title")
    val sessionTitle: String,

    @ColumnInfo(name = "linked_task_id")
    val linkedTaskId: String? = null,

    @ColumnInfo(name = "timer_mode")
    val timerMode: String = "COUNTDOWN", // 'COUNTDOWN', 'STOPWATCH', 'FROM_TO'

    @ColumnInfo(name = "start_time")
    val startTime: Long, // UTC epoch ms

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null, // Nullable UTC epoch ms

    @ColumnInfo(name = "actual_start_time")
    val actualStartTime: Long? = null,

    @ColumnInfo(name = "target_duration_minutes")
    val targetDurationMinutes: Int,

    @ColumnInfo(name = "actual_duration_seconds", defaultValue = "0")
    val actualDurationSeconds: Int = 0,

    @ColumnInfo(name = "breaks_taken", defaultValue = "0")
    val breaksTaken: Int = 0,

    @ColumnInfo(name = "breaks_total_seconds", defaultValue = "0")
    val breaksTotalSeconds: Int = 0,

    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String? = null,

    @ColumnInfo(name = "series_id")
    val seriesId: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "PLANNED", // 'PLANNED', 'ACTIVE', 'PAUSED_BREAK', 'COMPLETED', 'QUIT_PREMATURELY', 'SKIPPED', 'EXPIRED_ON_BOOT'

    @ColumnInfo(name = "reflection_rating")
    val reflectionRating: String? = null, // 'LOW', 'MEDIUM', 'HIGH', or NULL

    @ColumnInfo(name = "penalty_delay_applied_seconds", defaultValue = "0")
    val penaltyDelayAppliedSeconds: Int = 0,

    @ColumnInfo(name = "headsup_notification_armed", defaultValue = "1")
    val headsupNotificationArmed: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

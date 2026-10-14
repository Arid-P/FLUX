package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily Usage Cache Entity
 * Caches daily foreground screen time ingested from Android UsageStatsManager
 */
@Entity(
    tableName = "daily_usage",
    indices = [Index(value = ["date_iso", "package_name"], unique = true)]
)
data class DailyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date_iso")
    val dateIso: String, // Format: 'YYYY-MM-DD'

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "total_time_in_foreground_ms")
    val totalTimeInForegroundMs: Long,

    @ColumnInfo(name = "last_time_used_epoch_ms")
    val lastTimeUsedEpochMs: Long
)

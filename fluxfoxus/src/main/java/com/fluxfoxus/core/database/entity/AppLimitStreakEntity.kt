package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App Limit Streak Entity
 * Matches Table: app_limit_streaks in TRD v2.0 Section 3.6
 */
@Entity(tableName = "app_limit_streaks")
data class AppLimitStreakEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "current_streak", defaultValue = "0")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak", defaultValue = "0")
    val longestStreak: Int = 0,

    @ColumnInfo(name = "last_successful_date")
    val lastSuccessfulDate: String, // 'YYYY-MM-DD'

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

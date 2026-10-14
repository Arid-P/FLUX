package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App Limit Entity
 * Matches Table: app_limits in TRD v2.0 Section 3.4
 */
@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_label")
    val appLabel: String,

    @ColumnInfo(name = "daily_budget_minutes")
    val dailyBudgetMinutes: Int,

    @ColumnInfo(name = "extra_time_passes_used", defaultValue = "0")
    val extraTimePassesUsed: Int = 0, // Max 3 per day

    @ColumnInfo(name = "is_enabled", defaultValue = "1")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "limit_type", defaultValue = "'INDIVIDUAL'")
    val limitType: String = "INDIVIDUAL", // 'INDIVIDUAL' or 'CATEGORY_MEMBER'

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

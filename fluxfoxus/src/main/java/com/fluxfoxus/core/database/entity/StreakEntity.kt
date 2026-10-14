package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Focus Streak Record (Single-row table: id = 1)
 * Matches Table: streaks in TRD v2.0 Section 3.5
 */
@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val id: Long = 1L,

    @ColumnInfo(name = "current_streak", defaultValue = "0")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "best_streak", defaultValue = "0")
    val bestStreak: Int = 0,

    @ColumnInfo(name = "last_completed_date")
    val lastCompletedDate: String, // 'YYYY-MM-DD'

    @ColumnInfo(name = "relief_days_json", defaultValue = "'[\"SUNDAY\"]'")
    val reliefDaysJson: String = "[\"SUNDAY\"]",

    @ColumnInfo(name = "accumulated_grace_shields", defaultValue = "1")
    val accumulatedGraceShields: Int = 1,

    @ColumnInfo(name = "shields_used_lifetime", defaultValue = "0")
    val shieldsUsedLifetime: Int = 0,

    @ColumnInfo(name = "last_shield_granted_month", defaultValue = "'2026-10'")
    val lastShieldGrantedMonth: String = "2026-10",

    @ColumnInfo(name = "last_shield_used_date")
    val lastShieldUsedDate: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

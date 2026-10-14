package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Focus Preset Entity
 * Matches Table: presets in TRD v2.0 Section 3.1
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "emoji", defaultValue = "'⏳'")
    val emoji: String = "⏳",

    @ColumnInfo(name = "break_count", defaultValue = "2")
    val breakCount: Int = 2,

    @ColumnInfo(name = "break_duration_minutes", defaultValue = "5")
    val breakDurationMinutes: Int = 5,

    @ColumnInfo(name = "youtube_mode", defaultValue = "'STUDY_MODE'")
    val youtubeMode: String = "STUDY_MODE", // 'BLOCK', 'ALLOW', 'STUDY_MODE'

    @ColumnInfo(name = "browser_mode", defaultValue = "'STUDY_MODE'")
    val browserMode: String = "STUDY_MODE", // 'BLOCK', 'ALLOW', 'STUDY_MODE'

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

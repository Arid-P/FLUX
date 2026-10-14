package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Preset App Rule Entity
 * Matches Table: preset_app_rules in TRD v2.0 Section 3.2
 */
@Entity(
    tableName = "preset_app_rules",
    primaryKeys = ["preset_id", "package_name"],
    foreignKeys = [
        ForeignKey(
            entity = PresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["preset_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["preset_id"])
    ]
)
data class PresetAppRuleEntity(
    @ColumnInfo(name = "preset_id")
    val presetId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "rule_mode", defaultValue = "'BLOCKED'")
    val ruleMode: String = "BLOCKED" // 'BLOCKED', 'ALLOWED', 'STUDY_MODE'
)

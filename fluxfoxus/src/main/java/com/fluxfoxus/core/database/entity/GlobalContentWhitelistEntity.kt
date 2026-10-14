package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Global Content Whitelist Entity
 * Matches Table: global_content_whitelists in TRD v2.0 Section 3.8
 */
@Entity(tableName = "global_content_whitelists")
data class GlobalContentWhitelistEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "app_type")
    val appType: String, // 'YOUTUBE_CHANNEL' or 'BROWSER_DOMAIN'

    @ColumnInfo(name = "target_identifier")
    val targetIdentifier: String, // e.g. '@3blue1brown' or 'desmos.com'

    @ColumnInfo(name = "display_label")
    val displayLabel: String, // e.g. '3Blue1Brown' or 'Desmos Graphing'

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

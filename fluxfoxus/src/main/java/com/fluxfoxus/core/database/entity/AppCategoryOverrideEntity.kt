package com.fluxfoxus.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App Category Override Entity
 * Matches Table: app_category_overrides in TRD v2.0 Section 3.7
 */
@Entity(tableName = "app_category_overrides")
data class AppCategoryOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_label")
    val appLabel: String,

    @ColumnInfo(name = "category")
    val category: String, // 'PRODUCTIVE', 'SEMI_PRODUCTIVE', 'DISTRACTING', 'OTHERS'

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

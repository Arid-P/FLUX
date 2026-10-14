package com.fluxfoxus.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fluxfoxus.core.database.dao.AppLimitDao
import com.fluxfoxus.core.database.dao.DailyUsageDao
import com.fluxfoxus.core.database.dao.FocusSessionDao
import com.fluxfoxus.core.database.dao.PresetDao
import com.fluxfoxus.core.database.dao.StreakDao
import com.fluxfoxus.core.database.dao.WhitelistDao
import com.fluxfoxus.core.database.entity.AppCategoryOverrideEntity
import com.fluxfoxus.core.database.entity.AppLimitEntity
import com.fluxfoxus.core.database.entity.AppLimitStreakEntity
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.GlobalContentWhitelistEntity
import com.fluxfoxus.core.database.entity.PresetAppRuleEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.database.entity.StreakEntity

/**
 * FluxFoxus Core Room SQLite Database (Version 1)
 * Compliant with TRD v2.0 Section 3 schema specifications.
 */
@Database(
    entities = [
        PresetEntity::class,
        PresetAppRuleEntity::class,
        FocusSessionEntity::class,
        AppLimitEntity::class,
        StreakEntity::class,
        AppLimitStreakEntity::class,
        AppCategoryOverrideEntity::class,
        GlobalContentWhitelistEntity::class,
        DailyUsageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FluxFoxusDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun streakDao(): StreakDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun dailyUsageDao(): DailyUsageDao

    companion object {
        @Volatile
        private var INSTANCE: FluxFoxusDatabase? = null

        fun getDatabase(context: Context): FluxFoxusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FluxFoxusDatabase::class.java,
                    "fluxfoxus.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

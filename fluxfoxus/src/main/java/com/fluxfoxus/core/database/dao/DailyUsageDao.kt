package com.fluxfoxus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyUsage(items: List<DailyUsageEntity>)

    @Query("SELECT * FROM daily_usage WHERE date_iso = :dateIso ORDER BY total_time_in_foreground_ms DESC")
    fun getDailyUsageForDate(dateIso: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date_iso = :dateIso AND package_name = :packageName LIMIT 1")
    suspend fun getDailyUsageForPackage(dateIso: String, packageName: String): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE package_name = :packageName AND date_iso >= :startDateIso AND date_iso <= :endDateIso ORDER BY date_iso ASC")
    fun getDailyUsageForPackageInRange(packageName: String, startDateIso: String, endDateIso: String): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE date_iso >= :startDateIso AND date_iso <= :endDateIso ORDER BY date_iso ASC")
    fun getDailyUsageInRange(startDateIso: String, endDateIso: String): Flow<List<DailyUsageEntity>>
}

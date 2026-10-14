package com.fluxfoxus.core.database.dao

import androidx.room.*
import com.fluxfoxus.core.database.entity.AppCategoryOverrideEntity
import com.fluxfoxus.core.database.entity.AppLimitEntity
import com.fluxfoxus.core.database.entity.AppLimitStreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    // App Limits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: AppLimitEntity)

    @Update
    suspend fun updateLimit(limit: AppLimitEntity)

    @Query("SELECT * FROM app_limits ORDER BY app_label ASC")
    fun getAllLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE is_enabled = 1")
    fun getEnabledLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE package_name = :packageName LIMIT 1")
    suspend fun getLimitForPackage(packageName: String): AppLimitEntity?

    @Query("SELECT * FROM app_limits WHERE package_name = :packageName LIMIT 1")
    fun getLimitForPackageFlow(packageName: String): Flow<AppLimitEntity?>

    @Query("DELETE FROM app_limits WHERE package_name = :packageName")
    suspend fun deleteLimit(packageName: String)

    // App Limit Streaks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppLimitStreak(streak: AppLimitStreakEntity)

    @Query("SELECT * FROM app_limit_streaks WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppLimitStreak(packageName: String): AppLimitStreakEntity?

    @Query("SELECT * FROM app_limit_streaks")
    fun getAllAppLimitStreaks(): Flow<List<AppLimitStreakEntity>>

    // App Category Overrides
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryOverride(override: AppCategoryOverrideEntity)

    @Query("SELECT * FROM app_category_overrides WHERE package_name = :packageName LIMIT 1")
    suspend fun getCategoryOverride(packageName: String): AppCategoryOverrideEntity?

    @Query("SELECT * FROM app_category_overrides WHERE package_name = :packageName LIMIT 1")
    fun getCategoryOverrideFlow(packageName: String): Flow<AppCategoryOverrideEntity?>

    @Query("SELECT * FROM app_category_overrides")
    fun getAllCategoryOverrides(): Flow<List<AppCategoryOverrideEntity>>
}

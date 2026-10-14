package com.fluxfoxus.core.database.dao

import androidx.room.*
import com.fluxfoxus.core.database.entity.PresetAppRuleEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: String)

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): PresetEntity?

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    fun getPresetByIdFlow(id: String): Flow<PresetEntity?>

    @Query("SELECT * FROM presets ORDER BY created_at ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    // Preset App Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppRules(rules: List<PresetAppRuleEntity>)

    @Query("DELETE FROM preset_app_rules WHERE preset_id = :presetId")
    suspend fun deleteAppRulesForPreset(presetId: String)

    @Query("SELECT * FROM preset_app_rules WHERE preset_id = :presetId")
    fun getAppRulesForPreset(presetId: String): Flow<List<PresetAppRuleEntity>>

    @Query("SELECT * FROM preset_app_rules WHERE preset_id = :presetId AND package_name = :packageName LIMIT 1")
    suspend fun getAppRule(presetId: String, packageName: String): PresetAppRuleEntity?
}

package com.fluxfoxus.core.database.dao

import androidx.room.*
import com.fluxfoxus.core.database.entity.GlobalContentWhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWhitelistItem(item: GlobalContentWhitelistEntity)

    @Delete
    suspend fun deleteWhitelistItem(item: GlobalContentWhitelistEntity)

    @Query("DELETE FROM global_content_whitelists WHERE id = :id")
    suspend fun deleteWhitelistItemById(id: String)

    @Query("SELECT * FROM global_content_whitelists ORDER BY display_label ASC")
    fun getAllWhitelistItems(): Flow<List<GlobalContentWhitelistEntity>>

    @Query("SELECT * FROM global_content_whitelists WHERE app_type = :appType ORDER BY display_label ASC")
    fun getWhitelistItemsByType(appType: String): Flow<List<GlobalContentWhitelistEntity>>

    @Query("SELECT COUNT(*) > 0 FROM global_content_whitelists WHERE app_type = :appType AND target_identifier = :targetIdentifier")
    suspend fun isIdentifierWhitelisted(appType: String, targetIdentifier: String): Boolean
}

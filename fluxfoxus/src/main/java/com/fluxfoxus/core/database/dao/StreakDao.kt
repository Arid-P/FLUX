package com.fluxfoxus.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fluxfoxus.core.database.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: StreakEntity)

    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    suspend fun getStreak(): StreakEntity?

    @Query("SELECT * FROM streaks WHERE id = 1 LIMIT 1")
    fun getStreakFlow(): Flow<StreakEntity?>
}

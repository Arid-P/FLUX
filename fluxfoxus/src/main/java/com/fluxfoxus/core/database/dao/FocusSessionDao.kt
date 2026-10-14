package com.fluxfoxus.core.database.dao

import androidx.room.*
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    fun getSessionByIdFlow(id: String): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE status IN ('ACTIVE', 'PAUSED_BREAK') LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status IN ('ACTIVE', 'PAUSED_BREAK') LIMIT 1")
    fun getActiveSessionFlow(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE start_time >= :startEpochMs AND start_time < :endEpochMs ORDER BY start_time ASC")
    fun getSessionsInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    fun getCompletedSessionsCount(): Flow<Int>
}

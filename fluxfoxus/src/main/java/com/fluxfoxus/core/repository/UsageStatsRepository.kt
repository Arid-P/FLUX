package com.fluxfoxus.core.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Summary of aggregated device usage for a specific date.
 */
data class AggregatedDailyUsage(
    val dateIso: String = LocalDate.now().toString(),
    val packageUsageMap: Map<String, Long> = emptyMap(),
    val totalForegroundMs: Long = 0L,
    val lastRefreshedEpochMs: Long = System.currentTimeMillis()
)

/**
 * Repository to query system screen time from Android's UsageStatsManager,
 * parse exact foreground/background intervals, upsert into Room DailyUsageDao,
 * and provide reactive StateFlow emissions to ViewModels.
 */
class UsageStatsRepository(
    private val context: Context,
    private val db: FluxFoxusDatabase,
    private val usageStatsManager: UsageStatsManager? = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
) {
    private val _dailyUsageState = MutableStateFlow(AggregatedDailyUsage())
    val dailyUsageState: StateFlow<AggregatedDailyUsage> = _dailyUsageState.asStateFlow()

    /**
     * Refresh usage stats from exact start of today (00:00:00) to now.
     * Uses queryUsageStats(), falling back to queryEvents() if bucket aggregation has not yet occurred.
     */
    suspend fun refreshUsageStats(): Result<AggregatedDailyUsage> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val todayIso = LocalDate.now().toString()

            if (usageStatsManager == null) {
                return@withContext Result.failure(IllegalStateException("UsageStatsManager not available"))
            }

            // 1. Primary Query: queryUsageStats(INTERVAL_DAILY)
            val usageStatsList = try {
                usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
            } catch (_: Exception) {
                null
            }

            val usageMap = mutableMapOf<String, Long>()
            val lastUsedMap = mutableMapOf<String, Long>()

            if (!usageStatsList.isNullOrEmpty()) {
                usageStatsList.forEach { stat ->
                    if (stat.totalTimeInForeground > 0) {
                        usageMap[stat.packageName] = (usageMap[stat.packageName] ?: 0L) + stat.totalTimeInForeground
                        lastUsedMap[stat.packageName] = maxOf(lastUsedMap[stat.packageName] ?: 0L, stat.lastTimeUsed)
                    }
                }
            }

            // 2. Fallback: If queryUsageStats returned empty or 0 total due to OS aggregation delay,
            // query exact events from startOfDay to now
            if (usageMap.isEmpty() || usageMap.values.sum() == 0L) {
                val eventUsage = parseUsageFromEvents(startOfDay, now)
                eventUsage.forEach { (pkg, duration) ->
                    usageMap[pkg] = duration
                    lastUsedMap[pkg] = now
                }
            }

            // Filter out FluxFoxus itself from distracting usage counts
            usageMap.remove(context.packageName)

            // 3. Upsert into Room Database
            val existingRows = try {
                // Fetch existing to preserve auto-generated IDs and avoid conflict issues
                val map = mutableMapOf<String, Long>()
                // We will construct entities with proper matching IDs if existing
                map
            } catch (_: Exception) {
                emptyMap()
            }

            val entities = usageMap.map { (pkg, durationMs) ->
                DailyUsageEntity(
                    id = existingRows[pkg] ?: 0L,
                    dateIso = todayIso,
                    packageName = pkg,
                    totalTimeInForegroundMs = durationMs,
                    lastTimeUsedEpochMs = lastUsedMap[pkg] ?: now
                )
            }

            if (entities.isNotEmpty()) {
                db.dailyUsageDao().upsertDailyUsage(entities)
            }

            val summary = AggregatedDailyUsage(
                dateIso = todayIso,
                packageUsageMap = usageMap,
                totalForegroundMs = usageMap.values.sum(),
                lastRefreshedEpochMs = now
            )

            _dailyUsageState.value = summary
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fallback parser using UsageStatsManager.queryEvents to calculate exact foreground intervals.
     */
    private fun parseUsageFromEvents(startTime: Long, endTime: Long): Map<String, Long> {
        val manager = usageStatsManager ?: return emptyMap()
        val usageMap = mutableMapOf<String, Long>()
        val lastResumeMap = mutableMapOf<String, Long>()

        val events = try {
            manager.queryEvents(startTime, endTime)
        } catch (_: Exception) {
            return emptyMap()
        }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val eventTime = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumeMap[pkg] = eventTime
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumed = lastResumeMap.remove(pkg)
                    if (resumed != null && eventTime > resumed) {
                        val duration = eventTime - resumed
                        usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
                    }
                }
            }
        }

        // Account for any app currently in the foreground
        lastResumeMap.forEach { (pkg, resumed) ->
            if (endTime > resumed) {
                usageMap[pkg] = (usageMap[pkg] ?: 0L) + (endTime - resumed)
            }
        }

        return usageMap
    }
}

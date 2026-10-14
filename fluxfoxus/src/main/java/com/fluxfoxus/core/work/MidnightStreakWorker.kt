package com.fluxfoxus.core.work

import android.content.Context
import androidx.work.*
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.AppLimitStreakEntity
import com.fluxfoxus.core.database.entity.StreakEntity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MidnightStreakWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = FluxFoxusDatabase.getDatabase(applicationContext)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDateStr = dateFormat.format(cal.time)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1

        val isReliefDay = dayOfWeek == Calendar.SUNDAY

        // 1. Evaluate Macro Focus Streak
        val allSessions = db.focusSessionDao().getAllSessions().first()
        val completedYesterday = allSessions.filter {
            it.status == "COMPLETED" && dateFormat.format(Date(it.startTime)) == yesterdayDateStr
        }

        val streak = db.streakDao().getStreak() ?: StreakEntity(lastCompletedDate = yesterdayDateStr)

        val updatedStreak = if (completedYesterday.isNotEmpty()) {
            val newCurrent = streak.currentStreak + 1
            streak.copy(
                currentStreak = newCurrent,
                bestStreak = maxOf(streak.bestStreak, newCurrent),
                lastCompletedDate = yesterdayDateStr,
                updatedAt = System.currentTimeMillis()
            )
        } else if (isReliefDay) {
            // Sunday relief day: preserve streak without reset
            streak.copy(
                lastCompletedDate = yesterdayDateStr,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // Target missed on active work day: reset streak to 0
            streak.copy(
                currentStreak = 0,
                updatedAt = System.currentTimeMillis()
            )
        }
        db.streakDao().upsertStreak(updatedStreak)

        // 2. Evaluate Micro App Limit Streaks
        val limits = db.appLimitDao().getAllLimits().first()
        limits.forEach { limit ->
            if (limit.isEnabled) {
                val usage = db.dailyUsageDao().getDailyUsageForPackage(yesterdayDateStr, limit.packageName)
                val spentMins = if (usage != null) (usage.totalTimeInForegroundMs / 60000L).toInt() else 0

                val appStreak = db.appLimitDao().getAppLimitStreak(limit.packageName)
                    ?: AppLimitStreakEntity(
                        packageName = limit.packageName,
                        currentStreak = 0,
                        longestStreak = 0,
                        lastSuccessfulDate = yesterdayDateStr
                    )

                val updatedAppStreak = if (spentMins <= limit.dailyBudgetMinutes) {
                    val nextStreak = appStreak.currentStreak + 1
                    appStreak.copy(
                        currentStreak = nextStreak,
                        longestStreak = maxOf(appStreak.longestStreak, nextStreak),
                        lastSuccessfulDate = yesterdayDateStr,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    appStreak.copy(
                        currentStreak = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                db.appLimitDao().upsertAppLimitStreak(updatedAppStreak)
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "MidnightStreakWorker"

        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5) // Run 5 mins past midnight
                set(Calendar.SECOND, 0)
            }
            val initialDelayMs = midnight.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<MidnightStreakWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

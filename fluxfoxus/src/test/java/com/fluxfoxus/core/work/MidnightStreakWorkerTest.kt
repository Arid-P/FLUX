package com.fluxfoxus.core.work

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MidnightStreakWorkerTest {

    private lateinit var context: Context
    private lateinit var db: FluxFoxusDatabase
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        FluxFoxusDatabase.setDatabaseForTesting(db)
    }

    @After
    fun tearDown() {
        FluxFoxusDatabase.setDatabaseForTesting(null)
        db.close()
    }

    @Test
    fun testCompletedSessionYesterdayAdvancesStreak() = runBlocking {
        // Initial streak = 5
        db.streakDao().upsertStreak(
            StreakEntity(
                id = 1L,
                currentStreak = 5,
                bestStreak = 10,
                lastCompletedDate = "2026-10-12"
            )
        )

        // Preset
        db.presetDao().insertPreset(
            PresetEntity(id = "p1", name = "Math")
        )

        // Session completed yesterday
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(cal.time)

        db.focusSessionDao().insertSession(
            FocusSessionEntity(
                id = "s1",
                presetId = "p1",
                sessionTitle = "Math Exam",
                startTime = cal.timeInMillis,
                targetDurationMinutes = 50,
                status = "COMPLETED"
            )
        )

        val worker = TestListenableWorkerBuilder<MidnightStreakWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        val streak = db.streakDao().getStreak()
        assertNotNull(streak)
        assertEquals(6, streak!!.currentStreak)
        assertEquals(yesterdayStr, streak.lastCompletedDate)
    }

    @Test
    fun testMissedDayResetsStreakUnlessSunday() = runBlocking {
        // Initial streak = 18
        db.streakDao().upsertStreak(
            StreakEntity(
                id = 1L,
                currentStreak = 18,
                bestStreak = 20,
                lastCompletedDate = "2026-10-12"
            )
        )

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        val worker = TestListenableWorkerBuilder<MidnightStreakWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        val streak = db.streakDao().getStreak()
        assertNotNull(streak)
        if (isSunday) {
            // Sunday relief day: preserved!
            assertEquals(18, streak!!.currentStreak)
        } else {
            // Missed normal day: reset to 0
            assertEquals(0, streak!!.currentStreak)
        }
    }

    @Test
    fun testAppLimitStreakEvaluation() = runBlocking {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(cal.time)

        // Limit: 45 min
        db.appLimitDao().upsertLimit(
            AppLimitEntity(
                packageName = "com.instagram.android",
                appLabel = "Instagram",
                dailyBudgetMinutes = 45,
                isEnabled = true
            )
        )

        // Initial app streak = 3
        db.appLimitDao().upsertAppLimitStreak(
            AppLimitStreakEntity(
                packageName = "com.instagram.android",
                currentStreak = 3,
                longestStreak = 5,
                lastSuccessfulDate = "2026-10-12"
            )
        )

        // Usage yesterday: 30 min (under budget)
        db.dailyUsageDao().upsertDailyUsage(
            listOf(
                DailyUsageEntity(
                    dateIso = yesterdayStr,
                    packageName = "com.instagram.android",
                    totalTimeInForegroundMs = 1800000L, // 30m
                    lastTimeUsedEpochMs = cal.timeInMillis
                )
            )
        )

        val worker = TestListenableWorkerBuilder<MidnightStreakWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        val appStreak = db.appLimitDao().getAppLimitStreak("com.instagram.android")
        assertNotNull(appStreak)
        assertEquals(4, appStreak!!.currentStreak)
    }
}

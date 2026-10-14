package com.fluxfoxus.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fluxfoxus.core.database.dao.*
import com.fluxfoxus.core.database.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FluxFoxusDatabaseTest {

    private lateinit var db: FluxFoxusDatabase
    private lateinit var presetDao: PresetDao
    private lateinit var sessionDao: FocusSessionDao
    private lateinit var limitDao: AppLimitDao
    private lateinit var streakDao: StreakDao
    private lateinit var whitelistDao: WhitelistDao
    private lateinit var usageDao: DailyUsageDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        presetDao = db.presetDao()
        sessionDao = db.focusSessionDao()
        limitDao = db.appLimitDao()
        streakDao = db.streakDao()
        whitelistDao = db.whitelistDao()
        usageDao = db.dailyUsageDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testPresetInsertAndAppRuleCascade() = runBlocking {
        val preset = PresetEntity(
            id = "preset_deep_work",
            name = "Deep Work",
            emoji = "🧠",
            breakCount = 2,
            breakDurationMinutes = 5,
            youtubeMode = "STUDY_MODE",
            browserMode = "STUDY_MODE"
        )
        presetDao.insertPreset(preset)

        val retrievedPreset = presetDao.getPresetById("preset_deep_work")
        assertNotNull(retrievedPreset)
        assertEquals("Deep Work", retrievedPreset?.name)
        assertEquals(2, retrievedPreset?.breakCount)

        // Insert App Rules
        val rules = listOf(
            PresetAppRuleEntity(presetId = "preset_deep_work", packageName = "com.instagram.android", ruleMode = "BLOCKED"),
            PresetAppRuleEntity(presetId = "preset_deep_work", packageName = "com.twitter.android", ruleMode = "BLOCKED")
        )
        presetDao.insertAppRules(rules)

        val retrievedRules = presetDao.getAppRulesForPreset("preset_deep_work").first()
        assertEquals(2, retrievedRules.size)

        // Insert Focus Session referencing preset
        val session = FocusSessionEntity(
            id = "session_001",
            presetId = "preset_deep_work",
            sessionTitle = "Olympiad Math Deep Work",
            timerMode = "COUNTDOWN",
            startTime = 1700000000000L,
            targetDurationMinutes = 60,
            status = "ACTIVE"
        )
        sessionDao.insertSession(session)

        val activeSession = sessionDao.getActiveSession()
        assertNotNull(activeSession)
        assertEquals("session_001", activeSession?.id)

        // Delete Preset -> Cascades to preset_app_rules and focus_sessions
        presetDao.deletePresetById("preset_deep_work")

        assertNull(presetDao.getPresetById("preset_deep_work"))
        val emptyRules = presetDao.getAppRulesForPreset("preset_deep_work").first()
        assertTrue(emptyRules.isEmpty())
        assertNull(sessionDao.getSessionById("session_001"))
    }

    @Test
    fun testFocusSessionRangeAndActiveQuery() = runBlocking {
        val preset = PresetEntity(id = "p1", name = "Coding")
        presetDao.insertPreset(preset)

        val s1 = FocusSessionEntity(
            id = "s1",
            presetId = "p1",
            sessionTitle = "Kotlin Engine",
            timerMode = "COUNTDOWN",
            startTime = 1000L,
            targetDurationMinutes = 45,
            status = "COMPLETED"
        )
        val s2 = FocusSessionEntity(
            id = "s2",
            presetId = "p1",
            sessionTitle = "UI Review",
            timerMode = "STOPWATCH",
            startTime = 5000L,
            targetDurationMinutes = 20,
            status = "ACTIVE"
        )
        sessionDao.insertSession(s1)
        sessionDao.insertSession(s2)

        val active = sessionDao.getActiveSession()
        assertNotNull(active)
        assertEquals("s2", active?.id)

        val inRange = sessionDao.getSessionsInRange(0L, 6000L).first()
        assertEquals(2, inRange.size)

        val completedCount = sessionDao.getCompletedSessionsCount().first()
        assertEquals(1, completedCount)
    }

    @Test
    fun testAppLimitAndStreakOperations() = runBlocking {
        val limit = AppLimitEntity(
            packageName = "com.reddit.frontpage",
            appLabel = "Reddit",
            dailyBudgetMinutes = 30,
            extraTimePassesUsed = 1,
            isEnabled = true
        )
        limitDao.upsertLimit(limit)

        val retrievedLimit = limitDao.getLimitForPackage("com.reddit.frontpage")
        assertNotNull(retrievedLimit)
        assertEquals(30, retrievedLimit?.dailyBudgetMinutes)
        assertEquals(1, retrievedLimit?.extraTimePassesUsed)

        // App Limit Streak
        val streak = AppLimitStreakEntity(
            packageName = "com.reddit.frontpage",
            currentStreak = 7,
            longestStreak = 14,
            lastSuccessfulDate = "2026-10-13"
        )
        limitDao.upsertAppLimitStreak(streak)

        val retrievedStreak = limitDao.getAppLimitStreak("com.reddit.frontpage")
        assertNotNull(retrievedStreak)
        assertEquals(7, retrievedStreak?.currentStreak)

        // Category Override
        val override = AppCategoryOverrideEntity(
            packageName = "com.reddit.frontpage",
            appLabel = "Reddit",
            category = "DISTRACTING"
        )
        limitDao.upsertCategoryOverride(override)

        val retrievedOverride = limitDao.getCategoryOverride("com.reddit.frontpage")
        assertNotNull(retrievedOverride)
        assertEquals("DISTRACTING", retrievedOverride?.category)
    }

    @Test
    fun testStreakSingleRowRecord() = runBlocking {
        val streak = StreakEntity(
            id = 1L,
            currentStreak = 18,
            bestStreak = 24,
            lastCompletedDate = "2026-10-13",
            reliefDaysJson = "[\"SUNDAY\"]",
            accumulatedGraceShields = 2
        )
        streakDao.upsertStreak(streak)

        val retrieved = streakDao.getStreak()
        assertNotNull(retrieved)
        assertEquals(18, retrieved?.currentStreak)
        assertEquals("[\"SUNDAY\"]", retrieved?.reliefDaysJson)
        assertEquals(2, retrieved?.accumulatedGraceShields)
    }

    @Test
    fun testGlobalContentWhitelist() = runBlocking {
        val youtubeChannel = GlobalContentWhitelistEntity(
            id = "yt_3b1b",
            appType = "YOUTUBE_CHANNEL",
            targetIdentifier = "@3blue1brown",
            displayLabel = "3Blue1Brown"
        )
        val browserDomain = GlobalContentWhitelistEntity(
            id = "web_desmos",
            appType = "BROWSER_DOMAIN",
            targetIdentifier = "desmos.com",
            displayLabel = "Desmos Graphing"
        )
        whitelistDao.upsertWhitelistItem(youtubeChannel)
        whitelistDao.upsertWhitelistItem(browserDomain)

        assertTrue(whitelistDao.isIdentifierWhitelisted("YOUTUBE_CHANNEL", "@3blue1brown"))
        assertTrue(whitelistDao.isIdentifierWhitelisted("BROWSER_DOMAIN", "desmos.com"))
        assertFalse(whitelistDao.isIdentifierWhitelisted("YOUTUBE_CHANNEL", "@distraction_channel"))

        val youtubeList = whitelistDao.getWhitelistItemsByType("YOUTUBE_CHANNEL").first()
        assertEquals(1, youtubeList.size)
        assertEquals("3Blue1Brown", youtubeList[0].displayLabel)
    }

    @Test
    fun testDailyUsageCaching() = runBlocking {
        val usageList = listOf(
            DailyUsageEntity(
                dateIso = "2026-10-14",
                packageName = "com.fluxfoxus",
                totalTimeInForegroundMs = 3600000L,
                lastTimeUsedEpochMs = System.currentTimeMillis()
            ),
            DailyUsageEntity(
                dateIso = "2026-10-14",
                packageName = "com.instagram.android",
                totalTimeInForegroundMs = 900000L,
                lastTimeUsedEpochMs = System.currentTimeMillis()
            )
        )
        usageDao.upsertDailyUsage(usageList)

        val retrievedList = usageDao.getDailyUsageForDate("2026-10-14").first()
        assertEquals(2, retrievedList.size)
        assertEquals("com.fluxfoxus", retrievedList[0].packageName)
    }
}

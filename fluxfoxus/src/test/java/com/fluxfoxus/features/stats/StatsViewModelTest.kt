package com.fluxfoxus.features.stats

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class StatsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FluxFoxusDatabase
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        viewModel = StatsViewModel(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testModeSwitching() {
        assertEquals(StatsTabMode.DAILY, viewModel.uiState.value.mode)
        viewModel.setMode(StatsTabMode.WEEKLY)
        assertEquals(StatsTabMode.WEEKLY, viewModel.uiState.value.mode)
        viewModel.setMode(StatsTabMode.DAILY)
        assertEquals(StatsTabMode.DAILY, viewModel.uiState.value.mode)
    }

    @Test
    fun testCategoryFilterToggle() {
        assertNull(viewModel.uiState.value.selectedCategoryFilter)
        viewModel.toggleCategoryFilter("PRODUCTIVE")
        assertEquals("PRODUCTIVE", viewModel.uiState.value.selectedCategoryFilter)
        viewModel.toggleCategoryFilter("PRODUCTIVE")
        assertNull(viewModel.uiState.value.selectedCategoryFilter)
    }

    @Test
    fun testAppCategoryUpdate() = runTest(testDispatcher) {
        viewModel.updateAppCategory("com.notewise.app", "Notewise", "PRODUCTIVE")
        testScheduler.advanceUntilIdle()

        val override = db.appLimitDao().getCategoryOverride("com.notewise.app")
        assertNotNull(override)
        assertEquals("PRODUCTIVE", override!!.category)
    }

    @Test
    fun testFocusRatioCalculation() = runTest(testDispatcher) {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Insert preset
        db.presetDao().insertPreset(
            PresetEntity(
                id = "p1",
                name = "Math",
                breakCount = 1,
                breakDurationMinutes = 5
            )
        )

        // Insert completed focus session: 60 minutes
        db.focusSessionDao().insertSession(
            FocusSessionEntity(
                id = "s1",
                presetId = "p1",
                sessionTitle = "Math Study",
                startTime = System.currentTimeMillis() - 3600000L,
                endTime = System.currentTimeMillis(),
                targetDurationMinutes = 60,
                actualDurationSeconds = 3600,
                status = "COMPLETED"
            )
        )

        // Insert daily usage: 90 minutes total screen time
        db.dailyUsageDao().upsertDailyUsage(
            listOf(
                DailyUsageEntity(
                    dateIso = todayIso,
                    packageName = "com.notewise.app",
                    totalTimeInForegroundMs = 5400000L, // 90 min
                    lastTimeUsedEpochMs = System.currentTimeMillis()
                )
            )
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(60, state.focusMinutesToday)
        assertEquals(30, state.casualMinutesToday)
        assertEquals(2.0f, state.focusRatio, 0.1f)
        assertEquals("Deep Work", state.disciplineTierName)
    }
}

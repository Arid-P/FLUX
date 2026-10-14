package com.fluxfoxus.features.limits

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fluxfoxus.core.database.FluxFoxusDatabase
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LimitsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FluxFoxusDatabase
    private lateinit var viewModel: LimitsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        viewModel = LimitsViewModel(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testSaveLimitAndObserve() = runTest(testDispatcher) {
        viewModel.saveLimit(
            packageName = "com.instagram.android",
            appLabel = "Instagram",
            dailyBudgetMinutes = 45,
            extraPasses = 2,
            category = "DISTRACTING"
        )
        testScheduler.advanceUntilIdle()

        val limits = db.appLimitDao().getAllLimits().first()
        assertEquals(1, limits.size)
        assertEquals("com.instagram.android", limits[0].packageName)
        assertEquals(45, limits[0].dailyBudgetMinutes)
        assertEquals(2, limits[0].extraTimePassesUsed)
        assertTrue(limits[0].isEnabled)

        val overrides = db.appLimitDao().getAllCategoryOverrides().first()
        assertEquals(1, overrides.size)
        assertEquals("DISTRACTING", overrides[0].category)
    }

    @Test
    fun testDisableLimit() = runTest(testDispatcher) {
        viewModel.saveLimit(
            packageName = "com.reddit.frontpage",
            appLabel = "Reddit",
            dailyBudgetMinutes = 30,
            category = "DISTRACTING"
        )
        testScheduler.advanceUntilIdle()

        viewModel.confirmTurnOffLimit("com.reddit.frontpage", "REST_OF_DAY")
        testScheduler.advanceUntilIdle()

        val limit = db.appLimitDao().getLimitForPackage("com.reddit.frontpage")
        assertNotNull(limit)
        assertFalse(limit!!.isEnabled)
    }

    @Test
    fun testEditorVisibilityToggle() {
        assertFalse(viewModel.uiState.value.isEditorVisible)
        viewModel.openEditor()
        assertTrue(viewModel.uiState.value.isEditorVisible)
        viewModel.closeEditor()
        assertFalse(viewModel.uiState.value.isEditorVisible)
    }
}

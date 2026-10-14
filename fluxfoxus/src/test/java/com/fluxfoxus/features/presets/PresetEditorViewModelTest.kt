package com.fluxfoxus.features.presets

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
class PresetEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: FluxFoxusDatabase
    private lateinit var viewModel: PresetEditorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        viewModel = PresetEditorViewModel(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testCreateAndSavePreset() = runTest(testDispatcher) {
        viewModel.setName("Olympiad Physics")
        viewModel.setEmoji("🔬")
        viewModel.setBreakCount(3)
        viewModel.setBreakDuration(10)
        viewModel.setYoutubeMode("STUDY_MODE")
        viewModel.setBrowserMode("BLOCK")
        viewModel.setDescription("Daily physics problem set")
        viewModel.toggleAppBlocked("com.instagram.android")

        var saved = false
        viewModel.savePreset { saved = true }
        testScheduler.advanceUntilIdle()

        assertTrue(saved)

        val presets = db.presetDao().getAllPresets().first()
        assertEquals(1, presets.size)
        val p = presets[0]
        assertEquals("Olympiad Physics", p.name)
        assertEquals("🔬", p.emoji)
        assertEquals(3, p.breakCount)
        assertEquals(10, p.breakDurationMinutes)
        assertEquals("STUDY_MODE", p.youtubeMode)
        assertEquals("BLOCK", p.browserMode)

        val rules = db.presetDao().getAppRulesForPreset(p.id).first()
        assertEquals(1, rules.size)
        assertEquals("com.instagram.android", rules[0].packageName)
    }

    @Test
    fun testAddWhitelistItem() = runTest(testDispatcher) {
        viewModel.addWhitelistItem(
            type = "YOUTUBE",
            targetIdentifier = "@3blue1brown",
            displayLabel = "3Blue1Brown"
        )
        testScheduler.advanceUntilIdle()

        val items = db.whitelistDao().getAllWhitelistItems().first()
        assertEquals(1, items.size)
        assertEquals("@3blue1brown", items[0].targetIdentifier)
        assertEquals("YOUTUBE", items[0].appType)
    }
}

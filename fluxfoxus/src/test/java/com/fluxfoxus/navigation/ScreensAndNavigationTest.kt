package com.fluxfoxus.navigation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.designsystem.theme.FluxFoxusTheme
import com.fluxfoxus.features.limits.LimitsScreen
import com.fluxfoxus.features.limits.LimitsViewModel
import com.fluxfoxus.features.onboarding.OnboardingWizardScreen
import com.fluxfoxus.features.presets.PresetEditorScreen
import com.fluxfoxus.features.presets.PresetEditorViewModel
import com.fluxfoxus.features.stats.StatsScreen
import com.fluxfoxus.features.stats.StatsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ScreensAndNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: FluxFoxusDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FluxFoxusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        FluxFoxusDatabase.setDatabaseForTesting(db)
    }

    @After
    fun tearDown() {
        FluxFoxusDatabase.setDatabaseForTesting(null)
        db.close()
    }

    @Test
    fun testLimitsScreenRenders() {
        val vm = LimitsViewModel(db)
        composeTestRule.setContent {
            FluxFoxusTheme {
                LimitsScreen(viewModel = vm)
            }
        }
        composeTestRule.onNodeWithText("LIMITS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_limit_button").assertIsDisplayed()
    }

    @Test
    fun testStatsScreenRenders() {
        val vm = StatsViewModel(db)
        composeTestRule.setContent {
            FluxFoxusTheme {
                StatsScreen(viewModel = vm)
            }
        }
        composeTestRule.onNodeWithText("STATS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly").assertIsDisplayed()
    }

    @Test
    fun testPresetEditorScreenRenders() {
        val vm = PresetEditorViewModel(db)
        composeTestRule.setContent {
            FluxFoxusTheme {
                PresetEditorScreen(viewModel = vm, onBack = {}, onSaved = {})
            }
        }
        composeTestRule.onNodeWithText("New Preset").assertIsDisplayed()
        composeTestRule.onNodeWithTag("preset_name_input").assertIsDisplayed()
    }

    @Test
    fun testOnboardingWizardScreenRenders() {
        composeTestRule.setContent {
            FluxFoxusTheme {
                OnboardingWizardScreen(onFinishOnboarding = {})
            }
        }
        composeTestRule.onNodeWithText("Forge Unbreakable Focus").assertIsDisplayed()
        composeTestRule.onNodeWithTag("configure_shields_button").assertIsDisplayed()
    }
}

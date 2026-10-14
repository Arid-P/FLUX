package com.fluxfoxus.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.fluxfoxus.core.designsystem.theme.FluxFoxusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FlipClockAndModalTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testFlipClockRendersDigits() {
        composeTestRule.setContent {
            FluxFoxusTheme {
                FlipClock(
                    hours = "02",
                    minutes = "45",
                    isBreakMode = false
                )
            }
        }

        // Verify digits are displayed (split flap cards render top and bottom halves)
        composeTestRule.onAllNodesWithText("02").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("45").onFirst().assertIsDisplayed()
    }

    @Test
    fun testStopFocusingModalAsymmetricButtons() {
        var keepGoingClicked = false
        var confirmQuitClicked = false

        composeTestRule.setContent {
            FluxFoxusTheme {
                StopFocusingModal(
                    currentStreakDays = 18,
                    distractionMinutesToday = 0,
                    onKeepGoing = { keepGoingClicked = true },
                    onConfirmQuit = { confirmQuitClicked = true }
                )
            }
        }

        // Verify modal text
        composeTestRule.onNodeWithText("Surrender Focus?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Going").assertIsDisplayed()

        // Keep Going is always free and interactive
        composeTestRule.onNodeWithText("Keep Going").performClick()
        assertTrue(keepGoingClicked)
    }
}

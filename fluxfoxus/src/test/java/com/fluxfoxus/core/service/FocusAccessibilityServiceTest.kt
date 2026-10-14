package com.fluxfoxus.core.service

import com.fluxfoxus.core.database.entity.GlobalContentWhitelistEntity
import org.junit.Assert.*
import org.junit.Test

class FocusAccessibilityServiceTest {

    @Test
    fun testYouTubeChannelWhitelisting() {
        val whitelists = listOf(
            GlobalContentWhitelistEntity(
                id = "yt_1",
                appType = "YOUTUBE_CHANNEL",
                targetIdentifier = "@3blue1brown",
                displayLabel = "3Blue1Brown"
            ),
            GlobalContentWhitelistEntity(
                id = "yt_2",
                appType = "YOUTUBE_CHANNEL",
                targetIdentifier = "@mitocw",
                displayLabel = "MIT OpenCourseWare"
            )
        )

        // Matching screen content containing channel handle
        val onScreenContentValid = listOf("Home", "Subscriptions", "@3blue1brown", "Linear Algebra Essence", "1.2M views")
        assertTrue(FocusAccessibilityService.isContentWhitelisted(onScreenContentValid, whitelists))

        // Matching screen content containing display label
        val onScreenContentLabel = listOf("Video player", "3Blue1Brown", "Subscribe")
        assertTrue(FocusAccessibilityService.isContentWhitelisted(onScreenContentLabel, whitelists))

        // Distracting unwhitelisted channel
        val onScreenContentBlocked = listOf("Trending", "Gaming Channel", "Funny Clips Compilation 2026")
        assertFalse(FocusAccessibilityService.isContentWhitelisted(onScreenContentBlocked, whitelists))
    }

    @Test
    fun testBrowserDomainWhitelisting() {
        val whitelists = listOf(
            GlobalContentWhitelistEntity(
                id = "web_1",
                appType = "BROWSER_DOMAIN",
                targetIdentifier = "desmos.com",
                displayLabel = "Desmos Calculator"
            ),
            GlobalContentWhitelistEntity(
                id = "web_2",
                appType = "BROWSER_DOMAIN",
                targetIdentifier = "wikipedia.org",
                displayLabel = "Wikipedia"
            )
        )

        // Valid domain in URL bar
        val chromeUrlValid = listOf("desmos.com/calculator", "Graphing Calculator")
        assertTrue(FocusAccessibilityService.isContentWhitelisted(chromeUrlValid, whitelists))

        // Distracting unwhitelisted website
        val chromeUrlBlocked = listOf("reddit.com/r/all", "Popular discussions")
        assertFalse(FocusAccessibilityService.isContentWhitelisted(chromeUrlBlocked, whitelists))
    }

    @Test
    fun testSystemPackagesAreExempt() {
        assertTrue(FocusAccessibilityService.SYSTEM_PACKAGES.contains("com.fluxfoxus"))
        assertTrue(FocusAccessibilityService.SYSTEM_PACKAGES.contains("com.android.systemui"))
        assertTrue(FocusAccessibilityService.SYSTEM_PACKAGES.contains("com.android.launcher"))
        assertFalse(FocusAccessibilityService.SYSTEM_PACKAGES.contains("com.instagram.android"))
    }
}

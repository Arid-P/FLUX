package com.fluxfoxus.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UserPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun testUserPreferencesReadWrite() = runBlocking {
        val testFile = tmpFolder.newFile("test_user_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create { testFile }
        val repo = UserPreferencesRepository(dataStore)

        // Default values
        assertNull(repo.lastUsedPresetId.first())
        assertTrue(repo.soundChimeEnabled.first())
        assertTrue(repo.hapticEnabled.first())
        assertEquals(5, repo.pauseLimitMinutes.first())
        assertFalse(repo.onboardingCompleted.first())

        // Mutate values
        repo.setLastUsedPresetId("preset_deep_work")
        repo.setSoundChimeEnabled(false)
        repo.setHapticEnabled(false)
        repo.setPauseLimitMinutes(3)
        repo.setOnboardingCompleted(true)

        // Verify updated values
        assertEquals("preset_deep_work", repo.lastUsedPresetId.first())
        assertFalse(repo.soundChimeEnabled.first())
        assertFalse(repo.hapticEnabled.first())
        assertEquals(3, repo.pauseLimitMinutes.first())
        assertTrue(repo.onboardingCompleted.first())
    }
}

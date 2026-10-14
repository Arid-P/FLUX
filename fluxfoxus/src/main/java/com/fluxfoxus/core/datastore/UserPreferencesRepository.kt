package com.fluxfoxus.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fluxfoxus_preferences")

/**
 * User Preferences Repository backed by Android Jetpack DataStore
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_LAST_USED_PRESET_ID = stringPreferencesKey("last_used_preset_id")
        val KEY_SOUND_CHIME_ENABLED = booleanPreferencesKey("sound_chime_enabled")
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val KEY_PAUSE_LIMIT_MINUTES = intPreferencesKey("pause_limit_minutes")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val lastUsedPresetId: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_LAST_USED_PRESET_ID] }

    suspend fun setLastUsedPresetId(presetId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_USED_PRESET_ID] = presetId
        }
    }

    val soundChimeEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_SOUND_CHIME_ENABLED] ?: true }

    suspend fun setSoundChimeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SOUND_CHIME_ENABLED] = enabled
        }
    }

    val hapticEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_HAPTIC_ENABLED] ?: true }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAPTIC_ENABLED] = enabled
        }
    }

    val pauseLimitMinutes: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_PAUSE_LIMIT_MINUTES] ?: 5 }

    suspend fun setPauseLimitMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_PAUSE_LIMIT_MINUTES] = minutes
        }
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[KEY_ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}

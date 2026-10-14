package com.fluxfoxus.features.focus

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.database.entity.StreakEntity
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.service.FocusTimerService
import com.fluxfoxus.core.service.TimerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class FocusUiState(
    val presets: List<PresetEntity> = emptyList(),
    val selectedPresetId: String? = null,
    val selectedMode: String = "COUNTDOWN", // "COUNTDOWN", "STOPWATCH", "FROM_TO"
    val selectedDurationMinutes: Int = 25,
    val timerState: TimerState = TimerState.Idle,
    val streak: StreakEntity? = null,
    val showStopModal: Boolean = false
)

class FocusViewModel(
    private val db: FluxFoxusDatabase,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                db.presetDao().getAllPresets(),
                userPrefs.lastUsedPresetId,
                FocusTimerService.timerState,
                db.streakDao().getStreakFlow()
            ) { presets, lastPresetId, timerState, streak ->
                val selectedId = _uiState.value.selectedPresetId ?: lastPresetId ?: presets.firstOrNull()?.id
                _uiState.value.copy(
                    presets = presets,
                    selectedPresetId = selectedId,
                    timerState = timerState,
                    streak = streak
                )
            }.collect { updated ->
                _uiState.value = updated
            }
        }
    }

    fun selectPreset(presetId: String) {
        _uiState.value = _uiState.value.copy(selectedPresetId = presetId)
        viewModelScope.launch {
            userPrefs.setLastUsedPresetId(presetId)
        }
    }

    fun selectMode(mode: String) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun selectDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(selectedDurationMinutes = minutes)
    }

    fun startFocusSession(context: Context) {
        val state = _uiState.value
        val preset = state.presets.find { it.id == state.selectedPresetId } ?: state.presets.firstOrNull() ?: return

        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_SESSION_ID, UUID.randomUUID().toString())
            putExtra(FocusTimerService.EXTRA_PRESET_ID, preset.id)
            putExtra(FocusTimerService.EXTRA_TITLE, preset.name)
            putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, state.selectedDurationMinutes)
            putExtra(FocusTimerService.EXTRA_BREAKS_COUNT, preset.breakCount)
            putExtra(FocusTimerService.EXTRA_BREAK_DURATION_MINUTES, preset.breakDurationMinutes)
            putExtra(FocusTimerService.EXTRA_TIMER_MODE, state.selectedMode)
        }
        context.startService(intent)
    }

    fun takeBreak(context: Context) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_TAKE_BREAK
        }
        context.startService(intent)
    }

    fun pauseSession(context: Context) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeSession(context: Context) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun showStopModal() {
        _uiState.value = _uiState.value.copy(showStopModal = true)
    }

    fun dismissStopModal() {
        _uiState.value = _uiState.value.copy(showStopModal = false)
    }

    fun confirmQuit(context: Context) {
        _uiState.value = _uiState.value.copy(showStopModal = false)
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        context.startService(intent)

        // Reset streak to 0 on surrender per specification
        viewModelScope.launch {
            val currentStreak = db.streakDao().getStreak()
            if (currentStreak != null) {
                db.streakDao().upsertStreak(currentStreak.copy(currentStreak = 0, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun finishStopwatchSession(context: Context) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        context.startService(intent)
    }

    class Factory(
        private val db: FluxFoxusDatabase,
        private val userPrefs: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(db, userPrefs) as T
        }
    }
}

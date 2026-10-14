package com.fluxfoxus.features.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.database.entity.StreakEntity
import com.fluxfoxus.core.datastore.UserPreferencesRepository
import com.fluxfoxus.core.service.FocusTimerService
import com.fluxfoxus.core.service.TimerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HomeUiState(
    val currentStreakDays: Int = 0,
    val isStreakDormant: Boolean = true,
    val dailyFocusAverageMinutes: Int = 0,
    val weeklyTotalAverageMinutes: Int = 0,
    val todayFocusMinutes: Int = 0,
    val activePreset: PresetEntity? = null,
    val upcomingSessionTitle: String? = null,
    val upcomingMinutesRemaining: Int = 0,
    val timerState: TimerState = TimerState.Idle,
    val productiveMinutes24h: Int = 0,
    val semiProductiveMinutes24h: Int = 0,
    val distractingMinutes24h: Int = 0,
    val otherMinutes24h: Int = 0
)

class HomeViewModel(
    private val db: FluxFoxusDatabase,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            combine(
                db.streakDao().getStreakFlow(),
                FocusTimerService.timerState,
                db.presetDao().getAllPresets(),
                userPrefs.lastUsedPresetId,
                db.focusSessionDao().getAllSessions(),
                db.dailyUsageDao().getDailyUsageForDate(todayIso),
                db.appLimitDao().getAllCategoryOverrides()
            ) { values ->
                val streak = values[0] as? StreakEntity
                val timerState = values[1] as TimerState
                @Suppress("UNCHECKED_CAST")
                val presets = values[2] as List<PresetEntity>
                val lastPresetId = values[3] as? String
                @Suppress("UNCHECKED_CAST")
                val sessions = values[4] as List<FocusSessionEntity>
                @Suppress("UNCHECKED_CAST")
                val usageList = values[5] as List<com.fluxfoxus.core.database.entity.DailyUsageEntity>
                @Suppress("UNCHECKED_CAST")
                val categoryOverrides = (values[6] as List<com.fluxfoxus.core.database.entity.AppCategoryOverrideEntity>).associateBy { it.packageName }

                val activePreset = presets.find { it.id == lastPresetId } ?: presets.firstOrNull()

                val nowMs = System.currentTimeMillis()
                val upcoming = sessions.find {
                    it.status == "PLANNED" && it.startTime > nowMs && (it.startTime - nowMs) <= 15 * 60 * 1000L
                }

                val todaySessions = sessions.filter {
                    it.status == "COMPLETED" && (nowMs - it.startTime) < 24 * 60 * 60 * 1000L
                }
                val todayFocusMins = todaySessions.sumOf { it.actualDurationSeconds / 60 }

                var prodMins = todayFocusMins
                var semiMins = 0
                var distMins = 0
                var otherMins = 0

                usageList.forEach { usage ->
                    val mins = (usage.totalTimeInForegroundMs / (1000 * 60)).toInt()
                    val cat = categoryOverrides[usage.packageName]?.category ?: "OTHERS"
                    when (cat) {
                        "PRODUCTIVE" -> prodMins += mins
                        "SEMI_PRODUCTIVE" -> semiMins += mins
                        "DISTRACTING" -> distMins += mins
                        else -> otherMins += mins
                    }
                }

                HomeUiState(
                    currentStreakDays = streak?.currentStreak ?: 0,
                    isStreakDormant = (streak?.currentStreak ?: 0) == 0,
                    dailyFocusAverageMinutes = if (todayFocusMins > 0) todayFocusMins else 173,
                    weeklyTotalAverageMinutes = 200,
                    todayFocusMinutes = todayFocusMins,
                    activePreset = activePreset,
                    upcomingSessionTitle = upcoming?.sessionTitle,
                    upcomingMinutesRemaining = if (upcoming != null) (((upcoming.startTime - nowMs) / 60000).toInt()).coerceAtLeast(1) else 0,
                    timerState = timerState,
                    productiveMinutes24h = prodMins,
                    semiProductiveMinutes24h = semiMins,
                    distractingMinutes24h = distMins,
                    otherMinutes24h = otherMins
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startOrResumeFocus(context: Context) {
        val state = _uiState.value
        val preset = state.activePreset ?: return

        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_SESSION_ID, UUID.randomUUID().toString())
            putExtra(FocusTimerService.EXTRA_PRESET_ID, preset.id)
            putExtra(FocusTimerService.EXTRA_TITLE, preset.name)
            putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, 25)
            putExtra(FocusTimerService.EXTRA_BREAKS_COUNT, preset.breakCount)
            putExtra(FocusTimerService.EXTRA_BREAK_DURATION_MINUTES, preset.breakDurationMinutes)
            putExtra(FocusTimerService.EXTRA_TIMER_MODE, "COUNTDOWN")
        }
        context.startService(intent)
    }

    class Factory(
        private val db: FluxFoxusDatabase,
        private val userPrefs: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(db, userPrefs) as T
        }
    }
}

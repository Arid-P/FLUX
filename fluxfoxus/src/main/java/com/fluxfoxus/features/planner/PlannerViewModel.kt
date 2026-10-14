package com.fluxfoxus.features.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class PlannerUiState(
    val selectedDateEpochMs: Long = System.currentTimeMillis(),
    val scheduledSessions: List<FocusSessionEntity> = emptyList(),
    val availablePresets: List<PresetEntity> = emptyList(),
    val isScheduleSheetOpen: Boolean = false
)

class PlannerViewModel(
    private val db: FluxFoxusDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            combine(
                db.focusSessionDao().getAllSessions(),
                db.presetDao().getAllPresets()
            ) { sessions, presets ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = _uiState.value.selectedDateEpochMs
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

                val filtered = sessions.filter { it.startTime in startOfDay until endOfDay }
                _uiState.value.copy(
                    scheduledSessions = filtered,
                    availablePresets = presets
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectDate(epochMs: Long) {
        _uiState.value = _uiState.value.copy(selectedDateEpochMs = epochMs)
        observeSessions()
    }

    fun openScheduleSheet() {
        _uiState.value = _uiState.value.copy(isScheduleSheetOpen = true)
    }

    fun closeScheduleSheet() {
        _uiState.value = _uiState.value.copy(isScheduleSheetOpen = false)
    }

    fun scheduleSession(
        presetId: String,
        title: String,
        startTimeMs: Long,
        endTimeMs: Long,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val session = FocusSessionEntity(
                id = UUID.randomUUID().toString(),
                sessionOrigin = "MANUAL",
                presetId = presetId,
                sessionTitle = title,
                timerMode = "COUNTDOWN",
                startTime = startTimeMs,
                endTime = endTimeMs,
                targetDurationMinutes = durationMinutes.coerceAtLeast(1),
                status = "PLANNED"
            )
            db.focusSessionDao().insertSession(session)
            closeScheduleSheet()
        }
    }

    class Factory(private val db: FluxFoxusDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlannerViewModel(db) as T
        }
    }
}

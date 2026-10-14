package com.fluxfoxus.features.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.AppCategoryOverrideEntity
import com.fluxfoxus.core.database.entity.AppLimitEntity
import com.fluxfoxus.core.database.entity.AppLimitStreakEntity
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AppLimitItem(
    val limit: AppLimitEntity,
    val category: String, // "DISTRACTING", "SEMI_PRODUCTIVE", "PRODUCTIVE", "OTHERS"
    val spentMinutes: Int,
    val streakDays: Int
)

data class LimitsUiState(
    val distractingApps: List<AppLimitItem> = emptyList(),
    val semiProductiveApps: List<AppLimitItem> = emptyList(),
    val otherApps: List<AppLimitItem> = emptyList(),
    val isEditorVisible: Boolean = false,
    val editingLimit: AppLimitEntity? = null,
    val isStreakWarningVisible: Boolean = false,
    val streakWarningLimit: AppLimitEntity? = null,
    val streakWarningDays: Int = 0,
    val isInterventionOverlayVisible: Boolean = false,
    val interventionAppLabel: String = "",
    val interventionPackageName: String = "",
    val interventionSpentMinutes: Int = 0,
    val interventionLimitMinutes: Int = 0
)

class LimitsViewModel(
    private val db: FluxFoxusDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LimitsUiState())
    val uiState: StateFlow<LimitsUiState> = _uiState.asStateFlow()

    init {
        observeLimits()
    }

    private fun observeLimits() {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            combine(
                db.appLimitDao().getAllLimits(),
                db.appLimitDao().getAllCategoryOverrides(),
                db.appLimitDao().getAllAppLimitStreaks(),
                db.dailyUsageDao().getDailyUsageForDate(todayIso)
            ) { limits, overrides, streaks, usages ->
                val overrideMap = overrides.associateBy { it.packageName }
                val streakMap = streaks.associateBy { it.packageName }
                val usageMap = usages.associateBy { it.packageName }

                val items = limits.map { limit ->
                    val cat = overrideMap[limit.packageName]?.category ?: "DISTRACTING"
                    val spentMs = usageMap[limit.packageName]?.totalTimeInForegroundMs ?: 0L
                    val spentMins = (spentMs / 60000L).toInt()
                    val streakDays = streakMap[limit.packageName]?.currentStreak ?: 0

                    AppLimitItem(
                        limit = limit,
                        category = cat,
                        spentMinutes = spentMins,
                        streakDays = streakDays
                    )
                }

                val distracting = items.filter { it.category == "DISTRACTING" }
                val semiProd = items.filter { it.category == "SEMI_PRODUCTIVE" }
                val others = items.filter { it.category != "DISTRACTING" && it.category != "SEMI_PRODUCTIVE" }

                _uiState.value.copy(
                    distractingApps = distracting,
                    semiProductiveApps = semiProd,
                    otherApps = others
                )
            }.collect { updated ->
                _uiState.value = updated
            }
        }
    }

    fun openEditor(limit: AppLimitEntity? = null) {
        _uiState.value = _uiState.value.copy(
            isEditorVisible = true,
            editingLimit = limit
        )
    }

    fun closeEditor() {
        _uiState.value = _uiState.value.copy(
            isEditorVisible = false,
            editingLimit = null
        )
    }

    fun saveLimit(
        packageName: String,
        appLabel: String,
        dailyBudgetMinutes: Int,
        extraPasses: Int = 0,
        category: String = "DISTRACTING"
    ) {
        viewModelScope.launch {
            val limit = AppLimitEntity(
                packageName = packageName,
                appLabel = appLabel,
                dailyBudgetMinutes = dailyBudgetMinutes.coerceAtLeast(15),
                extraTimePassesUsed = extraPasses,
                isEnabled = true
            )
            db.appLimitDao().upsertLimit(limit)
            db.appLimitDao().upsertCategoryOverride(
                AppCategoryOverrideEntity(
                    packageName = packageName,
                    appLabel = appLabel,
                    category = category
                )
            )
            closeEditor()
        }
    }

    fun initiateTurnOffLimit(limit: AppLimitEntity) {
        val item = (_uiState.value.distractingApps + _uiState.value.semiProductiveApps + _uiState.value.otherApps)
            .firstOrNull { it.limit.packageName == limit.packageName }
        _uiState.value = _uiState.value.copy(
            isStreakWarningVisible = true,
            streakWarningLimit = limit,
            streakWarningDays = item?.streakDays ?: 0
        )
    }

    fun dismissStreakWarning() {
        _uiState.value = _uiState.value.copy(
            isStreakWarningVisible = false,
            streakWarningLimit = null
        )
    }

    fun confirmTurnOffLimit(packageName: String, durationOption: String) {
        viewModelScope.launch {
            val current = db.appLimitDao().getLimitForPackage(packageName)
            if (current != null) {
                db.appLimitDao().upsertLimit(current.copy(isEnabled = false))
            }
            dismissStreakWarning()
            closeEditor()
        }
    }

    fun toggleLimitEnabled(limit: AppLimitEntity) {
        viewModelScope.launch {
            if (limit.isEnabled) {
                initiateTurnOffLimit(limit)
            } else {
                db.appLimitDao().upsertLimit(limit.copy(isEnabled = true))
            }
        }
    }

    fun showInterventionOverlay(appLabel: String, packageName: String, spentMinutes: Int, limitMinutes: Int) {
        _uiState.value = _uiState.value.copy(
            isInterventionOverlayVisible = true,
            interventionAppLabel = appLabel,
            interventionPackageName = packageName,
            interventionSpentMinutes = spentMinutes,
            interventionLimitMinutes = limitMinutes
        )
    }

    fun dismissInterventionOverlay() {
        _uiState.value = _uiState.value.copy(
            isInterventionOverlayVisible = false
        )
    }

    class Factory(
        private val db: FluxFoxusDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LimitsViewModel(db) as T
        }
    }
}

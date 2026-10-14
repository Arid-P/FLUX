package com.fluxfoxus.features.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.GlobalContentWhitelistEntity
import com.fluxfoxus.core.database.entity.PresetAppRuleEntity
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.repository.AppInfo
import com.fluxfoxus.core.repository.InstalledAppsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class PresetEditorUiState(
    val presetId: String? = null,
    val isEditing: Boolean = false,
    val name: String = "",
    val emoji: String = "⏳",
    val breakCount: Int = 2,
    val breakDurationMinutes: Int = 5,
    val youtubeMode: String = "STUDY_MODE", // 'BLOCK', 'ALLOW', 'STUDY_MODE'
    val browserMode: String = "STUDY_MODE", // 'BLOCK', 'ALLOW', 'STUDY_MODE'
    val description: String = "",
    val blockedPackages: Set<String> = emptySet(),
    val installedApps: List<AppInfo> = emptyList(),
    val youtubeWhitelists: List<GlobalContentWhitelistEntity> = emptyList(),
    val browserWhitelists: List<GlobalContentWhitelistEntity> = emptyList(),
    val isEmojiSheetVisible: Boolean = false,
    val isAppSelectorVisible: Boolean = false,
    val isWhitelistManagerVisible: Boolean = false,
    val whitelistManagerType: String = "YOUTUBE" // "YOUTUBE" or "BROWSER"
)

class PresetEditorViewModel(
    private val db: FluxFoxusDatabase,
    private val initialPresetId: String? = null,
    private val installedAppsRepo: InstalledAppsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PresetEditorUiState(
            presetId = initialPresetId,
            isEditing = initialPresetId != null
        )
    )
    val uiState: StateFlow<PresetEditorUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (initialPresetId != null) {
                val preset = db.presetDao().getPresetById(initialPresetId)
                if (preset != null) {
                    _uiState.value = _uiState.value.copy(
                        name = preset.name,
                        emoji = preset.emoji,
                        breakCount = preset.breakCount,
                        breakDurationMinutes = preset.breakDurationMinutes,
                        youtubeMode = preset.youtubeMode,
                        browserMode = preset.browserMode,
                        description = preset.description ?: ""
                    )
                }
                db.presetDao().getAppRulesForPreset(initialPresetId).collectLatest { rules ->
                    val blocked = rules.filter { it.ruleMode == "BLOCKED" }.map { it.packageName }.toSet()
                    _uiState.value = _uiState.value.copy(blockedPackages = blocked)
                }
            }
        }

        viewModelScope.launch {
            if (installedAppsRepo != null) {
                val apps = installedAppsRepo.getInstalledApps()
                _uiState.value = _uiState.value.copy(installedApps = apps)
            }
        }

        viewModelScope.launch {
            db.whitelistDao().getAllWhitelistItems().collectLatest { items ->
                val yt = items.filter { it.appType == "YOUTUBE" }
                val br = items.filter { it.appType == "BROWSER" }
                _uiState.value = _uiState.value.copy(
                    youtubeWhitelists = yt,
                    browserWhitelists = br
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setEmoji(emoji: String) {
        _uiState.value = _uiState.value.copy(emoji = emoji, isEmojiSheetVisible = false)
    }

    fun setBreakCount(count: Int) {
        _uiState.value = _uiState.value.copy(breakCount = count.coerceIn(0, 6))
    }

    fun setBreakDuration(mins: Int) {
        _uiState.value = _uiState.value.copy(breakDurationMinutes = mins.coerceIn(1, 30))
    }

    fun setYoutubeMode(mode: String) {
        _uiState.value = _uiState.value.copy(youtubeMode = mode)
    }

    fun setBrowserMode(mode: String) {
        _uiState.value = _uiState.value.copy(browserMode = mode)
    }

    fun setDescription(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
    }

    fun toggleAppBlocked(pkg: String) {
        val current = _uiState.value.blockedPackages.toMutableSet()
        if (current.contains(pkg)) {
            current.remove(pkg)
        } else {
            current.add(pkg)
        }
        _uiState.value = _uiState.value.copy(blockedPackages = current)
    }

    fun setEmojiSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isEmojiSheetVisible = visible)
    }

    fun setAppSelectorVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isAppSelectorVisible = visible)
    }

    fun openWhitelistManager(type: String) {
        _uiState.value = _uiState.value.copy(
            isWhitelistManagerVisible = true,
            whitelistManagerType = type
        )
    }

    fun closeWhitelistManager() {
        _uiState.value = _uiState.value.copy(isWhitelistManagerVisible = false)
    }

    fun addWhitelistItem(type: String, targetIdentifier: String, displayLabel: String) {
        viewModelScope.launch {
            db.whitelistDao().upsertWhitelistItem(
                GlobalContentWhitelistEntity(
                    id = UUID.randomUUID().toString(),
                    appType = type,
                    targetIdentifier = targetIdentifier,
                    displayLabel = displayLabel
                )
            )
        }
    }

    fun deleteWhitelistItem(id: String) {
        viewModelScope.launch {
            db.whitelistDao().deleteWhitelistItemById(id)
        }
    }

    fun savePreset(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) return

        val id = state.presetId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            val preset = PresetEntity(
                id = id,
                name = state.name.trim(),
                emoji = state.emoji,
                breakCount = state.breakCount,
                breakDurationMinutes = state.breakDurationMinutes,
                youtubeMode = state.youtubeMode,
                browserMode = state.browserMode,
                description = state.description.ifBlank { null },
                updatedAt = System.currentTimeMillis()
            )
            db.presetDao().insertPreset(preset)

            // Save blocked apps
            db.presetDao().deleteAppRulesForPreset(id)
            val rules = state.blockedPackages.map { pkg ->
                PresetAppRuleEntity(
                    presetId = id,
                    packageName = pkg,
                    ruleMode = "BLOCKED"
                )
            }
            if (rules.isNotEmpty()) {
                db.presetDao().insertAppRules(rules)
            }

            onSaved()
        }
    }

    fun deletePreset(onDeleted: () -> Unit) {
        val id = _uiState.value.presetId ?: return
        viewModelScope.launch {
            db.presetDao().deletePresetById(id)
            onDeleted()
        }
    }

    class Factory(
        private val db: FluxFoxusDatabase,
        private val initialPresetId: String? = null,
        private val installedAppsRepo: InstalledAppsRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PresetEditorViewModel(db, initialPresetId, installedAppsRepo) as T
        }
    }
}

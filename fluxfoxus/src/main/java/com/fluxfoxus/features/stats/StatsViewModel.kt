package com.fluxfoxus.features.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.AppCategoryOverrideEntity
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import com.fluxfoxus.core.database.entity.FocusSessionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class StatsTabMode {
    DAILY, WEEKLY
}

data class AppUsageStat(
    val packageName: String,
    val appLabel: String,
    val totalTimeMs: Long,
    val category: String, // "PRODUCTIVE", "SEMI_PRODUCTIVE", "DISTRACTING", "OTHERS"
    val percentageOfTotal: Float
)

data class DayStackedBar(
    val dayLabel: String, // "M", "T", "W", "T", "F", "S", "S"
    val dateIso: String,
    val productiveMins: Int,
    val semiProductiveMins: Int,
    val distractingMins: Int,
    val otherMins: Int,
    val isToday: Boolean
) {
    val totalMins: Int get() = productiveMins + semiProductiveMins + distractingMins + otherMins
}

data class StatsUiState(
    val mode: StatsTabMode = StatsTabMode.DAILY,
    val focusRatio: Float = 1.0f,
    val focusMinutesToday: Int = 0,
    val casualMinutesToday: Int = 0,
    val totalScreenTimeMinutesToday: Int = 0,
    val disciplineTierName: String = "Developing Rhythm",
    val selectedCategoryFilter: String? = null,
    val dailyAppStats: List<AppUsageStat> = emptyList(),
    val weeklyAppStats: List<AppUsageStat> = emptyList(),
    val sevenDayBars: List<DayStackedBar> = emptyList(),
    // Weekly metrics
    val weeklyFocusMinutes: Int = 0,
    val weeklyDistractingMinutes: Int = 0,
    val weeklyAvgDailyMinutes: Int = 0,
    val disciplineGrade: String = "A",
    val weeklyFocusChangePercent: Int = 0,
    val weeklyDistractingChangePercent: Int = 0
)

class StatsViewModel(
    private val db: FluxFoxusDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayIso = dateFormat.format(calendar.time)

        // Calculate 7 day range
        val past7Days = mutableListOf<String>()
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            past7Days.add(dateFormat.format(d.time))
        }
        val startIso = past7Days.first()
        val endIso = past7Days.last()

        viewModelScope.launch {
            combine(
                db.dailyUsageDao().getDailyUsageInRange(startIso, endIso),
                db.focusSessionDao().getAllSessions(),
                db.appLimitDao().getAllCategoryOverrides()
            ) { usages, sessions, overrides ->
                val overrideMap = overrides.associateBy { it.packageName }

                // Today's usages
                val todayUsages = usages.filter { it.dateIso == todayIso }
                val totalTodayScreenMs = todayUsages.sumOf { it.totalTimeInForegroundMs }
                val totalTodayScreenMins = (totalTodayScreenMs / 60000L).toInt()

                // Today's completed focus sessions
                val todaySessions = sessions.filter {
                    val sDate = dateFormat.format(Date(it.startTime))
                    sDate == todayIso && it.status == "COMPLETED"
                }
                val todayFocusMins = todaySessions.sumOf {
                    (it.actualDurationSeconds / 60).coerceAtLeast(it.targetDurationMinutes)
                }

                val casualMins = (totalTodayScreenMins - todayFocusMins).coerceAtLeast(0)
                val ratio = if (casualMins <= 0) {
                    if (todayFocusMins > 0) 10.0f else 0.0f
                } else {
                    todayFocusMins.toFloat() / casualMins.toFloat()
                }

                val tier = when {
                    ratio > 2.0f -> "Monk Mode"
                    ratio >= 1.0f -> "Deep Work"
                    ratio >= 0.5f -> "Developing Rhythm"
                    else -> "Attention Leak"
                }

                // Daily App Stats
                val dailyStats = todayUsages.map { usage ->
                    val cat = overrideMap[usage.packageName]?.category ?: "DISTRACTING"
                    val label = overrideMap[usage.packageName]?.appLabel ?: usage.packageName.substringAfterLast(".")
                    val percent = if (totalTodayScreenMs > 0) usage.totalTimeInForegroundMs.toFloat() / totalTodayScreenMs.toFloat() else 0f
                    AppUsageStat(
                        packageName = usage.packageName,
                        appLabel = label,
                        totalTimeMs = usage.totalTimeInForegroundMs,
                        category = cat,
                        percentageOfTotal = percent
                    )
                }.sortedByDescending { it.totalTimeMs }

                // 7-day Stacked Bars
                val bars = past7Days.mapIndexed { idx, dateStr ->
                    val dayU = usages.filter { it.dateIso == dateStr }
                    var prod = 0
                    var semi = 0
                    var dist = 0
                    var other = 0

                    dayU.forEach { u ->
                        val mins = (u.totalTimeInForegroundMs / 60000L).toInt()
                        when (overrideMap[u.packageName]?.category ?: "DISTRACTING") {
                            "PRODUCTIVE" -> prod += mins
                            "SEMI_PRODUCTIVE" -> semi += mins
                            "DISTRACTING" -> dist += mins
                            else -> other += mins
                        }
                    }

                    val c = Calendar.getInstance().apply { time = dateFormat.parse(dateStr) ?: Date() }
                    val dayOfWeekIdx = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

                    DayStackedBar(
                        dayLabel = dayLabels[dayOfWeekIdx],
                        dateIso = dateStr,
                        productiveMins = prod,
                        semiProductiveMins = semi,
                        distractingMins = dist,
                        otherMins = other,
                        isToday = dateStr == todayIso
                    )
                }

                // Weekly aggregate
                val weeklyProd = bars.sumOf { it.productiveMins }
                val weeklyDist = bars.sumOf { it.distractingMins }
                val totalWeeklyMins = bars.sumOf { it.totalMins }
                val weeklyAvgDaily = if (bars.isNotEmpty()) totalWeeklyMins / bars.size else 0

                val grade = when {
                    weeklyProd >= 20 * 60 && weeklyDist <= 5 * 60 -> "A+"
                    weeklyProd >= 15 * 60 -> "A"
                    weeklyProd >= 10 * 60 -> "B"
                    else -> "C"
                }

                // Weekly App Stats
                val weeklyAppMap = mutableMapOf<String, Long>()
                usages.forEach { u ->
                    weeklyAppMap[u.packageName] = (weeklyAppMap[u.packageName] ?: 0L) + u.totalTimeInForegroundMs
                }
                val totalWeeklyMs = weeklyAppMap.values.sum()
                val weeklyStats = weeklyAppMap.map { (pkg, totalMs) ->
                    val cat = overrideMap[pkg]?.category ?: "DISTRACTING"
                    val label = overrideMap[pkg]?.appLabel ?: pkg.substringAfterLast(".")
                    val percent = if (totalWeeklyMs > 0) totalMs.toFloat() / totalWeeklyMs.toFloat() else 0f
                    AppUsageStat(
                        packageName = pkg,
                        appLabel = label,
                        totalTimeMs = totalMs,
                        category = cat,
                        percentageOfTotal = percent
                    )
                }.sortedByDescending { it.totalTimeMs }

                _uiState.value.copy(
                    focusRatio = ratio,
                    focusMinutesToday = todayFocusMins,
                    casualMinutesToday = casualMins,
                    totalScreenTimeMinutesToday = totalTodayScreenMins,
                    disciplineTierName = tier,
                    dailyAppStats = dailyStats,
                    weeklyAppStats = weeklyStats,
                    sevenDayBars = bars,
                    weeklyFocusMinutes = weeklyProd,
                    weeklyDistractingMinutes = weeklyDist,
                    weeklyAvgDailyMinutes = weeklyAvgDaily,
                    disciplineGrade = grade,
                    weeklyFocusChangePercent = 21,
                    weeklyDistractingChangePercent = -34
                )
            }.collect { updated ->
                _uiState.value = updated
            }
        }
    }

    fun setMode(mode: StatsTabMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun toggleCategoryFilter(category: String) {
        val current = _uiState.value.selectedCategoryFilter
        _uiState.value = _uiState.value.copy(
            selectedCategoryFilter = if (current == category) null else category
        )
    }

    fun updateAppCategory(packageName: String, appLabel: String, category: String) {
        viewModelScope.launch {
            db.appLimitDao().upsertCategoryOverride(
                AppCategoryOverrideEntity(
                    packageName = packageName,
                    appLabel = appLabel,
                    category = category,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    class Factory(
        private val db: FluxFoxusDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(db) as T
        }
    }
}

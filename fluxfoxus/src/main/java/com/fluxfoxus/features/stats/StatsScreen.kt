package com.fluxfoxus.features.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxfoxus.core.designsystem.theme.*

import com.fluxfoxus.core.designsystem.component.RusticMedleyHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = HolstColors.Background,
        topBar = {
            RusticMedleyHeader(
                title = "STATS",
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = HolstColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            SegmentPill(
                                text = "Daily",
                                isSelected = state.mode == StatsTabMode.DAILY,
                                onClick = { viewModel.setMode(StatsTabMode.DAILY) }
                            )
                            SegmentPill(
                                text = "Weekly",
                                isSelected = state.mode == StatsTabMode.WEEKLY,
                                onClick = { viewModel.setMode(StatsTabMode.WEEKLY) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.mode == StatsTabMode.DAILY) {
                // View A: Daily Tab
                item {
                    HeroFocusRatioCard(
                        focusRatio = state.focusRatio,
                        tierName = state.disciplineTierName,
                        focusMins = state.focusMinutesToday,
                        casualMins = state.casualMinutesToday
                    )
                }

                item {
                    SevenDayStackedBarChart(
                        bars = state.sevenDayBars,
                        targetMins = 240 // 4h target line
                    )
                }

                item {
                    CategoryFilterChips(
                        selectedFilter = state.selectedCategoryFilter,
                        onSelectFilter = { viewModel.toggleCategoryFilter(it) }
                    )
                }

                val filteredDailyApps = if (state.selectedCategoryFilter != null) {
                    state.dailyAppStats.filter { it.category == state.selectedCategoryFilter }
                } else {
                    state.dailyAppStats
                }

                item {
                    Text(
                        text = "TODAY'S APPS (${filteredDailyApps.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HolstColors.MutedBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (filteredDailyApps.isEmpty()) {
                    item {
                        Text(
                            text = "No app usage recorded for today.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.MutedBlue),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(filteredDailyApps, key = { it.packageName }) { appStat ->
                        AppUsageRow(
                            stat = appStat,
                            onClick = { onNavigateToAppDetail(appStat.packageName) }
                        )
                    }
                }
            } else {
                // View B: Weekly Trends Tab
                item {
                    WeekOverWeekHeroCard(
                        weeklyFocusMins = state.weeklyFocusMinutes,
                        weeklyDistractingMins = state.weeklyDistractingMinutes,
                        grade = state.disciplineGrade,
                        focusRatio = state.focusRatio,
                        focusChangePercent = state.weeklyFocusChangePercent,
                        distractingChangePercent = state.weeklyDistractingChangePercent
                    )
                }

                item {
                    SevenDayStackedBarChart(
                        bars = state.sevenDayBars,
                        targetMins = state.weeklyAvgDailyMinutes
                    )
                }

                item {
                    WeeklyCategoryDistributionCard(bars = state.sevenDayBars)
                }

                item {
                    Text(
                        text = "TOP APPS THIS WEEK (${state.weeklyAppStats.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HolstColors.MutedBlue,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                items(state.weeklyAppStats, key = { it.packageName }) { appStat ->
                    AppUsageRow(
                        stat = appStat,
                        isWeekly = true,
                        onClick = { onNavigateToAppDetail(appStat.packageName) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SegmentPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) HolstColors.PrimarySand else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isSelected) HolstColors.Background else HolstColors.MutedBlue,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun HeroFocusRatioCard(
    focusRatio: Float,
    tierName: String,
    focusMins: Int,
    casualMins: Int
) {
    val tierColor = when {
        focusRatio > 2.0f -> HolstColors.TextPrimary
        focusRatio >= 1.0f -> HolstColors.AccentSage
        focusRatio >= 0.5f -> HolstColors.PrimarySand
        else -> HolstColors.WarningTerracotta
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Focus / Screen Ratio",
                        style = MaterialTheme.typography.labelMedium.copy(color = HolstColors.MutedBlue)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fx", focusRatio),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tierColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tierColor)
                ) {
                    Text(
                        text = "$tierName ⚡",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = tierColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Sub-label
            val focusHours = focusMins / 60
            val focusRemMins = focusMins % 60
            val casualHours = casualMins / 60
            val casualRemMins = casualMins % 60

            Text(
                text = "${focusHours}h ${focusRemMins}m Deep Focus vs ${casualHours}h ${casualRemMins}m Casual Screen",
                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
            )

            // Horizontal dual-split progress bar
            val totalMins = (focusMins + casualMins).coerceAtLeast(1)
            val focusFraction = (focusMins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HolstColors.Border)
            ) {
                if (focusFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(focusFraction)
                            .background(HolstColors.AccentSage)
                    )
                }
                if (focusFraction < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - focusFraction).coerceAtLeast(0.001f))
                            .background(HolstColors.WarningTerracotta)
                    )
                }
            }
        }
    }
}

@Composable
fun SevenDayStackedBarChart(
    bars: List<DayStackedBar>,
    targetMins: Int
) {
    val maxMins = (bars.maxOfOrNull { it.totalMins } ?: 240).coerceAtLeast(targetMins).coerceAtLeast(1)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "7-DAY ACTIVITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HolstColors.MutedBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Target: ${targetMins / 60}h",
                    style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.PrimarySand)
                )
            }

            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Target line
                val targetFraction = (targetMins.toFloat() / maxMins.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(bottom = (140 * targetFraction).dp)
                        .height(1.dp)
                        .background(HolstColors.PrimarySand.copy(alpha = 0.4f))
                )

                // Bars Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEach { bar ->
                        val barHeightFraction = (bar.totalMins.toFloat() / maxMins.toFloat()).coerceIn(0.05f, 1f)
                        val total = bar.totalMins.coerceAtLeast(1).toFloat()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .then(
                                        if (bar.isToday) Modifier.border(
                                            1.5.dp,
                                            HolstColors.PrimarySand,
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        ) else Modifier
                                    )
                                    .background(HolstColors.Border)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Stack segments from top to bottom
                                    if (bar.otherMins > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(bar.otherMins / total)
                                                .background(HolstColors.Border)
                                        )
                                    }
                                    if (bar.distractingMins > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(bar.distractingMins / total)
                                                .background(HolstColors.WarningTerracotta)
                                        )
                                    }
                                    if (bar.semiProductiveMins > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(bar.semiProductiveMins / total)
                                                .background(HolstColors.PrimarySand)
                                        )
                                    }
                                    if (bar.productiveMins > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(bar.productiveMins / total)
                                                .background(HolstColors.AccentSage)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = bar.dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (bar.isToday) HolstColors.PrimarySand else HolstColors.TextMuted,
                                    fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChips(
    selectedFilter: String?,
    onSelectFilter: (String) -> Unit
) {
    val categories = listOf(
        Triple("PRODUCTIVE", "🟢 Productive", HolstColors.AccentSage),
        Triple("SEMI_PRODUCTIVE", "🟡 Semi-Prod", HolstColors.PrimarySand),
        Triple("DISTRACTING", "🔴 Distracting", HolstColors.WarningTerracotta),
        Triple("OTHERS", "⚪ Others", HolstColors.Border)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { (key, label, color) ->
            val isSelected = selectedFilter == key
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) color.copy(alpha = 0.25f) else HolstColors.Surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) color else HolstColors.Border
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectFilter(key) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) HolstColors.TextPrimary else HolstColors.MutedBlue,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun AppUsageRow(
    stat: AppUsageStat,
    isWeekly: Boolean = false,
    onClick: () -> Unit
) {
    val dotColor = when (stat.category) {
        "PRODUCTIVE" -> HolstColors.AccentSage
        "SEMI_PRODUCTIVE" -> HolstColors.PrimarySand
        "DISTRACTING" -> HolstColors.WarningTerracotta
        else -> HolstColors.Border
    }

    val totalMins = (stat.totalTimeMs / 60000L).toInt()
    val hours = totalMins / 60
    val mins = totalMins % 60
    val durationText = "${hours}h ${mins}m"
    val percentText = "${(stat.percentageOfTotal * 100).toInt()}%"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_usage_row_${stat.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(HolstColors.Border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stat.appLabel.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = HolstColors.PrimarySand,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.appLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = if (isWeekly) "$durationText (Avg ${totalMins / 7}m/day)" else "$durationText ($percentText)",
                    style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                )
            }

            // Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun WeekOverWeekHeroCard(
    weeklyFocusMins: Int,
    weeklyDistractingMins: Int,
    grade: String,
    focusRatio: Float,
    focusChangePercent: Int,
    distractingChangePercent: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEK-OVER-WEEK REPORT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HolstColors.MutedBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HolstColors.AccentSage.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.AccentSage)
                ) {
                    Text(
                        text = "Grade: $grade",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = HolstColors.AccentSage,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Weekly Focus", style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue))
                    Text(
                        text = "${weeklyFocusMins / 60}h ${weeklyFocusMins % 60}m",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "(+${focusChangePercent}% ▲)",
                        style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.AccentSage)
                    )
                }

                Column {
                    Text("Distracting", style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue))
                    Text(
                        text = "${weeklyDistractingMins / 60}h ${weeklyDistractingMins % 60}m",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "(${distractingChangePercent}% ▼)",
                        style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.AccentSage)
                    )
                }

                Column {
                    Text("Discipline Gain", style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue))
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fx", focusRatio),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Monk Flow",
                        style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.PrimarySand)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyCategoryDistributionCard(bars: List<DayStackedBar>) {
    val totalProd = bars.sumOf { it.productiveMins }
    val totalSemi = bars.sumOf { it.semiProductiveMins }
    val totalDist = bars.sumOf { it.distractingMins }
    val total = (totalProd + totalSemi + totalDist).coerceAtLeast(1).toFloat()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "WEEKLY CATEGORY DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HolstColors.MutedBlue,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "🟢 Productive: ${totalProd / 60}h ${totalProd % 60}m (${((totalProd / total) * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.TextPrimary)
            )
            Text(
                text = "🟡 Semi-Prod: ${totalSemi / 60}h ${totalSemi % 60}m (${((totalSemi / total) * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.TextPrimary)
            )
            Text(
                text = "🔴 Distracting: ${totalDist / 60}h ${totalDist % 60}m (${((totalDist / total) * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.TextPrimary)
            )
        }
    }
}

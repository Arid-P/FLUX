package com.fluxfoxus.features.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
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
import com.fluxfoxus.core.database.FluxFoxusDatabase
import com.fluxfoxus.core.database.entity.AppCategoryOverrideEntity
import com.fluxfoxus.core.database.entity.AppLimitEntity
import com.fluxfoxus.core.database.entity.DailyUsageEntity
import com.fluxfoxus.core.designsystem.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    db: FluxFoxusDatabase,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var appLabel by remember { mutableStateOf(packageName.substringAfterLast(".")) }
    var currentCategory by remember { mutableStateOf("DISTRACTING") }
    var appLimit by remember { mutableStateOf<AppLimitEntity?>(null) }
    var usageHistory by remember { mutableStateOf<List<DailyUsageEntity>>(emptyList()) }
    var isCategorySheetVisible by remember { mutableStateOf(false) }
    var isWeeklyMode by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, dd MMM", Locale.getDefault()) }
    var selectedDateIso by remember { mutableStateOf(dateFormat.format(Date())) }

    // Observe category override
    LaunchedEffect(packageName) {
        db.appLimitDao().getCategoryOverrideFlow(packageName).collectLatest { override ->
            if (override != null) {
                appLabel = override.appLabel
                currentCategory = override.category
            }
        }
    }

    // Observe app limit
    LaunchedEffect(packageName) {
        db.appLimitDao().getLimitForPackageFlow(packageName).collectLatest { limit ->
            appLimit = limit
            if (limit != null && appLabel == packageName.substringAfterLast(".")) {
                appLabel = limit.appLabel
            }
        }
    }

    // Observe 7-day usage history
    LaunchedEffect(packageName) {
        val past7Days = mutableListOf<String>()
        for (i in 6 downTo 0) {
            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            past7Days.add(dateFormat.format(d.time))
        }
        db.dailyUsageDao().getDailyUsageForPackageInRange(packageName, past7Days.first(), past7Days.last())
            .collectLatest { list ->
                usageHistory = list
            }
    }

    val categoryColor = when (currentCategory) {
        "PRODUCTIVE" -> HolstColors.AccentSage
        "SEMI_PRODUCTIVE" -> HolstColors.PrimarySand
        "DISTRACTING" -> HolstColors.WarningTerracotta
        else -> HolstColors.Border
    }

    val selectedDayUsageMs = usageHistory.firstOrNull { it.dateIso == selectedDateIso }?.totalTimeInForegroundMs ?: 0L
    val totalWeeklyMs = usageHistory.sumOf { it.totalTimeInForegroundMs }
    val avgDailyMs = if (usageHistory.isNotEmpty()) totalWeeklyMs / usageHistory.size else 0L

    val heroTimeMs = if (isWeeklyMode) avgDailyMs else selectedDayUsageMs
    val heroHours = (heroTimeMs / 3600000L).toInt()
    val heroMins = ((heroTimeMs % 3600000L) / 60000L).toInt()

    Scaffold(
        containerColor = HolstColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(HolstColors.Border, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appLabel.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = HolstColors.PrimarySand,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HolstColors.TextPrimary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HolstColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HolstColors.Background
                )
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
            // Mode Selector Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HolstColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (!isWeeklyMode) HolstColors.PrimarySand else Color.Transparent,
                                modifier = Modifier.clickable { isWeeklyMode = false }
                            ) {
                                Text(
                                    text = "Daily",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (!isWeeklyMode) HolstColors.Background else HolstColors.MutedBlue,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isWeeklyMode) HolstColors.PrimarySand else Color.Transparent,
                                modifier = Modifier.clickable { isWeeklyMode = true }
                            ) {
                                Text(
                                    text = "Weekly",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isWeeklyMode) HolstColors.Background else HolstColors.MutedBlue,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Hero Metric Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HolstColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Navigator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val c = Calendar.getInstance().apply { time = dateFormat.parse(selectedDateIso) ?: Date() }
                                    c.add(Calendar.DAY_OF_YEAR, -1)
                                    selectedDateIso = dateFormat.format(c.time)
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Day", tint = HolstColors.TextMuted)
                            }

                            val displayDate = try {
                                displayFormat.format(dateFormat.parse(selectedDateIso) ?: Date())
                            } catch (e: Exception) {
                                selectedDateIso
                            }

                            Text(
                                text = if (isWeeklyMode) "Past 7 Days" else displayDate,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = HolstColors.MutedBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            IconButton(
                                onClick = {
                                    val c = Calendar.getInstance().apply { time = dateFormat.parse(selectedDateIso) ?: Date() }
                                    c.add(Calendar.DAY_OF_YEAR, 1)
                                    val nextIso = dateFormat.format(c.time)
                                    if (nextIso <= dateFormat.format(Date())) {
                                        selectedDateIso = nextIso
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = HolstColors.TextMuted)
                            }
                        }

                        // Hero Time
                        Text(
                            text = "${heroHours}h ${heroMins}m",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = HolstColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp
                            )
                        )

                        if (isWeeklyMode) {
                            val weeklyTotalHours = (totalWeeklyMs / 3600000L).toInt()
                            val weeklyTotalMins = ((totalWeeklyMs % 3600000L) / 60000L).toInt()
                            Text(
                                text = "Total: ${weeklyTotalHours}h ${weeklyTotalMins}m this week",
                                style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 7-Day App Bar Chart
                        val past7Days = remember {
                            val list = mutableListOf<Pair<String, String>>()
                            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                            for (i in 6 downTo 0) {
                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                                val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                                list.add(dateFormat.format(cal.time) to labels[dayOfWeek])
                            }
                            list
                        }

                        val maxUsageDayMs = (usageHistory.maxOfOrNull { it.totalTimeInForegroundMs } ?: 3600000L).coerceAtLeast(3600000L)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            past7Days.forEach { (dateStr, label) ->
                                val dayMs = usageHistory.firstOrNull { it.dateIso == dateStr }?.totalTimeInForegroundMs ?: 0L
                                val heightFrac = (dayMs.toFloat() / maxUsageDayMs.toFloat()).coerceIn(0.05f, 1f)
                                val isSelected = if (isWeeklyMode) true else dateStr == selectedDateIso

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .fillMaxHeight(heightFrac)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (isSelected) categoryColor else HolstColors.Border)
                                            .clickable {
                                                if (!isWeeklyMode) selectedDateIso = dateStr
                                            }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) HolstColors.PrimarySand else HolstColors.TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Usage Category Assignment Card
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HolstColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCategorySheetVisible = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            @Suppress("DEPRECATION")
                            Icon(Icons.Default.Label, contentDescription = "Category", tint = HolstColors.PrimarySand)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = HolstColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        val categoryTitle = when (currentCategory) {
                            "PRODUCTIVE" -> "🟢 Productive ▾"
                            "SEMI_PRODUCTIVE" -> "🟡 Semi-Prod ▾"
                            "DISTRACTING" -> "🔴 Distracting ▾"
                            else -> "⚪ Others ▾"
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = categoryColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor)
                        ) {
                            Text(
                                text = categoryTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Manage Blocks Card
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HolstColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MANAGE BLOCKS & LIMITS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = HolstColors.MutedBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        val limitText = if (appLimit != null && appLimit!!.isEnabled) {
                            "${appLimit!!.dailyBudgetMinutes}m daily limit (Active 🟢)"
                        } else {
                            "No daily limit active"
                        }

                        Text(
                            text = limitText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = HolstColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Privacy Lock Footer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy",
                        tint = HolstColors.MutedBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "We care about your privacy. All data stays local.",
                        style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue)
                    )
                }
            }
        }
    }

    // Category Selector Dialog / Bottom Sheet
    if (isCategorySheetVisible) {
        val categories = listOf(
            "PRODUCTIVE" to "🟢 Productive (Academic tools, Notewise, study materials)",
            "SEMI_PRODUCTIVE" to "🟡 Semi-Productive (Calculators, system utilities, calendar)",
            "DISTRACTING" to "🔴 Distracting (Social media, video feeds, games)",
            "OTHERS" to "⚪ Others (Unclassified system apps)"
        )

        AlertDialog(
            onDismissRequest = { isCategorySheetVisible = false },
            title = {
                Text(
                    text = "Assign Category",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = currentCategory == catKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) HolstColors.PrimarySand.copy(alpha = 0.2f) else HolstColors.Surface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, HolstColors.PrimarySand) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        db.appLimitDao().upsertCategoryOverride(
                                            AppCategoryOverrideEntity(
                                                packageName = packageName,
                                                appLabel = appLabel,
                                                category = catKey,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                        currentCategory = catKey
                                        isCategorySheetVisible = false
                                    }
                                }
                        ) {
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) HolstColors.PrimarySand else HolstColors.TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isCategorySheetVisible = false }) {
                    Text("Close", color = HolstColors.MutedBlue)
                }
            },
            containerColor = HolstColors.Surface
        )
    }
}

package com.fluxfoxus.features.limits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fluxfoxus.core.database.entity.AppLimitEntity
import com.fluxfoxus.core.designsystem.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitsScreen(
    viewModel: LimitsViewModel,
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = HolstColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LIMITS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = HolstColors.TextPrimary
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openEditor() },
                        modifier = Modifier.testTag("add_limit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Limit",
                            tint = HolstColors.PrimarySand
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
            // Distracting Apps Section
            item {
                SectionHeader(title = "DISTRACTING APPS (${state.distractingApps.size})")
            }
            if (state.distractingApps.isEmpty()) {
                item {
                    EmptySectionText("No distracting app limits configured.")
                }
            } else {
                items(state.distractingApps, key = { it.limit.packageName }) { item ->
                    AppLimitCard(
                        item = item,
                        onClick = { onNavigateToAppDetail(item.limit.packageName) },
                        onEdit = { viewModel.openEditor(item.limit) },
                        onToggle = { viewModel.toggleLimitEnabled(item.limit) }
                    )
                }
            }

            // Semi-Productive Apps Section
            item {
                SectionHeader(title = "SEMI-PRODUCTIVE APPS (${state.semiProductiveApps.size})")
            }
            if (state.semiProductiveApps.isEmpty()) {
                item {
                    EmptySectionText("No semi-productive limits configured.")
                }
            } else {
                items(state.semiProductiveApps, key = { it.limit.packageName }) { item ->
                    AppLimitCard(
                        item = item,
                        onClick = { onNavigateToAppDetail(item.limit.packageName) },
                        onEdit = { viewModel.openEditor(item.limit) },
                        onToggle = { viewModel.toggleLimitEnabled(item.limit) }
                    )
                }
            }

            // Other Apps Section
            if (state.otherApps.isNotEmpty()) {
                item {
                    SectionHeader(title = "OTHER APPS (${state.otherApps.size})")
                }
                items(state.otherApps, key = { it.limit.packageName }) { item ->
                    AppLimitCard(
                        item = item,
                        onClick = { onNavigateToAppDetail(item.limit.packageName) },
                        onEdit = { viewModel.openEditor(item.limit) },
                        onToggle = { viewModel.toggleLimitEnabled(item.limit) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Add / Edit Modal
    if (state.isEditorVisible) {
        LimitConfigurationModal(
            existingLimit = state.editingLimit,
            onDismiss = { viewModel.closeEditor() },
            onSave = { pkg, label, budget, passes, cat ->
                viewModel.saveLimit(pkg, label, budget, passes, cat)
            },
            onTurnOff = { limit ->
                viewModel.initiateTurnOffLimit(limit)
            }
        )
    }

    // 3-Second Hold Streak Warning Modal
    if (state.isStreakWarningVisible && state.streakWarningLimit != null) {
        HardTurnOffStreakWarningModal(
            limit = state.streakWarningLimit!!,
            streakDays = state.streakWarningDays,
            onDismiss = { viewModel.dismissStreakWarning() },
            onConfirmTurnOff = { pkg, duration ->
                viewModel.confirmTurnOffLimit(pkg, duration)
            }
        )
    }

    // Intervention Overlay Preview
    if (state.isInterventionOverlayVisible) {
        DistractedInterventionOverlay(
            appLabel = state.interventionAppLabel,
            spentMinutes = state.interventionSpentMinutes,
            limitMinutes = state.interventionLimitMinutes,
            onSelectDuration = { viewModel.dismissInterventionOverlay() },
            onExitApp = { viewModel.dismissInterventionOverlay() }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            color = HolstColors.MutedBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun EmptySectionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.MutedBlue),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun AppLimitCard(
    item: AppLimitItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    val progress = if (item.limit.dailyBudgetMinutes > 0) {
        (item.spentMinutes.toFloat() / item.limit.dailyBudgetMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isOverLimit = item.spentMinutes >= item.limit.dailyBudgetMinutes && item.limit.dailyBudgetMinutes > 0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_limit_card_${item.limit.packageName}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon Placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(HolstColors.Border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.limit.appLabel.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.PrimarySand,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.limit.appLabel,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "${item.spentMinutes}m spent / ${item.limit.dailyBudgetMinutes}m limit",
                        style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                    )
                }

                // Status Pill
                val pillBg = if (item.limit.isEnabled) HolstColors.AccentSage else HolstColors.Border
                val pillText = if (item.limit.isEnabled) "Active 🟢" else "Paused"

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pillBg,
                    modifier = Modifier.clickable { onToggle() }
                ) {
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (item.limit.isEnabled) HolstColors.Background else HolstColors.TextMuted,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverLimit) HolstColors.WarningTerracotta else HolstColors.AccentSage,
                trackColor = HolstColors.Border
            )
        }
    }
}

@Composable
fun LimitConfigurationModal(
    existingLimit: AppLimitEntity?,
    onDismiss: () -> Unit,
    onSave: (packageName: String, label: String, budget: Int, passes: Int, category: String) -> Unit,
    onTurnOff: (AppLimitEntity) -> Unit
) {
    var packageName by remember { mutableStateOf(existingLimit?.packageName ?: "") }
    var appLabel by remember { mutableStateOf(existingLimit?.appLabel ?: "") }
    var hours by remember { mutableIntStateOf((existingLimit?.dailyBudgetMinutes ?: 45) / 60) }
    var minutes by remember { mutableIntStateOf((existingLimit?.dailyBudgetMinutes ?: 45) % 60) }
    var extraPasses by remember { mutableIntStateOf(existingLimit?.extraTimePassesUsed ?: 0) }
    var selectedCategory by remember { mutableStateOf("DISTRACTING") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HolstColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingLimit == null) "Configure App Limit" else "Edit Limit",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HolstColors.TextMuted)
                    }
                }

                if (existingLimit == null) {
                    OutlinedTextField(
                        value = appLabel,
                        onValueChange = {
                            appLabel = it
                            if (packageName.isBlank()) packageName = "com.app.${it.lowercase().replace(" ", "")}"
                        },
                        label = { Text("App Name", color = HolstColors.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HolstColors.TextPrimary,
                            unfocusedTextColor = HolstColors.TextPrimary,
                            focusedBorderColor = HolstColors.PrimarySand,
                            unfocusedBorderColor = HolstColors.Border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name", color = HolstColors.TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HolstColors.TextPrimary,
                            unfocusedTextColor = HolstColors.TextPrimary,
                            focusedBorderColor = HolstColors.PrimarySand,
                            unfocusedBorderColor = HolstColors.Border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Time Budget Stepper
                Text(
                    text = "TIME BUDGET (MIN 15m)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HolstColors.MutedBlue,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hours", style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.TextMuted))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { if (hours > 0) hours-- }) {
                                Text("-", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                            }
                            Text(
                                text = "${hours}h",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = HolstColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            TextButton(onClick = { if (hours < 6) hours++ }) {
                                Text("+", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                            }
                        }
                    }

                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minutes", style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.TextMuted))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { if (minutes >= 5) minutes -= 5 }) {
                                Text("-", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                            }
                            Text(
                                text = "${minutes}m",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = HolstColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            TextButton(onClick = { if (minutes <= 50) minutes += 5 }) {
                                Text("+", style = MaterialTheme.typography.titleLarge.copy(color = HolstColors.PrimarySand))
                            }
                        }
                    }
                }

                // UsageStats Smart Suggestion Bar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HolstColors.Border.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            hours = 0
                            minutes = 45
                        }
                ) {
                    Text(
                        text = "💡 Suggested limit is 45m based on your 7-day average usage.",
                        style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.PrimarySand),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Extra Time Passes Allowance (0 to 3)
                Text(
                    text = "EXTRA TIME PASSES ALLOWANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HolstColors.MutedBlue,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..3).forEach { passCount ->
                        val isSelected = extraPasses == passCount
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) HolstColors.PrimarySand else HolstColors.Border,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { extraPasses = passCount }
                        ) {
                            Text(
                                text = "$passCount",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) HolstColors.Background else HolstColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Primary Save Button
                Button(
                    onClick = {
                        val totalBudget = (hours * 60 + minutes).coerceAtLeast(15)
                        onSave(packageName, appLabel, totalBudget, extraPasses, selectedCategory)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_limit_button")
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = HolstColors.Background,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Turn Off Limit (if existing)
                if (existingLimit != null) {
                    TextButton(
                        onClick = { onTurnOff(existingLimit) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Turn Off Limit",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = HolstColors.AlertCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HardTurnOffStreakWarningModal(
    limit: AppLimitEntity,
    streakDays: Int,
    onDismiss: () -> Unit,
    onConfirmTurnOff: (packageName: String, durationOption: String) -> Unit
) {
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var holdComplete by remember { mutableStateOf(false) }
    var selectedDuration by remember { mutableStateOf("REST_OF_DAY") }

    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress,
        label = "HoldProgress"
    )

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HolstColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.AlertCrimson),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STREAK WARNING",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HolstColors.AlertCrimson,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Streak Transition Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HolstColors.Border,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "🔥 $streakDays Days → 🌧️ 0 Days",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HolstColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Text(
                    text = "Turning off this limit forfeits your streak for ${limit.appLabel}. Are you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!holdComplete) {
                    Text(
                        text = "Press and hold for 3 seconds to unlock:",
                        style = MaterialTheme.typography.labelSmall.copy(color = HolstColors.MutedBlue)
                    )

                    // 3-Second Hold Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HolstColors.Border)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHolding = true
                                        val job = coroutineScope.launch {
                                            val stepTime = 50L
                                            val steps = 3000L / stepTime
                                            for (i in 1..steps) {
                                                delay(stepTime)
                                                holdProgress = i.toFloat() / steps.toFloat()
                                            }
                                            holdComplete = true
                                        }
                                        tryAwaitRelease()
                                        job.cancel()
                                        isHolding = false
                                        if (!holdComplete) {
                                            holdProgress = 0f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Progress fill
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(HolstColors.AlertCrimson.copy(alpha = 0.5f))
                                .align(Alignment.CenterStart)
                        )

                        Text(
                            text = if (isHolding) "Hold... (${(animatedProgress * 100).toInt()}%)" else "Hold to Disable (3s)",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = HolstColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    // Duration Selection
                    Text(
                        text = "SELECT DURATION:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HolstColors.MutedBlue,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    val options = listOf(
                        "REST_OF_DAY" to "Rest of the Day",
                        "TILL_TOMORROW" to "Till Tomorrow Morning (6:00 AM)",
                        "SEVEN_DAYS" to "7 Days (Vacation / Sick Mode)"
                    )

                    options.forEach { (key, label) ->
                        val isSelected = selectedDuration == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) HolstColors.AlertCrimson.copy(alpha = 0.3f) else HolstColors.Border,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, HolstColors.AlertCrimson) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDuration = key }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSelected) HolstColors.AlertCrimson else HolstColors.TextPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onConfirmTurnOff(limit.packageName, selectedDuration) },
                        colors = ButtonDefaults.buttonColors(containerColor = HolstColors.AlertCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Confirm Turn Off",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium.copy(color = HolstColors.MutedBlue)
                    )
                }
            }
        }
    }
}

@Composable
fun DistractedInterventionOverlay(
    appLabel: String,
    spentMinutes: Int,
    limitMinutes: Int,
    onSelectDuration: (Int) -> Unit,
    onExitApp: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HolstColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HolstColors.PrimarySand),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "[$appLabel — ${limitMinutes}m Daily Limit]",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "$spentMinutes m / $limitMinutes m Spent Today",
                    style = MaterialTheme.typography.bodySmall.copy(color = HolstColors.MutedBlue)
                )

                Text(
                    text = "Intentional Usage Verification:\nHow long do you need this app for?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = HolstColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // 2x2 Time Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InterventionGridPill(minutes = 5, modifier = Modifier.weight(1f)) { onSelectDuration(5) }
                        InterventionGridPill(minutes = 10, modifier = Modifier.weight(1f)) { onSelectDuration(10) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InterventionGridPill(minutes = 20, modifier = Modifier.weight(1f)) { onSelectDuration(20) }
                        InterventionGridPill(minutes = 40, modifier = Modifier.weight(1f)) { onSelectDuration(40) }
                    }
                }

                // Exit Button
                Button(
                    onClick = onExitApp,
                    colors = ButtonDefaults.buttonColors(containerColor = HolstColors.PrimarySand),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Exit $appLabel (Home)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = HolstColors.Background,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InterventionGridPill(
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HolstColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HolstColors.Border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = "$minutes mins",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = HolstColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .padding(vertical = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

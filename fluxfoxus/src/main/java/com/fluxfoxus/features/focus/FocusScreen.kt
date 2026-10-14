package com.fluxfoxus.features.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxfoxus.core.database.entity.PresetEntity
import com.fluxfoxus.core.designsystem.component.FlipClock
import com.fluxfoxus.core.designsystem.component.StopFocusingModal
import com.fluxfoxus.core.designsystem.theme.FluxTheme
import com.fluxfoxus.core.service.TimerState

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateToCreatePreset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val timerState = uiState.timerState

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FluxTheme.colors.background)
    ) {
        if (timerState is TimerState.Running || timerState is TimerState.Paused) {
            // --- ACTIVE FLIP CLOCK SESSION VIEW ---
            ActiveSessionView(
                uiState = uiState,
                onTakeBreak = { viewModel.takeBreak(context) },
                onPause = { viewModel.pauseSession(context) },
                onResume = { viewModel.resumeSession(context) },
                onStop = { viewModel.showStopModal() },
                onFinishStopwatch = { viewModel.finishStopwatchSession(context) }
            )
        } else {
            // --- PRESET SELECTION & MODE SETUP VIEW ---
            PresetSetupView(
                uiState = uiState,
                onSelectPreset = { viewModel.selectPreset(it) },
                onSelectMode = { viewModel.selectMode(it) },
                onSelectDuration = { viewModel.selectDuration(it) },
                onStart = { viewModel.startFocusSession(context) },
                onNavigateToCreatePreset = onNavigateToCreatePreset
            )
        }

        // Asymmetric Friction Modal
        if (uiState.showStopModal) {
            StopFocusingModal(
                currentStreakDays = uiState.streak?.currentStreak ?: 0,
                distractionMinutesToday = 0,
                onKeepGoing = { viewModel.dismissStopModal() },
                onConfirmQuit = { viewModel.confirmQuit(context) }
            )
        }
    }
}

@Composable
private fun ActiveSessionView(
    uiState: FocusUiState,
    onTakeBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onFinishStopwatch: () -> Unit
) {
    val timerState = uiState.timerState
    val isRunning = timerState is TimerState.Running
    val isPaused = timerState is TimerState.Paused

    val remainingSeconds = when (timerState) {
        is TimerState.Running -> timerState.remainingSeconds
        is TimerState.Paused -> timerState.remainingSeconds
        else -> 0
    }

    val isBreak = (timerState as? TimerState.Running)?.isBreak == true
    val breaksRemaining = (timerState as? TimerState.Running)?.breaksRemaining ?: 0
    val totalBreaks = (timerState as? TimerState.Running)?.totalBreaks ?: 2
    val mode = (timerState as? TimerState.Running)?.timerMode ?: "COUNTDOWN"

    val hours = (remainingSeconds / 3600).toString().padStart(2, '0')
    val minutes = ((remainingSeconds % 3600) / 60).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preset Badge
            val activePreset = uiState.presets.find { it.id == uiState.selectedPresetId }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FluxTheme.colors.surface)
                    .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${activePreset?.emoji ?: "⏳"} ${activePreset?.name ?: "Focus Session"}",
                    style = FluxTheme.typography.bodyMedium,
                    color = FluxTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Mode / Break Indicator
            val headerStatus = when {
                isBreak -> "BREAK IN PROGRESS"
                isPaused -> "PAUSED"
                else -> if (mode == "STOPWATCH") "STOPWATCH" else "DEEP WORK"
            }
            Text(
                text = headerStatus,
                color = if (isBreak) FluxTheme.colors.accent else FluxTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Center: Mechanical Flip Clock
        FlipClock(
            hours = hours,
            minutes = minutes,
            isBreakMode = isBreak
        )

        // Bottom Controls: Two-Row Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Break (75%) + Pause (25%)
            if (!isBreak) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Take Break Button (75%)
                    val canBreak = breaksRemaining > 0
                    Box(
                        modifier = Modifier
                            .weight(0.75f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FluxTheme.colors.surface)
                            .border(1.dp, if (canBreak) FluxTheme.colors.accent else FluxTheme.colors.border, RoundedCornerShape(24.dp))
                            .clickable(enabled = canBreak) { onTakeBreak() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (canBreak) "Take Break ($breaksRemaining/$totalBreaks)" else "No Breaks Left",
                            color = if (canBreak) FluxTheme.colors.accent else FluxTheme.colors.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Pause Button (25%)
                    Box(
                        modifier = Modifier
                            .weight(0.25f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FluxTheme.colors.surface)
                            .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(24.dp))
                            .clickable { if (isPaused) onResume() else onPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPaused) "▶" else "⏸",
                            color = FluxTheme.colors.primary,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Row 2: Stop Focusing / Finish Session (100% full width)
            val isStopwatch = mode == "STOPWATCH"
            Button(
                onClick = { if (isStopwatch) onFinishStopwatch() else onStop() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStopwatch) FluxTheme.colors.primary else FluxTheme.colors.surface,
                    contentColor = if (isStopwatch) FluxTheme.colors.background else FluxTheme.colors.textMuted
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(
                        1.dp,
                        if (isStopwatch) FluxTheme.colors.primary else FluxTheme.colors.border,
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Text(
                    text = if (isStopwatch) "Finish Session" else "Stop Focusing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PresetSetupView(
    uiState: FocusUiState,
    onSelectPreset: (String) -> Unit,
    onSelectMode: (String) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onStart: () -> Unit,
    onNavigateToCreatePreset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Focus & Presets",
                    style = FluxTheme.typography.headlineMedium,
                    color = FluxTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onNavigateToCreatePreset) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = FluxTheme.colors.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "New", color = FluxTheme.colors.primary, fontWeight = FontWeight.Bold)
                }
            }

            // Presets List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.presets, key = { it.id }) { preset ->
                    val isSelected = preset.id == uiState.selectedPresetId
                    PresetCardItem(
                        preset = preset,
                        isSelected = isSelected,
                        onSelect = { onSelectPreset(preset.id) }
                    )
                }
            }
        }

        // Floating Island Mode Selector & CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(FluxTheme.colors.surface)
                    .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ModeChip("Timer", uiState.selectedMode == "COUNTDOWN") { onSelectMode("COUNTDOWN") }
                ModeChip("Stopwatch", uiState.selectedMode == "STOPWATCH") { onSelectMode("STOPWATCH") }
                ModeChip("From-To", uiState.selectedMode == "FROM_TO") { onSelectMode("FROM_TO") }
            }

            // Quick Duration Chips (if Timer Mode)
            if (uiState.selectedMode == "COUNTDOWN") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 45, 60, 90, 120).forEach { mins ->
                        val isSelected = uiState.selectedDurationMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) FluxTheme.colors.accent else FluxTheme.colors.surface)
                                .border(1.dp, if (isSelected) FluxTheme.colors.accent else FluxTheme.colors.border, RoundedCornerShape(8.dp))
                                .clickable { onSelectDuration(mins) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (isSelected) FluxTheme.colors.background else FluxTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Start Focusing Button
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FluxTheme.colors.primary,
                    contentColor = FluxTheme.colors.background
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Text(text = "Start Focusing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ModeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) FluxTheme.colors.accent else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) FluxTheme.colors.background else FluxTheme.colors.textMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PresetCardItem(
    preset: PresetEntity,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FluxTheme.colors.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) FluxTheme.colors.primary else FluxTheme.colors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = preset.emoji, fontSize = 24.sp)
                Column {
                    Text(
                        text = preset.name,
                        color = FluxTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${preset.breakCount} breaks • ${preset.breakDurationMinutes}m • YouTube: ${preset.youtubeMode}",
                        color = FluxTheme.colors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Radio Indicator Dot
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isSelected) FluxTheme.colors.primary else FluxTheme.colors.border, CircleShape)
                    .padding(3.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(FluxTheme.colors.primary)
                    )
                }
            }
        }
    }
}

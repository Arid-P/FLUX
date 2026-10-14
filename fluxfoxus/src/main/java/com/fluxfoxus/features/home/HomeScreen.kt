package com.fluxfoxus.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxfoxus.core.designsystem.theme.FluxTheme
import com.fluxfoxus.core.service.TimerState

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fluxfoxus.core.designsystem.component.RusticMedleyHeader

val TerracottaDistracting = Color(0xFFE07A5F)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToFocus: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Lifecycle-aware refresh: Trigger usage refresh when returning to app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshUsageStats()
    }

    Scaffold(
        containerColor = FluxTheme.colors.background,
        topBar = {
            RusticMedleyHeader(
                title = "FluxFoxus",
                streakDays = uiState.currentStreakDays,
                isStreakDormant = uiState.isStreakDormant,
                onSettingsClick = onNavigateToSettings,
                actions = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(FluxTheme.colors.surface)
                            .border(1.dp, FluxTheme.colors.accent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${uiState.dailyFocusAverageMinutes / 60}h ${uiState.dailyFocusAverageMinutes % 60}m",
                            color = FluxTheme.colors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. Upcoming Session Alert (if scheduled in <= 15 min)
            if (uiState.upcomingSessionTitle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FluxTheme.colors.surface)
                        .border(1.dp, FluxTheme.colors.primary, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Text(
                            text = "${uiState.upcomingSessionTitle} starts in ${uiState.upcomingMinutesRemaining} min",
                            color = FluxTheme.colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 3. Momentum Card (Last 24h)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FluxTheme.colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(16.dp))
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
                        Text(
                            text = "Momentum",
                            style = FluxTheme.typography.titleMedium,
                            color = FluxTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Last 24h",
                            style = FluxTheme.typography.labelSmall,
                            color = FluxTheme.colors.textMuted
                        )
                    }

                    // Stacked Momentum Bar / Area Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        val total = (uiState.productiveMinutes24h + uiState.semiProductiveMinutes24h +
                                uiState.distractingMinutes24h + uiState.otherMinutes24h).coerceAtLeast(1)

                        val prodWidth = size.width * (uiState.productiveMinutes24h.toFloat() / total)
                        val semiWidth = size.width * (uiState.semiProductiveMinutes24h.toFloat() / total)
                        val distWidth = size.width * (uiState.distractingMinutes24h.toFloat() / total)
                        val otherWidth = size.width * (uiState.otherMinutes24h.toFloat() / total)

                        var currentX = 0f
                        drawRect(color = Color(0xFF778D7A), topLeft = Offset(currentX, 0f), size = Size(prodWidth, size.height))
                        currentX += prodWidth
                        drawRect(color = Color(0xFFD4C4A8), topLeft = Offset(currentX, 0f), size = Size(semiWidth, size.height))
                        currentX += semiWidth
                        drawRect(color = TerracottaDistracting, topLeft = Offset(currentX, 0f), size = Size(distWidth, size.height))
                        currentX += distWidth
                        drawRect(color = Color(0xFF415A77), topLeft = Offset(currentX, 0f), size = Size(otherWidth, size.height))
                    }

                    // 2x2 Legend Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem("Productive", "${uiState.productiveMinutes24h}m", FluxTheme.colors.accent)
                        LegendItem("Semi-Prod", "${uiState.semiProductiveMinutes24h}m", FluxTheme.colors.primary)
                        LegendItem("Distracting", "${uiState.distractingMinutes24h}m", TerracottaDistracting)
                        LegendItem("Others", "${uiState.otherMinutes24h}m", FluxTheme.colors.border)
                    }
                }
            }

            // 4. Session Quick-Launcher Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FluxTheme.colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Today's Focus: ${uiState.todayFocusMinutes / 60}h ${uiState.todayFocusMinutes % 60}m",
                        color = FluxTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val preset = uiState.activePreset
                    if (preset != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = preset.emoji, fontSize = 24.sp)
                            Text(
                                text = preset.name,
                                color = FluxTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            text = "25m focus • ${preset.breakCount} breaks (${preset.breakDurationMinutes}m each)",
                            color = FluxTheme.colors.textMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 5. Primary Action CTA
            val isRunning = uiState.timerState is TimerState.Running
            Button(
                onClick = {
                    if (isRunning) {
                        onNavigateToFocus()
                    } else {
                        viewModel.startOrResumeFocus(context)
                        onNavigateToFocus()
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) FluxTheme.colors.accent else FluxTheme.colors.primary,
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
                    Text(
                        text = if (isRunning) "Resume Session" else "Start Focusing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = "$label: $value", fontSize = 11.sp, color = FluxTheme.colors.textMuted)
    }
}

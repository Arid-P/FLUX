package com.fluxfoxus.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fluxfoxus.core.designsystem.theme.FluxTheme
import kotlinx.coroutines.delay

/**
 * Stop-Focusing Friction Modal
 *
 * Implements the mandatory 15-25s asymmetric lock modal:
 * - [ Keep Going ] is ALWAYS free and immediately interactive in Muted Sage Green (#778D7A).
 * - [ Quit Session ] is strictly locked behind a countdown delay:
 *   Wait Seconds = clamp(15 + floor(distractionMinutes / 15), 15, 25).
 * - Outside tap dismissal is disabled.
 */
@Composable
fun StopFocusingModal(
    currentStreakDays: Int,
    distractionMinutesToday: Int = 0,
    onKeepGoing: () -> Unit,
    onConfirmQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Delay formula: clamp(15 + floor(distractionMinutes / 15), 15, 25)
    val totalDelaySeconds = remember(distractionMinutesToday) {
        val extraSeconds = distractionMinutesToday / 15
        (15 + extraSeconds).coerceIn(15, 25)
    }

    var secondsRemaining by remember { mutableIntStateOf(totalDelaySeconds) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
    }

    val progress by animateFloatAsState(
        targetValue = secondsRemaining.toFloat() / totalDelaySeconds.toFloat(),
        label = "DelayProgress"
    )

    val isQuitUnlocked = secondsRemaining == 0

    Dialog(
        onDismissRequest = { /* Dismiss disabled by spec */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // Dimmed scrim: Abyssal Navy at 85% opacity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FluxTheme.colors.background.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = RoundedCornerShape(16.dp)

            Column(
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .clip(cardShape)
                    .background(FluxTheme.colors.surface)
                    .border(1.dp, FluxTheme.colors.border, cardShape)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Header Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FluxTheme.colors.background)
                        .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚠️ Penalty Alert",
                        color = FluxTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Surrender Focus?",
                    style = FluxTheme.typography.headlineMedium,
                    color = FluxTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (currentStreakDays > 0) {
                        "Stopping now will forfeit your $currentStreakDays-day streak and log a premature quit penalty."
                    } else {
                        "Stopping now will log a premature quit penalty on your record."
                    },
                    style = FluxTheme.typography.bodyMedium,
                    color = FluxTheme.colors.textMuted,
                    textAlign = TextAlign.Center
                )

                // Circular Progress Indicator with countdown seconds
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = FluxTheme.colors.primary,
                        trackColor = FluxTheme.colors.border,
                        strokeWidth = 3.dp
                    )

                    Text(
                        text = if (isQuitUnlocked) "UNLOCKED" else "${secondsRemaining}s",
                        color = if (isQuitUnlocked) FluxTheme.colors.errorDanger else FluxTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isQuitUnlocked) 13.sp else 22.sp
                    )
                }

                // Asymmetric Buttons: Keep Going (Always Unlocked) vs Quit Session (Locked)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // [ Keep Going ] - ALWAYS FREE & UNLOCKED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FluxTheme.colors.accent)
                            .clickable { onKeepGoing() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keep Going",
                            color = FluxTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // [ Quit Session ] - LOCKED BEHIND DELAY
                    val quitModifier = if (isQuitUnlocked) {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FluxTheme.colors.surface)
                            .border(1.5.dp, FluxTheme.colors.errorDanger, RoundedCornerShape(24.dp))
                            .clickable { onConfirmQuit() }
                    } else {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FluxTheme.colors.surface)
                            .border(1.dp, FluxTheme.colors.border, RoundedCornerShape(24.dp))
                            .alpha(0.3f)
                    }

                    Box(
                        modifier = quitModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isQuitUnlocked) "Quit Session" else "Quit (${secondsRemaining}s)",
                            color = if (isQuitUnlocked) FluxTheme.colors.errorDanger else FluxTheme.colors.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

package com.fluxfoxus.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Locked Brand Identity Tokens (Theme/BRAND_IDENTITY.md - Rustic Medley)
val RiverStyxBackground = Color(0xFF13191F)
val CarbonFibreSurface = Color(0xFF2B2F2E)
val AfternoonTeaBorder = Color(0xFF594C3D)
val AmberAutumnAccent = Color(0xFFCA9C68)
val GhostWhiteText = Color(0xFFF8FAFC)
val TannedWoodMuted = Color(0xFF906D4B)

/**
 * Custom 2D Flat Rustic Medley Header.
 * Insets-aware (WindowInsets.statusBars / statusBarsPadding), zero default black bars,
 * perfectly aligned title, streak/status chip, and settings action.
 */
@Composable
fun RusticMedleyHeader(
    title: String,
    streakDays: Int? = null,
    isStreakDormant: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(RiverStyxBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title + Streak Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = GhostWhiteText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                if (streakDays != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CarbonFibreSurface)
                            .border(
                                1.dp,
                                if (!isStreakDormant) AmberAutumnAccent else AfternoonTeaBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("streak_header_chip")
                    ) {
                        Text(
                            text = if (!isStreakDormant) "🔥 $streakDays" else "💤 0",
                            color = if (!isStreakDormant) AmberAutumnAccent else TannedWoodMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: Actions / Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (actions != null) {
                    actions()
                }

                if (onSettingsClick != null) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AmberAutumnAccent
                        )
                    }
                }
            }
        }
    }
}

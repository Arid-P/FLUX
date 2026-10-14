package com.fluxfoxus.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fluxfoxus.core.designsystem.theme.FluxTheme
import com.fluxfoxus.core.designsystem.theme.TabularFlipClockTextStyle

/**
 * Mechanical Flip Clock Composable
 *
 * Implements tactile split-flap cards for Hours (HH) and Minutes (MM)
 * with 3D rotation physics, 1.5dp hairline divider, and Holst theme colors.
 *
 * @param hours Formatted hours string (e.g. "01")
 * @param minutes Formatted minutes string (e.g. "45")
 * @param isBreakMode When true, text and divider shift to Muted Sage Green (#778D7A)
 * @param modifier Optional modifier
 */
@Composable
fun FlipClock(
    hours: String,
    minutes: String,
    isBreakMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val digitColor = if (isBreakMode) FluxTheme.colors.accent else FluxTheme.colors.textPrimary
    val dividerColor = if (isBreakMode) FluxTheme.colors.accent else FluxTheme.colors.primary

    Column(
        modifier = modifier
            .fillMaxWidth(0.72f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hours Card (HH)
        FlipCard(
            text = hours.padStart(2, '0'),
            digitColor = digitColor,
            dividerColor = dividerColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        // Minutes Card (MM)
        FlipCard(
            text = minutes.padStart(2, '0'),
            digitColor = digitColor,
            dividerColor = dividerColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

/**
 * Split flap card replicating mechanical station clock flap.
 */
@Composable
fun FlipCard(
    text: String,
    digitColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    var currentText by remember { mutableStateOf(text) }
    var previousText by remember { mutableStateOf(text) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(text) {
        if (text != currentText) {
            previousText = currentText
            currentText = text
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
    }

    val density = LocalDensity.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(FluxTheme.colors.surface)
            .border(1.dp, FluxTheme.colors.border, shape),
        contentAlignment = Alignment.Center
    ) {
        // Base content (upcoming / current text displayed on upper & lower static halves)
        val displayText = if (rotation.value >= 90f) currentText else previousText
        val nextText = if (rotation.value >= 90f) previousText else currentText

        // Static lower card (shows new text on bottom half when flipping)
        FlapHalf(
            text = if (rotation.value >= 90f) currentText else previousText,
            digitColor = digitColor,
            isTopHalf = false,
            modifier = Modifier.fillMaxSize()
        )

        // Static upper card (shows upcoming new text on top half)
        FlapHalf(
            text = if (rotation.value >= 90f) currentText else nextText,
            digitColor = digitColor,
            isTopHalf = true,
            modifier = Modifier.fillMaxSize()
        )

        // 3D Rotating Flap
        if (rotation.value in 0.1f..179.9f) {
            val isFirstHalf = rotation.value < 90f
            val flapAngle = if (isFirstHalf) rotation.value else rotation.value - 180f
            val flapText = if (isFirstHalf) previousText else currentText

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationX = -flapAngle
                        cameraDistance = 12f * density.density
                        transformOrigin = if (isFirstHalf) {
                            TransformOrigin(0.5f, 1.0f) // Hinged at bottom of upper flap
                        } else {
                            TransformOrigin(0.5f, 0.0f) // Hinged at top of lower flap
                        }
                    }
            ) {
                FlapHalf(
                    text = flapText,
                    digitColor = digitColor,
                    isTopHalf = isFirstHalf,
                    modifier = Modifier.fillMaxSize()
                )

                // Simulated dynamic shadow gradient during rotation
                val shadowAlpha = ((rotation.value % 90f) / 90f) * 0.45f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = shadowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Horizontal 1.5dp hairline split divider in Warm Sand Khaki
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(dividerColor)
        )
    }
}

/**
 * Clips the numeric text to either the top or bottom half of the flap.
 */
@Composable
private fun FlapHalf(
    text: String,
    digitColor: Color,
    isTopHalf: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawWithContent {
            val halfHeight = size.height / 2f
            val clipRect = if (isTopHalf) {
                Rect(Offset.Zero, Size(size.width, halfHeight))
            } else {
                Rect(Offset(0f, halfHeight), Size(size.width, halfHeight))
            }
            val path = Path().apply { addRect(clipRect) }
            clipPath(path) {
                this@drawWithContent.drawContent()
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TabularFlipClockTextStyle,
            color = digitColor,
            textAlign = TextAlign.Center
        )
    }
}

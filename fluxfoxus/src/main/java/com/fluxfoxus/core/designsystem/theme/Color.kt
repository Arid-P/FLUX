package com.fluxfoxus.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * FluxFoxus Design System: "Holst" (Холст) Authoritative Palette
 * Sourced directly from Theme/Холст.jpeg and documentation/00_CORE/FLUX_DESIGN_TOKENS.md
 */
val HolstBackground = Color(0xFF0D1B2A)     // Abyssal Navy: Master screen background. Deep, tranquil, anti-glare canvas.
val HolstSurface = Color(0xFF1B263B)        // Oxford Slate: Cards, bottom sheets, navigation bar, dialog backdrops, flip flap tiles.
val HolstBorder = Color(0xFF415A77)         // Muted Steel Blue: 1dp card borders, list dividers, hairline separators.
val HolstAccent = Color(0xFF778D7A)         // Muted Sage Green: Secondary badges, break timers, streak badges, secondary status pills.
val HolstPrimary = Color(0xFFD4C4A8)        // Warm Sand Khaki: Primary action buttons ("Start Focusing"), active countdown digits, flip-clock split line, active tab pills.
val HolstTextPrimary = Color(0xFFF4F1DE)    // Eggshell Cream: High-contrast text, primary headers, active time digits without harsh glare.
val HolstTextMuted = Color(0xFF8E9CAE)      // Slate Blue-Grey: Secondary labels, disabled action states, timestamps.
val HolstErrorDanger = Color(0xFFDC2626)    // Crimson Alert: Destructive actions, quit confirmation warnings, expired intervention indicators.

@Immutable
data class FluxFoxusColors(
    val background: Color = HolstBackground,
    val surface: Color = HolstSurface,
    val border: Color = HolstBorder,
    val accent: Color = HolstAccent,
    val primary: Color = HolstPrimary,
    val textPrimary: Color = HolstTextPrimary,
    val textMuted: Color = HolstTextMuted,
    val errorDanger: Color = HolstErrorDanger
)

val LocalFluxFoxusColors = staticCompositionLocalOf { FluxFoxusColors() }

object HolstColors {
    val Background = HolstBackground
    val Surface = HolstSurface
    val Border = HolstBorder
    val AccentSage = HolstAccent
    val PrimarySand = HolstPrimary
    val TextPrimary = HolstTextPrimary
    val TextMuted = HolstTextMuted
    val MutedBlue = HolstTextMuted
    val AlertCrimson = HolstErrorDanger
    val WarningTerracotta = Color(0xFFE07A5F)
}

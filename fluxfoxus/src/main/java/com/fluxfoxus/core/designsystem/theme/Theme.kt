package com.fluxfoxus.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val DarkColorScheme = darkColorScheme(
    primary = HolstPrimary,
    onPrimary = HolstBackground,
    secondary = HolstAccent,
    onSecondary = HolstBackground,
    background = HolstBackground,
    onBackground = HolstTextPrimary,
    surface = HolstSurface,
    onSurface = HolstTextPrimary,
    outline = HolstBorder,
    error = HolstErrorDanger,
    onError = HolstTextPrimary
)

object FluxTheme {
    val colors: FluxFoxusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFluxFoxusColors.current

    val typography = FluxFoxusTypography
}

@Composable
fun FluxFoxusTheme(
    content: @Composable () -> Unit
) {
    val colors = FluxFoxusColors()

    CompositionLocalProvider(
        LocalFluxFoxusColors provides colors
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = FluxFoxusTypography,
            content = content
        )
    }
}

package com.twinscalev4.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = FoxOrange,
    onPrimary = CreamWhite,
    secondary = FoxOrangeDark,
    onSecondary = CreamWhite,
    background = CreamWhite,
    onBackground = DeepBlack,
    surface = ColorTokens.LightSurface,
    onSurface = DeepBlack,
    surfaceContainer = ColorTokens.LightContainer,
    surfaceContainerHigh = ColorTokens.LightContainerHigh,
    surfaceContainerHighest = ColorTokens.LightContainerHighest
)

private val DarkColors = darkColorScheme(
    primary = FoxOrange,
    onPrimary = DeepBlack,
    secondary = FoxOrangeDark,
    onSecondary = CreamWhite,
    background = DeepBlack,
    onBackground = CreamWhite,
    surface = NightCard,
    onSurface = CreamWhite,
    surfaceContainer = ColorTokens.DarkContainer,
    surfaceContainerHigh = ColorTokens.DarkContainerHigh,
    surfaceContainerHighest = ColorTokens.DarkContainerHighest
)

@Composable
fun TwinScaleTheme(content: @Composable () -> Unit) {
    val darkTheme = true
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

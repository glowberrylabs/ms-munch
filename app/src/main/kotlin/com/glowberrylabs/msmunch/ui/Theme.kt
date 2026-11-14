package com.glowberrylabs.msmunch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

private val ArcadeColors = darkColorScheme(
    primary = GamePalette.Muncher,
    onPrimary = GamePalette.Background,
    secondary = GamePalette.Dot,
    onSecondary = GamePalette.Background,
    tertiary = GamePalette.Wall,
    onTertiary = GamePalette.Background,
    background = GamePalette.Background,
    onBackground = GamePalette.Ink,
    surface = GamePalette.Surface,
    onSurface = GamePalette.Ink,
    surfaceVariant = GamePalette.Surface,
    onSurfaceVariant = GamePalette.Muted,
    error = GamePalette.Muncher,
    onError = GamePalette.Background
)

private val BaseTypography = Typography()

private val ArcadeTypography = Typography(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Black),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Black),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Black),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.Bold)
)

@Composable
fun MsMunchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArcadeColors,
        typography = ArcadeTypography,
        content = content
    )
}

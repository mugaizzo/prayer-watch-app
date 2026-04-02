package com.prayerwatch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors

private val PrayerWatchColorPalette = Colors(
    primary = PrimaryGreen,
    primaryVariant = PrimaryGreenDark,
    secondary = AccentGold,
    background = BackgroundBlack,
    surface = SurfaceDark,
    error = ErrorRed,
    onPrimary = BackgroundBlack,
    onSecondary = BackgroundBlack,
    onBackground = OnSurface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    onError = BackgroundBlack
)

@Composable
fun PrayerWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = PrayerWatchColorPalette,
        content = content
    )
}

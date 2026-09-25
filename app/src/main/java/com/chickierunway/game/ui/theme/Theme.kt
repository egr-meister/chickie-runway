package com.chickierunway.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ChickieColors = lightColorScheme(
    primary = BeakOrange,
    onPrimary = Cream,
    primaryContainer = SunYolk,
    onPrimaryContainer = NightNavy,
    secondary = SunYolk,
    onSecondary = NightNavy,
    tertiary = GrassGreen,
    onTertiary = NightNavy,
    background = SkyBlue,
    onBackground = NightNavy,
    surface = Charcoal,
    onSurface = Cream,
    surfaceVariant = Asphalt,
    onSurfaceVariant = Cream,
    error = WrongRed,
    onError = Cream,
    outline = SunYolk
)

/** The game always uses its own day-time palette (art is designed for it). */
@Composable
fun ChickieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChickieColors,
        typography = ChickieTypography,
        content = content
    )
}

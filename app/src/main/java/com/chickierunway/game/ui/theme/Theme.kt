package com.chickierunway.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Day: the art as drawn. Night: same plates and panels over a darkened sky. */
private fun dayColors(accent: AccentColor) = lightColorScheme(
    primary = BeakOrange,
    onPrimary = Cream,
    primaryContainer = accent.main,
    onPrimaryContainer = NightNavy,
    secondary = accent.main,
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
    outline = accent.main
)

private fun nightColors(accent: AccentColor) = darkColorScheme(
    primary = accent.main,
    onPrimary = NightNavy,
    primaryContainer = accent.deep,
    onPrimaryContainer = NightNavy,
    secondary = accent.main,
    onSecondary = NightNavy,
    tertiary = GrassGreen,
    onTertiary = NightNavy,
    background = NightSky,
    onBackground = Cream,
    surface = NightNavy,
    onSurface = Cream,
    surfaceVariant = Charcoal,
    onSurfaceVariant = Cream,
    error = WrongRed,
    onError = Cream,
    outline = accent.main
)

@Composable
fun isChickieDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ChickieTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentColor = AccentColor.GOLD,
    content: @Composable () -> Unit
) {
    val dark = isChickieDark(themeMode)
    val palette = ChickiePalette(
        accent = accent.main,
        accentDeep = accent.deep,
        isDark = dark,
        backdropDim = if (dark) 0.5f else 0f
    )
    CompositionLocalProvider(LocalChickiePalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) nightColors(accent) else dayColors(accent),
            typography = ChickieTypography,
            content = content
        )
    }
}

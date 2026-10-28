package com.chickierunway.game.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * "Sunny Road Dash" palette (sampled from the game art).
 *  SunYolk       #FFC629  titles, logo gold, borders, progress fill, highlights  (= accent)
 *  SunYolkDeep   #F2A900  pressed/secondary gold, chip borders                     (= accent, deep)
 *  BeakOrange    #E56F01  plate frame orange, accents
 *  BeakOrangeDeep#B55400  shadows under gold text
 *  SkyBlue       #4DAEFD  sky, splash background, timer chip
 *  Asphalt       #3F3C47  road, panel gradient top
 *  AsphaltLight  #4A4E5A  panel gradient top (lighter)
 *  Charcoal      #2C3039  plate centre, card back
 *  NightNavy     #171C2F  panel gradient bottom, text outline
 *  Cream         #FFF8E7  body text on dark, card faces
 *  GrassGreen    #719108  decoration
 *  CombRed       #E53935  hero comb, accents
 *  CorrectGreen  #157F45  correct / cleared (always paired with ✓ text)
 *  WrongRed      #C62F2F  wrong / low time (always paired with ✗ / "Hurry!")
 *  LockedGray    #6C7086  locked levels (paired with 🔒 + "Locked")
 *
 * The gold is the player's accent colour: SunYolk / SunYolkDeep read the accent chosen in
 * Settings, so every title, border and progress bar follows it.
 */
val SunYolkBase = Color(0xFFFFC629)
val SunYolkDeepBase = Color(0xFFF2A900)
val BeakOrange = Color(0xFFE56F01)
val BeakOrangeDeep = Color(0xFFB55400)
val SkyBlue = Color(0xFF4DAEFD)
val Asphalt = Color(0xFF3F3C47)
val AsphaltLight = Color(0xFF4A4E5A)
val Charcoal = Color(0xFF2C3039)
val NightNavy = Color(0xFF171C2F)
val NightSky = Color(0xFF0E1324)
val Cream = Color(0xFFFFF8E7)
val GrassGreen = Color(0xFF719108)
val CombRed = Color(0xFFE53935)
val CorrectGreen = Color(0xFF157F45)
val WrongRed = Color(0xFFC62F2F)
val LockedGray = Color(0xFF6C7086)
val Scrim = Color(0xB3171C2F)

/** Light / dark / follow the system. Stored by [key]; unknown keys fall back to SYSTEM. */
enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun from(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Accent colours offered in Settings. Stored by [key]; unknown keys fall back to GOLD. */
enum class AccentColor(val key: String, val main: Color, val deep: Color) {
    GOLD("gold", SunYolkBase, SunYolkDeepBase),
    SKY("sky", Color(0xFF7CCBFF), Color(0xFF3A9BE0)),
    MEADOW("meadow", Color(0xFFB4DE57), Color(0xFF7DAA24)),
    BERRY("berry", Color(0xFFFF8A9A), Color(0xFFE0566A));

    companion object {
        fun from(key: String?): AccentColor = entries.firstOrNull { it.key == key } ?: GOLD
    }
}

/** What the game's own components read: accent, and how much night falls over the art. */
@Immutable
data class ChickiePalette(
    val accent: Color,
    val accentDeep: Color,
    val isDark: Boolean,
    /** Extra navy dim over the background art (0 in light, a night sky in dark). */
    val backdropDim: Float
)

val LocalChickiePalette = staticCompositionLocalOf {
    ChickiePalette(SunYolkBase, SunYolkDeepBase, isDark = false, backdropDim = 0f)
}

/** The current accent. */
val SunYolk: Color
    @Composable @ReadOnlyComposable get() = LocalChickiePalette.current.accent

/** The current accent, deep shade. */
val SunYolkDeep: Color
    @Composable @ReadOnlyComposable get() = LocalChickiePalette.current.accentDeep

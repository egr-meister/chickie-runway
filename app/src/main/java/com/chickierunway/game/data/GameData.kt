package com.chickierunway.game.data

import kotlinx.serialization.Serializable

/**
 * Everything the game persists, stored as ONE JSON string in DataStore Preferences.
 * Every field has a default so older/newer JSON always decodes (forward/backward compatible).
 *
 * quizBest:  quiz category id -> best score (0..10)
 * pairsBest: level number as string ("1".."9") -> best matched pairs in that level
 */
@Serializable
data class GameData(
    val quizBest: Map<String, Int> = emptyMap(),
    val pairsBest: Map<String, Int> = emptyMap(),
    val settings: Settings = Settings()
) {
    /** Drops impossible values that could come from a hand-edited or damaged payload. */
    fun sanitized(): GameData = copy(
        quizBest = quizBest.filter { (key, value) -> key.isNotBlank() && value >= 0 }
            .mapValues { (_, value) -> value.coerceAtMost(MAX_QUIZ_SCORE) },
        pairsBest = pairsBest.filter { (key, value) -> key.toIntOrNull() != null && value >= 0 }
            .mapValues { (_, value) -> value.coerceAtMost(MAX_PAIRS) }
    )

    companion object {
        const val MAX_QUIZ_SCORE = 10
        const val MAX_PAIRS = 12
    }
}

/**
 * App settings. themeMode / accent are stored as stable string keys ("system" | "light" | "dark",
 * "gold" | "sky" | "meadow" | "berry"); unknown values fall back to the defaults when read.
 * The language is NOT here: AppCompatDelegate is its single source of truth.
 */
@Serializable
data class Settings(
    val soundEnabled: Boolean = true,
    val themeMode: String = "system",
    val accent: String = "gold"
)

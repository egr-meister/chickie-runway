package com.chickierunway.game.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. Every argument has a default so a damaged/missing
// argument decodes to an "invalid" value that the screen turns into a friendly fallback.

@Serializable data object MainRoute
@Serializable data object MenuRoute
@Serializable data object QuizCategoriesRoute
@Serializable data class QuizRoute(val categoryId: String = "")
@Serializable data class QuizResultRoute(val categoryId: String = "", val score: Int = -1, val total: Int = 0)
@Serializable data object LevelsRoute
@Serializable data class PairsRoute(val level: Int = 0)
@Serializable data class PairsResultRoute(
    val level: Int = 0,
    val won: Boolean = false,
    val matched: Int = -1,
    val total: Int = 0
)
@Serializable data object SummaryRoute
@Serializable data object SettingsRoute
@Serializable data object RulesRoute

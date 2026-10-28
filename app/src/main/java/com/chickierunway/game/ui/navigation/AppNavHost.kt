package com.chickierunway.game.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.chickierunway.game.ChickieApp
import com.chickierunway.game.data.QuizBank
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.ui.components.ErrorFallback
import com.chickierunway.game.ui.screens.LevelsScreen
import com.chickierunway.game.ui.screens.MainScreen
import com.chickierunway.game.ui.screens.MenuScreen
import com.chickierunway.game.ui.screens.PairsResultScreen
import com.chickierunway.game.ui.screens.PairsScreen
import com.chickierunway.game.ui.screens.QuizCategoryScreen
import com.chickierunway.game.ui.screens.QuizResultScreen
import com.chickierunway.game.ui.screens.QuizScreen
import com.chickierunway.game.ui.screens.RulesScreen
import com.chickierunway.game.ui.screens.SettingsScreen
import com.chickierunway.game.ui.screens.SummaryScreen
import com.chickierunway.game.ui.viewmodel.PairsViewModel
import com.chickierunway.game.ui.viewmodel.ProgressViewModel
import com.chickierunway.game.ui.viewmodel.QuizViewModel

/** Decodes typed args; any failure (process recreation with damaged state, etc.) yields null. */
private inline fun <reified T : Any> NavBackStackEntry.routeOrNull(): T? =
    try {
        toRoute<T>()
    } catch (e: Exception) {
        null
    }

/** Navigate only if [from] is still the visible entry — prevents double taps during transitions. */
private fun NavController.navigateFrom(
    from: NavBackStackEntry,
    route: Any,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.id != from.id) return
    try {
        navigate(route) {
            launchSingleTop = true
            builder()
        }
    } catch (e: Exception) {
        // Never crash on navigation; stay on the current screen.
    }
}

/** Pop only if [from] is visible and there is something below it (never empties the stack). */
private fun NavController.popFrom(from: NavBackStackEntry) {
    if (currentBackStackEntry?.id != from.id) return
    if (previousBackStackEntry == null) return
    popBackStack()
}

/** Jump to Menu, clearing everything above Main (Main is the start destination, always present). */
private fun NavController.menuFrom(from: NavBackStackEntry) {
    navigateFrom(from, MenuRoute) {
        popUpTo<MainRoute> { inclusive = false }
    }
}

@Composable
fun AppNavHost(app: ChickieApp) {
    val nav = rememberNavController()
    val progressFactory = remember(app) { ProgressViewModel.Factory(app.repository) }

    NavHost(
        navController = nav,
        startDestination = MainRoute,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) }
    ) {
        composable<MainRoute> { entry ->
            MainScreen(onStart = { nav.navigateFrom(entry, MenuRoute) })
        }

        composable<MenuRoute> { entry ->
            val vm: ProgressViewModel = viewModel(factory = progressFactory)
            MenuScreen(
                viewModel = vm,
                onQuiz = { nav.navigateFrom(entry, QuizCategoriesRoute) },
                onPairs = { nav.navigateFrom(entry, LevelsRoute) },
                onResults = { nav.navigateFrom(entry, SummaryRoute) },
                onSettings = { nav.navigateFrom(entry, SettingsRoute) },
                onBack = { nav.popFrom(entry) }
            )
        }

        composable<QuizCategoriesRoute> { entry ->
            val vm: ProgressViewModel = viewModel(factory = progressFactory)
            QuizCategoryScreen(
                viewModel = vm,
                onCategory = { id -> nav.navigateFrom(entry, QuizRoute(id)) },
                onBack = { nav.popFrom(entry) }
            )
        }

        composable<QuizRoute> { entry ->
            val category = QuizBank.category(entry.routeOrNull<QuizRoute>()?.categoryId)
            if (category == null || category.questions.isEmpty()) {
                ErrorFallback(
                    message = stringResource(R.string.error_quiz_missing),
                    onBack = { nav.popFrom(entry) }
                )
            } else {
                val vm: QuizViewModel = viewModel(
                    factory = QuizViewModel.Factory(app.repository, app.soundManager, category)
                )
                QuizScreen(
                    viewModel = vm,
                    onFinished = { score, total ->
                        nav.navigateFrom(entry, QuizResultRoute(category.id, score, total)) {
                            popUpTo<QuizRoute> { inclusive = true }
                        }
                    },
                    onMenu = { nav.menuFrom(entry) }
                )
            }
        }

        composable<QuizResultRoute> { entry ->
            val route = entry.routeOrNull<QuizResultRoute>()
            val category = QuizBank.category(route?.categoryId)
            if (route == null || category == null || route.total <= 0 || route.score !in 0..route.total) {
                ErrorFallback(
                    message = stringResource(R.string.error_quiz_result),
                    onBack = { nav.popFrom(entry) }
                )
            } else {
                val vm: ProgressViewModel = viewModel(factory = progressFactory)
                QuizResultScreen(
                    viewModel = vm,
                    category = category,
                    score = route.score,
                    total = route.total,
                    onContinue = { nav.popFrom(entry) },
                    onRestart = {
                        nav.navigateFrom(entry, QuizRoute(category.id)) {
                            popUpTo<QuizResultRoute> { inclusive = true }
                        }
                    },
                    onMenu = { nav.menuFrom(entry) }
                )
            }
        }

        composable<LevelsRoute> { entry ->
            val vm: ProgressViewModel = viewModel(factory = progressFactory)
            LevelsScreen(
                viewModel = vm,
                onLevel = { level -> nav.navigateFrom(entry, PairsRoute(level)) },
                onRules = { nav.navigateFrom(entry, RulesRoute) },
                onBack = { nav.popFrom(entry) }
            )
        }

        composable<PairsRoute> { entry ->
            val config = GameLogic.levelConfig(entry.routeOrNull<PairsRoute>()?.level ?: 0)
            if (config == null) {
                ErrorFallback(
                    message = stringResource(R.string.error_level_missing),
                    onBack = { nav.popFrom(entry) }
                )
            } else {
                val vm: PairsViewModel = viewModel(
                    factory = PairsViewModel.Factory(app.repository, app.soundManager, config)
                )
                PairsScreen(
                    viewModel = vm,
                    onFinished = { won, matched, total ->
                        nav.navigateFrom(entry, PairsResultRoute(config.level, won, matched, total)) {
                            popUpTo<PairsRoute> { inclusive = true }
                        }
                    },
                    onMenu = { nav.menuFrom(entry) }
                )
            }
        }

        composable<PairsResultRoute> { entry ->
            val route = entry.routeOrNull<PairsResultRoute>()
            val config = GameLogic.levelConfig(route?.level ?: 0)
            if (route == null || config == null || route.matched !in 0..config.pairs) {
                ErrorFallback(
                    message = stringResource(R.string.error_pairs_result),
                    onBack = { nav.popFrom(entry) }
                )
            } else {
                val vm: ProgressViewModel = viewModel(factory = progressFactory)
                val next = GameLogic.nextLevel(config.level)
                PairsResultScreen(
                    viewModel = vm,
                    level = config.level,
                    won = route.won,
                    matched = route.matched,
                    total = config.pairs,
                    hasNextLevel = route.won && next != null,
                    onContinue = {
                        if (route.won && next != null) {
                            nav.navigateFrom(entry, PairsRoute(next)) {
                                popUpTo<PairsResultRoute> { inclusive = true }
                            }
                        } else {
                            nav.popFrom(entry)
                        }
                    },
                    onRestart = {
                        nav.navigateFrom(entry, PairsRoute(config.level)) {
                            popUpTo<PairsResultRoute> { inclusive = true }
                        }
                    },
                    onMenu = { nav.menuFrom(entry) }
                )
            }
        }

        composable<SummaryRoute> { entry ->
            val vm: ProgressViewModel = viewModel(factory = progressFactory)
            SummaryScreen(viewModel = vm, onBack = { nav.popFrom(entry) })
        }

        composable<SettingsRoute> { entry ->
            val vm: ProgressViewModel = viewModel(factory = progressFactory)
            SettingsScreen(viewModel = vm, onBack = { nav.popFrom(entry) })
        }

        composable<RulesRoute> { entry ->
            RulesScreen(onBack = { nav.popFrom(entry) })
        }
    }
}

package com.chickierunway.game.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chickierunway.game.data.GameData
import com.chickierunway.game.data.GameRepository
import com.chickierunway.game.logic.GameLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class LevelUi(
    val level: Int,
    val unlocked: Boolean,
    val cleared: Boolean,
    val bestPairs: Int,
    val totalPairs: Int
)

@Immutable
data class ProgressUiState(
    val isLoaded: Boolean = false,
    val quizBest: Map<String, Int> = emptyMap(),
    val pairsBest: Map<String, Int> = emptyMap(),
    val soundEnabled: Boolean = true,
    val levels: List<LevelUi> = ProgressViewModel.buildLevels(emptyMap()),
    val completedLevels: Int = 0
)

/** Read-mostly progress for Menu / Quiz categories / Levels / Results / Settings / result panels. */
class ProgressViewModel(private val repository: GameRepository) : ViewModel() {

    val state: StateFlow<ProgressUiState> = repository.gameData
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun resetAll() {
        viewModelScope.launch { repository.resetAll() }
    }

    private fun GameData.toUiState(): ProgressUiState = ProgressUiState(
        isLoaded = true,
        quizBest = quizBest,
        pairsBest = pairsBest,
        soundEnabled = settings.soundEnabled,
        levels = buildLevels(pairsBest),
        completedLevels = GameLogic.completedLevelsCount(pairsBest)
    )

    companion object {
        fun buildLevels(pairsBest: Map<String, Int>): List<LevelUi> = GameLogic.levels.map { cfg ->
            LevelUi(
                level = cfg.level,
                unlocked = GameLogic.isLevelUnlocked(pairsBest, cfg.level),
                cleared = GameLogic.isLevelCleared(pairsBest, cfg.level),
                bestPairs = GameLogic.bestPairs(pairsBest, cfg.level).coerceAtMost(cfg.pairs),
                totalPairs = cfg.pairs
            )
        }
    }

    class Factory(private val repository: GameRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
                "Unknown ViewModel class ${modelClass.name}"
            }
            return ProgressViewModel(repository) as T
        }
    }
}

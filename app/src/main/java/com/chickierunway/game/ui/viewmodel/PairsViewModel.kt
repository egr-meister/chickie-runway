package com.chickierunway.game.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chickierunway.game.audio.Sfx
import com.chickierunway.game.audio.SoundManager
import com.chickierunway.game.data.GameRepository
import com.chickierunway.game.logic.GameLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PairsStatus { PLAYING, WON, LOST }

@Immutable
data class PairCardUi(
    val id: Int,
    val itemIndex: Int,
    val faceUp: Boolean = false,
    val matched: Boolean = false
)

@Immutable
data class PairsUiState(
    val level: Int,
    val columns: Int,
    val totalPairs: Int,
    val totalSeconds: Int,
    val cards: List<PairCardUi>,
    val matchedPairs: Int = 0,
    val secondsLeft: Int,
    val isPaused: Boolean = false,
    val status: PairsStatus = PairsStatus.PLAYING
) {
    val isLowTime: Boolean get() = secondsLeft <= 10
}

class PairsViewModel(
    private val repository: GameRepository,
    private val sound: SoundManager,
    private val config: GameLogic.LevelConfig
) : ViewModel() {

    private val _state = MutableStateFlow(newGame())
    val state: StateFlow<PairsUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var mismatchJob: Job? = null
    private var firstPickId: Int? = null
    private var resolvingMismatch = false

    init {
        startTimer()
    }

    private fun newGame(): PairsUiState = PairsUiState(
        level = config.level,
        columns = config.columns,
        totalPairs = config.pairs,
        totalSeconds = config.seconds,
        cards = GameLogic.buildDeck(config.pairs).map { PairCardUi(id = it.id, itemIndex = it.itemIndex) },
        secondsLeft = config.seconds
    )

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val current = _state.value
                if (current.status != PairsStatus.PLAYING) break
                if (current.isPaused) continue
                val left = current.secondsLeft - 1
                if (left <= 0) {
                    _state.value = current.copy(secondsLeft = 0)
                    finish(won = false)
                    break
                }
                _state.value = current.copy(secondsLeft = left)
            }
        }
    }

    fun onCardTapped(cardId: Int) {
        val current = _state.value
        if (current.status != PairsStatus.PLAYING || current.isPaused || resolvingMismatch) return
        val card = current.cards.firstOrNull { it.id == cardId } ?: return
        if (card.faceUp || card.matched) return

        val firstId = firstPickId
        if (firstId == null) {
            firstPickId = cardId
            sound.play(Sfx.CLICK)
            _state.value = current.copy(cards = current.cards.flipUp(cardId))
            return
        }

        val first = current.cards.firstOrNull { it.id == firstId }
        firstPickId = null
        if (first == null) {
            _state.value = current.copy(cards = current.cards.flipUp(cardId))
            firstPickId = cardId
            return
        }

        val isMatch = GameLogic.isMatch(
            GameLogic.DeckCard(first.id, first.itemIndex),
            GameLogic.DeckCard(card.id, card.itemIndex)
        )
        if (isMatch) {
            val cards = current.cards.map {
                if (it.id == first.id || it.id == card.id) it.copy(faceUp = true, matched = true) else it
            }
            val matched = current.matchedPairs + 1
            _state.value = current.copy(cards = cards, matchedPairs = matched)
            if (matched >= current.totalPairs) {
                finish(won = true)
            } else {
                sound.play(Sfx.MATCH)
            }
        } else {
            sound.play(Sfx.CLICK)
            _state.value = current.copy(cards = current.cards.flipUp(cardId))
            resolvingMismatch = true
            mismatchJob?.cancel()
            mismatchJob = viewModelScope.launch {
                delay(MISMATCH_DELAY_MS)
                _state.first { !it.isPaused }
                _state.update { s ->
                    s.copy(cards = s.cards.map { c -> if (c.matched) c else c.copy(faceUp = false) })
                }
                resolvingMismatch = false
            }
        }
    }

    private fun finish(won: Boolean) {
        val current = _state.value
        if (current.status != PairsStatus.PLAYING) return
        timerJob?.cancel()
        _state.value = current.copy(status = if (won) PairsStatus.WON else PairsStatus.LOST, isPaused = false)
        sound.play(if (won) Sfx.WIN else Sfx.LOSE)
        viewModelScope.launch { repository.savePairsBest(config.level, current.matchedPairs) }
    }

    fun pause() {
        _state.update { if (it.status == PairsStatus.PLAYING) it.copy(isPaused = true) else it }
    }

    fun resume() {
        _state.update { it.copy(isPaused = false) }
    }

    fun restart() {
        timerJob?.cancel()
        mismatchJob?.cancel()
        firstPickId = null
        resolvingMismatch = false
        _state.value = newGame()
        startTimer()
    }

    private fun List<PairCardUi>.flipUp(id: Int): List<PairCardUi> =
        map { if (it.id == id) it.copy(faceUp = true) else it }

    companion object {
        const val MISMATCH_DELAY_MS = 800L
    }

    class Factory(
        private val repository: GameRepository,
        private val sound: SoundManager,
        private val config: GameLogic.LevelConfig
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PairsViewModel::class.java)) {
                "Unknown ViewModel class ${modelClass.name}"
            }
            return PairsViewModel(repository, sound, config) as T
        }
    }
}

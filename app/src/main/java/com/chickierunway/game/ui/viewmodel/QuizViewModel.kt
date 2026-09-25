package com.chickierunway.game.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chickierunway.game.audio.Sfx
import com.chickierunway.game.audio.SoundManager
import com.chickierunway.game.data.GameRepository
import com.chickierunway.game.data.QuizBank
import com.chickierunway.game.data.QuizCategory
import com.chickierunway.game.data.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class QuizUiState(
    val categoryId: String,
    val categoryTitle: String,
    val questionIndex: Int,
    val total: Int,
    val question: QuizQuestion,
    val selectedIndex: Int? = null,
    val score: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false
) {
    val questionNumber: Int get() = questionIndex + 1
    val isAnswered: Boolean get() = selectedIndex != null

    /** Progress counts the current question once it is answered. */
    val progress: Float
        get() = if (total == 0) 0f else (questionIndex + if (isAnswered) 1 else 0).toFloat() / total
}

class QuizViewModel(
    private val repository: GameRepository,
    private val sound: SoundManager,
    private val category: QuizCategory
) : ViewModel() {

    private val questions: List<QuizQuestion> = category.questions
        .filter { it.options.size >= 2 && it.correctIndex in it.options.indices }
        .take(QuizBank.QUESTIONS_PER_CATEGORY)

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var advanceJob: Job? = null

    private fun initialState(): QuizUiState = QuizUiState(
        categoryId = category.id,
        categoryTitle = category.title,
        questionIndex = 0,
        total = questions.size,
        question = questions.firstOrNull() ?: QuizQuestion("", emptyList(), 0),
        isFinished = questions.isEmpty()
    )

    fun answer(optionIndex: Int) {
        val current = _state.value
        if (current.isPaused || current.isFinished || current.isAnswered) return
        if (optionIndex !in current.question.options.indices) return

        val correct = optionIndex == current.question.correctIndex
        sound.play(if (correct) Sfx.CORRECT else Sfx.WRONG)
        _state.value = current.copy(
            selectedIndex = optionIndex,
            score = current.score + if (correct) 1 else 0
        )
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(FEEDBACK_DELAY_MS)
            _state.first { !it.isPaused } // hold the feedback while paused
            advance()
        }
    }

    private suspend fun advance() {
        val current = _state.value
        val next = current.questionIndex + 1
        if (next >= questions.size) {
            repository.saveQuizBest(category.id, current.score)
            _state.value = current.copy(isFinished = true)
        } else {
            _state.value = current.copy(
                questionIndex = next,
                question = questions[next],
                selectedIndex = null
            )
        }
    }

    fun pause() {
        _state.update { if (it.isFinished) it else it.copy(isPaused = true) }
    }

    fun resume() {
        _state.update { it.copy(isPaused = false) }
    }

    fun restart() {
        advanceJob?.cancel()
        advanceJob = null
        _state.value = initialState()
    }

    companion object {
        const val FEEDBACK_DELAY_MS = 750L
    }

    class Factory(
        private val repository: GameRepository,
        private val sound: SoundManager,
        private val category: QuizCategory
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                "Unknown ViewModel class ${modelClass.name}"
            }
            return QuizViewModel(repository, sound, category) as T
        }
    }
}

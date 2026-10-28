package com.chickierunway.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameProgressBar
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.IconPlateButton
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.PanelActions
import com.chickierunway.game.ui.components.PanelOverlay
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.components.PlateButton
import com.chickierunway.game.ui.theme.CorrectGreen
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.theme.WrongRed
import com.chickierunway.game.ui.viewmodel.QuizViewModel

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onFinished: (score: Int, total: Int) -> Unit,
    onMenu: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Back while playing opens the pause overlay; Back on the overlay continues.
    BackHandler(enabled = !state.isFinished) {
        if (state.isPaused) viewModel.resume() else viewModel.pause()
    }
    // Auto-pause when the app goes to the background.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished(state.score, state.total)
    }

    val answered = state.selectedIndex != null
    val answeredRight = answered && state.selectedIndex == state.question.correctIndex
    // The mascots under the answers react to the last answer.
    val parade = when {
        !answered -> listOf(Assets.mascotA, Assets.mascotD)
        answeredRight -> listOf(Assets.mascotB, Assets.mascotA)
        else -> listOf(Assets.mascotC, Assets.mascotD)
    }

    GameBackground(blurred = true) {
        FillScrollColumn {
            GameTopBar(
                title = stringResource(R.string.quiz_screen_title, stringResource(state.categoryTitle)),
                onBack = viewModel::pause,
                backDescription = stringResource(R.string.action_pause_options),
                trailing = {
                    IconPlateButton(icon = Assets.iconPause, contentDescription = stringResource(R.string.action_pause), onClick = viewModel::pause)
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GameProgressBar(progress = state.progress, modifier = Modifier.weight(1f))
                    Spacer(Modifier.size(12.dp))
                    InfoChip(
                        text = stringResource(R.string.score_fraction, state.questionNumber, state.total),
                        description = stringResource(R.string.quiz_question_cd, state.questionNumber, state.total)
                    )
                }

                // 0 is the placeholder question of an empty category, which never reaches the screen.
                QuestionCard(text = if (state.question.text != 0) stringResource(state.question.text) else "")

                val correctLabel = stringResource(R.string.quiz_answer_correct_cd)
                val wrongLabel = stringResource(R.string.quiz_answer_wrong_cd)
                state.question.options.forEachIndexed { index, option ->
                    val isCorrect = index == state.question.correctIndex
                    val isSelected = index == state.selectedIndex
                    val (tint, prefix, stateText) = when {
                        answered && isCorrect -> Triple(CorrectGreen, "✓ ", correctLabel)
                        answered && isSelected -> Triple(WrongRed, "✗ ", wrongLabel)
                        else -> Triple(null, "", null)
                    }
                    PlateButton(
                        text = prefix + stringResource(option),
                        onClick = { viewModel.answer(index) },
                        tint = tint,
                        dimmed = answered && !isCorrect && !isSelected,
                        stateText = stateText,
                        playClickSound = false,
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    if (answered) {
                        Text(
                            text = stringResource(if (answeredRight) R.string.quiz_feedback_right else R.string.quiz_feedback_wrong),
                            color = Cream,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NightNavy.copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            }

            // Fills whatever space is left under the answers with our characters.
            MascotParade(
                mascots = parade,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
        }

        if (state.isPaused && !state.isFinished) {
            PanelOverlay(title = stringResource(R.string.paused), mascot = Assets.mascotA) {
                PanelText(stringResource(R.string.quiz_paused_status, state.questionNumber, state.total, state.score))
                PanelActions(
                    primaryLabel = stringResource(R.string.action_continue),
                    onPrimary = viewModel::resume,
                    onRestart = viewModel::restart,
                    onMenu = onMenu
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Cream)
            .border(BorderStroke(3.dp, SunYolk), shape)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NightNavy,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}

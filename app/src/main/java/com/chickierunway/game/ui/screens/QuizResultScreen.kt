package com.chickierunway.game.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.data.QuizCategory
import com.chickierunway.game.ui.components.CenteredScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameProgressBar
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.PanelActions
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.components.PanelWithMascot
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

@Composable
fun QuizResultScreen(
    viewModel: ProgressViewModel,
    category: QuizCategory,
    score: Int,
    total: Int,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val best = maxOf(state.quizBest[category.id] ?: 0, score)
    val (verdict, mascot) = when {
        score == total -> "Perfect run — a true farm champion!" to Assets.mascotB
        score * 10 >= total * 7 -> "Great run! The coop is proud." to Assets.mascotA
        score * 2 >= total -> "Nice try — keep running!" to Assets.mascotD
        else -> "Bumpy road. Have another go!" to Assets.mascotC
    }

    GameBackground(blurred = true) {
        CenteredScrollColumn(modifier = Modifier.padding(20.dp)) {
            PanelWithMascot(
                mascot = mascot,
                mascotDescription = "Chickie reacting to your score",
                title = "Quiz Complete",
                mascotSize = 150.dp
            ) {
                InfoChip(text = "${category.title}  ${difficultyStars(category.difficulty)}")
                Text(
                    text = "Score: $score/$total",
                    color = SunYolk,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                GameProgressBar(progress = if (total == 0) 0f else score.toFloat() / total)
                PanelText(verdict)
                PanelText("Best: $best/$total", style = MaterialTheme.typography.titleMedium)
                PanelActions(
                    primaryLabel = "Continue",
                    onPrimary = onContinue,
                    onRestart = onRestart,
                    onMenu = onMenu
                )
            }
        }
    }
}

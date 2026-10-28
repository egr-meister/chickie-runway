package com.chickierunway.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.data.QuizBank
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.Assets
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameProgressBar
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.ModalPanel
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.theme.Asphalt
import com.chickierunway.game.ui.theme.CorrectGreen
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.LockedGray
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

/** "Trip Log": best score per quiz difficulty + matching-pairs levels cleared. */
@Composable
fun SummaryScreen(viewModel: ProgressViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameBackground(dim = 0.05f) {
        FillScrollColumn {
            GameTopBar(title = stringResource(R.string.results_title), onBack = onBack)
            ModalPanel(title = stringResource(R.string.summary_trip_log), modifier = Modifier.padding(horizontal = 18.dp)) {
                Text(stringResource(R.string.summary_quiz_header), color = SunYolk, style = MaterialTheme.typography.titleMedium)
                QuizBank.categories.forEach { category ->
                    val best = state.quizBest[category.id] ?: 0
                    val total = QuizBank.QUESTIONS_PER_CATEGORY
                    val title = stringResource(category.title)
                    val rowDescription = stringResource(R.string.summary_quiz_cd, title, best, total)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {
                                contentDescription = rowDescription
                            }
                    ) {
                        Image(
                            painter = painterResource(difficultyMascot(category.difficulty)),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "$title  ${difficultyStars(category.difficulty)}",
                                    color = Cream,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(stringResource(R.string.score_fraction, best, total), color = SunYolk, style = MaterialTheme.typography.titleMedium)
                            }
                            GameProgressBar(progress = best.toFloat() / total)
                        }
                    }
                }

                Text(stringResource(R.string.pairs_title), color = SunYolk, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.summary_cleared, state.completedLevels, GameLogic.LEVEL_COUNT),
                    color = Cream,
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.levels.forEach { level ->
                        val levelDescription = stringResource(
                            when {
                                level.cleared -> R.string.summary_level_cleared_cd
                                level.unlocked -> R.string.summary_level_open_cd
                                else -> R.string.summary_level_locked_cd
                            },
                            level.level
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        level.cleared -> CorrectGreen
                                        level.unlocked -> Asphalt
                                        else -> LockedGray
                                    }
                                )
                                .clearAndSetSemantics {
                                    contentDescription = levelDescription
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (level.cleared) "✓" else level.level.toString(),
                                color = Cream,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                PanelText(stringResource(R.string.summary_legend), style = MaterialTheme.typography.bodyMedium)
            }
            MascotParade(
                mascots = listOf(Assets.mascotA, Assets.mascotB, Assets.mascotD),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }
}

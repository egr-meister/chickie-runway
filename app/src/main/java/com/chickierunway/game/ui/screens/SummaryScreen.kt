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
            GameTopBar(title = "Results", onBack = onBack)
            ModalPanel(title = "Trip Log", modifier = Modifier.padding(horizontal = 18.dp)) {
                Text("Farm Quiz — best scores", color = SunYolk, style = MaterialTheme.typography.titleMedium)
                QuizBank.categories.forEach { category ->
                    val best = state.quizBest[category.id] ?: 0
                    val total = QuizBank.QUESTIONS_PER_CATEGORY
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {
                                contentDescription = "${category.title} quiz: best $best of $total"
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
                                    "${category.title}  ${difficultyStars(category.difficulty)}",
                                    color = Cream,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("$best/$total", color = SunYolk, style = MaterialTheme.typography.titleMedium)
                            }
                            GameProgressBar(progress = best.toFloat() / total)
                        }
                    }
                }

                Text("Matching Pairs", color = SunYolk, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Cleared ${state.completedLevels} / ${GameLogic.LEVEL_COUNT}",
                    color = Cream,
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.levels.forEach { level ->
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
                                    contentDescription = "Level ${level.level}: " + when {
                                        level.cleared -> "cleared"
                                        level.unlocked -> "open"
                                        else -> "locked"
                                    }
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
                PanelText("✓ = cleared · number = open or locked", style = MaterialTheme.typography.bodyMedium)
            }
            MascotParade(
                mascots = listOf(Assets.mascotA, Assets.mascotB, Assets.mascotD),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }
}

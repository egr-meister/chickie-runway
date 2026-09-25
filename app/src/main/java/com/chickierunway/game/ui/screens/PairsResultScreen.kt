package com.chickierunway.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.ui.components.CenteredScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameProgressBar
import com.chickierunway.game.ui.components.PanelActions
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.components.PanelWithMascot
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

@Composable
fun PairsResultScreen(
    viewModel: ProgressViewModel,
    level: Int,
    won: Boolean,
    matched: Int,
    total: Int,
    hasNextLevel: Boolean,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val best = maxOf(GameLogic.bestPairs(state.pairsBest, level), matched).coerceAtMost(total)

    GameBackground(blurred = true) {
        CenteredScrollColumn(modifier = Modifier.padding(20.dp)) {
            PanelWithMascot(
                mascot = if (won) Assets.mascotB else Assets.mascotC,
                mascotDescription = if (won) "Chickie cheering" else "Chickie surprised by the clock",
                title = if (won) "Well Done" else "Out of Time",
                mascotSize = 150.dp
            ) {
                PanelText("Level $level")
                Text(
                    text = "Pairs: $matched/$total",
                    color = SunYolk,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                GameProgressBar(progress = if (total == 0) 0f else matched.toFloat() / total)
                if (won) {
                    // A little finish line: flag, cone and traffic light.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(0, 4, 7).forEach {
                            Image(painter = painterResource(Assets.item(it)), contentDescription = null, modifier = Modifier.size(44.dp))
                        }
                    }
                }
                PanelText("Best: $best/$total", style = MaterialTheme.typography.titleMedium)
                PanelText(
                    when {
                        won && hasNextLevel -> "Level ${level + 1} is unlocked. Continue to keep running!"
                        won -> "All ${GameLogic.LEVEL_COUNT} levels cleared — you own the road!"
                        else -> "The clock ran out. Restart to try again."
                    }
                )
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

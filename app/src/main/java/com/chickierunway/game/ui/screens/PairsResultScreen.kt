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
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
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
                mascotDescription = stringResource(if (won) R.string.cd_mascot_cheering else R.string.cd_mascot_clock),
                title = stringResource(if (won) R.string.pairs_won else R.string.pairs_lost),
                mascotSize = 150.dp
            ) {
                PanelText(stringResource(R.string.pairs_level_title, level))
                Text(
                    text = stringResource(R.string.pairs_result_pairs, matched, total),
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
                PanelText(stringResource(R.string.result_best, best, total), style = MaterialTheme.typography.titleMedium)
                PanelText(
                    when {
                        won && hasNextLevel -> stringResource(R.string.pairs_next_unlocked, level + 1)
                        won -> stringResource(R.string.pairs_all_cleared, GameLogic.LEVEL_COUNT)
                        else -> stringResource(R.string.pairs_time_up)
                    }
                )
                PanelActions(
                    primaryLabel = stringResource(R.string.action_continue),
                    onPrimary = onContinue,
                    onRestart = onRestart,
                    onMenu = onMenu
                )
            }
        }
    }
}

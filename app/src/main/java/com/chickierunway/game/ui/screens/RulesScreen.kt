package com.chickierunway.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chickierunway.game.Assets
import com.chickierunway.game.R
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.PanelWithMascot
import com.chickierunway.game.ui.components.PlateButton
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.SunYolk

@Composable
fun RulesScreen(onBack: () -> Unit) {
    GameBackground(blurred = true) {
        FillScrollColumn {
            GameTopBar(title = stringResource(R.string.rules_title), onBack = onBack)
            PanelWithMascot(
                mascot = Assets.mascotA,
                mascotDescription = null,
                title = stringResource(R.string.rules_how_to_play),
                mascotSize = 110.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                RuleSection(
                    stringResource(R.string.pairs_title),
                    listOf(
                        stringResource(R.string.rules_pairs_1),
                        stringResource(R.string.rules_pairs_2),
                        stringResource(R.string.rules_pairs_3),
                        stringResource(R.string.rules_pairs_4, GameLogic.LEVEL_COUNT),
                        stringResource(R.string.rules_pairs_5)
                    )
                )
                // Example pair
                val exampleName = stringResource(Assets.itemName(5))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(Assets.item(5)),
                        contentDescription = stringResource(R.string.rules_example_cd, exampleName),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("+", color = SunYolk, style = MaterialTheme.typography.headlineSmall)
                    Image(painterResource(Assets.item(5)), contentDescription = exampleName, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.rules_pair_equals), color = Cream, style = MaterialTheme.typography.titleMedium)
                }
                RuleSection(
                    stringResource(R.string.quiz_title),
                    listOf(
                        stringResource(R.string.rules_quiz_1),
                        stringResource(R.string.rules_quiz_2),
                        stringResource(R.string.rules_quiz_3),
                        stringResource(R.string.rules_quiz_4)
                    )
                )
                PlateButton(text = stringResource(R.string.action_got_it), onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f))
            }
            MascotParade(
                mascots = listOf(Assets.mascotD, Assets.mascotC),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun RuleSection(title: String, lines: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = SunYolk,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        lines.forEach { line ->
            Text(
                text = stringResource(R.string.bullet_line, line),
                color = Cream,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

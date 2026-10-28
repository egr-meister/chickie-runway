package com.chickierunway.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.data.QuizBank
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.IconPlateButton
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.MenuPlateButton
import com.chickierunway.game.ui.theme.SkyBlue
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

/** Menu: small logo, live progress chips, four framed "road sign" buttons, mascots below. */
@Composable
fun MenuScreen(
    viewModel: ProgressViewModel,
    onQuiz: () -> Unit,
    onPairs: () -> Unit,
    onResults: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val quizTotal = QuizBank.categories.sumOf { state.quizBest[it.id] ?: 0 }
    val quizMax = QuizBank.categories.size * QuizBank.QUESTIONS_PER_CATEGORY

    GameBackground(dim = 0.05f) {
        FillScrollColumn {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconPlateButton(icon = Assets.iconBack, contentDescription = stringResource(R.string.cd_back_to_start), onClick = onBack)
            }

            Image(
                painter = painterResource(Assets.logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.5f).widthIn(max = 260.dp).heightIn(max = 170.dp)
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoChip(
                    text = stringResource(R.string.menu_quiz_chip, quizTotal, quizMax),
                    description = stringResource(R.string.menu_quiz_chip_cd, quizTotal, quizMax)
                )
                InfoChip(
                    text = stringResource(R.string.pairs_chip, state.completedLevels, GameLogic.LEVEL_COUNT),
                    textColor = SkyBlue,
                    borderColor = SkyBlue,
                    description = stringResource(R.string.menu_pairs_chip_cd, state.completedLevels, GameLogic.LEVEL_COUNT)
                )
            }
            Spacer(Modifier.height(18.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuPlateButton(text = stringResource(R.string.menu_quiz), onClick = onQuiz, leadingIcon = Assets.item(12), widthFraction = 0.74f)
                MenuPlateButton(
                    text = stringResource(R.string.pairs_title),
                    onClick = onPairs,
                    leadingIcon = Assets.item(4),
                    widthFraction = 0.74f,
                    textStyle = MaterialTheme.typography.titleLarge
                )
                MenuPlateButton(text = stringResource(R.string.results_title), onClick = onResults, leadingIcon = Assets.item(0), widthFraction = 0.74f)
                MenuPlateButton(text = stringResource(R.string.settings_title), onClick = onSettings, leadingIcon = Assets.item(10), widthFraction = 0.74f)
            }

            MascotParade(
                mascots = listOf(Assets.mascotB, Assets.mascotA),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }
}

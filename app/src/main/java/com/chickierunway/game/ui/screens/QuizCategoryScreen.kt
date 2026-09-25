package com.chickierunway.game.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.data.QuizBank
import com.chickierunway.game.data.QuizCategory
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.OutlinedTitle
import com.chickierunway.game.ui.components.PlateButton
import com.chickierunway.game.ui.theme.Asphalt
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

/** Mascot shown for each difficulty (easy = happy, medium = running, hard = surprised). */
internal fun difficultyMascot(difficulty: Int): Int = when (difficulty) {
    1 -> Assets.mascotA
    2 -> Assets.mascotD
    else -> Assets.mascotC
}

internal fun difficultyStars(difficulty: Int): String =
    "★".repeat(difficulty.coerceIn(1, 3)) + "☆".repeat(3 - difficulty.coerceIn(1, 3))

@Composable
fun QuizCategoryScreen(
    viewModel: ProgressViewModel,
    onCategory: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameBackground(dim = 0.05f) {
        FillScrollColumn {
            GameTopBar(title = "Farm Quiz", onBack = onBack)
            InfoChip(text = "Choose difficulty")
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                QuizBank.categories.forEach { category ->
                    DifficultyCard(
                        category = category,
                        best = state.quizBest[category.id] ?: 0,
                        onPlay = { onCategory(category.id) }
                    )
                }
            }
            MascotParade(
                mascots = listOf(Assets.mascotB),
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                maxSize = 160.dp
            )
        }
    }
}

@Composable
private fun DifficultyCard(category: QuizCategory, best: Int, onPlay: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val total = QuizBank.QUESTIONS_PER_CATEGORY
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Asphalt, NightNavy)))
            .border(BorderStroke(3.dp, SunYolk), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "${category.title}, difficulty ${category.difficulty} of 3. " +
                    "${category.tagline}. Best score $best of $total."
            }
        ) {
            Image(
                painter = painterResource(difficultyMascot(category.difficulty)),
                contentDescription = null,
                modifier = Modifier.size(76.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTitle(text = category.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                Text(difficultyStars(category.difficulty), color = SunYolk, style = MaterialTheme.typography.titleLarge)
                Text(category.tagline, color = Cream, style = MaterialTheme.typography.bodyMedium)
            }
            InfoChip(text = "$best/$total")
        }
        PlateButton(
            text = "Play ${category.title}",
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

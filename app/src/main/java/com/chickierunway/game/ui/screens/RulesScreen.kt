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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chickierunway.game.Assets
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
            GameTopBar(title = "Game Rules", onBack = onBack)
            PanelWithMascot(
                mascot = Assets.mascotA,
                mascotDescription = null,
                title = "How to Play",
                mascotSize = 110.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                RuleSection(
                    "Matching Pairs",
                    listOf(
                        "Tap a card to flip it, then tap another.",
                        "Same picture? The pair stays open. Different? Both flip back.",
                        "Clear every pair before the timer reaches 0:00 to win.",
                        "Clearing a level unlocks the next one — ${GameLogic.LEVEL_COUNT} levels, more cards and less time per pair.",
                        "Pause any time: the timer stops while paused."
                    )
                )
                // Example pair
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Assets.item(5)), contentDescription = "Example: yellow car", modifier = Modifier.size(48.dp))
                    Text("+", color = SunYolk, style = MaterialTheme.typography.headlineSmall)
                    Image(painterResource(Assets.item(5)), contentDescription = "yellow car", modifier = Modifier.size(48.dp))
                    Text("= ✓ pair", color = Cream, style = MaterialTheme.typography.titleMedium)
                }
                RuleSection(
                    "Farm Quiz",
                    listOf(
                        "3 difficulty levels — Easy, Medium, Hard — with 10 questions each.",
                        "Each question has 4 answers and only one is right.",
                        "The correct answer turns green (✓); a wrong pick turns red (✗).",
                        "Your best score for each level is saved on this device."
                    )
                )
                PlateButton(text = "Got it", onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f))
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
                text = "•  $line",
                color = Cream,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

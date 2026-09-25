package com.chickierunway.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.IconPlateButton
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.PanelActions
import com.chickierunway.game.ui.components.PanelOverlay
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.theme.Asphalt
import com.chickierunway.game.ui.theme.CorrectGreen
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.SkyBlue
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.theme.WrongRed
import com.chickierunway.game.ui.viewmodel.PairCardUi
import com.chickierunway.game.ui.viewmodel.PairsStatus
import com.chickierunway.game.ui.viewmodel.PairsViewModel
import kotlinx.coroutines.delay

/**
 * Matching Pairs. The whole board always fits on one screen (no scrolling): card size is
 * computed from the space left under the header for the level's columns × rows.
 */
@Composable
fun PairsScreen(
    viewModel: PairsViewModel,
    onFinished: (won: Boolean, matched: Int, total: Int) -> Unit,
    onMenu: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playing = state.status == PairsStatus.PLAYING

    BackHandler(enabled = playing) {
        if (state.isPaused) viewModel.resume() else viewModel.pause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }

    LaunchedEffect(state.status) {
        if (state.status != PairsStatus.PLAYING) {
            delay(900) // let the last flip / match be seen
            onFinished(state.status == PairsStatus.WON, state.matchedPairs, state.totalPairs)
        }
    }

    GameBackground(blurred = true) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(
                title = "Level ${state.level}",
                onBack = viewModel::pause,
                backDescription = "Pause and show options",
                trailing = {
                    IconPlateButton(icon = Assets.iconPause, contentDescription = "Pause", onClick = viewModel::pause)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    text = "⏱ " + GameLogic.formatSeconds(state.secondsLeft) + if (state.isLowTime) " Hurry!" else "",
                    textColor = if (state.isLowTime) Color.White else SkyBlue,
                    borderColor = if (state.isLowTime) WrongRed else SkyBlue,
                    background = if (state.isLowTime) WrongRed else NightNavy.copy(alpha = 0.82f),
                    description = "Time left ${state.secondsLeft} seconds" + if (state.isLowTime) ", hurry" else ""
                )
                InfoChip(
                    text = "Pairs ${state.matchedPairs}/${state.totalPairs}",
                    description = "Matched ${state.matchedPairs} of ${state.totalPairs} pairs"
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val columns = state.columns.coerceAtLeast(1)
                val rows = ((state.cards.size + columns - 1) / columns).coerceAtLeast(1)
                val gap = if (columns >= 4) 8.dp else 12.dp
                val cellW = (maxWidth - gap * (columns - 1)) / columns
                val cellH = (maxHeight - gap * (rows - 1)) / rows
                // Portrait playing-card look (≈ 3:4) when there is room, never larger than the cell.
                val cardW: Dp = min(min(cellW, cellH / 0.8f), 220.dp)
                val cardH: Dp = min(cellH, cardW * 1.3f)

                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    state.cards.chunked(columns).forEachIndexed { rowIndex, rowCards ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            rowCards.forEachIndexed { colIndex, card ->
                                MemoryCard(
                                    card = card,
                                    position = rowIndex * columns + colIndex + 1,
                                    enabled = playing && !state.isPaused,
                                    onClick = { viewModel.onCardTapped(card.id) },
                                    modifier = Modifier.size(width = cardW, height = cardH)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.isPaused && playing) {
            PanelOverlay(title = "Paused", mascot = Assets.mascotD) {
                PanelText("Level ${state.level}  ·  ${GameLogic.formatSeconds(state.secondsLeft)} left")
                PanelText("Pairs ${state.matchedPairs}/${state.totalPairs}")
                PanelActions(
                    primaryLabel = "Continue",
                    onPrimary = viewModel::resume,
                    onRestart = viewModel::restart,
                    onMenu = onMenu
                )
            }
        }
    }
}

@Composable
private fun MemoryCard(
    card: PairCardUi,
    position: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.faceUp || card.matched) 180f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "flip"
    )
    val shape = RoundedCornerShape(14.dp)
    val name = Assets.itemName(card.itemIndex)
    val description = when {
        card.matched -> "Card $position, $name, matched"
        card.faceUp -> "Card $position, $name"
        else -> "Card $position, face down"
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .semantics {
                contentDescription = description
                if (card.matched) stateDescription = "Matched"
            }
            .clickable(
                enabled = enabled && !card.faceUp && !card.matched,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            // Back face: dark asphalt gradient, gold frame, game logo.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Asphalt, NightNavy)))
                    .border(BorderStroke(2.5.dp, SunYolk), shape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Assets.logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(min(maxWidth, maxHeight) * 0.7f)
                )
            }
        } else {
            // Front face (mirrored back so it reads correctly after the flip).
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .background(Brush.verticalGradient(listOf(Cream, Color(0xFFFFE9B0))))
                    .border(
                        BorderStroke(if (card.matched) 4.dp else 2.5.dp, if (card.matched) CorrectGreen else SunYolk),
                        shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Assets.item(card.itemIndex)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(min(maxWidth, maxHeight) * 0.8f)
                )
                if (card.matched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(CorrectGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

package com.chickierunway.game.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.audio.Sfx
import com.chickierunway.game.logic.GameLogic
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.LocalSoundManager
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.PlateButton
import com.chickierunway.game.ui.components.rememberClickGuard
import com.chickierunway.game.ui.theme.Asphalt
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.viewmodel.LevelUi
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

/** 9 level bubbles on a zig-zag "road" (3 per row, middle bubble dips), rules button, mascots. */
@Composable
fun LevelsScreen(
    viewModel: ProgressViewModel,
    onLevel: (Int) -> Unit,
    onRules: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameBackground(dim = 0.05f) {
        FillScrollColumn {
            GameTopBar(title = stringResource(R.string.pairs_title), onBack = onBack)
            InfoChip(
                text = stringResource(R.string.levels_cleared, state.completedLevels, GameLogic.LEVEL_COUNT),
                description = stringResource(R.string.levels_cleared_cd, state.completedLevels, GameLogic.LEVEL_COUNT)
            )
            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).padding(horizontal = 12.dp)) {
                // A little road with a dashed centre line runs behind each row of bubbles.
                Canvas(modifier = Modifier.matchParentSize()) {
                    val rowH = size.height / 3f
                    val dash = PathEffect.dashPathEffect(floatArrayOf(22f, 16f))
                    for (row in 0 until 3) {
                        val y = rowH * row + rowH * 0.4f
                        drawLine(Asphalt.copy(alpha = 0.85f), Offset(size.width * 0.1f, y), Offset(size.width * 0.9f, y), 26.dp.toPx(), StrokeCap.Round)
                        drawLine(Cream, Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y), 3.dp.toPx(), pathEffect = dash)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.levels.chunked(3).forEach { rowLevels ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowLevels.forEachIndexed { col, level ->
                                LevelBubble(
                                    level = level,
                                    onClick = { onLevel(level.level) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .offset(y = if (col == 1) 12.dp else 0.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            PlateButton(
                text = stringResource(R.string.rules_title),
                onClick = onRules,
                leadingIcon = Assets.item(2),
                modifier = Modifier.fillMaxWidth(0.78f).widthIn(max = 380.dp)
            )

            MascotParade(
                mascots = listOf(Assets.mascotD, Assets.mascotB),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun LevelBubble(level: LevelUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sound = LocalSoundManager.current
    val guard = rememberClickGuard()
    val grayscale = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val status = when {
        !level.unlocked -> stringResource(R.string.level_locked)
        level.cleared -> stringResource(R.string.level_cleared)
        level.bestPairs > 0 -> stringResource(R.string.level_best, level.bestPairs, level.totalPairs)
        else -> pluralStringResource(R.plurals.level_pairs, level.totalPairs, level.totalPairs)
    }
    val description = when {
        !level.unlocked -> stringResource(R.string.level_cd_locked, level.level, level.level - 1)
        level.cleared -> stringResource(R.string.level_cd_cleared, level.level, level.bestPairs, level.totalPairs)
        else -> stringResource(R.string.level_cd_open, level.level, level.totalPairs, level.bestPairs)
    }
    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = level.unlocked, role = Role.Button) {
                if (guard.tryClick()) {
                    sound?.play(Sfx.CLICK)
                    onClick()
                }
            }
            .clearAndSetSemantics { contentDescription = description }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 112.dp).aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Assets.plateRound),
                contentDescription = null,
                colorFilter = if (level.unlocked) null else grayscale,
                alpha = if (level.unlocked) 1f else 0.8f,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = if (level.unlocked) level.level.toString() else "🔒",
                color = if (level.unlocked) SunYolk else Color.White,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            if (level.cleared) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SunYolk),
                    contentAlignment = Alignment.Center
                ) {
                    Text("★", color = NightNavy, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = status,
            color = Cream,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NightNavy.copy(alpha = 0.85f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

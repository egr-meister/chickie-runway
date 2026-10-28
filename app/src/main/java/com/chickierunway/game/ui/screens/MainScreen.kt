package com.chickierunway.game.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.chickierunway.game.Assets
import com.chickierunway.game.ui.components.InfoChip
import com.chickierunway.game.ui.components.MenuPlateButton

/**
 * "Road Dash" home composition: the road from the background art is the stage. Road-side props
 * (cone, traffic light, flag, sign) stand along the verges, Chickie sprints down the middle of the
 * road, the logo floats in the sky and Start is the finish-line plate at the bottom.
 * No stats card, no button stack.
 */
@Composable
fun MainScreen(onStart: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "home")
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hop"
    )
    val tilt by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(840, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt"
    )
    val float by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Image(
            painter = painterResource(Assets.bgMain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val w = maxWidth
            val h = maxHeight
            val propSize = min(w * 0.2f, h * 0.11f)

            // Road-side props (decorative), placed relative to the road in the art.
            RoadProp(Assets.item(4), propSize, x = w * 0.04f, y = h * 0.50f, bobPhase = float)        // cone
            RoadProp(Assets.item(7), propSize * 1.1f, x = w * 0.78f, y = h * 0.44f, bobPhase = 1f - float) // traffic light
            RoadProp(Assets.item(15), propSize * 0.9f, x = w * 0.80f, y = h * 0.62f, bobPhase = float)  // stop sign
            RoadProp(Assets.item(3), propSize * 0.95f, x = w * 0.03f, y = h * 0.66f, bobPhase = 1f - float) // barrier

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Image(
                    painter = painterResource(Assets.logo),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .widthIn(max = 420.dp)
                        .height(min(h * 0.32f, w * 0.78f))
                        .graphicsLayer {
                            translationY = (float * 10f - 5f) * density
                            val s = 1f + float * 0.03f
                            scaleX = s
                            scaleY = s
                        }
                )

                Spacer(Modifier.weight(1f))

                Image(
                    painter = painterResource(Assets.mascotD),
                    contentDescription = stringResource(R.string.cd_main_runner),
                    modifier = Modifier
                        .size(min(w * 0.5f, h * 0.28f))
                        .graphicsLayer {
                            translationY = hop * density
                            rotationZ = tilt
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                )

                Spacer(Modifier.height(6.dp))

                Box(contentAlignment = Alignment.Center) {
                    MenuPlateButton(text = stringResource(R.string.main_start), onClick = onStart, leadingIcon = Assets.item(0))
                }

                Spacer(Modifier.height(10.dp))
                InfoChip(text = stringResource(R.string.main_tagline))
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RoadProp(@DrawableRes res: Int, size: Dp, x: Dp, y: Dp, bobPhase: Float) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .graphicsLayer { translationY = (bobPhase * 6f - 3f) * density }
    )
}

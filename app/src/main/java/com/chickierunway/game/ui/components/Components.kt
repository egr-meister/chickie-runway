package com.chickierunway.game.ui.components

import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.chickierunway.game.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.chickierunway.game.Assets
import com.chickierunway.game.audio.Sfx
import com.chickierunway.game.audio.SoundManager
import com.chickierunway.game.ui.theme.Asphalt
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.LocalChickiePalette
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.Scrim
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.theme.SunYolkDeep
import kotlin.math.roundToInt

/** Sound effects available to any composable (provided by MainActivity). */
val LocalSoundManager = staticCompositionLocalOf<SoundManager?> { null }

/** Ignores repeated taps within [intervalMs] (prevents double navigation / double answers). */
class ClickGuard(private val intervalMs: Long = 450L) {
    private var lastClick = 0L
    fun tryClick(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClick < intervalMs) return false
        lastClick = now
        return true
    }
}

@Composable
fun rememberClickGuard(): ClickGuard = remember { ClickGuard() }

private val LabelShadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 3f), blurRadius = 6f)

/**
 * Full-screen themed background + safe-area insets. The art is drawn edge-to-edge behind the
 * system bars; content is laid out inside the safe area. [blurred] softens the art on game screens
 * (real blur on API 31+, a slightly darker dim everywhere).
 */
@Composable
fun GameBackground(
    modifier: Modifier = Modifier,
    dim: Float = 0f,
    blurred: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Image(
            painter = painterResource(Assets.bgMain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurred) Modifier.blur(14.dp) else Modifier)
        )
        // Dark theme lays a night sky over the art; the plates and panels stay as they are.
        val night = LocalChickiePalette.current.backdropDim
        val totalDim = (dim + night + if (blurred) 0.18f else 0f).coerceIn(0f, 0.9f)
        if (totalDim > 0f) {
            Box(Modifier.fillMaxSize().background(NightNavy.copy(alpha = totalDim)))
        }
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            content = content
        )
    }
}

/**
 * Draws a plate image as 3 slices (left cap | stretched middle | right cap) so it can take any
 * width without squashing the rounded ends. Optionally fills the plate's inner area with [innerTint].
 */
@Composable
fun SlicedPlate(
    @DrawableRes plate: Int,
    modifier: Modifier = Modifier,
    innerTint: Color? = null,
    alpha: Float = 1f
) {
    val bitmap: ImageBitmap = ImageBitmap.imageResource(plate)
    Canvas(modifier = modifier) {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val dstW = size.width.roundToInt()
        val dstH = size.height.roundToInt()
        if (srcW <= 1 || srcH <= 0 || dstW <= 0 || dstH <= 0) return@Canvas

        val capSrc = (srcH * Assets.PLATE_CAP_FACTOR).roundToInt().coerceIn(1, srcW / 2)
        val scale = dstH.toFloat() / srcH
        val capDst = (capSrc * scale).roundToInt().coerceAtMost(dstW / 2)
        val midSrcW = srcW - capSrc * 2
        val midDstW = dstW - capDst * 2

        drawImage(
            image = bitmap,
            srcOffset = IntOffset(0, 0), srcSize = IntSize(capSrc, srcH),
            dstOffset = IntOffset(0, 0), dstSize = IntSize(capDst, dstH),
            alpha = alpha, filterQuality = FilterQuality.Medium
        )
        if (midSrcW > 0 && midDstW > 0) {
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(capSrc, 0), srcSize = IntSize(midSrcW, srcH),
                dstOffset = IntOffset(capDst, 0), dstSize = IntSize(midDstW, dstH),
                alpha = alpha, filterQuality = FilterQuality.Medium
            )
        }
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(srcW - capSrc, 0), srcSize = IntSize(capSrc, srcH),
            dstOffset = IntOffset(dstW - capDst, 0), dstSize = IntSize(capDst, dstH),
            alpha = alpha, filterQuality = FilterQuality.Medium
        )

        if (innerTint != null) {
            val inset = dstH * Assets.PLATE_INNER_INSET
            val innerH = (dstH - inset * 2).coerceAtLeast(0f)
            drawRoundRect(
                color = innerTint,
                topLeft = Offset(inset * 1.1f, inset),
                size = Size((dstW - inset * 2.2f).coerceAtLeast(0f), innerH),
                cornerRadius = CornerRadius(innerH / 2f, innerH / 2f),
                alpha = 0.92f * alpha
            )
        }
    }
}

/**
 * Image plate with a WHITE bold label drawn on top in code. One plate asset is reused everywhere.
 * [tint] fills the plate's inner area (e.g. green/red answer feedback) keeping the frame visible.
 * [leadingIcon] optionally shows one of the game icons at the start of the plate.
 */
@Composable
fun PlateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes plate: Int = Assets.btnPlate,
    tint: Color? = null,
    dimmed: Boolean = false,
    stateText: String? = null,
    accessibilityLabel: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    minHeight: Dp = 58.dp,
    maxLines: Int = 2,
    playClickSound: Boolean = true
) {
    val sound = LocalSoundManager.current
    val guard = rememberClickGuard()
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(percent = 50))
            .semantics(mergeDescendants = true) {
                if (stateText != null) stateDescription = stateText
                if (accessibilityLabel != null) contentDescription = accessibilityLabel
            }
            .clickable(enabled = enabled, role = Role.Button) {
                if (guard.tryClick()) {
                    if (playClickSound) sound?.play(Sfx.CLICK)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SlicedPlate(
            plate = plate,
            innerTint = tint,
            alpha = if (dimmed || (!enabled && tint == null)) 0.5f else 1f,
            modifier = Modifier.matchParentSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 12.dp)
        ) {
            if (leadingIcon != null) {
                Image(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                style = textStyle.copy(shadow = LabelShadow),
                textAlign = TextAlign.Center,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Ornate framed plate for main-menu buttons: ~70% of screen width, fixed aspect ratio. */
@Composable
fun MenuPlateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.7f,
    @DrawableRes leadingIcon: Int? = null,
    accessibilityLabel: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall
) {
    val sound = LocalSoundManager.current
    val guard = rememberClickGuard()
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .widthIn(max = 420.dp)
            .aspectRatio(Assets.BTN_MENU_ASPECT)
            .clip(RoundedCornerShape(percent = 30))
            .semantics(mergeDescendants = true) {
                if (accessibilityLabel != null) contentDescription = accessibilityLabel
            }
            .clickable(role = Role.Button) {
                if (guard.tryClick()) {
                    sound?.play(Sfx.CLICK)
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Assets.btnMenu),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 26.dp)
        ) {
            if (leadingIcon != null) {
                Image(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.size(10.dp))
            }
            Text(
                text = text,
                color = Color.White,
                style = textStyle.copy(shadow = LabelShadow),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Round icon button (back / pause) with a ≥48dp touch target and a content description. */
@Composable
fun IconPlateButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    val sound = LocalSoundManager.current
    val guard = rememberClickGuard()
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size.coerceAtLeast(48.dp))
            .clip(RoundedCornerShape(percent = 50))
            .clickable(role = Role.Button) {
                if (guard.tryClick()) {
                    sound?.play(Sfx.CLICK)
                    onClick()
                }
            }
    )
}

/** Game-style title: gold fill with a dark outline so it reads over any part of the art. */
@Composable
fun OutlinedTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = SunYolk,
    maxLines: Int = 2
) {
    Box(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = text
            heading()
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = style.copy(drawStyle = Stroke(width = 9f, join = StrokeJoin.Round)),
            color = NightNavy,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = text,
            style = style.copy(shadow = Shadow(Color(0x99000000), Offset(0f, 4f), 6f)),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Top bar: round back button, outlined gold title, optional trailing slot (e.g. pause). */
@Composable
fun GameTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = stringResource(R.string.action_back),
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconPlateButton(icon = Assets.iconBack, contentDescription = backDescription, onClick = onBack)
        OutlinedTitle(text = title, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        if (trailing != null) {
            trailing()
        } else {
            Spacer(Modifier.size(54.dp)) // keeps the title centred
        }
    }
}

/** Small rounded status chip (timer, pairs, best score…). */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = SunYolk,
    borderColor: Color = SunYolkDeep,
    background: Color = NightNavy.copy(alpha = 0.82f),
    description: String? = null
) {
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(2.dp, borderColor), shape)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(if (description != null) Modifier.clearAndSetSemantics { contentDescription = description } else Modifier)
    )
}

/**
 * Modal panel DRAWN IN COMPOSE: rounded rectangle, vertical gradient fill, accent border.
 * Grows with its content — no stretched frame image.
 */
@Composable
fun ModalPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Asphalt, NightNavy)))
            .border(BorderStroke(3.dp, SunYolk), shape)
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTitle(text = title)
        content()
    }
}

/** A mascot that "sits" on top of a panel: the image overlaps the panel's top border. */
@Composable
fun PanelWithMascot(
    @DrawableRes mascot: Int,
    mascotDescription: String?,
    title: String,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 130.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.widthIn(max = 460.dp).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        ModalPanel(title = title, modifier = Modifier.padding(top = mascotSize * 0.72f)) {
            content()
        }
        Image(
            painter = painterResource(mascot),
            contentDescription = mascotDescription,
            modifier = Modifier.size(mascotSize)
        )
    }
}

/** Dimmed full-screen overlay that swallows touches and centres a panel (scrolls if tall). */
@Composable
fun PanelOverlay(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes mascot: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(interactionSource = interaction, indication = null, onClick = {})
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        CenteredScrollColumn(modifier = Modifier.padding(20.dp)) {
            if (mascot != null) {
                PanelWithMascot(mascot = mascot, mascotDescription = null, title = title, mascotSize = 110.dp, content = content)
            } else {
                ModalPanel(title = title, content = content)
            }
        }
    }
}

/** A column that is vertically centred when it fits and scrolls when it does not. */
@Composable
fun CenteredScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .then(modifier),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * A scrollable column that is at least as tall as the viewport, so children may use
 * `Modifier.weight(...)` to fill the space left on tall screens. On short screens or with huge
 * fonts it simply scrolls and weighted children shrink to their minimum.
 */
@Composable
fun FillScrollColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * Fills leftover space with the mascots standing side by side. Draws nothing when the space is
 * too small, so it never pushes real content off-screen. Decorative (no semantics).
 */
@Composable
fun MascotParade(
    mascots: List<Int>,
    modifier: Modifier = Modifier,
    minVisible: Dp = 72.dp,
    maxSize: Dp = 190.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        if (mascots.isEmpty() || maxHeight < minVisible) return@BoxWithConstraints
        val size = min(min(maxHeight, maxWidth / mascots.size), maxSize)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            mascots.forEach { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

/** Standard 3-button action column for pause/results panels. */
@Composable
fun PanelActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlateButton(text = primaryLabel, onClick = onPrimary, modifier = Modifier.fillMaxWidth(0.9f))
        PlateButton(text = stringResource(R.string.action_restart), onClick = onRestart, modifier = Modifier.fillMaxWidth(0.9f))
        PlateButton(text = stringResource(R.string.action_menu), onClick = onMenu, modifier = Modifier.fillMaxWidth(0.9f))
    }
}

@Composable
fun PanelText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(text = text, color = Cream, style = style, textAlign = TextAlign.Center, modifier = modifier)
}

@Composable
fun GameProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.5.dp, SunYolkDeep), RoundedCornerShape(8.dp)),
        color = SunYolk,
        trackColor = NightNavy.copy(alpha = 0.7f),
        strokeCap = StrokeCap.Round
    )
}

/** Friendly fallback for invalid navigation args / missing data. Never crashes, always has Back. */
@Composable
fun ErrorFallback(
    message: String,
    onBack: () -> Unit,
    title: String = stringResource(R.string.error_title)
) {
    GameBackground(blurred = true) {
        CenteredScrollColumn(modifier = Modifier.padding(24.dp)) {
            PanelWithMascot(
                mascot = Assets.mascotC,
                mascotDescription = stringResource(R.string.cd_mascot_surprised),
                title = title
            ) {
                PanelText(message)
                PlateButton(text = stringResource(R.string.action_back), onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f))
            }
        }
    }
}

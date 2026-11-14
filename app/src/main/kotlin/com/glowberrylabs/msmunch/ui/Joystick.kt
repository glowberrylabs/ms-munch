package com.glowberrylabs.msmunch.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.glowberrylabs.msmunch.R
import com.glowberrylabs.msmunch.game.Direction
import com.glowberrylabs.msmunch.game.Handedness
import com.glowberrylabs.msmunch.game.JoystickResolver
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun Joystick(
    handedness: Handedness,
    enabled: Boolean,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolver = remember(handedness) { JoystickResolver() }
    val density = LocalDensity.current
    val baseRadius = with(density) { 66.dp.toPx() }
    val knobRadius = with(density) { 28.dp.toPx() }
    val padding = with(density) { 6.dp.toPx() }
    val travel = baseRadius * 0.55f
    var knobOffset by remember(handedness) { mutableStateOf(Offset.Zero) }
    val view = LocalView.current
    val currentOnDirection by rememberUpdatedState(onDirection)
    val description = stringResource(R.string.joystick_description)

    LaunchedEffect(enabled) {
        if (!enabled) {
            resolver.reset()
            knobOffset = Offset.Zero
        }
    }

    val inputModifier = if (enabled) {
        Modifier
            .fillMaxSize()
            .pointerInput(handedness, baseRadius, travel) {
                awaitEachGesture {
                    handleJoystickGesture(
                        handedness = handedness,
                        baseRadius = baseRadius,
                        padding = padding,
                        travel = travel,
                        resolver = resolver,
                        view = view,
                        onKnobOffset = { knobOffset = it },
                        onDirection = currentOnDirection
                    )
                }
            }
    } else {
        Modifier.fillMaxSize()
    }

    Box(modifier = modifier.semantics { contentDescription = description }) {
        Canvas(modifier = inputModifier) {
            drawJoystick(
                handedness = handedness,
                enabled = enabled,
                baseRadius = baseRadius,
                knobRadius = knobRadius,
                padding = padding,
                knobOffset = knobOffset
            )
        }
    }
}

private suspend fun AwaitPointerEventScope.handleJoystickGesture(
    handedness: Handedness,
    baseRadius: Float,
    padding: Float,
    travel: Float,
    resolver: JoystickResolver,
    view: View,
    onKnobOffset: (Offset) -> Unit,
    onDirection: (Direction) -> Unit
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    val center = Offset(
        x = baseCenterX(size.width.toFloat(), handedness, baseRadius, padding),
        y = size.height / 2f
    )
    val previous = resolver.direction
    val direction = resolver.onDown(
        down.position.x - center.x,
        down.position.y - center.y,
        baseRadius
    )
    onKnobOffset(clampOffset(down.position - center, travel))
    down.consume()
    if (direction != null) {
        onDirection(direction)
        if (direction != previous) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    var last = direction
    val pointerId = down.id
    var tracking = true
    while (tracking) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
        if (change == null) {
            tracking = false
        } else if (change.pressed) {
            val next = resolver.onMove(
                change.position.x - center.x,
                change.position.y - center.y,
                baseRadius
            )
            onKnobOffset(clampOffset(change.position - center, travel))
            if (next != null && next != last) {
                onDirection(next)
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                last = next
            }
            change.consume()
        } else {
            tracking = false
        }
    }
    resolver.onUp()
    onKnobOffset(Offset.Zero)
}

private fun DrawScope.drawJoystick(
    handedness: Handedness,
    enabled: Boolean,
    baseRadius: Float,
    knobRadius: Float,
    padding: Float,
    knobOffset: Offset
) {
    val center = Offset(
        x = baseCenterX(size.width, handedness, baseRadius, padding),
        y = size.height / 2f
    )
    val alpha = if (enabled) 1f else 0.4f
    drawBase(center, baseRadius, alpha)
    drawDirectionTicks(center, baseRadius, alpha)
    drawKnob(center + knobOffset, knobRadius, alpha)
}

private fun DrawScope.drawBase(center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GamePalette.Wall.copy(alpha = 0.18f * alpha), Color.Transparent),
            center = center,
            radius = radius * 1.35f
        ),
        radius = radius * 1.35f,
        center = center
    )
    drawCircle(
        color = GamePalette.Surface.copy(alpha = 0.82f * alpha),
        radius = radius,
        center = center
    )
    drawCircle(
        color = GamePalette.Wall.copy(alpha = 0.45f * alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = GamePalette.Wall.copy(alpha = 0.18f * alpha),
        radius = radius * 0.62f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawDirectionTicks(center: Offset, radius: Float, alpha: Float) {
    val tickRadius = radius * 0.78f
    DIRECTION_TICKS.forEach { direction ->
        drawCircle(
            color = GamePalette.Muted.copy(alpha = 0.5f * alpha),
            radius = 2.5.dp.toPx(),
            center = center + direction * tickRadius
        )
    }
}

private fun DrawScope.drawKnob(center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        color = GamePalette.Muncher.copy(alpha = 0.18f * alpha),
        radius = radius * 1.25f,
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF9DB4), GamePalette.Muncher),
            center = center - Offset(radius * 0.3f, radius * 0.3f),
            radius = radius * 1.4f
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.5f * alpha),
        radius = radius * 0.22f,
        center = center - Offset(radius * 0.35f, radius * 0.35f)
    )
}

private fun baseCenterX(width: Float, handedness: Handedness, radius: Float, padding: Float): Float {
    val minimum = radius + padding
    val maximum = max(minimum, width - radius - padding)
    return when (handedness) {
        Handedness.LEFT -> (width / 6f).coerceIn(minimum, maximum)
        Handedness.RIGHT -> (width * 5f / 6f).coerceIn(minimum, maximum)
    }
}

private fun clampOffset(offset: Offset, maxLength: Float): Offset {
    val length = hypot(offset.x, offset.y)
    if (length <= maxLength || length == 0f) return offset
    val scale = maxLength / length
    return Offset(offset.x * scale, offset.y * scale)
}

private val DIRECTION_TICKS = listOf(
    Offset(0f, -1f),
    Offset(0f, 1f),
    Offset(-1f, 0f),
    Offset(1f, 0f)
)

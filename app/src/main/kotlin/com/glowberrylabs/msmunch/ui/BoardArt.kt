package com.glowberrylabs.msmunch.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.glowberrylabs.msmunch.game.Direction
import com.glowberrylabs.msmunch.game.GamePhase
import com.glowberrylabs.msmunch.game.GameState
import com.glowberrylabs.msmunch.game.GhostKind
import com.glowberrylabs.msmunch.game.GhostMode
import com.glowberrylabs.msmunch.game.Grump
import com.glowberrylabs.msmunch.game.Tile
import kotlin.math.min
import kotlin.math.sin

fun DrawScope.drawBoard(game: GameState, time: Float, fruitScale: Float) {
    val cellWidth = size.width / game.maze.columns
    val cellHeight = size.height / game.maze.rows
    val unit = min(cellWidth, cellHeight)

    drawDots(game, cellWidth, cellHeight, unit, time)
    drawFruit(game, cellWidth, cellHeight, unit, time, fruitScale)
    drawGrumps(game, cellWidth, cellHeight, unit, time)
    drawPlayer(game, cellWidth, cellHeight, unit, time)
}

private fun DrawScope.drawDots(game: GameState, cellWidth: Float, cellHeight: Float, unit: Float, time: Float) {
    game.board.forEachIndexed { index, tile ->
        val col = index % game.maze.columns
        val row = index / game.maze.columns
        val center = Offset((col + 0.5f) * cellWidth, (row + 0.5f) * cellHeight)
        when (tile) {
            Tile.DOT -> drawCircle(
                color = GamePalette.Dot.copy(alpha = 0.92f),
                radius = unit * 0.075f,
                center = center
            )
            Tile.PELLET -> {
                val pulse = 0.9f + 0.12f * sin(time * 4f + index * 0.7f)
                drawCircle(
                    color = GamePalette.Pellet.copy(alpha = 0.18f),
                    radius = unit * 0.34f * pulse,
                    center = center
                )
                drawCircle(
                    color = GamePalette.Pellet,
                    radius = unit * 0.17f * pulse,
                    center = center
                )
            }
            else -> Unit
        }
    }
}

private fun DrawScope.drawPlayer(game: GameState, cellWidth: Float, cellHeight: Float, unit: Float, time: Float) {
    val player = game.player
    val center = Offset(
        (player.pos.x.toFloat() + 0.5f) * cellWidth,
        (player.pos.y.toFloat() + 0.5f) * cellHeight
    )
    val radius = unit * 0.43f
    val directionAngle = angleOf(player.direction)
    val moving = game.phase == GamePhase.RUNNING && player.direction != Direction.NONE
    val mouth = if (moving) {
        18f + 22f * (0.5f + 0.5f * sin(time * 12f))
    } else {
        10f
    }

    drawCircle(
        color = GamePalette.Muncher.copy(alpha = 0.16f),
        radius = radius * 1.35f,
        center = center
    )
    drawArc(
        color = GamePalette.Muncher,
        startAngle = directionAngle + mouth / 2f,
        sweepAngle = 360f - mouth,
        useCenter = true,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2f, radius * 2f)
    )
    drawMuncherBow(center, unit)
    drawMuncherEye(center, radius, unit, player.direction, moving, time)
}

private fun DrawScope.drawMuncherBow(center: Offset, unit: Float) {
    val bowY = center.y - unit * 0.43f * 0.86f
    drawCircle(
        color = Color(0xFFFFD1DC),
        radius = unit * 0.14f,
        center = Offset(center.x - unit * 0.16f, bowY)
    )
    drawCircle(
        color = Color(0xFFFFD1DC),
        radius = unit * 0.14f,
        center = Offset(center.x + unit * 0.16f, bowY)
    )
    drawCircle(
        color = Color(0xFFFFE3EA),
        radius = unit * 0.075f,
        center = Offset(center.x, bowY + unit * 0.02f)
    )
}

private fun DrawScope.drawMuncherEye(
    center: Offset,
    radius: Float,
    unit: Float,
    direction: Direction,
    moving: Boolean,
    time: Float
) {
    val eyeCenter = Offset(center.x + radius * 0.18f, center.y - radius * 0.34f)
    val blinking = !moving && (time % 4.2f) < 0.16f
    if (blinking) {
        drawLine(
            color = GamePalette.Background,
            start = Offset(eyeCenter.x - unit * 0.14f, eyeCenter.y),
            end = Offset(eyeCenter.x + unit * 0.14f, eyeCenter.y),
            strokeWidth = unit * 0.045f,
            cap = StrokeCap.Round
        )
    } else {
        drawCircle(color = Color.White, radius = unit * 0.13f, center = eyeCenter)
        drawCircle(
            color = GamePalette.Background,
            radius = unit * 0.065f,
            center = eyeCenter + directionUnit(direction) * unit * 0.035f
        )
    }
    drawLine(
        color = GamePalette.Background,
        start = Offset(eyeCenter.x - unit * 0.16f, eyeCenter.y - unit * 0.1f),
        end = Offset(eyeCenter.x - unit * 0.23f, eyeCenter.y - unit * 0.2f),
        strokeWidth = unit * 0.035f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = GamePalette.Background,
        start = Offset(eyeCenter.x, eyeCenter.y - unit * 0.16f),
        end = Offset(eyeCenter.x, eyeCenter.y - unit * 0.27f),
        strokeWidth = unit * 0.035f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawGrumps(game: GameState, cellWidth: Float, cellHeight: Float, unit: Float, time: Float) {
    game.grumps.forEach { grump ->
        val center = Offset(
            (grump.pos.x.toFloat() + 0.5f) * cellWidth,
            (grump.pos.y.toFloat() + 0.5f) * cellHeight
        )
        val radius = unit * 0.41f
        when (grump.mode) {
            GhostMode.EYES -> drawGhostEyes(center, radius, grump.direction)
            GhostMode.FRIGHTENED -> drawFrightenedGrump(game, center, radius, unit, time)
            else -> drawNormalGrump(grump, center, radius, unit)
        }
    }
}

private fun DrawScope.drawNormalGrump(grump: Grump, center: Offset, radius: Float, unit: Float) {
    val color = ghostColor(grump.kind)
    drawCircle(color = color.copy(alpha = 0.16f), radius = radius * 1.3f, center = center)
    drawGrumpBody(center, radius, color)
    drawGhostEyes(center, radius, grump.direction, unit)
}

private fun DrawScope.drawFrightenedGrump(game: GameState, center: Offset, radius: Float, unit: Float, time: Float) {
    val flashing = game.frightenedRemaining <= 2.0 && sin(time * 18f) > 0f
    val body = if (flashing) GamePalette.Ink else GamePalette.Frightened
    val face = GamePalette.Background
    drawCircle(color = body.copy(alpha = 0.16f), radius = radius * 1.3f, center = center)
    drawGrumpBody(center, radius, body)

    val eyeRadius = unit * 0.075f
    val eyeY = center.y - radius * 0.18f
    drawCircle(color = face, radius = eyeRadius, center = Offset(center.x - radius * 0.3f, eyeY))
    drawCircle(color = face, radius = eyeRadius, center = Offset(center.x + radius * 0.3f, eyeY))

    val mouthY = center.y + radius * 0.28f
    val step = unit * 0.13f
    var x = center.x - step * 1.5f
    var up = true
    repeat(4) {
        val nextX = x + step
        val nextY = if (up) mouthY - step * 0.4f else mouthY + step * 0.4f
        drawLine(
            color = face,
            start = Offset(x, if (up) mouthY + step * 0.4f else mouthY - step * 0.4f),
            end = Offset(nextX, nextY),
            strokeWidth = unit * 0.04f,
            cap = StrokeCap.Round
        )
        x = nextX
        up = !up
    }
}

private fun DrawScope.drawGhostEyes(center: Offset, radius: Float, direction: Direction, unit: Float = radius * 0.6f) {
    val eyeRadius = unit * 0.13f
    val pupilRadius = unit * 0.06f
    val offset = directionUnit(direction) * unit * 0.04f
    val leftEye = Offset(center.x - radius * 0.32f, center.y - radius * 0.18f)
    val rightEye = Offset(center.x + radius * 0.32f, center.y - radius * 0.18f)
    drawCircle(color = Color.White, radius = eyeRadius, center = leftEye)
    drawCircle(color = Color.White, radius = eyeRadius, center = rightEye)
    drawCircle(color = GamePalette.Background, radius = pupilRadius, center = leftEye + offset)
    drawCircle(color = GamePalette.Background, radius = pupilRadius, center = rightEye + offset)
}

private fun DrawScope.drawGrumpBody(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - radius, center.y)
        arcTo(
            rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(center.x + radius, center.y + radius * 0.55f)
        quadraticBezierTo(
            center.x + radius * 0.66f,
            center.y + radius * 1.05f,
            center.x + radius * 0.33f,
            center.y + radius * 0.55f
        )
        quadraticBezierTo(
            center.x,
            center.y + radius * 1.05f,
            center.x - radius * 0.33f,
            center.y + radius * 0.55f
        )
        quadraticBezierTo(
            center.x - radius * 0.66f,
            center.y + radius * 1.05f,
            center.x - radius,
            center.y + radius * 0.55f
        )
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawFruit(
    game: GameState,
    cellWidth: Float,
    cellHeight: Float,
    unit: Float,
    time: Float,
    scale: Float
) {
    val fruit = game.fruit ?: return
    if (scale <= 0f) return
    val center = Offset(
        (fruit.pos.col + 0.5f) * cellWidth,
        (fruit.pos.row + 0.5f) * cellHeight
    )
    val pulse = 1f + 0.05f * sin(time * 5f)
    val radius = unit * 0.28f * scale * pulse

    drawCircle(
        color = Color(0xFFFF4D6D).copy(alpha = 0.18f),
        radius = radius * 1.6f,
        center = center
    )
    drawCircle(color = Color(0xFFFF4D6D), radius = radius, center = center)
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = radius * 0.3f,
        center = center - Offset(radius * 0.3f, radius * 0.3f)
    )
    drawLine(
        color = GamePalette.Dot,
        start = center - Offset(0f, radius * 0.95f),
        end = center - Offset(radius * 0.35f, radius * 1.45f),
        strokeWidth = unit * 0.05f,
        cap = StrokeCap.Round
    )
    drawOval(
        color = GamePalette.Mint,
        topLeft = center - Offset(radius * 0.9f, radius * 1.6f),
        size = Size(radius * 0.9f, radius * 0.45f)
    )
}

private fun ghostColor(kind: GhostKind): Color = when (kind) {
    GhostKind.COCOA -> GamePalette.Cocoa
    GhostKind.BUBBLE -> GamePalette.Bubble
    GhostKind.TAFFY -> GamePalette.Taffy
    GhostKind.MINT -> GamePalette.Mint
}

private fun angleOf(direction: Direction): Float = when (direction) {
    Direction.RIGHT -> 0f
    Direction.DOWN -> 90f
    Direction.LEFT -> 180f
    Direction.UP -> 270f
    Direction.NONE -> 180f
}

private fun directionUnit(direction: Direction): Offset = Offset(
    direction.dx.toFloat(),
    direction.dy.toFloat()
)

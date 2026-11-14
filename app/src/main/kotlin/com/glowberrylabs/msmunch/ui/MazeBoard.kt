package com.glowberrylabs.msmunch.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.glowberrylabs.msmunch.game.GamePhase
import com.glowberrylabs.msmunch.game.GameState
import com.glowberrylabs.msmunch.game.Maze
import com.glowberrylabs.msmunch.game.Tile
import kotlin.math.min

@Composable
fun MazeBoard(game: GameState, modifier: Modifier = Modifier) {
    val maze = game.maze
    val density = LocalDensity.current
    val wallWidth = with(density) { 1.8.dp.toPx() }
    val glowWidth = with(density) { 6.dp.toPx() }
    val doorWidth = with(density) { 4.dp.toPx() }
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(game.phase) {
        if (game.phase == GamePhase.RUNNING) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { now -> time = (now - start) / NANOS_PER_SECOND }
            }
        }
    }

    val fruitScale = remember { Animatable(0f) }
    LaunchedEffect(game.fruit != null) {
        if (game.fruit != null) {
            fruitScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            fruitScale.snapTo(0f)
        }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val aspect = maze.columns.toFloat() / maze.rows
        val boardWidth = min(maxWidth.value, maxHeight.value * aspect)
        val boardHeight = boardWidth / aspect
        val widthPx = with(density) { boardWidth.dp.roundToPx() }.coerceAtLeast(1)
        val heightPx = with(density) { boardHeight.dp.roundToPx() }.coerceAtLeast(1)
        val mazeBitmap = remember(maze, widthPx, heightPx, wallWidth, glowWidth, doorWidth) {
            renderMaze(maze, widthPx, heightPx, wallWidth, glowWidth, doorWidth)
        }

        Canvas(modifier = Modifier.size(boardWidth.dp, boardHeight.dp)) {
            drawImage(
                image = mazeBitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
            drawBoard(game, time, fruitScale.value)
        }
    }
}

private fun renderMaze(
    maze: Maze,
    widthPx: Int,
    heightPx: Int,
    wallWidth: Float,
    glowWidth: Float,
    doorWidth: Float
): ImageBitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val cellWidth = widthPx.toFloat() / maze.columns
    val cellHeight = heightPx.toFloat() / maze.rows
    val path = buildWallPath(maze, cellWidth, cellHeight)

    val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = glowWidth
        color = GamePalette.Wall.copy(alpha = 0.20f).toArgb()
    }
    val wallPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = wallWidth
        color = GamePalette.Wall.toArgb()
    }
    canvas.drawPath(path, glowPaint)
    canvas.drawPath(path, wallPaint)

    val doorPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = doorWidth
        color = GamePalette.Muncher.copy(alpha = 0.9f).toArgb()
    }
    for (row in 0 until maze.rows) {
        for (col in 0 until maze.columns) {
            if (maze.tiles[row * maze.columns + col] == Tile.DOOR) {
                val y = (row + 0.5f) * cellHeight
                canvas.drawLine(
                    (col + 0.18f) * cellWidth,
                    y,
                    (col + 0.82f) * cellWidth,
                    y,
                    doorPaint
                )
            }
        }
    }
    return bitmap.asImageBitmap()
}

private fun buildWallPath(maze: Maze, cellWidth: Float, cellHeight: Float): android.graphics.Path {
    val path = android.graphics.Path()
    for (row in 0 until maze.rows) {
        for (col in 0 until maze.columns) {
            if (maze.tiles[row * maze.columns + col] == Tile.WALL) {
                addWallTile(path, maze, col, row, cellWidth, cellHeight)
            }
        }
    }
    return path
}

private fun addWallTile(
    path: android.graphics.Path,
    maze: Maze,
    col: Int,
    row: Int,
    cellWidth: Float,
    cellHeight: Float
) {
    val left = col * cellWidth
    val top = row * cellHeight
    val right = left + cellWidth
    val bottom = top + cellHeight
    if (!isWall(maze, col, row - 1)) {
        path.moveTo(left, top)
        path.lineTo(right, top)
    }
    if (!isWall(maze, col, row + 1)) {
        path.moveTo(left, bottom)
        path.lineTo(right, bottom)
    }
    if (!isWall(maze, col - 1, row)) {
        path.moveTo(left, top)
        path.lineTo(left, bottom)
    }
    if (!isWall(maze, col + 1, row)) {
        path.moveTo(right, top)
        path.lineTo(right, bottom)
    }
}

private fun isWall(maze: Maze, col: Int, row: Int): Boolean {
    if (row !in 0 until maze.rows) return false
    if (col !in 0 until maze.columns) return false
    return maze.tiles[row * maze.columns + col] == Tile.WALL
}

private const val NANOS_PER_SECOND = 1_000_000_000f

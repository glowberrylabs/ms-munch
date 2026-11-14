package com.glowberrylabs.msmunch.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun TilePos.toPosition(): Position = Position(col.toDouble(), row.toDouble())

data class Position(val x: Double, val y: Double) {

    fun tile(): TilePos = TilePos(x.roundToInt(), y.roundToInt())

    fun tile(maze: Maze): TilePos = TilePos(maze.wrapColumn(x.roundToInt()), y.roundToInt())

    fun isNearCenter(tolerance: Double): Boolean {
        val center = tile()
        return abs(x - center.col) <= tolerance && abs(y - center.row) <= tolerance
    }

    fun snappedToCenter(): Position {
        val center = tile()
        return Position(center.col.toDouble(), center.row.toDouble())
    }

    fun distanceTo(other: Position): Double {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceSquaredTo(other: Position): Double {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }
}

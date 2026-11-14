package com.glowberrylabs.msmunch.game

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    NONE(0, 0);

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
            NONE -> NONE
        }

    val isHorizontal: Boolean
        get() = dx != 0

    val isVertical: Boolean
        get() = dy != 0

    fun offset(pos: TilePos): TilePos = TilePos(pos.col + dx, pos.row + dy)

    fun offset(pos: TilePos, steps: Int): TilePos = TilePos(pos.col + dx * steps, pos.row + dy * steps)
}

package com.glowberrylabs.msmunch.game

data class MsMunch(
    val pos: Position,
    val direction: Direction = Direction.NONE,
    val desiredDirection: Direction? = null
)

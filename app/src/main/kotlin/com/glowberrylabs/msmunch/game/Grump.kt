package com.glowberrylabs.msmunch.game

data class Grump(
    val kind: GhostKind,
    val pos: Position,
    val direction: Direction = Direction.LEFT,
    val mode: GhostMode = GhostMode.IN_HOUSE,
    val releaseAtDots: Int = 0,
    val houseDelayRemaining: Double = 0.0,
    val homeSlot: TilePos = TilePos(0, 0),
    val decisionTile: Int = -1
)

package com.glowberrylabs.msmunch.game

data class GameState(
    val phase: GamePhase,
    val maze: Maze,
    val board: List<Tile>,
    val player: MsMunch,
    val level: Int = 1
)

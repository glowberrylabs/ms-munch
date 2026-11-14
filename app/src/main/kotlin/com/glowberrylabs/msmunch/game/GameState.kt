package com.glowberrylabs.msmunch.game

data class GameState(
    val phase: GamePhase,
    val maze: Maze,
    val board: List<Tile>,
    val player: MsMunch,
    val level: Int = 1,
    val grumps: List<Grump> = emptyList(),
    val globalMode: GhostMode = GhostMode.SCATTER,
    val modeIndex: Int = 0,
    val modeRemaining: Double = 0.0,
    val frightenedRemaining: Double = 0.0,
    val ghostChain: Int = 0,
    val dotsEaten: Int = 0
)

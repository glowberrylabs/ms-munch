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
    val dotsEaten: Int = 0,
    val score: Int = 0,
    val highScore: Int = 0,
    val lives: Int = 3,
    val dotsRemaining: Int = 0,
    val pelletsRemaining: Int = 0,
    val fruit: Fruit? = null,
    val fruitSpawned: Int = 0,
    val extraLifeAwarded: Boolean = false,
    val phaseRemaining: Double = 0.0
)

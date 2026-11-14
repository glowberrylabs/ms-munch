package com.glowberrylabs.msmunch.game

object RoundFlow {
    const val READY_SECONDS = 2.0
    const val DEATH_PAUSE_SECONDS = 1.5
    const val LEVEL_CLEAR_SECONDS = 2.0

    fun newGame(maze: Maze, highScore: Int): GameState {
        val schedule = LevelTable.schedule(1)
        return GameState(
            phase = GamePhase.START,
            maze = maze,
            board = maze.tiles,
            player = MsMunch(pos = maze.playerStart.toPosition()),
            grumps = initialGrumps(maze),
            globalMode = GhostMode.SCATTER,
            modeIndex = 0,
            modeRemaining = schedule.first().seconds,
            highScore = highScore,
            dotsRemaining = maze.dotCount(),
            pelletsRemaining = maze.pelletCount()
        )
    }

    fun start(state: GameState): GameState = when (state.phase) {
        GamePhase.START -> state.copy(
            phase = GamePhase.READY,
            phaseRemaining = READY_SECONDS,
            player = state.player.copy(pos = state.maze.playerStart.toPosition())
        )
        else -> state
    }

    fun respawn(state: GameState): GameState {
        val schedule = LevelTable.schedule(state.level)
        return state.copy(
            phase = GamePhase.READY,
            phaseRemaining = READY_SECONDS,
            player = MsMunch(pos = state.maze.playerStart.toPosition()),
            grumps = initialGrumps(state.maze),
            globalMode = GhostMode.SCATTER,
            modeIndex = 0,
            modeRemaining = schedule.first().seconds,
            frightenedRemaining = 0.0,
            ghostChain = 0,
            fruit = null
        )
    }

    fun nextLevel(state: GameState): GameState {
        val level = state.level + 1
        val maze = Mazes.forLevel(level)
        val schedule = LevelTable.schedule(level)
        return state.copy(
            phase = GamePhase.READY,
            phaseRemaining = READY_SECONDS,
            level = level,
            maze = maze,
            board = maze.tiles,
            player = MsMunch(pos = maze.playerStart.toPosition()),
            grumps = initialGrumps(maze),
            globalMode = GhostMode.SCATTER,
            modeIndex = 0,
            modeRemaining = schedule.first().seconds,
            frightenedRemaining = 0.0,
            ghostChain = 0,
            dotsEaten = 0,
            dotsRemaining = maze.dotCount(),
            pelletsRemaining = maze.pelletCount(),
            fruit = null,
            fruitSpawned = 0
        )
    }

    fun initialGrumps(maze: Maze): List<Grump> {
        val slots = maze.houseSlots
        return listOf(
            Grump(
                kind = GhostKind.COCOA,
                pos = maze.houseExit.toPosition(),
                direction = Direction.LEFT,
                mode = GhostMode.SCATTER,
                releaseAtDots = 0,
                homeSlot = slots[0]
            ),
            Grump(
                kind = GhostKind.BUBBLE,
                pos = slots[1].toPosition(),
                direction = Direction.UP,
                mode = GhostMode.IN_HOUSE,
                releaseAtDots = 20,
                homeSlot = slots[1]
            ),
            Grump(
                kind = GhostKind.TAFFY,
                pos = slots[2].toPosition(),
                direction = Direction.DOWN,
                mode = GhostMode.IN_HOUSE,
                releaseAtDots = 45,
                homeSlot = slots[2]
            ),
            Grump(
                kind = GhostKind.MINT,
                pos = slots[3].toPosition(),
                direction = Direction.UP,
                mode = GhostMode.IN_HOUSE,
                releaseAtDots = 70,
                homeSlot = slots[3]
            )
        )
    }
}

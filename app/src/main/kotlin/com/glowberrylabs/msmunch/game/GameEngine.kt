package com.glowberrylabs.msmunch.game

import kotlin.math.min

class GameEngine {

    fun newGame(maze: Maze = Mazes.forLevel(1)): GameState = GameState(
        phase = GamePhase.START,
        maze = maze,
        board = maze.tiles,
        player = MsMunch(pos = maze.playerStart.toPosition())
    )

    fun start(state: GameState): GameState = when (state.phase) {
        GamePhase.START -> state.copy(
            phase = GamePhase.READY,
            player = state.player.copy(pos = state.maze.playerStart.toPosition())
        )
        else -> state
    }

    fun turn(state: GameState, direction: Direction): GameState =
        state.copy(player = state.player.copy(desiredDirection = direction))

    fun advance(state: GameState, dtSeconds: Double): Pair<GameState, List<GameEvent>> {
        if (state.phase != GamePhase.RUNNING || dtSeconds <= 0.0) {
            return state to emptyList()
        }
        var current = state
        var remaining = dtSeconds
        while (remaining > EPSILON) {
            val step = min(remaining, MAX_STEP)
            current = advanceStep(current, step)
            remaining -= step
        }
        return current to emptyList()
    }

    private fun advanceStep(state: GameState, dt: Double): GameState {
        val player = ActorMotion.stepPlayer(
            maze = state.maze,
            player = state.player,
            speed = LevelTable.playerSpeed(state.level),
            dt = dt
        )
        return state.copy(player = player)
    }

    companion object {
        const val MAX_STEP = 1.0 / 120.0
        private const val EPSILON = 1e-9
    }
}

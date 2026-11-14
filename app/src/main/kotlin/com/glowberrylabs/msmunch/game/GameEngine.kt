package com.glowberrylabs.msmunch.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(private val random: Random = Random.Default) {

    private val ghosts = GhostController(random)

    fun newGame(maze: Maze = Mazes.forLevel(1), highScore: Int = 0): GameState = RoundFlow.newGame(maze, highScore)

    fun start(state: GameState): GameState = RoundFlow.start(state)

    fun turn(state: GameState, direction: Direction): GameState =
        state.copy(player = state.player.copy(desiredDirection = direction))

    fun frighten(state: GameState): GameState {
        val duration = LevelTable.frightenedSeconds(state.level)
        val grumps = state.grumps.map { grump ->
            if (grump.mode.isActive()) {
                grump.copy(
                    mode = GhostMode.FRIGHTENED,
                    direction = grump.direction.opposite,
                    decisionTile = -1
                )
            } else {
                grump
            }
        }
        return state.copy(
            grumps = grumps,
            frightenedRemaining = duration,
            ghostChain = 0
        )
    }

    fun advance(state: GameState, dtSeconds: Double): Pair<GameState, List<GameEvent>> {
        if (dtSeconds <= 0.0) return state to emptyList()
        return when (state.phase) {
            GamePhase.READY -> advanceReady(state, dtSeconds)
            GamePhase.RUNNING -> advanceRunning(state, dtSeconds)
            GamePhase.DYING -> advanceDying(state, dtSeconds)
            GamePhase.LEVEL_CLEAR -> advanceLevelClear(state, dtSeconds)
            else -> state to emptyList()
        }
    }

    private fun advanceReady(state: GameState, dt: Double): Pair<GameState, List<GameEvent>> {
        val remaining = state.phaseRemaining - dt
        return if (remaining > 0.0) {
            state.copy(phaseRemaining = remaining) to emptyList()
        } else {
            state.copy(phase = GamePhase.RUNNING, phaseRemaining = 0.0) to listOf(GameEvent.LevelStart)
        }
    }

    private fun advanceDying(state: GameState, dt: Double): Pair<GameState, List<GameEvent>> {
        val remaining = state.phaseRemaining - dt
        if (remaining > 0.0) {
            return state.copy(phaseRemaining = remaining) to emptyList()
        }
        return if (state.lives > 0) {
            RoundFlow.respawn(state) to listOf(GameEvent.LevelStart)
        } else {
            val highScore = max(state.highScore, state.score)
            state.copy(phase = GamePhase.GAME_OVER, phaseRemaining = 0.0, highScore = highScore) to
                listOf(GameEvent.GameOver)
        }
    }

    private fun advanceLevelClear(state: GameState, dt: Double): Pair<GameState, List<GameEvent>> {
        val remaining = state.phaseRemaining - dt
        return if (remaining > 0.0) {
            state.copy(phaseRemaining = remaining) to emptyList()
        } else {
            RoundFlow.nextLevel(state) to listOf(GameEvent.LevelStart)
        }
    }

    private fun advanceRunning(state: GameState, dtSeconds: Double): Pair<GameState, List<GameEvent>> {
        var current = state
        var remaining = dtSeconds
        val events = mutableListOf<GameEvent>()
        while (remaining > EPSILON && current.phase == GamePhase.RUNNING) {
            val step = min(remaining, MAX_STEP)
            val (next, stepEvents) = advanceStep(current, step)
            current = next
            events += stepEvents
            remaining -= step
        }
        return current to events
    }

    private fun advanceStep(state: GameState, dt: Double): Pair<GameState, List<GameEvent>> {
        var current = updateTimers(state, dt)
        current = current.copy(
            player = ActorMotion.stepPlayer(
                maze = current.maze,
                player = current.player,
                speed = LevelTable.playerSpeed(current.level),
                dt = dt
            )
        )

        val (fed, eatingEvents) = resolveEating(current)
        val events = eatingEvents.toMutableList()
        current = fed
        if (current.phase != GamePhase.RUNNING) return current to events

        current = current.copy(
            grumps = current.grumps.map { ghosts.update(current, it, dt) }
        )
        if (current.phase != GamePhase.RUNNING) return current to events

        val (resolved, collisionEvents) = CollisionResolver.resolve(current)
        events += collisionEvents
        return resolved to events
    }

    private fun updateTimers(state: GameState, dt: Double): GameState {
        var current = updateFruitTimer(state, dt)
        if (current.frightenedRemaining > 0.0) {
            val remaining = current.frightenedRemaining - dt
            return if (remaining > 0.0) {
                current.copy(frightenedRemaining = remaining)
            } else {
                current.copy(
                    frightenedRemaining = 0.0,
                    grumps = current.grumps.map { grump ->
                        if (grump.mode == GhostMode.FRIGHTENED) {
                            grump.copy(mode = current.globalMode, decisionTile = -1)
                        } else {
                            grump
                        }
                    }
                )
            }
        }

        if (!current.modeRemaining.isFinite()) return current
        val remaining = current.modeRemaining - dt
        return if (remaining > 0.0) current.copy(modeRemaining = remaining) else advanceMode(current)
    }

    private fun updateFruitTimer(state: GameState, dt: Double): GameState {
        val fruit = state.fruit ?: return state
        val remaining = fruit.remaining - dt
        return if (remaining > 0.0) {
            state.copy(fruit = fruit.copy(remaining = remaining))
        } else {
            state.copy(fruit = null)
        }
    }

    private fun advanceMode(state: GameState): GameState {
        val schedule = LevelTable.schedule(state.level)
        val nextIndex = (state.modeIndex + 1).coerceAtMost(schedule.lastIndex)
        val next = schedule[nextIndex]
        val grumps = state.grumps.map { grump ->
            if (grump.mode == GhostMode.SCATTER || grump.mode == GhostMode.CHASE) {
                grump.copy(
                    mode = next.mode,
                    direction = grump.direction.opposite,
                    decisionTile = -1
                )
            } else {
                grump
            }
        }
        return state.copy(
            grumps = grumps,
            globalMode = next.mode,
            modeIndex = nextIndex,
            modeRemaining = next.seconds
        )
    }

    private fun resolveEating(state: GameState): Pair<GameState, List<GameEvent>> {
        var current = state
        val events = mutableListOf<GameEvent>()
        val tilePos = current.player.pos.tile(current.maze)
        val index = current.maze.indexOf(tilePos)
        val tile = current.board[index]

        if (tile.isEdible) {
            val board = current.board.toMutableList()
            board[index] = Tile.EMPTY
            current = current.copy(board = board, dotsEaten = current.dotsEaten + 1)
            current = when (tile) {
                Tile.DOT -> {
                    events += GameEvent.Chomp
                    current.copy(
                        score = current.score + Scoring.DOT_SCORE,
                        dotsRemaining = current.dotsRemaining - 1
                    )
                }
                Tile.PELLET -> {
                    events += GameEvent.Pellet
                    frighten(
                        current.copy(
                            score = current.score + Scoring.PELLET_SCORE,
                            pelletsRemaining = current.pelletsRemaining - 1
                        )
                    )
                }
                else -> current
            }
        }

        val fruit = current.fruit
        if (fruit != null && fruit.pos == tilePos) {
            events += GameEvent.Fruit
            current = current.copy(score = current.score + LevelTable.fruitValue(current.level), fruit = null)
        }

        current = spawnFruitIfNeeded(current)
        current = Scoring.awardExtraLife(current, events)
        current = checkLevelClear(current, events)
        return current to events
    }

    private fun spawnFruitIfNeeded(state: GameState): GameState {
        val threshold = when (state.fruitSpawned) {
            0 -> FIRST_FRUIT_DOTS
            1 -> SECOND_FRUIT_DOTS
            else -> return state
        }
        if (state.dotsEaten < threshold) return state
        return state.copy(
            fruit = Fruit(state.maze.fruitAnchor, FRUIT_LIFETIME),
            fruitSpawned = state.fruitSpawned + 1
        )
    }

    private fun checkLevelClear(state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.dotsRemaining > 0 || state.pelletsRemaining > 0) return state
        events += GameEvent.LevelClear
        return state.copy(
            phase = GamePhase.LEVEL_CLEAR,
            phaseRemaining = RoundFlow.LEVEL_CLEAR_SECONDS,
            fruit = null,
            frightenedRemaining = 0.0,
            ghostChain = 0
        )
    }

    private fun GhostMode.isActive(): Boolean =
        this == GhostMode.SCATTER || this == GhostMode.CHASE || this == GhostMode.FRIGHTENED

    companion object {
        const val MAX_STEP = 1.0 / 120.0
        private const val EPSILON = 1e-9
        private const val FIRST_FRUIT_DOTS = 70
        private const val SECOND_FRUIT_DOTS = 170
        private const val FRUIT_LIFETIME = 9.0
    }
}

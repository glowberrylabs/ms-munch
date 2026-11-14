package com.glowberrylabs.msmunch.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(private val random: Random = Random.Default) {

    fun newGame(maze: Maze = Mazes.forLevel(1)): GameState {
        val schedule = LevelTable.schedule(1)
        val slots = maze.houseSlots
        val grumps = listOf(
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
        return GameState(
            phase = GamePhase.START,
            maze = maze,
            board = maze.tiles,
            player = MsMunch(pos = maze.playerStart.toPosition()),
            grumps = grumps,
            globalMode = GhostMode.SCATTER,
            modeIndex = 0,
            modeRemaining = schedule.first().seconds
        )
    }

    fun start(state: GameState): GameState = when (state.phase) {
        GamePhase.START -> state.copy(
            phase = GamePhase.READY,
            player = state.player.copy(pos = state.maze.playerStart.toPosition())
        )
        else -> state
    }

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
        if (state.phase != GamePhase.RUNNING || dtSeconds <= 0.0) {
            return state to emptyList()
        }
        var current = state
        var remaining = dtSeconds
        val events = mutableListOf<GameEvent>()
        while (remaining > EPSILON) {
            val step = min(remaining, MAX_STEP)
            val (next, stepEvents) = advanceStep(current, step)
            current = next
            events += stepEvents
            remaining -= step
        }
        events += collectEvents(state, current)
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
        current = current.copy(
            grumps = current.grumps.map { updateGrump(current, it, dt) }
        )
        return resolveCollisions(current)
    }

    private fun updateTimers(state: GameState, dt: Double): GameState {
        if (state.frightenedRemaining > 0.0) {
            val remaining = state.frightenedRemaining - dt
            return if (remaining > 0.0) {
                state.copy(frightenedRemaining = remaining)
            } else {
                state.copy(
                    frightenedRemaining = 0.0,
                    grumps = state.grumps.map { grump ->
                        if (grump.mode == GhostMode.FRIGHTENED) {
                            grump.copy(mode = state.globalMode, decisionTile = -1)
                        } else {
                            grump
                        }
                    }
                )
            }
        }

        if (!state.modeRemaining.isFinite()) return state
        val remaining = state.modeRemaining - dt
        return if (remaining > 0.0) state.copy(modeRemaining = remaining) else advanceMode(state)
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

    private fun updateGrump(state: GameState, grump: Grump, dt: Double): Grump = when (grump.mode) {
        GhostMode.EYES -> updateEyes(state, grump, dt)
        GhostMode.IN_HOUSE -> updateHouse(state, grump, dt)
        else -> updateActive(state, grump, dt)
    }

    private fun updateEyes(state: GameState, grump: Grump, dt: Double): Grump {
        if (state.maze.houseDistanceAt(grump.pos.tile(state.maze)) == 0) {
            return grump.copy(
                mode = GhostMode.IN_HOUSE,
                pos = grump.homeSlot.toPosition(),
                direction = Direction.UP,
                houseDelayRemaining = REVIVE_DELAY,
                releaseAtDots = state.dotsEaten,
                decisionTile = -1
            )
        }
        return GhostMotion.step(
            maze = state.maze,
            grump = grump,
            speed = EYES_SPEED,
            dt = dt,
            target = state.maze.door,
            random = random,
            pathDistances = state.maze.houseDistances
        )
    }

    private fun updateHouse(state: GameState, grump: Grump, dt: Double): Grump {
        val delay = max(0.0, grump.houseDelayRemaining - dt)
        if (delay > 0.0 || state.dotsEaten < grump.releaseAtDots) {
            return grump.copy(houseDelayRemaining = delay)
        }
        val stepped = GhostMotion.step(
            maze = state.maze,
            grump = grump.copy(houseDelayRemaining = delay),
            speed = LevelTable.ghostSpeed(state.level),
            dt = dt,
            target = state.maze.houseExit,
            random = random,
            pathDistances = state.maze.exitDistances
        )
        return if (state.maze.exitDistanceAt(stepped.pos.tile(state.maze)) == 0) {
            stepped.copy(
                mode = state.globalMode,
                direction = Direction.LEFT,
                decisionTile = -1
            )
        } else {
            stepped
        }
    }

    private fun updateActive(state: GameState, grump: Grump, dt: Double): Grump {
        val baseSpeed = if (grump.mode == GhostMode.FRIGHTENED) {
            LevelTable.ghostSpeed(state.level) * FRIGHTENED_SPEED_FACTOR
        } else {
            LevelTable.ghostSpeed(state.level)
        }
        val tunnelFactor = if (state.maze.tileAt(grump.pos.tile(state.maze)) == Tile.TUNNEL) {
            TUNNEL_SPEED_FACTOR
        } else {
            1.0
        }
        return GhostMotion.step(
            maze = state.maze,
            grump = grump,
            speed = baseSpeed * tunnelFactor,
            dt = dt,
            target = GhostTargets.targetFor(grump.kind, state),
            random = random
        )
    }

    private fun resolveCollisions(state: GameState): Pair<GameState, List<GameEvent>> {
        var chain = state.ghostChain
        val events = mutableListOf<GameEvent>()
        val grumps = state.grumps.map { grump ->
            val caught = grump.mode == GhostMode.FRIGHTENED &&
                state.player.pos.distanceSquaredTo(grump.pos) <= COLLISION_DISTANCE_SQUARED
            if (caught) {
                chain = min(chain + 1, MAX_GHOST_CHAIN)
                events += GameEvent.GhostEaten
                grump.copy(
                    mode = GhostMode.EYES,
                    direction = grump.direction.opposite,
                    decisionTile = -1
                )
            } else {
                grump
            }
        }
        return if (events.isEmpty()) {
            state to emptyList()
        } else {
            state.copy(grumps = grumps, ghostChain = chain) to events
        }
    }

    private fun collectEvents(before: GameState, after: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        if (after.phase == GamePhase.LEVEL_CLEAR && before.phase != GamePhase.LEVEL_CLEAR) {
            events += GameEvent.LevelClear
        }
        if (after.phase == GamePhase.GAME_OVER && before.phase != GamePhase.GAME_OVER) {
            events += GameEvent.GameOver
        }
        return events
    }

    private fun GhostMode.isActive(): Boolean =
        this == GhostMode.SCATTER || this == GhostMode.CHASE || this == GhostMode.FRIGHTENED

    companion object {
        const val MAX_STEP = 1.0 / 120.0
        private const val EPSILON = 1e-9
        private const val EYES_SPEED = 12.0
        private const val FRIGHTENED_SPEED_FACTOR = 0.55
        private const val TUNNEL_SPEED_FACTOR = 0.6
        private const val REVIVE_DELAY = 1.0
        private const val MAX_GHOST_CHAIN = 4
        private const val COLLISION_DISTANCE_SQUARED = 0.7 * 0.7
    }
}

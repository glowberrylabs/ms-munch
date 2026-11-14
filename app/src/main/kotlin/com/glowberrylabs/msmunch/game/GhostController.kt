package com.glowberrylabs.msmunch.game

import kotlin.math.max
import kotlin.random.Random

class GhostController(private val random: Random) {

    fun update(state: GameState, grump: Grump, dt: Double): Grump = when (grump.mode) {
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

    private companion object {
        const val EYES_SPEED = 12.0
        const val FRIGHTENED_SPEED_FACTOR = 0.55
        const val TUNNEL_SPEED_FACTOR = 0.6
        const val REVIVE_DELAY = 1.0
    }
}

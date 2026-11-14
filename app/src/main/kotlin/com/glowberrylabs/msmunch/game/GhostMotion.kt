package com.glowberrylabs.msmunch.game

import kotlin.random.Random

object GhostMotion {

    const val TURN_TOLERANCE = 0.2

    fun step(
        maze: Maze,
        grump: Grump,
        speed: Double,
        dt: Double,
        target: TilePos,
        random: Random,
        pathDistances: IntArray? = null
    ): Grump {
        var current = grump
        val tile = current.pos.tile(maze)
        val tileIndex = maze.indexOf(tile)
        if (current.pos.isNearCenter(TURN_TOLERANCE) && current.decisionTile != tileIndex) {
            current = current.copy(
                pos = current.pos.snappedToCenter(),
                direction = chooseDirection(maze, tile, current, target, random, pathDistances),
                decisionTile = tileIndex
            )
        }

        val nextTile = current.direction.offset(current.pos.tile(maze))
        if (!canEnter(maze, nextTile, current.mode)) {
            val centered = current.pos.snappedToCenter()
            val chosen = chooseDirection(
                maze = maze,
                tile = current.pos.tile(maze),
                grump = current,
                target = target,
                random = random,
                pathDistances = pathDistances,
                allowReverse = true
            )
            return current.copy(pos = centered, direction = chosen, decisionTile = -1)
        }

        val step = speed * dt
        var x = current.pos.x + current.direction.dx * step
        var y = current.pos.y + current.direction.dy * step
        if (x < -0.5) x += maze.columns
        if (x > maze.columns - 0.5) x -= maze.columns
        return current.copy(pos = Position(x, y))
    }

    private fun chooseDirection(
        maze: Maze,
        tile: TilePos,
        grump: Grump,
        target: TilePos,
        random: Random,
        pathDistances: IntArray?,
        allowReverse: Boolean = false
    ): Direction {
        val legal = DIRECTION_ORDER.filter { canEnter(maze, it.offset(tile), grump.mode) }
        if (legal.isEmpty()) return grump.direction.opposite
        val forward = legal.filter { it != grump.direction.opposite }
        val options = if (!allowReverse && forward.isNotEmpty()) forward else legal

        if (grump.mode == GhostMode.FRIGHTENED) {
            return options[random.nextInt(options.size)]
        }
        if (pathDistances != null) {
            return options.minBy { pathDistances[maze.indexOf(it.offset(tile))] }
        }
        return options.minBy { distanceSquared(it.offset(tile), target) }
    }

    private fun canEnter(maze: Maze, pos: TilePos, mode: GhostMode): Boolean {
        val tile = maze.tileAt(pos)
        if (tile == Tile.WALL) return false
        return when (mode) {
            GhostMode.EYES, GhostMode.IN_HOUSE -> true
            else -> tile != Tile.DOOR && tile != Tile.HOUSE
        }
    }

    private fun distanceSquared(pos: TilePos, target: TilePos): Int {
        val dx = pos.col - target.col
        val dy = pos.row - target.row
        return dx * dx + dy * dy
    }

    private val DIRECTION_ORDER = listOf(
        Direction.UP,
        Direction.LEFT,
        Direction.DOWN,
        Direction.RIGHT
    )
}

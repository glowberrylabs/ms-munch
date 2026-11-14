package com.glowberrylabs.msmunch.game

object ActorMotion {

    const val TURN_TOLERANCE = 0.2

    fun stepPlayer(maze: Maze, player: MsMunch, speed: Double, dt: Double): MsMunch {
        var direction = player.direction
        var desired = player.desiredDirection
        var position = player.pos

        if (desired == direction) {
            desired = null
        }

        if (desired != null) {
            if (direction != Direction.NONE && desired == direction.opposite) {
                direction = desired
                desired = null
            } else if (canTurn(maze, position, desired)) {
                position = position.snappedToCenter()
                direction = desired
                desired = null
            }
        }

        if (direction == Direction.NONE) {
            return player.copy(
                pos = position,
                direction = direction,
                desiredDirection = desired
            )
        }

        val tile = position.tile(maze)
        val nextTile = direction.offset(tile)
        if (!maze.isPlayerWalkable(nextTile)) {
            return player.copy(
                pos = position.snappedToCenter(),
                direction = direction,
                desiredDirection = desired
            )
        }

        val step = speed * dt
        var x = position.x + direction.dx * step
        var y = position.y + direction.dy * step
        if (x < -0.5) x += maze.columns
        if (x > maze.columns - 0.5) x -= maze.columns
        return MsMunch(Position(x, y), direction, desired)
    }

    private fun canTurn(maze: Maze, position: Position, direction: Direction): Boolean {
        if (!position.isNearCenter(TURN_TOLERANCE)) return false
        val tile = position.tile(maze)
        return maze.isPlayerWalkable(direction.offset(tile))
    }
}

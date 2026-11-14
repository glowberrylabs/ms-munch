package com.glowberrylabs.msmunch.game

object MazeValidator {

    fun validate(maze: Maze): List<String> {
        val errors = mutableListOf<String>()

        if (maze.columns != 28) errors += "Expected 28 columns, found ${maze.columns}"
        if (maze.rows != 31) errors += "Expected 31 rows, found ${maze.rows}"
        if (maze.pelletCount() != 4) errors += "Expected 4 pellets, found ${maze.pelletCount()}"
        if (maze.dotCount() == 0) errors += "Maze has no dots"

        if (!maze.isPlayerWalkable(maze.playerStart)) {
            errors += "Player start is not walkable"
        }
        if (!maze.isPlayerWalkable(maze.houseExit)) {
            errors += "Ghost house exit is not walkable"
        }
        if (!maze.isPlayerWalkable(maze.fruitAnchor)) {
            errors += "Fruit anchor is not walkable"
        }
        if (maze.tileAt(maze.door) != Tile.DOOR) {
            errors += "Ghost house door is not a door tile"
        }
        if (maze.houseSlots.any { maze.tileAt(it) != Tile.HOUSE }) {
            errors += "Ghost house slots must be house tiles"
        }

        errors += validateBorder(maze)
        errors += validatePlayerReachability(maze)
        errors += validateHouseReachability(maze)
        errors += validateTunnel(maze)
        errors += validateDeadEnds(maze)

        return errors
    }

    private fun validateBorder(maze: Maze): List<String> {
        val errors = mutableListOf<String>()
        for (col in 0 until maze.columns) {
            for (row in listOf(0, maze.rows - 1)) {
                if (maze.tiles[row * maze.columns + col] != Tile.WALL) {
                    errors += "Border tile ($col, $row) must be a wall"
                }
            }
        }
        for (row in 0 until maze.rows) {
            for (col in listOf(0, maze.columns - 1)) {
                val isTunnel = row in maze.tunnelRows &&
                    maze.tiles[row * maze.columns + col] == Tile.TUNNEL
                if (!isTunnel && maze.tiles[row * maze.columns + col] != Tile.WALL) {
                    errors += "Border tile ($col, $row) must be a wall or a tunnel"
                }
            }
        }
        return errors
    }

    private fun validatePlayerReachability(maze: Maze): List<String> {
        val reached = walk(maze, maze.playerStart) { maze.isPlayerWalkable(it) }
        val errors = mutableListOf<String>()
        for (row in 0 until maze.rows) {
            for (col in 0 until maze.columns) {
                val tile = maze.tiles[row * maze.columns + col]
                val needsReach = tile != Tile.WALL && tile != Tile.DOOR && tile != Tile.HOUSE
                if (needsReach && !reached[row * maze.columns + col]) {
                    errors += "Unreachable tile ($col, $row): $tile"
                }
            }
        }
        return errors
    }

    private fun validateHouseReachability(maze: Maze): List<String> {
        val reached = walk(maze, maze.houseExit) { maze.isGhostWalkable(it) }
        val errors = mutableListOf<String>()
        for (row in 0 until maze.rows) {
            for (col in 0 until maze.columns) {
                val tile = maze.tiles[row * maze.columns + col]
                val needsReach = tile == Tile.DOOR || tile == Tile.HOUSE
                if (needsReach && !reached[row * maze.columns + col]) {
                    errors += "Ghost house tile ($col, $row) is not reachable from the exit"
                }
            }
        }
        return errors
    }

    private fun validateTunnel(maze: Maze): List<String> {
        val errors = mutableListOf<String>()
        for (row in maze.tunnelRows) {
            if (row !in 0 until maze.rows) {
                errors += "Tunnel row $row is outside the maze"
                continue
            }
            if (maze.tileAt(0, row) != Tile.TUNNEL) {
                errors += "Tunnel on row $row does not reach the left edge"
            }
            if (maze.tileAt(maze.columns - 1, row) != Tile.TUNNEL) {
                errors += "Tunnel on row $row does not reach the right edge"
            }
        }
        return errors
    }

    private fun validateDeadEnds(maze: Maze): List<String> {
        val errors = mutableListOf<String>()
        for (row in 0 until maze.rows) {
            for (col in 0 until maze.columns) {
                val pos = TilePos(col, row)
                if (!maze.isPlayerWalkable(pos)) continue
                val exits = NEIGHBORS.count { offset ->
                    maze.isPlayerWalkable(TilePos(pos.col + offset.col, pos.row + offset.row))
                }
                if (exits <= 1) errors += "Dead end at ($col, $row)"
            }
        }
        return errors
    }

    private fun enqueue(
        maze: Maze,
        queue: ArrayDeque<TilePos>,
        visited: BooleanArray,
        pos: TilePos,
        offset: TilePos,
        canEnter: (TilePos) -> Boolean
    ) {
        val nextRow = pos.row + offset.row
        if (nextRow !in 0 until maze.rows) return
        val next = TilePos(maze.wrapColumn(pos.col + offset.col), nextRow)
        val index = nextRow * maze.columns + next.col
        if (visited[index] || !canEnter(next)) return
        visited[index] = true
        queue.addLast(next)
    }

    private fun walk(maze: Maze, start: TilePos, canEnter: (TilePos) -> Boolean): BooleanArray {
        val visited = BooleanArray(maze.columns * maze.rows)
        if (!canEnter(start)) return visited
        val queue = ArrayDeque<TilePos>()
        queue.addLast(start)
        visited[start.row * maze.columns + start.col] = true
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            for (offset in NEIGHBORS) {
                enqueue(maze, queue, visited, pos, offset, canEnter)
            }
        }
        return visited
    }

    private val NEIGHBORS = listOf(
        TilePos(0, -1),
        TilePos(-1, 0),
        TilePos(0, 1),
        TilePos(1, 0)
    )
}

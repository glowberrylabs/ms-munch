package com.glowberrylabs.msmunch.game

data class Maze(
    val name: String,
    val columns: Int,
    val rows: Int,
    val tiles: List<Tile>,
    val playerStart: TilePos,
    val door: TilePos,
    val houseExit: TilePos,
    val fruitAnchor: TilePos,
    val houseSlots: List<TilePos>,
    val scatterTargets: List<TilePos>,
    val tunnelRow: Int
) {
    init {
        require(tiles.size == columns * rows)
        require(houseSlots.isNotEmpty())
        require(scatterTargets.size == 4)
    }

    fun tileAt(col: Int, row: Int): Tile {
        if (row !in 0 until rows) return Tile.WALL
        val wrapped = wrapColumn(col)
        return tiles[row * columns + wrapped]
    }

    fun tileAt(pos: TilePos): Tile = tileAt(pos.col, pos.row)

    fun wrapColumn(col: Int): Int = ((col % columns) + columns) % columns

    fun isPlayerWalkable(pos: TilePos): Boolean = when (tileAt(pos)) {
        Tile.WALL, Tile.DOOR, Tile.HOUSE -> false
        else -> true
    }

    fun isGhostWalkable(pos: TilePos): Boolean = tileAt(pos) != Tile.WALL

    fun isHouse(pos: TilePos): Boolean = when (tileAt(pos)) {
        Tile.HOUSE, Tile.DOOR -> true
        else -> false
    }

    fun dotCount(): Int = tiles.count { it == Tile.DOT }

    fun pelletCount(): Int = tiles.count { it == Tile.PELLET }

    fun indexOf(pos: TilePos): Int = wrapColumn(pos.col) + pos.row * columns

    val exitDistances: IntArray = distanceField(listOf(houseExit))

    val houseDistances: IntArray = distanceField(
        tiles.indices.filter { tiles[it] == Tile.HOUSE }
            .map { TilePos(it % columns, it / columns) }
    )

    fun exitDistanceAt(pos: TilePos): Int = exitDistances[indexOf(pos)]

    fun houseDistanceAt(pos: TilePos): Int = houseDistances[indexOf(pos)]

    private fun distanceField(sources: List<TilePos>): IntArray {
        val distances = IntArray(columns * rows) { UNREACHABLE }
        val queue = ArrayDeque<TilePos>()
        sources.forEach { source ->
            distances[indexOf(source)] = 0
            queue.addLast(source)
        }
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            for (offset in NEIGHBOR_OFFSETS) {
                enqueueNeighbor(queue, distances, pos, offset)
            }
        }
        return distances
    }

    private fun enqueueNeighbor(queue: ArrayDeque<TilePos>, distances: IntArray, pos: TilePos, offset: TilePos) {
        val next = TilePos(wrapColumn(pos.col + offset.col), pos.row + offset.row)
        if (next.row !in 0 until rows) return
        val index = indexOf(next)
        if (distances[index] != UNREACHABLE || !isGhostWalkable(next)) return
        distances[index] = distances[indexOf(pos)] + 1
        queue.addLast(next)
    }

    private companion object {
        const val UNREACHABLE = Int.MAX_VALUE
        val NEIGHBOR_OFFSETS = listOf(
            TilePos(0, -1),
            TilePos(-1, 0),
            TilePos(0, 1),
            TilePos(1, 0)
        )
    }
}

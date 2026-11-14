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
}

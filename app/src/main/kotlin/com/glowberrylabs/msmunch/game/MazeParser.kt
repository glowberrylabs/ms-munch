package com.glowberrylabs.msmunch.game

import kotlin.math.abs

object MazeParser {

    fun parse(name: String, layout: String): Maze {
        val rows = layout.lines().filter { it.isNotEmpty() }
        require(rows.isNotEmpty()) { "Maze $name is empty" }
        val columns = rows.first().length
        require(columns > 0) { "Maze $name has no columns" }
        require(rows.all { it.length == columns }) {
            "Maze $name has rows with different widths"
        }

        val board = parseBoard(name, rows)
        val centerColumn = (columns - 1) / 2.0
        val playerStart = board.playerStarts.minBy { abs(it.col - centerColumn) }
        val door = board.doors.minBy { abs(it.col - centerColumn) }
        val houseExit = TilePos(door.col, door.row - 1)
        require(board.tiles.at(houseExit, columns) != Tile.WALL) {
            "Maze $name has no open tile above the ghost house door"
        }
        val houseSlots = houseSlots(name, board.house)
        val fruitAnchor = fruitAnchor(name, board.tiles, columns, rows.size, door, board.house)
        val tunnelRow = rows.indexOfFirst { row ->
            row.firstOrNull() == 'T' && row.lastOrNull() == 'T'
        }
        require(tunnelRow >= 0) { "Maze $name has no wrapping tunnel row" }

        return Maze(
            name = name,
            columns = columns,
            rows = rows.size,
            tiles = board.tiles,
            playerStart = playerStart,
            door = door,
            houseExit = houseExit,
            fruitAnchor = fruitAnchor,
            houseSlots = houseSlots,
            scatterTargets = listOf(
                TilePos(columns - 2, 1),
                TilePos(1, 1),
                TilePos(columns - 2, rows.size - 2),
                TilePos(1, rows.size - 2)
            ),
            tunnelRow = tunnelRow
        )
    }

    private fun parseBoard(name: String, rows: List<String>): ParsedBoard {
        val tiles = ArrayList<Tile>()
        val playerStarts = mutableListOf<TilePos>()
        val doors = mutableListOf<TilePos>()
        val house = mutableListOf<TilePos>()

        rows.forEachIndexed { row, line ->
            line.forEachIndexed { col, char ->
                val pos = TilePos(col, row)
                when (char) {
                    '#' -> tiles += Tile.WALL
                    '.' -> tiles += Tile.DOT
                    'o' -> tiles += Tile.PELLET
                    ' ' -> tiles += Tile.EMPTY
                    '-' -> {
                        tiles += Tile.DOOR
                        doors += pos
                    }
                    'H' -> {
                        tiles += Tile.HOUSE
                        house += pos
                    }
                    'T' -> tiles += Tile.TUNNEL
                    'P' -> {
                        tiles += Tile.EMPTY
                        playerStarts += pos
                    }
                    else -> error("Maze $name has unknown tile '$char' at $pos")
                }
            }
        }

        require(playerStarts.isNotEmpty()) { "Maze $name has no player start" }
        require(doors.isNotEmpty()) { "Maze $name has no ghost house door" }
        require(house.isNotEmpty()) { "Maze $name has no ghost house interior" }
        return ParsedBoard(tiles, playerStarts, doors, house)
    }

    private fun houseSlots(name: String, house: List<TilePos>): List<TilePos> {
        require(house.isNotEmpty()) { "Maze $name has no ghost house interior" }
        val left = house.minOf { it.col }
        val right = house.maxOf { it.col }
        val top = house.minOf { it.row }
        val bottom = house.maxOf { it.row }
        val slots = if (right - left >= 3 && bottom > top) {
            listOf(
                TilePos(left + 1, top),
                TilePos(right - 1, top),
                TilePos(left + 1, bottom),
                TilePos(right - 1, bottom)
            )
        } else {
            house.sortedWith(compareBy({ it.row }, { it.col })).take(4)
        }
        require(slots.size >= 4) { "Maze $name needs four ghost house slots" }
        return slots
    }

    private fun fruitAnchor(
        name: String,
        tiles: List<Tile>,
        columns: Int,
        rows: Int,
        door: TilePos,
        house: List<TilePos>
    ): TilePos {
        val bottom = house.maxOf { it.row }
        val fruitRow = (bottom + 1 until rows).firstOrNull { row ->
            tiles.at(TilePos(door.col, row), columns) != Tile.WALL
        }
        require(fruitRow != null) { "Maze $name has no open tile below the ghost house" }
        return TilePos(door.col, fruitRow)
    }

    private fun List<Tile>.at(pos: TilePos, columns: Int): Tile = this[pos.row * columns + pos.col]

    private data class ParsedBoard(
        val tiles: List<Tile>,
        val playerStarts: List<TilePos>,
        val doors: List<TilePos>,
        val house: List<TilePos>
    )
}

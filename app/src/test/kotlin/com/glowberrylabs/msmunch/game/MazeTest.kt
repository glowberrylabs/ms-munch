package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeTest {

    private val mazes = MazeLayouts.all.mapIndexed { index, layout ->
        MazeParser.parse("Maze ${index + 1}", layout)
    }

    @Test
    fun `all mazes parse to 28 by 31 boards`() {
        mazes.forEach { maze ->
            assertEquals(28, maze.columns)
            assertEquals(31, maze.rows)
            assertEquals(maze.columns * maze.rows, maze.tiles.size)
        }
    }

    @Test
    fun `all mazes pass validation`() {
        mazes.forEach { maze ->
            val errors = MazeValidator.validate(maze)
            assertTrue("${maze.name}: $errors", errors.isEmpty())
        }
    }

    @Test
    fun `all mazes match the arcade dot and pellet counts`() {
        val expectedDots = listOf(220, 240, 238, 234)
        mazes.forEachIndexed { index, maze ->
            assertEquals(expectedDots[index], maze.dotCount())
            assertEquals(4, maze.pelletCount())
        }
    }

    @Test
    fun `player start and house exits are open`() {
        mazes.forEach { maze ->
            assertTrue(maze.isPlayerWalkable(maze.playerStart))
            assertTrue(maze.isPlayerWalkable(maze.houseExit))
            assertTrue(maze.isPlayerWalkable(maze.fruitAnchor))
            assertEquals(4, maze.houseSlots.size)
        }
    }

    @Test
    fun `ghost house is closed to the player but open to ghosts`() {
        mazes.forEach { maze ->
            assertEquals(Tile.DOOR, maze.tileAt(maze.door))
            assertTrue(maze.houseSlots.all { maze.tileAt(it) == Tile.HOUSE })
            assertTrue(!maze.isPlayerWalkable(maze.door))
            assertTrue(maze.isGhostWalkable(maze.door))
        }
    }

    @Test
    fun `tunnels match the arcade rows and reach both edges`() {
        val expectedTunnels = listOf(listOf(8, 17), listOf(1, 23), listOf(9), listOf(13, 16))
        mazes.forEachIndexed { index, maze ->
            assertEquals(expectedTunnels[index], maze.tunnelRows)
            maze.tunnelRows.forEach { row ->
                assertEquals(Tile.TUNNEL, maze.tileAt(0, row))
                assertEquals(Tile.TUNNEL, maze.tileAt(maze.columns - 1, row))
            }
        }
    }

    @Test
    fun `no maze has a dead end`() {
        mazes.forEach { maze ->
            assertTrue("${maze.name} has dead ends: ${deadEnds(maze)}", deadEnds(maze).isEmpty())
        }
    }

    @Test
    fun `the four mazes are different layouts`() {
        assertEquals(4, MazeLayouts.all.toSet().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parser rejects ragged layouts`() {
        MazeParser.parse(
            "broken",
            """
            #####
            #P.#
            ####
            """.trimIndent()
        )
    }

    private fun deadEnds(maze: Maze): List<TilePos> {
        val neighbors = listOf(
            TilePos(0, -1),
            TilePos(-1, 0),
            TilePos(0, 1),
            TilePos(1, 0)
        )
        return buildList {
            for (row in 0 until maze.rows) {
                for (col in 0 until maze.columns) {
                    val pos = TilePos(col, row)
                    if (!maze.isPlayerWalkable(pos)) continue
                    val exits = neighbors.count { offset ->
                        maze.isPlayerWalkable(TilePos(pos.col + offset.col, pos.row + offset.row))
                    }
                    if (exits <= 1) add(pos)
                }
            }
        }
    }
}

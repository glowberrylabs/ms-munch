package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeTest {

    private val mazes = listOf(
        MazeParser.parse("A", MazeLayouts.mazeA),
        MazeParser.parse("B", MazeLayouts.mazeB)
    )

    @Test
    fun `both mazes parse to 28 by 31 boards`() {
        mazes.forEach { maze ->
            assertEquals(28, maze.columns)
            assertEquals(31, maze.rows)
            assertEquals(maze.columns * maze.rows, maze.tiles.size)
        }
    }

    @Test
    fun `both mazes pass validation`() {
        mazes.forEach { maze ->
            val errors = MazeValidator.validate(maze)
            assertTrue("${maze.name}: $errors", errors.isEmpty())
        }
    }

    @Test
    fun `both mazes have about 240 dots and four pellets`() {
        assertEquals(242, mazes[0].dotCount())
        assertEquals(236, mazes[1].dotCount())
        mazes.forEach { maze -> assertEquals(4, maze.pelletCount()) }
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
    fun `tunnels reach both edges on the tunnel row`() {
        mazes.forEach { maze ->
            assertEquals(Tile.TUNNEL, maze.tileAt(0, maze.tunnelRow))
            assertEquals(Tile.TUNNEL, maze.tileAt(maze.columns - 1, maze.tunnelRow))
        }
    }

    @Test
    fun `the two mazes are different layouts`() {
        assertNotEquals(MazeLayouts.mazeA, MazeLayouts.mazeB)
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
}

package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementTest {

    private val engine = GameEngine()
    private val maze = Mazes.A

    private fun state(
        x: Double,
        y: Double,
        direction: Direction,
        desired: Direction? = null,
        phase: GamePhase = GamePhase.RUNNING
    ): GameState = GameState(
        phase = phase,
        maze = maze,
        board = maze.tiles,
        player = MsMunch(Position(x, y), direction, desired)
    )

    @Test
    fun `moving right advances at level one speed`() {
        val (result, _) = engine.advance(state(9.0, 22.0, Direction.RIGHT), 0.5)
        assertEquals(12.25, result.player.pos.x, 0.001)
        assertEquals(22.0, result.player.pos.y, 0.001)
    }

    @Test
    fun `walls stop the player at the last corridor center`() {
        val (result, _) = engine.advance(state(18.0, 22.0, Direction.RIGHT), 1.0)
        assertEquals(18.0, result.player.pos.x, 0.001)
        assertEquals(Direction.RIGHT, result.player.direction)
    }

    @Test
    fun `perpendicular turn snaps to the tile center`() {
        val (result, _) = engine.advance(state(11.9, 22.0, Direction.RIGHT, Direction.UP), 1.0 / 120.0)
        assertEquals(Direction.UP, result.player.direction)
        assertEquals(12.0, result.player.pos.x, 0.001)
        assertTrue(result.player.pos.y < 22.0)
    }

    @Test
    fun `buffered turn applies at the next legal center`() {
        val (first, _) = engine.advance(state(10.0, 22.0, Direction.RIGHT, Direction.UP), 0.1)
        assertEquals(Direction.RIGHT, first.player.direction)
        assertEquals(Direction.UP, first.player.desiredDirection)

        val (second, _) = engine.advance(first, 0.25)
        assertEquals(Direction.UP, second.player.direction)
        assertEquals(12.0, second.player.pos.x, 0.001)
    }

    @Test
    fun `a wall blocks a perpendicular turn`() {
        val (result, _) = engine.advance(state(10.0, 22.0, Direction.RIGHT, Direction.UP), 0.05)
        assertEquals(Direction.RIGHT, result.player.direction)
        assertEquals(Direction.UP, result.player.desiredDirection)
    }

    @Test
    fun `reversal is instant anywhere in a corridor`() {
        val turned = engine.turn(state(12.0, 22.0, Direction.RIGHT), Direction.LEFT)
        val (result, _) = engine.advance(turned, 1.0 / 120.0)
        assertEquals(Direction.LEFT, result.player.direction)
        assertTrue(result.player.pos.x < 12.0)
    }

    @Test
    fun `tunnel wraps horizontally`() {
        val (result, _) = engine.advance(state(1.0, 15.0, Direction.LEFT), 0.4)
        assertEquals(Direction.LEFT, result.player.direction)
        assertTrue(result.player.pos.x > 25.0)
        assertTrue(result.player.pos.x < 28.0)
    }

    @Test
    fun `a paused game does not move`() {
        val paused = state(9.0, 22.0, Direction.RIGHT, phase = GamePhase.PAUSED)
        val (result, _) = engine.advance(paused, 1.0)
        assertEquals(paused, result)
    }

    @Test
    fun `starting a new game enters the ready phase at the player start`() {
        val newGame = engine.newGame()
        assertEquals(GamePhase.START, newGame.phase)
        val ready = engine.start(newGame)
        assertEquals(GamePhase.READY, ready.phase)
        assertEquals(maze.playerStart.toPosition(), ready.player.pos)
    }

    @Test
    fun `player speed rises with level and caps`() {
        assertEquals(6.5, LevelTable.playerSpeed(1), 0.0001)
        assertEquals(8.0, LevelTable.playerSpeed(20), 0.0001)
    }
}

package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhostTest {

    private val maze = Mazes.forLevel(1)

    private fun running(engine: GameEngine, level: Int = 1): GameState =
        engine.newGame(maze).copy(phase = GamePhase.RUNNING, level = level)

    private fun GameState.grump(kind: GhostKind): Grump = grumps.first { it.kind == kind }

    @Test
    fun `level one alternates scatter and chase windows`() {
        val schedule = LevelTable.schedule(1)
        assertEquals(
            listOf(
                GhostMode.SCATTER,
                GhostMode.CHASE,
                GhostMode.SCATTER,
                GhostMode.CHASE,
                GhostMode.SCATTER,
                GhostMode.CHASE
            ),
            schedule.map { it.mode }
        )
        assertEquals(6.0, schedule[0].seconds, 0.0001)
        assertEquals(5.0, schedule[2].seconds, 0.0001)
        assertEquals(4.0, schedule[4].seconds, 0.0001)
        assertTrue(schedule.last().seconds.isInfinite())
    }

    @Test
    fun `later level bands shorten scatter windows`() {
        assertEquals(3.0, LevelTable.schedule(8)[0].seconds, 0.0001)
        assertEquals(2.0, LevelTable.schedule(8)[2].seconds, 0.0001)
        assertEquals(1.0, LevelTable.schedule(8)[4].seconds, 0.0001)
    }

    @Test
    fun `mode timer swaps scatter to chase`() {
        val engine = GameEngine()
        val (before, _) = engine.advance(running(engine), 5.9)
        assertEquals(GhostMode.SCATTER, before.globalMode)
        assertEquals(0, before.modeIndex)

        val (after, _) = engine.advance(before, 0.2)
        assertEquals(GhostMode.CHASE, after.globalMode)
        assertEquals(1, after.modeIndex)
        assertTrue(after.modeRemaining > 19.5)
    }

    @Test
    fun `house release waits for the dot threshold`() {
        val engine = GameEngine()
        val (before, _) = engine.advance(running(engine).copy(dotsEaten = 19), 2.0)
        assertEquals(GhostMode.IN_HOUSE, before.grump(GhostKind.BUBBLE).mode)

        val (after, _) = engine.advance(before.copy(dotsEaten = 20), 2.0)
        assertNotEquals(GhostMode.IN_HOUSE, after.grump(GhostKind.BUBBLE).mode)
        assertFalse(maze.isHouse(after.grump(GhostKind.BUBBLE).pos.tile(maze)))
        assertEquals(GhostMode.IN_HOUSE, after.grump(GhostKind.TAFFY).mode)
    }

    @Test
    fun `power pellets frighten active ghosts and expire`() {
        val engine = GameEngine()
        val frightened = engine.frighten(running(engine))
        assertEquals(7.0, frightened.frightenedRemaining, 0.0001)
        assertEquals(GhostMode.FRIGHTENED, frightened.grump(GhostKind.COCOA).mode)
        assertEquals(GhostMode.IN_HOUSE, frightened.grump(GhostKind.BUBBLE).mode)

        val (stillScared, _) = engine.advance(frightened, 6.9)
        assertEquals(GhostMode.FRIGHTENED, stillScared.grump(GhostKind.COCOA).mode)

        val (calmed, _) = engine.advance(stillScared, 0.2)
        assertEquals(0.0, calmed.frightenedRemaining, 0.0001)
        assertEquals(GhostMode.SCATTER, calmed.grump(GhostKind.COCOA).mode)
    }

    @Test
    fun `frightened movement is deterministic with a seeded random`() {
        val first = frightenedRun(42L)
        val second = frightenedRun(42L)
        assertEquals(first, second)
    }

    private fun frightenedRun(seed: Long): List<Grump> {
        val engine = GameEngine(kotlin.random.Random(seed))
        val state = engine.frighten(running(engine).copy(dotsEaten = 100))
        return engine.advance(state, 3.0).first.grumps
    }

    @Test
    fun `eating a frightened ghost sends it home and it revives`() {
        val engine = GameEngine(kotlin.random.Random(7))
        var state = running(engine).copy(
            player = MsMunch(Position(9.0, 17.0), Direction.NONE)
        )
        state = engine.frighten(state)
        state = state.copy(
            grumps = state.grumps.map { grump ->
                if (grump.kind == GhostKind.COCOA) grump.copy(pos = Position(9.0, 17.0)) else grump
            }
        )

        val (eaten, events) = engine.advance(state, 1.0 / 120.0)
        assertTrue(events.contains(GameEvent.GhostEaten))
        assertEquals(GhostMode.EYES, eaten.grump(GhostKind.COCOA).mode)
        assertEquals(1, eaten.ghostChain)

        var current = eaten
        var sawInHouse = false
        repeat(720) {
            val (next, _) = engine.advance(current, 1.0 / 120.0)
            current = next
            if (current.grump(GhostKind.COCOA).mode == GhostMode.IN_HOUSE) {
                sawInHouse = true
            }
        }
        assertTrue(sawInHouse)
        val cocoa = current.grump(GhostKind.COCOA)
        assertTrue(cocoa.mode == GhostMode.SCATTER || cocoa.mode == GhostMode.CHASE)
        assertFalse(maze.isHouse(cocoa.pos.tile(maze)))
    }

    @Test
    fun `personality targets follow the player`() {
        val player = MsMunch(Position(13.0, 20.0), Direction.RIGHT)
        val cocoa = Grump(GhostKind.COCOA, Position(10.0, 20.0))
        val bubble = Grump(GhostKind.BUBBLE, Position(1.0, 1.0))
        val taffy = Grump(GhostKind.TAFFY, Position(5.0, 20.0))
        val mint = Grump(GhostKind.MINT, Position(1.0, 20.0))
        val state = GameState(
            phase = GamePhase.RUNNING,
            maze = maze,
            board = maze.tiles,
            player = player,
            grumps = listOf(cocoa, bubble, taffy, mint),
            globalMode = GhostMode.CHASE
        )

        assertEquals(TilePos(13, 20), GhostTargets.chaseTarget(GhostKind.COCOA, state))
        assertEquals(TilePos(17, 20), GhostTargets.chaseTarget(GhostKind.BUBBLE, state))
        assertEquals(TilePos(20, 20), GhostTargets.chaseTarget(GhostKind.TAFFY, state))
        assertEquals(TilePos(13, 20), GhostTargets.chaseTarget(GhostKind.MINT, state))
    }

    @Test
    fun `shy ghost retreats to its corner when close`() {
        val player = MsMunch(Position(13.0, 20.0), Direction.RIGHT)
        val nearMint = Grump(GhostKind.MINT, Position(13.0, 17.0))
        val nearState = GameState(
            phase = GamePhase.RUNNING,
            maze = maze,
            board = maze.tiles,
            player = player,
            grumps = listOf(nearMint),
            globalMode = GhostMode.CHASE
        )
        assertEquals(maze.scatterTargets[GhostKind.MINT.ordinal], GhostTargets.chaseTarget(GhostKind.MINT, nearState))
    }

    @Test
    fun `scatter mode sends ghosts to their corners`() {
        val state = running(GameEngine()).copy(globalMode = GhostMode.SCATTER)
        assertEquals(
            maze.scatterTargets[GhostKind.COCOA.ordinal],
            GhostTargets.targetFor(GhostKind.COCOA, state)
        )
    }
}

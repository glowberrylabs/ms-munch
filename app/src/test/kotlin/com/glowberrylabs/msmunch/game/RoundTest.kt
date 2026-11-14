package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundTest {

    private val engine = GameEngine(kotlin.random.Random(1))
    private val maze = Mazes.forLevel(1)

    private fun running(): GameState = engine.newGame(maze).copy(phase = GamePhase.RUNNING)

    private fun at(tile: TilePos, state: GameState = running()): GameState =
        state.copy(player = MsMunch(tile.toPosition(), Direction.NONE))

    private fun GameState.grump(kind: GhostKind): Grump = grumps.first { it.kind == kind }

    @Test
    fun `eating a dot scores ten and clears the tile`() {
        val start = at(TilePos(9, 23))
        val index = maze.indexOf(TilePos(9, 23))
        val (result, events) = engine.advance(start, 1.0 / 120.0)
        assertEquals(10, result.score)
        assertEquals(Tile.EMPTY, result.board[index])
        assertEquals(start.dotsRemaining - 1, result.dotsRemaining)
        assertTrue(events.contains(GameEvent.Chomp))
    }

    @Test
    fun `eating a pellet scores fifty and frightens ghosts`() {
        val pellet = TilePos(1, 2)
        val (result, events) = engine.advance(at(pellet), 1.0 / 120.0)
        assertEquals(50, result.score)
        assertEquals(3, result.pelletsRemaining)
        assertEquals(7.0, result.frightenedRemaining, 0.0001)
        assertEquals(GhostMode.FRIGHTENED, result.grump(GhostKind.COCOA).mode)
        assertTrue(events.contains(GameEvent.Pellet))
    }

    @Test
    fun `fruit appears at the first threshold and later expires`() {
        val start = running().copy(
            dotsEaten = 69,
            dotsRemaining = maze.dotCount() - 69,
            grumps = emptyList()
        )
        val (withFruit, _) = engine.advance(at(TilePos(9, 23), start), 1.0 / 120.0)
        assertEquals(70, withFruit.dotsEaten)
        assertEquals(1, withFruit.fruitSpawned)
        assertNotNull(withFruit.fruit)
        assertEquals(maze.fruitAnchor, withFruit.fruit?.pos)

        val (expired, _) = engine.advance(withFruit, 9.1)
        assertNull(expired.fruit)
    }

    @Test
    fun `fruit appears again at the second threshold`() {
        val start = running().copy(
            dotsEaten = 169,
            dotsRemaining = maze.dotCount() - 169,
            fruitSpawned = 1,
            grumps = emptyList()
        )
        val (result, _) = engine.advance(at(TilePos(9, 23), start), 1.0 / 120.0)
        assertEquals(170, result.dotsEaten)
        assertEquals(2, result.fruitSpawned)
        assertNotNull(result.fruit)
    }

    @Test
    fun `eating fruit awards its level value`() {
        val anchor = maze.fruitAnchor
        val board = maze.tiles.toMutableList()
        board[maze.indexOf(anchor)] = Tile.EMPTY
        val start = running().copy(
            board = board,
            fruit = Fruit(anchor, 5.0),
            fruitSpawned = 1,
            grumps = emptyList()
        )
        val (result, events) = engine.advance(at(anchor, start), 1.0 / 120.0)
        assertEquals(LevelTable.fruitValue(1), result.score)
        assertNull(result.fruit)
        assertTrue(events.contains(GameEvent.Fruit))
    }

    @Test
    fun `ghost chain scores double each pickup and then stops`() {
        val start = running().copy(
            player = MsMunch(maze.playerStart.toPosition(), Direction.NONE),
            frightenedRemaining = 7.0,
            grumps = running().grumps.map { grump ->
                grump.copy(mode = GhostMode.FRIGHTENED, pos = maze.playerStart.toPosition())
            }
        )
        val (eaten, events) = engine.advance(start, 1.0 / 120.0)
        assertEquals(3000, eaten.score)
        assertEquals(4, eaten.ghostChain)
        assertEquals(4, events.count { it == GameEvent.GhostEaten })

        val extra = running().copy(
            player = MsMunch(maze.playerStart.toPosition(), Direction.NONE),
            frightenedRemaining = 7.0,
            ghostChain = 4,
            grumps = running().grumps.take(1).map { grump ->
                grump.copy(mode = GhostMode.FRIGHTENED, pos = maze.playerStart.toPosition())
            }
        )
        val (noPoints, _) = engine.advance(extra, 1.0 / 120.0)
        assertEquals(0, noPoints.score)
        assertEquals(4, noPoints.ghostChain)
    }

    @Test
    fun `extra life is awarded once at ten thousand points`() {
        val pellet = TilePos(1, 2)
        val start = at(pellet, running()).copy(score = 9950, lives = 3)
        val (boosted, events) = engine.advance(start, 1.0 / 120.0)
        assertEquals(10_000, boosted.score)
        assertEquals(4, boosted.lives)
        assertTrue(boosted.extraLifeAwarded)
        assertTrue(events.contains(GameEvent.ExtraLife))

        val (again, moreEvents) = engine.advance(at(TilePos(9, 23), boosted), 1.0 / 120.0)
        assertEquals(4, again.lives)
        assertFalse(moreEvents.contains(GameEvent.ExtraLife))
    }

    @Test
    fun `death keeps dots and score then respawns`() {
        val start = running().copy(lives = 3, score = 123, dotsEaten = 5)
        val doomed = start.copy(
            grumps = start.grumps.map { grump ->
                if (grump.kind == GhostKind.COCOA) {
                    grump.copy(mode = GhostMode.CHASE, pos = start.player.pos)
                } else {
                    grump
                }
            }
        )
        val (dying, events) = engine.advance(doomed, 1.0 / 120.0)
        assertEquals(GamePhase.DYING, dying.phase)
        assertEquals(2, dying.lives)
        assertEquals(123, dying.score)
        assertEquals(doomed.board, dying.board)
        assertTrue(events.contains(GameEvent.Death))

        val (ready, _) = engine.advance(dying, 1.6)
        assertEquals(GamePhase.READY, ready.phase)
        assertEquals(2, ready.lives)
        assertEquals(123, ready.score)
        assertEquals(doomed.board, ready.board)
        assertEquals(maze.playerStart, ready.player.pos.tile(maze))

        val (resumed, _) = engine.advance(ready, 2.1)
        assertEquals(GamePhase.RUNNING, resumed.phase)
    }

    @Test
    fun `zero lives ends the game and records the best score`() {
        val start = running().copy(lives = 1, score = 555)
        val doomed = start.copy(
            grumps = start.grumps.map { grump ->
                if (grump.kind == GhostKind.COCOA) {
                    grump.copy(mode = GhostMode.CHASE, pos = start.player.pos)
                } else {
                    grump
                }
            }
        )
        val (dying, _) = engine.advance(doomed, 1.0 / 120.0)
        assertEquals(0, dying.lives)

        val (over, events) = engine.advance(dying, 1.6)
        assertEquals(GamePhase.GAME_OVER, over.phase)
        assertEquals(555, over.highScore)
        assertTrue(events.contains(GameEvent.GameOver))
    }

    @Test
    fun `clearing a level advances to the next level and resets rounds`() {
        val dot = TilePos(9, 23)
        val board = maze.tiles.map { if (it.isEdible) Tile.EMPTY else it }.toMutableList()
        board[maze.indexOf(dot)] = Tile.DOT
        val start = running().copy(
            board = board,
            player = MsMunch(dot.toPosition(), Direction.NONE),
            dotsRemaining = 1,
            pelletsRemaining = 0,
            score = 700,
            lives = 2
        )
        val (cleared, events) = engine.advance(start, 1.0 / 120.0)
        assertEquals(GamePhase.LEVEL_CLEAR, cleared.phase)
        assertTrue(events.contains(GameEvent.LevelClear))

        val (next, _) = engine.advance(cleared, 2.1)
        assertEquals(2, next.level)
        assertEquals(Mazes.forLevel(2).name, next.maze.name)
        assertEquals(Mazes.forLevel(2).dotCount(), next.dotsRemaining)
        assertEquals(4, next.pelletsRemaining)
        assertEquals(710, next.score)
        assertEquals(2, next.lives)
        assertEquals(GamePhase.READY, next.phase)
    }

    @Test
    fun `ready phase starts the round after its countdown`() {
        val ready = engine.start(engine.newGame(maze))
        assertEquals(GamePhase.READY, ready.phase)
        val (running, events) = engine.advance(ready, 2.1)
        assertEquals(GamePhase.RUNNING, running.phase)
        assertTrue(events.contains(GameEvent.LevelStart))
    }

    @Test
    fun `levels follow the arcade maze order`() {
        assertEquals("Maze 1", Mazes.forLevel(1).name)
        assertEquals("Maze 1", Mazes.forLevel(2).name)
        assertEquals("Maze 2", Mazes.forLevel(3).name)
        assertEquals("Maze 2", Mazes.forLevel(5).name)
        assertEquals("Maze 3", Mazes.forLevel(6).name)
        assertEquals("Maze 3", Mazes.forLevel(9).name)
        assertEquals("Maze 4", Mazes.forLevel(10).name)
        assertEquals("Maze 4", Mazes.forLevel(13).name)
        assertEquals("Maze 3", Mazes.forLevel(14).name)
        assertEquals("Maze 3", Mazes.forLevel(17).name)
        assertEquals("Maze 4", Mazes.forLevel(18).name)
        assertEquals("Maze 4", Mazes.forLevel(21).name)
        assertEquals("Maze 3", Mazes.forLevel(22).name)
    }

    @Test
    fun `level table values cap and cycle`() {
        assertEquals(8.0, LevelTable.playerSpeed(20), 0.0001)
        assertEquals(7.8, LevelTable.ghostSpeed(20), 0.0001)
        assertEquals(2.0, LevelTable.frightenedSeconds(20), 0.0001)
        assertEquals(100, LevelTable.fruitValue(1))
        assertEquals(5000, LevelTable.fruitValue(8))
        assertEquals(100, LevelTable.fruitValue(9))
        assertNotEquals(LevelTable.playerSpeed(1), LevelTable.playerSpeed(2))
    }
}

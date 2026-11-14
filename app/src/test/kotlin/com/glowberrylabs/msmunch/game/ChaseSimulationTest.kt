package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertTrue
import org.junit.Test

class ChaseSimulationTest {

    @Test
    fun `a stationary player is caught on both mazes`() {
        listOf(Mazes.A, Mazes.B).forEach { maze ->
            val engine = GameEngine(kotlin.random.Random(1))
            var state = engine.newGame(maze).copy(phase = GamePhase.RUNNING)
            var time = 0.0
            var caught = false
            while (time < CATCH_LIMIT_SECONDS) {
                val (next, events) = engine.advance(state, STEP_SECONDS)
                state = next
                if (events.contains(GameEvent.Death)) {
                    caught = true
                    break
                }
                time += STEP_SECONDS
            }
            assertTrue(
                "${maze.name} did not catch a stationary player within ${CATCH_LIMIT_SECONDS}s",
                caught
            )
        }
    }

    private companion object {
        const val STEP_SECONDS = 1.0 / 120.0
        const val CATCH_LIMIT_SECONDS = 20.0
    }
}

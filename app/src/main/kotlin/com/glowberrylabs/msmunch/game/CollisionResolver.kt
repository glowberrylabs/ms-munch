package com.glowberrylabs.msmunch.game

import kotlin.math.min

object CollisionResolver {

    fun resolve(state: GameState): Pair<GameState, List<GameEvent>> {
        var chain = state.ghostChain
        var score = state.score
        val events = mutableListOf<GameEvent>()
        val grumps = state.grumps.map { grump ->
            val caught = grump.mode == GhostMode.FRIGHTENED &&
                state.player.pos.distanceSquaredTo(grump.pos) <= COLLISION_DISTANCE_SQUARED
            if (caught) {
                score += Scoring.ghostChainScore(chain)
                chain = min(chain + 1, Scoring.MAX_GHOST_CHAIN)
                events += GameEvent.GhostEaten
                grump.copy(
                    mode = GhostMode.EYES,
                    direction = grump.direction.opposite,
                    decisionTile = -1
                )
            } else {
                grump
            }
        }

        var current = state.copy(grumps = grumps, ghostChain = chain, score = score)
        current = Scoring.awardExtraLife(current, events)

        val playerHit = current.grumps.any { grump ->
            grump.mode.isChasing() &&
                current.player.pos.distanceSquaredTo(grump.pos) <= COLLISION_DISTANCE_SQUARED
        }
        if (playerHit) {
            events += GameEvent.Death
            current = current.copy(
                phase = GamePhase.DYING,
                phaseRemaining = RoundFlow.DEATH_PAUSE_SECONDS,
                lives = (current.lives - 1).coerceAtLeast(0),
                fruit = null,
                frightenedRemaining = 0.0,
                ghostChain = 0
            )
        }
        return current to events
    }

    private fun GhostMode.isChasing(): Boolean = this == GhostMode.SCATTER || this == GhostMode.CHASE

    private const val COLLISION_DISTANCE_SQUARED = 0.7 * 0.7
}

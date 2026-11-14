package com.glowberrylabs.msmunch.game

object Scoring {
    const val DOT_SCORE = 10
    const val PELLET_SCORE = 50
    const val MAX_GHOST_CHAIN = 4
    const val EXTRA_LIFE_SCORE = 10_000

    fun awardExtraLife(state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.extraLifeAwarded || state.score < EXTRA_LIFE_SCORE) return state
        events += GameEvent.ExtraLife
        return state.copy(lives = state.lives + 1, extraLifeAwarded = true)
    }

    fun ghostChainScore(chain: Int): Int = if (chain < MAX_GHOST_CHAIN) 200 shl chain else 0
}

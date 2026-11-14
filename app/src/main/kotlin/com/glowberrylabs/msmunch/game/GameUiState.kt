package com.glowberrylabs.msmunch.game

data class GameUiState(val game: GameState, val muted: Boolean = false, val handedness: Handedness = Handedness.RIGHT)

package com.glowberrylabs.msmunch.ui

import com.glowberrylabs.msmunch.game.Direction
import com.glowberrylabs.msmunch.game.Handedness

data class GameActions(
    val onStart: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onRestart: () -> Unit,
    val onDirection: (Direction) -> Unit,
    val onToggleMute: () -> Unit,
    val onHandedness: (Handedness) -> Unit,
    val onFrame: (Long) -> Unit
)

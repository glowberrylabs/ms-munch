package com.glowberrylabs.msmunch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.glowberrylabs.msmunch.game.Direction
import com.glowberrylabs.msmunch.game.GamePhase
import com.glowberrylabs.msmunch.game.GameState
import com.glowberrylabs.msmunch.game.GameUiState
import com.glowberrylabs.msmunch.game.Handedness

@Composable
fun GameScreen(uiState: GameUiState, actions: GameActions, modifier: Modifier = Modifier) {
    val game = uiState.game
    val currentOnFrame by rememberUpdatedState(actions.onFrame)

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { actions.onPause() }
    KeepScreenOn(enabled = game.phase == GamePhase.RUNNING)
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { currentOnFrame(it) }
        }
    }

    GameScene(uiState = uiState, actions = actions, modifier = modifier)
}

@Composable
private fun GameScene(uiState: GameUiState, actions: GameActions, modifier: Modifier = Modifier) {
    val game = uiState.game
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GamePalette.Background,
                        Color(0xFF17113D),
                        GamePalette.Background
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val landscape = maxWidth > maxHeight
                if (landscape) {
                    LandscapeLayout(
                        game = game,
                        uiState = uiState,
                        onPause = actions.onPause,
                        onToggleMute = actions.onToggleMute,
                        onDirection = actions.onDirection
                    )
                } else {
                    PortraitLayout(
                        game = game,
                        uiState = uiState,
                        onPause = actions.onPause,
                        onToggleMute = actions.onToggleMute,
                        onDirection = actions.onDirection
                    )
                }
            }
            GameOverlays(
                uiState = uiState,
                onStart = actions.onStart,
                onResume = actions.onResume,
                onRestart = actions.onRestart,
                onToggleMute = actions.onToggleMute,
                onHandedness = actions.onHandedness,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    game: GameState,
    uiState: GameUiState,
    onPause: () -> Unit,
    onToggleMute: () -> Unit,
    onDirection: (Direction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameHud(
            game = game,
            muted = uiState.muted,
            onPause = onPause,
            onToggleMute = onToggleMute
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            MazeBoard(game = game, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
        ) {
            Joystick(
                enabled = game.phase == GamePhase.RUNNING,
                onDirection = onDirection,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    game: GameState,
    uiState: GameUiState,
    onPause: () -> Unit,
    onToggleMute: () -> Unit,
    onDirection: (Direction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GameHud(
            game = game,
            muted = uiState.muted,
            onPause = onPause,
            onToggleMute = onToggleMute
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SideGutter(
                side = Handedness.LEFT,
                uiState = uiState,
                game = game,
                onDirection = onDirection
            )
            MazeBoard(
                game = game,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            )
            SideGutter(
                side = Handedness.RIGHT,
                uiState = uiState,
                game = game,
                onDirection = onDirection
            )
        }
    }
}

@Composable
private fun SideGutter(side: Handedness, uiState: GameUiState, game: GameState, onDirection: (Direction) -> Unit) {
    Box(modifier = Modifier.width(172.dp).fillMaxHeight()) {
        if (uiState.handedness == side) {
            Joystick(
                enabled = game.phase == GamePhase.RUNNING,
                onDirection = onDirection,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

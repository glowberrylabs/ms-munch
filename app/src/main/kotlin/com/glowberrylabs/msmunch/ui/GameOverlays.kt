package com.glowberrylabs.msmunch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowberrylabs.msmunch.R
import com.glowberrylabs.msmunch.game.GamePhase
import com.glowberrylabs.msmunch.game.GameUiState
import com.glowberrylabs.msmunch.game.Handedness
import kotlin.math.max

@Composable
fun GameOverlays(
    uiState: GameUiState,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleMute: () -> Unit,
    onHandedness: (Handedness) -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = uiState.game.phase
    AnimatedVisibility(
        visible = phase in OVERLAY_PHASES,
        modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180)),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f, animationSpec = tween(120))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB30B0A1F)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(28.dp),
                color = GamePalette.Surface.copy(alpha = 0.97f),
                border = BorderStroke(1.dp, GamePalette.Wall.copy(alpha = 0.35f)),
                tonalElevation = 8.dp
            ) {
                OverlayContent(
                    uiState = uiState,
                    onStart = onStart,
                    onResume = onResume,
                    onRestart = onRestart,
                    onToggleMute = onToggleMute,
                    onHandedness = onHandedness
                )
            }
        }
    }
}

@Composable
private fun OverlayContent(
    uiState: GameUiState,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleMute: () -> Unit,
    onHandedness: (Handedness) -> Unit
) {
    val game = uiState.game
    Column(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (game.phase) {
            GamePhase.START -> StartContent(onStart)
            GamePhase.READY -> MessageContent(stringResource(R.string.ready))
            GamePhase.PAUSED -> PauseContent(
                uiState = uiState,
                onResume = onResume,
                onRestart = onRestart,
                onToggleMute = onToggleMute,
                onHandedness = onHandedness
            )
            GamePhase.LEVEL_CLEAR -> MessageContent(stringResource(R.string.level_cleared))
            GamePhase.GAME_OVER -> GameOverContent(game.score, game.highScore, onRestart)
            else -> Unit
        }
    }
}

@Composable
private fun StartContent(onStart: () -> Unit) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displaySmall,
        color = GamePalette.Muncher,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.start_subtitle),
        style = MaterialTheme.typography.titleMedium,
        color = GamePalette.Muted,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    BigIconButton(
        icon = Icons.Rounded.PlayArrow,
        description = stringResource(R.string.play),
        onClick = onStart
    )
}

@Composable
private fun MessageContent(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = GamePalette.Wall,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PauseContent(
    uiState: GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleMute: () -> Unit,
    onHandedness: (Handedness) -> Unit
) {
    Text(
        text = stringResource(R.string.paused),
        style = MaterialTheme.typography.headlineMedium,
        color = GamePalette.Wall,
        fontWeight = FontWeight.Black
    )
    Spacer(modifier = Modifier.height(20.dp))
    PauseButtons(
        muted = uiState.muted,
        onResume = onResume,
        onRestart = onRestart,
        onToggleMute = onToggleMute
    )
    Spacer(modifier = Modifier.height(24.dp))
    HandednessPicker(
        handedness = uiState.handedness,
        onHandedness = onHandedness
    )
}

@Composable
private fun PauseButtons(muted: Boolean, onResume: () -> Unit, onRestart: () -> Unit, onToggleMute: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BigIconButton(
            icon = Icons.Rounded.PlayArrow,
            description = stringResource(R.string.resume),
            onClick = onResume
        )
        FilledTonalIconButton(
            onClick = onRestart,
            modifier = Modifier.size(68.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.restart),
                modifier = Modifier.size(32.dp)
            )
        }
        FilledTonalIconButton(
            onClick = onToggleMute,
            modifier = Modifier.size(68.dp)
        ) {
            Icon(
                imageVector = if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                contentDescription = stringResource(
                    if (muted) R.string.enable_sound else R.string.mute_sound
                ),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun HandednessPicker(handedness: Handedness, onHandedness: (Handedness) -> Unit) {
    Text(
        text = stringResource(R.string.joystick_side),
        style = MaterialTheme.typography.labelLarge,
        color = GamePalette.Muted
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HandednessButton(
            label = stringResource(R.string.left_handed),
            selected = handedness == Handedness.LEFT,
            onClick = { onHandedness(Handedness.LEFT) }
        )
        HandednessButton(
            label = stringResource(R.string.right_handed),
            selected = handedness == Handedness.RIGHT,
            onClick = { onHandedness(Handedness.RIGHT) }
        )
    }
}

@Composable
private fun GameOverContent(score: Int, highScore: Int, onRestart: () -> Unit) {
    Text(
        text = stringResource(R.string.nice_try),
        style = MaterialTheme.typography.headlineMedium,
        color = GamePalette.Wall,
        fontWeight = FontWeight.Black
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        OverlayStat(stringResource(R.string.score_label), score)
        OverlayStat(stringResource(R.string.best_label), max(score, highScore))
    }
    Spacer(modifier = Modifier.height(24.dp))
    BigIconButton(
        icon = Icons.Rounded.PlayArrow,
        description = stringResource(R.string.play_again),
        onClick = onRestart
    )
}

@Composable
private fun OverlayStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GamePalette.Muted,
            letterSpacing = 1.sp
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = GamePalette.Ink,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun BigIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(68.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun HandednessButton(label: String, selected: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) GamePalette.Muncher else GamePalette.Background,
            contentColor = if (selected) GamePalette.Background else GamePalette.Ink
        )
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
    }
}

private val OVERLAY_PHASES = setOf(
    GamePhase.START,
    GamePhase.READY,
    GamePhase.PAUSED,
    GamePhase.LEVEL_CLEAR,
    GamePhase.GAME_OVER
)

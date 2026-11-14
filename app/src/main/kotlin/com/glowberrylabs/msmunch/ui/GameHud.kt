package com.glowberrylabs.msmunch.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glowberrylabs.msmunch.R
import com.glowberrylabs.msmunch.game.GamePhase
import com.glowberrylabs.msmunch.game.GameState
import kotlin.math.max

@Composable
fun GameHud(
    game: GameState,
    muted: Boolean,
    onPause: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(game.score) {
        if (game.score > 0) {
            bounce.snapTo(1.16f)
            bounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudStats(game = game, bounceScale = bounce.value)
        Spacer(modifier = Modifier.weight(1f))
        HudControls(
            phase = game.phase,
            muted = muted,
            onPause = onPause,
            onToggleMute = onToggleMute
        )
    }
}

@Composable
private fun HudStats(game: GameState, bounceScale: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatBlock(
            label = stringResource(R.string.score_label),
            value = game.score,
            modifier = Modifier.scale(bounceScale)
        )
        Spacer(modifier = Modifier.width(16.dp))
        StatBlock(
            label = stringResource(R.string.best_label),
            value = max(game.highScore, game.score)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(game.lives.coerceAtMost(5)) {
                MiniMuncher()
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.level_label, game.level),
            style = MaterialTheme.typography.labelLarge,
            color = GamePalette.Muted,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HudControls(phase: GamePhase, muted: Boolean, onPause: () -> Unit, onToggleMute: () -> Unit) {
    IconButton(
        onClick = onPause,
        enabled = phase == GamePhase.RUNNING
    ) {
        Icon(
            imageVector = Icons.Rounded.Pause,
            contentDescription = stringResource(R.string.pause_action),
            tint = GamePalette.Ink
        )
    }
    IconButton(onClick = onToggleMute) {
        Icon(
            imageVector = if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
            contentDescription = stringResource(
                if (muted) R.string.enable_sound else R.string.mute_sound
            ),
            tint = GamePalette.Ink
        )
    }
}

@Composable
private fun StatBlock(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GamePalette.Muted,
            letterSpacing = 1.sp
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = GamePalette.Ink,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MiniMuncher() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val diameter = size.minDimension
        drawArc(
            color = GamePalette.Muncher,
            startAngle = 36f,
            sweepAngle = 288f,
            useCenter = true,
            topLeft = Offset.Zero,
            size = Size(diameter, diameter)
        )
        drawCircle(
            color = Color(0xFFFFD1DC),
            radius = diameter * 0.09f,
            center = Offset(diameter * 0.34f, diameter * 0.12f)
        )
    }
}

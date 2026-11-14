package com.glowberrylabs.msmunch.game

import android.view.View
import androidx.lifecycle.ViewModel
import com.glowberrylabs.msmunch.audio.GameHaptics
import com.glowberrylabs.msmunch.audio.GameSounds
import com.glowberrylabs.msmunch.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(
    private val settings: SettingsStore,
    private val sounds: GameSounds,
    private val haptics: GameHaptics,
    private val engine: GameEngine = GameEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GameUiState(
            game = engine.newGame(highScore = settings.highScore()),
            muted = settings.muted(),
            handedness = settings.handedness()
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var lastFrameNanos = 0L
    private var accumulator = 0.0
    private var chompAlternate = false

    init {
        sounds.muted = _uiState.value.muted
    }

    fun startGame() {
        update { state ->
            if (state.game.phase == GamePhase.START) {
                state.copy(game = engine.start(state.game))
            } else {
                state
            }
        }
        resetClock()
    }

    fun pause() {
        update { state ->
            if (state.game.phase == GamePhase.RUNNING) {
                state.copy(game = state.game.copy(phase = GamePhase.PAUSED))
            } else {
                state
            }
        }
        sounds.stopAll()
        accumulator = 0.0
    }

    fun resume() {
        update { state ->
            if (state.game.phase == GamePhase.PAUSED) {
                state.copy(game = state.game.copy(phase = GamePhase.RUNNING))
            } else {
                state
            }
        }
        resetClock()
    }

    fun restart() {
        val current = _uiState.value
        val best = maxOf(current.game.highScore, settings.highScore())
        val fresh = engine.start(engine.newGame(highScore = best))
        _uiState.value = current.copy(game = fresh)
        sounds.stopAll()
        resetClock()
    }

    fun turn(direction: Direction) {
        update { state -> state.copy(game = engine.turn(state.game, direction)) }
    }

    fun toggleMute() {
        val muted = !_uiState.value.muted
        settings.saveMuted(muted)
        sounds.muted = muted
        if (muted) {
            sounds.stopAll()
        }
        update { state -> state.copy(muted = muted) }
    }

    fun setHandedness(handedness: Handedness) {
        settings.saveHandedness(handedness)
        update { state -> state.copy(handedness = handedness) }
    }

    fun attachHaptics(view: View) {
        haptics.attach(view)
    }

    fun detachHaptics(view: View) {
        haptics.detach(view)
    }

    fun advance(frameTimeNanos: Long) {
        val previous = lastFrameNanos
        lastFrameNanos = frameTimeNanos
        if (previous == 0L) return

        val delta = ((frameTimeNanos - previous) / NANOS_PER_SECOND).coerceIn(0.0, MAX_FRAME_SECONDS)
        val current = _uiState.value
        if (current.game.phase.isIdle()) {
            accumulator = 0.0
            return
        }

        accumulator += delta
        var game = current.game
        while (accumulator >= STEP_SECONDS) {
            val (next, events) = engine.advance(game, STEP_SECONDS)
            game = next
            handleEvents(events, game)
            accumulator -= STEP_SECONDS
            if (game.phase.isIdle()) {
                accumulator = 0.0
                break
            }
        }
        if (game != current.game) {
            _uiState.value = current.copy(game = game)
        }
    }

    private fun handleEvents(events: List<GameEvent>, game: GameState) {
        events.forEach { event ->
            when (event) {
                GameEvent.Chomp -> {
                    chompAlternate = !chompAlternate
                    sounds.playChomp(chompAlternate)
                }
                GameEvent.Pellet -> {
                    sounds.play(GameSounds.Effect.PELLET)
                    haptics.tick()
                }
                GameEvent.Fruit -> sounds.play(GameSounds.Effect.FRUIT)
                GameEvent.GhostEaten -> {
                    sounds.play(GameSounds.Effect.GHOST_EATEN)
                    haptics.tick()
                }
                GameEvent.ExtraLife -> sounds.play(GameSounds.Effect.EXTRA_LIFE)
                GameEvent.Death -> {
                    sounds.play(GameSounds.Effect.DEATH)
                    haptics.thump()
                }
                GameEvent.LevelStart -> sounds.play(GameSounds.Effect.LEVEL_START)
                GameEvent.LevelClear -> Unit
                GameEvent.GameOver -> settings.saveHighScore(game.highScore)
            }
        }
    }

    private fun update(transform: (GameUiState) -> GameUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun resetClock() {
        lastFrameNanos = 0L
        accumulator = 0.0
    }

    private fun GamePhase.isIdle(): Boolean =
        this == GamePhase.START || this == GamePhase.PAUSED || this == GamePhase.GAME_OVER

    override fun onCleared() {
        sounds.release()
        super.onCleared()
    }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val MAX_FRAME_SECONDS = 0.25
        private const val STEP_SECONDS = 1.0 / 120.0
    }
}

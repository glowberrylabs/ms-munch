package com.glowberrylabs.msmunch.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(private val engine: GameEngine = GameEngine()) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(game = engine.newGame()))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var lastFrameNanos = 0L
    private var accumulator = 0.0

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
        val fresh = engine.start(engine.newGame(highScore = current.game.highScore))
        _uiState.value = current.copy(game = fresh)
        resetClock()
    }

    fun turn(direction: Direction) {
        update { state -> state.copy(game = engine.turn(state.game, direction)) }
    }

    fun toggleMute() {
        update { state -> state.copy(muted = !state.muted) }
    }

    fun setHandedness(handedness: Handedness) {
        update { state -> state.copy(handedness = handedness) }
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
            val (next, _) = engine.advance(game, STEP_SECONDS)
            game = next
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

    private fun update(transform: (GameUiState) -> GameUiState) {
        _uiState.value = transform(_uiState.value)
    }

    private fun resetClock() {
        lastFrameNanos = 0L
        accumulator = 0.0
    }

    private fun GamePhase.isIdle(): Boolean =
        this == GamePhase.START || this == GamePhase.PAUSED || this == GamePhase.GAME_OVER

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val MAX_FRAME_SECONDS = 0.25
        private const val STEP_SECONDS = 1.0 / 120.0
    }
}

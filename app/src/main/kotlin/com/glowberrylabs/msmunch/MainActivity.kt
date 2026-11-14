package com.glowberrylabs.msmunch

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glowberrylabs.msmunch.game.GamePhase
import com.glowberrylabs.msmunch.game.GameViewModel
import com.glowberrylabs.msmunch.game.GameViewModelFactory
import com.glowberrylabs.msmunch.ui.GameActions
import com.glowberrylabs.msmunch.ui.GameScreen
import com.glowberrylabs.msmunch.ui.MsMunchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            MsMunchTheme {
                val factory = remember { GameViewModelFactory(applicationContext) }
                val viewModel: GameViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val view = LocalView.current

                DisposableEffect(viewModel, view) {
                    viewModel.attachHaptics(view)
                    onDispose { viewModel.detachHaptics(view) }
                }

                BackHandler {
                    if (uiState.game.phase == GamePhase.RUNNING) {
                        viewModel.pause()
                    } else {
                        finish()
                    }
                }

                val actions = remember(viewModel) {
                    GameActions(
                        onStart = viewModel::startGame,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onRestart = viewModel::restart,
                        onDirection = viewModel::turn,
                        onToggleMute = viewModel::toggleMute,
                        onHandedness = viewModel::setHandedness,
                        onFrame = viewModel::advance
                    )
                }

                GameScreen(uiState = uiState, actions = actions)
            }
        }
    }
}

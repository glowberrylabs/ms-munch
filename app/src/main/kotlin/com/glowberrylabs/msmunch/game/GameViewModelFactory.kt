package com.glowberrylabs.msmunch.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.glowberrylabs.msmunch.audio.GameHaptics
import com.glowberrylabs.msmunch.audio.GameSounds
import com.glowberrylabs.msmunch.data.SettingsStore

class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        return GameViewModel(
            settings = SettingsStore(appContext),
            sounds = GameSounds(appContext),
            haptics = GameHaptics()
        ) as T
    }
}

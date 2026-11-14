package com.glowberrylabs.msmunch.data

import android.content.Context
import com.glowberrylabs.msmunch.game.Handedness

class SettingsStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun highScore(): Int = preferences.getInt(KEY_HIGH_SCORE, 0)

    fun saveHighScore(score: Int) {
        if (score <= highScore()) return
        preferences.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    fun muted(): Boolean = preferences.getBoolean(KEY_MUTED, false)

    fun saveMuted(muted: Boolean) {
        preferences.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    fun handedness(): Handedness {
        val value = preferences.getString(KEY_HANDEDNESS, null) ?: return Handedness.RIGHT
        return runCatching { Handedness.valueOf(value) }.getOrDefault(Handedness.RIGHT)
    }

    fun saveHandedness(handedness: Handedness) {
        preferences.edit().putString(KEY_HANDEDNESS, handedness.name).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "ms_munch_settings"
        const val KEY_HIGH_SCORE = "high_score"
        const val KEY_MUTED = "muted"
        const val KEY_HANDEDNESS = "handedness"
    }
}

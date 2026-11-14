package com.glowberrylabs.msmunch.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.glowberrylabs.msmunch.R

class GameSounds(context: Context) {

    enum class Effect(@RawRes val resourceId: Int) {
        CHOMP_A(R.raw.chomp_a),
        CHOMP_B(R.raw.chomp_b),
        PELLET(R.raw.pellet),
        FRUIT(R.raw.fruit),
        GHOST_EATEN(R.raw.ghost_eaten),
        EXTRA_LIFE(R.raw.extra_life),
        DEATH(R.raw.death),
        LEVEL_START(R.raw.level_start)
    }

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<Effect, Int>()
    private val loadedIds = mutableSetOf<Int>()
    private var lastChompNanos = 0L

    var muted: Boolean = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(loadedIds) { loadedIds += sampleId }
            }
        }
        Effect.entries.forEach { effect ->
            soundIds[effect] = soundPool.load(context.applicationContext, effect.resourceId, 1)
        }
    }

    fun play(effect: Effect) {
        if (muted) return
        val id = soundIds[effect] ?: return
        val loaded = synchronized(loadedIds) { id in loadedIds }
        if (!loaded) return
        soundPool.play(id, VOLUME, VOLUME, PRIORITY, 0, 1f)
    }

    fun playChomp(alternate: Boolean) {
        if (muted) return
        val now = System.nanoTime()
        if (now - lastChompNanos < CHOMP_INTERVAL_NANOS) return
        lastChompNanos = now
        play(if (alternate) Effect.CHOMP_A else Effect.CHOMP_B)
    }

    fun stopAll() {
        if (muted) return
        soundIds.values.forEach { id ->
            soundPool.stop(id)
        }
    }

    fun release() {
        soundPool.release()
    }

    private companion object {
        const val VOLUME = 0.85f
        const val PRIORITY = 1
        const val CHOMP_INTERVAL_NANOS = 55_000_000L
    }
}

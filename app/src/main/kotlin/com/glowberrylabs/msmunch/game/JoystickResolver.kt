package com.glowberrylabs.msmunch.game

import kotlin.math.abs
import kotlin.math.hypot

class JoystickResolver(private val engageFraction: Float = 0.28f, private val releaseFraction: Float = 0.16f) {
    var direction: Direction? = null
        private set

    private var engaged = false

    fun onDown(x: Float, y: Float, radius: Float): Direction? {
        val length = hypot(x, y)
        if (length < engageFraction * radius) return null
        engaged = true
        direction = directionFor(x, y) ?: direction
        return direction
    }

    fun onMove(x: Float, y: Float, radius: Float): Direction? {
        val length = hypot(x, y)
        if (!engaged) {
            if (length >= engageFraction * radius) {
                engaged = true
                direction = directionFor(x, y) ?: direction
            }
        } else if (length >= releaseFraction * radius) {
            direction = directionFor(x, y) ?: direction
        }
        return if (engaged) direction else null
    }

    fun onUp(): Direction? {
        engaged = false
        return direction
    }

    fun reset() {
        engaged = false
        direction = null
    }

    private fun directionFor(x: Float, y: Float): Direction? {
        if (x == 0f && y == 0f) return null
        return if (abs(x) >= abs(y)) {
            if (x > 0f) Direction.RIGHT else Direction.LEFT
        } else {
            if (y > 0f) Direction.DOWN else Direction.UP
        }
    }
}

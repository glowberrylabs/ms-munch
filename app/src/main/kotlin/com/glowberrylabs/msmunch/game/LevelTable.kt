package com.glowberrylabs.msmunch.game

object LevelTable {

    fun playerSpeed(level: Int): Double = (6.5 + 0.25 * (level - 1)).coerceAtMost(8.0)

    fun ghostSpeed(level: Int): Double = (5.9 + 0.22 * (level - 1)).coerceAtMost(7.8)
}

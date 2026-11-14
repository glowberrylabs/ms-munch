package com.glowberrylabs.msmunch.game

data class ModeWindow(val mode: GhostMode, val seconds: Double)

object LevelTable {

    fun playerSpeed(level: Int): Double = (6.5 + 0.25 * (level - 1)).coerceAtMost(8.0)

    fun ghostSpeed(level: Int): Double = (5.9 + 0.22 * (level - 1)).coerceAtMost(7.8)

    fun frightenedSeconds(level: Int): Double = (7.0 - 0.5 * (level - 1)).coerceAtLeast(2.0)

    fun fruitValue(level: Int): Int = FRUIT_VALUES[(level - 1) % FRUIT_VALUES.size]

    fun schedule(level: Int): List<ModeWindow> {
        val scatters = when {
            level <= 2 -> listOf(6.0, 5.0, 4.0)
            level <= 4 -> listOf(5.0, 4.0, 3.0)
            level <= 6 -> listOf(4.0, 3.0, 2.0)
            else -> listOf(3.0, 2.0, 1.0)
        }
        val windows = mutableListOf<ModeWindow>()
        scatters.forEachIndexed { index, seconds ->
            windows += ModeWindow(GhostMode.SCATTER, seconds)
            val chaseSeconds = if (index == scatters.lastIndex) {
                Double.POSITIVE_INFINITY
            } else {
                CHASE_SECONDS
            }
            windows += ModeWindow(GhostMode.CHASE, chaseSeconds)
        }
        return windows
    }

    private const val CHASE_SECONDS = 20.0

    private val FRUIT_VALUES = listOf(100, 300, 500, 700, 1000, 2000, 3000, 5000)
}

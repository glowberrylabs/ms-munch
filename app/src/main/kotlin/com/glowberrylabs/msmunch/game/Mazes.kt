package com.glowberrylabs.msmunch.game

object Mazes {
    val A: Maze by lazy { MazeParser.parse("A", MazeLayouts.mazeA) }
    val B: Maze by lazy { MazeParser.parse("B", MazeLayouts.mazeB) }

    fun forLevel(level: Int): Maze = if (level % 2 == 1) A else B
}

package com.glowberrylabs.msmunch.game

object Mazes {
    val maze1: Maze by lazy { MazeParser.parse("Maze 1", MazeLayouts.maze1) }
    val maze2: Maze by lazy { MazeParser.parse("Maze 2", MazeLayouts.maze2) }
    val maze3: Maze by lazy { MazeParser.parse("Maze 3", MazeLayouts.maze3) }
    val maze4: Maze by lazy { MazeParser.parse("Maze 4", MazeLayouts.maze4) }

    val all: List<Maze> = listOf(maze1, maze2, maze3, maze4)

    fun forLevel(level: Int): Maze = when {
        level <= 2 -> maze1
        level <= 5 -> maze2
        level <= 9 -> maze3
        level <= 13 -> maze4
        (level - 14) / 4 % 2 == 0 -> maze3
        else -> maze4
    }
}

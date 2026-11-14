package com.glowberrylabs.msmunch.game

sealed interface GameEvent {
    data object Chomp : GameEvent
    data object Pellet : GameEvent
    data object Fruit : GameEvent
    data object GhostEaten : GameEvent
    data object ExtraLife : GameEvent
    data object Death : GameEvent
    data object LevelStart : GameEvent
    data object LevelClear : GameEvent
    data object GameOver : GameEvent
}

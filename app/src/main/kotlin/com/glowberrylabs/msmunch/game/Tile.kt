package com.glowberrylabs.msmunch.game

enum class Tile {
    WALL,
    DOT,
    PELLET,
    EMPTY,
    DOOR,
    HOUSE,
    TUNNEL;

    val isEdible: Boolean
        get() = this == DOT || this == PELLET
}

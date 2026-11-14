package com.glowberrylabs.msmunch.game

object GhostTargets {

    fun targetFor(kind: GhostKind, state: GameState): TilePos = if (state.globalMode == GhostMode.SCATTER) {
        state.maze.scatterTargets[kind.ordinal]
    } else {
        chaseTarget(kind, state)
    }

    fun chaseTarget(kind: GhostKind, state: GameState): TilePos {
        val playerTile = state.player.pos.tile(state.maze)
        return when (kind.personality) {
            GhostPersonality.CHASER -> playerTile
            GhostPersonality.AMBUSHER -> state.player.direction.offset(playerTile, 4)
            GhostPersonality.FLANKER -> {
                val ahead = state.player.direction.offset(playerTile, 2)
                val cocoa = state.grumps.first { it.kind == GhostKind.COCOA }.pos.tile(state.maze)
                TilePos(2 * ahead.col - cocoa.col, 2 * ahead.row - cocoa.row)
            }
            GhostPersonality.SHY -> {
                val grump = state.grumps.first { it.kind == kind }
                if (grump.pos.distanceTo(state.player.pos) <= SHY_DISTANCE) {
                    state.maze.scatterTargets[kind.ordinal]
                } else {
                    playerTile
                }
            }
        }
    }

    private const val SHY_DISTANCE = 8.0
}

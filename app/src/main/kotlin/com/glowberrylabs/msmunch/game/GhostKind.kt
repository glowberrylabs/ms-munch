package com.glowberrylabs.msmunch.game

enum class GhostPersonality {
    CHASER,
    AMBUSHER,
    FLANKER,
    SHY
}

enum class GhostKind(val personality: GhostPersonality, val displayName: String) {
    COCOA(GhostPersonality.CHASER, "Cocoa"),
    BUBBLE(GhostPersonality.AMBUSHER, "Bubble"),
    TAFFY(GhostPersonality.FLANKER, "Taffy"),
    MINT(GhostPersonality.SHY, "Mint")
}

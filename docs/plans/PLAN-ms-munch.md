# Plan: Ms. Munch

## Intent

- An offline Android maze-chase game inspired by classic 1980s arcade
  games, with an original name, art, and audio.
- Designed for kids: pick up and play, no reading required, no ads, no
  accounts, no network.
- Short rounds, immediate replay, and a persistent best score.
- An on-screen joystick replaces the D-pad used in Fruit Snake.
- No runtime permissions and no INTERNET permission. Every asset is
  bundled in the APK.

## Identity and conventions

- Display name: Ms. Munch.
- Application id and namespace: `com.glowberrylabs.msmunch`.
- Root project name: `msmunch`.
- Repository directory: `ms-munch`.
- Package directories under
  `app/src/main/kotlin/com/glowberrylabs/msmunch`.
- Single `app` module. Kotlin, Jetpack Compose, Material 3.
- compileSdk 36, minSdk 26, targetSdk 35, JVM target 17, JDK 21.
- Kotlin 2.3.21, AGP 8.13.2, Compose BOM 2026.05.00, Gradle 8.14.5.
- ktlint and detekt, with justfile recipes matching the sibling
  Android repos.
- Dual MIT/Apache-2.0 licensing.
- Dark-only arcade theme. No dynamic color. No `FLAG_SECURE`.
- No game engine, Room, dependency injection framework, or navigation
  library.

## Legal guardrail

- Homage only. No Bandai Namco names, characters, sprites, sounds, or
  fonts.
- Maze layouts reproduce the four classic Ms. Pac-Man arcade mazes.
  Character designs, vector art, and generated audio are original.
- In-app copy and user-facing documentation call it a maze chase and
  never use the Pac-Man trademarks.
- Reconfirm the licensing posture before any public distribution, since
  the layouts are copied from the arcade game.

## Board and rules

- 28 columns by 31 rows, the classic maze-chase proportion.
- Four layouts matching the arcade mazes. Levels 1-2 use maze 1,
  3-5 maze 2, 6-9 maze 3, and 10-13 maze 4. After level 13, mazes 3
  and 4 alternate every four levels.
- Tile types: wall, dot, power pellet, empty corridor, ghost house
  door, house interior, and tunnel.
- Side tunnels wrap horizontally. Maze 1 has tunnels on rows 8 and
  17, maze 2 on rows 1 and 23, maze 3 on row 9, and maze 4 on rows
  13 and 16 (zero-based rows).
- Edible counts per maze, including power pellets: 224, 244, 242, and
  238. Each maze has exactly four power pellets.
- Clear all dots and pellets to complete a level.
- Three lives. One extra life at 10,000 points per game.
- Fruit appears below the house at 70 and 170 dots eaten and stays for
  nine seconds.
- Dying resets actor positions but keeps eaten dots, score, and level.
- Game over at zero lives. The high score is saved when the round ends.

## Movement

- Continuous positions along corridors. All speeds are in tiles per
  second.
- Fixed-step simulation at 120 Hz with an accumulator. Rendering runs
  at the display refresh rate.
- Player speed is 6.5 tiles per second at level 1, increases by 0.25
  per level, and caps at 8.0.
- Instant 180-degree reversal is allowed anywhere in a corridor.
- Perpendicular turns are allowed only within a small tolerance of a
  tile center. The actor snaps to the center and turns.
- Direction input is buffered until the next legal turn.
- No diagonals and no corner cutting. Actors stay centered on their
  moving axis.
- Ghosts move at 60 percent speed in tunnels. The player passes at
  normal speed.
- Ghosts may not reverse except when a global mode change forces it.

## Ghosts and modes

- Four original candy-flavored ghosts, with provisional names and
  personalities:
  - Cocoa, chaser: targets the player tile.
  - Bubble, ambusher: targets four tiles ahead of the player.
  - Taffy, flanker: doubles the vector from Cocoa to two tiles ahead
    of the player.
  - Mint, shy: targets the player when far away and its home corner
    when within eight tiles.
- At intersections, ghosts pick the legal neighbor that minimizes
  Euclidean distance to their target, in a fixed tie-break order.
  Reverse is excluded.
- Scatter and chase alternate on a schedule by level band. At level 1:
  scatter 6s, chase 20s, scatter 5s, chase 20s, scatter 4s, then
  chase indefinitely. Later bands shorten the scatter windows.
- Scatter targets are the four maze corners.
- Ghost house release thresholds by dots eaten: Cocoa 0, Bubble 20,
  Taffy 45, Mint 70.
- A power pellet flips active ghosts to frightened for seven seconds
  at level 1, minus 0.5 seconds per level with a two-second floor, and
  forces a reversal. Frightened ghosts move randomly at 55 percent
  speed and flash near the end.
- Eating ghosts in one frightened window scores 200, 400, 800, then
  1600. Eaten ghosts become eyes, run home at 12 tiles per second,
  revive in the house, and leave through the normal door path.
- No chain points are available after the 1600 pickup until the next
  power pellet.

## Scoring and level ramp

- Dot 10, pellet 50. Fruit is worth 100, 300, 500, 700, 1000, 2000,
  3000, then 5000 by level band, cycling.
- Clearing a level pauses briefly, then starts the next level with
  that level's maze, fresh dots, and reset actors.
- Ghost normal speed is 5.9 tiles per second at level 1, increases by
  0.22 per level, and caps at 7.8.
- Speeds never drop below their level 1 values.
- Level 1 through 3 are tuned kid-friendly. By level 8 the pace
  approaches the arcade classics.

## Joystick controls

- A fixed joystick lives centered in a dedicated control strip below
  the maze in portrait and in a side gutter in landscape.
- The base is analog-looking with a knob, but output is four-way
  digital. Diagonal input is ignored.
- Base diameter 132dp, knob 56dp. Engage at 28 percent of the base
  radius, release with hysteresis at 16 percent.
- The dominant axis wins. The last direction sticks. There is no
  neutral state.
- Tapping the base edge selects that direction. Dragging anywhere in
  the strip steers continuously.
- One active pointer. Additional fingers are ignored.
- Direction changes fire a light haptic tick.
- A handedness toggle in the pause menu persists and chooses the
  landscape gutter.
- The stick is disabled during start, ready, paused, level-clear, and
  game-over overlays.
- Pause button in the HUD. Auto-pause at `ON_PAUSE`. Back pauses a
  live round; a second back exits.
- Keep the screen awake while a round is running.

## UI and art

- Deep indigo background, luminous cyan walls, warm gold dots, and
  soft glow accents. Dark mode only.
- Maze walls are thin outlines around wall regions with rounded
  joins, drawn neighbor-aware and cached into a bitmap per maze and
  size.
- Ms. Munch is a coral-pink round muncher with a bow, eyelashes, and an
  animated mouth that opens and closes while moving and blinks when
  idle.
- Ghosts are four rounded candy blobs with expressive eyes that track
  their movement. Frightened ghosts turn blue-white and flash. Eaten
  ghosts are eyes only.
- Pellets pulse. Fruit pops in with a spring. The score badge bounces
  on pickup.
- HUD: score, best, lives as mini muncher icons, level, pause, and
  mute.
- Overlays: start, Ready!, paused, level cleared, and game over, with
  friendly copy, large icon buttons, and fade and scale animation.
- Responsive layout: portrait stacks the HUD, maze, and a centered
  control strip. Landscape centers the maze with the joystick in the
  chosen side gutter.
- Edge-to-edge layout with safe drawing insets applied.
- No text is required to play.

## Audio and haptics

- SoundPool with `USAGE_GAME` attributes.
- Eight short WAV effects generated by `scripts/make_sounds.py` using
  only the Python standard library: chomp A, chomp B, pellet, fruit,
  ghost eaten, extra life, death, and level start.
- Chomps alternate per dot and are rate-limited so they do not
  overlap.
- Mute toggle persists in SharedPreferences.
- Haptics use `View.performHapticFeedback`: `CLOCK_TICK` on pellet and
  ghost eaten, `LONG_PRESS` on death.

## Persistence

- SharedPreferences holds the high score, mute preference, and
  joystick handedness.
- The high score is saved only when a round ends.

## Architecture

- `game` package, pure Kotlin and unit-testable:
  - `Maze`, `Tile`, `MazeParser`, and `MazeValidator`.
  - `Direction`, `ActorState`, `MsMunch`, `Grump`, `GhostPersonality`,
    and `GhostMode`.
  - `LevelTable` for speeds, scatter and chase timing, frightened
    duration, and fruit values.
  - `GamePhase`, `GameState`, `GameEvent`, and `GameEngine` with
    `newGame`, `turn`, and `advance(dtSeconds)` returning state plus
    events.
  - `JoystickResolver` for pure input quantization with hysteresis.
  - Injectable `Random` for frightened movement and any tie-break
    variation, so tests are deterministic.
- `data` package: `SettingsStore` wraps SharedPreferences.
- `audio` package: `GameSounds` wraps SoundPool loading and playback.
- `ui` package: `MsMunchTheme`, `GameScreen`, `MazeBoard`, `Joystick`,
  `GameHud`, `GameOverlays`, palette, and painter helpers.
- `GameViewModel` owns `GameState`, exposes a StateFlow, applies
  joystick input, runs the fixed-step accumulator inside
  `advance(frameTimeNanos)`, maps events to sound and haptics, and
  persists settings.
- The UI drives frames with `withFrameNanos` and calls the ViewModel.
  The ViewModel clamps large frame deltas to 250ms to avoid catch-up
  spirals.
- `MainActivity` handles edge-to-edge, ViewModel wiring, lifecycle
  auto-pause, and back handling.
- The UI depends only on immutable state and callbacks.
- The static maze layer is cached as an `ImageBitmap`. Dynamic dots,
  pellets, actors, and effects are drawn per frame.

## Testing

- JUnit 4 unit tests for the engine:
  - parsing and validating all four mazes, including connectivity,
    pellet reachability, house exits, dot totals, tunnel rows, and
    dead ends
  - corridor movement and wall blocking
  - perpendicular turn at a tile center with snapping
  - buffered turn applied at the next legal center
  - instant reversal
  - tunnel wraparound
  - dot and pellet scoring
  - ghost chain scoring and reset
  - fruit timing, value, and expiry
  - frightened duration, speed, flashing threshold, and forced
    reversal
  - eyes return home, revive, and re-release
  - house release thresholds
  - scatter and chase swaps
  - life loss keeps dots, game over at zero lives, extra life only
    once
  - level clear advances and resets, speeds cap
  - deterministic behavior with a seeded Random
- `JoystickResolver` tests: deadzone, hysteresis, axis dominance, and
  tap resolution.
- `just check` runs ktlint and detekt. `just test` runs unit tests.
  `just build` produces the debug APK. `just run` installs and
  launches on the pixel9a emulator.
- Manual verification: portrait and landscape, handedness, joystick
  tap and drag, pause, background and foreground, mute, high score
  persistence, all four mazes, the frightened chain, death, and game
  over.

## Stages

1. Plan document.
2. Project scaffold: gitignore, editorconfig, licenses, Gradle wrapper
   and scripts, manifest, theme, launcher placeholder, justfile, README
   skeleton.
3. Maze data, parser, validator, four arcade layouts, and tests.
4. Player and actor movement engine with tests.
5. Ghost AI, house and mode timers, frightened and eyes, with tests.
6. Round flow: scoring, fruit, lives, levels, extra life, events, and
   tests.
7. Joystick component and resolver tests.
8. Full UI: cached maze renderer, actors, HUD, overlays, responsive
   layout.
9. Audio generation, SoundPool, and preference persistence.
10. Verification, screenshot, README, and fixes.

## Non-features

- No cutscenes, attract mode, or intermissions.
- No co-op or multiplayer.
- No online leaderboard or cloud sync.
- No skins, unlockables, or in-app purchases.
- No localization beyond English.
- No bouncing bonus fruit or background music.
- No ads, analytics, accounts, crash reporting, or network access.
- No runtime permissions and no INTERNET permission.
- No release signing configuration.
- No game engine, dependency injection, Room, or navigation library.

## Definition of done

- Debug APK builds and installs.
- `just check` and `just test` pass.
- The merged manifest contains no runtime permissions and no INTERNET
  permission.
- Joystick steering, tap-to-turn, the landscape gutter choice, and
  pause work in portrait and landscape.
- All four mazes play, all four ghost personalities are observable, and
  the frightened chain and eyes return work.
- Lives, extra life, fruit, level ramp, high score, mute, and
  handedness persist correctly.
- Screenshot and README are complete.
- Both license files are present.

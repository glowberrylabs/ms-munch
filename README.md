# Ms. Munch

An offline maze-chase game for kids. Guide Ms. Munch through glowing
mazes, munch every dot, and outsmart four candy-colored grumps. Short
rounds, immediate replay, and a saved best score. No internet, no ads,
and no runtime permissions.

## How to play

- Steer with the on-screen joystick.
- Eat every dot and power pellet to clear the maze.
- Power pellets frighten the grumps for a few seconds. Catch them for
  bonus points.
- Three lives per round, with an extra life at 10,000 points.
- Pause any time. Back pauses a live round.

## Build

Prerequisites: JDK 21, Android SDK, `adb`, and `just` on your `PATH`.

```bash
just build
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Install

```bash
just run
```

## Offline and private

- No internet permission and no network access.
- No ads, analytics, accounts, or tracking.
- All art and audio are bundled in the APK.

## License

Licensed under either of

- MIT license ([LICENSE-MIT](LICENSE-MIT) or
  https://opensource.org/licenses/MIT)
- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or
  https://www.apache.org/licenses/LICENSE-2.0)

at your option.

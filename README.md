# Super Mario War – Android port

This repository contains an Android port of [Super Mario War](https://github.com/mmatyas/supermariowar),
maintained by [@doctormajid7-ux](https://github.com/doctormajid7-ux).

Super Mario War is a local multiplayer platform battle game created and maintained
by the Super Mario War contributors.

I originally created this Android port for my children. I spent many great hours
playing Super Mario War with my brother when I was younger, and this port lets us
enjoy the game together again.

**How to activate splitscreen (important):**

1. Install the APK, open the game, press OK / Pause to skip the splash screen
2. Go to the player / number of players selection screen
3. Set **Player 2 to HUMAN** (keep Player 1 as Human, set others to CPU or Off)
4. Then, with **Player 1, press the Action button** on that player-number selection screen to enable dual-screen
> It will NOT work if you select Player 3 or 4 - it must be Player 1 + Player 2 Human.

5. Enable `Portrait 2P: ON` and rotate your phone to portrait. Top half = Player 2 (flipped), bottom half = Player 1. Each half has its own D-pad + Jump + Action + Item buttons.

Tips: Direction + Action + Jump can be held at the same time with multitouch.

Would love feedback, especially from people testing 2P on real devices. Thanks to the original SMW creators and all contributors!

Fan project, not affiliated with Nintendo.

## Android features

- ARM64 Android build using SDL2, SDL2_image and SDL2_mixer.
- Multitouch controls with safe-area support; 4/6/8-contact logic covered by host tests.
- Menu-only D-pad Up; the dedicated Jump button handles jumping during matches.
- Optional portrait two-player mode with opposing control areas.
- Dedicated Player 2 touch controls.
- Bluetooth controller support through SDL2.
- Random bot skins and restore-default-controls actions.
- Automatic map-thumbnail cache generation.
- Renderer recovery after returning from another application.

## Download

The latest test APK is available from the
[GitHub Releases page](https://github.com/doctormajid7-ux/supermariowar-android-2Pdualscreen/releases).

## Android build

Requirements: Android SDK, Android NDK `27.2.12479018`, and Java 17.

```sh
ANDROID_HOME=/path/to/android-sdk JAVA_HOME=/path/to/jdk-17 ./android/build.sh
```

The APK is generated at:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Documentation

Android-specific documentation is available in [`docs/`](docs/), including touch
controls, portrait two-player mode, map thumbnails and porting notes.

For the original game, Windows and Linux versions, general compilation instructions,
game information and the complete upstream documentation, visit the
[official Super Mario War repository](https://github.com/mmatyas/supermariowar).

## Credits

- Original game: the Super Mario War contributors.
- Android port: [@doctormajid7-ux](https://github.com/doctormajid7-ux).

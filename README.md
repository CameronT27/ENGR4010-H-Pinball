# ENGR4010-H-Pinball

2.5D Pinball is a Java/LWJGL prototype for the ENGR4010-H capstone project. It
is planned as a downloadable desktop game for Windows and macOS. The target
presentation is a 3D-looking table with ball gameplay primarily constrained to
a 2D playfield; the current prototype is the first local gameplay foundation.

The current prototype opens an interactive OpenGL pinball table. The ball
starts in the right-side launcher; hold and release the launch key to choose
launch power. The launch follows a curved guide into the playfield, after
which gravity and collisions take over. Bumper hits increase the score, and
draining the ball between the flippers uses one of three balls.

Online connectivity is intentionally out of scope for the first MVP. The team
can revisit networking after the local game is stable and distributable.

## MVP acceptance checklist

The first MVP is complete when a new player can do the following without
developer assistance:

- Launch the packaged game on Windows or macOS.
- Start with one complete playable table.
- Launch the ball and operate both flippers.
- See the ball collide with table objects and bumpers.
- See the score increase after scoring events.
- See the remaining-ball count decrease when the ball drains.
- Hear feedback for launching, flippers, bumpers, table targets, and drains.
- Reach game over and reset the game.
- Understand the controls from the game documentation.

Features such as additional tables, power-ups, progression, game modes,
multiplayer, and advanced visual effects are deferred until this checklist is
reliable.

Gameplay sound effects are generated at runtime, so no separate audio files are
required. If the operating system has no available audio line, the game
continues to run without sound.

## Requirements

- Java 25 JDK
- Internet access the first time Gradle downloads its distribution and LWJGL dependencies
- macOS, Windows, or Linux with working graphics drivers

The project includes the Gradle wrapper, so a separate Gradle installation is not required.

## Launch

From the repository root, run the command for your platform.

### macOS and Linux

```sh
./gradlew run
```

### Windows PowerShell

```powershell
.\gradlew.bat run
```

### Windows Command Prompt

```bat
gradlew.bat run
```

The Gradle configuration selects the correct LWJGL 3.4.3 native libraries for the operating system and processor architecture. macOS automatically receives the required `-XstartOnFirstThread` JVM option.

## Build Check

To compile without opening the window:

```sh
./gradlew compileJava
```

On Windows, use `gradlew.bat compileJava` instead.

## Package the App

The project includes a `jpackage` task for creating a platform-specific app package. Run it on the platform you want to distribute:

### macOS

```sh
./gradlew clean packageApp
```

This creates a macOS disk image in `build/jpackage/`.

### Windows PowerShell

```powershell
.\gradlew.bat clean packageApp
```

This creates a Windows installer in `build/jpackage/`. Windows installer generation requires the WiX Toolset to be installed and available on `PATH`. If WiX is not installed, the current `packageApp` task cannot create the Windows package; use WiX or add a separate `app-image` task before distributing the application directory as a ZIP file.

### Linux

```sh
./gradlew clean packageApp
```

This creates a Linux application image in `build/jpackage/`, which can be compressed and distributed as a ZIP or tarball.

Each package must be built on its target operating system because LWJGL includes platform-specific native libraries. The package includes its own Java runtime, so end users do not need to install Java separately.

## Controls

- `Enter`: start the game from the welcome screen
- `Space`: hold to charge the right-side launcher, release to launch
- `A` or `Left Arrow`: activate the left flipper
- `D` or `Right Arrow`: activate the right flipper
- `Z`: nudge the table left
- `X`: nudge the table right
- `R`: reset the game and return the ball to the launcher
- `M`: return to the main menu after game over
- `Escape`: close the game window

## Troubleshooting

If Java is not found, install a Java 25 JDK and verify it with `java -version`. If Gradle cannot download dependencies, check the internet connection and run the launch command again.

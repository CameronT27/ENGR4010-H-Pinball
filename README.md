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

The game has four fixed table layouts built around different active rail
routes. Table 1 uses a rising central fork, table 2 uses staggered diagonals
for alternating shots, table 3 uses offset shelves for broad cross-table
routes, and table 4 enters from the left, climbs around the upper playfield,
crosses the top, and exits at the upper-right before sending the ball downward
into a bumper. Each table uses three deliberately placed bumpers and two
scoring rails. The HUD shows both the current table and the overall
progression.

Progression advances from 1 to 2 to 3 to 4 and then selects tables 1, 2, 3,
and 4 again. The score target continues increasing across the loop (350, 700,
1050, 1400, and so on), while the current-level score resets to zero when a new
table is loaded. The total score remains accumulated. Resetting with `R` starts
a new game at progression 1, table 1.

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

Features such as additional tables, game modes, multiplayer, and advanced
visual effects are deferred until this checklist is reliable. The current
prototype includes scoring progression, four hand-designed table layouts,
and a point power-up.

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
- `N`: development shortcut to qualify for the next level
- `Escape`: close the game window

The flippers, launcher, nudge controls, scoring, sound feedback, ball count,
and game-over behavior are shared across all four tables.

## Between-level shop

Completing a level opens a paused shop. The current-level score earns one coin
per 100 points (remainder discarded), up to a 99-coin wallet. Coins and
purchased perks persist across table changes; `R` starts a genuinely new game
and clears them.

- `Up` / `Down`: select an offer
- `B`: buy the selected perk when affordable
- `F`: refresh the three offers; the cost is 1, then 2, 4, 8, and so on
- `Enter`: leave the shop and start the next table

The shop offers Combo Multiplier (cap up to x5), Longer Combo Window (three
upgrades), Bumper Bonus (three +15% upgrades), and Extra Ball (two purchases).
Combo multiplier increases on consecutive bumper hits and resets when its
timer expires. The current combo multiplier and coin balance are shown during
gameplay and in the shop. The HUD also shows the number of consecutive bumper
hits in the active combo. Buying an offer removes it from the current shop;
refreshing or completing another level supplies new offers.

The backboard displays up to five ball indicators, including balls purchased
from the Extra Ball perk.

## Troubleshooting

If Java is not found, install a Java 25 JDK and verify it with `java -version`. If Gradle cannot download dependencies, check the internet connection and run the launch command again.

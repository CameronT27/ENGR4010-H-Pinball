# ENGR4010-H-Pinball

Online Pinball is a Java/LWJGL prototype for the ENGR4010-H capstone project.

The current prototype opens an interactive OpenGL pinball table. The ball is affected by gravity and bounces off the table boundary, bumpers, and flippers.

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

## Controls

- `A` or `Left Arrow`: activate the left flipper
- `D` or `Right Arrow`: activate the right flipper
- `Escape`: close the game window

## Troubleshooting

If Java is not found, install a Java 25 JDK and verify it with `java -version`. If Gradle cannot download dependencies, check the internet connection and run the launch command again.

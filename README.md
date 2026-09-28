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

This creates a Windows installer in `build/jpackage/`. Windows installer generation requires the WiX Toolset to be installed and available on `PATH`. If WiX is not installed, use the app-image option described below and distribute the generated application directory as a ZIP file.

### Linux

```sh
./gradlew clean packageApp
```

This creates a Linux application image in `build/jpackage/`, which can be compressed and distributed as a ZIP or tarball.

Each package must be built on its target operating system because LWJGL includes platform-specific native libraries. The package includes its own Java runtime, so end users do not need to install Java separately.

## Controls

- `A` or `Left Arrow`: activate the left flipper
- `D` or `Right Arrow`: activate the right flipper
- `Escape`: close the game window

## Troubleshooting

If Java is not found, install a Java 25 JDK and verify it with `java -version`. If Gradle cannot download dependencies, check the internet connection and run the launch command again.

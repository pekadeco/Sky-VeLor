# Sky VeLor — Beta-0.10.6.5v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, and quick notes.

## Features

- Settings hub and movable HUD editor with dark and light themes.
- Smooth, frame-rate-independent UI motion: soft hover and press feedback, animated toggles and sliders, a sliding segmented selector, a gently appearing dropdown, and responsive search-field focus.
- Rounded animated buttons in the main/pause menu and Quick Note screen, with distinct primary, secondary and destructive styles.
- Spirit Leap teammate picker, class labels, player heads and isolated preview.
- A 1-14 Click in order terminal: a panel with fourteen numbered buttons in two rows of seven that takes
  over the Floor VII / M7 Click in order chest, with the number to click now in green, the one after it in
  yellow, a mistake counter, a timer, restart on `R`, close on `Esc` and a transparency toggle. `/terminal`
  opens the same panel for practice.
- Dungeon mob highlights, Floor VII overlays and dragon alerts.
- Fishing and Slayer trackers, plus quick notes and client commands.

## Requirements

- Minecraft 1.8.9 with Forge `11.15.1.2318`.
- JDK 8 for compiling and Java 8 for the legacy Forge runtime.
- Internet access on the first build so Gradle and ForgeGradle can download their dependencies.

This project intentionally retains the ForgeGradle 2.1 / Gradle 2.14.1 toolchain used by Forge 1.8.9. Newer JDKs and Gradle releases are not drop-in replacements for this legacy build.

## Build on Windows

Open Command Prompt in this folder and make sure `JAVA_HOME` points to a JDK 8 installation. Then run:

```bat
gradlew.bat clean build dumpTestClasspath
```

To run the standalone tests in Command Prompt:

```bat
tests\run-tests.bat
```

## Build on Linux or Git Bash

With JDK 8 selected, run:

```sh
./gradlew clean build dumpTestClasspath
TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
```

The mod JAR is written to `build/libs/`. The generated `build/` and `.gradle/` directories are temporary and should not be included in source ZIPs.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.6.5v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Compatibility and testing limits

The project targets desktop Java 8. Android/Pojav compatibility is not verified. Automated tests cover logic, settings and configuration; they do not launch Minecraft or verify live-server behavior. Test rendering and gameplay in Minecraft 1.8.9 before publishing a release.

## Terminal GUI fix in Beta-0.10.6.5v

The fourteen numbered buttons were not drawn when the Click in order chest could not be read, so the panel
appeared empty in game while the `/terminal` preview was fine. The buttons are now always drawn and always
clickable, and the number of each item is read from its name, its lore or a stained glass pane's stack
size. The root cause, the change and the full verification (including the offline GUI test rig in
`tools/terminal-verify` and the release build report) are in `VERIFICATION.md`.

## License and credits

Both `LICENSE` and `LICENSE.txt` are retained from the supplied source archive. They contain different license texts (MIT and LGPL v3), so they must not be treated as interchangeable. Preserve upstream notices and verify the applicable license for each reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution should remain with the relevant code.

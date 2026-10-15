# Sky VeLor — Beta-0.10.6v

Sky VeLor is a client-side Forge mod with a configurable HUD, settings hub, and gameplay utilities for Minecraft 1.8.9.

## Features

- Settings hub and HUD editor with dark and light themes.
- Spirit Leap teammate picker, class labels, skin heads, and isolated preview.
- Dungeon mob highlights, Floor VII overlays, and dragon alerts.
- Fishing and Slayer trackers, plus quick notes and client commands.

## Requirements

- Minecraft 1.8.9 with Forge 11.15.1.2318.
- Java 8 for the desktop runtime; JDK 8 to build from source.

## Installation

1. Start Minecraft 1.8.9 using the required Forge version once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.6v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Build from source

With JDK 8 installed and access to the configured Forge/Maven repositories, run:

```sh
./gradlew build dumpTestClasspath
TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
```

The JAR is written to `build/libs/`.

## Compatibility and limitations

The project targets desktop Java 8. Android/Pojav compatibility is not verified. Automated tests do not launch Minecraft or verify live-server behavior; in-game rendering and gameplay should be tested in Minecraft 1.8.9.

## License and credits

See `LICENSE` and `LICENSE.txt` for the repository's license files. Parts of the project were ported from RNG Carried Bald; attribution is retained from the earlier project README.

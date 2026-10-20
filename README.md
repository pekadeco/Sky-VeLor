# Sky VeLor — Beta-0.10.7v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, and first-person held-item customization.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.7v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

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

The distributable JAR is `build/libs/SkyVeLor-1.8.9-<version>.jar`. `build` runs `compileJava`,
`jar`, `reobfJar`, `sourceJar`, `assemble` and `check` in that order; `reobfJar` is the step that
rewrites the mod's Minecraft references from development (MCP) names to the SRG names a real Forge
runtime uses, and it is what makes the jar in `build/libs/` shippable. Never ship anything from
`build/tmp/` — in particular `build/tmp/deobfMcMCP/deobfed.jar` is ForgeGradle's deobfuscated
Minecraft, about 11 MB, and contains no Sky VeLor class at all. A previous build was published from
that file by mistake, which is why nothing it claimed to add worked.

Confirm the artifact before publishing:

```sh
python3 tools/verify_release_jar.py build/libs/SkyVeLor-1.8.9-Beta-0.10.7v.jar --version Beta-0.10.7v
```

It checks the version metadata, that every expected mod class is present, that no unrelated project
file or nested archive was packaged, and — decisively — that the classes reference Minecraft members
by SRG name with no development names left. The generated `build/` and `.gradle/` directories are
temporary and should not be included in source ZIPs.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.7v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## License and credits

Both `LICENSE` and `LICENSE.txt` are retained from the supplied source archive. They contain different license texts (MIT and LGPL v3), so they must not be treated as interchangeable. Preserve upstream notices and verify the applicable license for each reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution should remain with the relevant code.

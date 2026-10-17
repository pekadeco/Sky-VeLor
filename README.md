# Sky VeLor — Beta-0.10.7.7v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, and Item Animation for the first-person held item.

## Features

- Settings hub and movable HUD editor with dark and light themes.
- Smooth, frame-rate-independent UI motion: soft hover and press feedback, animated toggles and sliders, a sliding segmented selector, a gently appearing dropdown, and responsive search-field focus.
- Rounded animated buttons in the main/pause menu and Quick Note screen, with distinct primary, secondary and destructive styles.
- Spirit Leap teammate picker, class labels, player heads and isolated preview.
- A passive Floor VII / M7 Click in order helper: it parses the live chest and outlines only the next real slot. It never replaces the chest, clicks, blocks input, or changes inventory behavior.
- Dungeon map with room/puzzle templates, party labels, plus mob highlights, Floor VII overlays and dragon alerts.
- Fishing and Slayer trackers, plus quick notes and client commands.
- Item Animation: live first-person X/Y/Z position, size and equip-animation controls, exact-value entry, clamped persistence and reset actions.

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
python3 tools/verify_release_jar.py build/libs/SkyVeLor-1.8.9-Beta-0.10.7.7v.jar --version Beta-0.10.7.7v
```

It checks the version metadata, that every expected mod class is present, that no unrelated project
file or nested archive was packaged, and — decisively — that the classes reference Minecraft members
by SRG name with no development names left. The generated `build/` and `.gradle/` directories are
temporary and should not be included in source ZIPs.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.7.7v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Item Animation — Renderer

**Item Animation** lives exclusively in the HUD editor as a clearly labelled, non-draggable renderer panel.
It provides live X/Y/Z offsets (stored in hundredths of a render unit, clamped to -75 through 75), Size and
Animation Speed (50–200%). Every slider has a visible numeric value and a nearby exact-value field; Enter
applies and saves a typed value. **Reset Item Animation** restores X/Y/Z to 0 and Size/Speed to 100%.
Legacy `firstPerson*` config keys are read once and atomically migrated to the `itemAnimation.*` category.

- **Position and size** wrap Minecraft's actual `ItemRenderer.renderItemInFirstPerson` call. The hook reads
  live values every hand frame and applies only that first-person matrix, so swords, tools, blocks, bows,
  maps and the bare arm use the same visual adjustment. Third-person items, world rendering and overlays are
  left on the vanilla path.
- **Animation Speed** changes only the first-person equipped-progress interpolation calculated by
  `ItemRenderer.updateEquippedItem`. It neither sends packets nor changes attack speed, item use, block
  breaking, server timing or other gameplay state. The adjusted progress is bounded to vanilla's 0–1 range to
  prevent high-speed overshoot and visual jitter.

The release registers a minimal Forge 1.8.9 coremod through the JAR manifest. It injects balanced callbacks
around the authoritative hand call and its equipped-progress update instead of replacing `ItemRenderer` or
reflectively writing final renderer fields. `/velor itemdebug` prints the same live values used by those callbacks
and reports whether the real hand/equip hook has been reached.

## Compatibility and testing limits

The project targets desktop Java 8. Android/Pojav compatibility is a goal but is not verified; Item Animation uses Forge 1.8.9 bytecode hooks rather than reflection, and still needs a real-client compatibility pass.

Automated tests cover logic, settings and configuration. They do not launch Minecraft, so they cannot prove what the held item looks like on screen. The release jar is verified structurally with `tools/verify_release_jar.py`, which confirms the artifact is the mod and not a ForgeGradle intermediate, that it carries the right version metadata, and that it was really reobfuscated to SRG names. Rendering and gameplay still need a human pass in Minecraft 1.8.9 with Forge before a release is published.

## Click in order terminal overlay

For a real Floor VII/M7 Click in order chest, the helper reads the live lower-chest slots, determines the
smallest unclicked displayed order, and draws one translucent outline over that slot. Duplicate, empty or
unparseable candidates leave the GUI untouched; player inventory, normal chest clicks and all container
behavior remain vanilla.

The old path replaced the server chest with a synthetic terminal screen, so it could desynchronise from the real slot state. The new path only observes and draws after the real screen, so a parsing failure has no gameplay effect.

## License and credits

Both `LICENSE` and `LICENSE.txt` are retained from the supplied source archive. They contain different license texts (MIT and LGPL v3), so they must not be treated as interchangeable. Preserve upstream notices and verify the applicable license for each reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution should remain with the relevant code. The dungeon-map-derived files retain the Better NEF LGPL notice in `THIRD_PARTY_NOTICES/Better-NEF-NOTICE.md`.

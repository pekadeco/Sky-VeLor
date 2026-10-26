# Sky VeLor — Beta-0.10.9.1.1v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD,
dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, Chat QoL,
Item Lock / Swap, and Item Animation for Minecraft's first-person hand renderer.

## Beta-0.10.9.1.1v — feature-oriented GUI refresh

- Adds a visible **SkyVeLor** wordmark at the upper-left of the settings header. The title bar still has
  exactly two 24 px actions: **Fullscreen** and **Close**; drag, Escape, tooltips, hover/press feedback,
  fullscreen persistence, and the existing close behavior remain intact.
- Renames only the visible eight-route rail to **Home → Player → Dungeons → Farming → Mining → Fishing →
  HUD & Visuals → Misc**. Existing enum identities, routes, feature ownership, search, and configuration
  keys remain unchanged; Farming remains a no-feature destination.
- Refines the shared flat value dropdown: clearer trigger/popup contrast, hover feedback, selected-row accent
  treatment and check mark, readable dividers, and adaptive placement/flow reservation without nesting choices
  or changing feature-value bindings.
- Adds explicit **Custom GUI Transparency** and **GUI Transparency** controls under **Misc → Appearance**.
  They retain `glassOpacityOn` / `glassOpacity` ownership and legacy loading; the custom glass range is now
  45%–100%, where lower opacity shows more of the game behind the GUI.
- The flat settings contract remains unchanged: fixed chevron/toggle columns, one-control inline rows,
  two-or-more-choice dropdowns, one-level searchable accordion disclosure, readable master-off feedback,
  reset links, and only the existing HUD Editor and Item Animation editor routes.
- `tools/apple-layout-renderer/` mirrors the left-aligned wordmark and renders headless Java2D Home, Dungeons,
  and GUI-scale-4 PNG inspection previews from the real layout/catalog classes; push CI stores them as
  short-lived review evidence.

The full presentation inventory, retained routes, old-config boundary, and test boundary are in
[`docs/FEATURE-SETTINGS-EDITOR.md`](docs/FEATURE-SETTINGS-EDITOR.md).

> **NOT tested in-game.** Automated build, test, renderer, and archive verification do not start
> Minecraft or validate visual/input behavior in a real 1.8.9 Forge client.

## Features

- Native settings shell and movable HUD editor with an Apple-inspired graphite-glass visual system and blue accent.
- Dungeon map with room/puzzle templates, party labels, cached Mob/Door ESP, Floor VII overlays,
  dragon alerts, Spirit Leap helpers, and an optional read-only Click in Order helper.
- Item Animation: live first-person X/Y/Z, scale, hit/slash controls, bounded persistence,
  reset actions, and a renderer-only Preview Swing.
- Item Lock and Item Swap: persistent player-inventory locks, safe paired-slot swaps, configurable
  badges/cooldown/lines, and packet-before-send protection for GUI clicks and Q/Ctrl+Q drops.
- Chat QoL: retained-message history, rounded message background, captured context menu, scrolling,
  and attached All/Party/Reply/custom filter tabs without split chat windows.
- Existing mining, inventory, visual-safety, movement, terminal, boss-HUD, waypoint, fishing,
  Slayer, and privacy-conscious Dungeon Sync helpers. Input/gameplay assists remain explicit opt-ins.

## Requirements

- Minecraft 1.8.9 with Forge `11.15.1.2318`.
- JDK 8 for compiling and Java 8 for the legacy Forge runtime.
- Internet access on the first build so Gradle and ForgeGradle can download dependencies.

This project deliberately retains ForgeGradle 2.1 / Gradle 2.14.1. Newer JDKs and Gradle releases
are not drop-in replacements for this legacy toolchain.

## Build

### Windows

```bat
gradlew.bat clean build dumpTestClasspath
set /p TEST_CP=<build\test-classpath.txt
tests\run-tests.bat
```

### Linux or Git Bash

```sh
./gradlew clean build dumpTestClasspath
TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
```

The distributable JAR is:

```text
build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.1v.jar
```

`reobfJar` is essential: it rewrites Minecraft development (MCP) references to the SRG names a
Forge runtime needs. Never distribute `build/tmp/deobfMcMCP/deobfed.jar`; it is a ForgeGradle
intermediate rather than Sky VeLor.

Verify the JAR before publishing:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.1v.jar \
  --version Beta-0.10.9.1.1v --mcmod-version 0.10.9.1.1
```

The verifier confirms version metadata, expected/retired class boundaries, reobfuscation, runtime
owners, and archive hygiene. The source archive is generated from a curated `git archive` list:

```sh
python3 tools/verify_source_zip.py \
  SkyVeLor-1.8.9-Beta-0.10.9.1.1v-source.zip \
  --version Beta-0.10.9.1.1v
```

A release delivery contains exactly three files: the JAR, a delivery ZIP containing the JAR and build
report, and the clean source ZIP. Generated `build/`, `.gradle/`, prior delivery files, Git state,
and unrelated archives do not belong in the source ZIP.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.9.1.1v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Settings routes

- **Player → Inventory and Chat → Item Lock and Swap** contains the existing key-binding guidance,
  cooldown, badge, line, clear, and reset controls. Configure the key in Minecraft **Controls → Sky
  VeLor**; no controls screen is launched from the settings shell.
- **Dungeons → Run Tools → Terminal Helper** has independent opt-in controls; there is no synthetic
  master switch. Auto click stays off by default.
- **HUD & Visuals → HUD → HUD Editor** opens the allowed editor for HUD and map placement.
- **HUD & Visuals → Item Animation → Edit** opens the standalone left-edge renderer panel. Its live
  values affect only Minecraft's first-person hand path; it neither sends packets nor changes attack,
  mining, server timing, or movement.
- **Misc → Appearance** owns the optional custom GUI-transparency toggle and opacity slider; disabling
  the custom toggle keeps the established default glass value without deleting the saved slider choice.

## Compatibility and testing limits

The project targets desktop Java 8. Android/Pojav compatibility is a goal but is not verified.
Automated tests cover logic, settings, old-property compatibility, catalog routing, disclosure state,
layout contracts, and release archives. They do not launch Minecraft. A human Minecraft 1.8.9 Forge
pass is still needed before claiming runtime validation; this release is **NOT tested in-game**.

## License and credits

`LICENSE` records the project license. Verify the applicable license for every reused component before
redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution remains
with the relevant code.

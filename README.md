# Sky VeLor — Beta-0.10.8.9.7v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD,
dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, Chat QoL,
Item Lock / Swap, and Item Animation for Minecraft's first-person hand renderer.

## Beta-0.10.8.9.7v — inline settings release

- Replaced retired feature-specific settings screens with collapsed, persisted inline dropdowns.
  Existing configuration owners and keys remain authoritative; presentation disclosure is the only
  new state and is saved under `ui.feature.dropdown.<id>`.
- Sidebar order is **Home**, **Gameplay**, **Dungeon**, **Farming**, **Mining**, **Fishing**,
  **Visuals**, **System**. Mining controls moved to Mining and Fishing controls moved to Fishing
  without changing their keys. Farming honestly states **No farming features yet**.
- Search finds nested settings and opens each parent dropdown. Feature masters use the standard
  small right-aligned toggle pill, while disabled masters leave their settings editable but dimmed.
- Chat QoL is grouped into **Background**, **Messages**, **Tabs**, **Menu and Highlight**, and
  **Reset**. Custom tabs remain editable inline through the original `ChatQolSettings` owner.
- Dungeon retains the map, no-master Terminal Helper, Spirit Leap, Boss HUD, M7 controls, compact
  Mob/Door ESP tables, and Dungeon Sync. Item Lock / Swap, mining, fishing, Auto Sprint, and Hurt
  Camera remain direct owner bindings.
- Only **HUD Editor** and **Item Animation Editor** open a settings screen. The existing HUD Editor
  still owns HUD/map placement; Item Animation retains its real first-person hand path and
  **Preview Swing**, without a held-item sample card.
- Removed obsolete feature-editor/sample surfaces and their focused models. No feature runtime or
  configuration key was intentionally replaced.
- The compact Safe-by-default notice is one line, the fullscreen icon uses corner brackets, and
  Sky VeLor does not draw external launcher MOUSE/GUI indicators.

The full old-to-new feature table, channel-move notes, and test boundary are in
[`docs/FEATURE-SETTINGS-EDITOR.md`](docs/FEATURE-SETTINGS-EDITOR.md).

> **NOT tested in-game.** Automated build, test, and archive verification are required, but they do
> not start Minecraft or validate visual/input behavior in a real 1.8.9 Forge client.

## Features

- Native settings shell and movable HUD editor with a neutral-dark visual system and blue accent.
- Dungeon map with room/puzzle templates, party labels, cached Mob/Door ESP, Floor VII overlays,
  dragon alerts, Spirit Leap helpers, and an optional read-only Click in Order helper.
- Item Animation: live first-person X/Y/Z, size, equip-animation controls, exact-value entry,
  bounded persistence, reset actions, and a renderer-only Preview Swing.
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
build/libs/SkyVeLor-1.8.9-Beta-0.10.8.9.7v.jar
```

`reobfJar` is essential: it rewrites Minecraft development (MCP) references to the SRG names a
Forge runtime needs. Never distribute `build/tmp/deobfMcMCP/deobfed.jar`; it is a ForgeGradle
intermediate rather than Sky VeLor.

Verify the JAR before publishing:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.8.9.7v.jar \
  --version Beta-0.10.8.9.7v --mcmod-version 0.10.8.9.7
```

The verifier confirms version metadata, expected/retired class boundaries, reobfuscation, runtime
owners, and archive hygiene. The source archive is generated from a curated `git archive` list:

```sh
python3 tools/verify_source_zip.py \
  SkyVeLor-1.8.9-Beta-0.10.8.9.7v-source.zip \
  --version Beta-0.10.8.9.7v
```

A release delivery contains exactly three files: the JAR, a delivery ZIP containing the JAR and build
report, and the clean source ZIP. Generated `build/`, `.gradle/`, prior delivery files, Git state,
and unrelated archives do not belong in the source ZIP.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.8.9.7v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Settings routes

- **Gameplay → Inventory and Chat → Item Lock and Swap** contains the existing key-binding guidance,
  cooldown, badge, line, clear, and reset controls. Configure the key in Minecraft **Controls → Sky
  VeLor**; no controls screen is launched from the settings shell.
- **Dungeon → Run Tools → Terminal Helper** has independent opt-in controls; there is no synthetic
  master switch. Auto click stays off by default.
- **Visuals → HUD → HUD Editor** opens the allowed editor for HUD and map placement.
- **Visuals → Item Animation → Item Animation Editor** opens the allowed renderer editor. Its live
  values affect only Minecraft's first-person hand path; it neither sends packets nor changes attack,
  mining, server timing, or movement.

## Compatibility and testing limits

The project targets desktop Java 8. Android/Pojav compatibility is a goal but is not verified.
Automated tests cover logic, settings, old-property compatibility, catalog routing, disclosure state,
layout contracts, and release archives. They do not launch Minecraft. A human Minecraft 1.8.9 Forge
pass is still needed before claiming runtime validation; this release is **NOT tested in-game**.

## License and credits

`LICENSE` records the project license. Verify the applicable license for every reused component before
redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution remains
with the relevant code.

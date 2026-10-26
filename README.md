# Sky VeLor — Beta-0.10.8.9.8v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD,
dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, Chat QoL,
Item Lock / Swap, and Item Animation for Minecraft's first-person hand renderer.

## Beta-0.10.8.9.8v — flat settings and standalone Item Animation

- Settings now use compact plain section labels and four player-facing row patterns: fixed-column
  toggle rows, one-control inline rows, single-level feature dropdowns, and distinct **[Edit]** rows.
  Master toggles keep the same `38 × 20` size; every normal row reserves fixed chevron-then-toggle
  columns, while editor rows use **[Edit]** and never use a chevron.
- Each tab keeps one accordion disclosure open at a time. Search opens the direct owner, expanded
  panels animate over 150 ms, use a lighter clipped inner surface with 8 px bottom padding, and
  leave master-off options visible but dimmed. Existing values and configuration keys are unchanged.
- Chat QoL is one flat owner with **Background**, **Messages**, **Tabs**, and **Click Menu** captions.
  Mob ESP, Door ESP, M7, map, lock/swap, mining, fishing, Slayer, movement, and all other owners
  are likewise limited to one visible disclosure level.
- Every expanded feature ends with one compact **Reset to defaults** link; reset rows and reset
  dropdowns were removed without removing their existing reset behavior or option inventory.
- Hurt Camera Shake is one inline slider. Spirit Leap Picker has its master and key option inside its
  dropdown. Terminal Helper has no master and displays its live `n / 5 on` count. Boss Health HUD,
  HUD Editor, and Item Animation use distinct Edit rows.
- Item Animation now opens a standalone left-edge panel (about 28% width) with a Close X, hook line,
  requested renderer controls, **Preview Swing**, and **Reset to Defaults**. It draws no HUD canvas,
  editor bar, world overlay, or retired Item Animation background/transparency/opacity control.
  Legacy background keys are ignored and removed on the next normal save.
- The Item Animation hook line reads **Hook: active** when available (green state), or
  **Hook: failed (reason)** when unavailable. The standard hook line does not expose transformer or renderer internals.

The full presentation inventory, text migration table, channel-move notes, and test boundary are in
[`docs/FEATURE-SETTINGS-EDITOR.md`](docs/FEATURE-SETTINGS-EDITOR.md).

> **NOT tested in-game.** Automated build, test, and archive verification are required, but they do
> not start Minecraft or validate visual/input behavior in a real 1.8.9 Forge client.

## Features

- Native settings shell and movable HUD editor with a neutral-dark visual system and blue accent.
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
build/libs/SkyVeLor-1.8.9-Beta-0.10.8.9.8v.jar
```

`reobfJar` is essential: it rewrites Minecraft development (MCP) references to the SRG names a
Forge runtime needs. Never distribute `build/tmp/deobfMcMCP/deobfed.jar`; it is a ForgeGradle
intermediate rather than Sky VeLor.

Verify the JAR before publishing:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.8.9.8v.jar \
  --version Beta-0.10.8.9.8v --mcmod-version 0.10.8.9.8
```

The verifier confirms version metadata, expected/retired class boundaries, reobfuscation, runtime
owners, and archive hygiene. The source archive is generated from a curated `git archive` list:

```sh
python3 tools/verify_source_zip.py \
  SkyVeLor-1.8.9-Beta-0.10.8.9.8v-source.zip \
  --version Beta-0.10.8.9.8v
```

A release delivery contains exactly three files: the JAR, a delivery ZIP containing the JAR and build
report, and the clean source ZIP. Generated `build/`, `.gradle/`, prior delivery files, Git state,
and unrelated archives do not belong in the source ZIP.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.8.9.8v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Settings routes

- **Gameplay → Inventory and Chat → Item Lock and Swap** contains the existing key-binding guidance,
  cooldown, badge, line, clear, and reset controls. Configure the key in Minecraft **Controls → Sky
  VeLor**; no controls screen is launched from the settings shell.
- **Dungeon → Run Tools → Terminal Helper** has independent opt-in controls; there is no synthetic
  master switch. Auto click stays off by default.
- **Visuals → HUD → HUD Editor** opens the allowed editor for HUD and map placement.
- **Visuals → Item Animation → Edit** opens the standalone left-edge renderer panel. Its live
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

# Sky VeLor — Beta-0.10.9.1.4v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD,
dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, Chat QoL,
Item Lock / Swap, and Item Animation for Minecraft's first-person hand renderer.

## Beta-0.10.9.1.4v — safer Dungeon ESP and smooth Aspect visuals

- Excludes the local player by identity during the Dungeon ESP scan and again in the live render pass. This prevents
  a player-owned mob-style outline/hitbox while retaining valid player-shaped dungeon NPC targets.
- Makes direct Enderman/Fel head markers conservative: a shared skull texture now needs its own star or a nearby
  resolved Starred/Special tag, and a marker at least two blocks above the player is suppressed. The fixed marker
  proxy is refined to a head-focused `0.68 × 1.60` blocks.
- Adds **Player → Comfort → Smooth Aspect Teleport**, enabled by default. After an already-completed local
  Aspect of the End/Void position jump, it uses a short `220 ms` dark-blue visual ease veil to soften the abrupt
  view change. It only observes local state: it does not initiate teleports, write movement/camera coordinates,
  alter input, or send packets.
- Keeps Minecraft 1.8.9 Forge compatibility, safe old-config loading, existing unrelated settings/features, the
  `SkyVeLor` wordmark, eight-route navigation, dropdown behavior, optional GUI transparency, fullscreen action,
  and the HUD/Item Animation editors.


The full presentation inventory, retained routes, old-config boundary, and test boundary are in
[`docs/FEATURE-SETTINGS-EDITOR.md`](docs/FEATURE-SETTINGS-EDITOR.md).

> Player screenshots supplied the settings-shell blur/corner and sleeping-Fel outline evidence. Automated build, test,
> renderer, and archive verification still do not start Minecraft, so the corrected behavior needs a fresh visual/input
> pass in a real 1.8.9 Forge client.

## Features

- Native settings shell and movable HUD editor with an Apple-inspired graphite-glass visual system and blue accent.
- Dungeon map with room/puzzle templates, party labels, cached Starred Mobs/Door ESP, Floor VII overlays,
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
build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.4v.jar
```

`reobfJar` is essential: it rewrites Minecraft development (MCP) references to the SRG names a
Forge runtime needs. Never distribute `build/tmp/deobfMcMCP/deobfed.jar`; it is a ForgeGradle
intermediate rather than Sky VeLor.

Verify the JAR before publishing:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.4v.jar \
  --version Beta-0.10.9.1.4v --mcmod-version 0.10.9.1.4
```

The verifier confirms version metadata, expected/retired class boundaries, reobfuscation, runtime
owners, and archive hygiene. The source archive is generated from a curated `git archive` list:

```sh
python3 tools/verify_source_zip.py \
  SkyVeLor-1.8.9-Beta-0.10.9.1.4v-source.zip \
  --version Beta-0.10.9.1.4v
```

A release delivery contains exactly three files: the JAR, a delivery ZIP containing the JAR and build
report, and the clean source ZIP. Generated `build/`, `.gradle/`, prior delivery files, Git state,
and unrelated archives do not belong in the source ZIP.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.9.1.4v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Settings routes

- **Player → Inventory and Chat → Item Lock and Swap** contains the existing key-binding guidance,
  cooldown, badge, line, clear, and reset controls. Configure the key in Minecraft **Controls → Sky
  VeLor**; no controls screen is launched from the settings shell.
- **Dungeons → Run Tools → Terminal Helper** has independent opt-in controls; there is no synthetic
  master switch. Auto click stays off by default.
- **Dungeons → Dungeon ESP → Starred Mobs** owns Starred Mob, Miniboss, Special Starred Mob, and Enderman styles.
  Skeletor uses the normal Starred Mob style when a nearby star tag selects it; player-shaped dungeon NPCs remain
  eligible, while the local player is excluded in both scan and render paths. Direct Enderman/Fel head markers need
  star evidence and are suppressed at least two blocks above the player.
- **Player → Comfort → Hide Dead Mobs** defaults on and removes a Dungeon ESP target during its death animation.
- **Player → Comfort → Smooth Aspect Teleport** defaults on and applies only a short render-only visual ease after an
  already-observed Aspect of the End/Void jump. It never initiates movement, changes camera/player coordinates, or sends packets.
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

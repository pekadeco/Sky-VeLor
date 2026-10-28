# Sky VeLor — Beta-0.10.9.1.9v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD,
dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, Chat QoL,
Item Lock / Swap, Shortcut, and Item Animation for Minecraft's first-person hand renderer.

## Beta-0.10.9.1.9v — Shortcut and key-capture polish

- **Item Lock And Swap Key** now visibly shows its binding and supports full-row click-to-capture: press a key to set
  it, Escape to cancel, or Backspace to clear it to `NONE`. A cleared binding safely disables the physical L gesture.
- Replaces the visible **Keybind Cut** dropdown with a standalone **Shortcut** card beside **Inventory And Chat**.
  Existing `assist.keybindCut.*`, older movement/shortcut aliases, and Command Keybind records migrate to the new
  `assist.shortcut.*` namespace without resetting a saved choice.
- Adds a visible **Wardrobe Shortcut** enable toggle and key capsule with the same capture/cancel/clear interaction.
  The saved Minecraft Controls binding is preserved when no new feature override exists.
- Wardrobe uses only direct physical player input: its key can send `/wardrobe` from normal gameplay, and a number
  key `1`–`9` makes one ordinary selection of slots `36`–`44` only in a recognized Wardrobe `GuiChest`. It cannot act
  in chat or another unrelated GUI, repeat while held, move a player, or send packets directly.
- Reworks **Command Keybind** into compact one-line entries: key, slash-command field, On/Off control, and a fully
  visible Delete button. The panel grows only to its capped visible rows; **Add** and **Reset** remain at the bottom.
- Command fields retain ordinary typed characters and inner spaces such as `/warp hub`; outer whitespace is normalized
  only when focus leaves or the editor closes, and a focused field consumes Space before general GUI/keybind handling.
- Documents the behavioral-only NotEnoughFakepixel Wardrobe reference and its LGPL-3.0-or-later license in
  [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md). No source, dependency, or asset was copied.
- Keeps Minecraft 1.8.9 Forge compatibility, old-config loading, Item Lock multi-pair legality, the existing GUI/HUD
  Editor, Item Animation, Chat QoL, Dungeon ESP, Party Sync default-off behavior, Home metrics, and fail-closed hooks.


The full presentation inventory, retained routes, old-config boundary, and test boundary are in
[`docs/FEATURE-SETTINGS-EDITOR.md`](docs/FEATURE-SETTINGS-EDITOR.md).

> Player screenshots supplied the settings-shell blur/corner and sleeping-Fel outline evidence. Automated build, test,
> renderer, and archive verification still do not start Minecraft, so the corrected behavior needs a fresh visual/input
> pass in a real 1.8.9 Forge client.

## Features

- Native settings shell and movable HUD editor with an Apple-inspired graphite-glass visual system and blue accent.
- Dungeon map with room/puzzle templates, party labels, cached Starred Mobs/Door ESP, Floor VII overlays,
  dragon alerts, Spirit Leap helpers, an optional read-only Click in Order helper, and opt-in local Spirit Bow/
  sidebar-readout displays.
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
build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.9v.jar
```

`reobfJar` is essential: it rewrites Minecraft development (MCP) references to the SRG names a
Forge runtime needs. Never distribute `build/tmp/deobfMcMCP/deobfed.jar`; it is a ForgeGradle
intermediate rather than Sky VeLor.

Verify the JAR before publishing:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.9v.jar \
  --version Beta-0.10.9.1.9v --mcmod-version 0.10.9.1.9
```

The verifier confirms version metadata, expected/retired class boundaries, reobfuscation, runtime
owners, and archive hygiene. The source archive is generated from a curated `git archive` list:

```sh
python3 tools/verify_source_zip.py \
  SkyVeLor-1.8.9-Beta-0.10.9.1.9v-source.zip \
  --version Beta-0.10.9.1.9v
```

A release delivery contains exactly three files: the JAR, a delivery ZIP containing the JAR and build
report, and the clean source ZIP. Generated `build/`, `.gradle/`, prior delivery files, Git state,
and unrelated archives do not belong in the source ZIP. In particular, the root reference archives
`Sky-VeLor-1.0.0.zip` and `AppleGUI-1.8.9 (1).zip` are explicitly excluded and the verifier checks both names.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.9.1.9v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Settings routes

- **QoL → Inventory And Chat → Item Lock And Swap** contains a visible current-key capsule, cooldown, badge,
  line, clear, and reset controls. Click its key row to capture a new key; Escape cancels and Backspace clears to
  `NONE`. A new pair must include a hotbar slot; illegal legacy saved pairs are dropped safely. Shared endpoints
  retain every legal pair and completed manual swaps cycle those targets in creation order.
- **QoL → Shortcut** is a standalone card for Wardrobe, Pets, and Command Keybind. Existing Keybind Cut and older
  shortcut settings load compatibly and save under `assist.shortcut.*`. **Wardrobe Shortcut** has an enable toggle and
  visible click-capture key; it sends `/wardrobe` only from normal gameplay and maps `1`–`9` to slots `36`–`44` only
  inside a recognized Wardrobe menu. **Command Keybind** has one master toggle and **Open Command Keybind Editor**;
  slash commands use direct physical presses while no chat, GUI, or editor is open and visibly warn about collisions.
- **Dungeons → Run Tools → Terminal Helper** has independent opt-in controls; there is no synthetic
  master switch. Auto click stays off by default.
- **Dungeons → Run Tools → Spirit Bow Locator** defaults off and uses
  `assist.dungeon.spiritBowLocator`. It only draws a client-side locator for the nearest nearby named marker in a
  confirmed dungeon; it has no keybind, command, click, movement, packet, or controller path.
- **Dungeons → Dungeon ESP → Starred Mobs** owns Starred Mob, Miniboss, Special Starred Mob, and Enderman styles.
  Skeletor uses the normal Starred Mob style when a nearby star tag selects it; player-shaped dungeon NPCs remain
  eligible, while the local player is excluded in both scan and render paths. Recognized Enderman/Fel skull markers
  remain direct Enderman targets without a separate tag or height gate.
- **QoL → Comfort → Hide Dead Mobs** defaults on and client-side suppresses dead/dying normal mobs and server NPC
  fake mobs—including their standard nameplates—when `isDead`, `deathTime > 0`, or zero health is observed. It keeps
  the local player and ordinary listed remote players visible, and never removes an entity or changes server state.
- **QoL → Comfort → Smooth Aspect Teleport** defaults on and arms only after a local Aspect use. At RenderTick START,
  after vanilla accepts a plausible completed jump, it eases only the rendered world/camera origin for `125 ms`; it
  never initiates movement, changes player/camera entity coordinates, or sends packets.
- **Slayer → Slayer Tracker** owns the existing Slayer tracker route. The visible channel changed, not the tracker
  configuration keys or values.
- **HUD & Visuals → HUD → Dungeon Run HUD** defaults off and is movable through **Open HUD Editor**. It reads only the
  visible dungeon sidebar at a bounded interval and displays observed floor, elapsed time, cleared percentage, secrets,
  and deaths; it does not estimate score, inspect the tab list, react to chat, or close secret chests.
- **HUD & Visuals → HUD → Open HUD Editor** opens the allowed editor for HUD and map placement.
- **HUD & Visuals → Item Animation → Open Item Animation Editor** opens the standalone left-edge renderer panel. Its live
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
with the relevant code. The independently implemented Wardrobe behavior reference and its LGPL-3.0-or-later
attribution are recorded in [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md).

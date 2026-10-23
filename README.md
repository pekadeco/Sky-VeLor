# Sky VeLor — Beta-0.10.8.8.1v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, and Item Animation for the first-person held item.

## Features

- Settings hub and movable HUD editor with dark and light themes.
- Smooth, frame-rate-independent UI motion: soft hover and press feedback, animated toggles and sliders, a sliding segmented selector, a gently appearing dropdown, and responsive search-field focus.
- Rounded animated buttons in the main/pause menu and Quick Note screen, with distinct primary, secondary and destructive styles.
- Spirit Leap teammate picker, class labels, player heads and isolated preview.
- A passive Floor VII / M7 Click in order helper: it parses the live chest and outlines only the next real slot. It never replaces the chest, clicks, blocks input, or changes inventory behavior.
- Dungeon map with room/puzzle templates, party labels, cached starred-mob/miniboss Dungeon ESP, cached wither/blood-door ESP, Floor VII overlays and dragon alerts.
- Fishing and Slayer trackers, plus quick notes and client commands.
- Item Animation: live first-person X/Y/Z position, size and equip-animation controls, exact-value entry, clamped persistence and reset actions.
- Item Lock and Item Swap: persistent player-inventory position locks, safe paired-slot swaps, configurable badges and cooldown, plus packet-before-send protection for GUI clicks and Q/Ctrl+Q drops.
- Opt-in Default Chat QoL: sequence-ordered retained-message history, rounded message-only background, grouped hover/three-action captured menu, smooth scrolling, and attached open-chat All/Party/Reply/custom filter tabs without split chat windows.
- Optional mining, inventory, visual-safety, movement, terminal, boss-HUD, waypoint, and privacy-conscious Dungeon Sync assists; all gameplay-affecting controls are off by default.

## Beta-0.10.8.8.1 flat-Hub/editor release

- The Hub is now a flat list: a **Simple** row has one toggle, while a one-level **Dropdown** has its status dot, the existing On/Off toggle, and one correctly named editor button. Detailed Appearance, Performance, assist, Dungeon ESP, Door ESP, Terminal, M7, Slayer and Catch Counter options remain in their focused editors; existing HUD, Item Animation, Chat and Mod Settings editors remain their owners. This is a presentation-only reorganization.
- **Quality of Life** adds inventory search/filtering, potion-panel hiding, Roman tooltip levels, long-tooltip scrolling, rarity tints, local no-hurt camera, UUID-backed item protection, opt-in Auto Sprint, and unbound Wardrobe/Pets shortcuts.
- **Mining** adds a local pickaxe-ready alert, read-only Puzzler route marker, and a configurable Mining Assistant HUD. **Dungeons** adds a Boss HP HUD, guarded terminal highlights/dimming/wrong-click blocking, terminal/device waypoints, and optional auto-click only for an unambiguous live F7/M7 terminal result.
- **Dungeons → Dungeon Sync** is off by default. It shares only allow-listed puzzle labels and aligned coordinates through a TLS WebSocket after deriving a local SHA-256 session; names are hashing inputs only and are never serialized. Read [the privacy and safety note](docs/DUNGEON_SYNC.md) before enabling it.
- New Controls bindings under **Sky VeLor** are unbound by default: Toggle Item Protection, Wardrobe Shortcut, and Pets Shortcut. Inventory Search appears in compatible inventory screens when enabled; it has no default keybind. `/velor selftest` reports the new assist/core-hook and sync state.
- These aids are client-side convenience features, not a guarantee of server safety. Auto-click, automated sprint, external sync, and every in-game behavior require user review; this release was **NOT tested in-game**.

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
python3 tools/verify_release_jar.py build/libs/SkyVeLor-1.8.9-Beta-0.10.8.8.1v.jar --version Beta-0.10.8.8.1v --mcmod-version 0.10.8.8.1
```

It checks the version metadata, that every expected mod class is present, that no unrelated project
file or nested archive was packaged, and — decisively — that the classes reference Minecraft members
by SRG name with no development names left. The generated `build/` and `.gradle/` directories are
temporary and should not be included in source ZIPs.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.8.8.1v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Item Lock and Item Swap

Open **Quality of Life → Item Lock and Item Swap**. The feature is on by default and uses **L** by default (the
key can be rebound there or in Controls). Hover a player-inventory position **A**, press and hold the key, move the
pointer to **B**, then release the key — no mouse click is part of pairing. Releasing over an **unpaired** A toggles
that position lock at any hold duration; releasing over empty space cancels unchanged. Locks persist for hotbar `0–8`,
main inventory `9–35`, boots `36`, leggings `37`, chestplate `38`, and helmet `39`, including empty positions.

Release A over a different unlocked, unpaired player position to create one of up to eight pairs. Tapping **L** on
either endpoint of an existing pair removes that pair (and shows **Swap removed**) instead of locking it; dragging one
endpoint to its partner removes it too. Releasing a paired endpoint over any other player slot refuses with
**Slot is already paired**. Locked targets, already-paired targets, duplicate endpoints, and a ninth pair are refused
without mutation. While the key is held, container mouse clicks are swallowed, a 150 ms hint appears, and a live
A-to-cursor line plus green/red target ring make the decision visible. Left- or right-clicking a paired slot with an
empty cursor still swaps it with its partner through the normal client controller after both destinations have been
checked, including armor placement. The cooldown defaults to 2 seconds; an invalid armor placement reports
**Cannot swap: item does not fit**.

Locks and pairs block normal window-click modes, number-key sources/destinations, drag/collect routes, and selected
hotbar Q/Ctrl+Q drops before packets are sent. The configurable top-right padlock and numbered pair badges are
shape-rendered, can be tinted/translucent, and can remain visible on the hotbar. Persistent rounded pair-center lines
can be set to **Always**, **On hover**, or **Off** (default: Always), with independent tint, opacity, and 1–4 px width.
`/velor lockdebug` reports the live key, drag state/start/hover slots, and last decision; `/velor selftest` reports the
fail-closed hook status. If the window-click hook is unavailable, selection and manual swaps intentionally do nothing
rather than implying protection.

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
smallest unclicked displayed order, and draws one translucent outline over that slot. It can also parse a deliberately
small allow-list of terminal layouts to dim or block only known-wrong choices. Duplicate, empty or unparseable
candidates leave the GUI untouched; player inventory and ordinary containers remain vanilla.

**Terminal auto click is off by default.** Even when enabled, it requires terminal visuals, an F7/M7 context, a
recognised live chest, exactly one candidate, a changed chest fingerprint, and a cooldown. Any uncertainty returns to
no highlight, no block, and no click. The old path replaced the server chest with a synthetic terminal screen, so it
could desynchronise from the real slot state; the current path keeps the real server chest in place.

## License and credits

Both `LICENSE` and `LICENSE.txt` are retained from the supplied source archive. They contain different license texts (MIT and LGPL v3), so they must not be treated as interchangeable. Preserve upstream notices and verify the applicable license for each reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution should remain with the relevant code. The dungeon-map-derived files retain the Better NEF LGPL notice in `THIRD_PARTY_NOTICES/Better-NEF-NOTICE.md`.

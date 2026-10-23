# Sky VeLor — Beta-0.10.8.9.3v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, and Item Animation for the first-person held item.

## Features

- Settings hub and movable HUD editor with dark and light themes; click an on-screen HUD element to select it, including dimmed disabled placeholders.
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

## Beta-0.10.8.9.3v update — reference settings shell and Stats Display

- The native settings surface now ports the supplied `analyze-sky-velor-mod-code.zip` React/Vite design proposal without adding a browser or GUI-framework dependency. Its visible architecture is the exact five-destination rail: **Home**, **Gameplay**, **Dungeon**, **Visuals**, and **System**.
- The toolbar contains global search, an **Advanced** gate, and **Done**; the content area is one clipped/smoothed scroll viewport with compact bordered cards, inline child disclosure, switches, sliders, choices, focused-editor actions, responsive rail behavior, and Home presets/quick controls.
- Existing ownership is preserved: Gameplay contains Comfort, Inventory, Chat, Skills and Shortcuts; Dungeon contains Run tools, Floor VII, Highlights and Party sync; Visuals contains HUD and Item Animation; System contains Appearance, Performance, General, Diagnostics and About. Existing configuration keys, direct focused editors, Dungeon ESP, chat, inventory, HUD systems, and commands remain the runtime owners.
- Diagnostics retains read-only Frame Rate/Ping/Memory telemetry inside the settings screen only. The in-game **Stats Display** remains the one restored three-card **FRAME RATE**, **MINING**, and **MEMORY** renderer: dark beveled cards, cyan frame-rate/BPS values, white memory, 172 × 102 card proportions, 9 px gaps, 8 px cuts, and centered top placement. The middle metric is the existing mining tracker (`BPS`), not ping.
- Stats Display remains opt-in for a fresh configuration. If an older independently positioned FPS/Ping/Memory HUD was enabled, it migrates once to the consolidated display unless an explicit modern Stats choice already exists; retired metric keys are removed so no duplicate metrics renderer can draw.
- Dungeon ESP's 0.10.8.9.2 reliability/display improvements, Hurt Camera Shake, Player Dollars removal, Inventory Search removal, and Chat-tab close behavior remain unchanged.

This source update is **NOT tested in-game**. Build/distribution remains gated on the Java 8 Forge build and release verifiers below; generated artifacts are valid only when produced by that workflow.

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
python3 tools/verify_release_jar.py build/libs/SkyVeLor-1.8.9-Beta-0.10.8.9.3v.jar --version Beta-0.10.8.9.3v --mcmod-version 0.10.8.9.3
```

It checks the version metadata, that every expected mod class is present, that no unrelated project
file or nested archive was packaged, and — decisively — that the classes reference Minecraft members
by SRG name with no development names left. The generated `build/` and `.gradle/` directories are
temporary and should not be included in source ZIPs.

The release also publishes `SkyVeLor-1.8.9-Beta-0.10.8.9.3v-source.zip`. It is generated with a
curated `git archive` allow-list under the `SkyVeLor-1.8.9-Beta-0.10.8.9.3v-source/` root prefix;
use `python3 tools/verify_source_zip.py <archive> --version Beta-0.10.8.9.3v` to verify its contents.
The only JAR allowed in that source archive is the required Gradle wrapper.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.8.9.3v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## Item Lock and Item Swap

Open **Gameplay → Inventory → Item Lock and Swap → Open details**. The feature is on by default and uses **L** by default (the
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

## Item Animation Settings

**Item Animation** is reached from **Visuals → Item Animation**; its master switch has inline size/tempo/offset controls and **Open Item Animation** keeps the established, non-draggable HUD-editor renderer panel available.
It provides live X/Y/Z offsets (stored in hundredths of a render unit, clamped to -150 through 150), Size
(30–200%), and Animation Speed (25–400%). Every slider has a visible numeric value and a nearby exact-value field; Enter
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

`LICENSE` records the project license. Verify the applicable license for every reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution remains with the relevant code.

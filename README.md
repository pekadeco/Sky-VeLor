# Sky VeLor — Beta-0.10.7.6v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, and first-person held-item customization.

## Features

- Settings hub and movable HUD editor with dark and light themes.
- Smooth, frame-rate-independent UI motion: soft hover and press feedback, animated toggles and sliders, a sliding segmented selector, a gently appearing dropdown, and responsive search-field focus.
- Rounded animated buttons in the main/pause menu and Quick Note screen, with distinct primary, secondary and destructive styles.
- Spirit Leap teammate picker, class labels, player heads and isolated preview.
- A passive Floor VII / M7 Click in order helper: it parses the live chest and outlines only the next real slot. It never replaces the chest, clicks, blocks input, or changes inventory behavior.
- Dungeon map with room/puzzle templates, party labels, plus mob highlights, Floor VII overlays and dragon alerts.
- Fishing and Slayer trackers, plus quick notes and client commands.
- First-person held-item renderer customization: visual X/Y/Z position, size and animation-tempo controls, with clamped saved values and reset actions.

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
python3 tools/verify_release_jar.py build/libs/SkyVeLor-1.8.9-Beta-0.10.7.6v.jar --version Beta-0.10.7.6v
```

It checks the version metadata, that every expected mod class is present, that no unrelated project
file or nested archive was packaged, and — decisively — that the classes reference Minecraft members
by SRG name with no development names left. The generated `build/` and `.gradle/` directories are
temporary and should not be included in source ZIPs.

## Installation

1. Start Minecraft 1.8.9 with Forge `11.15.1.2318` once.
2. Put `SkyVeLor-1.8.9-Beta-0.10.7.6v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile.

## First-Person Item — Renderer

The **First-Person Item** target in the HUD editor is explicitly renderer configuration, not a screen-HUD
placement. Its size (50–200%), X/Y/Z visual position (-100–100%), and visual-only animation multiplier
(50–200%) update the renderer immediately and persist in `skyvelor.properties`; hand-edited values are
clamped while loading. **Reset held item** restores size to 100%, X/Y/Z to 0%, and animation to 100%.
Size and animation each retain their own switch; an off switch gives the exact vanilla 100% value.

- **Position and size** translate before uniformly scaling `ItemRenderer.renderItemInFirstPerson`, so the
  chosen X/Y/Z position stays predictable as size changes. Swords, tools, blocks, bows, maps and the bare
  arm share that visual draw call. Water, fire and pumpkin overlays, other players, and third-person items
  are deliberately unaffected.
- **Animation multiplier** changes only the local equip-transition tempo (the slide out/back in when a held
  item changes). It does not send a packet or modify gameplay state.

Deliberately **not** affected: arm swings, bow draw, eating/drinking, attack cooldowns, block breaking,
and any server-observable mechanic. No potion effect is applied and there is no Haste option.

Implementation note: Minecraft 1.8.9 keeps two references to the first-person renderer.
`EntityRenderer` captures one in its own `final` field during construction and every render and update
call goes through that field, while `Minecraft.itemRenderer` is only where the instance is created.
`FirstPersonItemSetup` installs the custom renderer into the `EntityRenderer` field that is actually
read, and points `Minecraft`'s field at the same instance so both agree. It locates those fields by
declared type rather than by name, because the release jar is reobfuscated to SRG names and a
development name such as `itemRenderer` does not exist at runtime.

## Compatibility and testing limits

The project targets desktop Java 8. Android/Pojav compatibility is a goal but is not verified; the renderer swap uses `java.lang.reflect` to clear a `final` modifier, which is a Java 8 technique and is not guaranteed on a newer runtime.

Automated tests cover logic, settings and configuration. They do not launch Minecraft, so they cannot prove what the held item looks like on screen. The release jar is verified structurally with `tools/verify_release_jar.py`, which confirms the artifact is the mod and not a ForgeGradle intermediate, that it carries the right version metadata, and that it was really reobfuscated to SRG names. Rendering and gameplay still need a human pass in Minecraft 1.8.9 with Forge before a release is published.

## Click in order terminal overlay

For a real Floor VII/M7 Click in order chest, the helper reads the live lower-chest slots, determines the
smallest unclicked displayed order, and draws one translucent outline over that slot. Duplicate, empty or
unparseable candidates leave the GUI untouched; player inventory, normal chest clicks and all container
behavior remain vanilla.

The old path replaced the server chest with a synthetic terminal screen, so it could desynchronise from the real slot state. The new path only observes and draws after the real screen, so a parsing failure has no gameplay effect.

## License and credits

Both `LICENSE` and `LICENSE.txt` are retained from the supplied source archive. They contain different license texts (MIT and LGPL v3), so they must not be treated as interchangeable. Preserve upstream notices and verify the applicable license for each reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution should remain with the relevant code. The dungeon-map-derived files retain the Better NEF LGPL notice in `THIRD_PARTY_NOTICES/Better-NEF-NOTICE.md`.

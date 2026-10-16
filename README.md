# Sky VeLor — Beta-0.10.7v

Sky VeLor is a client-side Forge mod for Minecraft 1.8.9. It provides a configurable HUD, a settings hub, dungeon utilities, Spirit Leap helpers, fishing and Slayer trackers, quick notes, and first-person held-item customization.

## Features

- Settings hub and movable HUD editor with dark and light themes.
- Smooth, frame-rate-independent UI motion: soft hover and press feedback, animated toggles and sliders, a sliding segmented selector, a gently appearing dropdown, and responsive search-field focus.
- Rounded animated buttons in the main/pause menu and Quick Note screen, with distinct primary, secondary and destructive styles.
- Spirit Leap teammate picker, class labels, player heads and isolated preview.
- Dungeon mob highlights, Floor VII overlays and dragon alerts.
- Fishing and Slayer trackers, plus quick notes and client commands.
- First-person held-item customization: item scale and equip-animation tempo sliders (50-200%), each with its own switch and a reset action.

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

## First-person held item

Both settings live under **First-person held item** in the settings hub, next to the appearance
controls. Each has a switch (off means exactly 100%, i.e. untouched vanilla) and a 50-200% slider in
10% steps. Values are read every frame, so a change is visible immediately, and
`AppleHubScreen.onGuiClosed()` saves them to `skyvelor.properties`. Loaded values are clamped to the
slider range, so a hand-edited or corrupt file cannot produce an unusable setting. **Reset held item**
puts both back to 100%.

- **Item scale** wraps the first-person draw call in a uniform scale. Swords, tools, blocks, bows,
  maps and the bare arm all scale together, because in 1.8.9 they share one call
  (`ItemRenderer.renderItemInFirstPerson`). At 100% no matrix operation is issued at all, so the
  default appearance is the vanilla one exactly. The water, fire and pumpkin overlay screens are a
  separate call and are deliberately not scaled, and other players and third person are unaffected.
- **Animation speed** changes the tempo of the equip transition: the slide out and back in when you
  change the held item, and the settle when you pick one up. It rescales the per-tick step of the
  vanilla interpolation and clamps it so the value cannot pass its target, so the motion stays smooth
  and monotonic with no jump or jitter.

Deliberately **not** affected by animation speed, because they are gameplay state rather than
renderer state:

- the arm swing, which comes from the player entity's swing progress;
- bow draw, eating and drinking, which are driven by the item-use counter;
- attack cooldowns, block breaking and anything else a server can observe.

Both settings are client-side and visual only. No potion effect is applied and there is no Haste
option; nothing about combat or movement mechanics changes.

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

## License and credits

Both `LICENSE` and `LICENSE.txt` are retained from the supplied source archive. They contain different license texts (MIT and LGPL v3), so they must not be treated as interchangeable. Preserve upstream notices and verify the applicable license for each reused component before redistributing. Parts of the project were ported from RNG Carried Bald; existing attribution should remain with the relevant code.

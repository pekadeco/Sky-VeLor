# Sky VeLor Beta-0.10.7v — build and verification record

This file records what was actually built, how it was verified, and what remains
untested. Every figure below comes from a real CI run or from inspecting the
produced artifact, not from reading the source and assuming it works.

## Artifact

| | |
|---|---|
| File | `dist/SkyVeLor-1.8.9-Beta-0.10.7v.jar` |
| Size | 286,928 bytes (280.2 KB) |
| SHA-256 | `a2f0027ee2014f47c94ce17f29a37387bd3ffded910b6285312a93a4e829ac54` |
| Mod classes | 109 under `com/arena/pekaclient/` |
| `mcmod.info` | `version: Beta-0.10.7v`, `mcversion: 1.8.9`, `modid: pekaclient` |
| Minecraft members referenced | 224 distinct SRG names, 0 development (MCP) names left |

## Build environment and task graph

Built by GitHub Actions, run
[38895463660](https://github.com/pekadeco/Sky-VeLor/actions/runs/38895463660),
from source revision `d1d6d38`.

- Java: `openjdk 1.8.0_504` (Temurin)
- Gradle: `2.14.1` (the wrapper version this ForgeGradle 2.1 project requires)
- ForgeGradle: `2.1-SNAPSHOT`, Forge `1.8.9-11.15.1.2318-1.8.9`, mappings `stable_22`
- Result: `BUILD SUCCESSFUL`

Tasks that actually executed, in order:

```
:compileJava :processResources :classes :jar :reobfJar
:extractRangemapReplacedMain :retromapReplacedMain :sourceJar
:assemble :check :build :dumpTestClasspath
```

`:reobfJar` ran, and it is the step that rewrites the mod's references to
Minecraft from development names to the SRG names a real Forge runtime uses. That
is what makes `build/libs/SkyVeLor-1.8.9-Beta-0.10.7v.jar` the distributable
artifact.

Every jar the build produced, for the record:

| Size | Path | What it is |
|---|---|---|
| 286,928 | `build/libs/SkyVeLor-1.8.9-Beta-0.10.7v.jar` | **the release artifact** |
| 169,559 | `build/libs/SkyVeLor-1.8.9-Beta-0.10.7v-sources.jar` | retromapped sources, not for `mods/` |
| 167,820 | `build/retromapping/retromappedReplacedMain.jar` | intermediate |
| 11,224,716 | `build/tmp/deobfMcMCP/deobfed.jar` | deobfuscated **Minecraft**, not the mod |
| 9,237,551 | `build/tmp/deobfMcSRG/deobfed.jar` | deobfuscated **Minecraft**, not the mod |
| 6,515,902 | `build/tmp/decompileMc/forge-...-srgBin.jar` | Forge/Minecraft, not the mod |

## Why the previous release did nothing

The earlier build published `build/tmp/deobfMcMCP/deobfed.jar` — the 11 MB
deobfuscated Minecraft listed above — under the release name. Its CI step globbed
for any jar and took the first match. That archive contains no Sky VeLor class, so
no setting in it could have worked. Two independent defects in the Beta-0.10.7v
source also guaranteed the feature would not work even had the right jar shipped:

1. **The mod never compiled.** `PekaClientMod.java` contained a literal
   two-character `\n` sequence where a newline belonged, which is a syntax error,
   so `compileJava` failed and no mod jar existed to package. `CustomItemRenderer`
   additionally declared `@Override` on `renderFireInFirstPerson(float)` and
   `renderMapFirstPerson(float,float,float)`; in 1.8.9 the first is `private` and
   the second does not exist, so both were further compile errors.
2. **The renderer was installed where nothing reads it.** See below.

## The rendering defect, and the evidence that identified it

Minecraft 1.8.9 holds the first-person renderer in two places, and only one of
them is on the render path:

- `Minecraft.itemRenderer` — `private`, non-final. Only the construction site and
  the `getItemRenderer()` getter read it.
- `EntityRenderer.itemRenderer` — `public final`, captured in the `EntityRenderer`
  constructor from `mcIn.getItemRenderer()`.

Every call goes through the second one. `EntityRenderer.updateCameraAndRender`
calls `this.itemRenderer.updateEquippedItem()`, and `renderHand` calls
`this.itemRenderer.renderItemInFirstPerson(partialTicks)` and then
`renderOverlays(partialTicks)`.

Beta-0.10.7v replaced `Minecraft.itemRenderer`. `EntityRenderer` had already
captured the vanilla instance at startup and kept drawing with it, so the slider
moved, the config saved, and the screen did not change. It also looked up the
field by the literal name `"itemRenderer"`, which does not exist in a
reobfuscated runtime, so the lookup threw and the empty `catch` block marked the
feature as initialized anyway.

`FirstPersonItemSetup` now installs into the `EntityRenderer` field that is really
read, points `Minecraft`'s field at the same instance so both agree, finds both
fields by declared type rather than by name, clears `final` once on Java 8, reads
the write back to confirm it took, and logs instead of swallowing failures.

## Bytecode-level confirmation

Parsed from the shipped jar (`tools/verify_release_jar.py` plus a direct
constant-pool read of the two renderer classes):

`CustomItemRenderer` — superclass `net/minecraft/client/renderer/ItemRenderer`,
declares:

| Declared method | Descriptor | Vanilla member |
|---|---|---|
| `func_78440_a` | `(F)V` | `renderItemInFirstPerson(float)` |
| `func_78441_a` | `()V` | `updateEquippedItem()` |

and calls `ItemRenderer.func_78440_a`, `ItemRenderer.func_78441_a`,
`CustomItemRenderer.func_78444_b` (`resetEquippedProgress`),
`GlStateManager.func_179094_E` / `func_179152_a` / `func_179121_F`
(`pushMatrix` / `scale` / `popMatrix`), and — the part that proves the GUI is
wired through — `FirstPersonItem.getItemScale()` **and**
`FirstPersonItem.getAnimationSpeed()`.

Because the overrides are declared under the SRG names that `EntityRenderer`
calls virtually, they bind in the obfuscated runtime. A deobfuscated development
jar would declare `renderItemInFirstPerson` instead and would not be invoked.

`FirstPersonItemSetup` — calls `Minecraft.func_71410_x()` (`getMinecraft`), reads
`Minecraft.field_71460_t` (`entityRenderer`) as a direct field, constructs
`CustomItemRenderer`, and uses `Class.getDeclaredFields`, `Field.getType`,
`Modifier.isFinal`, `Field.setAccessible`, `Field.get`, `Field.set`,
`Class.getDeclaredField("modifiers")` and `Field.setInt` — the whole
type-based lookup and install mechanism, correctly remapped.

## Automated tests

`tests/run-tests.sh` against the compiled classes and the real Forge/Minecraft
classpath: **24 suites, 3,345 checks passed, 0 failed**, exit 0.

`FirstPersonItemTests` (28 checks) is new and covers the values that reach the
renderer: both defaults resolve to an exact `1.0f` multiplier, which is what makes
the renderer skip its matrix work at 100%; a switch turned off forces the vanilla
value regardless of slider position; the percent-to-multiplier conversion; the
clamp bounds, which mirror the range the properties loader clamps with; and
reset-to-defaults.

## Reproducing the verification

```sh
python3 tools/verify_release_jar.py dist/SkyVeLor-1.8.9-Beta-0.10.7v.jar --version Beta-0.10.7v
```

30 checks, exit 0. It confirms the artifact is the mod rather than a ForgeGradle
intermediate, that the version metadata is right, that every expected class is
present and the deleted dead class is absent, that no nested archive or unrelated
project file was packaged, and that Minecraft members are referenced by SRG name
with no development names left. Member names are read only from
`Fieldref`/`Methodref`/`InterfaceMethodref` constants, so the mod's own local
variables and methods cannot be mistaken for unmapped Minecraft members.

## What is still untested

**The game was never launched.** No environment available here can run Minecraft
1.8.9 with a display, so there is no screenshot and no in-game confirmation. What
is proven is that the artifact is correctly built, correctly reobfuscated, and
that the overrides and setting reads are present in the shipped bytecode and bind
to the right vanilla members. What is not proven is what the held item looks like
on screen.

Before publishing, check in Minecraft 1.8.9 with Forge `11.15.1.2318`:

- the log line `First-person item customization installed on EntityRenderer.<field>`
  appears once at startup, and no warning about the install follows it;
- item scale changes the held item live at 50%, 100% and 200% for a sword, a tool,
  a block, a drawn bow and an empty hand;
- 100% looks identical to vanilla with the mod installed;
- animation speed changes how fast the item slides out and back when switching
  hotbar slots, without a jump or a stutter;
- the water, fire and pumpkin overlays, third person, and other players are
  unchanged;
- settings survive a restart from `skyvelor.properties`;
- the existing HUDs, HUD editor, Spirit Leap, fishing and Slayer features still
  work;
- behaviour on PojavLauncher/Android, which is a goal but has never been verified.

`EntityRenderer.itemRenderer` is `final`, so installing the renderer relies on
clearing that modifier through `java.lang.reflect`. That is a supported Java 8
technique and this project targets Java 8 exactly; it is not guaranteed to keep
working on a newer JVM.

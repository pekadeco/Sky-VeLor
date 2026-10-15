# Sky VeLor Beta-0.8v — final report

**Deliverables** (all three locations: attached to the `beta-0.8v` release, committed at the repo root of this
branch, and in the `ci-evidence` branch under `dist/`):

| File | Size | sha256 |
| --- | --- | --- |
| `SkyVeLor-1.8.9-Beta-0.8v.jar` | 166,809 B | `92d31069b5663b1117c9a0c534dc00b7fa7ae0eceef9b5930a7b1ad176cd626b` |
| `SkyVeLor-1.8.9-Beta-0.8v-source.zip` | 148,099 B | `18f162b8bb2ea4dc12b7ff665645d08238d46cdc282a1d39122df5dbbe02fd0e` |

Release: https://github.com/pekadeco/Sky-VeLor/releases/tag/beta-0.8v · PR: https://github.com/pekadeco/Sky-VeLor/pull/2
Built by: GitHub Actions run [38717498298](https://github.com/pekadeco/Sky-VeLor/actions/runs/38717498298) — **success**,
revision `091b939`, JDK 1.8.0_504, Gradle 2.14.1, ForgeGradle 2.1-SNAPSHOT.
The source ZIP lists exactly the 76 files of the built revision.

Every task below was implemented. Nothing was skipped. The one thing I could not do is run the game — that is
called out at the end rather than glossed over.

---

## Task 1 — the loading screen appears too late

**Root cause.** In Forge 1.8.9 the first screen is drawn by Forge, not by the mod:
`FMLClientHandler.beginMinecraftLoading()` starts with `SplashProgress.start()`, which reads
`config/splash.properties` and paints the loading screen on its own thread while Minecraft's own load runs. That
splash is what replaces the Mojang screen. Every normal mod lifecycle hook (`preInit`/`init`/`postInit`, the
`FMLLoadCompleteEvent`, the first tick) runs **after** it. So Sky VeLor's `LoadingScreen`, opened from its own init,
could only ever appear at the point where loading was already finished — which is exactly the symptom reported.

**What was done.** The fix uses the only hook that is early enough — the coremod entry point, which runs during the
launchwrapper launch, before `Minecraft.startGame()`:

- `com.arena.pekaclient.startup.SkyVeLorLoadingPlugin` implements `IFMLLoadingPlugin`; the jar manifest declares
  `FMLCorePlugin` and `FMLCorePluginContainsFMLMod: true` (both verified present in the built jar).
- Its `injectData(Map)` reads `mcLocation`, and calls `EarlySplash.install(...)`.
- `EarlySplash` writes `assets/pekaclient/textures/gui/skyvelor_splash.png` into the resource pack Forge reads
  splash images from, and merges Sky VeLor's colours (background, bar, bar border) and the wordmark into Forge's
  own `splash.properties`, so the very first frame drawn — in place of the Mojang and Forge screen — carries Sky
  VeLor's wordmark, deep-blue background and accent progress bar, before any mod code runs.

**Deliberate constraints, all honoured:**
- No bytecode work at all: `getASMTransformerClass` and `getAccessTransformerClass` return null, no setup class.
  Nothing in Minecraft is touched before it is safe to touch.
- `enabled` is **never** written, so Forge's own decision stands (off by default on macOS, and under OptiFine unless
  it reports compatibility) — OptiFine and macOS behaviour is unchanged.
- A key the player already changed is never overwritten; only missing or stock-valued keys are branded.
- Everything is exception-guarded and silent on failure: a loading screen must never stop the game from launching.
- No custom screen is stacked after the splash, nothing is restyled beyond the branding keys, and the Mojang
  screen is never delayed.

**Honest limitation.** This repaints Forge's splash; it does not replace the splash framework with a Sky VeLor-drawn
frame. Doing that in 1.8.9 means patching `Minecraft.drawVanillaScreen`/`SplashProgress` yourself, which the task
explicitly ruled out as too invasive. If the splash is disabled by Forge or the player, the plain Mojang screen is
drawn and the mod's own in-game `LoadingScreen` remains the fallback. The mechanism is verified (manifest
attributes, the written config, the packaged texture); the pixels have not been seen on a real launch — see the
in-game section.

## Task 2 — startup speed

Two real defects were found in the init path and fixed:

1. **Duplicated work.** `Theme.refresh()` was called directly *and* queued again as a startup task, so the theme
   (colour derivation and cached state) was computed twice on every launch. It is now computed once.
2. **The main menu was delayed by the task queue.** Startup work used to be deferred one task per client tick behind
   a loading screen — a tick per task, holding the first screen. `StartupTasks.runAll()` now runs that work
   immediately during mod initialization, so nothing is left pending when the first screen appears.

The init path logs its own timing (`Sky VeLor init finished in N ms`) and each startup task already logs its work
time, so the cost is visible in `logs/latest.log` on a real launch.

**Evidence, honestly.** No wall-clock before/after number is claimed. There is no Minecraft runtime in this
environment, so I could not measure a launch; the change is described by what was removed (a duplicated theme
computation and a tick-per-task deferral, both deterministic costs), not by a measured figure. The instrumentation
needed to measure it on a real launch is in the build. Nothing was skipped, disabled or deferred for appearance's
sake, and no server-side work was involved.

## Task 3 — Spirit Leap GUI

### 3A. Corrupted / misplaced text — cause found and fixed

The renderer was not at fault. The data was:

- The class marker on the leap item itself (`[B]` in `[B] anuptripahti1`) was never read — only scoreboard *teams*
  and the tab list were.
- The class was matched by searching for the teammate's name *inside* that text, so a name that also appeared in a
  rank tag or another teammate's line could win the wrong match.
- The drawn name kept its rank tags and formatting codes, and the footer drew raw §-laden scoreboard text.
- `ownerName` fell back to the bracket-laden string, which then broke `getPlayerInfo` lookups and produced empty
  heads.

Fixed in the real rendering path: `LeapName` strips formatting codes, rank tags (`[VIP]`, `[MVP+]`, `[MVP++]`,
`[ADMIN]`, …) and the class marker before the name is drawn, so a card reads `Agent_Zeny` / `Berserk` rather than
`[B] Agent_Zeny` / `Unknown class`. The footer strips § too. **No truncation was introduced and no information was
dropped** — names are shown in full; rank and class appear as their own labels.

### 3B. Missing classes — new parser

`com.arena.pekaclient.feature.LeapName` (pure logic, no Minecraft types, so it is fully unit-testable) resolves the
class from the text the client already has, in priority order:

1. **the leap item's own display name** — it belongs to exactly this teammate,
2. the scoreboard team prefix/suffix,
3. the sidebar scoreboard lines (display slot 1 — the same objective the dungeon check reads, since
   `Scoreboard.getObjectives()` does not exist in MCP `stable_22`),
4. the tab-list entry (`NetworkPlayerInfo`),
5. the item's `Class: X` lore line as a last resort.

It normalises § codes and whitespace, isolates the marker independently of rank tags, and only treats a bracket
holding **exactly one** upper-case `A`, `B`, `M`, `T` or `H` as a class — so a rank tag can never be misread as a
class, and a class is never guessed from a name. It handles the marker attached to the name
(`[M]anuptripahti1`), a rank between marker and name (`[B] [VIP] Agent_Zeny`), a name the server cut short
(`[B] anuptripah...`, refused when ambiguous), and prefix/suffix splits. Match is by stable identity (tab-list /
scoreboard / team entry), not by display-name substring. When data is missing or the sources disagree, the class is
`Unknown` — never guessed, and never the same class for everyone. The existing helper was extended rather than
duplicated (`feature/LeapText` now delegates its `classFromPrefix`/`classFromTruncated` to `LeapName`), and the
screen re-reads the sidebar, so the display follows scoreboard changes.

### 3C. Layout and heads

The picker draws **2 columns × 2 rows** of dark Odin-inspired rectangular cards, each with a head, a readable name
and the correct class label, sized from the 1.8.9 GUI scale (`ScaledResolution`) so it stays aligned at every
scale, collapsing to a single column on very narrow windows. Missing head textures degrade gracefully: the skull's
own texture is used when the player is not in the tab list, otherwise a letter falls back — never a blank or a
crash.

New **`LeapNameTests`** (70 checks) covers rank prefixes, formatting codes, missing classes, varied name shapes and
the marker split from the name; `LeapTextTests` (76 checks) still passes unchanged, confirming the refactor kept
existing behaviour.

## Task 4 — GUI preview

**Hub → Dungeon → "Spirit Leap preview" → Open preview.** It opens the **real** `LeapScreen` — same class, same
card layout, same drawing code, same data path (`LeapScreen.preview(...)`), not a mock-up — populated with four
sample teammates in the 2×2 grid:

| Sample | What it exercises |
| --- | --- |
| `[B] [VIP] Agent_Zeny` | rank tag between marker and name |
| `[H] BlueGamer07` | the plain form |
| `[A] Quasar_Knight` (with colour codes) | formatting codes inside the name |
| `[M]anuptripahti1` | no space after the marker |

The panel is titled "Spirit Leap (Preview)" and marked "Test data". There is no container behind it, clicking only
moves the highlight, and `pick()` is disabled in preview mode, so **no packet, teleport or leap can occur**. Escape
or the Close button returns to the hub, and the entry lives in the existing Dungeon page of the existing hub, so it
integrates with the current menu system. Heads fall back to the letter style, because a sample name has no account
to fetch a skin from — which is the same fallback the live screen uses when a skin never arrives. A 2×2 grid holds
four cards, so the four samples requested are the ones shown; Tank and the unknown-class case are covered by the
parser tests instead.

## Task 5 — version

The release version has a single source of truth, `PekaVersion.NAME`, which `build.gradle` reads for the artifact
name; `mcmod.info` and the GUI version text follow it. It is now `Beta-0.8v`. Verified in the built jar: `mcmod.info`
reports `"version": "Beta-0.8v"` / `"mcversion": "1.8.9"`, the manifest carries `Implementation-Version: Beta-0.8v`,
and the only version string in the compiled classes is `Beta-0.8v` (5 occurrences, zero occurrences of `Beta-0.7v`).
The artifact name is exactly `SkyVeLor-1.8.9-Beta-0.8v.jar`. The directory is still called `SkyVeLor-Beta-0.7v` — it
is a path, not metadata, and renaming it would have been a large, unrelated diff.

## Task 6 — build (mandatory)

The existing Forge 1.8.9 + ForgeGradle setup was kept as-is: JDK 8, Gradle 2.14.1, ForgeGradle 2.1-SNAPSHOT, no
dependency bumps, no modernization. Build blockers were resolved one at a time, each from the real error in the log:

1. `gradle build` compiled the mod but **`compileJava` failed** — `LeapScreen.java:341: cannot find symbol: method
   getObjectives()` on `Scoreboard`. The 1.8.9 MCP API has no such method; the sidebar is read through
   `getObjectiveInDisplaySlot(1)` + `getSortedScores(...)` + `ScorePlayerTeam.formatPlayerName(...)`, which is what
   `DungeonContext` already did. Fixed and it compiled.
2. The verify step then exited 1 with **no message**: a counting `grep` that found *nothing* — the passing case — was
   killing the step under `set -euo pipefail`. Every counting pipeline now ends with `|| true`.
3. The verify step then failed for real, and the captured log named it: the source-ZIP-versus-git-tree diff listed
   every file as changed. Two causes: `git archive` writes directory entries into the zip while `git ls-tree -r`
   lists blobs only, and the two lists were built on different path bases (`ls-tree HEAD:<dir>` already prints paths
   relative to that directory). Both fixed in the check — the ZIP itself was always correct.
4. The test step failed to compile: a check of mine in `LeapNameTests` passed a `String` where a `boolean` was
   expected. Fixed.

Then, with a green run:

- **`gradle build dumpTestClasspath` → BUILD SUCCESSFUL**, jar and source ZIP produced and collected.
- **All 14 test suites run and pass: 914 checks, 0 failures** (details below).
- **The jar was inspected** (not assumed) on the released file:

```
mcmod.info   : version Beta-0.8v, mcversion 1.8.9, modid pekaclient, name Sky VeLor
manifest     : FMLCorePlugin: com.arena.pekaclient.startup.SkyVeLorLoadingPlugin
               FMLCorePluginContainsFMLMod: true, Implementation-Version: Beta-0.8v
classes      : 67   (includes LeapName, LeapPreviewData, EarlySplash, SkyVeLorLoadingPlugin)
assets       : pekaclient/textures/gui/skyvelor_splash.png packaged
reobfuscation: 174 srg member names, 0 MCP member names left, no com/nef entries
```

Worth recording for anyone reading the build in future: ForgeGradle's Forge plugin reobfuscates with
`ReobfMappingType.SEARGE`, so a *correct* Forge 1.8.9 jar keeps MCP **class** names (FML's runtime remapper handles
them) while members become `func_`/`field_`. A jar still containing `getMinecraft` is the broken one. The CI check
asserts exactly that: srg members present, zero MCP members.

### Test results (CI, JDK 1.8.0_504)

| Suite | Result | | Suite | Result |
| --- | --- | --- | --- | --- |
| **LeapName (new)** | **70/70** | | LeapLayout | 561/561 |
| **EarlySplash (new)** | **16/16** | | LeapText | 76/76 |
| PureLogic | 70/70 | | LeapClass | 27/27 |
| Config | 24/24 | | SpiritLeap | 12/12 |
| ConfigFiles | 7/7 | | DungeonContext | 7/7 |
| DollConfig | 14/14 | | MobMarkers | 8/8 |
| StatsText | 10/10 | | StartupTiming | 12/12 |

## In-game testing — what did not happen

**Nothing in Beta-0.8v has been run in Minecraft.** This environment has no display, no Minecraft install and no
Forge runtime, so I could not launch the game; claiming otherwise would be false. Consequently:

- the Sky VeLor splash replacing the Mojang screen was **not seen on a real launch** (the mechanism is verified
  statically: manifest attributes, the written `splash.properties`, the packaged texture),
- the Spirit Leap picker was **not** tested against a real Fakepixel dungeon, its live scoreboard or real heads,
- the startup timing improvement was **not** measured as a before/after number,
- no OptiFine, macOS or PojavLauncher behaviour was tested.

What *was* automated and run: compilation on the real Forge toolchain, all 14 suites, jar metadata/manifest/content
inspection, all 67 classes of the mod compiled and packaged, and the source-ZIP-versus-revision check.

## Remaining bugs and limitations

1. **Splash is repainted, not replaced.** If Forge's splash is off (macOS, OptiFine without its compatibility key,
   or `enabled=false`), the Mojang screen is drawn and the mod's in-game loading screen is the fallback.
2. **Live dungeon behaviour is unverified.** Scoreboard-driven class resolution depends on what Fakepixel actually
   puts on the board; the parser is written to say `Unknown` rather than guess when the server does not provide it.
3. **The leap key still requires the server's own leap menu to be open** — the key only sends the same use action as
   a right-click.
4. **Non-leap items are unaffected**: the picker only replaces the server's leap chest when the title or the item
   layout identifies it as the Spirit Leap menu.
5. **Heads in the preview are letter fallbacks** by design (sample names have no accounts); live heads depend on the
   tab list or the skull's own texture.
6. **Item-in-Hand and Scrollboard** remain unimplemented — they need a transform inside `EntityRenderer.renderHand`,
   which the no-ASM-transform constraint rules out.

# Sky VeLor, Beta-0.8v

Forge client mod for Minecraft 1.8.9 (Forge 11.15.1.2318). Desktop Java is the target.
Android launchers (for example PojavLauncher) are **not tested**.

Release: **SkyVeLor-1.8.9-Beta-0.8v.jar** (built and verified by GitHub Actions).

## What is in Beta-0.8v

Four changes were asked for, and all four are implemented in this build.

### 1. The loading screen now runs at startup, not after it

Forge draws the first screen itself. In `FMLClientHandler.beginMinecraftLoading` the very first call is
`SplashProgress.start()`, which reads `config/splash.properties` and then paints the loading screen on its own
thread while Minecraft loads. That screen is what replaces the plain Mojang screen. Every normal mod hook runs
later, which is why Sky VeLor's own loading screen could only appear once the game had already finished starting.

The fix uses the only point that is early enough: a coremod entry point. `SkyVeLorLoadingPlugin` (a
`IFMLLoadingPlugin`, declared in the jar manifest as `FMLCorePlugin` with `FMLCorePluginContainsFMLMod`) runs
during the launchwrapper launch, before `Minecraft.startGame()`. It calls `EarlySplash`, which writes the Sky
VeLor wordmark into the resource pack Forge loads splash images from and merges the Sky VeLor colours into
Forge's own `splash.properties`. From then on the loading screen carries Sky VeLor's wordmark, deep-blue
background and accent progress bar, drawn in place of the Mojang and Forge screen, before any mod code runs.

Deliberately unchanged, so the normal startup path is not put at risk:

- **No bytecode work at all.** The plugin registers no ASM transformer, no access transformer and no setup
  class. It only writes two files.
- **`enabled` is never written.** Forge keeps its own decision, which is off by default on macOS and off under
  OptiFine unless OptiFine reports compatibility. So OptiFine and macOS behaviour is exactly as before.
- **A value the player already changed is never overwritten.** Only keys that are missing or still hold Forge's
  stock value are branded.
- **Anything that goes wrong is swallowed.** A loading screen must never stop the game from launching.

Limitations, stated plainly:

- If the Forge splash is disabled (`enabled=false`, macOS, or OptiFine without the compatibility key), Forge
  draws the plain Mojang screen and no mod can draw in its place without patching Minecraft itself. In that case
  Sky VeLor's in-game loading screen is still the fallback.
- The splash's layout, progress bar and text are Forge's. Sky VeLor changes the wordmark, the colours, the
  background and the bar, not the framework around them.
- This has **not** been seen running in a real game from this environment. What is verified is the mechanism:
  the manifest attributes are in the jar, the configuration Forge reads is written before the splash starts, and
  the wordmark is packaged where the splash looks for it. See "Not tested in-game" below.

### 2. Startup work no longer delays the game

Two real problems were found and fixed in `PekaClientMod.init`:

- `Theme.refresh()` ran **twice** - once directly and once again as a queued startup task. The theme is now
  computed once.
- The queued startup work was deferred, one task per game tick, behind a loading screen. That held the main menu
  for a tick per task and is what made Sky VeLor's screen appear late. `StartupTasks.runAll()` now runs the work
  immediately during mod initialization, so nothing is left pending when the first screen appears and the menu is
  not delayed.

The init path logs its own timing (`Sky VeLor init finished in N ms`), and each startup task already logged its
own work time, so the cost is visible in `logs/latest.log`.

**No before/after wall-clock measurement is claimed.** The game cannot be run in this environment, so the
improvement is described by what was removed (a duplicated theme computation and a tick-per-task delay), not by
a measured number. The instrumentation is in place to measure it on a real launch.

### 3. Spirit Leap: text and classes

The old code only read the scoreboard *teams* and the tab list, and it matched the class to a teammate by looking
for the teammate's name inside that text. The class marker sitting on the leap item itself - the `[B]` in
`[B] anuptripahti1` - was never read, and the marker and rank tags stayed in the name that was drawn. That is
where "missing classes" and the corrupted-looking names came from.

New `LeapName` parser (pure, no Minecraft types) reads the text the client already has, in this order:

1. **the leap item's own display name** - it belongs to exactly this teammate,
2. the scoreboard team prefix/suffix,
3. the sidebar scoreboard lines (display slot 1, the same objective the dungeon check reads),
4. the tab-list entry,
5. the item's `Class: X` lore line as a last resort.

It:

- strips Minecraft formatting codes, rank tags (`[VIP]`, `[MVP+]`, `[MVP++]`, `[ADMIN]`, ...) and the class
  marker from the drawn name, so a card shows `Agent_Zeny` and `Berserk`, never `[B] Agent_Zeny` next to
  "Unknown class",
- only treats a bracket holding exactly one upper-case `A`, `B`, `M`, `T` or `H` as a class, so a rank tag can
  never be read as a class and a class is never guessed from a name,
- handles the marker against the name (`[M]anuptripahti1`), a rank between them (`[B] [VIP] Agent_Zeny`) and a
  name the server cut short (`[B] anuptripah...`), and refuses an ambiguous cut,
- never gives one teammate another teammate's class: the scoreboard and the tab list must agree, and a marker is
  only used when it is attached to that teammate's own name.

The preview and the live screen share this path, so what the preview shows is what a dungeon shows.

### 4. Spirit Leap preview in the mod GUI

**Hub -> Dungeon -> "Spirit Leap preview" -> Open preview.** It opens the real `LeapScreen` with four sample
teammates in the same 2-column x 2-row grid, using the same cards, layout, heads and text drawing:

| Sample | What it tests |
| --- | --- |
| `[B] [VIP] Agent_Zeny` | rank tag between the marker and the name |
| `[H] BlueGamer07` | the plain form |
| `[A] Quasar_Knight` (with colour codes) | formatting codes inside the name |
| `[M]anuptripahti1` | no space after the marker |

The panel is titled "Spirit Leap (Preview)" and marked "Test data". There is no container behind it, clicking a
card only moves the highlight, and `pick()` cannot run at all in preview mode, so nothing is ever sent to a
server. Escape or Close returns to the hub. Heads fall back to a letter in the preview, because a sample name has
no account to fetch a skin from - which is also the real fallback the live screen uses when a skin never arrives.

Tank is not in the grid: a 2 x 2 grid holds four cards and the request was for four. Tank and the
"unknown class" case are covered by the parser tests instead.

## Version

`PekaVersion.NAME` is `Beta-0.8v`, and it is the single source: `build.gradle` reads it, so the jar name
(`SkyVeLor-1.8.9-Beta-0.8v.jar`), `mcmod.info` and the version shown in the GUI all follow. Confirmed in the
built jar: `mcmod.info` says `"version": "Beta-0.8v"`, and the only version string in the classes is `Beta-0.8v`.

## Build

Requires JDK 8 and Gradle 2.14.1 with ForgeGradle 2.1-SNAPSHOT from `https://maven.minecraftforge.net`.

    gradle build dumpTestClasspath     # output: build/libs/SkyVeLor-1.8.9-Beta-0.8v.jar

`-x decompileMc` is **not** needed. `compileJava` depends on `deobfMcMCP`; only `setupDecompWorkspace` pulls in
`decompileMc`.

### About reobfuscation (worth knowing)

ForgeGradle's Forge plugin reobfuscates mod jars with `ReobfMappingType.SEARGE`, not `NOTCH`. A correct Forge
1.8.9 release jar therefore **keeps MCP class names** (FML's runtime remapper maps them to notch names at
launch) while every member name becomes an srg name (`func_NNNN_x`, `field_NNNN_x`). A jar that still contains
MCP member names such as `getMinecraft` is the broken one. The CI check asserts exactly that: srg member names
present, zero MCP member names left.

## Tests

    gradle build dumpTestClasspath
    TEST_CP=$PWD/build/test-classpath.txt sh tests/run-tests.sh

`TEST_CP` is the classpath file `dumpTestClasspath` writes, so the tests run against exactly the jars the build
used. `MC_JAR` / `LOG4J_JAR` / `GSON_JAR` still work if you prefer to point at jars yourself.

Beta-0.8v results (all 14 suites, 914 checks, JDK 1.8.0_504, in CI):

| Suite | Result |  | Suite | Result |
| --- | --- | --- | --- | --- |
| PureLogic | 70/70 | | LeapLayout | 561/561 |
| Config | 24/24 | | **LeapName (new)** | **70/70** |
| LeapText | 76/76 | | LeapClass | 27/27 |
| ConfigFiles | 7/7 | | SpiritLeap | 12/12 |
| DollConfig | 14/14 | | DungeonContext | 7/7 |
| MobMarkers | 8/8 | | StatsText | 10/10 |
| StartupTiming | 12/12 | | **EarlySplash (new)** | **16/16** |

## Verification of the built jar

Checked automatically on every push, and again by hand on the released file:

- `mcmod.info`: `"version": "Beta-0.8v"`, `"mcversion": "1.8.9"`.
- 67 class files, including `LeapName`, `LeapPreviewData`, `EarlySplash` and `SkyVeLorLoadingPlugin`.
- Reobfuscated the way Forge 1.8.9 expects: 174 srg member names, 0 MCP member names.
- Manifest carries `FMLCorePlugin: com.arena.pekaclient.startup.SkyVeLorLoadingPlugin` and
  `FMLCorePluginContainsFMLMod: true`, so the early hook actually runs.
- The splash wordmark is packaged at `assets/pekaclient/textures/gui/skyvelor_splash.png`.
- The source ZIP lists exactly the tree of the revision that was built.

## Not tested in-game

**Nothing in Beta-0.8v has been run in Minecraft.** This environment has no display, no Minecraft install and no
Forge runtime, so the following are implemented and unit-tested but not observed in a game:

- the Sky VeLor loading screen actually replacing the Mojang screen on a real launch (the mechanism is verified,
  the pixels are not),
- the Spirit Leap picker against a real Fakepixel dungeon, its real scoreboard and its real heads,
- the startup timing difference on a real launch, and any OptiFine or PojavLauncher behaviour.

What *was* verified: the project compiles under JDK 8 with ForgeGradle 2.1, all 14 suites pass, and the jar's
metadata, manifest, contents and reobfuscation were inspected as listed above.

---

## Previous release notes (Beta-0.7v)

Beta-0.7v was a source-only update: the version bump, the HUD editor key-scroll fix, and a source ZIP. The jar
in that release was built by CI with the same toolchain and is a valid Forge 1.8.9 jar.

## Previous release notes (Beta-0.6v)

Output: `SkyVeLor-1.8.9-Beta-0.6v.jar`. The Beta-0.5v, 0.4v, 0.3v and 0.2v jars are kept unchanged.

**Beta-0.6v:** Changes in this build:

- Spirit Leap: teammates are matched to the scoreboard sidebar (every team is searched, ignoring case), then the
  tab list. A name the server cut short with "..." matches only when exactly one player starts with that text.
  Names take their class colour. Heads come from the tab list, or from the skull's own texture when the player is
  not listed. Hovering a card shows the raw scoreboard and tab-list text in the footer.
- Spirit Leap is two columns (two rows for four teammates); the card width adapts to the screen width.
- Dungeon mobs: the outline is drawn around the mob under a star or level tag, not around the floating name tag.
- Camera yaw/pitch feature removed. Shortcuts section in QoL removed (keyboard tab keys are unchanged).
- Mouse wheel is faster by default; the scroll-speed setting now goes to 300%. Keyboard scrolling no longer uses
  it.
- New HUD element **FPS and ping** (off by default). TPS is not shown: the client's world clock is advanced
  locally and only corrected from the server, so a client-side TPS would not be the server's real tick rate.
- Startup timing: each startup task's work time and the wall time are logged, plus the JVM uptime when startup
  finishes.

Not in this build: Item-in-Hand (size, speed, hit and swing animation). It needs a transform inside
`EntityRenderer.renderHand`. The public hooks cannot do it, and the held-item renderer is `public final` in
`EntityRenderer`, so the only route is a Mixin or ASM hook, which changes how the client launches. Also not in
this build: Scrollboard, AOTV QoL, the expandable [QoL]/[HUD] layout and route work.

## Previous verification status (Beta-0.3v)

- **Code review:** done for every new file and for each changed screen. Not a full line-by-line audit.
- **Compilation:** BUILD SUCCESSFUL under JDK 8u504 and Gradle 2.14.1, no warnings.
- **Automated tests:** 12 suites, all passing.
- **Jar check:** 71 classes, no `com/nef` entries, `mcmod.info` version Beta-0.3v.
- **In-game (one smoke test, Xvfb with llvmpipe, no sound):** the client started with Forge loading 4 mods, no
  Sky VeLor errors, main menu with the Sky VeLor button, the hub opened on QoL, the HUD overview showed the
  element cards, the Player Dollars sub-page opened with its back row, and the Dungeon page rendered. The leap
  picker, mob highlights, routes, and the keybinds were **not** tested in-game.

## Features

- **Startup:** a loading screen (black to deep-blue gradient, wordmark, version, spinner, real progress bar over
  Sky VeLor's own startup tasks). Since Beta-0.8v the same branding is applied to Forge's own splash, which is
  the first screen drawn.
- **Hub:** tabs QoL, HUD, Farming, Dungeon, Slayer, Fishing. Slayer and Fishing are empty pages, not modules.
- **HUD tab:** an overview with one enable toggle and one "Open" row for each element: Player Dollars, Inventory
  (main 27-slot grid), Mining BPS (blocks mined per second), Player Doll, Hotbar, FPS and ping. Each opens a
  sub-page with a "< HUD" back row. The HUD editor and "Reset positions" stay on the overview.
- **Farming:** Mining BPS status and an "Open Mining BPS settings" button.
- **Spirit Leap (Dungeon tab):** the server's leap menu is replaced by a compact teammate picker: a card per
  teammate with a 2D skin head, name, and class in colour (Archer yellow, Berserk red, Mage sky blue, Tank
  green, Healer light pink). Unknown or missing classes show grey. Mouse, touch and keyboard work. Clicks are
  vanilla window clicks on the server's own container. A pick waits for the server, so a double tap cannot send a
  second leap. Since Beta-0.8v there is also a test preview with sample teammates.
- **Leap menu detection:** the title "Spirit Leap", or a recent Spirit Leap right-click inside a dungeon
  (Catacombs sidebar) with teammate heads in the chest.
- **Leap keybind (unbound by default):** sends the same right-click as holding the item.
- **Mob highlights (Dungeon tab):** outlines living, non-player mobs whose name tag has a level tag or the elite
  star, inside a dungeon. World overlay only; no entity renderer is touched.
- **Routes (Dungeon tab, route manager):** create, rename, reorder, enable, delete. Set rotation, set origin,
  mark waypoints, clear, and import or export (validated JSON). Saved in `skyvelor-routes.json`.
- **Window controls:** red closes everywhere. Yellow (collapse) and green (zoom) work in the hub. In the leap
  menu, route manager and HUD editor they are dimmed, and hovering shows why.
- **Player Doll and HUD:** scale, opacity, rotation, drag-to-turn, editor. Balance is called "Player Dollars". Its
  saved id is unchanged, so existing settings load.

## Limitations (stated plainly)

- **Splash:** see the section above. The FML splash is repainted, not replaced by a Sky VeLor-implemented frame.
- **Spirit Leap key:** the server must open its own leap menu first. The key only sends the same use action as a
  right-click.
- **Room safety:** Sky VeLor does not read a room ID from the game. A route shows only inside a dungeon and within
  24 blocks of its origin.
- **Mob highlights:** the name-tag rules (level tag `[LvN]`, elite star U+272F) come from reading the server's
  names, not a verified spec.
- **Routes, leap and mob highlights:** not tested against a live dungeon.
- **Platforms:** Android, PojavLauncher and gl4es are not tested.

## Config files

- `skyvelor.properties` and `skyvelor-hud.properties` are written. The older `pekaclient*.properties` are read
  only when the new file is absent.
- `skyvelor-routes.json` holds routes. Import reads `skyvelor-routes-import.json`. Export writes
  `skyvelor-routes-export.json`.
- `config/splash.properties` is Forge's own file. Sky VeLor fills in the branding keys it does not find set, and
  never touches `enabled` or a value you already changed.

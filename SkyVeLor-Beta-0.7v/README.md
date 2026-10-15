# Sky VeLor, Beta-0.7v

Forge client mod for Minecraft 1.8.9 (Forge 11.15.1.2318). Desktop Java is the target.
Android launchers (for example PojavLauncher) are **not tested**.

## Status of this build (read first)

**Beta-0.7v is a source-only update. No JAR was built and nothing was run in-game.**
The build environment for this session could not reach the Forge Maven repository, the Minecraft
library and asset hosts, or Maven Central, and no `javac` was available. So:

- **JAR:** not built. Running `gradle build -x decompileMc` in a normal environment is required to produce
  `SkyVeLor-1.8.9-Beta-0.7v.jar`. No jar named Beta-0.7v exists yet.
- **Compilation:** not run. The only check was a syntax parse of all 63 Java files with a Java 8 grammar parser
  (`javalang`). That does not type-check against Minecraft or Forge.
- **Automated tests:** not run (they need the Forge binary jar and the Minecraft classes).
- **In-game:** not run.

## Changes in Beta-0.7v

- **Version:** `PekaVersion.NAME` is now `Beta-0.7v`. `build.gradle`, `mcmod.info` and the GUI read it from there.
- **HUD editor keyboard scrolling:** the Up and Down arrow keys scrolled the inspector by the scroll-speed setting.
  They now use the fixed key step, as the hub does and as Beta-0.6v's notes already said. Mouse-wheel speed is
  unchanged.

## Audit notes for Beta-0.7v (what was read, and what was not fixed)

Source read: `PekaClientMod`, `feature/LeapClass`, `feature/LeapText`, `feature/SpiritLeap`, `gui/LeapScreen`,
`gui/LeapLayout`, `gui/ScrollState`, `gui/HudEditorScreen` (input), `gui/AppleHubScreen` (input),
`feature/MobMarkers`, `render/MobHighlight`, `startup/StartupTasks`, `gui/LoadingScreen`, `render/SkinHead`.

- **Spirit Leap class prefixes:** `LeapText.classFromPrefix` reads `[A]`, `[B]`, `[M]`, `[T]` and `[H]` from the
  scoreboard and tab-list text, and skips ranks such as `[VIP]`. Matching is case-insensitive and formatting codes
  are removed first. Those rules are unit-tested in the Beta-0.6v suites. The remaining "class not found" cases
  depend on what the live server sends. No live server was available, so this is **not verified**.
- **Spirit Leap heads:** the head uses the tab-list skin, or the skull's own texture. Loading is asynchronous and
  never blocks rendering. The placeholder/letter fallback is in place. Whether heads fail on a live server is not
  verified.
- **Dungeon mob hitboxes:** already implemented in Beta-0.6v as a world overlay (`render/MobHighlight`). It finds
  the mob under each marked name tag (level tag or star) and outlines its bounding box. Entity renderers, models,
  layers, visibility and culling are not changed. Not verified in a live dungeon.
- **Mouse wheel:** `Mouse.getEventDWheel()` is read before `super.handleMouseInput()` in both the hub and the HUD
  editor, which is the correct order. Wheel speed is set by `WHEEL_STEP` and the scroll-speed setting.
- **Startup order:** the Mojang and Forge splash shown before mod code runs cannot be replaced from a normal mod.
  Sky VeLor's loading screen opens on the first main menu (`GuiOpenEvent`) or the first client tick while startup
  tasks are pending. The startup tasks are small (HUD layout read and theme refresh). No startup time was measured,
  so no speed-up is claimed.
- **Chunk-loading FPS spikes:** not changed. Chunk generation is server-side, and no client-side change was
  verified.

## Item-in-Hand: not implemented in Beta-0.7v

The feature needs a transform inside the first-person hand render (`EntityRenderer.renderHand`). The public
Forge 1.8.9 hooks cannot apply it, so it needs a Mixin or ASM hook. That changes how the client launches, and
the exact Minecraft 1.8.9 (stable_22 MCP) member names could not be checked against the Minecraft jar in this
environment. Shipping it unverified could stop the whole client from starting. It is left out rather than shipped
as controls that do nothing. Next step: build with the Forge toolchain, inspect the deobfuscated jar for
`EntityRenderer.renderHand` and `ItemRenderer.renderItemInFirstPerson`, then decide on the hook.

## Build

Requires JDK 8 and Gradle 2.14.1 with ForgeGradle 2.1 (plugin 0.0.0 installed locally).

    gradle build -x decompileMc        # output: build/libs/SkyVeLor-1.8.9-Beta-0.7v.jar

Version: `PekaVersion.NAME` is the only source.

## Tests

Run after `gradle build` (no game is started):

    CLASSES=$PWD/build/classes/main MC_JAR=<forge binary jar> \
      LOG4J_JAR=<log4j-api jar> GSON_JAR=<gson 2.2.4 jar> sh tests/run-tests.sh

`MC_JAR` must be the Forge binary jar (`forgeBin-1.8.9-11.15.1.2318-1.8.9.jar`). Beta-0.7v has not been tested
in this way yet. The Beta-0.6v results below are from the Beta-0.6v build, not from this one.

---

## Previous release notes (Beta-0.6v)


Forge client mod for Minecraft 1.8.9 (Forge 11.15.1.2318). Desktop Java is the target.
Android launchers (for example PojavLauncher) are **not tested**.

Output: `/home/user/SkyVeLor-1.8.9-Beta-0.6v.jar` (built from this source).
The Beta-0.5v, 0.4v, 0.3v and 0.2v jars are kept unchanged.

**Beta-0.6v:** Changes in this build:
- Spirit Leap: teammates are matched to the scoreboard sidebar (every team is searched, ignoring case), then the tab
  list. A name the server cut short with "..." matches only when exactly one player starts with that text. Names take
  their class colour. Heads come from the tab list, or from the skull's own texture when the player is not listed.
  Hovering a card shows the raw scoreboard and tab-list text in the footer.
- Spirit Leap is two columns (two rows for four teammates); the card width adapts to the screen width.
- Dungeon mobs: the outline is drawn around the mob under a star or level tag, not around the floating name tag.
- Camera yaw/pitch feature removed. Shortcuts section in QoL removed (keyboard tab keys are unchanged).
- Mouse wheel is faster by default; the scroll-speed setting now goes to 300%. Keyboard scrolling no longer uses it.
- New HUD element **FPS and ping** (off by default). TPS is not shown: the client's world clock is advanced locally
  and only corrected from the server, so a client-side TPS would not be the server's real tick rate.
- Startup timing: each startup task's work time and the wall time are logged, plus the JVM uptime when startup
  finishes. The uptime includes Mojang and Forge startup, which Sky VeLor cannot split out.

Not in this build: Item-in-Hand (size, speed, hit and swing animation). It needs a transform inside
`EntityRenderer.renderHand`. The public hooks cannot do it, and the held-item renderer is `public final` in
`EntityRenderer`, so the only route is a Mixin or ASM hook, which changes how the client launches. Also not in this
build: Scrollboard (the sidebar scoreboard is already shown in the leap matching above), AOTV QoL, the expandable
[QoL]/[HUD] layout and route work.

## Build

Requires JDK 8 and Gradle 2.14.1 with ForgeGradle 2.1 (plugin 0.0.0 installed locally).

    gradle build -x decompileMc        # output: build/libs/SkyVeLor-1.8.9-Beta-0.6v.jar

Version: `PekaVersion.NAME` is the only source. `build.gradle`, `mcmod.info` and the GUI read it.

## Tests

Run after `gradle build` (no game is started):

    CLASSES=$PWD/build/classes/main MC_JAR=<deobfuscated minecraft jar> \
      LOG4J_JAR=<log4j-api jar> GSON_JAR=<gson 2.2.4 jar> sh tests/run-tests.sh

Suites and results (Beta-0.6v build, JDK 8): PureLogic 70/70, Config 24/24, LeapText 76/76,
ConfigFiles 7/7, DollConfig 14/14, LeapClass 27/27, SpiritLeap 12/12, DungeonContext 7/7, MobMarkers 8/8,
LeapLayout 561/561, StatsText 10/10, StartupTiming 12/12. LeapText is 76/76 after the name-match tests. `MC_JAR` must be the Forge binary jar (`forgeBin-1.8.9-11.15.1.2318-1.8.9.jar`): the
merged minecraft jar lacks the entity classes that `DollConfigTests` loads.

## Beta-0.6v verification

- **Code review:** leap class sources reviewed against the Forge 1.8.9 class signatures (scoreboard team API).
- **Code review:** leap class sources and the Forge hand-render path reviewed against the Forge 1.8.9 binary
  (`RenderHandEvent` is `@Cancelable`, fires before vanilla `renderHand`, and `renderHand` calls `loadIdentity`).
- **Compiled:** `gradle build -x decompileMc` succeeds (JDK 8u504, Gradle 2.14.1, ForgeGradle 2.1).
- **Automated tests:** all suites above pass.
- **In-game:** not run. The leap layout, class matching, FPS/ping element and startup timing have not been seen in
  a live client.
- **Item-in-Hand:** not implemented. Scale, rotation and offsets need a transform inside `EntityRenderer.renderHand`.
  The public hooks cannot do it, so it needs a Mixin or ASM hook, which changes how the client launches. That needs
  a separate decision and an in-game check before it ships.

## Features

- **Startup:** a loading screen (black to deep-blue gradient, wordmark, version, spinner, real progress bar
  over Sky VeLor's own startup tasks). It appears only while those tasks run, and it opens over the main menu.
- **Hub:** tabs QoL, HUD, Farming, Dungeon, Slayer, Fishing. Slayer and Fishing are empty pages, not modules.
- **HUD tab:** an overview with one enable toggle and one "Open" row for each element: Player Dollars,
  Inventory (main 27-slot grid), Mining BPS (blocks mined per second), Player Doll, Hotbar. Each opens a
  sub-page with a "< HUD" back row. The HUD editor and "Reset positions" stay on the overview.
- **Farming:** Mining BPS status and an "Open Mining BPS settings" button. There is one toggle, in HUD.
- **Spirit Leap (Dungeon tab):** the server's leap menu is replaced by a compact teammate picker: a card per
  teammate with a 2D skin head, name, and class in colour (Archer yellow, Berserk red, Mage sky blue,
  Tank green, Healer light pink). Unknown or missing classes show grey. Mouse, touch and keyboard work.
  Clicks are vanilla window clicks on the server's own container. A pick waits for the server, so a double
  tap cannot send a second leap.
- **Leap menu detection:** the title "Spirit Leap", or a recent Spirit Leap right-click inside a dungeon
  (Catacombs sidebar) with teammate heads in the chest.
- **Leap keybind (unbound by default):** sends the same right-click as holding the item. See limitations.
- **Mob highlights (Dungeon tab):** outlines living, non-player mobs whose name tag has a level tag or the
  elite star, inside a dungeon. It is a world overlay only and does not touch entity renderers.
- **Routes (Dungeon tab, route manager):** create, rename, reorder, enable, delete. Set rotation, set origin,
  mark waypoints (button or keybind, unbound by default), clear, and import or export (validated JSON).
  Routes are saved in `skyvelor-routes.json`, separate from the HUD file.
- **Window controls:** red closes everywhere. Yellow (collapse) and green (zoom) work in the hub. In the
  leap menu, route manager and HUD editor they are dimmed, and hovering shows why.
- **Player Doll and HUD:** unchanged from Beta-0.2v (scale, opacity, rotation, drag-to-turn, editor).
  Balance is now called "Player Dollars". Its saved id is unchanged, so existing settings load.

## Limitations (stated plainly)

- **Splash:** the Mojang and Forge splash shown before mod code runs is **not replaced**. A mod without a
  core hook cannot intercept it. Sky VeLor's loading screen starts when the main menu opens, so it is
  not the first screen of the launch.
- **Spirit Leap key:** the server must open its own leap menu first. The key only sends the same use action
  as a right-click. The picker then appears after the server opens its menu. Sky VeLor does not bypass
  or pre-select server validation.
- **Room safety:** Sky VeLor does not read a room ID from the game. A route shows only inside a dungeon
  and within 24 blocks of its origin. Put the origin near the middle of the room. A neighbouring room
  could still show the route if its wall is within 24 blocks of that origin. Rotation is chosen by the
  player. Nothing is auto-detected.
- **Mob highlights:** the name-tag rules (level tag `[LvN]`, elite star U+272F) come from reading the
  server's names, not a verified spec. Not checked against a live Hypixel dungeon.
- **Routes and leap:** neither the picker nor the route overlay has been tested against a live dungeon.
- **HUD toggles:** in the single in-game smoke test (see below), the enable toggles did not appear in the
  screenshots of the HUD overview or Dungeon page. The Open and Back buttons did. The toggle rendering is
  **unresolved** and needs a fix and a retest.
- **Platforms:** Android, PojavLauncher and gl4es are not tested.

## Verification status (Beta-0.3v)

- **Code review:** done for every new file and for each changed screen. Not a full line-by-line audit.
- **Compilation:** BUILD SUCCESSFUL under JDK 8u504 and Gradle 2.14.1, no warnings.
- **Automated tests:** 12 suites above, all passing. The pure-logic tests cover the new leap, dungeon,
  mob, route maths, route JSON and route store code. Route store file tests use a temporary folder.
  The damaged-file path (move aside to `.bad`) is covered by review only.
- **Jar check:** 71 classes, no `com/nef` entries, `mcmod.info` version Beta-0.3v.
- **Entity check:** no file for entity, renderer, model, layer or mixin changed against `backups/pre-beta-0.2v/src`.
- **In-game (one smoke test, Xvfb with llvmpipe, no sound):** the client started with Forge loading 4 mods, no
  Sky VeLor errors, main menu with the Sky VeLor button, the hub opened on QoL, the HUD overview showed
  the element cards, the Player Dollars sub-page opened with its back row, and the Dungeon page rendered.
  The leap picker, mob highlights, routes, and the keybinds were **not** tested in-game.

## Config files

- `skyvelor.properties` and `skyvelor-hud.properties` are written. The older `pekaclient*.properties` are read
  only when the new file is absent.
- `skyvelor-routes.json` holds routes. Import reads `skyvelor-routes-import.json`. Export writes
  `skyvelor-routes-export.json`.

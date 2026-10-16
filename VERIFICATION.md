# Sky VeLor Beta-0.10.6.5v — Terminal GUI fix and verification

Release: `Beta-0.10.6.5v`
Source of truth: `SkyVeLor-source (1).zip` (SHA-256 `324ebc8381c93e158ebf09a30c63767271a916b48515795c8790310f305bf0b4`),
extracted and used unchanged apart from the fix described here.
Artifact: `dist/SkyVeLor-1.8.9-Beta-0.10.6.5v.jar`
SHA-256 `0ad01d92c4e0abd143e4aa669590f2d3cb1cd98f02c3cdb79e20712dab61ac14` (309,872 bytes, 119 classes)

## 1. The bug

In an F7/M7 dungeon the terminal panel appeared with its title, header, transparency button and footer,
but the numbered grid was missing, so there was nothing to click. The `/terminal` practice panel showed
the grid correctly.

## 2. Root cause

`com.arena.pekaclient.gui.TerminalScreen.drawPanel` drew the fourteen buttons inside this loop:

```java
int count = live == null ? COUNT : liveCount;   // COUNT = 14
for (int i = 0; i < count; i++) { ...draw one button... }
```

Everything else on the panel (frame, header, divider, transparency button, footer) is drawn outside that
loop, which is why exactly those parts stayed visible.

`liveCount` was produced by `syncLive()`, which asked `TerminalSolver` to match the live chest's slots to
the numbers 1..14 and counted how many numbers were found from 1 without a gap:

```java
int found = mapSlots(size, false);   // the number must be the item's display name, "1".."14"
if (found == 0) found = mapSlots(size, true);   // or a stained glass pane whose stack size is the number
liveCount = found;
```

Two things follow from that:

- `liveCount` is `0` whenever the chest's items match neither of the two readings the solver accepted.
  The chest is then never drawn, on any frame, while the rest of the panel is. That is the reported
  symptom, exactly.
- Even in a chest the solver could read, `liveCount` was recomputed from the chest on every frame, so the
  grid disappeared whenever the slots momentarily read as empty (the window is opened by the server and its
  contents arrive with it) or as soon as the terminal cleared its slots on completion.

The practice panel from `/terminal` never had a chest: it used `COUNT`, so `count` was always 14 and the
same code drew the grid. That is why the preview looked right and the in-game panel did not.

## 3. The fix

`src/main/java/com/arena/pekaclient/gui/TerminalScreen.java`

- The button loop now runs `for (int i = 0; i < COUNT; i++)`: all fourteen buttons are always drawn and
  their hit boxes are always the same fourteen cells. The live chest only decides which number is next,
  which are already clicked and where a click is sent.
- A number the chest does not show, and a chest that cannot be read at all, keep the panel's own count, so
  the grid stays correct and usable either way.
- Clicks are confirmed against the chest as well: a slot whose stack changed after a click, or a number
  that left the chest, counts as clicked, with a 400 ms grace window while the click is in flight.
- `R` restarts the panel in both modes (`Esc` still closes the live chest through the player so the server
  knows the window is closed), and the pixel scale is always restored, so no state leaks into the GUI
  drawn after the panel.
- The transparency toggle now fades the panel behind the buttons only; the buttons and their numbers are
  drawn at full opacity, so the toggle can no longer hide the grid.
- One line per panel is written to the log: which reading of the chest worked and how many numbers were
  mapped, with a one-time slot dump when nothing could be read.

`src/main/java/com/arena/pekaclient/dungeon/TerminalSolver.java`

- `numberOf` reads the number from the item first (display name, then lore, accepting a standalone one or
  two digit token only) and only then falls back to the stack size of a stained glass pane or glass block.
  Both terminal formats are now understood, and stacks that have already turned into the clicked lime pane
  are skipped, so their slot is never used as a click target.

Existing behaviour is preserved: the same panel, colours, layout, scaling, timer, restart, close and
transparency semantics, the `Click in order` HUD element, the `DungeonHudScreen` preview and the M7
settings switch. No other GUI was touched.

## 4. Verification

### 4.1 The rendering bug, reproduced and fixed (offline, real code)

`tools/terminal-verify/` runs the actual `TerminalScreen` and `TerminalSolver` from `src/main/java` with
`drawScreen`, `mouseClicked`, `keyTyped` and `TerminalSolver.onGuiOpen`, against Minecraft API stubs and a
pixel canvas instead of OpenGL (see `tools/terminal-verify/README.md`).

```
sh tools/terminal-verify/run.sh          -> RESULT after: passed=72 failed=0
sh tools/terminal-verify/run.sh before    -> RESULT before-all: passed=60 failed=12
```

The twelve failures of the older code include exactly the reported defect:

```
FAIL  live/unreadable (unreadable chest, 45 slots): fourteen tiles drawn   [0 of 14]
FAIL  live/unreadable (empty chest, 27 slots): fourteen tiles drawn        [0 of 14]
FAIL  practice: transparent keeps all fourteen tiles                        [0 of 14]
```

and `tools/terminal-verify/examples/before-missing-grid.png` is that first case rendered: title, header,
transparency button and footer present, panel body empty — the reported screenshot. The same scenario after
the fix draws 14 of 14 tiles (`examples/after-in-game.png`).

Checked after the fix, all passing:

- All fourteen buttons drawn at 320x240, 427x240, 640x360, 960x540 and 1920x1080, with the number glyph
  pixels present in every tile, tile 1 green and tile 2 yellow at their own cells.
- The in-game path: a 45 slot `GuiChest` titled `Click in order` is replaced by the panel through
  `GuiOpenEvent`, the panel draws all fourteen tiles, and a click on tile 1 is sent as a window click to
  the slot that holds number 1 (verified as `0:0:0:0` on the recorded controller).
- A wrong click counts a mistake, flashes the tile red and does not advance `Next`.
- Correct clicks advance `Next 1`, `Next 2`, ... `Next 14` and finish the run; the timer runs.
- The transparency toggle fades the panel (`#FF1D1D20` to `#6D4B4B52` on the panel body) while tile 1
  stays green and tile 3 stays black.
- `R` restarts, `Esc` closes.
- A chest the solver cannot read still shows the grid, still counts clicks and still sends nothing to the
  chest (the panel's own count is used), so the failure mode that produced the report is gone.

### 4.2 The release build

The 1.8.9 toolchain (ForgeGradle 2.1-SNAPSHOT, Gradle 2.14.1, JDK 8) needs network access to the Forge
Maven and Mojang's asset servers, which the sandbox this fix was written in does not have. The build
therefore ran in the environment the project already provides for it: GitHub Actions, JDK 8 (Temurin
1.8.0_504), the project's own wrapper (Gradle 2.14.1), `./gradlew clean build dumpTestClasspath`, then the
standalone tests, then the JAR checks. Workflow run
[38898460945](https://github.com/pekadeco/Sky-VeLor/actions/runs/38898460945) passed every step.

Tasks that produced the artifact (from `dist/BUILD-REPORT.txt`):

```
:deobfMcMCP SKIPPED
:jar
:reobfJar      <- the reobfuscated, distributable mod jar
:build
```

Artifact checks, all on the published file:

| Check | Result |
| --- | --- |
| File name | `SkyVeLor-1.8.9-Beta-0.10.6.5v.jar` |
| Size / SHA-256 | 309,872 bytes / `0ad01d92...61ac14`, matches `dist/BUILD-REPORT.txt` |
| Classes | 119, including `gui/TerminalScreen`, `gui/DungeonHudScreen`, `dungeon/TerminalSolver`, `hud/ClickInOrderHud`, `command/TerminalCommand` |
| `net/minecraft/` entries | 0 - it is the mod, not a Minecraft or `deobfed.jar` dependency |
| Intermediate jars | none, and no `deobf` entry anywhere in the archive |
| Reobfuscation | `javap` on the shipped `TerminalScreen.class` shows `drawScreen` as `func_73863_a`, i.e. SRG names |
| Fixed code inside the jar | `javap -p` shows `syncLive`, `justSent`, `changedSinceClick`, `sendClick`, `advance`, `signature`, `stackAt`; the old `mapSlots` and `pressLive` are gone |
| `mcmod.info` | `"version": "Beta-0.10.6.5v"`, `"mcversion": "1.8.9"` |
| Other resources | `assets/pekaclient/lang/en_US.lang` present |

The standalone test suite (`tests/run-tests.sh`, 24 test classes) passed in the same run.

### 4.3 What is not verified

Minecraft itself was not launched: there is no 1.8.9 client and no OpenGL in the environment this fix was
written in. The panel drawing and clicking above ran against the real screen and solver classes, but
in-game behaviour on a live server (a real F7/M7 terminal) is not verified and should be confirmed in
Minecraft 1.8.9 with Forge 11.15.1.2318 before publishing widely. The one panel log line described in
section 3 makes that easy to check from `logs/latest.log`.

## 5. Reproducing

```sh
sh tools/terminal-verify/run.sh            # offline GUI checks, all must pass
./gradlew clean build dumpTestClasspath    # JDK 8: builds dist jar and the test classpath
TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
```

The release jar is written to `build/libs/SkyVeLor-1.8.9-Beta-0.10.6.5v.jar` and is published by the
`Build Sky VeLor` workflow into `dist/` and as an asset on the `SkyVeLor-Beta-0.10.6.5v` release.

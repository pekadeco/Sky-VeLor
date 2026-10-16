# Terminal GUI offline verification

This folder is a test rig for the Terminal GUI, not part of the mod. Nothing here is compiled into
`SkyVeLor-1.8.9-Beta-0.10.6.5v.jar`: Gradle only builds `src/main/java`, so these files never reach
the released mod.

## What it does

`harness/com/arena/pekaclient/gui/TerminalVerify.java` runs the real
`com.arena.pekaclient.gui.TerminalScreen` from `src/main/java`: it calls the same `drawScreen`,
`mouseClicked` and `keyTyped` methods Minecraft calls, and drives
`com.arena.pekaclient.dungeon.TerminalSolver.onGuiOpen` with a real `GuiChest` and `ContainerChest`
the way the game does when a Click in order terminal is opened.

Two things are replaced, and only these two:

- `stubs/` - the small set of Minecraft 1.8.9 and Forge classes the two terminal classes use, so the
  rig compiles and runs without the game.
- `harness/.../verify/Raster.java` - the rasteriser. `Gui.drawRect` and `FontRenderer.drawString` send
  their pixels here, and rectangles are composited with the same source-over alpha blending the 1.8.9
  GUI uses, at the scale `GlStateManager` reports. The result is a PNG of the panel exactly where the
  game would draw it, and every probe point is read back from that canvas.

## Running it

```sh
sh run.sh              # the current source: all checks must pass
sh run.sh before       # the same checks against the older screen, to see the missing grid
```

`run.sh` compiles with `javac` (JDK 8 is what the project uses) and writes `out/<mode>/` with
`report-after.txt` / `report-before-all.txt` and the rendered pictures. To test another copy of the
code, point it at it:

```sh
SRC=/path/to/TerminalScreen.java SRC2=/path/to/TerminalSolver.java sh run.sh before
```

`gen_stubs.py` regenerates `stubs/` from its own source; the stubs are checked in so the rig runs
without python3 as well.

## What is checked

- The fourteen buttons are drawn, twice seven, at five window sizes from 320x240 to 1920x1080, with
  the number glyph pixels present in every tile.
- Tile 1 is green, tile 2 yellow, the rest black, and a wrong click flashes red and counts a mistake.
- The transparency toggle fades the panel behind the buttons and leaves the buttons and numbers
  unchanged.
- Clicking follows the layout: the click on tile 1 is sent to the chest slot that holds number 1, a
  wrong click sends nothing, and the header advances `Next 1` to `Next 14` and then to `Done`.
- `R` restarts and `Esc` closes.
- A chest the solver cannot read still shows and uses the grid, which is the case that was broken.

## Pictures

`examples/` holds rendered pictures from both runs:

| File | What it shows |
| --- | --- |
| `before-missing-grid.png` | The reported bug: the panel, header, button and footer draw, the numbered grid does not. |
| `after-in-game.png` | The same in-game path after the fix: all fourteen buttons, 1 green and 2 yellow. |
| `after-practice.png` | The `/terminal` practice panel. |
| `after-transparent.png` | The transparency toggle: the panel fades, the grid does not. |

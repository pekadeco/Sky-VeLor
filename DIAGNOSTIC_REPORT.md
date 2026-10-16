# Sky VeLor source repair and original-style animation report

## Source repairs retained

- Restored the missing `com.arena.pekaclient.widget` classes used by existing screens: `Widget`, `AppleButton`, `AppleDropdown`, `AppleSegmented`, `AppleSlider`, `AppleToggle`, and `SearchField`.
- Restored `com.arena.pekaclient.startup.StartupTasks` and added the missing Forge resource metadata and English keybind labels.
- Kept the `com.arena.pekaclient` package and `pekaclient` Forge mod ID to avoid needless compatibility changes.
- Added/retained standalone interaction tests and Windows/Linux test runners. Generated build/cache output is excluded from the source package.

## Original appearance preserved in this revision

- `AppleButton` keeps its original bounds, corner radius, fill colors, label alignment, and non-primary border. Hover/press color changes now ease over time; no scaling, additional border, glow, shadow, or click flash is drawn.
- `AppleSlider` keeps the original 2 px track radius, 4 px knob radius, track colors, text, and value label. Only the knob/fill position follows value changes with easing; the displayed numeric value remains the actual setting.
- `AppleToggle` keeps the original track, knob dimensions, colors, and drag behavior. The knob position and track color transition between the same original off/on states.
- `AppleSegmented` keeps its original outer background, border, selected-pill size/radius/fill, and text alignment. Only the existing selected pill's position and selected/unselected text colors interpolate while selection changes.
- `AppleDropdown` keeps its original control geometry, border, arrow, hover fill endpoint, popup styling, and selected-row treatment. Hover color and popup reveal are eased without adding new visual treatments to the control itself.
- `SearchField` keeps its original size, radius, caret, fill colors, and one-pixel border. Hover fill and focus border transition smoothly between their original states; added shadow and underline treatments were removed.
- The menu entry and Quick Note buttons use Minecraft 1.8.9's original `GuiButton` texture and text colors, with a cross-fade between the normal and hover texture states. Their original coordinates and dimensions have been restored (84x20 for the menu entry; 62x18 for Quick Note footer buttons; 78x16 for the new-page button).
- Window-control dots keep their original radius and colors; only the already-existing hover glyphs fade in/out.
- All these animations respect the existing `Theme.animations` setting and speed where applicable. No shaders, mixins, or new external dependencies were added.

## Verification performed

- Compiled the changed widget classes, `AppleGuiButton`, and `WindowDots` with Java 8 source/target settings against compile-only Minecraft/Forge API stubs. The stubs are not included in the project ZIP.
- Ran the targeted standalone suites: `SearchFieldTests` (14 passed, 0 failed) and `WidgetInteractionTests` (10 passed, 0 failed): 24 relevant checks passed.
- Confirmed the mod menu and Quick Note call sites use the original button sizes again, and all call sites match the restored `AppleGuiButton` constructor.

## Remaining verification limits

- The official `gradlew.bat clean build dumpTestClasspath` build was not run for this revision. This legacy ForgeGradle 2.1 / Gradle 2.14.1 project requires JDK 8 and a network connection to resolve its dependencies; the current inspection environment does not provide that setup.
- The Minecraft client was not launched. The appearance and animations should still be checked in Minecraft 1.8.9 with Forge before release.

## License note

`LICENSE` contains MIT terms while `LICENSE.txt` contains LGPL v3 terms. Both were retained; confirm which terms apply to each component before redistributing the mod.

## Terminal GUI fix (Beta-0.10.6.5v)

The Beta-0.10.6v source archive added a Terminal GUI whose numbered grid was missing inside the game while
the panel, header, transparency button and footer were drawn. The cause was in `gui/TerminalScreen`: the
loop that drew the fourteen buttons ran `for (int i = 0; i < count; i++)` with
`count = live == null ? COUNT : liveCount`, and `liveCount` was the number of chest slots that could be
matched to a number 1..14 by the terminal solver. When the live chest could not be read (the slot items are
plain stained glass panes whose stack size is the number, which the solver did not accept, or the slots are
empty at the moment the screen is first built), `liveCount` was 0 and the loop drew nothing at all: the panel
appeared with an empty body. The practice panel from `/terminal` always used `COUNT`, so the same code looked
correct in the preview.

The fix:

- `gui/TerminalScreen` always draws all fourteen buttons and always keeps their hit boxes; the live chest only
  decides which number is next, which numbers are already clicked, and which slot a click is sent to.
- A number the chest cannot show is now counted by the panel itself, so the grid stays usable either way.
- `dungeon/TerminalSolver` now reads the number from the item name or lore first (with a standalone one or two
  digit token check), and falls back to the stack size of a stained glass pane/glass block, skipping stacks
  that have already turned into the clicked lime pane. Both readings are supported, which is what the Floor
  VII / M7 terminal needs.
- Clicks are confirmed against the chest as well: a slot that changed or a number that left the chest after a
  click was sent counts as clicked, with a 400 ms grace window while the click is in flight.
- `R` restarts the panel in both modes, `Esc` still closes the live chest through the player so the server
  knows the window is closed, and the pixel scale is applied and restored around the panel so no scale leaks
  into the rest of the GUI.
- The Transparent button fades the panel behind the buttons only: the button colours and numbers are drawn at
  full opacity, so the grid stays readable in either mode.
- One line is written to the log per panel: whether the chest was read from the item names, from the pane
  stack sizes, or not at all, and how many numbers were mapped. When nothing is found, the chest slots are
  listed once, so a live terminal can be diagnosed from the log.

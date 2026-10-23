# Hub and editor reachability — Beta-0.10.8.9.2v

This document audits the source UI routes for the Beta 0.10.8.9.2 update. It distinguishes direct controls from the existing one-level dropdown/editor routes and records the responsive Dungeon ESP editor behavior. It is source documentation; distributable artifacts still require the Java 8 Forge build and release verifier.

## Hub contract

Each visible Hub feature uses one of these non-nested forms:

1. **Toggle** — one direct On/Off control.
2. **Slider** — one direct bounded value control, currently **Hurt Camera Shake** at 0–100%.
3. **Settings** — one direct **Settings** button for a feature with no meaningful all-or-nothing master switch.
4. **Dropdown** — one non-nesting header with its accurate green/red status dot; its body has exactly **On/Off** and one **Settings** button.

There are no nested dropdowns, tree connector lines, or Inventory Search field rows. Section captions separate direct controls from editor routes; they are not controls.

## Feature / row type / option-location audit

| Tab | Feature | Row type | Current option location |
| --- | --- | --- | --- |
| General | Appearance | Dropdown | Appearance Settings editor |
| General | Mod Settings | Dropdown | Mod Settings screen |
| General | Performance | Dropdown | Performance Settings |
| Quality of Life | No Block Particles | Toggle | this tab's toggle |
| Quality of Life | No Hotbar Scrolling | Toggle | this tab's toggle |
| Quality of Life | Hurt Camera Shake | Slider | this tab's 0–100% slider |
| Quality of Life | Auto Sprint | Toggle | this tab's toggle |
| Quality of Life | Inventory Assist | Settings | Inventory Assist Settings (tooltips, potion panel, rarity) |
| Quality of Life | Movement and Shortcuts | Dropdown | Movement and Shortcuts Settings |
| Quality of Life | Item Lock and Swap | Dropdown | Item Lock and Swap Settings |
| Quality of Life | Chat QoL | Dropdown | Chat Settings |
| Quality of Life | Commands | Dropdown | Commands Settings |
| HUD | Item Animation | Dropdown | Item Animation Settings |
| HUD | Stats Display | Dropdown | HUD editor (Frame Rate, Mining, Memory cards) |
| HUD | Player Doll | Dropdown | Player Doll Settings |
| HUD | Inventory HUD | Dropdown | Inventory HUD Settings |
| Farming | Mining BPS | Dropdown | Mining BPS Settings |
| Farming | Mining Assistant | Dropdown | Mining Assistant Settings; its HUD route remains there |
| Dungeon | Door ESP | Dropdown | Door ESP Settings |
| Dungeon | Mob ESP | Dropdown | Mob ESP Settings |
| Dungeon | Dungeon Sync | Toggle | this tab's toggle |
| Dungeon | Floor VII (M7) Overlay | Dropdown | Floor VII (M7) Overlay Settings |
| Dungeon | Spirit Leap GUI | Dropdown | Spirit Leap GUI Settings |
| Dungeon | Terminal Assist | Dropdown | Terminal Assist Settings |
| Dungeon | Boss HP | Dropdown | Boss HP Settings |
| Dungeon | Dungeon Map | Dropdown | Dungeon Map Settings |
| Dungeon | Sync Details | Toggle | this tab's toggle |
| Slayer | Slayer Tracker | Dropdown | Slayer Tracker Settings |
| Slayer | Slayer HUD | Dropdown | Slayer HUD Settings |
| Fishing | Lord Jawbus | Toggle | this tab's toggle |
| Fishing | Nearby Encounters | Toggle | this tab's toggle |
| Fishing | Catch Counter | Dropdown | Catch Counter Settings |
| Fishing | Fishing HUD | Dropdown | Fishing HUD Settings |
| Fishing | Sea Creature HP | Dropdown | Sea Creature HP Settings |

`FeatureSettingsUiModel` is the source-level audit inventory for visible features and existing option labels. It proves one-level dropdown depth, direct slider/settings ownership, editor-route ownership, status-dot colors, and no duplicate option identifiers. `DungeonEspUiModel` remains a non-rendered legacy option inventory so former Dungeon ESP options still resolve to their editor route.

## Retired surfaces and compatibility

- **Hurt Camera Shake** persists under `assist.visual.hurtCameraShake`; old binary No Hurt Cam keys map once to 0%/100% and are omitted on the next save.
- **Inventory Search** has no feature row, option id, text-input/filter hook, or persisted setting. Tooltip conversion/scrolling, rarity overlays, and optional potion-panel layout remain under Inventory Assist.
- **Player Dollars** has no HUD element or editor route. Old `dollars.*`, `balance.source`, and `balance.manual` keys are ignored so remaining HUD settings can load normally.
- The default-off **Stats Display** replaces the retired individually placed FPS/Ping/Memory HUD surface. Old `fps-display.*`, `ping-display.*`, and `memory-display.*` keys are ignored; General-tab Ping telemetry remains independent.

## Verification scope

Headless tests cover shake persistence at 0/intermediate/100%, the one-time hurt-tilt transformer hook, removed class/control surfaces, normal Inventory HUD retention, Stats labels/layout, and the pure GuiChat close transition. They do not start Minecraft; render behavior and the actual Escape/send chat paths still require an in-game Forge 1.8.9 pass.

## Mob and Door ESP editors

- The left pane is a flat item list. **Mob ESP** has Global, Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor, and Other Mobs; each item has its existing individual toggle and compact hitbox thumbnail.
- **Door ESP** has Global, Wither Doors, and Blood Doors; each uses its existing toggle and compact cached-prism thumbnail.
- The selected settings pane owns detailed customization. Mob Global owns master/performance/presets plus **Mob Category Labels** and **Maximum Mob Labels**. Category style pages retain color, mode, through-walls, width, opacity, copy/apply, Reset, and **Apply This Style to All**. Door filters and shared style stay in the Door editor.
- On wide GUI scales, the selected settings column sits beside a clipped Live Preview. On compact scales, the preview yields to one full-width, scrollable settings column so sliders/dropdowns remain usable; all categories remain reachable through the independently scrollable flat list.
- Compact thumbnails and the larger preview both call `DungeonEspPreviewRenderer` and the existing `DungeonEspRenderBatch.drawStyledBox` path. No dummy world or entity is created. The live mob renderer uses the same category color/style for its bounded badges; it always restores its world GL state after drawing.

## Compatibility and verification

- Existing feature/config owners remain unchanged. Old `ui.feature.tree.*` presentation values are accepted and ignored; editor selections continue under `ui.feature.editor.*`.
- Headless coverage checks direct slider/settings routes and one-level dropdown bodies, editor routes, Mob/door categories and toggles, responsive preview/control geometry, live widget-to-setting ownership, label persistence/bounds, status dots, GUI-scale non-overlap, old-config handling, and the Beta 0.10.8.9.2 Dungeon ESP paths.
- This source update is **NOT tested in-game**; generated artifacts require the Java 8 Forge build and verifier.

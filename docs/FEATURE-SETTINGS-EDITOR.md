# Flat Hub and editor reachability — Beta-0.10.8.8.1v

This release changes presentation only. It does **not** change gameplay behavior, setting keys, values, defaults, HUD placement, or feature ownership.

## Hub contract

Every feature in a Hub tab uses one of two flat forms:

1. **Simple** — one row with its existing On/Off toggle, feature name, and one gray description.
2. **Dropdown** — one non-nesting header with title and accurate green/red status dot. Its body has exactly **On/Off** and one **Settings** button.

Simple rows come before dropdowns in each tab. There are no tree arrows, connector lines, indentation, tab-level per-mob/per-door rows, nested dropdowns, or child-row settings buttons. Section captions only separate the two permitted forms; they are not controls.

## Feature / row type / option-location audit

| Tab | Feature | Row type | Existing option location |
| --- | --- | --- | --- |
| General | Appearance | Dropdown | Appearance Settings editor |
| General | Mod Settings | Dropdown | existing Mod Settings screen |
| General | Performance | Dropdown | Performance Settings |
| Quality of Life | No Block Particles | Simple | this tab's toggle |
| Quality of Life | No Hotbar Scrolling | Simple | this tab's toggle |
| Quality of Life | Inventory Assist | Dropdown | Inventory Assist Settings |
| Quality of Life | Movement and Shortcuts | Dropdown | Movement and Shortcuts Settings |
| Quality of Life | Item Lock and Swap | Dropdown | Item Lock and Swap Settings |
| Quality of Life | Chat QoL | Dropdown | Chat Settings |
| Quality of Life | Commands | Dropdown | Commands Settings |
| HUD | Item Animation | Dropdown | Item Animation Settings |
| HUD | Player Doll | Dropdown | Player Doll Settings |
| HUD | Inventory HUD | Dropdown | Inventory HUD Settings |
| HUD | Player Dollars | Dropdown | Player Dollars Settings |
| Farming | Mining BPS | Dropdown | Mining BPS Settings |
| Farming | Mining Assistant | Dropdown | Mining Assistant Settings; its HUD route remains there |
| Dungeon | Door ESP | Dropdown | Door ESP Settings |
| Dungeon | Mob ESP | Dropdown | Mob ESP Settings |
| Dungeon | Dungeon Sync | Simple | this tab's toggle |
| Dungeon | Floor VII (M7) Overlay | Dropdown | Floor VII (M7) Overlay Settings |
| Dungeon | Spirit Leap GUI | Dropdown | Spirit Leap GUI Settings |
| Dungeon | Terminal Assist | Dropdown | Terminal Assist Settings |
| Dungeon | Boss HP | Dropdown | Boss HP Settings |
| Dungeon | Dungeon Map | Dropdown | Dungeon Map Settings |
| Dungeon | Sync Details | Simple | this tab's toggle |
| Slayer | Slayer Tracker | Dropdown | Slayer Tracker Settings |
| Slayer | Slayer HUD | Dropdown | Slayer HUD Settings |
| Fishing | Lord Jawbus | Simple | this tab's toggle |
| Fishing | Nearby Encounters | Simple | this tab's toggle |
| Fishing | Catch Counter | Dropdown | Catch Counter Settings |
| Fishing | Fishing HUD | Dropdown | Fishing HUD Settings |
| Fishing | Sea Creature HP | Dropdown | Sea Creature HP Settings |

`FeatureSettingsUiModel` is the source-level audit inventory for the visible features and their existing option labels. It proves one-level dropdown depth, the exact two-control dropdown body, editor route ownership, simple-row ownership, status-dot colors, and no duplicate option identifiers. `DungeonEspUiModel` remains a non-rendered 67-control legacy option inventory so each former Dungeon ESP option is still checked against the editor route.

## Mob and Door ESP editors

- The left pane is a flat item list. **Mob ESP** has Global, Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor, and Other Mobs; each item has its existing individual toggle and compact hitbox thumbnail.
- **Door ESP** has Global, Wither Doors, and Blood Doors; each uses its existing toggle and compact cached-prism thumbnail.
- The selected right pane owns detailed customization. Mob Global owns master/performance/presets. Category style pages retain color, mode, through-walls, width, opacity, copy/apply, Reset, and **Apply This Style to All**. Door filters and shared style stay in the Door editor.
- Compact thumbnails and the larger preview both call `DungeonEspPreviewRenderer` and the existing `DungeonEspRenderBatch.drawStyledBox` path. No dummy world or entity is created.

## Compatibility and verification

- Existing feature/config owners remain unchanged. Old `ui.feature.tree.*` presentation values are accepted and ignored; editor selections continue under `ui.feature.editor.*`.
- Headless coverage checks dropdown depth/body contract, simple rows, old-option reachability, editor routes, Mob/door categories and toggles, status dots, GUI-scale non-overlap, and old-config loading.
- This release was **NOT tested in-game**.

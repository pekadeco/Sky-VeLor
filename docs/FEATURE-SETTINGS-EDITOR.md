# Flat Hub and editor reachability — Beta-0.10.8.8.1v

This release changes presentation only. It does **not** change gameplay behavior, setting keys, values, defaults, HUD placement, or feature ownership.

## Hub contract

Every feature in a Hub tab uses one of two flat forms:

1. **Simple** — one row with its existing On/Off toggle, feature name, and one gray description.
2. **Dropdown** — one non-nesting header with title and accurate green/red status dot. Its body has exactly the feature's existing On/Off toggle and one **Open _Name_ Editor** button.

Simple rows come before dropdowns in each tab. There are no tree arrows, connector lines, indentation, tab-level per-mob/per-door rows, nested dropdowns, or child-row settings buttons. Section captions only separate the two permitted forms; they are not controls.

## Feature / row type / option-location audit

| Tab | Feature | Row type | Existing option location |
| --- | --- | --- | --- |
| General | Appearance | Dropdown | Appearance Settings editor |
| General | Mod Settings | Dropdown | existing Mod Settings screen |
| General | Fonts | Dropdown | existing Font Settings screen |
| General | Performance | Dropdown | Performance Settings editor |
| Quality of Life | No Block Particles | Simple | this tab's toggle |
| Quality of Life | Disable Hotbar Scrolling | Simple | this tab's toggle |
| Quality of Life | Inventory Assist | Dropdown | Inventory Assist Settings editor |
| Quality of Life | Item Protection | Dropdown | Item Protection Settings editor |
| Quality of Life | Movement and Shortcuts | Dropdown | Movement and Shortcuts editor |
| Quality of Life | Item Lock and Swap | Dropdown | Item Lock & Swap Settings editor |
| Quality of Life | Chat QoL | Dropdown | existing Chat Editor |
| Quality of Life | Commands | Dropdown | Command Settings editor |
| HUD | Item Animation | Dropdown | existing HUD Editor, Item Animation panel |
| HUD | Player Doll | Dropdown | existing HUD Editor, Player Doll inspector |
| HUD | Inventory HUD | Dropdown | existing HUD Editor, Inventory inspector |
| HUD | Player Dollars | Dropdown | existing HUD Editor, Player Dollars inspector |
| HUD | Hotbar | Dropdown | existing HUD Editor, Hotbar inspector |
| Farming | Mining BPS | Dropdown | existing HUD Editor, Mining BPS inspector |
| Farming | Mining Assistant | Dropdown | Mining Assistant Settings editor; its HUD route remains there |
| Dungeon | Enable Dungeon Sync | Simple | this tab's toggle |
| Dungeon | Dungeon Sync Debug | Simple | this tab's toggle |
| Dungeon | Spirit Leap GUI | Dropdown | Spirit Leap Settings editor |
| Dungeon | Dungeon Map | Dropdown | existing HUD Editor, Dungeon Map inspector |
| Dungeon | Boss HP | Dropdown | existing HUD Editor, Boss HP inspector |
| Dungeon | Terminal Assist | Dropdown | Terminal Assist Settings editor |
| Dungeon | Mob ESP | Dropdown | Mob ESP Settings editor |
| Dungeon | Door ESP | Dropdown | Door ESP Settings editor |
| Dungeon | Floor VII (M7) Overlay | Dropdown | Floor VII Overlay Settings editor |
| Slayer | Slayer Tracker | Dropdown | Slayer Settings editor |
| Slayer | Slayer HUD | Dropdown | existing HUD Editor, Slayer inspector |
| Fishing | Lord Jawbus | Simple | this tab's toggle |
| Fishing | Nearby Encounters | Simple | this tab's toggle |
| Fishing | Catch Counter | Dropdown | Catch Counter Settings editor |
| Fishing | Fishing HUD | Dropdown | existing HUD Editor, Fishing inspector |
| Fishing | Sea Creature HP | Dropdown | existing HUD Editor, Sea Creature HP inspector |

`FeatureSettingsUiModel` is the source-level audit inventory for these 35 visible features and 190 existing option labels. It proves one-level dropdown depth, the exact two-control dropdown body, editor route ownership, simple-row ownership, status-dot colors, and no duplicate option identifiers. `DungeonEspUiModel` remains a non-rendered 67-control legacy option inventory so each former Dungeon ESP option is still checked against the editor route.

## Mob and Door ESP editors

- The left pane is a flat item list. **Mob ESP** has Global, Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor, and Other Mobs; each item has its existing individual toggle and compact hitbox thumbnail.
- **Door ESP** has Global, Wither Doors, and Blood Doors; each uses its existing toggle and compact cached-prism thumbnail.
- The selected right pane owns detailed customization. Mob Global owns master/performance/presets. Category style pages retain color, mode, through-walls, width, opacity, copy/apply, Reset, and **Apply This Style to All**. Door filters and shared style stay in the Door editor.
- Compact thumbnails and the larger preview both call `DungeonEspPreviewRenderer` and the existing `DungeonEspRenderBatch.drawStyledBox` path. No dummy world or entity is created.

## Compatibility and verification

- Existing feature/config owners remain unchanged. Old `ui.feature.tree.*` presentation values are accepted and ignored; editor selections continue under `ui.feature.editor.*`.
- Headless coverage checks dropdown depth/body contract, simple rows, old-option reachability, editor routes, Mob/door categories and toggles, status dots, GUI-scale non-overlap, and old-config loading.
- This release was **NOT tested in-game**.

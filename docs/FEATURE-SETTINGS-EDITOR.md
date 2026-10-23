# Hub and editor reachability — Beta-0.10.8.9.3v

This document audits the source UI routes for Beta 0.10.8.9.3. It records the restored single-page expandable Hub and the existing focused editors that still own every detailed setting. It is source documentation; distributable artifacts still require the Java 8 Forge build and release verifier.

## Restored Hub contract

The main Hub is one scrollable settings page. It has no sidebar, segmented tab strip, feature tree, nested feature dropdown, or per-feature status-dot navigation.

- The only expansion level is a historical-style category header: chevron, title, and direct-row count.
- The display order is **Quality of Life**, **HUD Hub Editor**, **General**, **Farming**, **Dungeon**, **Slayer**, and **Fishing**. Quality of Life opens on a fresh Hub session.
- Direct rows contain exactly one existing toggle, slider, or named **Open** route. Search temporarily reveals matching rows from closed categories without changing expansion state.
- **Item Animation** is a named Quality of Life tool. Its established Item Animation editor remains its sole renderer-control surface.
- **HUD Hub Editor** is the named expandable HUD category; its distinct **HUD Editor** route opens the existing direct-canvas editor. HUD elements retain their own visibility switch and one placement/style route.

## Feature / row type / option-location audit

| Expandable section | Feature | Main Hub row | Detailed option location |
| --- | --- | --- | --- |
| Quality of Life | No Block Particles | Toggle | Hub row |
| Quality of Life | No Hotbar Scrolling | Toggle | Hub row |
| Quality of Life | Hurt Camera Shake | 0–100% slider | Hub row |
| Quality of Life | Auto Sprint | Toggle | Hub row |
| Quality of Life | Item Animation | Open route | Item Animation editor |
| Quality of Life | Inventory Assist | Open route | Inventory Assist Settings |
| Quality of Life | Movement and Shortcuts | Open route | Movement and Shortcuts Settings |
| Quality of Life | Item Lock and Swap | Open route | Item Lock and Swap Settings |
| Quality of Life | Chat QoL | Open route | Chat Settings |
| Quality of Life | Commands | Open route | Commands Settings |
| HUD Hub Editor | HUD Editor | Open route | Existing direct-canvas HUD editor |
| HUD Hub Editor | Stats Display | Visibility toggle + Open route | HUD editor — Frame Rate, Mining, Memory cards |
| HUD Hub Editor | Player Doll / Inventory HUD | Visibility toggle + Open route | HUD editor |
| General | FPS / Ping / Memory | Read-only GUI cards | General category only; not an in-game HUD |
| General | Appearance / Mod Settings / Performance | Open routes | Their existing focused editors |
| Farming | Mining BPS | Visibility toggle + Open route | HUD editor |
| Farming | Mining Assistant | Open route | Mining Assistant Settings |
| Dungeon | Door ESP / Mob ESP | Open routes | Existing ESP editors |
| Dungeon | Dungeon Sync / Sync Details | Toggles | Hub rows |
| Dungeon | M7 Overlay / Spirit Leap / Terminal Assist | Open routes | Their focused editors |
| Dungeon | Boss HP / Dungeon Map | Visibility toggle + Open route | HUD editor |
| Slayer | Slayer Tracker / Slayer HUD | Open routes / HUD route | Existing editor / HUD editor |
| Fishing | Jawbus / Nearby Encounters | Toggles | Hub rows |
| Fishing | Catch Counter / Fishing HUD / Sea Creature HP | Open routes / HUD routes | Existing editor / HUD editor |

`FeatureSettingsUiModel` is the source-level audit inventory for visible features and existing option labels. It proves direct-row/editor-route ownership, no duplicate option identifiers, zero feature-dropdown depth, section reachability, and complete legacy-option routing. `DungeonEspUiModel` remains a non-rendered legacy option inventory so former Dungeon ESP options still resolve to their editor route.

## Stats Display restoration and compatibility

- The only restored **in-game** metric group is **Stats Display**: **FRAME RATE**, **MINING**, and **MEMORY**. The middle card uses the existing mining tracker and shows bounded `BPS`, not ping.
- The existing General-category FPS/Ping/Memory cards remain a read-only settings-GUI telemetry view. They are not `HudElement`s and cannot create an in-game duplicate of Stats Display.
- It uses the historic fixed three-card geometry: 172 × 102 cards, 9 px gaps, 8 px cut corners, a 2 px dark offset edge, centered near the top of the HUD. Frame rate and Mining values are cyan; Memory is white.
- Stats Display remains opt-in on a fresh configuration. The HUD Hub Editor exposes its visibility switch and direct editor route, and the HUD editor shows disabled elements as selectable faded placeholders.
- Legacy visible `fps-display.*`, `ping-display.*`, or `memory-display.*` configurations migrate once to `stats.enabled=true` only when no explicit modern `stats.enabled` choice exists. The retired keys are then ignored, so no duplicate metric renderer is registered. An explicit modern false value wins.
- **Player Dollars** remains absent. Old `dollars.*`, `balance.source`, and `balance.manual` keys are ignored so active HUD settings load normally.

## Dungeon ESP and other retained behavior

- Existing Dungeon ESP detection, entity filters, styles, cached doors, badges, live preview, and GL-state restoration remain unchanged. Mob ESP retains Global, Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor, and Other Mobs. Door ESP retains Global, Wither Doors, and Blood Doors.
- On wide GUI scales the focused ESP editor keeps its clipped Live Preview beside controls; on compact scales it gives settings one full-width scrollable column. All categories and controls remain reachable.
- Existing configuration owners and key names remain unchanged. Old `ui.feature.tree.*` presentation values are accepted and ignored; editor selections continue under `ui.feature.editor.*`.

## Verification scope

Headless tests cover section/order contracts, direct routes, editor ownership, persisted configuration compatibility, row flow/scale geometry, scroll behavior, Dungeon ESP routes, Stats labels/geometry/default placement, legacy metric visibility migration, and no retired metric HUD registration. They do not start Minecraft; rendering and gameplay still require an in-game Forge 1.8.9 pass.

This source update is **NOT tested in-game**. Generated artifacts require the Java 8 Forge build, standalone test suite, release-JAR verifier, and source-ZIP verifier.

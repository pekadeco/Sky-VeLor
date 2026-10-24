# Inline settings inventory — Beta-0.10.8.9.7v

Sky VeLor uses its native Minecraft 1.8.9 Forge GUI. The approved
`analyze-sky-velor-mod-code.zip` reference informed the presentation contract only; the release
ships no browser runtime, React code, or third-party GUI framework.

## Settings contract

`AppleSettingsScreen` is the single visible feature-settings surface for the keybind, `/sv`,
and menu entry points. Its fixed sidebar order is:

**Home → Gameplay → Dungeon → Farming → Mining → Fishing → Visuals → System**.

Every feature owner is a collapsed-by-default inline dropdown. The presentation state is stored
separately under `ui.feature.dropdown.<catalog-id>` by `FeatureUiState` /
`FeatureUiPreferences`; it never replaces or migrates a feature configuration key. A missing
presentation value means closed. Search indexes nested labels, ids, descriptions, and keywords;
choosing a result changes to its tab and opens every parent dropdown needed to reveal it.

Feature rows use the standard small right-aligned `AppleToggle` pill (`38 × 20` GUI pixels) or a
disclosure dropdown. A disabled master dims dependent rows without disabling their established
controls, so users can adjust a value before re-enabling its owner. Sliders, choices, text fields,
reset/copy actions, and the two permitted editor launches appear only inside the relevant dropdown.

Only these settings actions open a screen:

1. **HUD Editor** — the existing HUD layout editor, including the existing dungeon-map canvas and
   HUD-element placement controls.
2. **Item Animation Editor** — the existing first-person renderer editor, including the real
   hand controls and real **Preview Swing** action.

There are no feature-specific settings screens, detail screens, controls-launch buttons, or
map-settings-launch buttons. Existing key binding instructions point to Minecraft Controls.

## Old-to-new feature table

| Previous presentation owner | New inline location | Retained values / actions |
| --- | --- | --- |
| Appearance focused editor | System → Appearance | Corner radius and panel opacity switches/values; Reset Appearance |
| Performance focused editor | System → Performance | Curve quality, shadows, animation switch/speed, adaptive quality, performance boost; Reset Performance |
| Mod Settings focused editor | System → Interface | Scroll speed and smooth scrolling; Reset Interface |
| Inventory Assist focused editor | Gameplay → Inventory and Chat → Inventory Assist | Potion panel, Roman numerals, tooltip scrolling, rarity highlight; Reset Inventory Assist |
| Item Lock / Swap focused editor | Gameplay → Inventory and Chat → Item Lock and Swap | Master, existing Controls guidance, cooldown, badges, line options, clear locks/pairs, full reset |
| Chat editor | Gameplay → Inventory and Chat → Chat QoL | Background, Messages, Tabs, Menu and Highlight, Reset; custom-tab add/edit/order/delete and group/all resets |
| Movement shortcuts focused editor | Gameplay → Inventory and Chat → Movement and Shortcuts | Wardrobe and Pets switches; Reset Movement Shortcuts |
| Slayer focused editor | Gameplay → Inventory and Chat → Slayer Tracker | Master, Boss Timer, Price Drops, `/setprice` guidance, per-family history clears, reset |
| Dungeon Map route | Dungeon → Run Tools → Dungeon Map | Master, rotation, names, puzzle labels, scale/zoom, party announcement; placement remains in HUD Editor |
| Terminal Helper focused editor | Dungeon → Run Tools → Terminal Helper | No-master status dropdown: visual helpers, auto click, hide/prevent incorrect choices, waypoints, scale, reset |
| Spirit Leap focused editor | Dungeon → Run Tools → Spirit Leap Picker | Master, key switch, reset |
| Boss HUD route | Dungeon → Run Tools → Boss Health HUD | Master and HUD Editor placement guidance |
| M7 focused editor | Dungeon → Floor VII → Floor VII Overlay | Overlay, relic areas, dragon alert, Click in Order, Boss Hitboxes, Dragon Hitboxes, reset |
| Dungeon ESP focused editors | Dungeon → Dungeon ESP → Mob ESP / Door ESP | Existing global limits and presets; compact per-Mob style tables; shared Door Style and Blood Door colour; no sample panel |
| Dungeon Sync route | Dungeon → Party Sync | Master, diagnostics, existing endpoint guidance |
| Mining Assistant route | Mining → Mining Speed / Mining Assistant | BPS HUD switch, ability alert, Puzzler helper, assistant HUD, reset |
| Catch/Fishing routes | Fishing → Fishing Tracker | Catch Counter/Festival/reset, Sea Creature Health HUD, Jawbus and nearby encounter alerts |
| HUD routes | Visuals → HUD | HUD Editor launch plus Player Doll and Inventory HUD owners |
| Item Animation route | Visuals → Item Animation | Master and Item Animation Editor launch; all renderer controls remain in that permitted editor |
| Commands focused editor | System → Commands | `/sv`, `/qn`, diagnostics guidance, reset |

## Channel placement

- **Mining**: Mining Speed/BPS and Mining Assistant moved from the broader Gameplay presentation
  channel to **Mining** only. Their existing `HudManager`/`AssistSettings` owners and keys remain
  unchanged.
- **Fishing**: Catch Counter, Sea Creature Health, Jawbus, and nearby-encounter controls moved to
  **Fishing** only. Their `AppleSettings`, `FishingStats`, and HUD owners remain unchanged.
- **Farming**: the repository has no genuine crop/farming feature to move. It deliberately shows
  only **No farming features yet**; no crop automation, macros, auto-clicking, auto-breaking, or
  movement behavior was introduced.
- **Uncertain placement kept conservative**: Slayer remains Gameplay, Terminal/M7/ESP remain
  Dungeon, and HUD/Item Animation remain Visuals.

## Requested grouped surfaces

- **Chat QoL** contains exactly **Background**, **Messages**, **Tabs**, **Menu and Highlight**, and
  **Reset**. Custom tab fields are edited in Tabs and save directly through `ChatQolSettings`.
- **Terminal Helper** is deliberately a status dropdown rather than a synthetic master feature;
  each existing terminal setting remains independently opt-in.
- **Mob ESP** keeps the compact Global/Mob Category style hierarchy. Each category exposes color,
  render mode, Through Walls, outline width/opacity, fill opacity, and the existing session-only
  copy/apply actions.
- **Door ESP** keeps Door Scope, key/distance options, presets/reset, the single shared Door Style
  (color, mode, Through Walls, width, outline opacity, fill opacity), and Blood Door color. It does
  not invent Mob-only clipboard actions for doors.
- **Item Lock and Swap**, **Fishing**, **Dungeon Map**, **Spirit Leap**, **Boss HUD**, **M7**,
  **Auto Sprint**, and **Hurt Camera Shake** retain their original settings owners and runtime paths.

## Removed-only presentation code

Retired code consists solely of presentation/sample paths and obsolete focused-editor models:

- Chat sample/demo/send-test/automatic sample controls and their screen/model/layout.
- Inventory/Swap sample cards, Dungeon ESP hitbox sample panels, Door sample panels, and the
  Spirit Leap local sample data path.
- Generic focused feature-editor, settings-audit, chat, ESP, fishing, quality-of-life, and mod
  settings model/screen classes.

The Item Animation renderer remains intact. Its real `Preview Swing` starts only the temporary local
hand-render path; it does not send input, packets, or gameplay actions. The editor contains no held-item
sample card.

## Screenshot and shell behavior

The compact **Safe by default** notice is one line with its icon. The fullscreen control uses a
crisp corner-bracket glyph and persists only `settings.window.fullscreen`. Sky VeLor does not draw
or reserve external launcher MOUSE/GUI indicators; those belong to the launcher, not this client
GUI. The shell is rounded/clipped, draggable in small mode, and uses a single scroll viewport.

## Automated coverage and release boundary

Headless tests cover the eight-tab order, empty Farming channel, owner/config-key bridges,
old-property loading, collapsed disclosure persistence, parent-expanding search, option inventory,
allowed editor boundaries, ESP tables, standard toggle dimensions, and scale-1–4 row geometry.
`tools/verify_release_jar.py` checks the packaged class boundary: retired focused editor/sample
classes must be absent; inline catalog/binding classes and retained runtime owners must be present.

Build with Java 8 Forge and verify the result, for example:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.8.9.7v.jar \
  --version Beta-0.10.8.9.7v --mcmod-version 0.10.8.9.7
```

The checks do not launch Minecraft. Rendering, input feel, placement, and gameplay are **NOT tested
in-game**.

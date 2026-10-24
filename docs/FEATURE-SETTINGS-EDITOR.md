# Reference settings shell and editor reachability — Beta-0.10.8.9.5.5v

This document audits the native Minecraft 1.8.9 Forge settings implementation. The approved
`analyze-sky-velor-mod-code.zip` reference informed the layout and interaction contract; no browser runtime, React,
or third-party GUI framework is shipped. Distributable artifacts still require the Java 8 Forge build and release
verifiers.

## Refined shell contract

`AppleSettingsScreen` is the visible settings entry point for the keybind, `/sv` command path, and main/pause menu
button. `AppleHubScreen` remains only as a binary-compatible alias for an old caller and no longer renders a legacy
Hub surface.

- The direct rail order is exactly **Home**, **Gameplay**, **Dungeon**, **Farming**, **Visuals**, **System**.
  Gameplay and Visuals regroup existing presentation cards only; their setting ids, configuration owners, and focused
  editors remain unchanged.
- Farming follows Dungeon with a wheat/sprout rail glyph and is indexed by global search. The repository has no genuine
  crop/farming control, counter, or display to move, so its page displays only **No farming features yet**. Mining
  Assistant controls remain Mining functionality inside **Gameplay**; no farming automation is introduced.
- Normal desktop mode starts centered at 85% of available width and 82% of available height until its fixed maximum.
  It can be dragged by the clear header area; its offset is clamped to leave a visible world border. The header has
  exactly **Fullscreen** and **Close** 24 px icon targets with hover/state tooltips. Fullscreen persists through
  `settings.window.fullscreen`; returning to small mode restores the in-memory dragged position.
- One shared per-corner rounded-rectangle primitive draws the shell, rail, cards, controls, fields, pills, popups,
  previews, tooltips, chat click menu, and chat background. It clamps radii to each box, reuses cached geometry,
  applies a one-GUI-pixel anti-alias fringe, and shares named window/card/button/pill radii. Hover, toggle, and tab
  easing settle only while a GUI is open.
- The shared layout scale is **4 / 8 / 12 / 16 GUI px**. Full rails use 30 px destinations with exact 4 px gaps;
  compact rails use a visible two-row, three-column 28 px grid with the same gaps, so all six destinations remain
  reachable at GUI scales 1–4. The Profile footer is docked at the rail bottom rather than being spaced between tabs.
  Page cards/rows use 8 px gaps and 12 px padding. Below the usable content-width threshold, settings controls and
  Home quick controls reflow to one column instead of overlapping icons, text, and controls.
- `/velor layoutdebug` records the measured settings rail, card, row, Home quick-control, and Profile bounds. It is
  diagnostic-only and does not alter saved settings or layout behavior.
- The header owns the merged **Sky VeLor · Settings** title plus exactly the 24 px Fullscreen and Close icons. The
  compact second strip is search-only. The visible **Advanced** button and its reserved width were removed; parent
  child rows render directly below their owner with no READY/OPTIONAL badge, chevron/count disclosure, or expandable
  state. Formerly Advanced-marked cards, child rows, and search results remain directly visible.
- The whole shell is clipped to its one equal-radius rounded outer window; its nested scroll viewport owns wheel,
  Shift fine-wheel, touch/mouse drag, page, Home and End scrolling. A dedicated scrollbar gutter prevents thumb/text
  overlap, final cards have 12 px bottom padding, and a short edge fade marks clipped continuation. `Ctrl+F` or `/`
  focuses search; Escape clears focused search before closing the screen.
- Home has no setup presets. Its equal **LIVE PERFORMANCE** FPS, Java-memory, and local-tab-list ping cards read the
  cached `ClientMetrics.Reader`, record only its 500 ms generations in a fixed ring, and render large values, status,
  bars, and sparklines. They are the only FPS/memory/ping display; no in-game metric HUD is registered.
- The rail **Profile** control opens a themed compact popup listing creator **pekadeco** and contributors **ii88w** and
  **whiteShadow**. Its X, Escape, and outside click close it.
- `render/Theme.java` is the one token authority: neutral-dark surfaces, blue-only positive interaction accent,
  semantic warning/error/unavailable states, type scale, 4/8/12/16 spacing, radii, shadows/dim layers, and
  120–180 ms GUI motion. Visible Appearance controls intentionally retain only Corner radius and Panel opacity.

## Category ownership and current runtime routes

| Rail destination | Card | Current visible routes and owner |
| --- | --- | --- |
| Home | Live Performance / Quick controls | Cached FPS, Java memory, ping; Sprint, Terminal, Chat, Hand quick toggles |
| Gameplay | Comfort | No Block Particles, hotbar-wheel guard, Hurt Camera Shake, Auto Sprint |
| Gameplay | Inventory | Item Lock and Swap plus badges/line route; Inventory Assist and its focused editor |
| Gameplay | Chat | Chat QoL master and inline controls; Chat Settings route |
| Gameplay | Shortcuts / Mining / Slayer / Fishing | Existing shortcuts, Mining BPS/Assistant/Puzzler/HUD, Slayer routes, catch counter, Fishing HUD, Sea Creature HP, Jawbus and nearby encounter alerts |
| Dungeon | Run tools / Floor VII / Highlights / Party sync | Existing Dungeon Map, terminal, Spirit Leap, M7, ESP, boss HUD and sync routes |
| Farming | No farming features yet | Audited empty state only; no crop setting, counter, display, config key, or automation was fabricated |
| Visuals | HUD Hub / Item Animation | Direct-canvas HUD Editor, Player Doll, Inventory HUD, Item Animation and focused routes |
| System | Appearance / Performance / General | Fixed Theme geometry/opacity controls, performance and shared scrolling routes |
| System | Diagnostics / About | Command/sync diagnostics, version and client-side safety information |

`ReferenceSettingsCatalog` is the immutable presentation inventory. It contains the rail/card order, search synonyms,
inline children, and every current route that must remain accessible. `FeatureSettingsUiModel` is a non-rendered audit
projection, not a second menu model or configuration schema. `DungeonEspUiModel` remains the detailed non-rendered
editor inventory for established ESP controls.

## Persistence, rendering cleanup, and editor transitions

Search uses each setting's id, label, description, keywords, and inline child copy. Results identify the owning
rail/card while retaining the same live control and focused route. Existing owners remain authoritative:
`AppleSettings`, `AssistSettings`, `ChatQolSettings`, `DungeonEspSettings`, `M7Settings`, `ItemLockSwapSettings`, and
each HUD element. Historic `appearance.settingsTheme`, `darkMode`, and accent keys remain readable and bounded so old
configuration files are not damaged; the active Theme deliberately ignores those legacy palette choices and renders
one neutral-dark/blue system. `settings.window.fullscreen` persists only the shell mode; a missing old key safely opens
the responsive small window.

HUD Editor/HUD Hub Editor transitions preserve the fixed Theme and shared OpenGL/UI state:

- The parent settings screen reapplies the fixed Theme in `initGui` and before every draw, so a returned child cannot
  leave the parent on a generic or tinted palette.
- `UIRender` has a neutral GUI restoration baseline. HUD frames, editor frames, scissor blocks, hovered-editor flags,
  element matrices/alpha, inventory-item passes, entity previews, and direct held-item previews use `finally` cleanup.
- `HudManager`, `HudElement`, `ItemSlots`, `PlayerPreview`, and `HudEditorScreen` restore state even when a live
  preview renderer fails. This is automated/source-contract coverage, not a substitute for an in-game render pass.

The .4v fresh-default audit remains deliberately fresh-install-only: Roman numerals, tooltip scrolling, and rarity
highlighting may default on for an empty configuration; a populated older configuration never gains a behavior merely
because it lacks a new key. Dungeon Sync, terminal automatic clicks, Auto Sprint, shortcuts, party chat actions,
nearby encounter titles, M7 overlays/solvers, and fresh Dungeon ESP remain opt-in. Schema v3 does not overwrite an
explicit saved choice.

## Home metric and verification scope

- **FPS**, **Java memory**, and **ping** are displayed only by Home's LIVE PERFORMANCE cards. Their reader is queried
  while Home is open, refreshes at most once per 500 ms, and appends to a fixed 24-sample ring only for a new snapshot.
- No `StatsHud`, metric HUD-editor target, Visuals route, default, render-event overlay, or active persisted metric
  key remains. Historical `fps-display.*`, `ping-display.*`, `memory-display.*`, and `stats.*` keys are consumed and
  ignored, without enabling a replacement display.
- Existing Dungeon ESP detection, entity filters, styles, cached doors, badges, and focused-editor live preview remain
  available through Dungeon → Highlights.
- Headless contracts cover exact six-tab order, category/config-key ownership, ungated search/direct children,
  Farming's searchable audited empty state, 4/8/12/16 spacing, scale-1–4 rail/header/control-edge reachability and
  no-overlap, one-column narrow reflow, rounded shell clip/gutter/final-padding geometry, fixed Theme behavior,
  absence of chips/expanders/palette selectors, Home metric format/unknown-ping/ring bounds, drag/clamp geometry, and
  rendered state-restoration contracts.
- `HardcodedGuiColorTests` scopes renderer-facing `gui`, `widget`, `notes`, `hud`, and `render` source. Its only
  direct hexadecimal-colour allowlist entry is `render/Theme.java`. Configuration-owned legacy palette arrays are
  intentionally outside that scan because they are load-compatibility data, not active GUI rendering; feature/world
  colour values (for example configurable ESP styles) likewise remain outside this GUI-only boundary.

The release workflow runs Java 8 Forge compilation/reobfuscation, the standalone test suite, release-JAR verification,
and source-ZIP verification. These checks do **not** start Minecraft. Rendering, input feel, and gameplay remain **NOT
tested in-game** until a real Minecraft 1.8.9 Forge pass is available.

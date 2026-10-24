# Reference settings shell and editor reachability — Beta-0.10.8.9.5v

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
  It can be dragged by the title bar; its offset is clamped to leave a visible world border. Green toggles
  normal/larger layout, yellow minimizes/restores a compact strip, red closes, and window/controls/hitboxes are all
  rebuilt from the same moved layout bounds after drag or resize.
- Small GUI scales use an icon-capable compact horizontal rail and the existing clipped scrolling content viewport.
  Full-width rails retain readable labels; compact rails keep every category reachable instead of overlapping text.
- The toolbar contains product/version chrome and global **Search settings**. The visible **Advanced** button and its
  reserved width were removed. Formerly Advanced-marked cards, child rows, and search results remain directly visible;
  `Optional` is informational only and never filters a control.
- One scissored viewport owns wheel, Shift fine-wheel, touch/mouse drag, page, Home and End scrolling. A swipe cancels
  a pending control click. `Ctrl+F` or `/` focuses search; Escape clears focused search before closing the screen.
- Home has no setup presets. Its **LIVE PERFORMANCE** card reads the existing cached `ClientMetrics.Reader` for real
  FPS, Java used/max heap MB, and local tab-list ping; it never creates a second in-game metric renderer.
- The rail **Profile** control opens a themed compact popup listing creator **pekadeco** and contributors **ii88w** and
  **whiteShadow**. Its X, Escape, and outside click close it.
- **Settings Theme** persists either the established **Forest & Cyan** shell or **Black & Blue**. Black & Blue uses
  near-black surfaces, blue/cyan accents, light text, subtle borders/cards, and the same palette in main controls,
  navigation, dropdowns, popups, and focused settings editors.

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
| Visuals | HUD Hub / Item Animation | Direct-canvas HUD Editor, Stats Display, Player Doll, Inventory HUD, Item Animation and focused routes |
| System | Appearance / Performance / General | Settings Theme, legacy appearance sliders, performance and shared scrolling routes |
| System | Diagnostics / About | Read-only client metrics, command/sync diagnostics, version and client-side safety information |

`ReferenceSettingsCatalog` is the immutable presentation inventory. It contains the rail/card order, search synonyms,
inline children, and every current route that must remain accessible. `FeatureSettingsUiModel` is a non-rendered audit
projection, not a second menu model or configuration schema. `DungeonEspUiModel` remains the detailed non-rendered
editor inventory for established ESP controls.

## Persistence, rendering cleanup, and editor transitions

Search uses each setting's id, label, description, keywords, and inline child copy. Results identify the owning
rail/card while retaining the same live control and focused route. Existing owners remain authoritative:
`AppleSettings`, `AssistSettings`, `ChatQolSettings`, `DungeonEspSettings`, `M7Settings`, `ItemLockSwapSettings`, and
each HUD element. The additive `appearance.settingsTheme` key defaults missing legacy configurations to Forest & Cyan
and bounds saved values to the two supported choices.

HUD Editor/HUD Hub Editor transitions preserve the selected palette and shared OpenGL/UI state:

- The parent settings screen reapplies its reference palette in `initGui` and before every draw, so a returned child
  cannot leave the parent on a generic or tinted palette.
- `UIRender` has a neutral GUI restoration baseline. HUD frames, editor frames, scissor blocks, hovered-editor flags,
  element matrices/alpha, inventory-item passes, entity previews, and direct held-item previews use `finally` cleanup.
- `HudManager`, `HudElement`, `ItemSlots`, `PlayerPreview`, and `HudEditorScreen` restore state even when a live
  preview renderer fails. This is automated/source-contract coverage, not a substitute for an in-game render pass.

The .4v fresh-default audit remains deliberately fresh-install-only: Roman numerals, tooltip scrolling, and rarity
highlighting may default on for an empty configuration; a populated older configuration never gains a behavior merely
because it lacks a new key. Dungeon Sync, terminal automatic clicks, Auto Sprint, shortcuts, party chat actions,
nearby encounter titles, M7 overlays/solvers, and fresh Dungeon ESP remain opt-in. Schema v3 does not overwrite an
explicit saved choice.

## Stats Display and verification scope

- The only restored **in-game** metric group is **Stats Display**: **FRAME RATE**, **MINING**, and **MEMORY**. The
  Home live-performance card is settings-only and does not replace it.
- Stats Display remains opt-in on a fresh configuration. The **Visuals** HUD card exposes visibility/editor routes, and
  the HUD editor shows disabled elements as selectable faded placeholders.
- Existing Dungeon ESP detection, entity filters, styles, cached doors, badges, and focused-editor live preview remain
  available through Dungeon → Highlights.
- Headless contracts cover exact six-tab order, category/config-key ownership, ungated search/children, Farming's
  searchable audited empty state, Black & Blue persistence/palette, drag/clamp geometry, and rendered state-restoration
  contracts.

The release workflow runs Java 8 Forge compilation/reobfuscation, the standalone test suite, release-JAR verification,
and source-ZIP verification. These checks do **not** start Minecraft. Rendering, input feel, and gameplay remain **NOT
tested in-game** until a real Minecraft 1.8.9 Forge pass is available.

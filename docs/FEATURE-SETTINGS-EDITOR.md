# Reference settings shell and editor reachability — Beta-0.10.8.9.4v

This document audits the native Forge implementation of the supplied design source at
`/home/user/Sky-VeLor/analyze-sky-velor-mod-code.zip`. That archive is a React/Vite interactive design
proposal, so its layout and interaction contract was adapted manually to Minecraft 1.8.9 Forge; no browser,
React, or third-party GUI framework is shipped. Distributable artifacts still require the Java 8 Forge build
and release verifiers.

## Refined shell contract

`AppleSettingsScreen` is the visible settings entry point for the keybind, `/sv` command path, and main/pause
menu button. `AppleHubScreen` is retained only as a binary-compatible alias for an old caller and no longer
renders the previous seven-section surface.

- The exact rail order is **Home**, **Gameplay**, **Dungeon**, **Visuals**, **System**.
- Normal desktop mode is centered at 85% of available width and 82% of available height until its fixed maximum;
  expanded mode is larger, and very small GUI scales use the bounded panel plus scrolling rather than unreadable
  shrinking. The margin around the panel keeps the world visible.
- The top-left controls are real desktop actions: **red** closes to the game, **yellow** toggles a compact,
  restorable minimized strip, and **green** toggles normal/larger layout. Enter/Space also restores a minimized
  strip. Escape still closes the screen.
- The toolbar contains product/version chrome, a global **Search settings** field, and **Advanced**. The former
  visible **Done** action and its reserved gap are removed. `Ctrl+F` or `/` focuses search; Escape clears focused
  search before closing the screen. Closing remains the persistence point.
- One scissored viewport owns wheel, Shift fine-wheel, touch/mouse drag, page, Home and End scrolling. A swipe
  cancels a pending control click. The rail becomes a compact horizontal strip at narrow GUI widths, with a second
  compact profile row so information remains reachable.
- Cards remain visible inside a destination. A row with children exposes an inline disclosure link; it does not
  create an alternate navigation page. Advanced cards/children are hidden until **Advanced** is enabled.
- Home has no setup presets. Its **LIVE PERFORMANCE** card reads the existing cached `ClientMetrics.Reader` once
  per 500 ms for real FPS, Java used/max heap MB, and local tab-list ping. It uses the reader's honest `Ping -- ms`
  disconnected value and never creates a `HudElement` or a second in-game metric renderer.
- The sidebar **Profile** control opens a themed compact popup listing creator **pekadeco** and contributors
  **ii88w** and **whiteShadow**. Its X, Escape, and an outside click close the popup. It is a popup, not a sixth tab.
- Native `AppleToggle`, `AppleSlider`, and `AppleButton` controls keep their existing focus/drag/animation
  semantics. Choice controls use the reference's compact segmented presentation.

## Reference hierarchy and current runtime ownership

| Rail destination | Card | Current visible routes and owner |
| --- | --- | --- |
| Home | Live Performance / Quick controls | Cached FPS, Java memory, ping; Sprint, Terminal, Chat, Hand quick toggles |
| Gameplay | Comfort | No Block Particles, hotbar-wheel guard, Hurt Camera Shake, Auto Sprint |
| Gameplay | Inventory | Item Lock and Swap plus badges/line route; Inventory Assist and its focused editor |
| Gameplay | Chat | Chat QoL master and inline tabs/smooth/hover controls; Chat Settings route |
| Gameplay | Skills | Mining BPS/assistant, Slayer/HUD, Fishing counter/HUD/sea HP/Jawbus routes |
| Gameplay | Shortcuts | Wardrobe/Pets focused route (Advanced) |
| Dungeon | Run tools | Dungeon Map, Terminal Assist, Spirit Leap, Boss HP HUD routes |
| Dungeon | Floor VII | M7 overlay, dragon alerts, relic areas |
| Dungeon | Highlights | Existing Mob ESP and Door ESP focused style editors |
| Dungeon | Party sync | Dungeon Sync and advanced diagnostics, with fail-closed notice |
| Visuals | HUD | Direct-canvas HUD Editor, Stats Display, Player Doll, Inventory HUD and their editor routes |
| Visuals | Item Animation | Existing master plus inline size/tempo/X/Y/Z/reset and the focused Item Animation editor |
| System | Appearance / Performance / General | Theme/accent/appearance sliders; performance and shared scrolling controls with focused routes |
| System | Diagnostics / About | Read-only client metrics, command/sync diagnostics, version and client-side safety information |

`ReferenceSettingsCatalog` is the immutable presentation inventory. It contains the exact rail/card order,
search synonyms, Advanced flags, inline children, and every current route that must remain accessible.
`FeatureSettingsUiModel` is a non-rendered audit projection of that catalog; it is not a second menu model or
configuration schema. `DungeonEspUiModel` remains the detailed non-rendered editor inventory for established
ESP controls.

## Defaults, persistence, search, and accessibility

Search uses each setting's id, label, description, keywords, and inline child copy. Results identify the owning
rail/card while retaining the same live control and focused route. The screen preserves all existing configuration
owners: `AppleSettings`, `AssistSettings`, `ChatQolSettings`, `DungeonEspSettings`, `M7Settings`,
`ItemLockSwapSettings`, and each HUD element are written directly. No prototype-only browser state is persisted.

The .4v default audit is deliberately fresh-install-only:

- Roman numerals, tooltip scrolling, and rarity highlighting are useful client-only inventory presentation defaults.
- A populated older settings file does not acquire any of those behaviors when it lacks a new key; explicit saved
  values always win.
- Dungeon Sync, terminal automatic clicks, Auto Sprint, shortcuts, party chat actions, nearby encounter titles,
  M7 overlays/solvers, and fresh Dungeon ESP all remain opt-in. Existing saved Dungeon ESP/M7 choices and historic
  fallback behavior are preserved.
- Schema v3 records an upgraded configuration without promoting or overwriting an explicit saved choice.

Headless tests cover five-rail order, card hierarchy, required-route inventory, inline-detail search, Advanced
gating, compact geometry bounds, normal/larger/minimized window geometry, profile credits, Home preset removal,
cached live-metric surface claims, safe default migration, saved-setting preservation, and disconnected metric
formatting.

## Stats Display and other retained behavior

- The only restored **in-game** metric group is **Stats Display**: **FRAME RATE**, **MINING**, and **MEMORY**.
  The middle card uses the existing mining tracker and bounded `BPS`, not ping.
- The Home live performance card is settings-only and shares `ClientMetrics`; it does not replace the Stats HUD.
- Stats Display uses the historic fixed three-card geometry: 172 × 102 cards, 9 px gaps, 8 px cut corners, a
  2 px dark offset edge, centered near the top of the HUD. Frame Rate and Mining values are cyan; Memory is white.
- Stats Display remains opt-in on a fresh configuration. The Visuals HUD card exposes its visibility switch
  and direct editor route, and the HUD editor shows disabled elements as selectable faded placeholders.
- Legacy visible `fps-display.*`, `ping-display.*`, or `memory-display.*` configurations migrate once to
  `stats.enabled=true` only when no explicit modern `stats.enabled` choice exists. Retired keys are then ignored,
  so no duplicate metrics renderer is registered. An explicit modern false value wins.
- Existing Dungeon ESP detection, entity filters, styles, cached doors, badges, live preview, and GL-state
  restoration remain available through Dungeon → Highlights. On wide GUI scales its focused editor keeps a clipped
  Live Preview beside controls; on compact scales it gives settings one full-width scrollable column.

## Verification scope

The release workflow runs Java 8 Forge compilation/reobfuscation, the standalone test suite, release-JAR
verification, and source-ZIP verification. These checks do not start Minecraft. Rendering, input feel, and gameplay
remain **NOT tested in-game** until a real Minecraft 1.8.9 Forge pass is available.

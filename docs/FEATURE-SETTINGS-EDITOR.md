# Reference settings shell and editor reachability — Beta-0.10.8.9.3v

This document audits the native Forge implementation of the supplied design source at
`/home/user/Sky-VeLor/analyze-sky-velor-mod-code.zip`. That archive is a React/Vite interactive design
proposal, so its layout and interaction contract was ported manually to Minecraft 1.8.9 Forge; no browser,
React, or third-party GUI framework is shipped. Distributable artifacts still require the Java 8 Forge build
and release verifier.

## Reference-shell contract

`AppleSettingsScreen` is the visible settings entry point for the keybind, `/sv` command path, and main/pause
menu button. `AppleHubScreen` is retained only as a binary-compatible alias for an old caller and no longer
renders the previous seven-section surface.

- The exact rail order is **Home**, **Gameplay**, **Dungeon**, **Visuals**, **System**.
- The toolbar contains product/version chrome, a global **Search settings** field, **Advanced**, and **Done**.
  `Ctrl+F` or `/` focuses search; Escape clears focused search before closing the screen.
- One scissored viewport owns wheel, Shift fine-wheel, touch/mouse drag, page, Home and End scrolling. A swipe
  cancels a pending control click. The rail becomes a compact horizontal strip at narrow GUI widths.
- Cards are always visible inside a destination. A row with children exposes an inline disclosure link; it does
  not create an alternate navigation page. Advanced cards/children are hidden until **Advanced** is enabled.
- Home provides four bounded presets and quick controls. Presets use current live setting owners and retain the
  safety rule that terminal auto-click and Dungeon Sync are never enabled by a preset.
- Native `AppleToggle`, `AppleSlider`, and `AppleButton` controls keep their existing focus/drag/animation
  semantics. Choice controls use the reference's compact segmented presentation. Changes save on **Done**/close.

## Reference hierarchy and current runtime ownership

| Rail destination | Card | Current visible routes and owner |
| --- | --- | --- |
| Home | Presets / Quick controls | Vanilla+, Quality of Life, Dungeon, Minimal; Sprint, Terminal, Chat, Hand quick toggles |
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

## Search, accessibility, and persistence

Search uses each setting's id, label, description, keywords, and inline child copy. Results identify the owning
rail/card while retaining the same live control and focused route. The screen preserves all existing config owners
and defaults: `AppleSettings`, `AssistSettings`, `ChatQolSettings`, `DungeonEspSettings`, `M7Settings`,
`ItemLockSwapSettings`, and each HUD element are written directly. No prototype-only browser state is persisted.

The source-level tests cover five-rail order, card hierarchy, required-route inventory, inline-detail search,
Advanced gating, duplicate-id protection, stale prototype-only route exclusion, and compact geometry bounds.

## Stats Display restoration and compatibility

- The only restored **in-game** metric group is **Stats Display**: **FRAME RATE**, **MINING**, and **MEMORY**.
  The middle card uses the existing mining tracker and bounded `BPS`, not ping.
- System → Diagnostics retains read-only settings-screen Frame Rate/Ping/Memory telemetry. It is not a
  `HudElement`, so it cannot create another in-game metrics renderer.
- Stats Display uses the historic fixed three-card geometry: 172 × 102 cards, 9 px gaps, 8 px cut corners, a
  2 px dark offset edge, centered near the top of the HUD. Frame Rate and Mining values are cyan; Memory is white.
- Stats Display remains opt-in on a fresh configuration. The Visuals HUD card exposes its visibility switch
  and direct editor route, and the HUD editor shows disabled elements as selectable faded placeholders.
- Legacy visible `fps-display.*`, `ping-display.*`, or `memory-display.*` configurations migrate once to
  `stats.enabled=true` only when no explicit modern `stats.enabled` choice exists. Retired keys are then ignored,
  so no duplicate metrics renderer is registered. An explicit modern false value wins.
- **Player Dollars** remains absent. Old `dollars.*`, `balance.source`, and `balance.manual` keys are ignored so
  active HUD settings load normally.

## Dungeon ESP and other retained behavior

- Existing Dungeon ESP detection, entity filters, styles, cached doors, badges, live preview, and GL-state
  restoration remain unchanged. Mob ESP retains Global, Starred Mob, Miniboss, Special Starred Mob, Enderman,
  Skeletor, and Other Mobs. Door ESP retains Global, Wither Doors, and Blood Doors.
- On wide GUI scales the focused ESP editor keeps its clipped Live Preview beside controls; on compact scales it
  gives settings one full-width scrollable column. All categories and controls remain reachable from Dungeon →
  Highlights.
- Existing configuration owners and key names remain unchanged. Old `ui.feature.tree.*` presentation values are
  accepted and ignored; editor selections continue under `ui.feature.editor.*`.

## Verification scope

The release workflow runs Java 8 Forge compilation/reobfuscation, the standalone test suite, release-JAR
verification, and source-ZIP verification. These checks do not start Minecraft. Rendering, input feel, and gameplay
remain **NOT tested in-game** until a real Minecraft 1.8.9 Forge pass is available.

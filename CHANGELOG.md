# Changelog

## Beta-0.10.9.1.9v — 2026-10-29

### Fixed

- **Item Lock And Swap Key** now displays its current binding and supports full-row click capture. Pressing a key
  assigns it, Escape cancels, and Backspace saves `NONE`; zero is a deliberate persisted clear state rather than a
  fallback to the default L key.
- **Command Keybind** no longer strips a typed trailing space while its field has focus. It accepts normal characters
  and internal spaces such as `/warp hub`, then trims only leading/trailing whitespace when the field commits.
- Compact Command Keybind rows no longer need a separate edit surface or leave a large blank panel: each row contains
  key, command, On/Off, and a fully visible Delete button, while scrolling begins only after the capped visible rows.

### Added

- Added the standalone **QoL → Shortcut** card, styled as a peer of **Inventory And Chat**. It contains Wardrobe
  Shortcut, Pets Shortcut, Command Keybind, and reset.
- Added **Wardrobe Shortcut** enable/key capture. Its physical key can send `/wardrobe` only from normal gameplay;
  in a recognized Wardrobe `GuiChest`, direct non-repeat number keys `1`–`9` make at most one ordinary selection of
  slots `36`–`44`. Chat and unrelated screens fail closed.
- Added focused migration, capture, compact-editor, `/warp hub`, Wardrobe title/slot-policy, source-boundary, and
  reobfuscated-JAR verifier coverage.
- Added `THIRD-PARTY-NOTICES.md` acknowledging NotEnoughFakepixel's LGPL-3.0-or-later Wardrobe behavior reference.
  Sky VeLor uses an independent implementation and includes no source, dependency, asset, or binary from it.

### Changed

- Replaced the visible **Keybind Cut** dropdown with **Shortcut**. New values save under `assist.shortcut.*`; prior
  `assist.keybindCut.*`, older movement/shortcut aliases, and `.1.8v` Command Keybind records load without reset.
- Preserved the existing Item Lock / Swap legality and multi-pair cycle behavior, HUD Editor, Item Animation, Chat
  QoL, Dungeon/Door ESP, Party Sync default-off behavior, Home metrics, reflection fail-closed behavior, and old
  configuration loading.

### Verification boundary

- GitHub Actions Java 8 Forge compilation, standalone logic/layout tests, reobfuscated JAR verification, and curated
  source-archive verification cover this release. They do not launch Minecraft; this release is **NOT tested in-game**.

## Beta-0.10.9.1.8v — 2026-10-28

### Fixed

- Item Lock And Swap now rejects every new pair that has no hotbar endpoint. Inventory↔inventory,
  armor↔inventory, inventory↔armor, and armor↔armor paths are blocked through the existing blocked-message gate;
  malformed and legacy illegal saved pairs are discarded safely during load.
- A paired position can now retain multiple legal targets. Lines render to every saved target, an L tap on a paired
  position removes one saved pair, and completed manual swaps cycle shared-endpoint targets in creation order without
  skipping the existing cooldown or failed fit/cursor checks.

### Added

- Added **Command Keybind** inside **QoL → Inventory And Chat → Keybind Cut**. The aligned row has an opt-in master
  toggle plus **Open Command Keybind Editor**. Its opaque/no-dim editor supports add, edit, delete, entry On/Off,
  click-to-capture, Escape cancel, Backspace clear, reset, persistent slash commands, and collision warnings.
- Added direct physical-edge runtime handling with a 300 ms anti-spam cooldown. Commands cannot fire while chat, a GUI,
  or the editor is open; a held key never repeats and duplicate command keys dispatch only their first creation-order
  entry.
- Added focused Item Lock pair legality/multi-pair/cycling migration tests, Command Keybind persistence/collision/
  direct-edge policy tests, editor-source coverage, and release/source archive verifier checks.

### Changed

- Renamed the visible **Movement And Shortcuts** route to **Keybind Cut** in headers and search. The former
  `assist.movement.*` and `assist.shortcuts.*` values load compatibly and save under the new
  `assist.keybindCut.*` namespace; new keys take precedence when both exist.
- Rechecked the established GUI/HUD Editor, Item Animation, Hide Dead Mobs, Smooth Aspect Teleport, Dungeon ESP, and
  Chat QoL contracts with the Java 8 Forge build, standalone tests, headless layout evidence, and release verifiers.

### Verification boundary

- Automated verification does not launch Minecraft. Item Lock And Swap interaction, Command Keybind input feel,
  rendering, and gameplay compatibility are **NOT tested in-game**.

## Beta-0.10.9.1.7v — 2026-10-28

### Fixed

- Aligned **Open HUD Editor**, **Open Item Animation Editor**, and every other settings action to the same shared
  trailing right edge, `132 × 20` control target, and toggle-row vertical center across all channels.
- Made the standalone Item Animation editor panel solid, dark, and opaque so its controls remain readable without a
  fullscreen dim layer, HUD canvas, or gameplay preview surface behind the panel controls.
- Fixed **QoL → Comfort → Hide Dead Mobs** at the live render layer. Dead/dying normal living mobs and SkyBlock/
  Fakepixel-style server NPC fake mobs now have both their normal body render and standard nameplate suppressed when
  zero health, `deathTime`, or `isDead` indicates death. The local player and ordinary listed remote players remain
  protected; no entity is removed and no server state is changed.
- Reworked **QoL → Comfort → Smooth Aspect Teleport** so an observed local Aspect use is armed/expired on client ticks
  but its accepted position discontinuity is sampled at `RenderTick START`. A plausible, bounded (up to 64-block)
  server-authoritative jump now starts the existing `125 ms` client-render camera/world transition on the frame it is
  rendered, avoiding the previous tick-timing snap path and rejecting respawn/correction-scale jumps.

### Added

- Added the visible **Slayer** sidebar channel immediately after **QoL** and moved the existing Slayer Tracker route
  into it. Its feature/configuration owner and values are unchanged.
- Added focused policy, observation, packaged-class, source-archive, and release-JAR checks for dead-mob rendering,
  render-time Aspect sampling, the bounded displacement guard, opaque Item Animation panel, QoL/Slayer catalog,
  shared action geometry, and the compact nine-destination rail.

### Changed

- Renamed the visible **Player** sidebar/header/search channel to **QoL** while retaining the historical `GAMEPLAY`
  tab identity and all existing persistence/configuration keys for backward compatibility.
- The compact rail now supports nine destinations across three reachable rows at GUI scale four; the established GUI,
  HUD Editor, Dungeon ESP, Chat QoL, Item Lock / Swap, Party Sync default-off behavior, Home metrics, and old-config
  loading contracts remain intact.

### Verification boundary

- The Java 8 Forge build, headless logic/layout suite, reobfuscated JAR verifier, and curated-source verifier are run
  in CI. They do not launch Minecraft; this release is **NOT tested in-game**.

## Beta-0.10.9.1.6v — 2026-10-28

### Added

- Added clean-room, default-off **Dungeons → Run Tools → Spirit Bow Locator** (`assist.dungeon.spiritBowLocator`).
  It timer-scans nearby loaded marker armor stands in a confirmed dungeon and renders only a local line/outline to the
  nearest `Spirit Bow` name; it has no keybind, gameplay command, click, movement, controller, packet, chat, or custom
  player-data path.
- Added clean-room, default-off **HUD & Visuals → HUD → Dungeon Run HUD**, movable through the existing HUD Editor.
  It reads only ordinary visible dungeon-sidebar rows at a bounded interval and displays explicitly observed floor,
  elapsed time, cleared percentage, secrets, and deaths—never a copied score estimate, tab-list value, chat action, or
  secret-chest action. `/velor selftest` reports concise registration status for both new hooks.

### Changed

- Replaced the rejected dark/blue **Smooth Aspect Teleport** HUD veil with a short `125 ms`, client-side rendered
  camera/world glide for observed AOTE/AOTV jumps. Vanilla still applies the ordinary server-authoritative teleport;
  a fail-closed render path temporarily eases the camera, terrain, entities, and world overlays from the pre-jump
  position to the accepted destination.
- Replaced every generic editor button with a clear route CTA: **Open HUD Editor** for HUD destinations and
  **Open Item Animation Editor** for the existing held-item editor. No extra editor screen was added.
- A normal new settings GUI visit now collapses all remembered feature disclosures, including legacy persisted-open
  rows. The existing child-editor return keeps its parent instance intact, and the intentional Chat route opens
  Chat QoL after the normal reset.

### Safety and verification

- Added pure marker/readout parsing, isolated config, catalog/binding, HUD persistence, `/velor selftest`, and packaged
  JAR boundary coverage. The curated source ZIP verifier now also requires the new source/tests and explicitly rejects
  both root reference ZIPs.
- The Aspect path remains visual-only: it neither initiates nor alters a teleport, sends/replays packets, changes
  input, writes player/camera entity coordinates, or invokes a controller. It enables only when both verified
  1.8.9 render-coordinate hook surfaces are available.
- Added deterministic render-path tests plus bytecode fixtures that prove the EntityRenderer/RenderGlobal hook is
  narrow and fail-closed. Existing Fel/Enderman ESP, configuration compatibility, and editor-route coverage remain.
- Automated checks do not launch Minecraft; validate the final smooth camera feel in a real Minecraft 1.8.9 Forge
  client before claiming in-game runtime validation.

## Beta-0.10.9.1.5v — 2026-10-27

### Fixed

- Restored direct Enderman/Fel skull-marker outlines. Recognized markers no longer depend on a nearby star/name stand
  or a player-relative height gate, so live dungeon timing and elevated marker placement cannot hide their outline.
- Restored the proven compact Fel/Enderman marker proxy to `0.76 × 1.85` blocks, covering the complete marker while
  remaining smaller than the retired oversized `1.1 × 2.7` outline.
- Reworked **Smooth Aspect Teleport** around an observed local Aspect use plus a short server-response window. It now
  catches short completed AOTE/AOTV jumps and renders a more visible, bounded `340 ms` blue ease veil.

### Safety and verification

- The Aspect path remains observation/render-only: it does not replay or cancel the click, initiate teleportation,
  write movement or camera coordinates, alter key state, or send packets.
- Added pure explicit-use/jump-window coverage and direct Fel-marker regressions. The exact local-player identity
  exclusion and player-shaped dungeon NPC targeting remain in place.
- Automated checks still do not launch Minecraft; validate Fel placement and Aspect feel in a real 1.8.9 Forge client.

## Beta-0.10.9.1.4v — 2026-10-27

### Fixed

- Dungeon ESP now excludes the local player by identity in its cached scanner and live render collector, so the client
  cannot receive a dungeon mob outline/hitbox. Player-shaped dungeon NPCs remain eligible when a real nearby dungeon
  tag selects them.
- Enderman/Fel skull markers now require explicit star evidence (their own star or a nearby resolved Starred/Special
  tag), and markers at least two blocks above the player are suppressed. Their fixed proxy is refined to a
  head-focused `0.68 × 1.60` blocks.

### Added

- **Player → Comfort → Smooth Aspect Teleport**, persisted as `assist.visual.smoothAspectTeleport` and enabled by
  default. It observes only an already-completed local Aspect of the End/Void position jump and draws a `220 ms`
  render-only visual ease veil. It does not initiate movement or teleports, write player/camera coordinates, alter
  input, or send packets.

### Verification boundary

- Added pure scanner/head-policy/marker-geometry and Aspect-transition-policy regressions, config and inline-owner
  coverage, and packaged-JAR checks for the local-player render guard, star evidence, height suppression, and
  render-only transition boundary.
- Automated checks do not launch Minecraft; the local-player exclusion, Enderman marker placement, and visual
  transition still require a real Minecraft 1.8.9 Forge in-game pass.

## Beta-0.10.9.1.3v — 2026-10-27

- Compacted the normal native settings shell to 72% safe width / 78% safe height with a `720 × 480` logical cap.
  A versioned one-time layout migration resets legacy saved fullscreen shells to compact without removing the
  subsequent explicit Fullscreen choice, drag behavior, wordmark, or Close action.
- Snapped vanilla bitmap-font text scale/origin to the active physical GUI grid once per `UIRender` frame and raised
  cached rounded-arc capacity to 16 segments. Smooth is the new/reset curve default; Balanced/Fast persisted values
  stay compatible while even Fast renders eight-segment quarter arcs.
- Removed Starred Mobs Default/Subtle actions, the dedicated Skeletor category/style, and the ESP tracer UI/config/
  renderer path. Star-tagged Skeletor now uses normal Starred styling; Other Mobs remains absent. Retired keys are
  ignored safely and cleaned on save.
- Replaced Dungeon ESP **Hide Zero Health** with default-on **Player → Comfort → Hide Dead Mobs**. Its new
  `assist.comfort.hideDeadMobs` key wins over and migrates the retired ESP key, and both scanner and live render
  guard suppress zero-health / `EntityLivingBase.deathTime > 0` targets during the death animation.
- Tightened the sleeping Fel/Enderman head-marker proxy from `1.1 × 2.7` to `0.76 × 1.85` blocks.
- Added configuration, catalog, association/death-animation, marker-geometry, pixel-grid, curve, layout-migration,
  and packaged-JAR verifier coverage. Updated display/numeric Forge metadata, build contract, documentation, and
  verifier examples to `Beta-0.10.9.1.3v` / numeric `0.10.9.1.3`.
- Player screenshots identified the settings and Fel-outline defects. Automated validation does not launch Minecraft;
  a fresh Minecraft 1.8.9 Forge visual/input pass is still required before in-game validation can be claimed.

## Beta-0.10.9.1.2v — 2026-10-26

- Renamed the visible **Mob ESP** owner to **Starred Mobs** and limited its runtime target set to tagged starred
  targets and named minibosses. The generic **Other Mobs** style and room-wide force/bat/invisible controls are gone.
  Existing `dungeonEsp.otherMobs.*` and room-wide fallback keys load harmlessly, then are removed on the next save.
- Removed the Starred Mobs session-only Copy Style, Apply Copied Style, and Apply Style To All actions.
- Fixed player-shaped/NPC dungeon bodies being discarded before their nearby star or miniboss tag could create an
  outline target. Starred NPCs and miniboss NPCs now reach their existing visible outline styles; untagged room mobs
  still never receive a generic outline.
- Kept Miniboss's visible `Outline + Filled` default with nonzero outline opacity and added tracker/config/catalog
  regression coverage for both NPC target selection and the removed controls.
- Updated display metadata, numeric Forge metadata, build contract, documentation, and verifier examples to
  `Beta-0.10.9.1.2v` / numeric `0.10.9.1.2`.
- Player reports identified the pre-fix NPC/miniboss-outline defects. The corrected build still requires a fresh
  Minecraft 1.8.9 Forge visual pass; the assistant environment has no playable client.

## Beta-0.10.9.1.1v — 2026-10-26

- Added the literal **SkyVeLor** upper-left settings-header wordmark while retaining exactly the existing
  **Fullscreen** and **Close** header actions, title-bar dragging, Escape, tooltips, hover/press feedback,
  and persisted fullscreen behavior.
- Renamed visible rail categories only to **Home**, **Player**, **Dungeons**, **Farming**, **Mining**,
  **Fishing**, **HUD & Visuals**, and **Misc**. The existing eight enum routes, feature owners, search behavior,
  configuration keys, and no-feature Farming state are unchanged.
- Refined the shared one-level `AppleDropdown` with dedicated Theme tokens for trigger/popup contrast, borders,
  row hover, selected accent treatment/check, and dividers. Existing adaptive placement, editor flow reservation,
  flat-choice depth, and feature-value bindings remain unchanged.
- Made GUI transparency explicit in **Misc → Appearance** with **Custom GUI Transparency** and **GUI Transparency**.
  The retained `glassOpacityOn` / `glassOpacity` keys continue to load/save old configurations; the custom range is
  now 45%–100% so lower opacity visibly shows more of the game behind the GUI.
- Corrected expanded feature disclosures: an open owner has a subtle blue state surface, while its children render
  inside a dark bordered inset with child hover feedback and inset dividers. Master-off children remain editable,
  but now use readable muted copy and `.78` control alpha rather than a near-invisible veil.
- Added layout-renderer, catalog/config, Theme, packaged-release, and verifier coverage for the wordmark,
  feature-oriented labels, optional transparency, legacy keys, and dropdown visual contract; added focused
  Theme/package/verifier coverage for readable expanded disclosures.
- Updated display metadata, numeric Forge metadata, build contract, verifier examples, documentation, and release
  filenames to `Beta-0.10.9.1.1v` / numeric `0.10.9.1.1`.
- A player supplied in-game evidence of the pre-fix expanded-control readability problem. The corrected build still
  requires a fresh Minecraft 1.8.9 Forge visual/input pass; the assistant environment has no playable client.

## Beta-0.10.9.1v — 2026-10-26

- Restored the historical Apple-inspired graphite-glass settings hierarchy: translucent bordered window/cards,
  icon/title/subtitle card headers, broad profile-free eight-channel rail, soft shadow, and 8 px large-layout
  world margin. Existing feature scope, values, configuration owners/keys, old-config loading, and routes remain intact.
- Kept exactly **Fullscreen** and **Close** as 24 px title-bar icon targets while retaining drag, Escape,
  tooltip, hover/press acknowledgement, close behavior, and persisted fullscreen state. No traffic lights,
  profiles/presets, READY badges, details buttons, option-count expanders, or extra editor routes returned.
- Kept Home live performance cards/sparklines, Party Sync, quick controls, and the Safe-by-default note; kept the
  exact **Home → Gameplay → Dungeon → Farming → Mining → Fishing → Visuals → System** order and no-feature Farming page.
- Audited the flat settings contract: all `CHOICE` selectors have at least two values and each direct dropdown
  disclosure is owned by its listed tab. Existing one-level accordion/search/reset/master-off behavior remains intact.
- Added a tools-only Java2D Apple layout renderer using the production window/header/rail/control/catalog models.
  Java-8 CI renders Home, Dungeon, and GUI-scale-4 PNG preview evidence, validates the renderer source contract,
  and uploads the three previews without adding generated images to `dist/`.
- Updated release verification to enforce the restored Apple shell, two-action title bar, profile-free rail, and
  flat choice audit. Updated display metadata, build contract, documentation, and verifier examples to
  `Beta-0.10.9.1v` / numeric `0.10.9.1`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input/gameplay pass remains required.

## Beta-0.10.8.9.8v — 2026-10-26

- Flattened every visible settings owner to one disclosure level with compact plain section labels, fixed toggle and
  chevron columns, standard `38 × 20` master pills, 150 ms clipped inner-panel expansion, 8 px expanded padding,
  dimmed-but-editable master-off options, per-page accordion state, and search-driven disclosure.
- Replaced reset rows/dropdowns with one compact trailing **Reset to defaults** link per expanded feature while
  preserving the existing owner reset behavior and exact configurable-id inventory.
- Added distinct no-chevron **Edit** rows for Boss Health HUD, HUD Editor, and Item Animation; made Hurt Camera Shake
  an inline slider, kept Spirit Leap master/key controls together, and retained Terminal Helper as a no-master live
  `n / 5 on` dropdown.
- Rewrote player-facing catalog text to short Title Case/sentence case and added the documented old-to-new text table.
- Replaced the accessible Item Animation HUD-editor target with `ItemAnimationEditorScreen`: an undimmed left-edge
  panel with Close X, requested renderer controls, Preview Swing, Reset to Defaults, and green `Hook: active` / plain
  `Hook: failed (reason)` status. Retired Item Animation background/transparency/opacity keys remain ignored.
- Added depth/count/accordion/search/copy/binding/scale/panel-exclusion regression coverage and updated the release
  JAR verifier for the standalone panel contract.
- Updated display metadata, Forge metadata, build contract, verifier examples, documentation, and archive references
  to `Beta-0.10.8.9.8v` / numeric `0.10.8.9.8`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input/gameplay pass remains required.

## Beta-0.10.8.9.7v — 2026-10-24

- Replaced retired feature-specific settings screens with persisted, collapsed inline dropdowns in
  `AppleSettingsScreen` and `ReferenceSettingsCatalog`. Existing feature owners and configuration keys remain
  unchanged; disclosure only is stored under additive `ui.feature.dropdown.<id>` keys.
- Set the exact eight-destination order to **Home**, **Gameplay**, **Dungeon**, **Farming**, **Mining**,
  **Fishing**, **Visuals**, **System**. Moved genuine Mining and Fishing controls to their matching channels;
  Farming remains the explicit **No farming features yet** empty state with no automation added.
- Preserved every former settings value/action inline: Chat's five groups and custom tabs, Item Lock / Swap,
  Slayer, Terminal Helper, Spirit Leap, map, Boss HUD, M7, compact Mob/Door ESP, Dungeon Sync, mining,
  fishing, appearance/performance/interface/commands resets, and direct owner bindings.
- Removed generic focused-editor, Chat, ESP, Fishing, quality-of-life, and mod-settings presentation classes along
  with sample/demo-only chat, inventory/swap, ESP/door, Spirit Leap, and held-item editor surfaces. Only HUD Editor
  and Item Animation Editor remain reachable; Item Animation retains the real hand renderer and Preview Swing.
- Standardized feature-master pills to `38 × 20`, made search reveal collapsed ancestors, retained dimmed-but-editable
  dependent options, made Terminal Helper a no-master status dropdown, and added persisted disclosure, old-owner,
  option inventory, ESP table, scale-1–4 geometry, and allowed-editor regression coverage.
- Applied the Safe-by-default one-line layout and corner-bracket fullscreen glyph. Sky VeLor does not draw external
  launcher MOUSE/GUI indicators.
- Updated display metadata, generated Forge metadata, build contract, verifier examples, documentation, and archive
  references to `Beta-0.10.8.9.7v` / numeric `0.10.8.9.7`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input/gameplay pass remains required.

## Beta-0.10.8.9.6v — 2026-10-24

- Finalized the native Forge settings polish with one neutral-dark `Theme` token authority, blue-only positive
  interaction accent, semantic status colors, shared type/spacing/radius/shadow tokens, and 120–180 ms control motion.
- Merged **Sky VeLor · Settings** into the primary header, retained exactly Fullscreen and Close there, narrowed the
  rail, made the lower strip search-only, and replaced traffic-light child-editor chrome with explicit neutral Close
  icons. Parent child controls are direct: READY/OPTIONAL chips, chevrons/count disclosures, and expanders are gone.
- Removed visible legacy theme/base-surface/accent selectors while retaining Corner radius and Panel opacity. Historic
  appearance keys continue to load and save safely, but do not fork active GUI rendering.
- Added scale-1–4 right-control/no-overlap proof, Theme/config regression coverage, a scoped hardcoded-GUI-colour
  allowlist test, and release verifier/documentation updates. No gameplay or feature behavior changed beyond the
  previously documented removal of the in-game FPS/memory/ping HUD output.
- Updated display metadata, generated Forge metadata, build contract, verifier examples, and documentation to
  `Beta-0.10.8.9.6v` / numeric `0.10.8.9.6`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input pass remains required for visual confirmation.

## Beta-0.10.8.9.5.5v — 2026-10-24

- Added one shared cached, per-corner rounded-rectangle primitive for Sky VeLor GUI surfaces. It clamps each radius,
  emits filled/outlined geometry with a one-GUI-pixel anti-alias fringe, and is used by the settings shell/cards/controls,
  editor and preview surfaces, popups/tooltips, click menu, and chat background. Named radii and 120–180 ms GUI-open
  hover, toggle-slide, and tab easing keep the visual language consistent without changing feature behavior.
- Added the exact direct rail order **Home**, **Gameplay**, **Dungeon**, **Farming**, **Visuals**, **System**. Gameplay
  groups existing Quality of Life, Mining, Slayer, and Fishing presentation cards; Visuals retains HUD Hub and Item
  Animation presentation. Existing IDs, config owners, routes, and persisted keys remain unchanged.
- Audited Farming: no genuine crop/farming counter, display, or config owner exists in this source, so the Farming move
  list is empty. Its wheat/sprout destination is searchable and its page intentionally says only **No farming features
  yet**. No farming automation, macro, auto-clicking, auto-breaking, or movement behavior was added.
- Applied the shared 4/8/12/16 GUI-pixel spacing scale: 30 px full-rail and 28 px compact-rail destinations use exact
  4 px gaps; Profile is bottom-docked; cards/rows use 8 px gaps and 12 px padding; narrow settings and Home controls
  reflow to one column. `/velor layoutdebug` now registers clipped settings rail, Profile, card, row, and Home bounds.
- Replaced active selectable palette rendering with one neutral-dark Theme authority and a blue-only positive accent.
  Existing `appearance.settingsTheme`, `darkMode`, and accent keys still load/save for old configuration compatibility,
  but no visible selector or active Theme branch uses them.
- Merged **Sky VeLor · Settings** into the header, made the lower strip search-only, narrowed the rail, and removed
  READY/OPTIONAL chips, chevrons/count disclosures, and parent-row expanders. Children render directly under their
  owners; right-side controls share a scale-tested alignment policy.
- Added scale-1–4 rail/reflow/control-edge/no-overlap contracts, a scoped hardcoded-GUI-colour allowlist test, release
  JAR package checks, and updated GUI documentation. Updated display metadata, Forge metadata, build contract,
  documentation, and verifier examples to `Beta-0.10.8.9.5.5v` / numeric `0.10.8.9.5.5`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input pass remains required for visual confirmation.

## Beta-0.10.8.9.5v — 2026-10-23

- Fixed the main settings desktop lifecycle and rendering cleanup around HUD Editor/HUD Hub Editor transitions. Shared UI/OpenGL frame state, scissor state, item/entity previews, HUD-element matrix/alpha, inventory item passes, and editor-only hover flags now have exception-safe restoration; the returned parent reapplies its selected palette on init and draw.
- Added a bounded title-bar drag contract for the settings window. Normal, larger, and minimized windows retain a centered startup position, clamp a moved window inside the visible world border, and rebuild the exact same layout bounds for rendering and input after dragging or GUI-scale resize.
- Reorganized the visible rail into **Home**, **Quality of Life**, **Mining**, **Slayer**, **Fishing**, **Dungeon**, **HUD Hub**, and **System**. Existing mining, Slayer, and Fishing controls moved only to their matching real categories; no empty Farming destination was fabricated because this source has no genuine farming setting.
- Renamed **Gameplay** to **Quality of Life** and **Visuals** to **HUD Hub**. Removed the visible **Advanced** toolbar control, reclaimed its search space, and made former Advanced-marked cards, children, and search matches directly reachable without deleting their controls.
- Added persisted **Settings Theme** selection with existing **Forest & Cyan** and new **Black & Blue** palettes. Black & Blue applies near-black backgrounds, blue/cyan accents, light text, and subtle cards/borders across the settings shell, navigation, dropdowns, popups, and focused settings UI.
- Added/updated headless contracts for direct category ownership, ungated search/children, existing Mining/Slayer/Fishing routes, absence of a fabricated Farming tab, theme persistence/palette, drag/clamp geometry, and packaged rendering-state restoration; strengthened release-JAR verification accordingly.
- Updated display metadata, generated Forge metadata, documentation, verifier examples, and build contract to `Beta-0.10.8.9.5v` / numeric `0.10.8.9.5`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input pass remains required for final visual confirmation.

## Beta-0.10.8.9.4v — 2026-10-23

- Refined the native five-destination settings shell rather than replacing it: normal mode is a centered 85% × 82% desktop window before fixed maximums, expanded mode is larger, small GUI scales remain scrollable/readable, and a world border stays visible.
- Made the top-left desktop circles functional: red closes to the game, yellow minimizes/restores a compact strip, and green toggles normal/larger layout. The compact profile popup identifies creator `pekadeco` and contributors `ii88w`, `whiteShadow`, with X/Escape/outside-click dismissal.
- Removed Home's Vanilla+, Quality of Life, Dungeon, and Minimal setup cards and their application workflow. Removed the visible Done button and reclaimed its search-toolbar space; close, Escape, and red retain save-on-close persistence.
- Added Home's cached Live Performance panel for actual FPS, Java used/max heap MB, and local tab-list ping. It reuses the 500 ms `ClientMetrics.Reader` snapshot, shows disconnected ping as `-- ms`, and remains separate from the in-game three-card Stats HUD.
- Audited fresh defaults without overwriting old files: local inventory presentation conveniences default on only for an empty configuration; sync, automation, gameplay/input assists, M7 overlays/solvers, and fresh Dungeon ESP stay opt-in. Schema v3 marks upgrades without rewriting explicit saved choices.
- Added headless contracts for normal/larger/minimized geometry, Home preset removal, profile credits, fresh/legacy defaults, M7 persistence, and retained disconnected metric handling.
- Updated display metadata, generated Forge metadata, verifier examples, documentation, and build contract to `Beta-0.10.8.9.4v` / numeric `0.10.8.9.4`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render/input pass remains required for visual confirmation.

## Beta-0.10.8.9.3v — 2026-10-23

- Ported the supplied `analyze-sky-velor-mod-code.zip` React/Vite GUI proposal into native Forge 1.8.9 rendering/input code. No browser runtime, React dependency, or replacement configuration schema was added.
- Replaced the visible seven-section Hub construction path with the reference's five-destination rail: **Home**, **Gameplay**, **Dungeon**, **Visuals**, and **System**. The former `AppleHubScreen` remains only as a binary-compatible alias and is no longer a visible entry point.
- Added the reference toolbar (global search, **Advanced**, **Done**), one scissored/smoothed content viewport, responsive vertical/compact rail, bordered card surfaces, inline child disclosure, Home presets/quick controls, card-level notices, and native switches/sliders/choices/focused-editor actions.
- Mapped live current owners into the reference hierarchy without removing behavior: Gameplay (Comfort, Inventory, Chat, Skills, Shortcuts), Dungeon (Run tools, Floor VII, Highlights, Party sync), Visuals (HUD, Item Animation), and System (Appearance, Performance, General, Diagnostics, About). Existing focused editors, config keys/defaults, Item Animation, HUD Editor/HUD Hub Editor, Dungeon ESP, terminal settings, and all retained routes survive.
- Restored Diagnostics' read-only in-settings Frame Rate/Ping/Memory telemetry without registering another HUD. The reference-style **Stats Display** remains the only consolidated in-game Frame Rate/**MINING**/**MEMORY** renderer: fixed dark beveled 172 × 102 cards with cyan FPS/BPS values, white memory, 9 px gaps, 8 px cuts, 2 px dark offsets, and centered top placement. Historical/source evidence resolves the middle card to Mining BPS, not Ping.
- Kept safe legacy Stats visibility migration: an enabled old `fps-display.*`, `ping-display.*`, or `memory-display.*` element enables Stats Display once only when no explicit `stats.enabled` value exists. Retired metric keys are removed before loading active elements, preventing duplicate renderers; fresh Stats Display remains opt-in.
- Migrated headless/release verification from obsolete seven-section expansion claims to the reference catalog, rail/card hierarchy, Advanced/search/inline-detail routes, responsive geometry, focused-editor reachability, and Stats preservation. Dungeon ESP 0.10.8.9.2 behavior remains unchanged.
- Updated display version to `Beta-0.10.8.9.3v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.9.3`.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render pass remains required for final visual confirmation.

## Beta-0.10.8.9.2v — 2026-10-23

- Fixed the focused **Dungeon ESP Settings** editor regression introduced by its secondary visual sample: preview renderers now flush before using their world-style GL path and the screen restores Apple UI depth, texture, blend, cull, alpha, and color state before drawing controls. A failed preview cannot leave the settings pane clipped or depth-hidden.
- Made the focused editor responsive: when a Minecraft GUI scale cannot fit both a readable settings column and preview, it uses one full-width scrollable settings column. Wide windows retain the secondary visual sample with a minimum-width control column. Existing category scrolling, dropdowns, Back/Reset behavior, remembered selection, direct configuration owners, and save-on-close behavior remain.
- Redesigned **Dungeon Mob ESP** display without changing detection: cached/interpolated boxes retain each category's existing style, while a bounded hierarchy-first category badge identifies **MINIBOSS**, **SPECIAL**, **ENDERMAN**, **SKELETOR**, **STARRED**, or **HOSTILE** targets. Badges use each category color, a restrained contrast backing, existing through-wall behavior, closest-first ordering within a category priority, and short world-space lanes to avoid needless overlap.
- Added persisted **Mob Category Labels** and **Maximum Mob Labels** controls (default on / 16), while retaining the existing independent **Distance Text** setting as an optional distance suffix. Disabling category labels restores the old distance-only behavior; Default/Subtle presets use readable 16/10 badge budgets.
- Kept detector/filter behavior, category styles, box/tracer controls, door ESP, cached scans, range caps, and stale/dead-target cleanup intact. `/velor espdebug` now reports the live rendered badge count alongside tracked/rendered entities.
- Added focused persistence/bounds, live editor-widget ownership, responsive preview-layout, and label hierarchy/distance-cap tests; strengthened the release verifier to require the redesigned renderer and the GUI-state restoration path.
- **NOT tested in-game.** A real Minecraft 1.8.9 Forge render pass remains required for final visual confirmation.

## Beta-0.10.8.9v — 2026-10-23

- Rebuilt the focused settings editors around a responsive scrollable settings pane and scissored secondary visual sample surface; previews update the real shared state immediately.
- Moved cached local FPS, tab-list Ping, and used/max Java heap telemetry into color-coded **General** GUI cards. Retired FPS/Ping/Memory HUD elements and their old keys are safely ignored.
- Historical category wording that placed Mining Assistant under **Farming** was superseded by the direct **Mining** ownership audit; no Farming category is claimed without a genuine farming control.
- Added the reproducible `SkyVeLor-1.8.9-Beta-0.10.8.9v-source.zip` source delivery. CI creates it with a rooted `git archive` allow-list, verifies it with `tools/verify_source_zip.py`, uploads it as an artifact, and publishes exactly the JAR, delivery ZIP, and source ZIP in `dist/`.
- Rechecked the prior cleanup claims with `ReleaseClaimsTests` and the release-JAR verifier: no custom font subsystem or HUD target sidebar remains; No Hurt Cam/Auto Sprint are simple toggles; retired Hotbar customization and Item Protection are absent while Item Lock and Swap remains routed.
- Updated display version to `Beta-0.10.8.9v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.9`.
- **NOT tested in-game.**

## Beta-0.10.8.8.2v — 2026-10-23

- Restored independent default-off **FPS Display**, **Ping Display**, and **Memory Display** HUD elements with cached, roughly once-per-second text updates.
- Removed the optional custom-font system, generated font texture, font resource-pack/reload path, bundled font assets/licenses, font settings, and all custom font-renderer hooks; Sky VeLor now uses Minecraft's vanilla font only.
- Simplified HUD editing to direct canvas selection: click an enabled or dimmed disabled element to select it, cycle overlaps by clicking again, and drag the selected element to move it.
- Added default-off visual-only **No Hurt Cam** and default-off **Auto Sprint**; Auto Sprint only owns the normal vanilla sprint key while forward/sprint conditions hold and sends no custom packets.
- Removed the retired Hotbar HUD customization (nine-slot renderer, selected-slot highlight, panel styling, controls and route) while retaining normal gameplay hotbar behavior and Item Lock and Swap hotbar icons.
- Removed only the distinct UUID/persistent-store **Item Protection** feature; **Item Lock and Swap** remains intact.
- Ordered Dungeon rows by usage: Door ESP and Mob ESP; Dungeon Sync, Floor VII (M7) Overlay, Spirit Leap GUI, and Terminal Assist; then Boss HP, Dungeon Map, and Sync Details.
- Standardized every Hub dropdown body to **On/Off** plus **Settings**, gave focused screens `<Feature> Settings` titles, and simplified labels to **No Hotbar Scrolling**, **Dungeon Sync**, and **Sync Details** without changing config keys.
- Removed-feature and former-presentation config keys continue to load harmlessly and are ignored where their owners no longer exist.
- Updated display version to `Beta-0.10.8.8.2v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.8.2`.
- **NOT tested in-game.**

## Beta-0.10.8.8.1v — 2026-10-23

- Replaced Hub feature trees with a flat two-form surface: **Simple** rows contain only the existing On/Off switch, name, and gray description; one-level **Dropdowns** contain a title, accurate green/red status dot, that same existing switch, and one correctly named editor button.
- Removed tree arrows, connector lines, indentation, nested dropdowns, per-mob/per-door tab rows, and every child-row settings button. Simple rows are ordered before dropdowns in every tab.
- Kept detailed controls in their established focused owners: HUD/Item Animation, Chat, Mod Settings, the shared Feature Settings Editor, and Mob/Door ESP editors. No feature behavior or configuration schema was changed.
- Simplified the shared editor left pane to a flat list. Mob/Door category entries expose their existing individual toggles and compact previews; Mob style pages add **Apply This Style to All** while Global retains performance and preset controls.
- Added flat-Hub model/audit tests for body shape, depth, routes, legacy-option reachability, status dots, category toggles/thumbnails, GUI-scale geometry, and old-config loading. Updated the release-JAR verifier for the flat architecture.
- Added the complete feature / row type / option-location audit in `docs/FEATURE-SETTINGS-EDITOR.md`.
- Updated display version to `Beta-0.10.8.8.1v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.8.1`.
- **NOT tested in-game.**

## Beta-0.10.8.8v — 2026-10-23

- Reworked settings presentation only: every crowded Hub tab now uses compact, persisted feature-tree rows with an optional master toggle, gray description, and one **Open Settings** route. Existing feature behavior, config keys/values, migration and old-config loading are unchanged.
- Added the reusable **Feature Settings Editor** framework with Back/Reset, remembered category selection, scrollable left navigation/tree and settings pane, shared responsive geometry, and the existing dropdown palette style.
- Moved Mob ESP and Door ESP into focused editors. Mob ESP lists Global plus Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor and Other Mobs; Door ESP covers global filters plus Wither/Blood doors. Both retain all prior options, Default/Subtle/Reset and session-only copy/apply style actions.
- Added an in-editor safe named hitbox/prism preview. Preview and live Dungeon ESP both emit style geometry through `DungeonEspRenderBatch.drawStyledBox`; no dummy world/entity is constructed.
- Routed detailed Appearance, Performance, assist, Item Lock & Swap, Spirit Leap, Terminal, M7, Catch Counter and Slayer controls through the framework. Existing HUD Editor, Item Animation editor, Chat Editor and Mod Settings remain their focused owners.
- Added headless persisted-tree, connector/indentation, reachability and Dungeon ESP control-route coverage; updated release-jar audit checks for the compact editor architecture.
- Updated display version to `Beta-0.10.8.8v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.8`.
- **NOT tested in-game.**

## Beta-0.10.8.7v — 2026-10-23

- Added a clean-room assist foundation with namespaced, bounds-checked, old-key-compatible settings and a one-place Hub catalog. New game-affecting assists default to off; new keybinds are unbound and registered under **Sky VeLor**.
- Added inventory search/filter dimming, potion-panel hiding, Roman tooltip rendering, long-tooltip scrolling, rarity tints, UUID-tagged item protection with persistent allow-list, local no-hurt camera, opt-in Auto Sprint, and Fakepixel-gated Wardrobe/Pets shortcuts.
- Added Mining Assistant and Boss HP HUD elements, pickaxe-ready alerts, a read-only Dwarven Mines Puzzler marker, protected boss-health parsing, and HUD-editor registration without moving existing HUD positions.
- Added guarded F7/M7 terminal visual helpers, known-wrong dim/block behavior, optional constrained auto-click, and local terminal/device waypoints. Unknown, malformed, duplicate, or stale containers deliberately do nothing.
- Added opt-in Dungeon Sync: a local SHA-256 session derivation, WSS-only Java 8 transport with verified handshake/client masking/bounded frames, allow-listed inbound puzzle data, bounded map-memory integration, reconnect limits, debug-safe status, and the privacy disclosure in `docs/DUNGEON_SYNC.md`.
- Added clean-room visual hook support for inventory tooltip filtering/rarity rendering and local hurt-camera suppression, with transformer status in `/velor selftest`; existing Item Animation, Chat QoL, settings, item lock/swap, Dungeon ESP/doors, HUD editor, sea-creature counters, and CorruptedHP behavior remain in place.
- Added headless policy/config/parser/identity/reconnect/transformer tests for the new code. Runtime behavior still needs a real Minecraft 1.8.9 Forge pass.
- The friend archive was compared only against its declared `Beta-0.10.6v` base. No archive is retained in the release tip; research-derived solvers were independently recreated only where safely supportable, with no third-party source copied.
- Updated display version to `Beta-0.10.8.7v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.7`.

## Beta-0.10.8.6.4.1v — 2026-10-21

- Consolidated every Dungeon ESP control into the **Mob ESP**, **Door ESP**, **Performance**, and **Presets** collapsible groups on the Dungeon page. Every control has a gray one-line explanation; responsive shared row geometry reserves distinct label/control columns at every GUI scale.
- Fixed starred **Crypt Souleater**, split armor-stand tags, tall-body association, star variants, real Endermen, and the Dungeon Enderman-head marker. Matching consumes the nearest eligible body once, tracks the real body hitbox, and uses a readable fixed box for head-marker armor stands. Added `/velor espdump` nearby-entity/chat-log diagnostics.
- Added independent **Skeletor** and **Other Mobs** styles, plus off-by-default **Force Render All Mobs in Room** cached coverage. It includes only eligible hostiles by default, keeps bats/invisible mobs opt-in, excludes NPCs/pets/dead/passive entities, and does not duplicate tagged targets.
- Changed new wither-door boxes to red and blood-door boxes to dark red. Existing stored former defaults migrate once; non-default custom colors remain unchanged.
- Added headless coverage for split/tall tags, Crypt Souleater, real/head Endermen, Skeletor, force inclusion/exclusion/no-duplicates, color migration/config persistence, and small/large GUI geometry.
- Enderman-head behavior was implemented clean-room from runtime compatibility data; no external source or notice is included.
- Updated display version to `Beta-0.10.8.6.4.1v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.6.4.1`.

## Beta-0.10.8.6.4v — 2026-10-21

- Replaced the old per-frame **Mob highlights** renderer with one cached Dungeon ESP: it recognizes yellow-star (`✮`/`✯`) armor-stand tags, associates the loaded living body below each tag on configurable client ticks (default 5), draws the body’s interpolated real hitbox, and removes dead, zero-health, despawned, or unpaired targets. Plain `[Lv]` tags are no longer highlighted.
- Added independently implemented starred, miniboss, and special-starred categories with color-palette selection, outline/filled/both mode, per-category through-wall behavior, edge width, outline/fill opacity, global range, tracer, distance text, and max-box controls. The Dungeon tab now has shared-layout collapsible **Dungeon ESP**, category, Global, Doors, and Presets groups with Default/Subtle/reset profiles.
- Added cached closed wither/blood-door ESP for the observed 3×4×3 coal/red-stained-clay prism behavior. Door discovery is timer-driven, cache entries are invalidated when opened, and UI controls cover nearest/all, key-only, blood visibility/color, rendering, and distance text.
- Added `/velor espdebug` live dungeon state, tracked/rendered counts, scan/render timings, scan interval, door-cache size, and bounded-cache drops.
- Added mock-500 entity storage-reuse/allocation regression coverage, tag/body delay/death/classification tests, sample door-layout/invalidation tests, persisted Dungeon ESP migration/bounds/preset tests, and shared GUI-scale overlap coverage.
- Source research was clean-room only; no external source or notice was copied. The superseded `MobHighlight`/`MobMarkers` implementation was removed.
- Updated display version to `Beta-0.10.8.6.4v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.6.4`.

## Beta-0.10.8.6.3.1v — 2026-10-20

- Fixed paired Item Swap endpoint taps: pressing and releasing the default **L** on either endpoint now removes and persists that pair instead of locking the slot, removes its badges/line on the next overlay frame, and reports **Swap removed**. Existing A→partner drag removal and all other drag decisions are unchanged.
- Fixed Default Chat `/msg` chronology with an immutable monotonically increasing arrival sequence. All, Party, Reply/custom, and closed-chat rebuilds now use sequence order rather than wall-clock timestamps, so A then B renders A above B even for same-millisecond arrivals; compact counter refreshes retain the original line position and do not add an echo entry.
- Extended `/velor chatdebug` to report the last 10 logical server arrivals with each sequence number, active tab, matching tabs, and top-to-bottom display order.
- Added endpoint-tap/persistence and same-millisecond/compact-position chronology regressions; updated the release verifier to audit the paired-tap feedback/persistence and sequence-order chat paths.
- Updated display version to `Beta-0.10.8.6.3.1v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.6.3.1`.

## Beta-0.10.8.6.3v — 2026-10-20

- Fixed Item Swap pairing by replacing the unreliable key-up/click selection flow with one physical-key poll in `GuiScreenEvent.DrawScreenEvent.Post`: press L over A, move to B, and release L to create A↔B without any mouse click.
- Releasing over A now toggles its lock at any hold length; empty-space release cancels; paired A→partner removes the pair; paired A→other, locked/already-paired targets, duplicate endpoints, and a ninth pair refuse without mutating saved pairs.
- Added a held-150 ms instruction, live rounded start-to-cursor line, valid/invalid target ring, and persistent rounded pair-center lines drawn above items but below tooltips at every GUI scale.
- Added **Swap lines** (Always/On hover/Off), line tint, opacity, and 1–4 px width controls. Existing `itemLockSwap.pairs` data and old configs load unchanged, with Always/2 px line defaults.
- Extended `/velor lockdebug` with physical key state, drag state/start/hover slots, and the last release decision. Added mock-grid/mock-keyboard polling, release, pair/lock/cancel/repeat/close/click-swallow, armor/hotbar/max-pair, config, and line-endpoint regressions.
- Updated display version to `Beta-0.10.8.6.3v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.6.3`.

## Beta-0.10.8.6.2.1v — 2026-10-20

- Added client-side Item Lock and Item Swap: persistent 0–39 player-inventory position locks, up to eight pairs, default-L tap/hold selection, manual paired-slot swaps with armor-fit and cooldown protection, configurable shape badges, and the Quality of Life dropdown.
- Added fail-closed `PlayerControllerMP.windowClick` plus `EntityPlayerSP.dropOneItem` guards for click modes 0–6 and Q/Ctrl+Q, a bounded internal-swap bypass, `/velor lockdebug`/`/velor selftest` diagnostics, mapping/config/UI/policy/transformer tests, and release-jar audit proof.
- Updated display version to `Beta-0.10.8.6.2.1v`; generated Forge `mcmod.info`/build metadata resolve numeric version `0.10.8.6.2.1`.

## Beta-0.10.8.6.2v — 2026-10-19

- Added a bounded 500-entry local incoming-message history (rich component, clean text, compact count, timestamp). Tab views now filter and rewrap that history instead of vanilla's short fading cache; compact updates refresh their original entry, and the history clears only on disconnect.
- Fixed closed Default Chat to preserve the remembered selected All/Party/Reply/custom filter, retain other-tab unread state, and fit its background only to rows currently visible for that selection.
- Corrected chat-only wheel direction: `+120` moves toward older rows and `-120` toward newer rows, with default/custom speed, Shift fine scrolling, smoothing, and endpoint clamping isolated from hotbar scrolling.
- Routed every open context-menu mouse event through one captured-state handler. Either button executes an item only on matching mouse-up; outside/tab clicks close and consume, while native links and `[Reply]` left actions remain intact.
- Updated display version to `Beta-0.10.8.6.2v`; generated Forge `mcmod.info`/build metadata resolve numeric version `0.10.8.6.2`.

## Beta-0.10.8.6.1.1v — 2026-10-19

- Fixed closed Default Chat visibility: closed HUD chat now evaluates **All**, clears stale open-tab scrolling, preserves vanilla fit/line-limit diagnostics, and `/velor chatdebug` reports the newest 15 logical messages with closed/open visible-or-hidden reasons.
- Reworked **Compact messages** to replace only immediate formatted duplicates with fresh high per-group ids, preserve original rich copy/click/hover/`/msg` data, append a gray `(xN)` suffix after wrapped text, and refresh fade on replacement.
- Put **General** first. **Quality of Life** now contains expandable Gameplay, **Chat QoL**, and **Commands** groups; Chat QoL master/toggles/editor route and every command setting remain available.
- Kept Item Animation renderer-only: no background/transparency/opacity control is exposed through its HUD editor or HUD dropdown; retired configuration keys remain load-compatible.
- Updated display version to `Beta-0.10.8.6.1.1v` and numeric Forge `mcmod.info`/build metadata version to `0.10.8.6.1.1`.

## Beta-0.10.8.6.1v — 2026-10-19

- Made Chat QoL **Compact messages** and **No block particles** default on. Schema v2 migrates older configurations once, then preserves either user choice on later restarts.
- Added a dedicated top-level **Quality of Life** tab with exactly **Disable hotbar scrolling** (default off) and **No block particles**; the former mixed QoL overview is retained as **General**.
- Kept scroll speed and smooth scrolling in **Mod Settings** only. Removed its duplicate Item Animation and Chat QoL master entries; Item Animation remains in HUD and Chat QoL retains its own master control.
- Updated display version to `Beta-0.10.8.6.1v` and numeric Forge `mcmod.info`/build metadata version to `0.10.8.6.1`.

## Beta-0.10.8.6v — 2026-10-18

- Added opt-in **Disable hotbar scrolling** in Mod Settings → Mouse. `PekaClientMod` cancels only gameplay wheel input while no GUI is open; inventory/chat scrolling and 1–9 keys remain vanilla.
- Added Chat QoL **Compact messages** with formatting-exact consecutive-message tracking, a fixed high-ID vanilla-gray `(xN)` line, original-message copy/reply/tab payloads, refreshed fade/arrival placement, and an adjustable Chat Editor counter colour.
- Added opt-in **No block particles**, a fail-closed 1.8.9 `EffectRenderer.addEffect(EntityFX)` core hook that filters only `EntityDiggingFX`, plus `/velor selftest` hook diagnostics.
- Added default-on visual-only **Smooth swing** and Smoothness controls in the Item Animation HUD editor. Swing blur/Intensity are intentionally unavailable because safe translucent first-person ghost passes are not supported by this narrow hook.
- Spirit Leap now evaluates the established dungeon scoreboard detector at GUI-open time and leaves uncertain/non-dungeon menus vanilla; `/velor selftest` reports `inDungeon`, reason, and the matched sidebar line.
- Updated display version to `Beta-0.10.8.6v` and numeric Forge `mcmod.info`/build metadata version to `0.10.8.6`.

## Beta-0.10.8.5.4v — 2026-10-18

- Restyled Chat QoL tabs with semi-transparent black unselected surfaces, blue selected surfaces, lighter hover treatment, white labels, and preserved unread dots. The Tabs editor now exposes independent unselected/selected colours and opacity controls; Reset restores black/blue defaults.
- Extended the Party filter beyond `Party >` chat to typical Hypixel-style party command results: invitations, joins/leaves, kicks, disbands, transfers, promotions, warps, and party member/list output. Matching messages remain in All.
- Consolidated the copied-message menu into one captured `ChatMenuState` for both mouse paths. Copy to chat, clipboard, and Message player retain that state until after action completion; empty unformatted composite chat roots now fall back to visible formatted text.
- Removed the in-box channel/mode label and its input-width reservation. Tab prefixes still send Party through `/pc`, Reply through `/r`, and All normally.
- Updated display version to `Beta-0.10.8.5.4v` and numeric Forge `mcmod.info` version to `0.10.8.5.4`.

## Beta-0.10.8.5.3v — 2026-10-18

- Changed the opt-in Default Chat background to follow only currently visible message rows. Closed chat has no panel when no rows are visible; open chat defaults to **Fit to messages** with optional **Full area** and **Fit width to text** controls. The rounded shape eases between live message bounds and clears on the final fade.
- Retired the Chat Editor's **Show tabs** and **Menu animation** controls. Tabs draw only with an open chat, and the copied-message menu now opens/closes instantly while retaining hover feedback. Old setting keys are ignored and removed on save.
- Restyled tabs around a translucent blue base with white text, stronger active opacity, light hover, unread dots, custom accent underlines, and Tabs-section colour/opacity reset controls.
- Added the stored-message **Message player** action. It detects safe Fakepixel-style names from ranked/badged/party/From/To lines and prepares a bounded focused `/msg <Name> ` command without changing copy or component-click behavior.
- Retired Item Animation background transparency compatibility keys; the renderer-only Item Animation panel and HUD dropdown expose no such control, while old configs load safely and rewrite without it.
- Updated display version to `Beta-0.10.8.5.3v` and numeric Forge `mcmod.info` version to `0.10.8.5.3`.

## Beta-0.10.8.5.2v — 2026-10-18

- Replaced the fragile split-chat runtime with one guarded Forge renderer for the real Default Chat. Chat 1/Chat 2 windows, classifier and pattern routing, target/link-all editor state, and their persisted output are removed; retired keys are harmlessly ignored and omitted on save.
- Added Default Chat’s rounded configurable background, open/closed opacity, smooth fade/slide arrivals, eased scrolling, full wrapped-group highlight, safe native component forwarding, and a captured-at-open two-action copy menu.
- Reworked attached All/Party/Reply/custom tabs with a reserved message boundary, unread dots, active channel label/prefix, open-only/always visibility, up to eight total tabs, and a focused editor for custom filter validation/testing, colour, sound, order, visibility, and deletion.
- Reduced Chat Editor to **Background**, **Messages**, **Tabs**, **Menu and Highlight**, and **Reset** with native-metric, clipped non-overlapping flow. `/velor chatdebug` now reports the one renderer’s live tab/group/menu state; `/velor layoutdebug` covers its shared layout.
- Added headless coverage for custom filtering/invalid regex/All membership, retired-key omission, captured menu payloads, grouped highlight bounds, tab geometry, and editor flow at compact scales.
- Updated display version to `Beta-0.10.8.5.2v` and numeric Forge `mcmod.info` version to `0.10.8.5.2`.

## Beta-0.10.8.5.1v — 2026-10-18

- Fixed Chat Editor section/card placement through native Minecraft text metrics, with a collision resolver for small windows, GUI scales, and expanded controls.
- Made Separate Chats a single opt-in ownership mode: off is fully native Default Chat; on exclusively routes new classified traffic to Chat 1 or Chat 2, leaves pre-toggle vanilla history untouched, and clears the opt-in mirror on exit. Unknown, system, command, and server-result traffic now safely belongs only to Chat 2.
- Repaired the production chat input insertion path for **Copy to chat box**: clean text is inserted at the focused cursor or appended, capped at 256 characters, while the chat field stays open and focused. The existing clipboard action and copied confirmation remain available.
- Replaced recursive menu hover smoothing with a real elapsed-time ease-out that reaches its configured default 100 ms target; the Chat Editor exposes **Open menu with** (Right click / Left click / Both) and **Menu animation**, including Off.
- Made message hover and context selection block-based across wrapped rows and rebuilt safely on layout changes. Left click preserves component actions while right click always opens the menu; outside clicks and Escape dismiss it.
- Added `/velor layoutdebug` bounds/overlap diagnostics and `/velor chatdebug` grouped-message outlines, recent routing evidence, and stored/GUI/router/renderer Separate Chats state. Optional vanilla button discovery is guarded and fail-closed.
- Updated the display version to `Beta-0.10.8.5.1v` and numeric Forge `mcmod.info` version to `0.10.8.5.1`; release metadata validation now accepts the five-component numeric release form.

## Beta-0.10.8.5v — 2026-10-18

- Reworked all Sky VeLor-owned screens around the shared `GuiLayoutToolkit`: clamped shells, measured compact rows, ellipsis, clipping, common scrollbar semantics, Shift fine scrolling, and responsive small-scale layouts. Quick Note now switches to a non-overlapping compact editor instead of retaining a fixed sidebar.
- Routed Default Chat, Chat 1, and Chat 2 through the production `ChatPipeline` for message wrapping, layout, full-row hit targets, hover/fade, shared scrolling, context-copy selection, and component-forwarding bridges. Vanilla chat remains unchanged when Chat QoL is off or its reflection bridge is unavailable.
- Consolidated scroll speed and smoothing into top-level **Mod Settings**. Retired per-target Chat QoL scroll keys are read once as a compatibility migration and omitted on the next save; one hovered viewport consumes each wheel event, with Shift fine scrolling.
- Updated release metadata and verification for display version `Beta-0.10.8.5v` with numeric Forge `mcmod.info` version `0.10.8.5`, and strengthened the release-JAR audit for the shared layout, chat pipeline, and global settings.

## Beta-0.10.8.4v — 2026-10-18

- Unified Default Chat, Chat 1, and Chat 2 message hit testing, eased hover state, right-click handling, and bounded scrolling through one native interaction model. Default vanilla-layout chat now receives the same opt-in highlight/menu path when Separate Chats is off.
- Replaced the copy menu with exactly **Copy to clipboard** (clean text) and **Copy to chat box** (inserts clean text). The menu is clamped, eased, dismisses on outside click/Escape, and right-click never fires a message component action.
- Restored native component behavior in Chat 1/Chat 2: wrapped/moved/scaled rows resolve the clicked component and forward left clicks through vanilla-compatible `run_command`, `suggest_command`, and `open_url` handling; native hover events show a safe tooltip.
- Added independent Default Chat / Chat 1 / Chat 2 appearance and scrolling profiles, Link all, Copy settings from, per-target resets, and a real customized Default Chat draw pass that keeps its cached vanilla wrapped lines and vanilla-compatible scroll/component lookup.
- Added draggable, screen-clamped, GUI-scale-persisted Chat Editor panel state with title double-click reset and persisted collapse state; the editor now exposes target selection, per-target colors/scale/background/border/spacing/shadow/appearance/fade/hover/scroll controls.
- Added headless checks for all three interaction surfaces, exactly two menu labels, moved/scaled hit coordinates, one wheel application, draggable/clamped/resettable panel state, independent/linked/copied target settings, and target persistence.

## Beta-0.10.8.3v — 2026-10-17

- Replaced the previous separate chat pages with one compact **QoL → Chat QoL** dropdown: a safe master switch, **Chat Tabs**, **Separate Chats**, **Smooth Messages**, **Hover Highlight**, **Right Click Menu**, and **Open Chat Editor**. All customization now belongs only to the editor.
- Added a native Apple-style **Chat Editor** (also `/velor chateditor` and an unbound keybind) with clipped, non-overlapping **Windows**, **Background**, **Messages**, **Tabs**, and **Hover / menu** sections, per-section resets, per-GUI-scale draggable/resizable snapped windows, editable names/limits/fade, pattern testing, and tab visibility/order controls.
- Reworked chat tabs to attached **All / Party / Reply** controls with unread markers, safe All fallback, typed-text-preserving switches, Ctrl+Tab plus a configurable cycle key, and `/reply` routing for Reply. Removed unsupported channel controls.
- Added optional native split windows: Chat 1 receives player/party/reply traffic; Chat 2 receives system and unknown lines. Classification has bounded editable rules, debug evidence through `/velor chatdebug`, and unknown traffic fails safely to Chat 2 and All.
- Added native separate-window hover highlighting and a safe right-click text menu for clean copy, copy-to-input, and optional formatted copy. Link/click/hover component actions are never replayed by the menu.
- Retired the old external Chatting animation configuration path. Legacy chat settings migrate to `chat.qol.*`; optional external-runtime detection remains silent and fail-closed, while existing packaged attribution notices remain intact.
- Added headless tests for Chat QoL ownership, migration/clamps, classification/routing, copy text, attached-tab non-overlay geometry, editor layout, and per-scale window bounds.

## Beta-0.10.8.2v — 2026-10-17

- Improved **Sea Creature HP** resilience for compatible Fakepixel-style nameplates: health-bearing display text now wins over a name-only tag, the scoreboard-composed entity name is considered, and the last valid rows persist for a bounded 1.5 seconds during entity replacement.
- Excluded **Water Hydra** and **Reindrake** from the Sea Creature HP compatibility profile by explicit review decision; Yeti and Lord Jawbus HP labels remain supported, including Corrupted/Corrupt forms already recognised by the parser.
- Added an optional, default-off local **Nearby Yeti / Lord Jawbus titles** toggle. It is transition-only, sends no packets, and does not change the established Lord Jawbus chat-arrival alert.

## Beta-0.10.8.1v — 2026-10-17

- Added **AllNonLegendaryCC**: Common through Epic sea-creature pulls are counted alongside existing Legendary/Mythic catches and shown in one optional compact **Common Creature (CC)** HUD row. Existing Festival rows and saved counters remain intact.
- Added optional **All / Party / Guild / PM / Co-op** chat tabs. They are off by default and fail open if another client mod changes Minecraft chat internals.
- Added an opt-in bridge for an already-installed compatible Chatting runtime; Sky VeLor packages no Chatting runtime and leaves external configuration untouched unless the bridge is enabled.
- Hardened Sea Creature HP scan scheduling and added a bounded armor-stand nameplate fallback for compatible server labels.

## Beta-0.10.8.0.1v — 2026-10-17

- Removed the **Auto Fix Hit/Slash Animation** option and its saved setting; the corrected renderer-only swing timeline, including held left-click/mining repeats, is now always on. Existing 0.10.8v config keys are safely ignored and omitted on the next save.
- Replaced the HUD tab’s Item Animation rows with the shared collapsible **Item Animation** section containing only its ON/OFF toggle and **Open Item Animation Editor** button.
- Removed the redundant HUD-editor Item Animation settings launcher. Item Animation remains a normal target in the editor list, and direct routes open its panel immediately.
- Moved duplicated HUD-element and Dungeon Map customization rows out of GUI tabs; tabs retain visibility toggles and editor links while the HUD editor remains the single customization surface.

## Beta-0.10.8v — 2026-10-17

- Put **Open the HUD editor** first in the Hub’s **HUD** tab and kept **Item Animation** directly below it with its master toggle and direct settings launcher.
- Reworked the HUD editor into a responsive fixed-row inspector with clipped labels, right-side controls, a real scrollbar, and compact Item Animation **Scale**, collapsible **Position**, **Animation**, and **Options** groups.
- Added decimal X/Y/Z exact inputs with saved-value compatibility, Enter/Escape handling, clamping, Shift fine control, slider double-click resets, and a Position-group reset.
- Added appended **Chop** and **Stab** visual styles without moving existing saved style indexes.
- Made renderer-only visual swing cycles repeat while attack/mining remains held, while retaining vanilla gameplay timing, packets, hit registration, and the existing smooth equip/swap interpolation.
- Added headless UI-layout/reachability and visual-swing state-machine tests for this release.

## Beta-0.10.7.9v — 2026-10-17

- Put **Item Animation** at the top of the Hub’s **HUD** tab with an ON/OFF toggle and **Open Item Animation Settings** launcher, and added the always-visible direct entry at the top of the HUD editor.
- Added `/velor itemanimation`, `/velor ia`, and the unbound **Open Item Animation** Controls binding; all three open the Item Animation HUD-editor panel directly.
- Expanded the visual-only panel with Scale, X/Y/Z, Remove Haste Effect, Auto Fix Hit/Slash, custom Hit/Slash style and speed, Preview Swing, Reset to Defaults, persisted clamps, and a single join-hook diagnostic.
- Added a no-game-window Item Animation reachability test and release-JAR proof that both real GUI classes reference the registered HUD/editor model.

## Beta-0.10.7.8v — 2026-10-17

- Repaired Item Animation with an explicit Forge 1.8.9 `ItemRenderer.renderItemInFirstPerson(float)` MCP/SRG/notch transformer target instead of the old caller-shape guess.
- Added balanced direct held-item matrix hooks, map/empty-arm exclusion, anchor-preserving scale, visual swing/equip speed handling, coremod status diagnostics, master toggle, and the HUD-editor inactive warning.
- Expanded the Item Animation editor to the safe X/Y/Z, size, and speed ranges with Shift fine control, double-click per-value reset, exact entries, and persisted default-on state.

## Beta-0.10.7.7v — 2026-10-17

- Repaired and renamed the first-person feature as **Item Animation**.
- Replaced the unreliable renderer-field swap with a Forge 1.8.9 coremod hook around the real first-person hand and equipped-progress paths.
- Added the HUD editor’s non-draggable Item Animation panel with X/Y/Z, Size and Animation Speed sliders, exact-value entry, live held-item preview, reset, bounds and persistence.
- Migrated legacy `firstPerson*` settings to the `itemAnimation.*` config category and added `/velor itemdebug`.
- Kept Item Animation visual-only: no third-person, world, attack-speed or gameplay changes; Haste remains removed.

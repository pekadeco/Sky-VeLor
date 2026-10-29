# Apple settings inventory — Beta-0.10.9.3v

Sky VeLor uses its native Minecraft 1.8.9 Forge GUI. The historical in-project Apple settings shell
informs presentation only: this release ships no browser runtime, React code, third-party GUI framework,
or reference-mod feature/config data.

## Presentation contract

`AppleSettingsScreen` remains the feature-settings entry point for the keybind, `/sv`, and menu.
Its sidebar order is fixed:

**Home → QoL → Slayer → Dungeons → Farming → Mining → Fishing → HUD & Visuals → Misc**.

These are visible presentation labels only: the established `GAMEPLAY` tab identity remains the persistence/routing
identity for the visible **QoL** label, while **Slayer** is a new presentation destination for the existing Slayer
tracker route. Existing feature owners, configuration keys, search behavior, and persistence remain unchanged. The
normal shell uses a compact graphite-glass
window (`72%` safe width / `78%` safe height; maximum `720 × 480` logical GUI pixels), translucent bordered cards,
icon/title/subtitle hierarchy, soft shadow, and an 8 px world margin. Its upper-left header shows the literal
**SkyVeLor** wordmark and contains exactly two 24 px icon targets: **Fullscreen** and **Close**. Dragging, Escape,
hover/press acknowledgement, tooltips, and fullscreen persistence are preserved. A one-time layout-version migration
opens a legacy saved fullscreen shell compact; a later explicit Fullscreen choice still persists. There are no traffic
lights, profiles, READY badges, detail buttons, or option-count expanders.

A visible settings row is one of five patterns:

1. a fixed-column toggle row;
2. one inline control (slider, choice, text, or action);
3. a feature dropdown with two or more flat child options;
4. a visibly distinct named editor-route row; or
5. a key-capture row that shows its current key and may carry a standard enable toggle.

Every normal row reserves the same right-side chevron-then-toggle columns. Toggles are the standard
`38 × 20` pill. **Wardrobe Shortcut**, **Pets Shortcut**, and **Item Lock And Swap Key** place their shared-width
key button immediately left of the shared right-edge toggle lane; **Open Command Keybind Editor** uses that same key
column. **Wardrobe Number Keys** uses the aligned toggle lane. Other named editor routes retain the standard
`132 × 20` action geometry and vertical center. Key-capture rows use a visible key capsule and register a separate
full-row hit so the capsule/toggle geometry is never overwritten. Card headings supply the visual hierarchy without
becoming an interactive disclosure.

Each tab is a one-open accordion. All native bitmap-font text snaps its origin and effective scale to the active
physical GUI pixel grid once per frame, avoiding fractional-scale blur without a custom-font subsystem. Rounded
surfaces use up to 16 quarter-arc segments; even Fast quality uses eight, and new/reset settings select Smooth.
Presentation-only disclosure state is stored under `ui.feature.dropdown.<catalog-id>` by
`FeatureUiState` / `FeatureUiPreferences`; it does not replace,
rename, or migrate a feature configuration key. A newly created top-level settings shell closes every remembered
disclosure, and writes those legacy `true` values back as `false` on the next save, so a normal close/reopen always
starts collapsed. Returning from a child editor reuses its parent screen instance, while the explicit
Chat route intentionally opens Chat QoL after this normal fresh-screen reset. Search indexes labels, ids, descriptions,
and keywords, changes to the result tab, and opens the direct owning dropdown. Expanded panels use a dark bordered
inset surface with an open-header accent, child hover feedback, and inset dividers; they animate their height for
150 ms, have 8 px bottom padding, and retain readable muted labels plus deliberately muted-but-editable controls
when a master is off. The protected panel surface keeps this hierarchy legible even at low GUI-opacity settings.

There is no visible nested dropdown. `ReferenceSettingsCatalog.maxDropdownDepth()` returns one,
`everyDropdownHasAtLeastTwoOptions()` rejects a one-option disclosure, and
`everyChoiceHasAtLeastTwoOptions()` rejects a non-actionable selector. The shared `AppleDropdown` uses a
higher-contrast graphite trigger and popup, divider-separated choices, hover feedback, and a blue selected-row
check mark. Its adaptive placement and editor flow reservation remain intact, so this is visual polish rather
than a routing or binding change. Captions group flat content without becoming a tree.

## Reset and ownership

Every expanded feature has at most one compact **Reset to defaults** text link and that link is the
last expanded option. The standalone **Shortcut** card ends with the same small, left-aligned footer button. Reset
behavior continues to use the established owners; no reset dropdown was added.

- **Hurt Camera Shake** is one inline slider.
- **QoL → Comfort → Smooth Aspect Teleport** is a direct toggle backed by
  `assist.visual.smoothAspectTeleport`. It defaults on; an observed local Aspect use arms the next completed server
  position jump. RenderTick START observes an already-applied plausible jump (at most 64 blocks) while only client
  ticks age the short arm window. An independent render-only origin then eases the camera, terrain, entities, and
  world overlays from the observed pre-jump point to the authoritative destination over 125 ms. It does not initiate
  a teleport, write player/camera entity coordinates, alter input, invoke a controller, or send packets.
- **Spirit Leap Picker** is a master dropdown containing its key option.
- **Terminal Helper** has no master; it shows its live `n / 5 on` count.
- **Boss Health HUD** is a master toggle plus **Open HUD Editor**.
- **Dungeons → Dungeon HUD → Open Dungeon HUD Editor** is a separate centered, solid editor rather than the generic
  HUD window. Its compact picker has **Score / Stats**, **Room Alert**, and **Dungeon Map** targets. It saves per-target
  enable/move/resize/reset state through the existing HUD/map owners, snaps drag placement, and suppresses the live map
  while its map preview is being edited.
- **Dungeons → Dungeon HUD → Dungeon Score/Stats HUD** is a dungeon-only borderless movable HUD. It independently
  toggles Score/rank, Crypts, Deaths, and Run Time lines. The native calculator has no API or packet path: it reads
  visible Fakepixel sidebar, public tab-list, and chat values for floor, cleared rooms, secrets, deaths/Spirit penalty
  reduction, failed puzzles, crypts, runtime, mimic, and Paul. A complete live estimate never renders `S+`; only a
  visible `Extra Stats`/`Score Breakdown` end recap total plus matching rank may do so.
- **Dungeons → Dungeon HUD → Room Clear Alert** is a movable dedicated-editor target. It accepts only narrow normal
  room-clear messages in a confirmed dungeon, deduplicates rooms, rejects blood/fairy/entrance/boss/already-cleared
  text, and offers bounded local duration plus optional local sound.
- **Dungeons → Dungeon HUD → Auto Chest Close** is explicitly off by default. In a confirmed dungeon it is an exact
  title allow-list for normal `Chest` and `Large Chest` containers only; rewards, terminals, and all other menus are
  left open. It never clicks slots, accepts rewards, sends packets, or alters player movement.
- **Dungeons → Run Tools → Dungeon Map** has no border/frame. Its existing `100%` zoom default remains valid and
  saved `50%`–`200%` values keep working; the upper bound is now `500%`. **Player Heads** defaults on and **Player
  Head Size** uses a bounded native-map-pixel slider. In confirmed dungeons only, local/party map markers with ready
  tab-list skins render the 8×8 face plus hat layer in place of their matching native arrows. Their projected centers
  interpolate with render partial ticks, are clamped inside the `128 × 128` map viewport, preserve unrelated map
  decorations, and leave the vanilla arrow visible until a skin is ready. Turning Player Heads off restores arrows.
- **Dungeons → Run Tools → Spirit Bow Locator** is a direct, searchable default-off toggle backed only by
  `assist.dungeon.spiritBowLocator`. In a confirmed dungeon it scans nearby loaded armor stands on a bounded timer and
  draws a local line/outline for the nearest name containing `Spirit Bow`; it never clicks, changes input/position,
  calls a controller, sends chat/custom data/packets, or has a keybind/gameplay command. `/velor selftest` only
  reports whether its Forge render hook registered.
- **HUD & Visuals → HUD → Dungeon Run HUD** is a searchable default-off toggle backed by the established movable HUD
  persistence under `dungeonRun.*`; it is edited with the existing **Open HUD Editor** route. Its Forge tracker samples
  only the already-visible dungeon sidebar at a bounded interval and shows explicitly observed floor, elapsed time,
  cleared percentage, secrets, and deaths. It does not estimate score, read tab-list internals, parse chat, close
  chests, or send data; `/velor selftest` reports its sidebar-reader hook.
- **HUD Editor** uses **Open HUD Editor**.
- **Item Animation** is a master toggle plus **Open Item Animation Editor**.
- **QoL → Shortcut** is a standalone card styled like **Inventory And Chat**. It owns **Wardrobe Shortcut**,
  **Wardrobe Number Keys**, **Pets Shortcut**, **Command Keybind**, and its small left-aligned reset footer. New settings save below
  `assist.shortcut.*`; `.1.8v` `assist.keybindCut.*`, older `assist.movement.*` / `assist.shortcuts.*`, and flat
  aliases still load, with new Shortcut keys taking precedence.
- **Wardrobe Shortcut** and **Pets Shortcut** are enabled key-capture rows: click the whole row or key capsule to
  capture a key, Escape to cancel, and Backspace to persist `NONE`. Fresh installs and explicit Shortcut reset use
  `W` for Wardrobe and `P` for Pets, while an established saved Controls/Shortcut key remains authoritative. Each key
  row reserves a small red warning line for a collision with a Minecraft control or another Sky VeLor keybind. A
  physical Wardrobe down edge sends `/wardrobe` only while enabled, a player exists, and no GUI is open. Pets uses the
  same direct physical edge/no-screen gate for `/pets` while retaining its server guard.
- **Wardrobe Number Keys** defaults on. Only while it and Wardrobe Shortcut are enabled, number keys `1`–`9` are
  intercepted in a recognized title-bearing Wardrobe `GuiChest` with a real `ContainerChest`; each non-repeat input
  makes at most one ordinary `windowClick` on slots `36`–`44`. Turning it off does nothing special with numbers.
  Chat and unrelated GUIs fail closed. This is direct user input, not movement, packet, farming, automatic click, or
  repeat automation.
- **Command Keybind** exposes only **Open Command Keybind Editor**; its visible Shortcut-row master toggle is gone.
  Entries store a physical key and slash command, and a valid pair is active regardless of a retained legacy master
  record. Old master-on/off and per-entry On/Off records load safely, with old per-entry false values treated as
  active. Its solid centered window uses roughly 60% of normal GUI width, a title bar/close button, a fixed Add/Reset
  footer, and a clipped scroll area. Normal screens show key, command field, and full **Delete** button on one readable
  row; narrow Android/Pojav layouts put the command field beneath the key/Delete pair instead of overlapping or
  clipping. Click capture, Escape cancel, Backspace clear, persistence, and red in-row collision warnings remain.
  The field owns Space and ordinary characters while focused; internal and typed outer spaces remain in the draft, and
  only persistence trims its ends. The runtime samples physical key-down edges only; it sends the first
  creation-order command for a duplicated key only when no chat, GUI, or editor is open, and uses a short cooldown.
- **Item Lock And Swap** accepts a new pair only when at least one endpoint is in the hotbar. Its current L (or custom)
  binding is visible in a full-row key capture control: Escape cancels and Backspace persists `NONE`, which disables
  the physical gesture. Inventory↔hotbar and armor↔hotbar routes remain legal; inventory-only and armor-only legacy
  records are safely discarded on load. A slot may keep several legal pairs, each has its own line, L-tapping a paired
  slot removes one saved pair, and successful manual swaps cycle that source endpoint through its targets in creation
  order while retaining the existing cooldown.
- **Dungeons → Starred Mobs** owns only tagged starred targets and named minibosses. Its four retained target styles
  are Starred Mob, Miniboss, Special Starred Mob, and Enderman. A star-tagged Skeletor resolves through normal
  Starred Mob styling; player-shaped/NPC bodies remain eligible when a nearby tag selects them, while the local
  player is excluded by identity in both scan and live render collection.
- **QoL → Comfort → Hide Dead Mobs** is on by default. It owns `assist.comfort.hideDeadMobs`, migrates the former
  `dungeonEsp.hideZeroHealth` choice, and cancels only the client render of a dead/dying normal mob or server-NPC
  fake mob—including its standard nameplate—when `isDead`, `deathTime > 0`, or zero health is observed. It keeps the
  local player and ordinary listed remote players visible; it never removes entities or changes packets/server state.
- The former Other Mobs/Force All Room Mobs fallback, dedicated Skeletor style, tracer path, Starred Mobs
  Default/Subtle profile actions, and session-only Copy/Apply style actions are absent. Historic
  `dungeonEsp.otherMobs.*`, `dungeonEsp.forceRenderAllMobs`, `dungeonEsp.includeBats`,
  `dungeonEsp.includeInvisibleMobs`, `dungeonEsp.skeletor.*`, and `dungeonEsp.tracers` keys load harmlessly and
  are removed on the next normal save.
- Recognized Enderman/Fel armor-stand head markers remain direct targets without a separate star-tag or
  player-relative height gate. Their centered `0.76 × 1.85` fixed proxy covers the complete marker representation
  while remaining smaller than the retired `1.1 × 2.7` outline.
- Map, Item Lock / Swap, inventory, Slayer, M7, Starred Mobs/Door ESP, mining, fishing, and Misc owners retain
  their active option ids and configuration owners. Shortcut writes `assist.shortcut.*` after accepting its Keybind
  Cut and older movement/shortcut predecessor namespaces.
- **Misc → Appearance** exposes **Custom GUI Transparency** plus **GUI Transparency**. These controls retain
  the established `glassOpacityOn` and `glassOpacity` keys: disabling the custom toggle uses the default glass
  opacity without discarding the saved value, and the 45%–100% slider range makes lower opacity visibly useful.

## Item Animation standalone panel

**HUD & Visuals → Item Animation → Open Item Animation Editor** and `/velor itemanimation` open
`ItemAnimationEditorScreen`, not the HUD editor. It is an undimmed left-edge rounded panel at roughly 28% of GUI width, with Close X
and **Reset to Defaults**. Its own panel fill and border are solid dark/opaque, so gameplay pixels cannot bleed through
its controls. Its render plan contains only the Item Animation panel: no fullscreen dim veil, HUD canvas, HUD-editor
bar, world overlay, or held-item sample surface is drawn behind the panel controls.

The panel retains only:

- Hook status; Enable; Scale; compact Position X/Y/Z;
- Animation: Custom Hit/Slash Style, Custom Hit/Slash Speed, Smooth Swing, Smoothness;
- Options: Remove Haste Effect; Loop Preview Swing; Preview Swing; Reset to Defaults.

The status is **Hook: active** with a green semantic indicator when the hook is available, or
**Hook: failed (reason)** otherwise. Detailed transformer diagnostics remain out of the standard
panel and join line. Item Animation background/transparency/opacity controls are absent. Legacy Item Animation
background/opacity keys are ignored on load and removed on the next normal save; other old Item
Animation values continue to load unchanged.

## Command Keybind editor

**QoL → Shortcut → Open Command Keybind Editor** opens `CommandKeybindEditorScreen`. It is a centered, opaque
settings-sized window at roughly 60% of ordinary GUI width, with a readable title bar, explicit Close button, clipped
scroll area, and fixed **Add** / **Reset** footer. It never opens at the top-left corner. On a normal screen each row
has a click-to-capture key, direct slash-command field, and fully visible **Delete** button. On narrow Android/Pojav
launcher coordinates the command field moves below the key/Delete pair rather than overlapping or clipping text.
Escape cancels a pending key capture and Backspace clears that captured key. A small red warning is painted in the
same entry whenever its key collides with another Command Keybind, a Sky VeLor binding, or a Minecraft control.

Entries persist under `assist.shortcut.commandKeybinds` and `assist.shortcut.commandKeybinds.entries`; the previous
`.1.8v` `assist.keybindCut.commandKeybinds` namespace loads once and is removed on the next save. The editor has no
visible master or per-entry On/Off state: a valid slash command plus valid key is executable even if a retained legacy
master record says off. Old saved per-entry false values are deliberately read as active entries, so existing commands
do not silently disappear. Raw field text stays intact while focused—normal characters and spaces are not consumed by
outer GUI handlers—and only persistence trims its ends. The client sends it only from a new physical
key-down edge while `currentScreen` is absent, which excludes chat, all GUIs, and the editor itself. A 300 ms cooldown
and held-key edge tracking prevent automatic repetition. Duplicate command-entry keys dispatch only their first
creation-order entry; the collision warning remains visible so this is never silent.

## Wardrobe behavioral attribution

The Wardrobe interaction was independently implemented after reviewing the behavior of
[im2pb/NotEnoughFakepixel](https://github.com/im2pb/NotEnoughFakepixel), which is LGPL-3.0-or-later. No source,
assets, binaries, or dependency code were copied. The complete attribution and clean-room boundary are in
[`THIRD-PARTY-NOTICES.md`](../THIRD-PARTY-NOTICES.md).

## Player-facing text migration

All catalog labels and descriptions are limited to 60 characters and use Title Case labels with
sentence-case descriptions. Implementation-facing wording is excluded by the catalog copy audit.

| Earlier player-facing text | Beta-0.10.9.3v text |
| --- | --- |
| `No farming features yet` | `No Farming Features Yet` |
| `Item Lock / Swap` | `Item Lock And Swap` |
| `Hurt camera shake` | `Hurt Camera Shake` |
| `Boss health HUD` | `Boss Health HUD` |
| `Menu and Highlight` | `Click Menu` |
| `Custom accent colour` | `Accent Color` |
| `Filter type` | `Filter Type` |
| `Message sound` | `Message Sound` |
| `Open chat background` | `Open Chat Background` |
| `Item Animation Editor` route | `Item Animation` with `Open Item Animation Editor` |
| `Mob ESP` | `Starred Mobs` |
| `Hide Zero Health` under Starred Mobs | `Hide Dead Mobs` under QoL → Comfort; render-only body/nameplate cleanup |
| No Aspect visual easing | `Smooth Aspect Teleport` under QoL → Comfort; render-only |
| Visible `Player` channel | Visible `QoL` label with the same `GAMEPLAY` persistence identity |
| `Movement And Shortcuts` / `Keybind Cut` | standalone `Shortcut`, with migrated `assist.shortcut.*` keys, Wardrobe capture, and Command Keybind editor |
| Slayer route under Player | Slayer Tracker under the visible Slayer channel; keys unchanged |
| Dedicated Skeletor style | Normal `Starred Mob` style for star-tagged Skeletor |
| Tracers; Starred Mobs Default/Subtle actions | Removed; legacy keys clean up on save |
| `Other Mobs`, Force All Room Mobs, bat/invisible controls | Removed; legacy keys clean up on save |
| Copy Style, Apply Copied Style, Apply Style To All | Removed |
| `HOOK ACTIVE — …` | `Hook: active` |
| `HOOK INACTIVE — …` | `Hook: failed (reason)` or `Hook: checking` |

## Channel placement

- **Mining** owns Mining Speed/BPS and Mining Assistant presentation. Existing `HudManager` and
  `AssistSettings` owners remain unchanged.
- **Fishing** owns Catch Counter, Sea Creature Health, Jawbus, and nearby-encounter presentation.
  Existing `AppleSettings`, `FishingStats`, and HUD owners remain unchanged.
- **Farming** deliberately shows only **No Farming Features Yet**. No crop automation, macros,
  auto-clicking, auto-breaking, or movement behavior was introduced.
- **Slayer** owns the established Slayer Tracker route; Terminal/M7/ESP remain Dungeons; HUD and Item Animation
  remain HUD & Visuals. QoL retains the former Player presentation routes while `GAMEPLAY` and every existing config
  key remain backward-compatible.

## Automated coverage and release boundary

Headless coverage includes flat depth/count/choice invariants, nine-destination fixed-column scale-1–4 geometry,
shared action trailing-edge geometry, Apple title-bar targets and 8 px margin behavior, named editor-route/no-tree
behavior, fresh-open accordion reset with the intentional direct-route disclosure, search behavior, player-copy
validation, old-config loading, Shortcut/Keybind Cut migration, Item Lock and Wardrobe capture behavior, Wardrobe
slot-map/screen-gating policy, Command Keybind serialization/collision/direct-edge cooldown and focused-space entry,
Dungeon Map player-head interpolation/bounds and 500% projection coverage, Dungeon HUD editor picker/move/reset routing,
visible-data score calculation/recap-safe `S+` handling, room-clear and normal-chest allow-list policies, exact
configurable-id preservation, direct binding audit, hotbar-only Item Lock pair migration/multi-pair cycling, Spirit Bow
marker matching, bounded Dungeon Run sidebar parsing/HUD persistence, opaque Item Animation and Command Keybind panel
exclusion, dead-mob render policy protections, deterministic render-time Aspect camera-path easing, and fail-closed
EntityRenderer/RenderGlobal transformer-shape checks.
`tools/apple-layout-renderer/` compiles against the real layout/catalog classes and renders exactly
Home, Dungeons, and GUI-scale-4 Home Java2D PNG review previews; CI uploads those previews as
short-lived evidence. `tools/verify_release_jar.py` additionally checks the packaged class and
presentation boundary.

Build with Java 8 Forge and verify the result:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.9.3v.jar \
  --version Beta-0.10.9.3v --mcmod-version 0.10.9.3
```

The checks do not launch Minecraft. Rendering, input feel, placement, and gameplay are
**NOT tested in-game**.

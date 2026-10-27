# Apple settings inventory — Beta-0.10.9.1.6v

Sky VeLor uses its native Minecraft 1.8.9 Forge GUI. The historical in-project Apple settings shell
informs presentation only: this release ships no browser runtime, React code, third-party GUI framework,
or reference-mod feature/config data.

## Presentation contract

`AppleSettingsScreen` remains the feature-settings entry point for the keybind, `/sv`, and menu.
Its sidebar order is fixed:

**Home → Player → Dungeons → Farming → Mining → Fishing → HUD & Visuals → Misc**.

These are visible presentation labels only: the eight existing `Tab` enum identities, routes, feature owners,
configuration keys, search behavior, and persistence remain unchanged. The normal shell uses a compact graphite-glass
window (`72%` safe width / `78%` safe height; maximum `720 × 480` logical GUI pixels), translucent bordered cards,
icon/title/subtitle hierarchy, soft shadow, and an 8 px world margin. Its upper-left header shows the literal
**SkyVeLor** wordmark and contains exactly two 24 px icon targets: **Fullscreen** and **Close**. Dragging, Escape,
hover/press acknowledgement, tooltips, and fullscreen persistence are preserved. A one-time layout-version migration
opens a legacy saved fullscreen shell compact; a later explicit Fullscreen choice still persists. There are no traffic
lights, profiles, READY badges, detail buttons, or option-count expanders.

A visible settings row is one of four patterns:

1. a fixed-column toggle row;
2. one inline control (slider, choice, text, or action);
3. a feature dropdown with two or more flat child options; or
4. a visibly distinct named editor-route row.

Every normal row reserves the same right-side chevron-then-toggle columns. Toggles are the standard
`38 × 20` pill. Editor rows use a descriptive destination CTA—**Open HUD Editor** or
**Open Item Animation Editor**—and never show a chevron. Card headings supply the visual hierarchy
without becoming an interactive disclosure.

Each tab is a one-open accordion. All native bitmap-font text snaps its origin and effective scale to the active
physical GUI pixel grid once per frame, avoiding fractional-scale blur without a custom-font subsystem. Rounded
surfaces use up to 16 quarter-arc segments; even Fast quality uses eight, and new/reset settings select Smooth.
Presentation-only disclosure state is stored under `ui.feature.dropdown.<catalog-id>` by
`FeatureUiState` / `FeatureUiPreferences`; it does not replace,
rename, or migrate a feature configuration key. A newly created top-level settings shell closes every remembered
disclosure, and writes those legacy `true` values back as `false` on the next save, so a normal close/reopen always
starts collapsed. Returning from either existing child editor reuses its parent screen instance, while the explicit
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
last expanded option. Reset behavior continues to use the established owners; no reset row or reset
dropdown was added.

- **Hurt Camera Shake** is one inline slider.
- **Player → Comfort → Smooth Aspect Teleport** is a direct toggle backed by
  `assist.visual.smoothAspectTeleport`. It defaults on; an observed local Aspect use arms the next completed server
  position jump. After vanilla accepts that jump, an independent render-only origin eases the camera, terrain,
  entities, and world overlays from the observed pre-jump point to the authoritative destination over 125 ms. It
  does not initiate a teleport, write player/camera entity coordinates, alter input, invoke a controller, or send
  packets.
- **Spirit Leap Picker** is a master dropdown containing its key option.
- **Terminal Helper** has no master; it shows its live `n / 5 on` count.
- **Boss Health HUD** is a master toggle plus **Open HUD Editor**.
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
- **Dungeons → Starred Mobs** owns only tagged starred targets and named minibosses. Its four retained target styles
  are Starred Mob, Miniboss, Special Starred Mob, and Enderman. A star-tagged Skeletor resolves through normal
  Starred Mob styling; player-shaped/NPC bodies remain eligible when a nearby tag selects them, while the local
  player is excluded by identity in both scan and live render collection.
- **Player → Comfort → Hide Dead Mobs** is on by default. It owns `assist.comfort.hideDeadMobs`, migrates the former
  `dungeonEsp.hideZeroHealth` choice, and hides zero-health or `deathTime > 0` living targets before despawn.
- The former Other Mobs/Force All Room Mobs fallback, dedicated Skeletor style, tracer path, Starred Mobs
  Default/Subtle profile actions, and session-only Copy/Apply style actions are absent. Historic
  `dungeonEsp.otherMobs.*`, `dungeonEsp.forceRenderAllMobs`, `dungeonEsp.includeBats`,
  `dungeonEsp.includeInvisibleMobs`, `dungeonEsp.skeletor.*`, and `dungeonEsp.tracers` keys load harmlessly and
  are removed on the next normal save.
- Recognized Enderman/Fel armor-stand head markers remain direct targets without a separate star-tag or
  player-relative height gate. Their centered `0.76 × 1.85` fixed proxy covers the complete marker representation
  while remaining smaller than the retired `1.1 × 2.7` outline.
- Map, Item Lock / Swap, inventory, movement, Slayer, M7, Starred Mobs/Door ESP, mining, fishing, and Misc
  owners retain their active option ids and configuration owners.
- **Misc → Appearance** exposes **Custom GUI Transparency** plus **GUI Transparency**. These controls retain
  the established `glassOpacityOn` and `glassOpacity` keys: disabling the custom toggle uses the default glass
  opacity without discarding the saved value, and the 45%–100% slider range makes lower opacity visibly useful.

## Item Animation standalone panel

**HUD & Visuals → Item Animation → Open Item Animation Editor** and `/velor itemanimation` open
`ItemAnimationEditorScreen`, not the HUD editor. It is an undimmed left-edge rounded panel at roughly 28% of GUI width, with Close X
and **Reset to Defaults**. Its render plan contains only the Item Animation panel: no HUD canvas,
HUD-editor bar, world overlay, or held-item sample surface is drawn behind it.

The panel retains only:

- Hook status; Enable; Scale; compact Position X/Y/Z;
- Animation: Custom Hit/Slash Style, Custom Hit/Slash Speed, Smooth Swing, Smoothness;
- Options: Remove Haste Effect; Loop Preview Swing; Preview Swing; Reset to Defaults.

The status is **Hook: active** with a green semantic indicator when the hook is available, or
**Hook: failed (reason)** otherwise. Detailed transformer diagnostics remain out of the standard
panel and join line. Item Animation background/transparency/opacity controls are absent. Legacy Item Animation
background/opacity keys are ignored on load and removed on the next normal save; other old Item
Animation values continue to load unchanged.

## Player-facing text migration

All catalog labels and descriptions are limited to 60 characters and use Title Case labels with
sentence-case descriptions. Implementation-facing wording is excluded by the catalog copy audit.

| Earlier player-facing text | Beta-0.10.9.1.6v text |
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
| `Hide Zero Health` under Starred Mobs | `Hide Dead Mobs` under Player → Comfort |
| No Aspect visual easing | `Smooth Aspect Teleport` under Player → Comfort; render-only |
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
- Slayer remains Player; Terminal/M7/ESP remain Dungeons; HUD and Item Animation remain HUD & Visuals.

## Automated coverage and release boundary

Headless coverage includes flat depth/count/choice invariants, fixed-column scale-1–4 geometry,
Apple title-bar targets and 8 px margin behavior, named editor-route/no-tree behavior, fresh-open accordion reset
with the intentional direct-route disclosure, search behavior, player-copy validation, old-config loading, exact
configurable-id preservation,
direct binding audit, Spirit Bow marker matching, bounded Dungeon Run sidebar parsing/HUD persistence, Item Animation
panel render-list/control exclusion, deterministic Aspect camera-path easing, and fail-closed
EntityRenderer/RenderGlobal transformer-shape checks.
`tools/apple-layout-renderer/` compiles against the real layout/catalog classes and renders exactly
Home, Dungeons, and GUI-scale-4 Home Java2D PNG review previews; CI uploads those previews as
short-lived evidence. `tools/verify_release_jar.py` additionally checks the packaged class and
presentation boundary.

Build with Java 8 Forge and verify the result:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.6v.jar \
  --version Beta-0.10.9.1.6v --mcmod-version 0.10.9.1.6
```

The checks do not launch Minecraft. Rendering, input feel, placement, and gameplay are
**NOT tested in-game**.

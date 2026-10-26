# Apple settings inventory — Beta-0.10.9.1.1v

Sky VeLor uses its native Minecraft 1.8.9 Forge GUI. The historical in-project Apple settings shell
informs presentation only: this release ships no browser runtime, React code, third-party GUI framework,
or reference-mod feature/config data.

## Presentation contract

`AppleSettingsScreen` remains the feature-settings entry point for the keybind, `/sv`, and menu.
Its sidebar order is fixed:

**Home → Player → Dungeons → Farming → Mining → Fishing → HUD & Visuals → Misc**.

These are visible presentation labels only: the eight existing `Tab` enum identities, routes, feature owners,
configuration keys, search behavior, and persistence remain unchanged. The shell uses a graphite-glass window,
translucent bordered cards, icon/title/subtitle hierarchy, soft shadow, and an 8 px large-layout world margin.
Its upper-left header shows the literal **SkyVeLor** wordmark and contains exactly two 24 px icon targets:
**Fullscreen** and **Close**. Dragging, Escape, hover/press acknowledgement, tooltips, and fullscreen
persistence are preserved; there are no traffic lights, profiles, presets, READY badges, detail buttons,
or option-count expanders.

A visible settings row is one of four patterns:

1. a fixed-column toggle row;
2. one inline control (slider, choice, text, or action);
3. a feature dropdown with two or more flat child options; or
4. a visibly distinct **Edit** row.

Every normal row reserves the same right-side chevron-then-toggle columns. Toggles are the standard
`38 × 20` pill. Editor rows use a distinct **[Edit]** button and never show a chevron. Card headings
supply the visual hierarchy without becoming an interactive disclosure.

Each tab is a one-open accordion. Presentation-only disclosure state is stored under
`ui.feature.dropdown.<catalog-id>` by `FeatureUiState` / `FeatureUiPreferences`; it does not replace,
rename, or migrate a feature configuration key. Search indexes labels, ids, descriptions, and
keywords, changes to the result tab, and opens the direct owning dropdown. Expanded panels are
clipped lighter inner surfaces, animate their height for 150 ms, have 8 px bottom padding, and retain
dimmed-but-visible dependent options when a master is off.

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
- **Spirit Leap Picker** is a master dropdown containing its key option.
- **Terminal Helper** has no master; it shows its live `n / 5 on` count.
- **Boss Health HUD** is a master toggle plus **Edit**.
- **HUD Editor** is an **Edit** row.
- **Item Animation** is a master toggle plus **Edit**.
- Map, Item Lock / Swap, inventory, movement, Slayer, M7, Mob/Door ESP, mining, fishing, and Misc
  owners retain their existing option ids and configuration owners.
- **Misc → Appearance** exposes **Custom GUI Transparency** plus **GUI Transparency**. These controls retain
  the established `glassOpacityOn` and `glassOpacity` keys: disabling the custom toggle uses the default glass
  opacity without discarding the saved value, and the 45%–100% slider range makes lower opacity visibly useful.

## Item Animation standalone panel

**HUD & Visuals → Item Animation → Edit** and `/velor itemanimation` open `ItemAnimationEditorScreen`, not
the HUD editor. It is an undimmed left-edge rounded panel at roughly 28% of GUI width, with Close X
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

| Earlier player-facing text | Beta-0.10.9.1.1v text |
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
| `Item Animation Editor` route | `Item Animation` with `[Edit]` |
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
Apple title-bar targets and 8 px margin behavior, editor-row/no-tree behavior, accordion persistence,
search disclosure, player-copy validation, old-config loading, exact configurable-id preservation,
direct binding audit, and Item Animation panel render-list/control exclusion.
`tools/apple-layout-renderer/` compiles against the real layout/catalog classes and renders exactly
Home, Dungeons, and GUI-scale-4 Home Java2D PNG review previews; CI uploads those previews as
short-lived evidence. `tools/verify_release_jar.py` additionally checks the packaged class and
presentation boundary.

Build with Java 8 Forge and verify the result:

```sh
python3 tools/verify_release_jar.py \
  build/libs/SkyVeLor-1.8.9-Beta-0.10.9.1.1v.jar \
  --version Beta-0.10.9.1.1v --mcmod-version 0.10.9.1.1
```

The checks do not launch Minecraft. Rendering, input feel, placement, and gameplay are
**NOT tested in-game**.

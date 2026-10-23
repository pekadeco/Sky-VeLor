# Feature Settings Editor reachability — Beta-0.10.8.8v

The Hub now presents short, persisted feature trees. A row has an optional master toggle and one **Open Settings** route; its gray copy says what the feature does. Tree arrows store only `ui.feature.tree.*` presentation state, so feature values and legacy config loading remain unchanged.

## Before / after option reachability

| Tab | Before | After |
|---|---|---|
| General | Appearance and Performance each expanded into 6–7 inline rows | One feature row → **Feature Settings Editor**; Mod Settings and Fonts retain their existing editors |
| Quality of Life | Inventory, Item Protection, Movement, Item Lock & Swap, Chat, and Commands crowded one long tab | One root tree; each detailed feature opens **Feature Settings Editor** (Chat routes to the existing Chat Editor) |
| HUD | Item Animation and every HUD element used multiple tab rows | One Item Animation row → existing Item Animation editor; each HUD element keeps a toggle + HUD Editor route |
| Farming | Mining Assistant exposed four inline rows | Mining BPS routes to HUD Editor; Mining Assistant opens **Feature Settings Editor** and retains its HUD Editor route |
| Dungeon | Spirit Leap, Terminal Assist, Mob ESP, Door ESP, M7 and all ESP style controls occupied the tab | One tree; detailed features open **Feature Settings Editor**. Dungeon Sync is the deliberate two-toggle inline exception |
| Slayer | Tracker, HUD, timer, prices and history clears were separate groups | Tracker/HUD short rows; timer, prices, `/setprice` help and history clears open **Feature Settings Editor** |
| Fishing | Counter, Festival, reset, alerts and HUD routes were separate groups | Catch Counter opens **Feature Settings Editor**; Jawbus/Nearby are deliberate one-toggle exceptions; HUDs retain HUD Editor routes |

## Dungeon ESP click paths

- **Mob ESP:** `Dungeon → Dungeon Features → Dungeon ESP → Mob ESP → Open Settings`.
- Expanding **Mob ESP** reveals **Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor,** and **Other Mobs**. Each child’s button opens that exact category.
- **Door ESP:** `Dungeon → Dungeon Features → Dungeon ESP → Door ESP → Open Settings`; the Wither Doors and Blood Doors children open their matching category.

## Shared rendering and safe preview

The Mob/Door editors use `DungeonEspPreviewRenderer`. It shows a named, real-dimension fallback hitbox/prism rather than constructing a dummy entity or world. Both live rendering and preview call `DungeonEspRenderBatch.drawStyledBox` with the same `DungeonEspSettings.Style`, so mode, color, opacity, outline width and through-walls behavior are not recreated separately.

## State and limits

- Editor selection is remembered under `ui.feature.editor.*`.
- Tree branches are physically zero-height while closed, preventing overlap at all GUI scales.
- The editor has Back, Reset, scrollable left list/tree, scrollable settings pane, reused dropdown palette controls, and optional preview.
- Copy/Apply Style is session-only and introduces no feature config key.

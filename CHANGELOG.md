# Changelog

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

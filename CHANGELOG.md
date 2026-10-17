# Changelog

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

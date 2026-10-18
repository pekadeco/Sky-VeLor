# Changelog

## Beta-0.10.8.5.1v — 2026-10-18

- Fixed Chat Editor section/card placement through the active selected font’s measured metrics, with a collision resolver for small windows, GUI scales, and expanded controls.
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
- Moved global font ownership fully to top-level **Fonts** and added bundled OFL-licensed Rounded (Nunito) and Mono (JetBrains Mono) alongside Smooth and Doodle. Added packaged notices/licenses, preview, safe external-file validation, and fail-closed vanilla fallback for unavailable native-font bridges.
- Updated release metadata and verification for display version `Beta-0.10.8.5v` with numeric Forge `mcmod.info` version `0.10.8.5`, and strengthened the release-JAR audit for the shared layout, chat pipeline, global settings, and bundled font resources.

## Beta-0.10.8.4v — 2026-10-18

- Unified Default Chat, Chat 1, and Chat 2 message hit testing, eased hover state, right-click handling, and bounded scrolling through one native interaction model. Default vanilla-layout chat now receives the same opt-in highlight/menu path when Separate Chats is off.
- Replaced the copy menu with exactly **Copy to clipboard** (clean text) and **Copy to chat box** (inserts clean text). The menu is clamped, eased, dismisses on outside click/Escape, and right-click never fires a message component action.
- Restored native component behavior in Chat 1/Chat 2: wrapped/moved/scaled rows resolve the clicked component and forward left clicks through vanilla-compatible `run_command`, `suggest_command`, and `open_url` handling; native hover events show a safe tooltip.
- Added independent Default Chat / Chat 1 / Chat 2 appearance and scrolling profiles, Link all, Copy settings from, per-target resets, and a real customized Default Chat draw pass that keeps its cached vanilla wrapped lines and vanilla-compatible scroll/component lookup.
- Added draggable, screen-clamped, GUI-scale-persisted Chat Editor panel state with title double-click reset and persisted collapse state; the editor now exposes target selection, per-target colors/scale/background/border/spacing/shadow/appearance/fade/hover/scroll controls.
- Replaced the chat-only font preference with global Sky VeLor font settings and an **Open Font Settings** route. Added Minecraft Default, bundled OFL-licensed Smooth (Abel) and Doodle (Architects Daughter) fonts, external config-folder `.ttf`/`.otf` discovery, cached bilinear glyph atlases, safe fallback, per-area scales, and an optional fail-closed all-Minecraft-text switch.
- Added headless checks for all three interaction surfaces, exactly two menu labels, moved/scaled hit coordinates, one wheel application, draggable/clamped/resettable panel state, independent/linked/copied target settings, target persistence, and font migration/fallback.

## Beta-0.10.8.3v — 2026-10-17

- Replaced the previous separate chat pages with one compact **QoL → Chat QoL** dropdown: a safe master switch, **Chat Tabs**, **Separate Chats**, **Smooth Messages**, **Hover Highlight**, **Right Click Menu**, **Custom Font**, and **Open Chat Editor**. All customization now belongs only to the editor.
- Added a native Apple-style **Chat Editor** (also `/velor chateditor` and an unbound keybind) with clipped, non-overlapping **Windows**, **Background**, **Messages**, **Font**, **Tabs**, and **Hover / menu** sections, per-section resets, per-GUI-scale draggable/resizable snapped windows, editable names/limits/fade, pattern testing, font fallback, and tab visibility/order controls.
- Reworked chat tabs to attached **All / Party / Reply** controls with unread markers, safe All fallback, typed-text-preserving switches, Ctrl+Tab plus a configurable cycle key, and `/reply` routing for Reply. Removed unsupported channel controls.
- Added optional native split windows: Chat 1 receives player/party/reply traffic; Chat 2 receives system and unknown lines. Classification has bounded editable rules, debug evidence through `/velor chatdebug`, and unknown traffic fails safely to Chat 2 and All.
- Added native separate-window hover highlighting and a safe right-click text menu for clean copy, copy-to-input, and optional formatted copy. Link/click/hover component actions are never replayed by the menu.
- Retired the old external Chatting animation configuration path. Legacy chat settings migrate to `chat.qol.*`; optional external-runtime detection remains silent and fail-closed, while existing packaged attribution notices remain intact.
- Added headless tests for Chat QoL ownership, migration/clamps, classification/routing, copy text, font fallback, attached-tab non-overlay geometry, editor layout, and per-scale window bounds.

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

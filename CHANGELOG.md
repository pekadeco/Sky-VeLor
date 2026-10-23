# Changelog

## Beta-0.10.8.8v — 2026-10-23

- Reworked settings presentation only: every crowded Hub tab now uses compact, persisted feature-tree rows with an optional master toggle, gray description, and one **Open Settings** route. Existing feature behavior, config keys/values, migration and old-config loading are unchanged.
- Added the reusable **Feature Settings Editor** framework with Back/Reset, remembered category selection, scrollable left navigation/tree and settings pane, shared responsive geometry, and the existing dropdown palette style.
- Moved Mob ESP and Door ESP into focused editors. Mob ESP lists Global plus Starred Mob, Miniboss, Special Starred Mob, Enderman, Skeletor and Other Mobs; Door ESP covers global filters plus Wither/Blood doors. Both retain all prior options, Default/Subtle/Reset and session-only copy/apply style actions.
- Added an in-editor safe named hitbox/prism preview. Preview and live Dungeon ESP both emit style geometry through `DungeonEspRenderBatch.drawStyledBox`; no dummy world/entity is constructed.
- Routed detailed Appearance, Performance, assist, Item Lock & Swap, Spirit Leap, Terminal, M7, Catch Counter and Slayer controls through the framework. Existing HUD Editor, Item Animation editor, Chat Editor, Fonts and Mod Settings remain their focused owners.
- Added headless persisted-tree, connector/indentation, reachability and Dungeon ESP control-route coverage; updated release-jar audit checks for the compact editor architecture.
- Updated display version to `Beta-0.10.8.8v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.8`.
- **NOT tested in-game.**

## Beta-0.10.8.7v — 2026-10-23

- Added a clean-room assist foundation with namespaced, bounds-checked, old-key-compatible settings and a one-place Hub catalog. New game-affecting assists default to off; new keybinds are unbound and registered under **Sky VeLor**.
- Added inventory search/filter dimming, potion-panel hiding, Roman tooltip rendering, long-tooltip scrolling, rarity tints, UUID-tagged item protection with persistent allow-list, local no-hurt camera, opt-in Auto Sprint, and Fakepixel-gated Wardrobe/Pets shortcuts.
- Added Mining Assistant and Boss HP HUD elements, pickaxe-ready alerts, a read-only Dwarven Mines Puzzler marker, protected boss-health parsing, and HUD-editor registration without moving existing HUD positions.
- Added guarded F7/M7 terminal visual helpers, known-wrong dim/block behavior, optional constrained auto-click, and local terminal/device waypoints. Unknown, malformed, duplicate, or stale containers deliberately do nothing.
- Added opt-in Dungeon Sync: a local SHA-256 session derivation, WSS-only Java 8 transport with verified handshake/client masking/bounded frames, allow-listed inbound puzzle data, bounded map-memory integration, reconnect limits, debug-safe status, and the privacy disclosure in `docs/DUNGEON_SYNC.md`.
- Added clean-room visual hook support for inventory tooltip filtering/rarity rendering and local hurt-camera suppression, with transformer status in `/velor selftest`; existing Item Animation, Chat QoL, Fonts, settings, item lock/swap, Dungeon ESP/doors, HUD editor, sea-creature counters, and CorruptedHP behavior remain in place.
- Added headless policy/config/parser/identity/reconnect/transformer tests for the new code. Runtime behavior still needs a real Minecraft 1.8.9 Forge pass.
- The friend archive was compared only against its declared `Beta-0.10.6v` base. No archive is retained in the release tip; NotEnoughFakepixel/Better-NEF notice-derived solvers were independently recreated only where safely supportable, with no LGPL source copied.
- Updated display version to `Beta-0.10.8.7v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.7`.

## Beta-0.10.8.6.4.1v — 2026-10-21

- Consolidated every Dungeon ESP control into the **Mob ESP**, **Door ESP**, **Performance**, and **Presets** collapsible groups on the Dungeon page. Every control has a gray one-line explanation; responsive shared row geometry reserves distinct label/control columns at every GUI scale.
- Fixed starred **Crypt Souleater**, split armor-stand tags, tall-body association, star variants, real Endermen, and the Dungeon Enderman-head marker. Matching consumes the nearest eligible body once, tracks the real body hitbox, and uses a readable fixed box for head-marker armor stands. Added `/velor espdump` nearby-entity/chat-log diagnostics.
- Added independent **Skeletor** and **Other Mobs** styles, plus off-by-default **Force Render All Mobs in Room** cached coverage. It includes only eligible hostiles by default, keeps bats/invisible mobs opt-in, excludes NPCs/pets/dead/passive entities, and does not duplicate tagged targets.
- Changed new wither-door boxes to red and blood-door boxes to dark red. Existing stored former defaults migrate once; non-default custom colors remain unchanged.
- Added headless coverage for split/tall tags, Crypt Souleater, real/head Endermen, Skeletor, force inclusion/exclusion/no-duplicates, color migration/config persistence, and small/large GUI geometry.
- Enderman-head behavior was implemented clean-room from runtime compatibility data; no NotEnoughFakepixel source or LGPL code was copied, so no NEF notice is included.
- Updated display version to `Beta-0.10.8.6.4.1v`; generated Forge `mcmod.info` and build metadata resolve numeric version `0.10.8.6.4.1`.

## Beta-0.10.8.6.4v — 2026-10-21

- Replaced the old per-frame **Mob highlights** renderer with one cached Dungeon ESP: it recognizes yellow-star (`✮`/`✯`) armor-stand tags, associates the loaded living body below each tag on configurable client ticks (default 5), draws the body’s interpolated real hitbox, and removes dead, zero-health, despawned, or unpaired targets. Plain `[Lv]` tags are no longer highlighted.
- Added independently implemented starred, miniboss, and special-starred categories with color-palette selection, outline/filled/both mode, per-category through-wall behavior, edge width, outline/fill opacity, global range, tracer, distance text, and max-box controls. The Dungeon tab now has shared-layout collapsible **Dungeon ESP**, category, Global, Doors, and Presets groups with Default/Subtle/reset profiles.
- Added cached closed wither/blood-door ESP for the observed 3×4×3 coal/red-stained-clay prism behavior. Door discovery is timer-driven, cache entries are invalidated when opened, and UI controls cover nearest/all, key-only, blood visibility/color, rendering, and distance text.
- Added `/velor espdebug` live dungeon state, tracked/rendered counts, scan/render timings, scan interval, door-cache size, and bounded-cache drops.
- Added mock-500 entity storage-reuse/allocation regression coverage, tag/body delay/death/classification tests, sample door-layout/invalidation tests, persisted Dungeon ESP migration/bounds/preset tests, and shared GUI-scale overlap coverage.
- Source research was clean-room only: NotEnoughFakepixel is LGPL-3.0 while Sky VeLor is MIT, so no NEF source or notices were copied. The superseded `MobHighlight`/`MobMarkers` implementation was removed.
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
- Reduced Chat Editor to **Background**, **Messages**, **Tabs**, **Menu and Highlight**, and **Reset** with active-font-measured, clipped non-overlapping flow. `/velor chatdebug` now reports the one renderer’s live tab/group/menu state; `/velor layoutdebug` covers its shared layout.
- Added headless coverage for custom filtering/invalid regex/All membership, retired-key omission, captured menu payloads, grouped highlight bounds, tab geometry, and editor flow at compact scales.
- Updated display version to `Beta-0.10.8.5.2v` and numeric Forge `mcmod.info` version to `0.10.8.5.2`.

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

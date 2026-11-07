# Changelog

## Beta-0.10.9.1.12v — partial Phase 2 Dungeon HUD update

- Moved the single Dungeon Map row into the Dungeon HUD category; its controls remain in the Dungeon HUD Editor and saved map settings retain their existing keys.
- Increased Room Clear Alert size to 800%, with viewport fitting and selection geometry, and enabled it by default. Old settings are migrated on once; later explicit off choices are preserved.
- The requested Boss Health Bar upgrade is **not included**. The existing Boss HP text HUD and F7/M7 name/health tracker remain unchanged; the requested rendering, controls, vanilla-bar hiding, and performance work remain outstanding.
- Automated build and tests do not launch Minecraft. Map, editor, large text, autosave, migration and the retained boss tracker have not been tested in-game.

## Beta-0.10.9.1.11v — Phase 1B dungeon cleanup

- Removed both dungeon rating calculators, rank and notification logic, party messages, score/Stats HUD, settings, editor target, old configuration outputs, score-only tests and score diagnostics.
- Room Clear Alert now reads the same raw sidebar percentage from a neutral parser; its chat, baseline and de-duplication policy remain. The map and chest-close policy retain their existing independent context and parsing paths.
- Dungeon Secrets HUD now displays observed secrets or percentage without rating-derived targets. `/luna dungeon debug` and `/svdungeon debug` retain concise local detector and clear-percentage diagnostics.
- Legacy rating settings are ignored on load and omitted on the next save. Attribution and licenses for retained NotEnoughFakepixel-origin components remain.
- Build, standalone tests and artifact verifiers cannot replace in-game Forge/Fakepixel/Pojav checks; this release has not been tested in-game.

## Beta-0.10.9.1.10v — partial Phase 1 cleanup

- Replaced the old white-box-prone logo assets with a pre-baked pixel crescent and sparkles for the Home and header artwork.
- Removed the Dungeon Run status HUD and both Dungeon/Party Sync implementations and settings; local dungeon progress and map support remain.
- Removed the manual Save Settings action and added autosave on changes and editor closure.
- Default Wardrobe to H and migrate the old W default once, while retaining other saved bindings; Pets stays P.
- Dungeon Score removal was not included in this historical release; see the newer release above. The unified-editor and earlier visual redesign requests remain incomplete.
- Automated Java 8 Forge build, standalone tests, and artifact verifiers do not replace an in-game Forge/Fakepixel/Pojav check; appearance, shortcuts, autosave, map, alerts, and other retained functionality have not been tested in-game.

## Beta-0.10.9.1.9v — partial settings visual update

- Centralized the requested dark/orange reference colors in the shared theme, preserving the existing opacity, radius and motion preferences.
- Updated the settings header artwork and selected navigation color; added a scroll-end Save Settings action with write-failure feedback while retaining autosave.
- Corrected the source sampling of the existing Lunaria logo artwork. Cached unchanged search text and search matches, and avoided repeated disclosure-state boxing when settled.
- This is not the complete requested redesign: section tabs, fixed save footer, About rail entry, full glossy/glow component treatment, new font, small-screen/Pojav layout fixes, and the requested pre-baked/culled render-performance work are not included. GUI input, appearance and performance have not been tested in-game.

## Beta-0.10.9.1.8v — The Sky's Lunaria

- Renamed and reordered settings channels; moved Dungeon Run HUD to Dungeons without changing its saved settings.
- Added a menu key picker and recovery command; `/luna` opens settings, with legacy aliases retained.
- Window-size options and a unified Dungeon HUD Editor are not included in this release.

## Beta-0.10.9.1.7v — The Sky's Lunaria

- Align player-facing title and release artifact names with Lunaria.
- Keep the existing command aliases and legacy configuration compatibility.
- The requested editor improvements are not yet included in this stage.

## Beta-0.10.9.4v — The Sky's Lunaria Mod

### Added

- Lunaria public metadata, `lunaria` resource namespace, centered logo/moon artwork, and canonical `/lunaria` command route.
- Added dungeon diagnostics and compatibility routes.
- Supplied aspect-fill loading artwork, soft blue-to-purple progress treatment, startup status/percent copy, and a post-draw downloading-terrain overlay.
- Current-first configuration migration, including current Lunaria HUD, notes, fishing, sync, Slot Lock, external-font, and main-setting write targets with read-only historical fallbacks.
- Friend-source capabilities not present in the earlier Arena surface: detailed Chat Editor, Chat QoL models, optional bundled/external fonts, dungeon progress/readout support, terminal/puzzle helpers, safety policies, and their retained tests.
- Current-first external-font discovery for `config/lunaria/fonts`, with legacy files left in place and exact-name current precedence.
- Misc → Fonts catalog controls and a named Chat Editor route, so retained font and chat-editor capabilities are reachable from Lunaria settings.
- Regression coverage for Lunaria migration, Slot Lock precedence, font discovery, retained Chat Editor layout, font catalog bindings, loading artwork contract, and merged-release claims.

### Changed

- Merged the friend ZIP feature set with the latest Arena feature set using current implementations where ownership overlaps.
- Retained Shortcut keys; Item Lock & Swap; dungeon map/player heads; direct-canvas Dungeon HUD Editor; Room Clear Alert; Auto Chest Close; Hide Dead Mobs; Smooth Aspect Teleport; Item Animation; Chat QoL; and existing HUD, fishing, Slayer, mining, ESP, and sync surfaces.
- Rebranded player-facing GUI labels, chat/log prefixes, resource packaging, artifact names, CI, source/JAR verifiers, documentation, and notices for Lunaria.
- Switched release delivery names to `Lunaria-Mod-1.8.9-Beta-0.10.9.4v` and removed automatic GitHub Release creation from CI. Verified branch artifacts remain the three-file `dist/` delivery surface.
- Expanded the standalone test runners to compile and run every retained test class rather than maintaining a partial handwritten list.
- Corrected loading-art source cropping so aspect-fill uses the calculated cropped source dimensions instead of stretching the entire image from a cropped origin.

### Compatibility

- New files are written only under Lunaria names. Existing prior configuration files and font folders are read as fallbacks when no current target exists; they are never renamed, copied, or deleted at startup.
- Existing internal Java package names and selected network/session literals remain where changing them could break binary, server, or saved-data compatibility.

### Validation status

- This release requires Java 8 Forge CI verification. Automated checks cannot replace a real Minecraft 1.8.9 Forge/Fakepixel/Pojav visual and input pass.

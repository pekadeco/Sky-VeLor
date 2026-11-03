# Changelog

## Beta-0.10.9.4v — The Sky's Lunaria Mod

### Added

- Lunaria public metadata, `lunaria` resource namespace, centered logo/moon artwork, and canonical `/lunaria` command route.
- `/lunaria dungeon debug` while preserving `/sv`, `/velor`, and `/svdungeon debug` compatibility routes.
- Supplied aspect-fill loading artwork, soft blue-to-purple progress treatment, startup status/percent copy, and a post-draw downloading-terrain overlay.
- Current-first configuration migration, including current Lunaria HUD, notes, fishing, sync, Slot Lock, external-font, and main-setting write targets with read-only historical fallbacks.
- Friend-source capabilities not present in the earlier Arena surface: detailed Chat Editor, Chat QoL models, optional bundled/external fonts, dungeon progress/readout support, terminal/puzzle helpers, safety policies, and their retained tests.
- Current-first external-font discovery for `config/lunaria/fonts`, with legacy files left in place and exact-name current precedence.
- Misc → Fonts catalog controls and a named Chat Editor route, so retained font and chat-editor capabilities are reachable from Lunaria settings.
- Regression coverage for Lunaria migration, Slot Lock precedence, font discovery, retained Chat Editor layout, font catalog bindings, loading artwork contract, and merged-release claims.

### Changed

- Merged the friend ZIP feature set with the latest Arena feature set using current implementations where ownership overlaps.
- Retained Shortcut keys; Item Lock & Swap; dungeon map/player heads; direct-canvas Dungeon HUD Editor; Dungeon Score calculator/debug path; Room Clear Alert; Auto Chest Close; Hide Dead Mobs; Smooth Aspect Teleport; Item Animation; Chat QoL; and existing HUD, fishing, Slayer, mining, ESP, and sync surfaces.
- Rebranded player-facing GUI labels, chat/log prefixes, resource packaging, artifact names, CI, source/JAR verifiers, documentation, and notices for Lunaria.
- Switched release delivery names to `Lunaria-Mod-1.8.9-Beta-0.10.9.4v` and removed automatic GitHub Release creation from CI. Verified branch artifacts remain the three-file `dist/` delivery surface.
- Expanded the standalone test runners to compile and run every retained test class rather than maintaining a partial handwritten list.
- Corrected loading-art source cropping so aspect-fill uses the calculated cropped source dimensions instead of stretching the entire image from a cropped origin.

### Compatibility

- New files are written only under Lunaria names. Existing prior configuration files and font folders are read as fallbacks when no current target exists; they are never renamed, copied, or deleted at startup.
- Existing internal Java package names and selected network/session literals remain where changing them could break binary, server, or saved-data compatibility.

### Validation status

- This release requires Java 8 Forge CI verification. Automated checks cannot replace a real Minecraft 1.8.9 Forge/Fakepixel/Pojav visual and input pass.

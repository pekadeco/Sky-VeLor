# The Sky's Lunaria Mod — Beta-0.10.9.4v

**The Sky's Lunaria Mod** is a client-side Forge mod for Minecraft 1.8.9. It combines the retained Lunaria feature set with the reliable Arena implementation: configurable HUDs, dungeon tools, map player heads, safe inventory rules, shortcut keys, Chat QoL, Item Animation, fishing, Slayer, mining, and privacy-conscious party sync.

## Highlights

- **Settings and HUD:** native Lunaria settings shell, Home performance cards, direct-canvas Dungeon HUD Editor, normal HUD Editor, movable Dungeon Score/Stats, Room Clear Alert, Dungeon Run, secrets, boss, fishing, mining, and inventory surfaces.
- **Dungeon tools:** map and interpolated player heads, terminal hints, puzzle/terminal support, Spirit Leap helper, score calculator and diagnostics, Room Clear Alert, exact-title Auto Chest Close, Spirit Bow locator, Floor VII support, and client-side ESP.
- **Safe gameplay QoL:** Shortcut keys, Item Lock & Swap rules, protected inventory gestures, Auto Sprint, Hide Dead Mobs, Smooth Aspect Teleport, quiet block particles, and held-item animation. Client visual helpers do not send movement or combat packets.
- **Chat and fonts:** detailed Chat Editor, chat tabs, history, compact messages, click menu, optional bundled/external Lunaria fonts, and a clear native-font fallback.
- **Compatibility:** canonical `/lunaria` commands plus `/sv` and `/velor` aliases; current settings are written under Lunaria names while prior files remain read-only fallbacks.

## Requirements

- Minecraft **1.8.9** with Forge **11.15.1.2318**.
- Java 8 for the Forge runtime and JDK 8 for builds.
- Internet access for the first ForgeGradle dependency resolution.

ForgeGradle 2.1 and Gradle 2.14.1 are intentionally retained for this legacy Minecraft target.

## Commands

| Command | Purpose |
| --- | --- |
| `/lunaria` | Open Lunaria settings. |
| `/lunaria dungeon debug` | Print local dungeon-score diagnostics for support. |
| `/lunaria itemanimation` | Open the Item Animation editor. |
| `/lunaria chateditor` | Open the retained detailed Chat Editor. |
| `/lunaria selftest` | Report local hook status. |
| `/sv`, `/velor` | Compatibility aliases for `/lunaria`. |
| `/svdungeon debug` | Compatibility direct alias for dungeon diagnostics. |

All commands are client-side. They do not send slash commands to a server.

## Settings routes

- **QoL → Inventory And Chat:** Item Lock And Swap, Inventory Assist, and Chat QoL.
- **QoL → Shortcut:** Wardrobe, Pets, and custom slash-command keybinds.
- **Dungeons:** map/player-head controls, terminals, score, Room Clear Alert, Auto Chest Close, Floor VII, and ESP.
- **HUD & Visuals:** direct HUD editors and Item Animation.
- **Misc → Fonts:** optional font enable, discovery/selection, scale, reload, and the detailed Chat Editor. Place new `.ttf` or `.otf` files in `config/lunaria/fonts`.

Read [`docs/FEATURE-SETTINGS-EDITOR.md`](docs/FEATURE-SETTINGS-EDITOR.md) for behavior and safety boundaries, [`docs/CONFIGURATION-MIGRATION.md`](docs/CONFIGURATION-MIGRATION.md) for migration details, and [`docs/LOADING-SCREEN.md`](docs/LOADING-SCREEN.md) for loading-screen behavior.

## Build and test

### Windows

```bat
gradlew.bat clean build dumpTestClasspath
set /p TEST_CP=<build\test-classpath.txt
tests\run-tests.bat
```

### Linux / Git Bash

```sh
./gradlew clean build dumpTestClasspath
TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
```

The standalone runner compiles every test and runs each test class with a `main` method. It does not start Minecraft.

Expected release files:

```text
Lunaria-Mod-1.8.9-Beta-0.10.9.4v.jar
Lunaria-Mod-1.8.9-Beta-0.10.9.4v-source.zip
Lunaria-Mod-1.8.9-Beta-0.10.9.4v.zip
```

Verify a built JAR with:

```sh
python3 tools/verify_release_jar.py \
  build/libs/Lunaria-Mod-1.8.9-Beta-0.10.9.4v.jar \
  --version Beta-0.10.9.4v --mcmod-version 0.10.9.4
```

CI builds, tests, verifies the JAR/source archive, and publishes the three verified files to `dist/` only when a branch commit explicitly includes `[publish-artifacts]`. It does **not** create a GitHub Release.

## Installation

1. Install Forge `11.15.1.2318` for Minecraft 1.8.9.
2. Put `Lunaria-Mod-1.8.9-Beta-0.10.9.4v.jar` in the Minecraft `mods` folder.
3. Launch the Forge profile and open `/lunaria`.

## Repository note

This codebase keeps its existing GitHub repository identity by design. Renaming the repository is a manual maintainer follow-up: use GitHub **Settings → General → Repository name**, then update clones, remote URLs, links, badges, and any deployment/webhook integrations. No automated build step renames the repository.

## Validation boundary

The Java 8 build, standalone tests, archive verifiers, and source inspection validate code and packaged resources. They do not perform a live Forge/Fakepixel/Pojav playthrough. Test real in-game input, rendering, server presentation variants, and third-party endpoint availability before making live-server guarantees.

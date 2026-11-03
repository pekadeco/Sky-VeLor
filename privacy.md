# Privacy

The Sky's Lunaria Mod is primarily a local client-side mod.

## Local data

Settings, HUD placement, notes, counters, Item Lock & Swap data, and optional font selections are stored in the Minecraft configuration area. New Lunaria files are written under Lunaria names. Earlier files are read only as compatibility fallbacks and are not renamed, copied, or deleted automatically.

External font discovery reads file names from `config/lunaria/fonts` and, when needed, the prior fallback folder. It does not upload those files.

## Optional Dungeon Sync

Dungeon Sync is disabled by default. If enabled, it can connect to the configured secure WebSocket endpoint to exchange allow-listed puzzle/room state with a dungeon party. See [`docs/DUNGEON_SYNC.md`](docs/DUNGEON_SYNC.md) for the exact data boundary and transport behavior.

No feature should be understood as anonymity, security, or server-rule compliance advice. Normal network transport metadata can be visible to a service you explicitly connect to.

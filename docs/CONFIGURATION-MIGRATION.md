# Configuration migration

Lunaria migration is deliberately non-destructive.

## Rule

For each migrated store, Lunaria prefers its current target. If no current target exists, it may read the prior file as a fallback. It never renames, copies, or deletes the fallback at startup. A later normal save writes only the current Lunaria target.

## Main files

| Data | Current write target | Fallbacks when current target is absent |
| --- | --- | --- |
| Main settings | `.minecraft/lunaria.properties` | `.minecraft/skyvelor.properties`, then `.minecraft/pekaclient.properties` |
| HUD layout | `.minecraft/lunaria-hud.properties` | prior HUD names, then earliest legacy HUD name |
| Notes | Lunaria notes file | prior notes file |
| Fishing counters | Lunaria fishing properties | prior fishing properties |
| Dungeon Sync | Lunaria sync properties | prior sync properties |
| Slot Lock data | `config/lunaria/slot-locks.properties` | `config/skyvelor/slot-locks.properties` |
| External fonts | `config/lunaria/fonts` | `config/skyvelor/fonts` |

The first existing current file is authoritative, including a recovered current temporary write. Historical Slot Lock and font data are never overwritten by mere discovery or loading.

## Settings ownership

Current modular owners load before bounded friend-only compatibility settings. Where a setting overlaps, the current owner wins. Friend-only settings are retained and written in the current main settings file. Old dotted first-person Item Animation values are accepted only when modern Item Animation keys are absent; saving normalizes to the modern schema.

Font settings are stored under `fonts.*`. Earlier chat-font keys are accepted for migration, then the normalized font state is saved under `fonts.*`.

## What to do as a player

1. Keep existing configuration files in place.
2. Launch Lunaria normally.
3. Confirm settings/HUD placement once in `/lunaria`.
4. Let a normal settings save create the current Lunaria file.
5. Keep old files as a backup until you have verified the migrated result.

Manual file moves are unnecessary and can make it harder to recover an older layout.

# Privacy

The Sky's Lunaria Mod is primarily a local client-side mod.

## Local data

Settings, HUD placement, notes, counters, Item Lock & Swap data, and optional font selections are stored in the Minecraft configuration area. New Lunaria files are written under Lunaria names. Earlier files are read only as compatibility fallbacks and are not renamed, copied, or deleted automatically.

External font discovery reads file names from `config/lunaria/fonts` and, when needed, the prior fallback folder. It does not upload those files.

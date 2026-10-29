# Third-party notices

## NotEnoughFakepixel Wardrobe shortcut behavioural reference

Sky VeLor’s opt-in Wardrobe Shortcut was independently implemented after reviewing the
Wardrobe shortcut behaviour in [im2pb/NotEnoughFakepixel](https://github.com/im2pb/NotEnoughFakepixel),
commit `f80cddf6ac1e752bd402d1d963f3f987253b4795` (reviewed 2026-10-28).

NotEnoughFakepixel is licensed under **LGPL-3.0-or-later**. No NotEnoughFakepixel source,
assets, binaries, or dependency code are included in Sky VeLor. The Sky VeLor implementation
uses its own small policy/controller structure and is limited to a player’s physical key edge:
it can send `/wardrobe` from normal gameplay or invoke one ordinary click in an already-open,
recognized Wardrobe menu. It does not automate movement, packets, farming, clicks, or inventory
actions.

## NotEnoughFakepixel dungeon score behavioural reference

Sky VeLor’s Dungeon Score Calculator was independently implemented after reviewing the
dungeon-score behaviour in [im2pb/NotEnoughFakepixel](https://github.com/im2pb/NotEnoughFakepixel),
commit `f80cddf6ac1e752bd402d1d963f3f987253b4795` (reviewed 2026-10-29).

NotEnoughFakepixel is licensed under **LGPL-3.0-or-later**. No NotEnoughFakepixel source,
assets, binaries, or dependency code are included in Sky VeLor. Sky VeLor uses its own
Minecraft-free parser/calculator and observes only already-visible Fakepixel data: Catacombs/floor
and `Dungeon Cleared` sidebar rows; public tab-list `Dungeon:`, `Secrets Found:`, `Time:`,
`Deaths:`, and `Crypts:` rows; and dungeon chat puzzle/Spirit Pet/Blood Door/final-recap events.
It independently implements the observed score behaviour, including the visible Blood Door clear
adjustment and capped crypt/Paul bonus only. It performs no API requests and does not adopt
NotEnoughFakepixel's source, assets, binaries, or dependencies.

# Dungeon Sync privacy and safety

Dungeon Sync is disabled by default. It starts only after you enable **Dungeons → Dungeon Sync** and Sky VeLor can locally validate a Catacombs scoreboard, a temporary server-instance identifier, and a 2–5 player dungeon roster.

## What leaves the client

- A SHA-256 session value derived locally from the server instance, floor, and party profile names.
- A random connection identifier for that launch/session.
- One allow-listed puzzle label, its 32-block-aligned room coordinates, and confidence when the local dungeon map recognises a room.

Profile names are inputs to the local hash only; they are not put in the WebSocket URL body, JSON messages, debug output, or chat notices. The SHA-256 value is a session-grouping identifier, not a secret: a service that already knows the exact server instance, floor, and roster could reproduce it. The remote service can still receive normal transport metadata, including your IP address, as any HTTPS/WebSocket service can.

## Transport and limits

The client accepts only `wss://` endpoints ending in `/ws`, verifies the TLS host and WebSocket handshake, masks client frames, bounds frames/queues, and backs off reconnects from 1.5 to 30 seconds. Incoming data is allow-listed and bounds-checked before the map can display it. Turning the toggle off, leaving the dungeon, or changing worlds closes the connection and clears pending data.

Dungeon Sync is an optional third-party network feature. It has not been tested in-game for this release.

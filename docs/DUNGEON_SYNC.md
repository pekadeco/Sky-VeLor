# Dungeon Sync privacy and safety

Dungeon Sync is disabled by default. It starts only after a player enables it and the client can locally validate dungeon context, a temporary server-instance identifier, and a small dungeon roster.

## What can leave the client

- A locally derived session grouping value based on the instance, floor, and party profile names.
- A random connection identifier for the launch/session.
- Allow-listed puzzle labels, bounded room coordinates, and confidence when the local map recognizes a room.

Profile names are inputs to the local session derivation; they are not intended to be sent as ordinary payload fields. The session grouping value is not a secret: a service that already knows all inputs could reproduce it. A service can still receive ordinary network transport metadata, including IP address, when a player explicitly connects.

## Transport and limits

The client accepts secure WebSocket endpoints, validates endpoint shape and incoming data, bounds frames/queues, and backs off reconnects. Turning the setting off, leaving the dungeon, or changing worlds closes the connection and clears pending state.

Dungeon Sync is an optional third-party network feature. It requires real-server verification before any production availability claim.

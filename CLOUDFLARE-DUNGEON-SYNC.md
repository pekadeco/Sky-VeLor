# Dungeon Sync endpoint deployment notes

Dungeon Sync is an optional, disabled-by-default client feature. The client keeps its established production-compatible endpoint/session behavior because changing an endpoint or session-domain literal without a coordinated backend rollout can isolate existing users.

## Expected service boundary

- Accept secure WebSocket connections only.
- Validate origin, route, payload size, and schema server-side.
- Treat client puzzle/room data as untrusted.
- Do not use the service for authentication, moderation, payment, or sensitive personal data.
- Publish a privacy policy and retention policy before operating a public endpoint.

## Release compatibility

This client release does not deploy or rename a backend. Coordinate any endpoint/domain/session change with a backwards-compatible server rollout, migration window, and real-client verification. The mod's build workflow does not publish infrastructure or create a GitHub Release.

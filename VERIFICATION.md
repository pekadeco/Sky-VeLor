# Release verification checklist

For **The Sky's Lunaria Mod Beta-0.10.9.4v**:

1. Build with Java 8 ForgeGradle.
2. Run every standalone test via `tests/run-tests.sh` or `tests/run-tests.bat`.
3. Verify the reobfuscated JAR with `tools/verify_release_jar.py`.
4. Verify the curated source ZIP with `tools/verify_source_zip.py`.
5. Confirm delivery ZIP contains the JAR and build report.
6. For a publish stage, confirm `dist/` contains exactly the verified JAR, source ZIP, and delivery ZIP.
7. Perform a manual Minecraft 1.8.9 Forge visual/input check before claiming live-server behavior.

The artifact verifier checks Lunaria metadata, required feature surfaces, supplied artwork dimensions, archive hygiene, and compatibility command routes. It does not simulate a real server or replace a human in-game test.

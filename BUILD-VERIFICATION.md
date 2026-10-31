# Build verification

The Lunaria build gate is Java 8 ForgeGradle compilation, layout evidence, every standalone test, JAR verification, source-archive verification, and delivery-ZIP creation.

Run locally:

```sh
./gradlew clean build dumpTestClasspath
TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
python3 tools/verify_release_jar.py build/libs/Lunaria-Mod-1.8.9-Beta-0.10.9.4v.jar --version Beta-0.10.9.4v --mcmod-version 0.10.9.4
```

CI publishes only after all gates pass and a branch commit explicitly carries `[publish-artifacts]`. That publication writes the verified JAR, source ZIP, and delivery ZIP to `dist/`; it does not create a GitHub Release.

#!/bin/sh
# Compiles and runs the pure-logic, config, text and migration tests. No game is started.
#
# Needs: a JDK 8 on PATH, the compiled classes (build/classes/main, from `gradle build`), and two jars:
#   MC_JAR    minecraft_merged-1.8.9.jar (Forge's deobfuscated 1.8.9 client jar)
#   LOG4J_JAR log4j-api jar (used by the HUD config warnings)
#   GSON_JAR  gson jar (Minecraft 1.8.9 ships gson 2.2.4; used by the route file reader)
# Example:
#   MC_JAR=~/.gradle/caches/.../minecraft_merged-1.8.9.jar LOG4J_JAR=~/.gradle/.../log4j-api.jar sh tests/run-tests.sh
set -e
HERE=$(cd "$(dirname "$0")/.." && pwd)
CLASSES="${CLASSES:-$HERE/build/classes/main}"
# Easiest way to get every jar the build used: `gradle build dumpTestClasspath` then
#   TEST_CP=$PWD/build/test-classpath.txt sh tests/run-tests.sh
if [ -n "${TEST_CP:-}" ]; then
    MC_JAR="${MC_JAR:-x}"
    LOG4J_JAR="${LOG4J_JAR:-x}"
    GSON_JAR="${GSON_JAR:-x}"
    EXTRA=$(cat "$TEST_CP")
else
    : "${MC_JAR:?set MC_JAR, or set TEST_CP to the file written by gradle dumpTestClasspath}"
    : "${LOG4J_JAR:?set LOG4J_JAR to the log4j-api jar, or set TEST_CP}"
    : "${GSON_JAR:?set GSON_JAR to the gson jar, or set TEST_CP}"
    EXTRA="$MC_JAR:$LOG4J_JAR:$GSON_JAR"
fi
OUT=$(mktemp -d)
CP="$CLASSES:$EXTRA"
javac -nowarn -cp "$CP" -d "$OUT" $(find "$HERE/tests" -name '*.java')
java -cp "$OUT:$CP" com.arena.pekaclient.gui.PureLogicTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.ConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapNameTests
java -cp "$OUT:$CP" com.arena.pekaclient.startup.EarlySplashTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.ConfigFilesTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.DollConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapClassTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.SpiritLeapTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.DungeonContextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.MobMarkersTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.StatsTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.startup.StartupTimingTests
rm -rf "$OUT"

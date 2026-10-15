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
: "${MC_JAR:?set MC_JAR to minecraft_merged-1.8.9.jar}"
: "${LOG4J_JAR:?set LOG4J_JAR to the log4j-api jar}"
: "${GSON_JAR:?set GSON_JAR to the gson jar}"
OUT=$(mktemp -d)
CP="$CLASSES:$MC_JAR:$LOG4J_JAR:$GSON_JAR"
javac -nowarn -cp "$CP" -d "$OUT" $(find "$HERE/tests" -name '*.java')
java -cp "$OUT:$CP" com.arena.pekaclient.gui.PureLogicTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.ConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.ConfigFilesTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.DollConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapClassTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.SpiritLeapTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.DungeonContextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.MobMarkersTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.StatsTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.startup.StartupTimingTests
java -cp "$OUT:$CP" com.arena.pekaclient.fishing.FishingTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeon.DragonKillTests
java -cp "$OUT:$CP" com.arena.pekaclient.slayer.SlayerTests
rm -rf "$OUT"

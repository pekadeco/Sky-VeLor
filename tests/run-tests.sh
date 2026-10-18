#!/bin/sh
# Compiles and runs the pure-logic, config, text and migration tests. No game is started.
#
# Needs a JDK 8, compiled classes (build/classes/main), and either TEST_CP (the complete Forge test classpath)
# or the MC_JAR, LOG4J_JAR and GSON_JAR paths used by the legacy invocation.
# Example after `gradle build dumpTestClasspath`:
#   TEST_CP="$(cat build/test-classpath.txt)" sh tests/run-tests.sh
set -e
HERE=$(cd "$(dirname "$0")/.." && pwd)
CLASSES="${CLASSES:-$HERE/build/classes/main}"
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
if [ -n "${TEST_CP:-}" ]; then
  CP="$CLASSES:$TEST_CP"
else
  : "${MC_JAR:?set MC_JAR to minecraft_merged-1.8.9.jar, or set TEST_CP}"
  : "${LOG4J_JAR:?set LOG4J_JAR to the log4j-api jar, or set TEST_CP}"
  : "${GSON_JAR:?set GSON_JAR to the gson jar, or set TEST_CP}"
  CP="$CLASSES:$MC_JAR:$LOG4J_JAR:$GSON_JAR"
fi
javac -nowarn -encoding UTF-8 -cp "$CP" -d "$OUT" $(find "$HERE/tests" -name '*.java')
java -cp "$OUT:$CP" com.arena.pekaclient.gui.PureLogicTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.GuiLayoutToolkitTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ClipboardChainTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ChatPlayerNameTests
java -Djava.awt.headless=true -cp "$OUT:$CP" com.arena.pekaclient.render.FontAtlasGeneratorTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.FontSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.ConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapIdentityTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapClassCacheTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.ConfigFilesTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.AppleSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.ChatQolSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.render.ThemeTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.DollConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapClassTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.SpiritLeapTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.DungeonContextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.HotbarScrollPolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ItemAnimationTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.VisualSwingStateMachineTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ChatTabsTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ChatInteractionModelTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.ChatQolLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.notes.QuickNoteLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.ItemAnimationReachabilityTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.MobMarkersTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapPaletteTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapPreviewDataTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.StatsTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.startup.StartupTimingTests
java -cp "$OUT:$CP" com.arena.pekaclient.fishing.FishingTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeon.DragonKillTests
java -cp "$OUT:$CP" com.arena.pekaclient.slayer.SlayerTests
java -cp "$OUT:$CP" com.arena.pekaclient.widget.SearchFieldTests
java -cp "$OUT:$CP" com.arena.pekaclient.widget.WidgetInteractionTests

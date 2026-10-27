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
java -cp "$OUT:$CP" com.arena.pekaclient.gui.ReferenceSettingsWindowLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.SettingsShellLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.AppleLayoutRendererSourceTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.HomeMetricsLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.HomeMetricHistoryTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.SettingsSpacingLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.SettingsControlLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.HardcodedGuiColorTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.HudEditorSelectionPolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.InlineSettingsCatalogTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.FlatDropdownContractTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.PlayerFacingTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.FarmingNavigationTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.ReleaseClaimsTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ClipboardChainTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ChatPlayerNameTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ChatCompactMessagesTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ChatMessageHistoryTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ChatMenuInputRouterTests
java -cp "$OUT:$CP" com.arena.pekaclient.chat.ChatVisibilityAuditTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.ConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.compat.RemovedFeatureSurfaceTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapIdentityTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapClassCacheTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.ConfigFilesTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.AppleSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.ChatQolSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.DungeonEspSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.config.AssistSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.assist.AssistFeatureTests
java -cp "$OUT:$CP" com.arena.pekaclient.assist.dungeon.TerminalParsePolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeonesp.DungeonEspTrackerTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeonesp.DungeonEspMarkerBoundsTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeonesp.DungeonEspMobDisplayTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeonesp.WitherDoorCacheTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.InlineEspCatalogTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.InlineSettingsBindingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.FlatCatalogBindingAuditTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.InlineOptionPreservationTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapSettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapSlotMapperTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapProtectionPolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapInputStateTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapLineGeometryTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapSwapPolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.itemlockswap.ItemLockSwapTransformerTests
java -cp "$OUT:$CP" com.arena.pekaclient.render.ThemeTests
java -cp "$OUT:$CP" com.arena.pekaclient.render.GuiPixelGridTests
java -cp "$OUT:$CP" com.arena.pekaclient.render.RoundedRectGeometryTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.DollConfigTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.LeapClassTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.SpiritLeapTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.DungeonContextTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.HotbarScrollPolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.BlockParticlePolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.render.blockparticles.BlockParticleTransformerTests
java -cp "$OUT:$CP" com.arena.pekaclient.assist.visual.VisualAssistTransformerTests
java -cp "$OUT:$CP" com.arena.pekaclient.assist.visual.AspectTeleportTransitionPolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.assist.visual.AspectTeleportTransitionObservationTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ItemAnimationTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.VisualSwingStateMachineTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.SwingEasingTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ChatTabsTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ChatTabClosePolicyTests
java -cp "$OUT:$CP" com.arena.pekaclient.feature.ChatInteractionModelTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.InlineDropdownLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.notes.QuickNoteLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.InlineOwnerBindingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.ItemAnimationReachabilityTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapLayoutTests
java -cp "$OUT:$CP" com.arena.pekaclient.gui.LeapPaletteTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.ClientMetricsTests
java -cp "$OUT:$CP" com.arena.pekaclient.hud.ClientMetricTextTests
java -cp "$OUT:$CP" com.arena.pekaclient.startup.StartupTimingTests
java -cp "$OUT:$CP" com.arena.pekaclient.fishing.FishingTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeon.DragonKillTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeon.M7SettingsTests
java -cp "$OUT:$CP" com.arena.pekaclient.dungeon.PuzzleRoomMemoryTests
java -cp "$OUT:$CP" com.arena.pekaclient.slayer.SlayerTests
java -cp "$OUT:$CP" com.arena.pekaclient.widget.SearchFieldTests
java -cp "$OUT:$CP" com.arena.pekaclient.widget.WidgetInteractionTests

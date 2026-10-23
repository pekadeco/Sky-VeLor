@echo off
setlocal EnableExtensions
set "ROOT=%~dp0.."
set "CLASSES=%ROOT%\build\classes\main"
set "CP_FILE=%ROOT%\build\test-classpath.txt"
set "OUT=%TEMP%\skyvelor-tests-%RANDOM%-%RANDOM%"

if not exist "%CLASSES%" (
    echo Main classes not found. Run gradlew.bat build dumpTestClasspath first.
    exit /b 1
)
if not exist "%CP_FILE%" (
    echo Test classpath not found. Run gradlew.bat build dumpTestClasspath first.
    exit /b 1
)
set /p "TEST_CP="<"%CP_FILE%"
if not defined TEST_CP (
    echo Test classpath is empty: %CP_FILE%
    exit /b 1
)
mkdir "%OUT%" || exit /b 1

for /r "%ROOT%\tests" %%F in (*.java) do echo "%%F" >> "%OUT%\sources.txt"
javac -nowarn -encoding UTF-8 -cp "%CLASSES%;%TEST_CP%" -d "%OUT%" @"%OUT%\sources.txt"
if errorlevel 1 goto fail

set "CP=%OUT%;%CLASSES%;%TEST_CP%"
call :run com.arena.pekaclient.gui.PureLogicTests || goto fail
call :run com.arena.pekaclient.gui.GuiLayoutToolkitTests || goto fail
call :run com.arena.pekaclient.gui.FeatureSettingsUiModelTests || goto fail
call :run com.arena.pekaclient.chat.ClipboardChainTests || goto fail
call :run com.arena.pekaclient.chat.ChatPlayerNameTests || goto fail
call :run com.arena.pekaclient.chat.ChatCompactMessagesTests || goto fail
call :run com.arena.pekaclient.chat.ChatMessageHistoryTests || goto fail
call :run com.arena.pekaclient.chat.ChatMenuInputRouterTests || goto fail
call :run com.arena.pekaclient.chat.ChatVisibilityAuditTests || goto fail
java -Djava.awt.headless=true -cp "%CP%" com.arena.pekaclient.render.FontAtlasGeneratorTests
if errorlevel 1 goto fail
call :run com.arena.pekaclient.config.FontSettingsTests || goto fail
call :run com.arena.pekaclient.hud.ConfigTests || goto fail
call :run com.arena.pekaclient.feature.LeapTextTests || goto fail
call :run com.arena.pekaclient.feature.LeapIdentityTests || goto fail
call :run com.arena.pekaclient.feature.LeapClassCacheTests || goto fail
call :run com.arena.pekaclient.config.ConfigFilesTests || goto fail
call :run com.arena.pekaclient.config.AppleSettingsTests || goto fail
call :run com.arena.pekaclient.config.ChatQolSettingsTests || goto fail
call :run com.arena.pekaclient.config.DungeonEspSettingsTests || goto fail
call :run com.arena.pekaclient.config.AssistSettingsTests || goto fail
call :run com.arena.pekaclient.assist.AssistFeatureTests || goto fail
call :run com.arena.pekaclient.assist.dungeon.TerminalParsePolicyTests || goto fail
call :run com.arena.pekaclient.dungeonesp.DungeonEspTrackerTests || goto fail
call :run com.arena.pekaclient.dungeonesp.WitherDoorCacheTests || goto fail
call :run com.arena.pekaclient.gui.DungeonEspUiModelTests || goto fail
call :run com.arena.pekaclient.gui.AssistUiModelTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapSettingsTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapSlotMapperTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapProtectionPolicyTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapInputStateTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapLineGeometryTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapSwapPolicyTests || goto fail
call :run com.arena.pekaclient.itemlockswap.ItemLockSwapTransformerTests || goto fail
call :run com.arena.pekaclient.render.ThemeTests || goto fail
call :run com.arena.pekaclient.hud.DollConfigTests || goto fail
call :run com.arena.pekaclient.feature.LeapClassTests || goto fail
call :run com.arena.pekaclient.feature.SpiritLeapTests || goto fail
call :run com.arena.pekaclient.feature.DungeonContextTests || goto fail
call :run com.arena.pekaclient.feature.HotbarScrollPolicyTests || goto fail
call :run com.arena.pekaclient.feature.BlockParticlePolicyTests || goto fail
call :run com.arena.pekaclient.render.blockparticles.BlockParticleTransformerTests || goto fail
call :run com.arena.pekaclient.assist.visual.VisualAssistTransformerTests || goto fail
call :run com.arena.pekaclient.feature.ItemAnimationTests || goto fail
call :run com.arena.pekaclient.feature.VisualSwingStateMachineTests || goto fail
call :run com.arena.pekaclient.feature.SwingEasingTests || goto fail
call :run com.arena.pekaclient.feature.ChatTabsTests || goto fail
call :run com.arena.pekaclient.feature.ChatInteractionModelTests || goto fail
call :run com.arena.pekaclient.gui.ChatQolLayoutTests || goto fail
call :run com.arena.pekaclient.notes.QuickNoteLayoutTests || goto fail
call :run com.arena.pekaclient.gui.QualityOfLifeUiModelTests || goto fail
call :run com.arena.pekaclient.gui.ItemAnimationReachabilityTests || goto fail
call :run com.arena.pekaclient.gui.LeapLayoutTests || goto fail
call :run com.arena.pekaclient.gui.LeapPaletteTests || goto fail
call :run com.arena.pekaclient.gui.LeapPreviewDataTests || goto fail
call :run com.arena.pekaclient.hud.StatsTextTests || goto fail
call :run com.arena.pekaclient.startup.StartupTimingTests || goto fail
call :run com.arena.pekaclient.fishing.FishingTests || goto fail
call :run com.arena.pekaclient.dungeon.DragonKillTests || goto fail
call :run com.arena.pekaclient.dungeon.PuzzleRoomMemoryTests || goto fail
call :run com.arena.pekaclient.slayer.SlayerTests || goto fail
call :run com.arena.pekaclient.widget.SearchFieldTests || goto fail
call :run com.arena.pekaclient.widget.WidgetInteractionTests || goto fail

echo All Sky VeLor tests passed.
rmdir /s /q "%OUT%"
exit /b 0

:run
 echo === %~1 ===
 java -cp "%CP%" %~1
 if errorlevel 1 exit /b 1
 exit /b 0

:fail
 echo Tests failed. Temporary output kept at "%OUT%" for diagnosis.
 exit /b 1

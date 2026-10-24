package com.arena.pekaclient.gui;

import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Packaged-class regression proof for the refined direct-category settings shell and retained boundaries. */
public final class ReleaseClaimsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        vanillaFontSurfaceHasNoCustomFontSubsystem();
        hudEditorHasNoTargetSidebar();
        referenceShellHasRequiredBehavior();
        sharedRoundedRendererIsPackaged();
        compactSettingsLayoutIsPackaged();
        editorStateCleanupIsPackaged();
        retainedControlsKeepTheirOwners();
        removedPrototypeEntriesStayRemoved();
        done();
    }

    private static void vanillaFontSurfaceHasNoCustomFontSubsystem() throws Exception {
        Method accessor = AppleScreen.class.getDeclaredMethod("fr");
        check("shared GUI accessor has vanilla FontRenderer type",
                "net.minecraft.client.gui.FontRenderer".equals(accessor.getReturnType().getName()));
        check("shared GUI accessor references Minecraft FontRenderer", classText("com.arena.pekaclient.gui.AppleScreen")
                .contains("net/minecraft/client/gui/FontRenderer"));
        check("removed custom-font classes are absent", !classExists("com.arena.pekaclient.config.FontSettings")
                && !classExists("com.arena.pekaclient.feature.ChatFontPolicy")
                && !classExists("com.arena.pekaclient.gui.FontsScreen")
                && !classExists("com.arena.pekaclient.render.FontAtlasGenerator")
                && !classExists("com.arena.pekaclient.render.GlobalFontManager"));
    }

    private static void hudEditorHasNoTargetSidebar() {
        String editor = classText("com.arena.pekaclient.gui.HudEditorScreen");
        check("HUD editor gives the direct-canvas selection instruction",
                editor.contains("Click an on-screen HUD element to select it; drag to move it."));
        check("HUD editor has no target-sidebar route", !classExists("com.arena.pekaclient.gui.HudSidebar")
                && !editor.contains("Choose HUD target") && !editor.contains("buildHudEditorTargetList"));
        boolean[] overlap = {true, true, false};
        check("canvas selection still cycles overlapping elements", HudEditorSelectionPolicy.nextIndex(0, overlap) == 1
                && HudEditorSelectionPolicy.nextIndex(1, overlap) == 0);
    }

    private static void referenceShellHasRequiredBehavior() {
        String[] destinations = {"Home", "Gameplay", "Dungeon", "Farming", "Visuals", "System"};
        check("reference shell destination order is exact", Arrays.equals(destinations, AppleSettingsScreen.destinationNames()));
        check("reference screen class is packaged", classExists("com.arena.pekaclient.gui.AppleSettingsScreen"));
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        check("screen has global search without an Advanced-button state", screen.contains("Search settings")
                && !screen.contains("Advanced settings shown") && !screen.contains("Advanced settings hidden"));
        check("Farming page packages its audited empty-state message and rail destination",
                screen.contains("No farming features yet") && screen.contains("FARMING"));
        check("screen clips its full rounded shell plus one settings viewport", screen.contains("roundedClipBegin")
                && screen.contains("scissorBegin") && screen.contains("GuiLayoutToolkit$Scroll"));
        check("screen packages title-bar drag, fullscreen persistence and one Theme-owned shell", screen.contains("smallWindowOffsetX")
                && screen.contains("settingsWindowFullscreen") && screen.contains("Sky VeLor") && screen.contains("Settings")
                && screen.contains("SettingsControlLayout") && screen.contains("refreshReferenceTheme")
                && !screen.contains("appearance-theme") && !screen.contains("appearance-accent"));
        check("old Hub is compatibility-only", classText("com.arena.pekaclient.gui.AppleHubScreen")
                .contains("AppleSettingsScreen"));
    }

    private static void sharedRoundedRendererIsPackaged() {
        String renderer = classText("com.arena.pekaclient.render.UIRender");
        check("shared cached rounded geometry is packaged", classExists("com.arena.pekaclient.render.RoundedRectGeometry")
                && classExists("com.arena.pekaclient.render.GuiStyle") && classExists("com.arena.pekaclient.render.GuiMotion"));
        check("all fill and outline facades route through one rounded primitive", renderer.contains("RoundedRectGeometry")
                && renderer.contains("roundedRect") && renderer.contains("roundRectBorder") && renderer.contains("roundRectRing"));
    }

    private static void compactSettingsLayoutIsPackaged() {
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        check("shared compact rail and page-reflow policies are packaged",
                classExists("com.arena.pekaclient.gui.SettingsRailLayout")
                        && classExists("com.arena.pekaclient.gui.SettingsPageLayout")
                        && screen.contains("SettingsRailLayout") && screen.contains("SettingsPageLayout"));
        check("settings layout exposes overlap-debug bounds", screen.contains("Settings rail")
                && screen.contains("Settings card") && screen.contains("Settings row"));
    }

    private static void editorStateCleanupIsPackaged() {
        String uiRender = classText("com.arena.pekaclient.render.UIRender");
        String manager = classText("com.arena.pekaclient.hud.HudManager");
        String element = classText("com.arena.pekaclient.hud.HudElement");
        String itemSlots = classText("com.arena.pekaclient.hud.ItemSlots");
        String editor = classText("com.arena.pekaclient.gui.HudEditorScreen");
        check("shared renderer packages a neutral GUI restoration baseline", uiRender.contains("restoreVanillaGuiState")
                && uiRender.contains("restoreFrameState"));
        check("HUD editor packages palette/state restoration around direct previews", editor.contains("restoreVanillaGuiState")
                && editor.contains("restoreFrameState"));
        check("HUD manager resets editor-only hover flags", manager.contains("backgroundOnHoverOnly")
                && manager.contains("hoveredInEditor"));
        check("HUD element and item slots use shared state restoration", element.contains("alphaMul")
                && itemSlots.contains("restoreFrameState"));
    }

    private static void retainedControlsKeepTheirOwners() {
        ReferenceSettingsCatalog.Setting shake = ReferenceSettingsCatalog.find("hurt-camera-shake");
        ReferenceSettingsCatalog.Setting sprint = ReferenceSettingsCatalog.find("auto-sprint");
        check("Hurt Camera Shake remains a direct Quality of Life slider", shake != null
                && shake.control == ReferenceSettingsCatalog.Control.SLIDER && shake.min == 0 && shake.max == 100);
        check("Auto Sprint remains a direct Quality of Life control", sprint != null
                && sprint.control == ReferenceSettingsCatalog.Control.TOGGLE);
        ReferenceSettingsCatalog.Setting lock = ReferenceSettingsCatalog.find("item-lock-swap");
        check("Item Lock and Swap retains a focused route", lock != null && "Open details".equals(lock.actionLabel)
                && ItemLockSwapUiModel.buildControlModel().size() == 14);
        check("Item Animation route and inline settings survive", ReferenceSettingsCatalog.find("item-animation") != null
                && ReferenceSettingsCatalog.find("item-animation-size") != null);
        check("HUD editor remains while the retired metric HUD route is absent", ReferenceSettingsCatalog.find("hud-stats") == null
                && ReferenceSettingsCatalog.find("hud-editor") != null);
        check("Dungeon route inventory remains complete", ReferenceSettingsCatalog.routesAreComplete()
                && ReferenceSettingsCatalog.find("door-esp") != null && ReferenceSettingsCatalog.find("m7-overlay") != null);
        check("fixed theme keeps only radius and opacity appearance controls", ReferenceSettingsCatalog.find("appearance-theme") == null
                && ReferenceSettingsCatalog.find("appearance") == null
                && ReferenceSettingsCatalog.find("appearance-accent") == null
                && ReferenceSettingsCatalog.find("appearance-corners") != null
                && ReferenceSettingsCatalog.find("appearance-opacity") != null);
    }

    private static void removedPrototypeEntriesStayRemoved() {
        check("retired hotbar customization has no reference route", ReferenceSettingsCatalog.find("hud-hotbar") == null
                && !classExists("com.arena.pekaclient.hud.HotbarHud"));
        check("retired Item Protection has no reference route", ReferenceSettingsCatalog.find("item-protection") == null
                && !classExists("com.arena.pekaclient.assist.guard.ItemGuardController"));
        check("binary No Hurt Cam remains retired", ReferenceSettingsCatalog.find("no-hurt-cam") == null);
        check("Player Dollars remains retired", ReferenceSettingsCatalog.find("hud-dollars") == null);
        check("in-game metric HUD classes remain absent", !classExists("com.arena.pekaclient.hud.StatsHud")
                && !classExists("com.arena.pekaclient.hud.StatsHudText"));
    }

    private static boolean classExists(String binaryName) {
        return ReleaseClaimsTests.class.getClassLoader().getResource(binaryName.replace('.', '/') + ".class") != null;
    }

    private static String classText(String binaryName) {
        InputStream input = ReleaseClaimsTests.class.getClassLoader().getResourceAsStream(binaryName.replace('.', '/') + ".class");
        if (input == null) return "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), "ISO-8859-1");
        } catch (IOException ignored) {
            return "";
        } finally {
            try { input.close(); } catch (IOException ignored) { }
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("release claims: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}

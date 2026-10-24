package com.arena.pekaclient.gui;

import com.arena.pekaclient.widget.AppleToggle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Packaged-class regression proof for the inline-only settings release. */
public final class ReleaseClaimsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        vanillaFontSurfaceHasNoCustomFontSubsystem();
        onlyAllowedEditorScreensRemain();
        inlineShellHasRequiredBehavior();
        sharedRoundedRendererIsPackaged();
        headerAndToggleContractsArePackaged();
        removedFocusedSurfacesStayAbsent();
        done();
    }

    private static void vanillaFontSurfaceHasNoCustomFontSubsystem() throws Exception {
        Method accessor = AppleScreen.class.getDeclaredMethod("fr");
        check("shared GUI accessor has vanilla FontRenderer type",
                "net.minecraft.client.gui.FontRenderer".equals(accessor.getReturnType().getName()));
        check("removed custom-font classes are absent", !classExists("com.arena.pekaclient.config.FontSettings")
                && !classExists("com.arena.pekaclient.feature.ChatFontPolicy")
                && !classExists("com.arena.pekaclient.gui.FontsScreen"));
    }

    private static void onlyAllowedEditorScreensRemain() {
        check("HUD editor remains", classExists("com.arena.pekaclient.gui.HudEditorScreen"));
        check("retained HUD editor owns Item Animation editor mode", classText("com.arena.pekaclient.gui.HudEditorScreen")
                .contains("Item Animation"));
        String[] retired = {
                "com.arena.pekaclient.gui.FeatureSettingsEditorScreen",
                "com.arena.pekaclient.gui.FeatureSettingsEditors",
                "com.arena.pekaclient.gui.DungeonEspEditorSpecs",
                "com.arena.pekaclient.gui.ChatEditorScreen",
                "com.arena.pekaclient.gui.ModSettingsScreen"
        };
        for (String name : retired) check("focused surface removed: " + name, !classExists(name));
    }

    private static void inlineShellHasRequiredBehavior() {
        String[] destinations = {"Home", "Gameplay", "Dungeon", "Farming", "Mining", "Fishing", "Visuals", "System"};
        check("reference shell destination order is exact", Arrays.equals(destinations, AppleSettingsScreen.destinationNames()));
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        check("screen has search and persisted inline disclosure", screen.contains("Search settings")
                && screen.contains("HIT_DISCLOSURE") && screen.contains("dropdownOpen")
                && screen.contains("expandSearchResult"));
        check("screen clips its rounded shell and one settings viewport", screen.contains("roundedClipBegin")
                && screen.contains("scissorBegin") && screen.contains("GuiLayoutToolkit$Scroll"));
        check("screen packages title drag, fullscreen persistence and right-aligned control layout",
                screen.contains("smallWindowOffsetX") && screen.contains("settingsWindowFullscreen")
                        && screen.contains("SettingsControlLayout") && screen.contains("drawFullscreenGlyph"));
        check("Chat command route enters the inline owner", classText("com.arena.pekaclient.command.ClientActions")
                .contains("openChatSettings") && !classText("com.arena.pekaclient.command.ClientActions").contains("ChatEditorScreen"));
    }

    private static void sharedRoundedRendererIsPackaged() {
        String renderer = classText("com.arena.pekaclient.render.UIRender");
        check("shared cached rounded geometry is packaged", classExists("com.arena.pekaclient.render.RoundedRectGeometry")
                && classExists("com.arena.pekaclient.render.GuiStyle") && classExists("com.arena.pekaclient.render.GuiMotion"));
        check("rounded fill and outline facades share one primitive", renderer.contains("RoundedRectGeometry")
                && renderer.contains("roundRectBorder") && renderer.contains("roundRectRing"));
    }

    private static void headerAndToggleContractsArePackaged() {
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        AppleToggle toggle = new AppleToggle(() -> false, value -> { });
        check("fullscreen icon has a dedicated corner-bracket renderer", screen.contains("drawFullscreenGlyph"));
        check("all inline toggles retain the standard 38 by 20 pill", toggle.preferredWidth() == 38f && toggle.h == 20f);
        check("safe notice is compact one line", screen.contains("Safe by default"));
        check("settings shell has no launcher-owned MOUSE/GUI labels", !screen.contains("MOUSE")
                && !screen.contains("MOUSE/GUI") && !screen.contains("GUI AREA"));
    }

    private static void removedFocusedSurfacesStayAbsent() {
        check("chat and ESP preview-only classes remain absent", !classExists("com.arena.pekaclient.gui.ChatEditorPreview")
                && !classExists("com.arena.pekaclient.gui.DungeonEspPreviewRenderer")
                && !classExists("com.arena.pekaclient.gui.LeapPreviewData"));
        String hudEditor = classText("com.arena.pekaclient.gui.HudEditorScreen");
        check("Item Animation retains Preview Swing without an editor item sample surface",
                hudEditor.contains("previewSwing") && !hudEditor.contains("LIVE HELD-ITEM PREVIEW")
                        && !hudEditor.contains("drawItemAnimationEditorPreview"));
        check("retired feature routes are not reintroduced", ReferenceSettingsCatalog.find("hud-stats") == null
                && ReferenceSettingsCatalog.find("item-protection") == null
                && ReferenceSettingsCatalog.find("no-hurt-cam") == null);
    }

    private static boolean classExists(String binaryName) {
        return ReleaseClaimsTests.class.getClassLoader().getResource(binaryName.replace('.', '/') + ".class") != null;
    }

    private static String classText(String binaryName) {
        InputStream input = ReleaseClaimsTests.class.getClassLoader()
                .getResourceAsStream(binaryName.replace('.', '/') + ".class");
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

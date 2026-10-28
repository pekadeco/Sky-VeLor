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
        visualRuntimeContractsArePackaged();
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
        check("Item Animation owns a separate editor surface", classExists("com.arena.pekaclient.gui.ItemAnimationEditorScreen")
                && classText("com.arena.pekaclient.command.ClientActions").contains("ItemAnimationEditorScreen")
                && !classExists("com.arena.pekaclient.gui.ItemAnimationUiModel"));
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
        String[] destinations = {"Home", "QoL", "Slayer", "Dungeons", "Farming", "Mining", "Fishing", "HUD & Visuals", "Misc"};
        check("feature-oriented shell destination order is exact", Arrays.equals(destinations, AppleSettingsScreen.destinationNames()));
        String screen = classText("com.arena.pekaclient.gui.AppleSettingsScreen");
        check("screen has search, accordion disclosure, fresh-open reset, and header reveal", screen.contains("Search settings")
                && screen.contains("HIT_DISCLOSURE") && screen.contains("dropdownOpen")
                && screen.contains("expandSearchResult") && screen.contains("openAccordion")
                && screen.contains("closeAllDropdowns") && screen.contains("normalizeAccordion")
                && screen.contains("revealDropdownHeader"));
        check("screen clips its rounded shell and one settings viewport", screen.contains("roundedClipBegin")
                && screen.contains("scissorBegin") && screen.contains("GuiLayoutToolkit$Scroll"));
        check("screen packages wordmark drag, fullscreen persistence and named editor control layout",
                screen.contains("SkyVeLor") && screen.contains("smallWindowOffsetX") && screen.contains("settingsWindowFullscreen")
                        && screen.contains("SettingsControlLayout") && screen.contains("drawFullscreenGlyph")
                        && screen.contains("editorButtonWidth") && screen.contains("all QoL ->")
                        && !screen.contains("all player ->") && !screen.contains("[Edit]"));
        String catalog = classText("com.arena.pekaclient.gui.ReferenceSettingsCatalog");
        check("editor routes expose clear destination labels", catalog.contains("Open HUD Editor")
                && catalog.contains("Open Item Animation Editor"));
        check("Chat command route enters its requested inline owner after the ordinary reset",
                classText("com.arena.pekaclient.command.ClientActions").contains("openChatSettings")
                        && !classText("com.arena.pekaclient.command.ClientActions").contains("ChatEditorScreen")
                        && screen.contains("chat-qol") && screen.contains("openAccordion"));
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
        String dropdown = classText("com.arena.pekaclient.widget.AppleDropdown");
        String theme = classText("com.arena.pekaclient.render.Theme");
        AppleToggle toggle = new AppleToggle(() -> false, value -> { });
        check("upper-left SkyVeLor wordmark keeps the existing fullscreen/close-only header", screen.contains("SkyVeLor")
                && screen.contains("HIT_WINDOW_FULLSCREEN") && screen.contains("HIT_WINDOW_CLOSE"));
        check("fullscreen icon has a dedicated corner-bracket renderer", screen.contains("drawFullscreenGlyph"));
        check("all inline toggles retain the standard 38 by 20 pill", toggle.preferredWidth() == 38f && toggle.h == 20f);
        check("all named and maintenance actions use the shared trailing action geometry",
                screen.contains("actionButtonX") && screen.contains("actionButtonWidth")
                        && screen.contains("ACTION_BUTTON_H") && screen.contains("editorToggleX")
                        && SettingsControlLayout.ACTION_BUTTON_WIDTH == 132f
                        && SettingsControlLayout.CONTROL_HEIGHT == 20f);
        check("safe notice is compact one line", screen.contains("Safe by default"));
        check("settings shell has no launcher-owned MOUSE/GUI labels", !screen.contains("MOUSE")
                && !screen.contains("MOUSE/GUI") && !screen.contains("GUI AREA"));
        check("retired profile card and traffic-light actions stay absent", !screen.contains("HIT_PROFILE")
                && !screen.contains("window-red") && !screen.contains("window-yellow") && !screen.contains("window-green"));
        check("restored Apple card chrome keeps icon, glass surface, and subtitle hierarchy", screen.contains("drawCardGlyph")
                && screen.contains("roundRectBorder") && screen.contains("subtitle"));
        check("shared flat dropdown packages contrast, hover, selected check, and dividers", dropdown.contains("dropdownTrigger")
                && dropdown.contains("dropdownPopup") && dropdown.contains("dropdownRowHover")
                && dropdown.contains("dropdownRowSelected") && dropdown.contains("dropdownDivider")
                && dropdown.contains("drawSelectedCheck") && dropdown.contains("reservePopup"));
        check("expanded feature disclosures package a readable inset panel and muted-but-legible dependencies",
                screen.contains("expandedPanel") && screen.contains("expandedBorder")
                        && screen.contains("expandedRowHover") && screen.contains("dependentTextPrimary")
                        && theme.contains("EXPANDED_PANEL") && theme.contains("DEPENDENT_TEXT_PRIMARY")
                        && theme.contains("DEPENDENT_CONTROL_ALPHA"));
    }

    private static void removedFocusedSurfacesStayAbsent() {
        check("chat and ESP preview-only classes remain absent", !classExists("com.arena.pekaclient.gui.ChatEditorPreview")
                && !classExists("com.arena.pekaclient.gui.DungeonEspPreviewRenderer")
                && !classExists("com.arena.pekaclient.gui.LeapPreviewData"));
        String itemPanel = classText("com.arena.pekaclient.gui.ItemAnimationEditorScreen");
        String theme = classText("com.arena.pekaclient.render.Theme");
        check("Item Animation control model retains Preview Swing",
                ItemAnimationPanelModel.controlLabels().contains("Preview Swing"));
        check("Item Animation panel has reset, an opaque dark surface, and no HUD/world preview layer",
                ItemAnimationPanelModel.controlLabels().contains("Reset to Defaults") && itemPanel.contains("roundRectBorder")
                        && theme.contains("ITEM_ANIMATION_PANEL") && theme.contains("ITEM_ANIMATION_PANEL_BORDER")
                        && !itemPanel.contains("drawDefaultBackground") && !itemPanel.contains("HudManager")
                        && !itemPanel.contains("drawItemAnimationEditorPreview"));
        String hudEditor = classText("com.arena.pekaclient.gui.HudEditorScreen");
        check("HUD Editor retains only the compatibility route, not an Item Animation target",
                hudEditor.contains("ItemAnimationEditorScreen") && !hudEditor.contains("ITEM_ANIMATION")
                        && !hudEditor.contains("ItemAnimationUiModel"));
        check("retired feature routes are not reintroduced", ReferenceSettingsCatalog.find("hud-stats") == null
                && ReferenceSettingsCatalog.find("item-protection") == null
                && ReferenceSettingsCatalog.find("no-hurt-cam") == null);
    }

    private static void visualRuntimeContractsArePackaged() {
        String client = classText("com.arena.pekaclient.PekaClientMod");
        String deadFilter = classText("com.arena.pekaclient.assist.visual.DeadMobRenderFilter");
        String deadPolicy = classText("com.arena.pekaclient.assist.visual.DeadMobRenderPolicy");
        String aspectRenderer = classText("com.arena.pekaclient.assist.visual.AspectTeleportTransitionRenderer");
        String aspectObservation = classText("com.arena.pekaclient.assist.visual.AspectTeleportTransitionObservation");
        String aspectPolicy = classText("com.arena.pekaclient.assist.visual.AspectTeleportTransitionPolicy");
        check("dead-mob cleanup has a dedicated cancellable living render filter",
                classExists("com.arena.pekaclient.assist.visual.DeadMobRenderFilter")
                        && deadFilter.contains("RenderLivingEvent$Pre") && deadFilter.contains("setCanceled")
                        && deadFilter.contains("NetworkPlayerInfo") && deadFilter.contains("getPlayerInfo")
                        && deadPolicy.contains("shouldSuppress") && deadPolicy.contains("hasNpcMarker"));
        check("dead-mob cleanup remains client render-only", !deadFilter.contains("sendPacket")
                && !deadFilter.contains("setPosition") && !deadFilter.contains("setHealth")
                && !deadFilter.contains("removeEntity") && !deadFilter.contains("setDead"));
        check("client registers dead-body/nameplate filtering and render-time Aspect observation",
                client.contains("deadMobRenderFilter") && client.contains("DeadMobRenderFilter")
                        && client.contains("RenderTickEvent") && client.contains("renderTick"));
        check("Smooth Aspect arms on ticks but samples the visual discontinuity at render time",
                aspectRenderer.contains("advanceTick") && aspectRenderer.contains("renderTick")
                        && aspectObservation.contains("advanceTick") && aspectPolicy.contains("MAX_TELEPORT_DISTANCE"));
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

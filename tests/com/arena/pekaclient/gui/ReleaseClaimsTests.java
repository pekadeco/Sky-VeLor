package com.arena.pekaclient.gui;

import com.arena.pekaclient.itemlockswap.ItemLockSwapUiModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Regression proof for the Beta-0.10.8.9.1 cleanup and migration claims.
 *
 * <p>The release-JAR verifier repeats the packaged-artifact side of these checks. This suite keeps the
 * user-visible models and the intentionally absent implementation surfaces honest before packaging.</p>
 */
public final class ReleaseClaimsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        vanillaFontSurfaceHasNoCustomFontSubsystem();
        hudEditorHasNoTargetSidebar();
        hurtCameraShakeAndAutoSprintUseDirectControls();
        hotbarAndItemProtectionStayRetiredWhileItemLockSwapRemains();
        categoryNamesAndDungeonOrderStayExact();
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
        String transforms = classText("com.arena.pekaclient.render.itemanimation.ItemAnimationTransformer")
                + classText("com.arena.pekaclient.render.blockparticles.BlockParticleTransformer")
                + classText("com.arena.pekaclient.assist.visual.VisualAssistTransformer")
                + classText("com.arena.pekaclient.itemlockswap.ItemLockSwapTransformer");
        check("registered bytecode transformers do not target FontRenderer", !transforms.contains("FontRenderer")
                && !transforms.contains("fontRenderer"));
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

    private static void hurtCameraShakeAndAutoSprintUseDirectControls() {
        FeatureSettingsUiModel.Feature shake = FeatureSettingsUiModel.feature("hurt-camera-shake");
        FeatureSettingsUiModel.Feature sprint = FeatureSettingsUiModel.feature("auto-sprint");
        check("Hurt Camera Shake is a one-slider Quality of Life row", shake != null && shake.isSlider()
                && "Quality of Life".equals(shake.tab) && "Hurt Camera Shake".equals(shake.title)
                && shake.optionIds.size() == 1 && shake.editorId.length() == 0);
        check("binary No Hurt Cam route is retired", FeatureSettingsUiModel.feature("no-hurt-cam") == null);
        check("Auto Sprint is a one-toggle Quality of Life row", isSimpleToggle(sprint, "Quality of Life", "Auto Sprint"));
        check("Auto Sprint text preserves normal-key-state behavior",
                sprint != null && "Sprints automatically while you hold forward.".equals(sprint.description));
    }

    private static void hotbarAndItemProtectionStayRetiredWhileItemLockSwapRemains() {
        check("retired hotbar customization has no source route", FeatureSettingsUiModel.feature("hud-hotbar") == null
                && !classExists("com.arena.pekaclient.hud.HotbarHud"));
        check("retired Item Protection has no source route", FeatureSettingsUiModel.feature("item-protection") == null
                && !classExists("com.arena.pekaclient.assist.guard.ItemGuardController")
                && !classExists("com.arena.pekaclient.assist.guard.ItemGuardPolicy"));
        FeatureSettingsUiModel.Feature lockSwap = FeatureSettingsUiModel.feature("item-lock-swap");
        check("Item Lock and Swap retains its editor route", lockSwap != null && lockSwap.isDropdown()
                && "item-lock-swap".equals(lockSwap.editorId)
                && ItemLockSwapUiModel.buildControlModel().size() == 14);
    }

    private static void categoryNamesAndDungeonOrderStayExact() {
        String[] categories = {"General", "Quality of Life", "HUD", "Farming", "Dungeon", "Slayer", "Fishing"};
        check("Hub category table is exact", Arrays.equals(categories, AppleHubScreen.PAGE_NAMES));
        for (AssistUiModel.Entry entry : AssistUiModel.entries()) {
            check("assist catalog uses a current category: " + entry.name,
                    Arrays.asList(categories).contains(entry.tab));
        }
        List<String> dungeon = new ArrayList<String>();
        for (FeatureSettingsUiModel.Feature feature : FeatureSettingsUiModel.features()) {
            if ("Dungeon".equals(feature.tab)) dungeon.add(feature.id);
        }
        check("Dungeon rows retain the requested usage order", dungeon.equals(Arrays.asList(
                "door-esp", "mob-esp", "dungeon-sync-enabled", "m7-overlay", "spirit-leap",
                "terminal-assist", "dungeon-boss-hud", "dungeon-map", "dungeon-sync-debug")));
    }

    private static boolean isSimpleToggle(FeatureSettingsUiModel.Feature feature, String tab, String title) {
        return feature != null && feature.isSimple() && tab.equals(feature.tab) && title.equals(feature.title)
                && feature.tabControls.equals(Collections.singletonList(FeatureSettingsUiModel.TOGGLE))
                && feature.optionIds.size() == 1 && feature.editorId.length() == 0;
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

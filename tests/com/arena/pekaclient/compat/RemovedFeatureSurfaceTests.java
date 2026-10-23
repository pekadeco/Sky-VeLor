package com.arena.pekaclient.compat;

import com.arena.pekaclient.assist.inventory.InventoryAssistController;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.gui.FeatureSettingsUiModel;
import com.arena.pekaclient.hud.HudElement;
import com.arena.pekaclient.hud.HudManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Focused source/class-surface checks for narrowly retired features and retained normal inventory support. */
public final class RemovedFeatureSurfaceTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        removedClassesAreNotPackaged();
        retiredControlsAndSettingsAreAbsent();
        inventoryControllerRetainsOnlyNormalHelpers();
        normalInventoryHudRemainsRegistered();
        System.out.println("removed feature surfaces: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " removal-surface checks failed");
    }

    private static void removedClassesAreNotPackaged() {
        check("Player Dollars HUD class is absent", classAbsent("com.arena.pekaclient.hud.DollarsHud"));
        check("Player Dollars balance source is absent", classAbsent("com.arena.pekaclient.hud.BalanceSource"));
        check("Inventory Search policy is absent", classAbsent("com.arena.pekaclient.assist.inventory.InventorySearchPolicy"));
    }

    private static void retiredControlsAndSettingsAreAbsent() {
        check("Player Dollars Hub route is absent", FeatureSettingsUiModel.feature("hud-dollars") == null);
        check("binary No Hurt Cam route is absent", FeatureSettingsUiModel.feature("no-hurt-cam") == null);
        check("Inventory Search option is absent", FeatureSettingsUiModel.option("inventory-search") == null);
        check("Hurt Camera Shake replaces binary camera control", FeatureSettingsUiModel.feature("hurt-camera-shake") != null
                && FeatureSettingsUiModel.feature("hurt-camera-shake").isSlider());
        check("retired Search setting field is absent", fieldAbsent(AssistSettings.class, "inventorySearch"));
        check("retired binary camera setting field is absent", fieldAbsent(AssistSettings.class, "noHurtCam"));
    }

    private static void inventoryControllerRetainsOnlyNormalHelpers() {
        boolean guiOpen = false;
        boolean drawPre = false;
        boolean drawPost = false;
        boolean mouse = false;
        boolean retiredInputOrFilter = false;
        for (Method method : InventoryAssistController.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(java.util.Locale.ROOT);
            if ("onguiopen".equals(name)) guiOpen = true;
            if ("ondrawpre".equals(name)) drawPre = true;
            if ("ondrawpost".equals(name)) drawPost = true;
            if ("onmouse".equals(name)) mouse = true;
            if (name.contains("search") || name.contains("filter") || name.contains("keytyped") || name.contains("chartyped")) {
                retiredInputOrFilter = true;
            }
        }
        check("normal inventory lifecycle hooks remain", guiOpen && drawPre && drawPost && mouse);
        check("Inventory Search input/filter hooks are absent", !retiredInputOrFilter);
    }

    private static void normalInventoryHudRemainsRegistered() {
        boolean inventory = false;
        boolean dollars = false;
        for (HudElement element : HudManager.ELEMENTS) {
            if ("inventory".equals(element.id)) inventory = true;
            if ("dollars".equals(element.id)) dollars = true;
        }
        check("normal Inventory HUD remains registered", inventory);
        check("removing Player Dollars does not leave its HUD element", !dollars);
    }

    private static boolean classAbsent(String className) {
        try {
            Class.forName(className, false, RemovedFeatureSurfaceTests.class.getClassLoader());
            return false;
        } catch (ClassNotFoundException expected) {
            return true;
        }
    }

    private static boolean fieldAbsent(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            return field == null;
        } catch (NoSuchFieldException expected) {
            return true;
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }
}

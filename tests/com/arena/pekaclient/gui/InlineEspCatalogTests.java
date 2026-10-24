package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Verifies every former Mob/Door ESP editor value is reachable in the compact inline tables. */
public final class InlineEspCatalogTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        globalAndDoorControlsRemainReachable();
        everyMobStyleHasItsFullValueSet();
        doorStylesKeepTheSharedValueSetWithoutExtraActions();
        nestedSearchOpensTheCorrectTables();
        done();
    }

    private static void globalAndDoorControlsRemainReachable() {
        String[] ids = {
                "mob-esp-enabled", "esp-max-distance", "esp-update-interval", "esp-hide-zero-health", "esp-tracers",
                "esp-mob-labels", "esp-max-mob-labels", "esp-distance-text", "esp-max-boxes", "esp-default-preset",
                "esp-subtle-preset", "esp-reset", "door-esp-enabled", "door-scope", "door-only-key", "door-distance-text",
                "door-default-preset", "door-subtle-preset", "door-reset", "door-blood"
        };
        for (String id : ids) check("ESP option preserved: " + id, ReferenceSettingsCatalog.find(id) != null);
        check("masters remain compact dropdown owners", ReferenceSettingsCatalog.find("mob-esp-enabled").isDropdown()
                && ReferenceSettingsCatalog.find("door-esp-enabled").isDropdown());
    }

    private static void everyMobStyleHasItsFullValueSet() {
        String[] styles = {"esp-starred", "esp-miniboss", "esp-special", "esp-enderman", "esp-skeletor", "esp-other"};
        String[] suffixes = {"", "-color", "-mode", "-walls", "-width", "-outline-opacity", "-fill-opacity", "-copy", "-apply", "-apply-all"};
        for (String style : styles) {
            for (String suffix : suffixes) check("Mob style option preserved: " + style + suffix,
                    ReferenceSettingsCatalog.find(style + suffix) != null);
        }
    }

    private static void doorStylesKeepTheSharedValueSetWithoutExtraActions() {
        String[] suffixes = {"-color", "-mode", "-walls", "-width", "-outline-opacity", "-fill-opacity"};
        for (String suffix : suffixes) check("Door shared style option preserved: " + suffix,
                ReferenceSettingsCatalog.find("door-style" + suffix) != null);
        check("door table does not invent Mob-only clipboard actions", ReferenceSettingsCatalog.find("door-style-copy") == null
                && ReferenceSettingsCatalog.find("door-style-apply") == null
                && ReferenceSettingsCatalog.find("door-style-apply-all") == null);
    }

    private static void nestedSearchOpensTheCorrectTables() {
        ReferenceSettingsCatalog.Match match = null;
        for (ReferenceSettingsCatalog.Match candidate : ReferenceSettingsCatalog.search("starred through walls")) {
            if ("esp-starred-walls".equals(candidate.setting.id)) { match = candidate; break; }
        }
        List<String> parents = new ArrayList<String>();
        if (match != null) for (ReferenceSettingsCatalog.Setting parent : match.parents) parents.add(parent.id);
        check("style search reveals Mob ESP then Starred owner", parents.equals(Arrays.asList("mob-esp-enabled", "esp-starred")));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline ESP catalog: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}

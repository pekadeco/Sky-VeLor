package com.arena.pekaclient.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Verifies the simplified Starred Mobs owner and the retained independent Door ESP values. */
public final class InlineEspCatalogTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        globalAndDoorControlsRemainReachable();
        everyRetainedStarredMobStyleHasDirectControlsOnly();
        retiredMobControlsStayAbsent();
        doorStylesKeepTheSharedValueSetWithoutExtraActions();
        nestedSearchOpensTheCorrectTables();
        done();
    }

    private static void globalAndDoorControlsRemainReachable() {
        String[] ids = {
                "mob-esp-enabled", "esp-max-distance", "esp-update-interval", "esp-mob-labels", "esp-max-mob-labels",
                "esp-distance-text", "esp-max-boxes", "esp-reset", "door-esp-enabled", "door-scope", "door-only-key",
                "door-distance-text", "door-default-preset", "door-subtle-preset", "door-reset", "door-blood",
                "hide-dead-mobs"
        };
        for (String id : ids) check("retained ESP/Comfort option: " + id, ReferenceSettingsCatalog.find(id) != null);
        check("masters remain compact dropdown owners", ReferenceSettingsCatalog.find("mob-esp-enabled").isDropdown()
                && ReferenceSettingsCatalog.find("door-esp-enabled").isDropdown());
        check("visible master name is Starred Mobs", "Starred Mobs".equals(ReferenceSettingsCatalog.find("mob-esp-enabled").label));
        check("Hide Dead Mobs moved to QoL Comfort with body/nameplate wording",
                ReferenceSettingsCatalog.find("hide-dead-mobs").label.equals("Hide Dead Mobs")
                        && ReferenceSettingsCatalog.find("hide-dead-mobs").description.contains("name tags")
                        && ReferenceSettingsCatalog.ownerOf("hide-dead-mobs") == ReferenceSettingsCatalog.Tab.GAMEPLAY);
    }

    private static void everyRetainedStarredMobStyleHasDirectControlsOnly() {
        String[] styles = {"esp-starred", "esp-miniboss", "esp-special", "esp-enderman"};
        String[] suffixes = {"", "-color", "-mode", "-walls", "-width", "-outline-opacity", "-fill-opacity"};
        for (String style : styles) {
            for (String suffix : suffixes) check("Starred Mob style option retained: " + style + suffix,
                    ReferenceSettingsCatalog.find(style + suffix) != null);
        }
    }

    private static void retiredMobControlsStayAbsent() {
        String[] retired = {
                "esp-other", "esp-force-all", "esp-include-bats", "esp-include-invisible", "esp-skeletor",
                "esp-hide-zero-health", "esp-tracers", "esp-default-preset", "esp-subtle-preset"
        };
        for (String id : retired) check("retired ESP control stays absent: " + id, ReferenceSettingsCatalog.find(id) == null);
        String[] styles = {"esp-starred", "esp-miniboss", "esp-special", "esp-enderman", "esp-skeletor"};
        String[] actions = {"-copy", "-apply", "-apply-all"};
        for (String style : styles) for (String action : actions) {
            check("removed style action stays absent: " + style + action,
                    ReferenceSettingsCatalog.find(style + action) == null);
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
        check("style search opens the one flat Starred Mobs owner", parents.equals(Arrays.asList("mob-esp-enabled")));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline ESP catalog: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}

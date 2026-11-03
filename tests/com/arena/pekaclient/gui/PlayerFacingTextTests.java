package com.arena.pekaclient.gui;

import java.util.List;

/** Keeps catalog text short and player-facing rather than exposing implementation terminology. */
public final class PlayerFacingTextTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        namesAndDescriptionsUsePlayerLanguage();
        itemAnimationStatusUsesShortPlayerText();
        done();
    }

    private static void namesAndDescriptionsUsePlayerLanguage() {
        List<String> violations = ReferenceSettingsCatalog.playerTextViolations();
        check("every player-facing catalog name and description is short and plain: " + violations, violations.isEmpty());
    }

    private static void itemAnimationStatusUsesShortPlayerText() {
        String display = com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus.getDisplay();
        check("Item Animation status starts with Hook", display.startsWith("Hook:"));
        check("Item Animation status avoids renderer internals", display.indexOf("SRG") < 0
                && display.indexOf("ItemRenderer") < 0 && display.indexOf("HOOK ACTIVE") < 0);
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("player-facing text: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}

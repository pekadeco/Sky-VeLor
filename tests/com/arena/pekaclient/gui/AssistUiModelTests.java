package com.arena.pekaclient.gui;

import java.util.HashSet;
import java.util.Set;

/** Proves every new control has one owner, a visible label, a gray-description source, and scale-safe row spacing. */
public final class AssistUiModelTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        entriesAreOwnedAndDescribed();
        descriptionRowsDoNotOverlapAtGuiScales();
        done();
    }

    private static void entriesAreOwnedAndDescribed() {
        Set<String> names = new HashSet<String>();
        for (AssistUiModel.Entry entry : AssistUiModel.entries()) {
            check("has tab " + entry.name, entry.tab.length() > 0);
            check("has group " + entry.name, entry.group.length() > 0);
            check("has label " + entry.name, entry.name.length() > 0 && names.add(entry.name));
            check("has one-line description " + entry.name, entry.description.length() >= 12 && entry.description.indexOf('\n') < 0);
        }
        AssistUiModel.Entry noHurtCam = AssistUiModel.find("No Hurt Cam");
        check("new areas are represented", AssistUiModel.find("Enable Dungeon Sync") != null
                && AssistUiModel.find("Mining Assistant HUD") != null && AssistUiModel.find("Item Protection") != null
                && noHurtCam != null && "Simple features".equals(noHurtCam.group));
    }

    private static void descriptionRowsDoNotOverlapAtGuiScales() {
        for (float scale : new float[]{.5F, 1F, 2F, 4F}) {
            float y = 0F;
            for (AssistUiModel.Entry ignored : AssistUiModel.entries()) {
                float titleTop = y + 5F * scale;
                float descriptionTop = y + 19F * scale;
                float nextRow = y + 40F * scale + GuiLayoutToolkit.GAP * scale;
                check("description below title scale " + scale, descriptionTop >= titleTop + 9F * scale);
                check("next row below description scale " + scale, nextRow >= descriptionTop + 9F * scale);
                y = nextRow;
            }
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("ok   " + name); }
        else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("assist UI model: passed " + passed + ", failed " + failed);
        if (failed != 0) throw new AssertionError(failed + " assist UI checks failed");
    }
}

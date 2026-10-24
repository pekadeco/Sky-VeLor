package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.DungeonEspSettings;

/** Action-level regression checks for inline settings that deliberately carry no new persisted state. */
public final class InlineSettingsBindingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        espStyleClipboardRemainsSessionOnly();
        unknownActionsStayUnclaimed();
        done();
    }

    private static void espStyleClipboardRemainsSessionOnly() {
        DungeonEspSettings.Style source = DungeonEspSettings.starred;
        DungeonEspSettings.Style destination = DungeonEspSettings.miniboss;
        int sourceColor = source.color;
        int destinationColor = destination.color;
        boolean sourceEnabled = source.enabled;
        boolean destinationEnabled = destination.enabled;
        int sourceMode = source.mode;
        int destinationMode = destination.mode;
        boolean sourceWalls = source.throughWalls;
        boolean destinationWalls = destination.throughWalls;
        int sourceWidth = source.outlineWidth;
        int destinationWidth = destination.outlineWidth;
        int sourceOutline = source.outlineOpacity;
        int destinationOutline = destination.outlineOpacity;
        int sourceFill = source.fillOpacity;
        int destinationFill = destination.fillOpacity;
        try {
            source.color = DungeonEspSettings.colorForIndex(6);
            source.throughWalls = !sourceWalls;
            destination.color = DungeonEspSettings.colorForIndex(1);
            destination.throughWalls = sourceWalls;
            check("copy action is handled", InlineSettingsBindings.runAction("esp-starred-copy"));
            check("apply action is handled", InlineSettingsBindings.runAction("esp-miniboss-apply"));
            check("copied color reaches existing destination Style", destination.color == source.color);
            check("copied through-walls reaches existing destination Style", destination.throughWalls == source.throughWalls);
        } finally {
            source.color = sourceColor;
            destination.color = destinationColor;
            source.enabled = sourceEnabled;
            destination.enabled = destinationEnabled;
            source.mode = sourceMode;
            destination.mode = destinationMode;
            source.throughWalls = sourceWalls;
            destination.throughWalls = destinationWalls;
            source.outlineWidth = sourceWidth;
            destination.outlineWidth = destinationWidth;
            source.outlineOpacity = sourceOutline;
            destination.outlineOpacity = destinationOutline;
            source.fillOpacity = sourceFill;
            destination.fillOpacity = destinationFill;
        }
    }

    private static void unknownActionsStayUnclaimed() {
        check("unknown action does not manufacture a feature path", !InlineSettingsBindings.runAction("not-a-setting"));
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline actions: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}

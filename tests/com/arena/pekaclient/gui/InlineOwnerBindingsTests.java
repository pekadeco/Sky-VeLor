package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;

/** Proves catalog rows continue reading/writing their original configuration owners. */
public final class InlineOwnerBindingsTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        booleanOwnersRemainDirect();
        numericAndChoiceOwnersRemainDirect();
        espStyleOwnersIncludeThroughWalls();
        done();
    }

    private static void booleanOwnersRemainDirect() {
        boolean oldParticles = AppleSettings.noBlockParticles;
        boolean oldSprint = AssistSettings.autoSprint;
        boolean oldAspectTeleport = AssistSettings.smoothAspectTeleport;
        boolean oldHideDead = AssistSettings.hideDeadMobs;
        boolean oldMap = AppleSettings.dungeonMap;
        boolean oldChat = ChatQolSettings.enabled;
        try {
            InlineSettingsBindings.setBoolean("no-block-particles", !oldParticles);
            InlineSettingsBindings.setBoolean("auto-sprint", !oldSprint);
            InlineSettingsBindings.setBoolean("smooth-aspect-teleport", !oldAspectTeleport);
            InlineSettingsBindings.setBoolean("hide-dead-mobs", !oldHideDead);
            InlineSettingsBindings.setBoolean("dungeon-map", !oldMap);
            InlineSettingsBindings.setBoolean("chat-qol", !oldChat);
            check("particle row writes AppleSettings owner", AppleSettings.noBlockParticles == !oldParticles
                    && InlineSettingsBindings.booleanValue("no-block-particles") == !oldParticles);
            check("sprint row writes AssistSettings owner", AssistSettings.autoSprint == !oldSprint
                    && InlineSettingsBindings.booleanValue("auto-sprint") == !oldSprint);
            check("Smooth Aspect Teleport writes its AssistSettings visual owner", AssistSettings.smoothAspectTeleport == !oldAspectTeleport
                    && InlineSettingsBindings.booleanValue("smooth-aspect-teleport") == !oldAspectTeleport);
            check("Hide Dead Mobs writes the Player Comfort AssistSettings owner", AssistSettings.hideDeadMobs == !oldHideDead
                    && InlineSettingsBindings.booleanValue("hide-dead-mobs") == !oldHideDead);
            check("map row writes original dungeon-map owner", AppleSettings.dungeonMap == !oldMap
                    && InlineSettingsBindings.booleanValue("dungeon-map") == !oldMap);
            check("Chat master writes ChatQolSettings owner", ChatQolSettings.enabled == !oldChat
                    && InlineSettingsBindings.booleanValue("chat-qol") == !oldChat);
        } finally {
            AppleSettings.noBlockParticles = oldParticles;
            AssistSettings.autoSprint = oldSprint;
            AssistSettings.smoothAspectTeleport = oldAspectTeleport;
            AssistSettings.hideDeadMobs = oldHideDead;
            AppleSettings.dungeonMap = oldMap;
            ChatQolSettings.enabled = oldChat;
        }
    }

    private static void numericAndChoiceOwnersRemainDirect() {
        int oldShake = AssistSettings.hurtCameraShake;
        int oldScale = AppleSettings.dungeonMapScale;
        int oldMenu = ChatQolSettings.menuOpenMode;
        try {
            InlineSettingsBindings.setInt("hurt-camera-shake", 73);
            InlineSettingsBindings.setInt("dungeon-map-scale", 145);
            InlineSettingsBindings.setChoice("chat-menu-mode", 1);
            check("hurt camera slider keeps its AssistSettings owner", AssistSettings.hurtCameraShake == 73
                    && InlineSettingsBindings.intValue("hurt-camera-shake") == 73);
            check("map scale slider keeps its AppleSettings owner", AppleSettings.dungeonMapScale == 145
                    && InlineSettingsBindings.intValue("dungeon-map-scale") == 145);
            check("menu choice still exposes the existing menu owner", InlineSettingsBindings.choiceValue("chat-menu-mode") == 1);
        } finally {
            AssistSettings.hurtCameraShake = oldShake;
            AppleSettings.dungeonMapScale = oldScale;
            ChatQolSettings.menuOpenMode = oldMenu;
        }
    }

    private static void espStyleOwnersIncludeThroughWalls() {
        DungeonEspSettings.Style style = DungeonEspSettings.starred;
        boolean oldWalls = style.throughWalls;
        int oldWidth = style.outlineWidth;
        try {
            InlineSettingsBindings.setBoolean("esp-starred-walls", !oldWalls);
            InlineSettingsBindings.setInt("esp-starred-width", 4);
            check("style through-walls toggle uses the existing Style field", style.throughWalls == !oldWalls
                    && InlineSettingsBindings.booleanValue("esp-starred-walls") == !oldWalls);
            check("style width slider uses the existing Style field", style.outlineWidth == 4
                    && InlineSettingsBindings.intValue("esp-starred-width") == 4);
        } finally {
            style.throughWalls = oldWalls;
            style.outlineWidth = oldWidth;
        }
    }

    private static void check(String name, boolean okay) {
        if (okay) passed++; else { failed++; System.out.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("inline owner bindings: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}

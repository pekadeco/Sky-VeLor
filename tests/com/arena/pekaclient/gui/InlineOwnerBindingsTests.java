package com.arena.pekaclient.gui;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.ChatQolSettings;
import com.arena.pekaclient.config.DungeonEspSettings;
import com.arena.pekaclient.hud.HudManager;

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
        boolean oldPlayerHeads = AppleSettings.dungeonMapPlayerHeads;
        boolean oldChat = ChatQolSettings.enabled;
        boolean oldSpiritBowLocator = AssistSettings.spiritBowLocator;
        boolean oldDungeonRunHud = HudManager.DUNGEON_RUN.enabled;
        try {
            InlineSettingsBindings.setBoolean("no-block-particles", !oldParticles);
            InlineSettingsBindings.setBoolean("auto-sprint", !oldSprint);
            InlineSettingsBindings.setBoolean("smooth-aspect-teleport", !oldAspectTeleport);
            InlineSettingsBindings.setBoolean("hide-dead-mobs", !oldHideDead);
            InlineSettingsBindings.setBoolean("dungeon-map", !oldMap);
            InlineSettingsBindings.setBoolean("dungeon-map-player-heads", !oldPlayerHeads);
            InlineSettingsBindings.setBoolean("chat-qol", !oldChat);
            InlineSettingsBindings.setBoolean("spirit-bow-locator", !oldSpiritBowLocator);
            InlineSettingsBindings.setBoolean("hud-dungeon-run", !oldDungeonRunHud);
            check("particle row writes AppleSettings owner", AppleSettings.noBlockParticles == !oldParticles
                    && InlineSettingsBindings.booleanValue("no-block-particles") == !oldParticles);
            check("sprint row writes AssistSettings owner", AssistSettings.autoSprint == !oldSprint
                    && InlineSettingsBindings.booleanValue("auto-sprint") == !oldSprint);
            check("Smooth Aspect Teleport writes its AssistSettings visual owner", AssistSettings.smoothAspectTeleport == !oldAspectTeleport
                    && InlineSettingsBindings.booleanValue("smooth-aspect-teleport") == !oldAspectTeleport);
            check("Hide Dead Mobs writes the QoL Comfort AssistSettings owner", AssistSettings.hideDeadMobs == !oldHideDead
                    && InlineSettingsBindings.booleanValue("hide-dead-mobs") == !oldHideDead);
            check("map row writes original dungeon-map owner", AppleSettings.dungeonMap == !oldMap
                    && InlineSettingsBindings.booleanValue("dungeon-map") == !oldMap);
            check("Player Heads row writes the persisted dungeon-map owner", AppleSettings.dungeonMapPlayerHeads == !oldPlayerHeads
                    && InlineSettingsBindings.booleanValue("dungeon-map-player-heads") == !oldPlayerHeads);
            check("Chat master writes ChatQolSettings owner", ChatQolSettings.enabled == !oldChat
                    && InlineSettingsBindings.booleanValue("chat-qol") == !oldChat);
            check("Spirit Bow Locator writes its isolated AssistSettings owner", AssistSettings.spiritBowLocator == !oldSpiritBowLocator
                    && InlineSettingsBindings.booleanValue("spirit-bow-locator") == !oldSpiritBowLocator);
            check("Dungeon Run HUD writes its movable HUD owner", HudManager.DUNGEON_RUN.enabled == !oldDungeonRunHud
                    && InlineSettingsBindings.booleanValue("hud-dungeon-run") == !oldDungeonRunHud);
        } finally {
            AppleSettings.noBlockParticles = oldParticles;
            AssistSettings.autoSprint = oldSprint;
            AssistSettings.smoothAspectTeleport = oldAspectTeleport;
            AssistSettings.hideDeadMobs = oldHideDead;
            AppleSettings.dungeonMap = oldMap;
            AppleSettings.dungeonMapPlayerHeads = oldPlayerHeads;
            ChatQolSettings.enabled = oldChat;
            AssistSettings.spiritBowLocator = oldSpiritBowLocator;
            HudManager.DUNGEON_RUN.enabled = oldDungeonRunHud;
        }
    }

    private static void numericAndChoiceOwnersRemainDirect() {
        int oldShake = AssistSettings.hurtCameraShake;
        int oldScale = AppleSettings.dungeonMapScale;
        int oldZoom = AppleSettings.dungeonMapZoom;
        int oldHeadSize = AppleSettings.dungeonMapPlayerHeadSize;
        int oldMenu = ChatQolSettings.menuOpenMode;
        try {
            InlineSettingsBindings.setInt("hurt-camera-shake", 73);
            InlineSettingsBindings.setInt("dungeon-map-scale", 145);
            InlineSettingsBindings.setInt("dungeon-map-zoom", 500);
            InlineSettingsBindings.setInt("dungeon-map-player-head-size", 14);
            InlineSettingsBindings.setChoice("chat-menu-mode", 1);
            check("hurt camera slider keeps its AssistSettings owner", AssistSettings.hurtCameraShake == 73
                    && InlineSettingsBindings.intValue("hurt-camera-shake") == 73);
            check("map scale slider keeps its AppleSettings owner", AppleSettings.dungeonMapScale == 145
                    && InlineSettingsBindings.intValue("dungeon-map-scale") == 145);
            check("map zoom accepts the new 500 percent maximum", AppleSettings.dungeonMapZoom == 500
                    && InlineSettingsBindings.intValue("dungeon-map-zoom") == 500);
            check("Player Head Size slider keeps its AppleSettings owner", AppleSettings.dungeonMapPlayerHeadSize == 14
                    && InlineSettingsBindings.intValue("dungeon-map-player-head-size") == 14);
            check("menu choice still exposes the existing menu owner", InlineSettingsBindings.choiceValue("chat-menu-mode") == 1);
        } finally {
            AssistSettings.hurtCameraShake = oldShake;
            AppleSettings.dungeonMapScale = oldScale;
            AppleSettings.dungeonMapZoom = oldZoom;
            AppleSettings.dungeonMapPlayerHeadSize = oldHeadSize;
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

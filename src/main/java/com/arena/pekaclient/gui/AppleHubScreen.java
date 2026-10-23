package com.arena.pekaclient.gui;

import net.minecraft.client.gui.GuiScreen;

/**
 * Compatibility name for integrations compiled against the pre-reference Hub.
 *
 * <p>No visible entry point constructs this class: keyboard, command and menu routes open
 * {@link AppleSettingsScreen}.  Keeping this thin alias avoids breaking a caller that still knows the old class
 * name without keeping the retired seven-section expandable surface alive.</p>
 */
@Deprecated
public final class AppleHubScreen extends AppleSettingsScreen {

    public AppleHubScreen(GuiScreen parent) {
        super(parent);
    }

    private AppleHubScreen(GuiScreen parent, ReferenceSettingsCatalog.Tab initialTab) {
        super(parent, initialTab);
    }

    /** Historical deep link now opens the Dungeon destination in the reference rail. */
    public static AppleHubScreen dungeonHud(GuiScreen parent) {
        return new AppleHubScreen(parent, ReferenceSettingsCatalog.Tab.DUNGEON);
    }
}

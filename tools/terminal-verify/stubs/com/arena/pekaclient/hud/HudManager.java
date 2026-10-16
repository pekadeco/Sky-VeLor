package com.arena.pekaclient.hud;

/** Stand-in for the real HUD manager: only the members TerminalSolver uses. */
public class HudManager {
    public static final ClickInOrderHud CLICK_IN_ORDER = new ClickInOrderHud();

    public static boolean visibleInGame() { return false; }
    public static void render(net.minecraft.client.gui.ScaledResolution resolution) { }
}

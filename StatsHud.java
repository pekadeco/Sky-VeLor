package com.arena.pekaclient.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.Properties;

/**
 * FPS and ping HUD. Both values are read on the client: the frame rate from the game, and the ping from the
 * player's own tab-list entry. Off by default, so it changes nothing until it is turned on in the HUD editor.
 *
 * TPS is not shown. The client keeps its own world clock and only corrects it from the server now and then, so a
 * TPS read from the client would not reflect the server's real tick rate.
 */
public final class StatsHud extends HudElement {

    public boolean showFps = true;
    public boolean showPing = true;

    public StatsHud() {
        super("stats", "FPS and ping",
                new HudPlacement(HudPlacement.Anchor.END, 6, HudPlacement.Anchor.START, 40), false);
    }

    /** The current line. Reads the game state, so it is called once per frame at most. */
    private String statsText() {
        return StatsText.line(showFps, Minecraft.getDebugFPS(), showPing, pingMs());
    }

    /** The player's ping in ms from the tab-list entry, or -1 when it is not known yet. */
    static int pingMs() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return -1;
        }
        NetHandlerPlayClient net = mc.getNetHandler();
        if (net == null) {
            return -1;
        }
        NetworkPlayerInfo info = net.getPlayerInfo(mc.thePlayer.getName());
        if (info == null || info.getResponseTime() < 0) {
            return -1;
        }
        return info.getResponseTime();
    }

    @Override
    public float width() {
        String s = statsText();
        return 10f + textWidth(s) + 10f;
    }

    @Override
    public float height() {
        return 9f * textScale() + 12f;
    }

    @Override
    protected void drawContent() {
        float w = width();
        float h = height();
        drawPanel(w, h);
        float ty = h / 2f - 4.5f * textScale();
        text(statsText(), 10f, ty, accentColor());
    }

    @Override
    protected void resetExtra() {
        showFps = true;
        showPing = true;
    }

    @Override
    protected void loadExtra(Properties p, String k) {
        showFps = bool(p, k + "fps", true);
        showPing = bool(p, k + "ping", true);
    }

    @Override
    protected void saveExtra(Properties p, String k) {
        p.setProperty(k + "fps", String.valueOf(showFps));
        p.setProperty(k + "ping", String.valueOf(showPing));
    }
}

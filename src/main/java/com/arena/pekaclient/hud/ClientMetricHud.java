package com.arena.pekaclient.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

/**
 * One independently positioned client metric display. The text refreshes at most once per second; render frames reuse
 * the cached string so FPS, tab-list ping, and heap reads do not manufacture per-frame strings or collections.
 */
public final class ClientMetricHud extends HudElement {
    public enum Metric { FPS, PING, MEMORY }

    private static final long REFRESH_MILLIS = 1000L;

    private final Metric metric;
    private long refreshAt;
    private String cachedText;

    public ClientMetricHud(String id, String name, HudPlacement placement, Metric metric) {
        super(id, name, placement, false);
        this.metric = metric == null ? Metric.FPS : metric;
        this.cachedText = placeholder(this.metric);
    }

    @Override
    public float width() {
        return 10f + textWidth(displayText()) + 10f;
    }

    @Override
    public float height() {
        return 9f * textScale() + 12f;
    }

    @Override
    protected void drawContent() {
        String value = displayText();
        float w = 10f + textWidth(value) + 10f;
        float h = height();
        drawPanel(w, h);
        text(value, 10f, h * .5f - 4.5f * textScale(), accentColor());
    }

    private String displayText() {
        long now = System.currentTimeMillis();
        if (now >= refreshAt) {
            cachedText = readText();
            refreshAt = now + REFRESH_MILLIS;
        }
        return cachedText;
    }

    private String readText() {
        if (metric == Metric.FPS) return ClientMetricText.fps(Minecraft.getDebugFPS());
        if (metric == Metric.PING) return ClientMetricText.ping(ownPingMs());
        Runtime runtime = Runtime.getRuntime();
        return ClientMetricText.memory(runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory());
    }

    /** Player-only tab-list response time, never a server TPS estimate. */
    static int ownPingMs() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return -1;
        NetHandlerPlayClient handler = mc.getNetHandler();
        if (handler == null) return -1;
        NetworkPlayerInfo info = handler.getPlayerInfo(mc.thePlayer.getName());
        return info == null || info.getResponseTime() < 0 ? -1 : info.getResponseTime();
    }

    private static String placeholder(Metric metric) {
        if (metric == Metric.PING) return ClientMetricText.ping(-1);
        if (metric == Metric.MEMORY) return ClientMetricText.memory(-1L, -1L);
        return ClientMetricText.fps(-1);
    }
}

package com.arena.pekaclient.feature;

import java.util.List;

/**
 * Geometry authority for the one attached default-chat tab strip. The strip is always immediately above the real
 * GuiChat input field, so it cannot cover rendered messages or steal the field's text area.
 */
public final class ChatTabLayout {
    public static final float HEIGHT = 18f;
    public static final float GAP = 2f;
    public static final float LEFT = 2f;

    private ChatTabLayout() {
    }

    public static final class Rect {
        public String id = "";
        public float x;
        public float y;
        public float w;
        public float h;

        public boolean contains(float px, float py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        void set(String nextId, float nextX, float nextY, float nextW, float nextH) {
            id = nextId == null ? "" : nextId;
            x = nextX;
            y = nextY;
            w = Math.max(0f, nextW);
            h = Math.max(0f, nextH);
        }
    }

    public static final class Layout {
        public final Rect[] tabs = new Rect[ChatTabs.MAX_TABS];
        public final Rect bounds = new Rect();
        public int count;

        public Layout() {
            for (int i = 0; i < tabs.length; i++) tabs[i] = new Rect();
        }

        public Rect at(String id) {
            for (int i = 0; i < count; i++) if (tabs[i].id.equals(id)) return tabs[i];
            return null;
        }
    }

    /**
     * Uses compact even widths instead of text-dependent raw widths so eight custom labels cannot run past a small
     * screen. The renderer ellipsizes labels inside each returned rectangle using Minecraft's native font.
     */
    public static void update(Layout out, float screenWidth, float inputTop, List<ChatTabs.Tab> tabs) {
        if (out == null) return;
        int count = 0;
        if (tabs != null) for (ChatTabs.Tab tab : tabs) if (tab != null && tab.enabled() && count < out.tabs.length) count++;
        float available = Math.max(1f, screenWidth - LEFT * 2f - Math.max(0, count - 1) * GAP);
        float preferred = count <= 3 ? 58f : count <= 5 ? 51f : 46f;
        float width = count == 0 ? 0f : Math.max(18f, Math.min(preferred, available / count));
        // If even the compact width does not fit, keep every tab reachable rather than drawing off-screen.
        if (count > 0 && width * count + (count - 1) * GAP > available) width = Math.max(1f, available / count);
        float y = Math.max(0f, inputTop - HEIGHT - 2f);
        float x = LEFT;
        int slot = 0;
        if (tabs != null) {
            for (ChatTabs.Tab tab : tabs) {
                if (tab == null || !tab.enabled() || slot >= out.tabs.length) continue;
                out.tabs[slot++].set(tab.id(), x, y, width, HEIGHT);
                x += width + GAP;
            }
        }
        out.count = slot;
        for (int i = slot; i < out.tabs.length; i++) out.tabs[i].set("", 0f, 0f, 0f, 0f);
        out.bounds.set("tabs", LEFT, y, Math.max(0f, x - LEFT - (slot == 0 ? 0f : GAP)), HEIGHT);
    }
}

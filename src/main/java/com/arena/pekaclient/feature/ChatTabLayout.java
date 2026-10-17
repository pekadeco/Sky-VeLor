package com.arena.pekaclient.feature;

/**
 * Pure attached-tab geometry for GuiChat. Tabs either occupy reserved rows directly above the input field or are
 * hidden when a tiny GUI cannot fit them; they are never drawn over typed text. The live handler caches this result
 * until GUI dimensions, visibility or order changes.
 */
public final class ChatTabLayout {

    public static final int INPUT_INSET = 2;
    public static final int INPUT_HEIGHT = 12;
    public static final int TAB_HEIGHT = 14;
    public static final int GAP = 1;
    public static final int SAFE_TOP = 2;

    public static final class Rect {
        public int x;
        public int y;
        public int w;
        public int h;
        public boolean visible;

        public boolean contains(int px, int py) {
            return visible && px >= x && py >= y && px < x + w && py < y + h;
        }

        public boolean intersects(Rect other) {
            return visible && other != null && other.visible && x < other.x + other.w && x + w > other.x
                    && y < other.y + other.h && y + h > other.y;
        }
    }

    public static final class Layout {
        public final Rect input = new Rect();
        public final Rect[] tabs = {new Rect(), new Rect(), new Rect()};
        public int rows;
        public boolean shown;
    }

    private ChatTabLayout() {
    }

    /**
     * Updates the supplied reusable layout. orderedTabs uses ChatFilterRules All/Party/Reply values and visibleTabs
     * has matching indexes. Invalid/duplicate order entries are safely ignored.
     */
    public static void update(Layout out, int screenW, int screenH, int[] orderedTabs, boolean[] visibleTabs) {
        int width = Math.max(0, screenW);
        int height = Math.max(0, screenH);
        out.input.x = INPUT_INSET;
        out.input.y = Math.max(0, height - 14);
        out.input.w = Math.max(0, width - INPUT_INSET * 2);
        out.input.h = Math.min(INPUT_HEIGHT, Math.max(0, height - out.input.y));
        out.input.visible = out.input.w > 0 && out.input.h > 0;
        out.rows = 0;
        out.shown = false;
        for (Rect rect : out.tabs) rect.visible = false;
        if (out.input.w < 24 || out.input.y <= SAFE_TOP || orderedTabs == null || visibleTabs == null) return;

        int[] sequence = new int[3];
        int count = 0;
        for (int candidate : orderedTabs) {
            if (candidate < ChatFilterRules.ALL || candidate > ChatFilterRules.REPLY || candidate >= visibleTabs.length
                    || !visibleTabs[candidate]) continue;
            boolean duplicate = false;
            for (int i = 0; i < count; i++) if (sequence[i] == candidate) duplicate = true;
            if (!duplicate && count < sequence.length) sequence[count++] = candidate;
        }
        if (count == 0) return;

        int[] widths = new int[3];
        int current = 0;
        int rows = 1;
        for (int i = 0; i < count; i++) {
            int tab = sequence[i];
            int wanted = preferredWidth(tab);
            // On narrow screens compact labels can shrink, but never so far that hit targets overlap.
            wanted = Math.max(18, Math.min(wanted, out.input.w));
            if (current > 0 && current + GAP + wanted > out.input.w) {
                rows++;
                current = 0;
            }
            widths[tab] = wanted;
            current += (current == 0 ? 0 : GAP) + wanted;
        }
        int top = out.input.y - GAP - rows * TAB_HEIGHT - (rows - 1) * GAP;
        if (top < SAFE_TOP) return;

        int row = 0;
        int x = out.input.x;
        for (int i = 0; i < count; i++) {
            int tab = sequence[i];
            int wanted = widths[tab];
            if (x != out.input.x && x + GAP + wanted > out.input.x + out.input.w) {
                row++;
                x = out.input.x;
            }
            if (x != out.input.x) x += GAP;
            Rect rect = out.tabs[tab];
            rect.x = x;
            rect.y = top + row * (TAB_HEIGHT + GAP);
            rect.w = wanted;
            rect.h = TAB_HEIGHT;
            rect.visible = true;
            x += wanted;
        }
        out.rows = rows;
        out.shown = true;
    }

    public static int preferredWidth(int tab) {
        return tab == ChatFilterRules.PARTY ? 42 : tab == ChatFilterRules.REPLY ? 43 : 29;
    }

    public static String label(int tab) {
        return tab == ChatFilterRules.PARTY ? "Party" : tab == ChatFilterRules.REPLY ? "Reply" : "All";
    }
}

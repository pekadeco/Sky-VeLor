package com.arena.pekaclient.notes;

import com.arena.pekaclient.gui.GuiLayoutToolkit;

/**
 * Shared-layout-backed geometry for the quick-note screen. The compact form deliberately trades the searchable
 * page rail for the existing Prev/Next controls instead of letting a fixed sidebar overlap the editor on Small,
 * Large or Auto GUI scales.
 */
public final class QuickNoteLayout {
    public static final class Metrics {
        public final GuiLayoutToolkit.Rect panel = new GuiLayoutToolkit.Rect();
        public final GuiLayoutToolkit.Rect sidebar = new GuiLayoutToolkit.Rect();
        public final GuiLayoutToolkit.Rect heading = new GuiLayoutToolkit.Rect();
        public final GuiLayoutToolkit.Rect note = new GuiLayoutToolkit.Rect();
        public final GuiLayoutToolkit.Rect footer = new GuiLayoutToolkit.Rect();
        public boolean compact;
        public boolean showSidebar;
    }

    private QuickNoteLayout() {
    }

    /** Recomputes every surface from the scaled viewport, keeping all rectangles positive and on-screen. */
    public static void update(Metrics out, float viewportWidth, float viewportHeight) {
        if (out == null) {
            return;
        }
        float wantedW = Math.min(568f, Math.max(160f, viewportWidth - 20f));
        float wantedH = Math.min(252f, Math.max(120f, viewportHeight - 40f));
        GuiLayoutToolkit.clampPanel(out.panel, (viewportWidth - wantedW) * .5f,
                (viewportHeight - wantedH) * .5f, wantedW, wantedH, viewportWidth, viewportHeight);

        out.compact = out.panel.w < 390f || out.panel.h < 180f;
        float inset = out.compact ? 6f : 10f;
        float footerHeight = out.compact ? 22f : 44f;
        out.footer.set(out.panel.x + inset, out.panel.bottom() - footerHeight,
                Math.max(1f, out.panel.w - 2f * inset), footerHeight);

        if (out.compact) {
            out.showSidebar = false;
            out.sidebar.set(0f, 0f, 0f, 0f);
            out.heading.set(out.panel.x + inset, out.panel.y + 25f,
                    Math.max(1f, out.panel.w - 2f * inset), 14f);
            float noteY = out.heading.bottom() + 4f;
            out.note.set(out.panel.x + inset, noteY, Math.max(1f, out.panel.w - 2f * inset),
                    Math.max(1f, out.footer.y - noteY - 2f));
            return;
        }

        out.showSidebar = true;
        float sidebarW = Math.min(132f, Math.max(72f, out.panel.w * .30f));
        out.sidebar.set(out.panel.x + inset, out.panel.y + 42f, sidebarW,
                Math.max(1f, out.footer.y - (out.panel.y + 42f)));
        float contentX = out.sidebar.right() + 10f;
        float contentW = Math.max(1f, out.panel.right() - inset - contentX);
        out.heading.set(contentX + 58f, out.panel.y + 26f, Math.max(1f, contentW - 58f), 14f);
        float noteY = out.panel.y + 46f;
        out.note.set(contentX, noteY, contentW, Math.max(1f, out.footer.y - noteY - 2f));
    }
}

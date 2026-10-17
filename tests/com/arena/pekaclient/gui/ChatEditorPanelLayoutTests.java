package com.arena.pekaclient.gui;

/** Headless drag/clamp/per-scale/reset proof for Chat Editor setting panels. */
public final class ChatEditorPanelLayoutTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        draggableClampedAndResettable();
        collapsedPersistsPerScale();
        System.out.println("chat editor panel layout: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void draggableClampedAndResettable() {
        ChatEditorPanelLayout.State state = new ChatEditorPanelLayout.State(2500, 1200);
        ChatEditorPanelLayout.Rect rect = new ChatEditorPanelLayout.Rect();
        ChatEditorPanelLayout.resolve(rect, state, 2, 300, 180, 800, 450);
        float originalX = rect.x;
        float originalY = rect.y;
        state.move(2, 9999, -500, 300, 180, 800, 450);
        ChatEditorPanelLayout.resolve(rect, state, 2, 300, 180, 800, 450);
        check("drag clamps right edge", rect.x >= 0 && rect.x + rect.w <= 800.01f);
        check("drag clamps top edge", rect.y >= 0 && rect.y + rect.h <= 450.01f);
        state.resetScale(2);
        ChatEditorPanelLayout.resolve(rect, state, 2, 300, 180, 800, 450);
        check("double-click reset restores this scale default", Math.abs(rect.x - originalX) < 0.01f
                && Math.abs(rect.y - originalY) < 0.01f);
    }

    private static void collapsedPersistsPerScale() {
        ChatEditorPanelLayout.State state = new ChatEditorPanelLayout.State(0, 0);
        state.set(1, 100, 200, true);
        state.set(3, 700, 800, false);
        check("collapse state is GUI-scale-specific", state.collapsed(1) && !state.collapsed(3));
        check("position is GUI-scale-specific", state.x(1) == 100 && state.y(1) == 200
                && state.x(3) == 700 && state.y(3) == 800);
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}

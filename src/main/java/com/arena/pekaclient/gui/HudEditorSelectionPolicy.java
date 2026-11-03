package com.arena.pekaclient.gui;

/**
 * Pure click-selection order for on-screen HUD elements. Candidates are stored in draw order, so the highest index is
 * visually on top. Repeated clicks cycle through overlapping candidates instead of leaving a dimmed/covered element
 * permanently unreachable now that the editor has no target list.
 */
public final class HudEditorSelectionPolicy {
    private HudEditorSelectionPolicy() { }

    /**
     * @param selectedIndex current selected element index, or -1 when the current target is not a HUD element
     * @param hits          hit candidates in draw order; disabled placeholders participate exactly like enabled ones
     * @return next selected candidate, or -1 when nothing was hit
     */
    public static int nextIndex(int selectedIndex, boolean[] hits) {
        if (hits == null || hits.length == 0) return -1;
        int topmost = -1;
        boolean afterSelected = false;
        for (int i = hits.length - 1; i >= 0; i--) {
            if (!hits[i]) continue;
            if (topmost < 0) topmost = i;
            if (afterSelected) return i;
            if (i == selectedIndex) afterSelected = true;
        }
        return topmost;
    }
}

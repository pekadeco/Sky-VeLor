package com.arena.pekaclient.assist.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure fixed-height tooltip windowing; callers keep the source tooltip untouched until rendering time. */
public final class TooltipWindow {
    private TooltipWindow() {
    }

    public static int maxOffset(int lineCount, int visibleLines) {
        return Math.max(0, lineCount - Math.max(1, visibleLines));
    }

    public static int clampOffset(int requested, int lineCount, int visibleLines) {
        return Math.max(0, Math.min(maxOffset(lineCount, visibleLines), requested));
    }

    public static List<String> visible(List<String> lines, int offset, int visibleLines) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        int start = clampOffset(offset, lines.size(), visibleLines);
        int end = Math.min(lines.size(), start + Math.max(1, visibleLines));
        return new ArrayList<String>(lines.subList(start, end));
    }
}

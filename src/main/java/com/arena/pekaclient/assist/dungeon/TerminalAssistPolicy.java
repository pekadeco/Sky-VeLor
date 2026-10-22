package com.arena.pekaclient.assist.dungeon;

import java.util.Locale;

/** Title recognition and safety gates for terminal assistance. Parsing failures always leave vanilla input untouched. */
public final class TerminalAssistPolicy {
    public static final int MIN_AUTO_CLICK_DELAY_MS = 150;

    private TerminalAssistPolicy() {
    }

    public static String kindForTitle(String title) {
        String value = plain(title);
        if (value.indexOf("click in order") >= 0) return "click-in-order";
        if (value.indexOf("what starts with") >= 0) return "starts-with";
        if (value.indexOf("select all the") >= 0) return "select-colors";
        if (value.indexOf("correct all the panes") >= 0) return "correct-panes";
        if (value.indexOf("navigate the maze") >= 0 || value.indexOf("complete the maze") >= 0) return "maze";
        return "";
    }

    /** Automation is an explicit second opt-in and is never selected for unknown/ambiguous terminal titles. */
    public static boolean allowAutoClick(boolean hintsEnabled, boolean autoClickEnabled, String kind,
                                         long nowMillis, long lastClickMillis) {
        return hintsEnabled && autoClickEnabled && kind != null && kind.length() > 0
                && (lastClickMillis <= 0L || nowMillis - lastClickMillis >= MIN_AUTO_CLICK_DELAY_MS);
    }

    public static boolean shouldHideIncorrect(boolean hintsEnabled, boolean enabled, String kind) {
        return hintsEnabled && enabled && ("correct-panes".equals(kind) || "select-colors".equals(kind));
    }

    private static String plain(String title) {
        if (title == null) return "";
        StringBuilder out = new StringBuilder(Math.min(title.length(), 128));
        for (int i = 0; i < title.length() && out.length() < 128; i++) {
            char c = title.charAt(i);
            if (c == '\u00a7' && i + 1 < title.length()) { i++; continue; }
            out.append(c);
        }
        return out.toString().trim().toLowerCase(Locale.ROOT);
    }
}

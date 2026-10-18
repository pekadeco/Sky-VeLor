package com.arena.pekaclient.chat;

/** Pure mouse-policy contract for the two-action chat context menu. */
public final class ChatMenuPolicy {

    /** Retained only when migrating an old explicit menu=false profile; new UI choices never expose it. */
    public static final int DISABLED = -1;
    public static final int RIGHT = 0;
    public static final int LEFT = 1;
    public static final int BOTH = 2;

    private ChatMenuPolicy() {
    }

    public static int normalize(int value) {
        return value == LEFT || value == BOTH ? value : RIGHT;
    }

    public static boolean isEnabled(int mode) {
        return mode != DISABLED;
    }

    public static boolean allowsRight(int mode) {
        return mode == RIGHT || mode == BOTH;
    }

    public static boolean allowsLeft(int mode) {
        return mode == LEFT || mode == BOTH;
    }

    /**
     * Right click is always menu-only. Left click is menu-only on non-clickable text/row space; a native ClickEvent
     * remains the owner of a left click so commands, suggest actions and links continue to work.
     */
    public static boolean opens(int mode, int button, boolean componentHasClickEvent) {
        if (!isEnabled(mode)) return false;
        if (button == 1) return allowsRight(mode);
        return button == 0 && allowsLeft(mode) && !componentHasClickEvent;
    }

    public static String label(int mode) {
        return mode == LEFT ? "Left" : mode == RIGHT ? "Right" : "Both";
    }
}

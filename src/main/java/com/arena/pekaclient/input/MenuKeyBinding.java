package com.arena.pekaclient.input;

/** Validation and persistence contract shared by the menu picker and its recovery command. */
public final class MenuKeyBinding {
    public static final int DEFAULT_KEY = 54; // LWJGL KEY_RSHIFT
    public static final int ESCAPE = 1; // LWJGL KEY_ESCAPE
    private MenuKeyBinding() { }

    public interface Target {
        String conflict(int keyCode);
        void set(int keyCode);
        void save();
    }

    public static boolean valid(int keyCode) {
        return keyCode > 0 && keyCode <= 255 && keyCode != ESCAPE;
    }

    /** Escape, unbound and mouse codes never modify the target or write options. */
    public static String apply(int keyCode, Target target) {
        if (target == null || !valid(keyCode)) return null;
        String conflict = target.conflict(keyCode);
        target.set(keyCode);
        target.save();
        return conflict;
    }
}

package com.arena.pekaclient.feature;

/**
 * Font choice normalisation for Chat QoL. Minecraft 1.8.9 supplies the actual renderer, so no bundled font files or
 * unsafe platform lookups are needed. Unknown persisted names fall back to Minecraft instead of failing chat draw.
 */
public final class ChatFontPolicy {

    public static final String MINECRAFT = "Minecraft";
    public static final String UNICODE = "Unicode";
    public static final String COMPACT = "Compact";

    private ChatFontPolicy() {
    }

    public static String resolve(String requested) {
        if (UNICODE.equalsIgnoreCase(requested)) return UNICODE;
        if (COMPACT.equalsIgnoreCase(requested)) return COMPACT;
        return MINECRAFT;
    }

    /** Compact is a safe spacing policy over Minecraft's renderer, not a downloaded or embedded third-party font. */
    public static float scaleFor(String resolved) {
        return COMPACT.equals(resolve(resolved)) ? 0.86f : 1.0f;
    }
}

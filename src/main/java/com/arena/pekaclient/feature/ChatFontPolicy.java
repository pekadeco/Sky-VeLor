package com.arena.pekaclient.feature;

import com.arena.pekaclient.config.FontSettings;

/**
 * Compatibility adapter for saved pre-0.10.8.4 Chat QoL font names. New code uses the global font settings and
 * GlobalFontManager; this class stays intentionally tiny because old profiles and standalone migration tests refer
 * to it directly.
 */
public final class ChatFontPolicy {

    public static final String MINECRAFT = FontSettings.MINECRAFT_DEFAULT;
    public static final String UNICODE = "Unicode"; // historic value: migrated to Minecraft Default
    public static final String COMPACT = "Compact"; // historic value: migrated to Smooth
    public static final String SMOOTH = FontSettings.SMOOTH;
    public static final String DOODLE = FontSettings.DOODLE;

    private ChatFontPolicy() {
    }

    /** Safe migration/fallback policy. Unknown names always use Minecraft Default rather than a platform font. */
    public static String resolve(String requested) {
        if (SMOOTH.equalsIgnoreCase(requested) || COMPACT.equalsIgnoreCase(requested)) return SMOOTH;
        if (DOODLE.equalsIgnoreCase(requested)) return DOODLE;
        return MINECRAFT;
    }

    /** Legacy Compact had a smaller chat scale. Preserve that visual intent after migration without loading a font. */
    public static float scaleFor(String resolved) {
        return SMOOTH.equals(resolve(resolved)) ? 1.0f : 1.0f;
    }
}

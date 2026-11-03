package com.arena.pekaclient.feature;

import java.awt.Color;
import java.util.regex.Pattern;

/** Starred Mob marker, settings, geometry and color policy without Minecraft dependencies. */
public final class StarredMobPolicy {
    public static final int DISPLAY_BOX = 0;
    public static final int DISPLAY_OUTLINE = 1;
    public static final int DISPLAY_DISABLED = 2;
    public static final char STAR = '\u272E';

    private static final Pattern FORMAT_CODE = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern ZERO_HEALTH_SUFFIX = Pattern.compile(
            "(?:^|\\s)0(?:\\.0+)?\\s*(?:/\\s*[0-9,.]+(?:\\.0+)?)?\\s*[❤♥]\\s*$");
    private static long chromaStartTime = -1L;

    private StarredMobPolicy() {
    }

    public static int clampDisplayMode(int mode) {
        return Math.max(DISPLAY_BOX, Math.min(DISPLAY_DISABLED, mode));
    }

    /** New settings win when present; otherwise preserve the user's previous on/off setting. */
    public static int resolveDisplayMode(String savedMode, boolean legacyEnabled) {
        if (savedMode == null) {
            return legacyEnabled ? DISPLAY_BOX : DISPLAY_DISABLED;
        }
        try {
            return clampDisplayMode(Integer.parseInt(savedMode.trim()));
        } catch (NumberFormatException ignored) {
            return legacyEnabled ? DISPLAY_BOX : DISPLAY_DISABLED;
        }
    }

    /** Better NEF identifies star mobs from the exact U+272E marker on the floating name-tag stand. */
    public static boolean hasStarMarker(String rawName) {
        return rawName != null && stripFormatting(rawName).indexOf(STAR) >= 0;
    }

    /** Return true immediately when the entity is dead or has reached the zero-health threshold. */
    public static boolean isDeadOrZeroHealth(boolean dead, float health) {
        return dead || health <= 0.1F;
    }

    /** Name-based fallback for servers that report a zero-health mob before its entity health updates. */
    public static boolean isDyingName(String formattedName) {
        if (formattedName == null || formattedName.isEmpty()) {
            return false;
        }
        String plain = WHITESPACE.matcher(stripFormatting(formattedName).replace('\u00a0', ' ')).replaceAll(" ").trim();
        return ZERO_HEALTH_SUFFIX.matcher(plain).find();
    }

    public static String stripFormatting(String value) {
        return value == null ? "" : FORMAT_CODE.matcher(value).replaceAll("");
    }

    public static boolean containsIgnoreCase(String value, String search) {
        if (value == null || search == null || search.isEmpty() || search.length() > value.length()) {
            return false;
        }
        // This is used for per-frame Starred Mob color/box selection. Walk the formatted label directly
        // instead of constructing a regex Matcher, stripped String, and lowercase String on every call.
        for (int start = skipFormatting(value, 0); start < value.length();
             start = skipFormatting(value, start + 1)) {
            int cursor = start;
            int matched = 0;
            while (matched < search.length()) {
                cursor = skipFormatting(value, cursor);
                if (cursor >= value.length()
                        || Character.toLowerCase(value.charAt(cursor))
                        != Character.toLowerCase(search.charAt(matched))) {
                    break;
                }
                cursor++;
                matched++;
            }
            if (matched == search.length()) return true;
        }
        return false;
    }

    private static int skipFormatting(String value, int index) {
        while (index + 1 < value.length() && value.charAt(index) == '§'
                && isFormattingCode(value.charAt(index + 1))) {
            index += 2;
        }
        return index;
    }

    private static boolean isFormattingCode(char code) {
        char lower = Character.toLowerCase(code);
        return lower >= '0' && lower <= '9' || lower >= 'a' && lower <= 'f'
                || lower >= 'k' && lower <= 'o' || lower == 'r';
    }

    /**
     * Resolves the color by the starred armor stand name, matching Better NEF's named overrides.
     * The order is intentional so the more specific names are checked first.
     */
    public static int colorForName(String name, int defaultColor, int stormyColor,
                                   int withermancerColor, int zombieCommanderColor,
                                   int skeletonMasterColor) {
        if (containsIgnoreCase(name, "Stormy")) {
            return stormyColor;
        }
        if (containsIgnoreCase(name, "Withermancer")) {
            return withermancerColor;
        }
        if (containsIgnoreCase(name, "Zombie Commander")) {
            return zombieCommanderColor;
        }
        if (containsIgnoreCase(name, "Skeleton Master")) {
            return skeletonMasterColor;
        }
        return defaultColor;
    }

    /**
     * Vertical extents used by Better NEF's armor-stand name-tag box mode, relative to the
     * armor stand's feet. These deliberately differ from the target mob's own bounding box.
     */
    public static double starredBoxMinYOffset(String name) {
        if (containsIgnoreCase(name, "Withermancer")) return -2.6D;
        if (containsIgnoreCase(name, "Fels")) return -3.0D;
        return -2.0D;
    }

    public static double starredBoxMaxYOffset(String name) {
        if (containsIgnoreCase(name, "Withermancer")) return -0.2D;
        return 0.0D;
    }

    /** Exact Enderman-head texture value used by Better NEF to identify Fel armor stands. */
    public static boolean isFelHeadTexture(String textureValue) {
        if (textureValue == null) return false;
        return textureValue.equals("e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E1OWJiMGE3YTMyOTY1YjNkOTBkOGVhZmE4OTlkMTgzNWY0MjQ1MDllYWRkNGU2YjcwOWFkYTUwYjljZiJ9fX0=")
                || textureValue.equals("e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdhNTliYjBhN2EzMjk2NWIzZDkwZDhlYWZhODk5ZDE4MzVmNDI0NTA5ZWFkZDRlNmI3MDlhZGE1MGI5Y2YifX19");
    }

    /** Parses the saved chromaSpeed:alpha:red:green:blue format, matching Better NEF's hue cycle. */
    public static int parseColor(String color, int fallback) {
        if (color == null) return fallback;
        String value = color.trim();
        int first = value.indexOf(':');
        if (first < 0) return fallback;
        int second = value.indexOf(':', first + 1);
        if (second < 0) return fallback;
        int third = value.indexOf(':', second + 1);
        if (third < 0) return fallback;
        int fourth = value.indexOf(':', third + 1);
        if (fourth < 0 || value.indexOf(':', fourth + 1) >= 0) return fallback;

        // This method is used by ESP rendering. Parse bounded decimal fields directly rather than
        // split() + Integer.parseInt() so a rendered mob does not allocate five Strings and an array.
        int speed = parseColorField(value, 0, first);
        int alpha = parseColorField(value, first + 1, second);
        int red = parseColorField(value, second + 1, third);
        int green = parseColorField(value, third + 1, fourth);
        int blue = parseColorField(value, fourth + 1, value.length());
        if (speed < 0 || alpha < 0 || red < 0 || green < 0 || blue < 0) return fallback;
        if (speed == 0) return alpha << 24 | red << 16 | green << 8 | blue;

        if (chromaStartTime < 0L) chromaStartTime = System.currentTimeMillis();
        // Allocation-free equivalent of RGBtoHSB for the original RGB hue-cycle algorithm.
        // Keep the integer max/min arithmetic and operation order used by Color.RGBtoHSB so chroma
        // output remains pixel-identical to earlier releases rather than changing due to float rounding.
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        float brightness = max / 255.0F;
        float saturation = max == 0 ? 0.0F : (float) (max - min) / (float) max;
        float hue = 0.0F;
        if (saturation != 0.0F) {
            float redc = (float) (max - red) / (float) (max - min);
            float greenc = (float) (max - green) / (float) (max - min);
            float bluec = (float) (max - blue) / (float) (max - min);
            if (red == max) hue = bluec - greenc;
            else if (green == max) hue = 2.0F + redc - bluec;
            else hue = 4.0F + greenc - redc;
            hue /= 6.0F;
            if (hue < 0.0F) hue += 1.0F;
        }
        float seconds = ((255 - speed) / 254.0F) * 59.0F + 1.0F;
        hue += (System.currentTimeMillis() - chromaStartTime) / 1000.0F / seconds;
        hue %= 1.0F;
        if (hue < 0.0F) hue += 1.0F;
        return alpha << 24 | (Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF);
    }

    /** Parses a single 0..255 field without allocating a substring. Returns -1 for invalid input. */
    private static int parseColorField(String value, int start, int end) {
        if (start >= end) return -1;
        boolean negative = false;
        char first = value.charAt(start);
        if (first == '+' || first == '-') {
            negative = first == '-';
            start++;
            if (start >= end) return -1;
        }
        int result = 0;
        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') return -1;
            result = result * 10 + (c - '0');
            if (result > 255) return -1;
        }
        // Integer.parseInt accepted signed zero in the old config parser; preserve that edge case.
        return negative && result != 0 ? -1 : result;
    }

    /** Returns the configured hue-cycle speed (0 disables chroma). */
    public static int chromaSpeed(String color) {
        if (color == null) return 0;
        String[] parts = color.trim().split(":");
        if (parts.length != 5) return 0;
        try {
            return Math.max(0, Math.min(255, Integer.parseInt(parts[0])));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /** Changes only chroma speed, preserving opacity and the selected base RGB color. */
    public static String withChromaSpeed(String color, int speed) {
        if (color == null) return "0:255:255:255:255";
        String[] parts = color.trim().split(":");
        if (parts.length != 5) return color;
        parts[0] = Integer.toString(Math.max(0, Math.min(255, speed)));
        return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":" + parts[4];
    }
}

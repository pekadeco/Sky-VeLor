package com.arena.pekaclient.feature;

import java.util.Locale;

/** Pure title matcher for deciding whether the active sidebar identifies SkyBlock. */
public final class SkyblockContextPolicy {
    private SkyblockContextPolicy() { }

    public static boolean isSkyblockScoreboardTitle(String title) {
        if (title == null || title.trim().isEmpty()) return false;
        String normalized = title.replaceAll("(?i)\\u00a7[0-9A-FK-OR]", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        return normalized.contains("SKYBLOCK");
    }
}

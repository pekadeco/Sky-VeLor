package com.arena.pekaclient.hud;

import java.util.ArrayList;
import java.util.Properties;

/** Removes only retired HUD keys so older layout files remain harmless while active elements load normally. */
public final class HudConfigCompatibility {
    private static final String[] RETIRED_METRIC_PREFIXES = {
            "fps-display.", "ping-display.", "memory-display.", "stats."
    };
    private static final String[] RETIRED_DOLLARS_PREFIXES = {"dollars."};

    private HudConfigCompatibility() { }

    /** The former metric elements and consolidated stats group are retired without migrating visibility. */
    public static void ignoreRetiredMetricKeys(Properties properties) {
        removeMatching(properties, true, false);
    }

    /** Player Dollars and its manual balance source are retired; unrelated HUD keys remain untouched. */
    public static void ignoreRetiredPlayerDollarsKeys(Properties properties) {
        removeMatching(properties, false, true);
    }

    public static boolean isRetiredMetricKey(String key) {
        return startsWith(key, RETIRED_METRIC_PREFIXES);
    }

    public static boolean isRetiredPlayerDollarsKey(String key) {
        return startsWith(key, RETIRED_DOLLARS_PREFIXES)
                || "balance.source".equals(key) || "balance.manual".equals(key);
    }

    private static void removeMatching(Properties properties, boolean metrics, boolean dollars) {
        if (properties == null || properties.isEmpty()) return;
        // Properties has no safe removal while iterating its key set on Java 8, and this happens only once at load.
        ArrayList<String> retired = new ArrayList<String>();
        for (Object key : properties.keySet()) {
            String name = String.valueOf(key);
            if ((metrics && isRetiredMetricKey(name)) || (dollars && isRetiredPlayerDollarsKey(name))) {
                retired.add(name);
            }
        }
        for (String key : retired) properties.remove(key);
    }

    private static boolean startsWith(String key, String[] prefixes) {
        if (key == null) return false;
        for (String prefix : prefixes) if (key.startsWith(prefix)) return true;
        return false;
    }
}

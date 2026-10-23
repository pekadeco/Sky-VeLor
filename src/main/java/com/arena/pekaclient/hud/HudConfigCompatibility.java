package com.arena.pekaclient.hud;

import java.util.ArrayList;
import java.util.Properties;

/** Retires former metric-HUD keys without making old HUD configuration files fail to load. */
public final class HudConfigCompatibility {
    private static final String[] RETIRED_PREFIXES = {
            "fps-display.", "ping-display.", "memory-display.", "stats."
    };

    private HudConfigCompatibility() { }

    /** Removes only retired metric keys. All remaining HUD keys retain their normal load behavior. */
    public static void ignoreRetiredMetricKeys(Properties properties) {
        if (properties == null || properties.isEmpty()) return;
        // Properties has no safe removal while iterating its key set on Java 8, and this happens only once at load.
        ArrayList<String> retired = new ArrayList<String>();
        for (Object key : properties.keySet()) {
            String name = String.valueOf(key);
            if (isRetiredMetricKey(name)) retired.add(name);
        }
        for (String key : retired) properties.remove(key);
    }

    public static boolean isRetiredMetricKey(String key) {
        if (key == null) return false;
        for (String prefix : RETIRED_PREFIXES) if (key.startsWith(prefix)) return true;
        return false;
    }
}

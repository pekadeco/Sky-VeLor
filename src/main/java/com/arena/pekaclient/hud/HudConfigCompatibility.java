package com.arena.pekaclient.hud;

import java.util.ArrayList;
import java.util.Properties;

/** Removes only retired HUD keys so older layout files remain harmless while active elements load normally. */
public final class HudConfigCompatibility {
    private static final String[] RETIRED_METRIC_PREFIXES = {
            "fps-display.", "ping-display.", "memory-display."
    };
    private static final String[] RETIRED_DOLLARS_PREFIXES = {"dollars."};

    private HudConfigCompatibility() { }

    /**
     * Carries an opt-in legacy metric display forward to the restored one-piece Stats Display.
     *
     * <p>The three old elements were independently positioned, while the reference display is one fixed
     * Frame Rate / Mining / Memory group. If any old element was explicitly enabled and the user has not already
     * chosen {@code stats.enabled}, enable that one group before retiring the old keys. An explicit modern value,
     * including {@code false}, always wins. This migration cannot create duplicate renderers because the old element
     * classes are no longer registered and their keys are removed immediately afterward.</p>
     *
     * @return true when this call inserted {@code stats.enabled=true}
     */
    public static boolean migrateVisibleRetiredMetricDisplay(Properties properties) {
        if (properties == null || properties.containsKey("stats.enabled")) return false;
        for (String prefix : RETIRED_METRIC_PREFIXES) {
            String enabled = properties.getProperty(prefix + "enabled");
            if (enabled != null && "true".equalsIgnoreCase(enabled.trim())) {
                properties.setProperty("stats.enabled", "true");
                return true;
            }
        }
        return false;
    }

    /** The former independently-positioned metric elements are retired after optional visibility migration. */
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

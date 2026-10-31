package com.arena.pekaclient.feature.mining;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parsing and timing rules for the supported Fakepixel pickaxe abilities. */
public final class MiningAbilityCooldown {

    private static final Pattern USE_MESSAGE = Pattern.compile(
            "You used your\\s+(.+?)\\s+Pickaxe Ability[!.]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMAT_CODE = Pattern.compile("\u00a7[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    private MiningAbilityCooldown() {
    }

    /**
     * Returns a supported ability activation, or null when the message is unrelated or its cooldown is unknown.
     * Current nominal durations: Pickobulus 60 seconds, Mining Speed Boost 120 seconds.
     */
    public static AbilityUse parseUseMessage(String message) {
        if (message == null || message.isEmpty()) return null;
        String clean = FORMAT_CODE.matcher(message).replaceAll("").trim();
        Matcher matcher = USE_MESSAGE.matcher(clean);
        if (!matcher.find()) return null;

        String name = matcher.group(1).trim();
        String normalized = name.toLowerCase(Locale.ROOT);
        int seconds;
        if (normalized.contains("pickobulus")) {
            seconds = 60;
        } else if (normalized.contains("mining speed boost")) {
            seconds = 120;
        } else {
            return null;
        }
        return new AbilityUse(name, seconds);
    }

    /** Returns zero once ready; otherwise rounds remaining time up to a whole second. */
    public static long remainingSeconds(long readyAtMillis, long nowMillis) {
        if (readyAtMillis <= nowMillis) return 0L;
        return (readyAtMillis - nowMillis + 999L) / 1000L;
    }

    /** Pure display helper for the mining HUD. */
    public static String statusText(long nowMillis, boolean connectedToFakepixel,
                                    String abilityName, long readyAtMillis) {
        if (!connectedToFakepixel || abilityName == null || abilityName.isEmpty()) return "Not tracked";
        long seconds = remainingSeconds(readyAtMillis, nowMillis);
        return seconds <= 0L ? "Ready" : abilityName + " - " + seconds + "s";
    }

    public static final class AbilityUse {
        public final String name;
        public final int cooldownSeconds;

        private AbilityUse(String name, int cooldownSeconds) {
            this.name = name;
            this.cooldownSeconds = cooldownSeconds;
        }
    }
}

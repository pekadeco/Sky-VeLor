package com.arena.pekaclient.assist.mining;

import java.util.Locale;

/** Pure, bounded parsing and timing rules for the two explicitly supported Fakepixel pickaxe ability notices. */
public final class MiningAbilityPolicy {

    private MiningAbilityPolicy() {
    }

    public static final class Activation {
        public final String name;
        public final int cooldownSeconds;

        private Activation(String name, int cooldownSeconds) {
            this.name = name;
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    public static Activation parseActivation(String message) {
        String clean = plain(message);
        if (clean.length() == 0 || clean.length() > 256) return null;
        String lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("you used your ") || lower.indexOf(" pickaxe ability") < 0) return null;
        if (lower.indexOf("pickobulus") >= 0) return new Activation("Pickobulus", 60);
        if (lower.indexOf("mining speed boost") >= 0) return new Activation("Mining Speed Boost", 120);
        return null;
    }

    public static long remainingSeconds(long readyAtMillis, long nowMillis) {
        if (readyAtMillis <= nowMillis) return 0L;
        return (readyAtMillis - nowMillis + 999L) / 1000L;
    }

    public static String status(long nowMillis, boolean supportedServer, Activation activation, long readyAtMillis) {
        if (!supportedServer || activation == null) return "Not tracked";
        long remaining = remainingSeconds(readyAtMillis, nowMillis);
        return remaining == 0L ? "Ready" : activation.name + " - " + remaining + "s";
    }

    public static String plain(String message) {
        if (message == null) return "";
        StringBuilder out = new StringBuilder(Math.min(message.length(), 256));
        for (int i = 0; i < message.length() && out.length() < 256; i++) {
            char c = message.charAt(i);
            if (c == '\u00a7' && i + 1 < message.length()) {
                i++;
            } else if (c != '\r' && c != '\n') {
                out.append(c);
            }
        }
        return out.toString().trim();
    }
}

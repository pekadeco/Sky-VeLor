package com.arena.pekaclient.chat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the sender/recipient name from a stored Fakepixel-style chat line. This intentionally has no Minecraft
 * dependency so the exact context-menu decision can be verified with real-looking chat fixtures before rendering.
 */
public final class ChatPlayerName {

    private static final Pattern TRAILING_NAME = Pattern.compile("([A-Za-z0-9_]{3,16})$");

    private ChatPlayerName() {
    }

    /**
     * Returns the 3–16 character ASCII player name directly before the first colon-space, or an empty string when the
     * line is not player traffic. Rank/level/guild brackets, symbols, emoji, Party >, and From/To prefixes naturally
     * fall before the final name token and are therefore not included in the result.
     */
    public static String extract(String source) {
        String clean = ChatText.clean(source == null ? "" : source).replace('\n', ' ').trim();
        int delimiter = clean.indexOf(": ");
        if (delimiter <= 0) return "";
        String before = clean.substring(0, delimiter).trim();
        if (systemPrefix(before)) return "";
        Matcher matcher = TRAILING_NAME.matcher(before);
        if (!matcher.find()) return "";
        String name = matcher.group(1);
        return systemName(name) ? "" : name;
    }

    /** A menu label stays concrete about the command while leaving the two legacy copy labels unchanged. */
    public static String messageLabel(String name) {
        String clean = extract("From " + (name == null ? "" : name) + ": preview");
        return clean.length() == 0 ? "" : "Message player: /msg " + clean;
    }

    private static boolean systemPrefix(String value) {
        String lower = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("you ") || lower.startsWith("your ") || lower.startsWith("server")
                || lower.startsWith("system") || lower.startsWith("auction house") || lower.startsWith("bazaar")
                || lower.startsWith("skyblock") || lower.startsWith("profile") || lower.startsWith("tip")
                || lower.startsWith("warp") || lower.startsWith("friend");
    }

    private static boolean systemName(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return "server".equals(lower) || "system".equals(lower) || "auction".equals(lower)
                || "bazaar".equals(lower) || "profile".equals(lower) || "skyblock".equals(lower)
                || "tip".equals(lower) || "party".equals(lower) || "guild".equals(lower)
                || "from".equals(lower) || "to".equals(lower);
    }
}

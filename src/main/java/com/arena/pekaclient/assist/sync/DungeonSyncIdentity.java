package com.arena.pekaclient.assist.sync;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/** Local-only session fingerprint derivation. Raw names are never placed in an outbound payload by this class. */
public final class DungeonSyncIdentity {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private DungeonSyncIdentity() {
    }

    public static String sessionId(String serverId, String floor, Collection<String> names) {
        if (!validServerId(serverId) || !validFloor(floor) || names == null) return "";
        ArrayList<String> normalized = new ArrayList<String>();
        for (String name : names) {
            if (!validName(name)) return "";
            String next = name.toLowerCase(Locale.ROOT);
            if (normalized.contains(next)) return "";
            normalized.add(next);
        }
        if (normalized.size() < 2 || normalized.size() > 5) return "";
        Collections.sort(normalized);
        StringBuilder input = new StringBuilder(serverId.toLowerCase(Locale.ROOT)).append('|')
                .append(floor.toLowerCase(Locale.ROOT));
        for (String name : normalized) input.append('|').append(name);
        return sha256(input.toString());
    }

    public static boolean isStartLine(String value) {
        return "dungeon starts in 1 second.".equalsIgnoreCase(plain(value));
    }

    public static boolean validName(String name) {
        if (name == null || name.length() < 1 || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) return false;
        }
        return true;
    }

    public static boolean validServerId(String id) {
        if (id == null || id.length() < 3 || id.length() > 48) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) return false;
        }
        return true;
    }

    public static boolean validFloor(String floor) {
        return floor != null && floor.matches("(?i)(?:F|M)[1-7]");
    }

    private static String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(UTF8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException unavailable) {
            return "";
        }
    }

    private static String plain(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' && i + 1 < value.length()) { i++; continue; }
            out.append(c);
        }
        return out.toString().trim();
    }
}

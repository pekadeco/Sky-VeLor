package com.arena.pekaclient.assist.inventory;

import java.util.Locale;

/** Allocation-light token matching shared by the inventory overlay and its headless regression tests. */
public final class InventorySearchPolicy {
    private InventorySearchPolicy() {
    }

    public static String normalize(String value) {
        if (value == null || value.length() == 0) return "";
        StringBuilder out = new StringBuilder(Math.min(value.length(), 512));
        boolean previousSpace = true;
        for (int i = 0; i < value.length() && out.length() < 512; i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' && i + 1 < value.length()) {
                i++;
                continue;
            }
            if (Character.isLetterOrDigit(c)) {
                out.append(Character.toLowerCase(c));
                previousSpace = false;
            } else if (!previousSpace) {
                out.append(' ');
                previousSpace = true;
            }
        }
        int length = out.length();
        if (length > 0 && out.charAt(length - 1) == ' ') out.setLength(length - 1);
        return out.toString();
    }

    /** Every whitespace-delimited search term must occur in the normalized item text. */
    public static boolean matches(String itemText, String query) {
        String needle = normalize(query);
        if (needle.length() == 0) return true;
        String haystack = normalize(itemText);
        int start = 0;
        while (start < needle.length()) {
            int end = needle.indexOf(' ', start);
            if (end < 0) end = needle.length();
            if (end > start && haystack.indexOf(needle.substring(start, end)) < 0) return false;
            start = end + 1;
        }
        return true;
    }
}

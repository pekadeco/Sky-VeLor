package com.arena.pekaclient.feature.qol;

import java.util.Locale;

/** Text-only inventory search logic, kept independent of Minecraft for regression tests. */
public final class InventorySearchMatcher {
    private InventorySearchMatcher() {
    }

    public static boolean matches(String displayName, Iterable<String> lore, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) return true;

        StringBuilder searchable = new StringBuilder();
        append(searchable, displayName);
        if (lore != null) {
            for (String line : lore) append(searchable, line);
        }
        String text = searchable.toString();
        String[] terms = normalizedQuery.split("\\s+");
        for (String term : terms) {
            if (!term.isEmpty() && !text.contains(term)) return false;
        }
        return true;
    }

    public static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\u00a7' && i + 1 < value.length()) {
                i++;
                continue;
            }
            result.append(c);
        }
        return result.toString().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static void append(StringBuilder target, String value) {
        if (value == null || value.isEmpty()) return;
        if (target.length() > 0) target.append(' ');
        target.append(normalize(value));
    }
}

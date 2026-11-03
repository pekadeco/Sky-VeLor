package com.arena.pekaclient.feature.qol;

/** Pure text selection for chat copying; includes Minecraft formatting codes only when requested. */
public final class ChatCopyTextPolicy {
    private ChatCopyTextPolicy() { }

    public static String selectText(String unformatted, String formatted, boolean includeFormatting) {
        String text = includeFormatting ? safe(formatted) : stripFormatting(safe(unformatted));
        if (includeFormatting) {
            while (text.endsWith("\u00a7r")) text = text.substring(0, text.length() - 2);
        }
        return text.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String stripFormatting(String value) {
        String text = safe(value);
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length() && isFormatCode(text.charAt(i + 1))) {
                i++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isFormatCode(char code) {
        char c = Character.toLowerCase(code);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                || (c >= 'k' && c <= 'o') || c == 'r' || c == 'x';
    }
}

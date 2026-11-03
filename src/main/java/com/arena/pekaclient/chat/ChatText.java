package com.arena.pekaclient.chat;

/** Formatting-safe chat text conversion shared by the pipeline and clipboard service. */
public final class ChatText {
    private ChatText() { }
    public static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u00a7' && i + 1 < raw.length()) { i++; continue; }
            if (c != '\r') out.append(c);
        }
        return out.toString();
    }

    /** Sanitizes copied content for Minecraft's single-line chat input, not for general clipboard output. */
    public static String cleanForChatInput(String raw) {
        if (raw == null || raw.length() == 0) return "";
        StringBuilder out = new StringBuilder(raw.length());
        boolean pendingSpace = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            // Strip a Minecraft formatting pair before considering its code character as ordinary input.
            if (c == '\u00a7') {
                if (i + 1 < raw.length()) i++;
                continue;
            }
            if (c == '\n' || c == '\r' || c == '\t') {
                pendingSpace = out.length() > 0;
                continue;
            }
            // Chat packets are single-line; discard control characters, DEL, and literal section signs.
            if (c < 0x20 || c == 0x7F) continue;
            if (pendingSpace && c != ' ') out.append(' ');
            pendingSpace = false;
            out.append(c);
        }
        return out.toString().trim();
    }
}

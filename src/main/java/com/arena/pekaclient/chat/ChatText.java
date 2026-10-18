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
}

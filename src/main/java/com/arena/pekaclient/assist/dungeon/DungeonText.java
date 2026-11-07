package com.arena.pekaclient.assist.dungeon;

/** Shared formatting cleanup retained by room-alert and chest parsing. */
final class DungeonText {
    private DungeonText() { }
    static String normalize(String raw) {
        if (raw == null || raw.length() == 0) return "";
        StringBuilder clean = new StringBuilder(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            char c = raw.charAt(index);
            if (c == '\u00a7' && index + 1 < raw.length()) {
                index++;
            } else if (c >= 32) {
                clean.append(c);
            }
        }
        return clean.toString().trim();
    }

 }

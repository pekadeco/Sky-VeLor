package com.arena.pekaclient.assist.sync;

/** Minimal strict JSON-free protocol helpers; the transport accepts only bounded puzzle records. */
public final class DungeonSyncProtocol {
    private DungeonSyncProtocol() {
    }

    public static String encodePuzzle(String name, int x, int y, int z, double confidence) {
        if (!validPuzzleName(name) || confidence < .65D || confidence > 1D) return "";
        return "{\"type\":\"puzzle\",\"name\":\"" + escape(name) + "\",\"x\":" + x
                + ",\"y\":" + y + ",\"z\":" + z + ",\"confidence\":" + String.format(java.util.Locale.ROOT, "%.2f", confidence) + "}";
    }

    public static boolean validPuzzleName(String name) {
        if (name == null || name.length() < 1 || name.length() > 48) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_')) return false;
        }
        return true;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

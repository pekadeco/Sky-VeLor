package com.arena.pekaclient.assist;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Persistent direct-user command bindings for the Keybind Cut editor. This model deliberately has no Minecraft
 * dependency, so malformed data, entry order, and execution eligibility can be covered by standalone tests.
 */
public final class CommandKeybindSettings {

    /** The new Keybind Cut namespace is intentionally separate from the migrated wardrobe/pets aliases. */
    public static final String PREFIX = "assist.keybindCut.commandKeybinds";
    public static final String ENTRIES_KEY = PREFIX + ".entries";
    public static final int MAX_ENTRIES = 16;
    public static final int MAX_KEY_CODE = 255;
    public static final int MAX_COMMAND_LENGTH = 240;

    /** Master switch shown beside the editor route in the settings shell. Default off: commands are always opt-in. */
    public static boolean enabled;

    private static final ArrayList<Entry> ENTRIES = new ArrayList<Entry>();

    private CommandKeybindSettings() {
    }

    /** Immutable entry snapshots prevent renderer/runtime callers from mutating persistent state by accident. */
    public static final class Entry {
        private final int keyCode;
        private final String command;
        private final boolean enabled;

        private Entry(int keyCode, String command, boolean enabled) {
            this.keyCode = keyCode;
            this.command = command;
            this.enabled = enabled;
        }

        public int keyCode() {
            return keyCode;
        }

        public String command() {
            return command;
        }

        public boolean enabled() {
            return enabled;
        }
    }

    public static int count() {
        return ENTRIES.size();
    }

    /** Returns null rather than throwing for a stale UI row after an entry was deleted. */
    public static Entry entryAt(int index) {
        return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
    }

    /** Adds an incomplete but visibly editable slash-command row. It cannot execute until it has a real key/command. */
    public static boolean add() {
        if (ENTRIES.size() >= MAX_ENTRIES) return false;
        ENTRIES.add(new Entry(0, "/", true));
        return true;
    }

    public static boolean delete(int index) {
        if (index < 0 || index >= ENTRIES.size()) return false;
        ENTRIES.remove(index);
        return true;
    }

    public static void setKeyCode(int index, int keyCode) {
        Entry current = entryAt(index);
        if (current == null) return;
        // Zero is the explicit cleared state. Every other malformed value fails closed to zero.
        replace(index, validKeyCode(keyCode) ? keyCode : 0, current.command, current.enabled);
    }

    public static void setCommand(int index, String command) {
        Entry current = entryAt(index);
        if (current == null) return;
        replace(index, current.keyCode, normalizeCommand(command), current.enabled);
    }

    public static void setEntryEnabled(int index, boolean value) {
        Entry current = entryAt(index);
        if (current == null) return;
        replace(index, current.keyCode, current.command, value);
    }

    /** The first creation-order entry wins duplicate keys; the editor warns rather than silently dispatching two chats. */
    public static Entry firstExecutableForKey(int keyCode) {
        if (!enabled || !validKeyCode(keyCode)) return null;
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            if (entry.enabled && entry.keyCode == keyCode && isExecutable(entry)) return entry;
        }
        return null;
    }

    public static boolean hasOtherEntryWithKey(int index, int keyCode) {
        if (!validKeyCode(keyCode)) return false;
        for (int i = 0; i < ENTRIES.size(); i++) {
            if (i != index && ENTRIES.get(i).keyCode == keyCode) return true;
        }
        return false;
    }

    public static boolean validKeyCode(int keyCode) {
        return keyCode > 0 && keyCode <= MAX_KEY_CODE;
    }

    public static boolean isExecutable(Entry entry) {
        return entry != null && validKeyCode(entry.keyCode) && entry.command.length() > 1
                && entry.command.charAt(0) == '/';
    }

    /** A command is always normalized to one slash-led, single-line message and never stores control characters. */
    public static String normalizeCommand(String raw) {
        String source = raw == null ? "" : raw.trim();
        StringBuilder clean = new StringBuilder(Math.min(MAX_COMMAND_LENGTH, source.length() + 1));
        for (int i = 0; i < source.length() && clean.length() < MAX_COMMAND_LENGTH; i++) {
            char c = source.charAt(i);
            if (c >= 32 && c != 127) clean.append(c);
        }
        if (clean.length() == 0) return "/";
        if (clean.charAt(0) != '/') clean.insert(0, '/');
        if (clean.length() > MAX_COMMAND_LENGTH) clean.setLength(MAX_COMMAND_LENGTH);
        return clean.toString();
    }

    public static void reset() {
        enabled = false;
        ENTRIES.clear();
    }

    /** Loading never crashes on a corrupt entry: valid siblings retain their creation order. */
    public static void load(Properties source) {
        Properties p = source == null ? new Properties() : source;
        reset();
        enabled = Boolean.parseBoolean(p.getProperty(PREFIX, "false").trim());
        String rawEntries = p.getProperty(ENTRIES_KEY, "");
        if (rawEntries.length() == 0) return;
        String[] pieces = rawEntries.split(";");
        for (int i = 0; i < pieces.length && ENTRIES.size() < MAX_ENTRIES; i++) {
            Entry parsed = parse(pieces[i]);
            if (parsed != null) ENTRIES.add(parsed);
        }
    }

    public static void save(Properties p) {
        if (p == null) return;
        p.setProperty(PREFIX, Boolean.toString(enabled));
        p.setProperty(ENTRIES_KEY, serialize());
    }

    private static void replace(int index, int keyCode, String command, boolean entryEnabled) {
        ENTRIES.set(index, new Entry(keyCode, command, entryEnabled));
    }

    private static Entry parse(String raw) {
        if (raw == null) return null;
        int first = raw.indexOf(',');
        int second = first < 0 ? -1 : raw.indexOf(',', first + 1);
        if (first <= 0 || second <= first + 1 || second >= raw.length() - 1) return null;
        try {
            int key = Integer.parseInt(raw.substring(0, first));
            if (key != 0 && !validKeyCode(key)) return null;
            boolean entryEnabled = Boolean.parseBoolean(raw.substring(first + 1, second));
            String decoded = decode(raw.substring(second + 1));
            if (decoded == null) return null;
            return new Entry(key, normalizeCommand(decoded), entryEnabled);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String serialize() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ENTRIES.size(); i++) {
            Entry entry = ENTRIES.get(i);
            if (i > 0) out.append(';');
            out.append(entry.keyCode).append(',').append(entry.enabled).append(',').append(encode(entry.command));
        }
        return out.toString();
    }

    /** Hex UTF-16 keeps separators, spaces and all user-visible command characters round-trippable without codecs. */
    private static String encode(String value) {
        String safe = value == null ? "/" : value;
        StringBuilder out = new StringBuilder(safe.length() * 4);
        for (int i = 0; i < safe.length(); i++) {
            char c = safe.charAt(i);
            out.append(hex((c >>> 12) & 15));
            out.append(hex((c >>> 8) & 15));
            out.append(hex((c >>> 4) & 15));
            out.append(hex(c & 15));
        }
        return out.toString();
    }

    private static String decode(String value) {
        if (value == null || value.length() == 0 || value.length() % 4 != 0) return null;
        StringBuilder out = new StringBuilder(value.length() / 4);
        for (int i = 0; i < value.length(); i += 4) {
            int a = Character.digit(value.charAt(i), 16);
            int b = Character.digit(value.charAt(i + 1), 16);
            int c = Character.digit(value.charAt(i + 2), 16);
            int d = Character.digit(value.charAt(i + 3), 16);
            if (a < 0 || b < 0 || c < 0 || d < 0) return null;
            out.append((char) ((a << 12) | (b << 8) | (c << 4) | d));
        }
        return out.toString();
    }

    private static char hex(int value) {
        return "0123456789abcdef".charAt(value & 15);
    }
}

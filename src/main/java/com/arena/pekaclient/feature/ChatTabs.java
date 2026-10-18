package com.arena.pekaclient.feature;

import com.arena.pekaclient.chat.ChatText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Small, Minecraft-free tab/filter contract for the single default-chat implementation.
 *
 * Tabs never route or suppress received chat. They only decide which already-received message groups are visible in
 * an opted-in view. This keeps All complete, makes custom matches additive, and lets malformed regular expressions
 * fail closed without ever reaching the renderer.
 */
public final class ChatTabs {

    public static final String ALL = "all";
    public static final String PARTY = "party";
    public static final String REPLY = "reply";
    public static final String CUSTOM_PREFIX = "custom-";
    public static final int MAX_TABS = 8;
    public static final int BUILT_IN_COUNT = 3;
    public static final int MAX_CUSTOM_TABS = MAX_TABS - BUILT_IN_COUNT;

    public static final int CONTAINS = 0;
    public static final int STARTS_WITH = 1;
    public static final int REGEX = 2;

    private ChatTabs() {
    }

    /** A saved tab. Built-ins are immutable as filters but may be hidden/reordered by the settings layer. */
    public static final class Tab {
        private final String id;
        private final boolean builtIn;
        private String name;
        private boolean enabled;
        private int filterType;
        private String filterText;
        private String sendPrefix;
        private int colour;
        private boolean sound;
        private Pattern compiled;
        private String validationError = "";
        /** True only for malformed persisted input. A rejected editor draft keeps the last safe compiled matcher. */
        private boolean loadedInvalid;

        public Tab(String id, boolean builtIn, String name, boolean enabled, int filterType, String filterText,
                   String sendPrefix, int colour, boolean sound) {
            this.id = normalizeId(id, builtIn);
            this.builtIn = builtIn;
            this.name = normalizeName(name, this.id);
            this.enabled = enabled;
            this.filterType = normalizeFilterType(filterType);
            this.filterText = normalizeFilter(filterText);
            this.sendPrefix = normalizePrefix(sendPrefix);
            this.colour = normalizeColour(colour, defaultColour(this.id));
            this.sound = sound;
            recompile();
        }

        public String id() { return id; }
        public boolean builtIn() { return builtIn; }
        public String name() { return name; }
        public boolean enabled() { return enabled; }
        public int filterType() { return filterType; }
        public String filterText() { return filterText; }
        public String sendPrefix() { return sendPrefix; }
        public int colour() { return colour; }
        public boolean sound() { return sound; }
        public String validationError() { return validationError; }
        public boolean valid() { return validationError.length() == 0; }

        public void setName(String value) { name = normalizeName(value, id); }
        public void setEnabled(boolean value) { enabled = value; }
        public void setSendPrefix(String value) { sendPrefix = normalizePrefix(value); }
        public void setColour(int value) { colour = normalizeColour(value, defaultColour(id)); }
        public void setSound(boolean value) { sound = value; }

        /**
         * Applies a custom filter only when it is valid. The old valid filter remains in force on a rejected edit;
         * callers can display the returned error without risking a render-time PatternSyntaxException.
         */
        public boolean setFilter(int type, String value) {
            int nextType = normalizeFilterType(type);
            String nextText = normalizeFilter(value);
            Pattern next = null;
            String error = "";
            if (!builtIn && nextType == REGEX && nextText.length() > 0) {
                try {
                    next = Pattern.compile(nextText, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException bad) {
                    error = compactError(bad.getDescription());
                }
            }
            if (error.length() > 0) {
                validationError = error;
                return false;
            }
            filterType = nextType;
            filterText = nextText;
            compiled = next;
            validationError = "";
            loadedInvalid = false;
            return true;
        }

        /** Used by config loading: invalid legacy/custom regexes become safely non-matching, never a crash. */
        public void loadFilter(int type, String value) {
            filterType = normalizeFilterType(type);
            filterText = normalizeFilter(value);
            recompile();
        }

        public boolean matches(String source) {
            String clean = ChatText.clean(source == null ? "" : source);
            if (ALL.equals(id)) return true;
            if (PARTY.equals(id)) return partyLine(clean);
            if (REPLY.equals(id)) return replyLine(clean);
            if (builtIn || loadedInvalid || filterText.length() == 0) return false;
            String lower = clean.toLowerCase(Locale.ROOT);
            if (filterType == CONTAINS) return lower.contains(filterText.toLowerCase(Locale.ROOT));
            if (filterType == STARTS_WITH) return lower.startsWith(filterText.toLowerCase(Locale.ROOT));
            return compiled != null && compiled.matcher(clean).find();
        }

        public Tab copy() {
            Tab copy = new Tab(id, builtIn, name, enabled, filterType, filterText, sendPrefix, colour, sound);
            copy.validationError = validationError;
            copy.loadedInvalid = loadedInvalid;
            if (loadedInvalid) copy.compiled = null;
            return copy;
        }

        private void recompile() {
            compiled = null;
            validationError = "";
            loadedInvalid = false;
            if (!builtIn && filterType == REGEX && filterText.length() > 0) {
                try {
                    compiled = Pattern.compile(filterText, Pattern.CASE_INSENSITIVE);
                } catch (PatternSyntaxException bad) {
                    validationError = compactError(bad.getDescription());
                    loadedInvalid = true;
                }
            }
        }
    }

    public static Tab all() {
        return new Tab(ALL, true, "All", true, CONTAINS, "", "", 0xFFFFFF, false);
    }

    public static Tab party() {
        return new Tab(PARTY, true, "Party", true, CONTAINS, "", "/pc", 0x30D158, false);
    }

    public static Tab reply() {
        return new Tab(REPLY, true, "Reply", true, CONTAINS, "", "/r", 0x64D2FF, false);
    }

    public static Tab custom(String id, String name) {
        return new Tab(id, false, name, true, CONTAINS, "", "", 0xFF9F0A, false);
    }

    public static List<Tab> defaults() {
        List<Tab> tabs = new ArrayList<Tab>();
        tabs.add(all());
        tabs.add(party());
        tabs.add(reply());
        return tabs;
    }

    /** All always matches; a custom match is therefore additive and never removes the message from All. */
    public static boolean appearsIn(String tabId, String source, List<Tab> tabs) {
        Tab tab = find(tabs, tabId);
        return tab != null && tab.matches(source);
    }

    public static Tab find(List<Tab> tabs, String id) {
        if (tabs == null || id == null) return null;
        for (Tab tab : tabs) if (tab != null && id.equals(tab.id())) return tab;
        return null;
    }

    public static List<Tab> copyTabs(List<Tab> source) {
        List<Tab> result = new ArrayList<Tab>();
        if (source != null) {
            for (Tab tab : source) if (tab != null && result.size() < MAX_TABS) result.add(tab.copy());
        }
        return Collections.unmodifiableList(result);
    }

    public static String outgoingPrefix(Tab tab, String message) {
        String raw = message == null ? "" : message;
        if (raw.trim().length() == 0 || raw.startsWith("/") || tab == null || tab.sendPrefix().length() == 0) {
            return raw;
        }
        return tab.sendPrefix() + (tab.sendPrefix().endsWith(" ") ? "" : " ") + raw;
    }

    public static boolean partyLine(String clean) {
        String value = clean == null ? "" : clean.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("party >") || value.startsWith("party>") || value.startsWith("p >")
                || value.startsWith("p>") || value.contains(" party >");
    }

    public static boolean replyLine(String clean) {
        String value = clean == null ? "" : clean.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("from ") || value.startsWith("to ") || value.startsWith("[reply]")
                || value.startsWith("reply >") || value.startsWith("reply>");
    }

    public static boolean validCustomId(String id) {
        return id != null && id.startsWith(CUSTOM_PREFIX) && id.length() <= 32
                && id.substring(CUSTOM_PREFIX.length()).matches("[a-z0-9-]{1,24}");
    }

    public static String filterTypeName(int type) {
        return type == STARTS_WITH ? "Starts with" : type == REGEX ? "Regex" : "Contains text";
    }

    public static int normalizeFilterType(int type) {
        return type == STARTS_WITH || type == REGEX ? type : CONTAINS;
    }

    private static String normalizeId(String requested, boolean builtIn) {
        String value = requested == null ? "" : requested.trim().toLowerCase(Locale.ROOT);
        if (ALL.equals(value) || PARTY.equals(value) || REPLY.equals(value)) return value;
        if (!builtIn && validCustomId(value)) return value;
        return builtIn ? ALL : CUSTOM_PREFIX + "new";
    }

    private static String normalizeName(String requested, String fallback) {
        String value = requested == null ? "" : requested.trim();
        if (value.length() == 0) value = ALL.equals(fallback) ? "All" : PARTY.equals(fallback) ? "Party"
                : REPLY.equals(fallback) ? "Reply" : "Custom";
        return value.length() > 24 ? value.substring(0, 24) : value;
    }

    private static String normalizeFilter(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.length() > 160 ? clean.substring(0, 160) : clean;
    }

    private static String normalizePrefix(String value) {
        String clean = value == null ? "" : value.trim().replace('\n', ' ').replace('\r', ' ');
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static int normalizeColour(int value, int fallback) {
        return value < 0 || value > 0xFFFFFF ? fallback : value;
    }

    private static int defaultColour(String id) {
        return PARTY.equals(id) ? 0x30D158 : REPLY.equals(id) ? 0x64D2FF : 0xFF9F0A;
    }

    private static String compactError(String value) {
        String text = value == null ? "Invalid regex" : value.trim();
        return text.length() > 54 ? text.substring(0, 51) + "..." : text;
    }
}

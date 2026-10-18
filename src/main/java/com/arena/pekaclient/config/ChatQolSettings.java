package com.arena.pekaclient.config;

import com.arena.pekaclient.chat.ChatMenuPolicy;
import com.arena.pekaclient.feature.ChatTabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Persisted settings for the single, fail-closed Default Chat QoL implementation.
 *
 * The old split-window keys are intentionally consumed only as harmless compatibility input: this class never
 * recreates Chat 1, Chat 2, their classifier, windows, or pattern routing. A save writes just the simple default
 * chat schema below, so retired keys naturally disappear from the next config file.
 */
public final class ChatQolSettings {

    public static final String PREFIX = "chat.qol.";
    public static final String FONT_MINECRAFT_DEFAULT = "Minecraft Default";
    public static final String FONT_SMOOTH = "Smooth";
    public static final String FONT_DOODLE = "Doodle";

    /** Master switch remains opt-in: false means Minecraft's chat is not intercepted at all. */
    public static boolean enabled;
    public static boolean tabsEnabled;
    public static boolean smoothMessages;
    public static boolean hoverHighlight;
    public static int menuOpenMode;

    public static int backgroundColor;
    public static int backgroundOpacityOpen;
    public static int backgroundOpacityClosed;
    public static int backgroundRadius;
    /** Open chat uses message-fit height by default; true deliberately reserves the full visible chat area. */
    public static boolean backgroundOpenFullArea;
    /** False keeps the configured vanilla chat width; true uses the widest visible line plus a small edge pad. */
    public static boolean backgroundFitWidth;
    /** Unselected tabs default to translucent black; selected tabs stay blue and custom colours remain accents. */
    public static int tabInactiveColor;
    public static int tabBackgroundColor;
    public static int tabOpacity;
    public static int tabActiveOpacity;
    public static int messageAnimationMillis;
    public static int fadeSeconds;
    /** Default-on formatter-exact consecutive message compaction. */
    public static boolean compactMessages;
    /** RGB counter colour used by Sky VeLor's renderer; the vanilla cache fallback stays gray. */
    public static int compactCounterColor;
    public static int hoverColor;
    public static int hoverOpacity;

    private static final List<ChatTabs.Tab> TABS = new ArrayList<ChatTabs.Tab>();
    private static String selectedTabId = ChatTabs.ALL;
    private static boolean migrationPending;

    private ChatQolSettings() {
    }

    public static final class Result {
        public final boolean success;
        public final String message;
        public final String id;

        private Result(boolean success, String message, String id) {
            this.success = success;
            this.message = message == null ? "" : message;
            this.id = id == null ? "" : id;
        }

        public static Result ok(String message, String id) { return new Result(true, message, id); }
        public static Result error(String message) { return new Result(false, message, ""); }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isMigrationPending() {
        return migrationPending;
    }

    public static List<ChatTabs.Tab> tabs() {
        return Collections.unmodifiableList(TABS);
    }

    /** Visible list used by the real renderer. A malformed config cannot leave the tab strip without a safe All tab. */
    public static List<ChatTabs.Tab> visibleTabs() {
        List<ChatTabs.Tab> visible = new ArrayList<ChatTabs.Tab>();
        for (ChatTabs.Tab tab : TABS) if (tab.enabled()) visible.add(tab);
        if (visible.isEmpty()) {
            ChatTabs.Tab all = tab(ChatTabs.ALL);
            if (all != null) {
                all.setEnabled(true);
                visible.add(all);
            }
        }
        return visible;
    }

    public static ChatTabs.Tab tab(String id) {
        return ChatTabs.find(TABS, id);
    }

    public static String selectedTabId() {
        ensureSelectedVisible();
        return selectedTabId;
    }

    public static ChatTabs.Tab selectedTab() {
        return tab(selectedTabId());
    }

    public static void setSelectedTab(String id) {
        ChatTabs.Tab requested = tab(id);
        if (requested != null && requested.enabled()) {
            selectedTabId = requested.id();
        } else {
            selectedTabId = firstVisibleId();
        }
    }

    public static boolean isTabVisible(String id) {
        ChatTabs.Tab found = tab(id);
        return found != null && found.enabled();
    }

    /** Built-ins can be hidden but their filter/prefix definitions are never deleted or modified. */
    public static void setTabVisible(String id, boolean visible) {
        ChatTabs.Tab found = tab(id);
        if (found == null) return;
        found.setEnabled(visible);
        ensureOneVisible();
        ensureSelectedVisible();
    }

    public static Result addCustomTab(String name) {
        if (customCount() >= ChatTabs.MAX_CUSTOM_TABS || TABS.size() >= ChatTabs.MAX_TABS) {
            return Result.error("Tab limit reached (" + ChatTabs.MAX_TABS + ").");
        }
        String id = nextCustomId();
        ChatTabs.Tab created = ChatTabs.custom(id, name);
        TABS.add(created);
        return Result.ok("Added " + created.name() + ".", id);
    }

    /** Atomically edits one custom tab. Invalid regex input is rejected and leaves the last safe tab untouched. */
    public static Result updateCustomTab(String id, String name, int filterType, String filterText, String sendPrefix,
                                         int colour, boolean sound, boolean visible) {
        ChatTabs.Tab old = tab(id);
        if (old == null || old.builtIn()) return Result.error("Built-in tabs cannot be edited as custom tabs.");
        ChatTabs.Tab next = old.copy();
        next.setName(name);
        next.setSendPrefix(sendPrefix);
        next.setColour(colour);
        next.setSound(sound);
        next.setEnabled(visible);
        if (!next.setFilter(filterType, filterText)) {
            return Result.error("Invalid regex: " + next.validationError());
        }
        int index = TABS.indexOf(old);
        TABS.set(index, next);
        ensureOneVisible();
        ensureSelectedVisible();
        return Result.ok("Saved " + next.name() + ".", id);
    }

    public static boolean deleteCustomTab(String id) {
        ChatTabs.Tab old = tab(id);
        if (old == null || old.builtIn()) return false;
        TABS.remove(old);
        if (id.equals(selectedTabId)) selectedTabId = ChatTabs.ALL;
        ensureOneVisible();
        ensureSelectedVisible();
        return true;
    }

    /** Moves any tab one or more positions while retaining its stable id and saved filter. */
    public static boolean moveTab(String id, int delta) {
        int index = indexOf(id);
        if (index < 0 || delta == 0) return false;
        int destination = clamp(index + delta, 0, TABS.size() - 1);
        if (destination == index) return false;
        ChatTabs.Tab value = TABS.remove(index);
        TABS.add(destination, value);
        return true;
    }

    public static void setMenuOpenMode(int value) {
        menuOpenMode = value == ChatMenuPolicy.DISABLED ? ChatMenuPolicy.DISABLED : ChatMenuPolicy.normalize(value);
    }

    public static void resetBackground() {
        backgroundColor = 0x000000;
        backgroundOpacityOpen = 45;
        backgroundOpacityClosed = 25;
        backgroundRadius = 4;
        backgroundOpenFullArea = false;
        backgroundFitWidth = false;
    }

    public static void resetMessages() {
        smoothMessages = true;
        messageAnimationMillis = 140;
        fadeSeconds = 10;
        compactMessages = true;
        compactCounterColor = 0xAAAAAA;
    }

    public static void resetTabs() {
        TABS.clear();
        TABS.addAll(ChatTabs.defaults());
        selectedTabId = ChatTabs.ALL;
        tabsEnabled = true;
        tabInactiveColor = 0x000000;
        tabBackgroundColor = 0x2F7DFF;
        tabOpacity = 55;
        tabActiveOpacity = 60;
    }

    public static void resetMenuHighlight() {
        hoverHighlight = true;
        hoverColor = 0xFFFFFF;
        hoverOpacity = 25;
        setMenuOpenMode(ChatMenuPolicy.BOTH);
    }

    public static void resetDefaults() {
        enabled = false;
        tabsEnabled = true;
        resetMessages();
        resetMenuHighlight();
        resetBackground();
        resetTabs();
        migrationPending = false;
    }

    /** Reads current keys plus old split-chat keys without ever reviving their behavior. */
    public static void load(Properties source) {
        Properties p = source == null ? new Properties() : source;
        resetDefaults();
        boolean oldSplit = hasAnyPrefix(p, PREFIX + "separate", PREFIX + "window1.", PREFIX + "window2.",
                PREFIX + "windows.", PREFIX + "patterns.", PREFIX + "target.", PREFIX + "editor.");
        migrationPending = oldSplit || hasRetiredSimpleKeys(p);

        // Retired split/bridge keys never enable or configure the new renderer. Only the deliberately retained
        // top-level master/settings keys are honored.
        enabled = bool(p, PREFIX + "enabled", false);
        tabsEnabled = bool(p, PREFIX + "tabs", true);
        smoothMessages = bool(p, PREFIX + "smooth", smoothMessages);
        hoverHighlight = bool(p, PREFIX + "hover", hoverHighlight);
        if (p.containsKey(PREFIX + "menu.open")) {
            setMenuOpenMode(integer(p, PREFIX + "menu.open", menuOpenMode, ChatMenuPolicy.DISABLED,
                    ChatMenuPolicy.BOTH));
        } else if (p.containsKey(PREFIX + "menu")) {
            setMenuOpenMode(bool(p, PREFIX + "menu", false) ? ChatMenuPolicy.RIGHT : ChatMenuPolicy.DISABLED);
        }

        backgroundColor = colour(p, PREFIX + "background.color", 0x000000);
        backgroundOpacityOpen = integer(p, PREFIX + "background.openOpacity", 45, 0, 100);
        backgroundOpacityClosed = integer(p, PREFIX + "background.closedOpacity", 25, 0, 100);
        backgroundRadius = integer(p, PREFIX + "background.radius", 4, 0, 12);
        backgroundOpenFullArea = bool(p, PREFIX + "background.openFullArea", false);
        backgroundFitWidth = bool(p, PREFIX + "background.fitWidth", false);
        tabInactiveColor = colour(p, PREFIX + "tabs.inactiveColor", 0x000000);
        tabBackgroundColor = colour(p, PREFIX + "tabs.baseColor", 0x2F7DFF);
        tabOpacity = integer(p, PREFIX + "tabs.opacity", 55, 0, 100);
        tabActiveOpacity = integer(p, PREFIX + "tabs.activeOpacity", 60, 0, 100);
        messageAnimationMillis = integer(p, PREFIX + "messages.animation", 140, 0, 600);
        fadeSeconds = integer(p, PREFIX + "messages.fade", 10, 0, 60);
        compactMessages = bool(p, PREFIX + "messages.compact", true);
        compactCounterColor = colour(p, PREFIX + "messages.compactCounterColor", 0xAAAAAA);
        hoverColor = colour(p, PREFIX + "highlight.color", 0xFFFFFF);
        hoverOpacity = integer(p, PREFIX + "highlight.opacity", 25, 0, 100);

        loadTabs(p);
        String modernSelected = text(p, PREFIX + "tabs.selectedId", "");
        setSelectedTab(modernSelected.length() > 0 ? modernSelected : ChatTabs.ALL);
        normalise();
    }

    /** Saves only the new one-default-chat schema. Old Separate Chats data is deliberately omitted. */
    public static void save(Properties p) {
        if (p == null) return;
        // save() can be called with an existing Properties object by tools/tests, so actively remove retired split
        // keys and stale custom slots instead of relying on AppleSettings' fresh output map.
        removeRetiredKeys(p);
        normalise();
        p.setProperty(PREFIX + "enabled", Boolean.toString(enabled));
        p.setProperty(PREFIX + "tabs", Boolean.toString(tabsEnabled));
        p.setProperty(PREFIX + "tabs.selectedId", selectedTabId());
        p.setProperty(PREFIX + "tabs.inactiveColor", colourString(tabInactiveColor));
        p.setProperty(PREFIX + "tabs.baseColor", colourString(tabBackgroundColor));
        p.setProperty(PREFIX + "tabs.opacity", Integer.toString(tabOpacity));
        p.setProperty(PREFIX + "tabs.activeOpacity", Integer.toString(tabActiveOpacity));
        p.setProperty(PREFIX + "smooth", Boolean.toString(smoothMessages));
        p.setProperty(PREFIX + "hover", Boolean.toString(hoverHighlight));
        p.setProperty(PREFIX + "menu.open", Integer.toString(menuOpenMode));
        p.setProperty(PREFIX + "background.color", colourString(backgroundColor));
        p.setProperty(PREFIX + "background.openOpacity", Integer.toString(backgroundOpacityOpen));
        p.setProperty(PREFIX + "background.closedOpacity", Integer.toString(backgroundOpacityClosed));
        p.setProperty(PREFIX + "background.radius", Integer.toString(backgroundRadius));
        p.setProperty(PREFIX + "background.openFullArea", Boolean.toString(backgroundOpenFullArea));
        p.setProperty(PREFIX + "background.fitWidth", Boolean.toString(backgroundFitWidth));
        p.setProperty(PREFIX + "messages.animation", Integer.toString(messageAnimationMillis));
        p.setProperty(PREFIX + "messages.fade", Integer.toString(fadeSeconds));
        p.setProperty(PREFIX + "messages.compact", Boolean.toString(compactMessages));
        p.setProperty(PREFIX + "messages.compactCounterColor", colourString(compactCounterColor));
        p.setProperty(PREFIX + "highlight.color", colourString(hoverColor));
        p.setProperty(PREFIX + "highlight.opacity", Integer.toString(hoverOpacity));
        saveTabs(p);
        migrationPending = false;
    }

    private static void loadTabs(Properties p) {
        TABS.clear();
        ChatTabs.Tab all = ChatTabs.all();
        ChatTabs.Tab party = ChatTabs.party();
        ChatTabs.Tab reply = ChatTabs.reply();
        all.setEnabled(bool(p, PREFIX + "tabs.all", true));
        party.setEnabled(bool(p, PREFIX + "tabs.party", true));
        reply.setEnabled(bool(p, PREFIX + "tabs.reply", true));
        TABS.add(all);
        TABS.add(party);
        TABS.add(reply);

        int count = integer(p, PREFIX + "tabs.custom.count", 0, 0, ChatTabs.MAX_CUSTOM_TABS);
        Set<String> ids = new HashSet<String>();
        ids.add(ChatTabs.ALL); ids.add(ChatTabs.PARTY); ids.add(ChatTabs.REPLY);
        for (int i = 0; i < count; i++) {
            String key = PREFIX + "tabs.custom." + i + ".";
            String id = text(p, key + "id", "").toLowerCase(Locale.ROOT);
            if (!ChatTabs.validCustomId(id) || ids.contains(id)) continue;
            ids.add(id);
            ChatTabs.Tab custom = ChatTabs.custom(id, text(p, key + "name", "Custom"));
            custom.setEnabled(bool(p, key + "enabled", true));
            custom.setSendPrefix(text(p, key + "prefix", ""));
            custom.setColour(colour(p, key + "colour", 0xFF9F0A));
            custom.setSound(bool(p, key + "sound", false));
            custom.loadFilter(integer(p, key + "filter.type", ChatTabs.CONTAINS, ChatTabs.CONTAINS, ChatTabs.REGEX),
                    text(p, key + "filter.text", ""));
            if (!custom.valid()) custom.setEnabled(false);
            TABS.add(custom);
        }
        applyOrder(text(p, PREFIX + "tabs.order", "all,party,reply"));
        ensureOneVisible();
    }

    private static void saveTabs(Properties p) {
        StringBuilder order = new StringBuilder();
        int custom = 0;
        for (ChatTabs.Tab tab : TABS) {
            if (order.length() > 0) order.append(',');
            order.append(tab.id());
            if (tab.builtIn()) {
                p.setProperty(PREFIX + "tabs." + tab.id(), Boolean.toString(tab.enabled()));
                continue;
            }
            String key = PREFIX + "tabs.custom." + custom + ".";
            p.setProperty(key + "id", tab.id());
            p.setProperty(key + "name", tab.name());
            p.setProperty(key + "enabled", Boolean.toString(tab.enabled()));
            p.setProperty(key + "filter.type", Integer.toString(tab.filterType()));
            p.setProperty(key + "filter.text", tab.filterText());
            p.setProperty(key + "prefix", tab.sendPrefix());
            p.setProperty(key + "colour", colourString(tab.colour()));
            p.setProperty(key + "sound", Boolean.toString(tab.sound()));
            custom++;
        }
        p.setProperty(PREFIX + "tabs.order", order.toString());
        p.setProperty(PREFIX + "tabs.custom.count", Integer.toString(custom));
    }

    private static void applyOrder(String source) {
        String[] requested = source == null ? new String[0] : source.toLowerCase(Locale.ROOT).split(",");
        List<ChatTabs.Tab> reordered = new ArrayList<ChatTabs.Tab>();
        Set<String> used = new HashSet<String>();
        for (String raw : requested) {
            String id = raw.trim();
            ChatTabs.Tab found = tab(id);
            if (found != null && used.add(found.id())) reordered.add(found);
        }
        for (ChatTabs.Tab tab : TABS) if (used.add(tab.id())) reordered.add(tab);
        TABS.clear();
        TABS.addAll(reordered);
    }

    private static void normalise() {
        backgroundColor = validColour(backgroundColor, 0x000000);
        backgroundOpacityOpen = clamp(backgroundOpacityOpen, 0, 100);
        backgroundOpacityClosed = clamp(backgroundOpacityClosed, 0, 100);
        backgroundRadius = clamp(backgroundRadius, 0, 12);
        tabInactiveColor = validColour(tabInactiveColor, 0x000000);
        tabBackgroundColor = validColour(tabBackgroundColor, 0x2F7DFF);
        tabOpacity = clamp(tabOpacity, 0, 100);
        tabActiveOpacity = clamp(tabActiveOpacity, 0, 100);
        messageAnimationMillis = clamp(messageAnimationMillis, 0, 600);
        fadeSeconds = clamp(fadeSeconds, 0, 60);
        compactCounterColor = validColour(compactCounterColor, 0xAAAAAA);
        hoverColor = validColour(hoverColor, 0xFFFFFF);
        hoverOpacity = clamp(hoverOpacity, 0, 100);
        setMenuOpenMode(menuOpenMode);
        ensureOneVisible();
        ensureSelectedVisible();
    }

    private static void ensureOneVisible() {
        for (ChatTabs.Tab tab : TABS) if (tab.enabled()) return;
        ChatTabs.Tab all = tab(ChatTabs.ALL);
        if (all != null) all.setEnabled(true);
    }

    private static void ensureSelectedVisible() {
        ChatTabs.Tab selected = tab(selectedTabId);
        if (selected == null || !selected.enabled()) selectedTabId = firstVisibleId();
    }

    private static String firstVisibleId() {
        for (ChatTabs.Tab tab : TABS) if (tab.enabled()) return tab.id();
        return ChatTabs.ALL;
    }

    private static int customCount() {
        int count = 0;
        for (ChatTabs.Tab tab : TABS) if (!tab.builtIn()) count++;
        return count;
    }

    private static int indexOf(String id) {
        for (int i = 0; i < TABS.size(); i++) if (TABS.get(i).id().equals(id)) return i;
        return -1;
    }

    private static String nextCustomId() {
        for (int i = 1; i <= 999; i++) {
            String candidate = ChatTabs.CUSTOM_PREFIX + i;
            if (tab(candidate) == null) return candidate;
        }
        return ChatTabs.CUSTOM_PREFIX + System.currentTimeMillis();
    }

    private static boolean hasRetiredSimpleKeys(Properties p) {
        return p.containsKey(PREFIX + "tabs.selected") || p.containsKey(PREFIX + "tabs.always")
                || p.containsKey(PREFIX + "menu.animation") || p.containsKey(PREFIX + "background.opacity")
                || p.containsKey(PREFIX + "smooth.duration") || p.containsKey(PREFIX + "hover.opacity")
                || p.containsKey("chat.tabMode") || p.containsKey("chat.filterMode")
                || p.containsKey("chat.animation.enabled") || p.containsKey("chat.animation.messages");
    }

    private static boolean hasPrefix(Properties p, String prefix) {
        for (Object key : p.keySet()) if (key != null && key.toString().startsWith(prefix)) return true;
        return false;
    }

    private static boolean hasAnyPrefix(Properties p, String... prefixes) {
        for (String prefix : prefixes) if (hasPrefix(p, prefix)) return true;
        return false;
    }

    private static void removeRetiredKeys(Properties p) {
        List<Object> remove = new ArrayList<Object>();
        for (Object key : p.keySet()) {
            if (key == null) continue;
            String name = key.toString();
            if (name.startsWith(PREFIX + "tabs.custom.") || retiredKey(name)) remove.add(key);
        }
        for (Object key : remove) p.remove(key);
    }

    private static boolean retiredKey(String key) {
        return key.startsWith(PREFIX + "separate") || key.startsWith(PREFIX + "window1.")
                || key.startsWith(PREFIX + "window2.") || key.startsWith(PREFIX + "windows.")
                || key.startsWith(PREFIX + "patterns.") || key.startsWith(PREFIX + "target.")
                || key.startsWith(PREFIX + "editor.") || key.equals(PREFIX + "tabs.selected")
                || key.equals(PREFIX + "tabs.always") || key.equals(PREFIX + "menu.animation")
                || key.equals(PREFIX + "background.opacity") || key.equals(PREFIX + "smooth.duration")
                || key.equals(PREFIX + "hover.opacity") || key.equals("chat.tabMode")
                || key.equals("chat.filterMode") || key.equals("chat.animation.enabled")
                || key.equals("chat.animation.messages");
    }

    private static String text(Properties p, String key, String fallback) {
        String raw = p.getProperty(key);
        if (raw == null) return fallback == null ? "" : fallback;
        String value = raw.trim();
        return value.length() > 160 ? value.substring(0, 160) : value;
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        if (raw == null) return fallback;
        return "true".equalsIgnoreCase(raw.trim()) ? true : "false".equalsIgnoreCase(raw.trim()) ? false : fallback;
    }

    private static int integer(Properties p, String key, int fallback, int min, int max) {
        try { return clamp(Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()), min, max); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int colour(Properties p, String key, int fallback) {
        String raw = p.getProperty(key);
        if (raw == null) return fallback;
        String value = raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try { return validColour((int) Long.parseLong(value, 16), fallback); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String colourString(int colour) {
        String value = Integer.toHexString(validColour(colour, 0)).toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder("#");
        for (int i = value.length(); i < 6; i++) out.append('0');
        return out.append(value).toString();
    }

    private static int validColour(int value, int fallback) { return value < 0 || value > 0xFFFFFF ? fallback : value; }
    private static int clamp(int value, int min, int max) { return value < min ? min : value > max ? max : value; }
}

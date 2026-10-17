package com.arena.pekaclient.config;

import com.arena.pekaclient.feature.ChatPatternRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Persisted, client-only configuration for the native Chat QoL feature.
 *
 * This class deliberately contains no Minecraft references. That keeps configuration migration, layout clamping,
 * rule compilation and the editor contract testable without launching a game. All values are bounded before they
 * reach the render/input paths, and the compiled rule snapshot changes only after a configuration update.
 */
public final class ChatQolSettings {

    public static final String PREFIX = "chat.qol.";
    public static final int TAB_ALL = 0;
    public static final int TAB_PARTY = 1;
    public static final int TAB_REPLY = 2;
    public static final int SCALE_MIN = 1;
    public static final int SCALE_MAX = 4;

    private static final int POSITION_MIN = 0;
    private static final int POSITION_MAX = 10000;
    private static final int SIZE_MIN = 1000;
    private static final int SIZE_MAX = 10000;
    private static final int LIMIT_MIN = 10;
    private static final int LIMIT_MAX = 300;
    private static final int FADE_MIN = 0;
    private static final int FADE_MAX = 60;

    /** Master switch is false for a fresh install, so no chat behavior changes until the user opts in. */
    public static boolean enabled;
    public static boolean tabsEnabled;
    public static boolean separateChats;
    public static boolean smoothMessages;
    public static boolean hoverHighlight;
    public static boolean rightClickMenu;
    public static boolean customFont;

    public static int selectedTab;
    public static boolean showAll;
    public static boolean showParty;
    public static boolean showReply;
    /** Comma-separated stable IDs: all,party,reply. It is normalised at load and every editor update. */
    public static String tabOrder;

    public static boolean snapWindows;
    public static int backgroundOpacity;
    public static int backgroundRadius;
    public static int highlightOpacity;
    public static boolean copyWithColours;
    public static String fontName;
    public static int smoothDuration;

    private static final Window CHAT_1 = new Window("Chat 1", 2, 56, 45, 35);
    private static final Window CHAT_2 = new Window("Chat 2", 52, 56, 45, 35);
    private static final List<ChatPatternRules.Rule> PATTERNS = new ArrayList<ChatPatternRules.Rule>();
    private static ChatPatternRules compiledPatterns = new ChatPatternRules(Collections.<ChatPatternRules.Rule>emptyList());
    private static boolean migrationPending;

    private ChatQolSettings() {
    }

    /** A window's position and size are percentages stored independently for every Minecraft GUI scale. */
    public static final class Window {
        private final String defaultName;
        private final int defaultX;
        private final int defaultY;
        private final int defaultW;
        private final int defaultH;
        private final int[] x = new int[SCALE_MAX + 1];
        private final int[] y = new int[SCALE_MAX + 1];
        private final int[] w = new int[SCALE_MAX + 1];
        private final int[] h = new int[SCALE_MAX + 1];
        private String name;
        private int messageLimit;
        private int fadeSeconds;

        private Window(String defaultName, int defaultX, int defaultY, int defaultW, int defaultH) {
            this.defaultName = defaultName;
            this.defaultX = defaultX * 100;
            this.defaultY = defaultY * 100;
            this.defaultW = defaultW * 100;
            this.defaultH = defaultH * 100;
            reset();
        }

        private void reset() {
            name = defaultName;
            messageLimit = 100;
            fadeSeconds = 10;
            for (int scale = SCALE_MIN; scale <= SCALE_MAX; scale++) {
                x[scale] = defaultX;
                y[scale] = defaultY;
                w[scale] = defaultW;
                h[scale] = defaultH;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            String next = value == null ? "" : value.trim();
            if (next.length() == 0) {
                next = defaultName;
            }
            name = next.length() > 24 ? next.substring(0, 24) : next;
        }

        public int getMessageLimit() {
            return messageLimit;
        }

        public void setMessageLimit(int value) {
            messageLimit = clamp(value, LIMIT_MIN, LIMIT_MAX);
        }

        public int getFadeSeconds() {
            return fadeSeconds;
        }

        public void setFadeSeconds(int value) {
            fadeSeconds = clamp(value, FADE_MIN, FADE_MAX);
        }

        public int getX(int scale) {
            return x[scaleIndex(scale)];
        }

        public int getY(int scale) {
            return y[scaleIndex(scale)];
        }

        public int getW(int scale) {
            return w[scaleIndex(scale)];
        }

        public int getH(int scale) {
            return h[scaleIndex(scale)];
        }

        /** Updates a whole geometry atomically and clamps it inside its GUI-scale viewport. */
        public void setGeometry(int scale, int left, int top, int width, int height) {
            int index = scaleIndex(scale);
            w[index] = clamp(width, SIZE_MIN, SIZE_MAX);
            h[index] = clamp(height, SIZE_MIN, SIZE_MAX);
            x[index] = clamp(left, POSITION_MIN, POSITION_MAX - w[index]);
            y[index] = clamp(top, POSITION_MIN, POSITION_MAX - h[index]);
        }

        /** Reset only one GUI scale so users can keep a carefully arranged layout at other scales. */
        public void resetScale(int scale) {
            int index = scaleIndex(scale);
            setGeometry(index, defaultX, defaultY, defaultW, defaultH);
        }

        private void load(Properties p, String prefix) {
            setName(readString(p, prefix + "name", defaultName, 24));
            setMessageLimit(readInt(p, prefix + "limit", 100, LIMIT_MIN, LIMIT_MAX));
            setFadeSeconds(readInt(p, prefix + "fade", 10, FADE_MIN, FADE_MAX));
            for (int scale = SCALE_MIN; scale <= SCALE_MAX; scale++) {
                String key = prefix + "s" + scale + ".";
                setGeometry(scale,
                        readInt(p, key + "x", defaultX, POSITION_MIN, POSITION_MAX),
                        readInt(p, key + "y", defaultY, POSITION_MIN, POSITION_MAX),
                        readInt(p, key + "w", defaultW, SIZE_MIN, SIZE_MAX),
                        readInt(p, key + "h", defaultH, SIZE_MIN, SIZE_MAX));
            }
        }

        private void save(Properties p, String prefix) {
            p.setProperty(prefix + "name", name);
            p.setProperty(prefix + "limit", Integer.toString(messageLimit));
            p.setProperty(prefix + "fade", Integer.toString(fadeSeconds));
            for (int scale = SCALE_MIN; scale <= SCALE_MAX; scale++) {
                String key = prefix + "s" + scale + ".";
                p.setProperty(key + "x", Integer.toString(x[scale]));
                p.setProperty(key + "y", Integer.toString(y[scale]));
                p.setProperty(key + "w", Integer.toString(w[scale]));
                p.setProperty(key + "h", Integer.toString(h[scale]));
            }
        }
    }

    public static Window chat1() {
        return CHAT_1;
    }

    public static Window chat2() {
        return CHAT_2;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Returns a read-only snapshot that is safe for editor inspection. */
    public static List<ChatPatternRules.Rule> patternRules() {
        return Collections.unmodifiableList(new ArrayList<ChatPatternRules.Rule>(PATTERNS));
    }

    public static ChatPatternRules compiledPatterns() {
        return compiledPatterns;
    }

    public static int patternRuleCount() {
        return PATTERNS.size();
    }

    public static void setPatternRules(List<ChatPatternRules.Rule> source) {
        PATTERNS.clear();
        if (source != null) {
            int limit = Math.min(16, source.size());
            for (int i = 0; i < limit; i++) {
                ChatPatternRules.Rule rule = source.get(i);
                if (rule == null) continue;
                String expression = rule.expression == null ? "" : rule.expression.trim();
                if (expression.length() == 0 || expression.length() > 256) continue;
                PATTERNS.add(new ChatPatternRules.Rule(rule.target, expression));
            }
        }
        if (PATTERNS.isEmpty()) {
            addDefaultPatterns();
        }
        compiledPatterns = new ChatPatternRules(PATTERNS);
    }

    public static void resetPatterns() {
        PATTERNS.clear();
        addDefaultPatterns();
        compiledPatterns = new ChatPatternRules(PATTERNS);
    }

    public static boolean isTabVisible(int tab) {
        return tab == TAB_PARTY ? showParty : tab == TAB_REPLY ? showReply : showAll;
    }

    public static void setTabVisible(int tab, boolean visible) {
        if (tab == TAB_PARTY) {
            showParty = visible;
        } else if (tab == TAB_REPLY) {
            showReply = visible;
        } else {
            showAll = visible;
        }
        // The editor may hide any individual tab, but never leaves an unusable empty strip.
        if (!showAll && !showParty && !showReply) showAll = true;
        if (!isTabVisible(selectedTab)) selectedTab = firstVisibleTab();
    }

    public static void setSelectedTab(int tab) {
        selectedTab = clamp(tab, TAB_ALL, TAB_REPLY);
        if (!isTabVisible(selectedTab)) selectedTab = firstVisibleTab();
    }

    /** Normalises arbitrary text to a stable, duplicate-free order while preserving the editor's requested order. */
    public static void setTabOrder(String requested) {
        String[] candidates = requested == null ? new String[0] : requested.toLowerCase(java.util.Locale.ROOT).split(",");
        StringBuilder result = new StringBuilder();
        for (String candidate : candidates) {
            String value = candidate.trim();
            if (("all".equals(value) || "party".equals(value) || "reply".equals(value))
                    && result.indexOf(value) < 0) {
                if (result.length() > 0) result.append(',');
                result.append(value);
            }
        }
        appendMissingTab(result, "all");
        appendMissingTab(result, "party");
        appendMissingTab(result, "reply");
        tabOrder = result.toString();
    }

    /** Small allocation used only when a GUI is opened/reconfigured, not in the per-frame render loop. */
    public static int[] orderedTabs() {
        String[] values = tabOrder.split(",");
        int[] out = new int[3];
        int count = 0;
        for (String value : values) {
            int tab = "party".equals(value) ? TAB_PARTY : "reply".equals(value) ? TAB_REPLY : TAB_ALL;
            boolean duplicate = false;
            for (int i = 0; i < count; i++) if (out[i] == tab) duplicate = true;
            if (!duplicate) out[count++] = tab;
        }
        for (int tab = TAB_ALL; count < out.length && tab <= TAB_REPLY; tab++) {
            boolean duplicate = false;
            for (int i = 0; i < count; i++) if (out[i] == tab) duplicate = true;
            if (!duplicate) out[count++] = tab;
        }
        return out;
    }

    public static void resetWindows() {
        CHAT_1.reset();
        CHAT_2.reset();
        snapWindows = true;
    }

    public static void resetBackground() {
        backgroundOpacity = 48;
        backgroundRadius = 4;
    }

    public static void resetMessages() {
        CHAT_1.setMessageLimit(100);
        CHAT_2.setMessageLimit(100);
        CHAT_1.setFadeSeconds(10);
        CHAT_2.setFadeSeconds(10);
        smoothDuration = 120;
        resetPatterns();
    }

    public static void resetFont() {
        fontName = "Minecraft";
    }

    public static void resetTabs() {
        showAll = true;
        showParty = true;
        showReply = true;
        setTabOrder("all,party,reply");
        selectedTab = TAB_ALL;
    }

    public static void resetHoverMenu() {
        highlightOpacity = 35;
        copyWithColours = false;
    }

    /** True after a legacy chat profile has been read and should be rewritten using only chat.qol.* keys. */
    public static boolean isMigrationPending() {
        return migrationPending;
    }

    /** Resets every value. Public for deterministic headless configuration tests. */
    public static void resetDefaults() {
        enabled = false;
        tabsEnabled = true;
        separateChats = false;
        smoothMessages = false;
        hoverHighlight = false;
        rightClickMenu = false;
        customFont = false;
        selectedTab = TAB_ALL;
        showAll = true;
        showParty = true;
        showReply = true;
        tabOrder = "all,party,reply";
        snapWindows = true;
        backgroundOpacity = 48;
        backgroundRadius = 4;
        highlightOpacity = 35;
        copyWithColours = false;
        fontName = "Minecraft";
        smoothDuration = 120;
        CHAT_1.reset();
        CHAT_2.reset();
        resetPatterns();
        migrationPending = false;
    }

    /** Load and migrate settings. New chat.qol keys always win over the retired chat.tabs/chat.animation keys. */
    public static void load(Properties source) {
        Properties p = source == null ? new Properties() : source;
        resetDefaults();
        boolean hasNative = hasPrefix(p, PREFIX);
        boolean legacyTabProfile = p.containsKey("chat.tabMode") || p.containsKey("chat.filterMode")
                || readBool(p, "chat.tabs.enabled", false);
        boolean legacyAnimationProfile = readBool(p, "chat.animation.enabled", false)
                || p.containsKey("chat.animation.messages") || p.containsKey("chat.animation.messageSpeed")
                || p.containsKey("chat.animation.background") || p.containsKey("chat.animation.backgroundDuration")
                || p.containsKey("chat.animation.scrolling") || p.containsKey("chat.animation.scrollDuration")
                || p.containsKey("chat.animation.removeScrollBar");
        migrationPending = !hasNative && (legacyTabProfile || legacyAnimationProfile);

        enabled = readBool(p, PREFIX + "enabled", legacyTabProfile || legacyAnimationProfile);
        // An old animation-only profile must not suddenly add tabs. A historic tab profile alone opts tabs in;
        // fresh native defaults remain ready for use once the intentionally-off master is enabled.
        boolean tabsFallback = legacyTabProfile || (!legacyAnimationProfile && tabsEnabled);
        tabsEnabled = readBool(p, PREFIX + "tabs", tabsFallback);
        separateChats = readBool(p, PREFIX + "separate", separateChats);
        smoothMessages = readBool(p, PREFIX + "smooth", readBool(p, "chat.animation.messages", smoothMessages));
        hoverHighlight = readBool(p, PREFIX + "hover", hoverHighlight);
        rightClickMenu = readBool(p, PREFIX + "menu", rightClickMenu);
        customFont = readBool(p, PREFIX + "font.enabled", customFont);

        selectedTab = mapSelectedTab(p);
        showAll = readBool(p, PREFIX + "tabs.all", showAll);
        showParty = readBool(p, PREFIX + "tabs.party", showParty);
        showReply = readBool(p, PREFIX + "tabs.reply", showReply);
        setTabVisible(TAB_ALL, showAll);
        setTabOrder(readString(p, PREFIX + "tabs.order", tabOrder, 32));
        setSelectedTab(selectedTab);

        snapWindows = readBool(p, PREFIX + "windows.snap", snapWindows);
        backgroundOpacity = readInt(p, PREFIX + "background.opacity", backgroundOpacity, 0, 100);
        backgroundRadius = readInt(p, PREFIX + "background.radius", backgroundRadius, 0, 12);
        highlightOpacity = readInt(p, PREFIX + "hover.opacity", highlightOpacity, 0, 100);
        copyWithColours = readBool(p, PREFIX + "menu.colours", copyWithColours);
        fontName = readString(p, PREFIX + "font.name", fontName, 24);
        smoothDuration = readInt(p, PREFIX + "smooth.duration", smoothDuration, 0, 1000);
        CHAT_1.load(p, PREFIX + "window1.");
        CHAT_2.load(p, PREFIX + "window2.");

        List<ChatPatternRules.Rule> loaded = new ArrayList<ChatPatternRules.Rule>();
        int count = readInt(p, PREFIX + "patterns.count", -1, -1, 16);
        if (count >= 0) {
            for (int i = 0; i < count; i++) {
                String expression = readString(p, PREFIX + "patterns." + i + ".regex", "", 256);
                String target = readString(p, PREFIX + "patterns." + i + ".target", ChatPatternRules.CHAT_1, 8);
                if (expression.length() > 0) loaded.add(new ChatPatternRules.Rule(target, expression));
            }
        }
        if (!loaded.isEmpty()) {
            setPatternRules(loaded);
        }
    }

    /** Writes only native keys. Retired external-animation keys are intentionally not written back. */
    public static void save(Properties p) {
        if (p == null) return;
        normalise();
        p.setProperty(PREFIX + "enabled", Boolean.toString(enabled));
        p.setProperty(PREFIX + "tabs", Boolean.toString(tabsEnabled));
        p.setProperty(PREFIX + "separate", Boolean.toString(separateChats));
        p.setProperty(PREFIX + "smooth", Boolean.toString(smoothMessages));
        p.setProperty(PREFIX + "hover", Boolean.toString(hoverHighlight));
        p.setProperty(PREFIX + "menu", Boolean.toString(rightClickMenu));
        p.setProperty(PREFIX + "font.enabled", Boolean.toString(customFont));
        p.setProperty(PREFIX + "tabs.selected", Integer.toString(selectedTab));
        p.setProperty(PREFIX + "tabs.all", Boolean.toString(showAll));
        p.setProperty(PREFIX + "tabs.party", Boolean.toString(showParty));
        p.setProperty(PREFIX + "tabs.reply", Boolean.toString(showReply));
        p.setProperty(PREFIX + "tabs.order", tabOrder);
        p.setProperty(PREFIX + "windows.snap", Boolean.toString(snapWindows));
        p.setProperty(PREFIX + "background.opacity", Integer.toString(backgroundOpacity));
        p.setProperty(PREFIX + "background.radius", Integer.toString(backgroundRadius));
        p.setProperty(PREFIX + "hover.opacity", Integer.toString(highlightOpacity));
        p.setProperty(PREFIX + "menu.colours", Boolean.toString(copyWithColours));
        p.setProperty(PREFIX + "font.name", fontName);
        p.setProperty(PREFIX + "smooth.duration", Integer.toString(smoothDuration));
        CHAT_1.save(p, PREFIX + "window1.");
        CHAT_2.save(p, PREFIX + "window2.");
        p.setProperty(PREFIX + "patterns.count", Integer.toString(PATTERNS.size()));
        for (int i = 0; i < PATTERNS.size(); i++) {
            ChatPatternRules.Rule rule = PATTERNS.get(i);
            p.setProperty(PREFIX + "patterns." + i + ".target", rule.target);
            p.setProperty(PREFIX + "patterns." + i + ".regex", rule.expression);
        }
        migrationPending = false;
    }

    private static void normalise() {
        setSelectedTab(selectedTab);
        setTabOrder(tabOrder);
        backgroundOpacity = clamp(backgroundOpacity, 0, 100);
        backgroundRadius = clamp(backgroundRadius, 0, 12);
        highlightOpacity = clamp(highlightOpacity, 0, 100);
        smoothDuration = clamp(smoothDuration, 0, 1000);
        CHAT_1.setName(CHAT_1.getName());
        CHAT_2.setName(CHAT_2.getName());
    }

    private static int firstVisibleTab() {
        if (showAll) return TAB_ALL;
        if (showParty) return TAB_PARTY;
        return TAB_REPLY;
    }

    private static int mapSelectedTab(Properties p) {
        if (p.containsKey(PREFIX + "tabs.selected")) {
            return readInt(p, PREFIX + "tabs.selected", TAB_ALL, TAB_ALL, TAB_REPLY);
        }
        int legacy = p.containsKey("chat.tabMode")
                ? readInt(p, "chat.tabMode", TAB_ALL, 0, 4)
                : readInt(p, "chat.filterMode", TAB_ALL, 0, 2);
        if (legacy == 1) return TAB_PARTY;
        // The historical direct-message slot was 3. Unsupported historical destinations safely return to All.
        return legacy == 3 ? TAB_REPLY : TAB_ALL;
    }

    private static void addDefaultPatterns() {
        PATTERNS.add(new ChatPatternRules.Rule(ChatPatternRules.CHAT_1,
                "^(?:\\[[^\\]]{1,32}\\]\\s*)?[A-Za-z0-9_]{1,16}:\\s+.+$"));
        PATTERNS.add(new ChatPatternRules.Rule(ChatPatternRules.CHAT_2,
                "^(?:Welcome|You are|Sending to|Unable to).*$"));
    }

    private static void appendMissingTab(StringBuilder result, String wanted) {
        String[] values = result.length() == 0 ? new String[0] : result.toString().split(",");
        for (String value : values) {
            if (wanted.equals(value)) return;
        }
        if (result.length() > 0) result.append(',');
        result.append(wanted);
    }

    private static boolean hasPrefix(Properties p, String prefix) {
        for (Object key : p.keySet()) {
            if (key != null && key.toString().startsWith(prefix)) return true;
        }
        return false;
    }

    private static int scaleIndex(int scale) {
        return clamp(scale, SCALE_MIN, SCALE_MAX);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static int readInt(Properties p, String key, int fallback, int min, int max) {
        try {
            return clamp(Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()), min, max);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean readBool(Properties p, String key, boolean fallback) {
        String raw = p.getProperty(key);
        if (raw == null) return fallback;
        return "true".equalsIgnoreCase(raw.trim()) ? true
                : "false".equalsIgnoreCase(raw.trim()) ? false : fallback;
    }

    private static String readString(Properties p, String key, String fallback, int maxLength) {
        String value = p.getProperty(key, fallback == null ? "" : fallback).trim();
        if (value.length() == 0) return fallback == null ? "" : fallback;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}

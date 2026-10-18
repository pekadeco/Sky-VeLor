package com.arena.pekaclient.config;

import com.arena.pekaclient.feature.ChatPatternRules;
import com.arena.pekaclient.gui.ChatEditorPanelLayout;

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

    /** Appearance targets deliberately mirror the shared interaction surfaces. */
    public static final int TARGET_DEFAULT = 0;
    public static final int TARGET_CHAT_1 = 1;
    public static final int TARGET_CHAT_2 = 2;
    public static final int TARGET_COUNT = 3;
    public static final String FONT_MINECRAFT_DEFAULT = "Minecraft Default";
    public static final String FONT_SMOOTH = "Smooth";
    public static final String FONT_DOODLE = "Doodle";

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
    /** Retained as a migration alias for pre-0.10.8.4 chat-only font profiles. */
    public static boolean customFont;

    /** Selected target and Link all are editor state; target values themselves stay independent. */
    public static int editorTarget;
    public static boolean linkAll;

    /** Global Sky VeLor font settings. Vanilla text is never intercepted unless a future safe bridge is available. */
    public static boolean globalFontEnabled;
    public static String globalFontName;
    public static boolean applyAllMinecraftText;
    public static int fontUiScale;
    public static int fontHudScale;
    public static int fontEditorScale;
    public static int fontTabScale;
    public static int fontChatScale;

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

    private static final Appearance[] APPEARANCES = {
            new Appearance(TARGET_DEFAULT), new Appearance(TARGET_CHAT_1), new Appearance(TARGET_CHAT_2)
    };
    private static final ChatEditorPanelLayout.State[] EDITOR_PANELS = {
            new ChatEditorPanelLayout.State(2500, 1200),
            // Section panels store offsets around 50%; their default stack still comes from ChatEditorLayout.
            new ChatEditorPanelLayout.State(5000, 5000),
            new ChatEditorPanelLayout.State(5000, 5000),
            new ChatEditorPanelLayout.State(5000, 5000),
            new ChatEditorPanelLayout.State(5000, 5000),
            new ChatEditorPanelLayout.State(5000, 5000),
            new ChatEditorPanelLayout.State(5000, 5000)
    };

    private static final Window CHAT_1 = new Window("Chat 1", 2, 56, 45, 35);
    private static final Window CHAT_2 = new Window("Chat 2", 52, 56, 45, 35);
    private static final List<ChatPatternRules.Rule> PATTERNS = new ArrayList<ChatPatternRules.Rule>();
    private static ChatPatternRules compiledPatterns = new ChatPatternRules(Collections.<ChatPatternRules.Rule>emptyList());
    private static boolean migrationPending;

    private ChatQolSettings() {
    }

    /**
     * Independent visual/input policy for one chat target. Colours are RGB (alpha is supplied by their paired
     * opacity), scales are percentages, and every mutator clamps malformed config before it reaches rendering.
     */
    public static final class Appearance {
        private final int target;
        private int textColor;
        private int textScale;
        private int backgroundColor;
        private int backgroundOpacity;
        private int radius;
        private int borderColor;
        private int borderOpacity;
        private int lineSpacing;
        private boolean shadow;
        private String appearStyle;
        private int appearSpeed;
        private int fadeSeconds;
        private int hoverColor;
        private int hoverOpacity;
        private int scrollSpeed;
        private boolean smoothScroll;

        private Appearance(int target) {
            this.target = target;
            reset();
        }

        public int target() { return target; }
        public int textColor() { return textColor; }
        public int textScale() { return textScale; }
        public int backgroundColor() { return backgroundColor; }
        public int backgroundOpacity() { return backgroundOpacity; }
        public int radius() { return radius; }
        public int borderColor() { return borderColor; }
        public int borderOpacity() { return borderOpacity; }
        public int lineSpacing() { return lineSpacing; }
        public boolean shadow() { return shadow; }
        public String appearStyle() { return appearStyle; }
        public int appearSpeed() { return appearSpeed; }
        public int fadeSeconds() { return fadeSeconds; }
        public int hoverColor() { return hoverColor; }
        public int hoverOpacity() { return hoverOpacity; }
        public int scrollSpeed() { return scrollSpeed; }
        public boolean smoothScroll() { return smoothScroll; }

        /** Only Default Chat uses this to decide whether cancelling vanilla draw is necessary. */
        public boolean needsCustomDefaultRenderer() {
            return target == TARGET_DEFAULT && (textColor != 0xFFFFFF || textScale != 100 || backgroundOpacity != 0
                    || radius != 0 || borderOpacity != 0 || lineSpacing != 0 || shadow
                    || !"VANILLA".equals(appearStyle) || appearSpeed != 120 || fadeSeconds != 10
                    || scrollSpeed != 100 || smoothScroll);
        }

        public void setTextColor(int value) { textColor = colour(value, 0xFFFFFF); }
        public void setTextScale(int value) { textScale = clamp(value, 50, 200); }
        public void setBackgroundColor(int value) { backgroundColor = colour(value, 0x15171B); }
        public void setBackgroundOpacity(int value) { backgroundOpacity = clamp(value, 0, 100); }
        public void setRadius(int value) { radius = clamp(value, 0, 16); }
        public void setBorderColor(int value) { borderColor = colour(value, 0x4A4A50); }
        public void setBorderOpacity(int value) { borderOpacity = clamp(value, 0, 100); }
        public void setLineSpacing(int value) { lineSpacing = clamp(value, -2, 12); }
        public void setShadow(boolean value) { shadow = value; }
        public void setAppearStyle(String value) {
            appearStyle = "SLIDE".equalsIgnoreCase(value) ? "SLIDE"
                    : "FADE".equalsIgnoreCase(value) ? "FADE" : "VANILLA";
        }
        public void setAppearSpeed(int value) { appearSpeed = clamp(value, 0, 1000); }
        public void setFadeSeconds(int value) { fadeSeconds = clamp(value, 0, 60); }
        public void setHoverColor(int value) { hoverColor = colour(value, 0x64D2FF); }
        public void setHoverOpacity(int value) { hoverOpacity = clamp(value, 0, 100); }
        public void setScrollSpeed(int value) { scrollSpeed = clamp(value, 25, 400); }
        public void setSmoothScroll(boolean value) { smoothScroll = value; }

        private void reset() {
            textColor = 0xFFFFFF;
            textScale = 100;
            backgroundColor = 0x15171B;
            backgroundOpacity = target == TARGET_DEFAULT ? 0 : 48;
            radius = target == TARGET_DEFAULT ? 0 : 4;
            borderColor = target == TARGET_CHAT_1 ? 0x30D158 : target == TARGET_CHAT_2 ? 0x64D2FF : 0x4A4A50;
            borderOpacity = target == TARGET_DEFAULT ? 0 : 80;
            lineSpacing = 0;
            shadow = target != TARGET_DEFAULT;
            appearStyle = "VANILLA";
            appearSpeed = 120;
            fadeSeconds = 10;
            hoverColor = 0x64D2FF;
            hoverOpacity = 35;
            scrollSpeed = 100;
            smoothScroll = false;
        }

        private void copyFrom(Appearance source) {
            if (source == null || source == this) return;
            textColor = source.textColor;
            textScale = source.textScale;
            backgroundColor = source.backgroundColor;
            backgroundOpacity = source.backgroundOpacity;
            radius = source.radius;
            borderColor = source.borderColor;
            borderOpacity = source.borderOpacity;
            lineSpacing = source.lineSpacing;
            shadow = source.shadow;
            appearStyle = source.appearStyle;
            appearSpeed = source.appearSpeed;
            fadeSeconds = source.fadeSeconds;
            hoverColor = source.hoverColor;
            hoverOpacity = source.hoverOpacity;
            scrollSpeed = source.scrollSpeed;
            smoothScroll = source.smoothScroll;
        }

        private void load(Properties p, String prefix) {
            setTextColor(readColour(p, prefix + "text.color", textColor));
            setTextScale(readInt(p, prefix + "text.scale", textScale, 50, 200));
            setBackgroundColor(readColour(p, prefix + "background.color", backgroundColor));
            setBackgroundOpacity(readInt(p, prefix + "background.opacity", backgroundOpacity, 0, 100));
            setRadius(readInt(p, prefix + "background.radius", radius, 0, 16));
            setBorderColor(readColour(p, prefix + "border.color", borderColor));
            setBorderOpacity(readInt(p, prefix + "border.opacity", borderOpacity, 0, 100));
            setLineSpacing(readInt(p, prefix + "line.spacing", lineSpacing, -2, 12));
            setShadow(readBool(p, prefix + "shadow", shadow));
            setAppearStyle(readString(p, prefix + "appear.style", appearStyle, 12));
            setAppearSpeed(readInt(p, prefix + "appear.speed", appearSpeed, 0, 1000));
            setFadeSeconds(readInt(p, prefix + "fade", fadeSeconds, 0, 60));
            setHoverColor(readColour(p, prefix + "hover.color", hoverColor));
            setHoverOpacity(readInt(p, prefix + "hover.opacity", hoverOpacity, 0, 100));
            setScrollSpeed(readInt(p, prefix + "scroll.speed", scrollSpeed, 25, 400));
            setSmoothScroll(readBool(p, prefix + "scroll.smooth", smoothScroll));
        }

        private void save(Properties p, String prefix) {
            p.setProperty(prefix + "text.color", colourString(textColor));
            p.setProperty(prefix + "text.scale", Integer.toString(textScale));
            p.setProperty(prefix + "background.color", colourString(backgroundColor));
            p.setProperty(prefix + "background.opacity", Integer.toString(backgroundOpacity));
            p.setProperty(prefix + "background.radius", Integer.toString(radius));
            p.setProperty(prefix + "border.color", colourString(borderColor));
            p.setProperty(prefix + "border.opacity", Integer.toString(borderOpacity));
            p.setProperty(prefix + "line.spacing", Integer.toString(lineSpacing));
            p.setProperty(prefix + "shadow", Boolean.toString(shadow));
            p.setProperty(prefix + "appear.style", appearStyle);
            p.setProperty(prefix + "appear.speed", Integer.toString(appearSpeed));
            p.setProperty(prefix + "fade", Integer.toString(fadeSeconds));
            p.setProperty(prefix + "hover.color", colourString(hoverColor));
            p.setProperty(prefix + "hover.opacity", Integer.toString(hoverOpacity));
            // Scroll values are consumed only as a compatibility input; AppleSettings owns the one global policy.
        }
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

    /** Returns the independent appearance for Default Chat, Chat 1 or Chat 2. */
    public static Appearance appearance(int target) {
        return APPEARANCES[targetIndex(target)];
    }

    public static void setEditorTarget(int target) {
        editorTarget = targetIndex(target);
    }

    /** Applies one edit to the selected target or atomically to all three targets when Link all is enabled. */
    public interface AppearanceEdit {
        void apply(Appearance appearance);
    }

    public static void editAppearance(int target, AppearanceEdit edit) {
        if (edit == null) return;
        if (linkAll) {
            for (int i = 0; i < TARGET_COUNT; i++) edit.apply(APPEARANCES[i]);
        } else {
            edit.apply(APPEARANCES[targetIndex(target)]);
        }
        syncLegacyAppearanceAliases();
    }

    /** Copies every target-specific visual/input setting from one source. Link all deliberately fans out the copy. */
    public static void copyAppearanceFrom(int target, int source) {
        Appearance from = appearance(source);
        if (linkAll) {
            for (int i = 0; i < TARGET_COUNT; i++) if (i != targetIndex(source)) APPEARANCES[i].copyFrom(from);
        } else {
            appearance(target).copyFrom(from);
        }
        syncLegacyAppearanceAliases();
    }

    /** Resets a target only; Link all makes reset intentionally apply to every target. */
    public static void resetAppearance(int target) {
        if (linkAll) {
            for (Appearance appearance : APPEARANCES) appearance.reset();
        } else {
            appearance(target).reset();
        }
        syncLegacyAppearanceAliases();
    }

    /** Per-GUI-scale geometry/collapse state for the Chat Editor panels. */
    public static ChatEditorPanelLayout.State editorPanel(int panel) {
        int safe = panel < 0 ? 0 : (panel >= EDITOR_PANELS.length ? EDITOR_PANELS.length - 1 : panel);
        return EDITOR_PANELS[safe];
    }

    public static int editorPanelCount() {
        return EDITOR_PANELS.length;
    }

    public static void resetEditorPanels() {
        for (ChatEditorPanelLayout.State panel : EDITOR_PANELS) panel.resetAll();
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
        editAppearance(editorTarget, new AppearanceEdit() {
            @Override
            public void apply(Appearance appearance) {
                appearance.setBackgroundColor(0x15171B);
                appearance.setBackgroundOpacity(appearance.target() == TARGET_DEFAULT ? 0 : 48);
                appearance.setRadius(appearance.target() == TARGET_DEFAULT ? 0 : 4);
                appearance.setBorderOpacity(appearance.target() == TARGET_DEFAULT ? 0 : 80);
            }
        });
    }

    public static void resetMessages() {
        CHAT_1.setMessageLimit(100);
        CHAT_2.setMessageLimit(100);
        CHAT_1.setFadeSeconds(10);
        CHAT_2.setFadeSeconds(10);
        editAppearance(editorTarget, new AppearanceEdit() {
            @Override
            public void apply(Appearance appearance) {
                appearance.setLineSpacing(0);
                appearance.setShadow(appearance.target() != TARGET_DEFAULT);
                appearance.setAppearStyle("VANILLA");
                appearance.setAppearSpeed(120);
                appearance.setFadeSeconds(10);
                appearance.setScrollSpeed(100);
                appearance.setSmoothScroll(false);
            }
        });
        smoothDuration = 120;
        resetPatterns();
    }

    public static void resetFont() {
        globalFontEnabled = false;
        globalFontName = FONT_MINECRAFT_DEFAULT;
        fontName = globalFontName;
        customFont = false;
        applyAllMinecraftText = false;
        fontUiScale = fontHudScale = fontEditorScale = fontTabScale = fontChatScale = 100;
    }

    public static void resetTabs() {
        showAll = true;
        showParty = true;
        showReply = true;
        setTabOrder("all,party,reply");
        selectedTab = TAB_ALL;
    }

    public static void resetHoverMenu() {
        editAppearance(editorTarget, new AppearanceEdit() {
            @Override
            public void apply(Appearance appearance) {
                appearance.setHoverColor(0x64D2FF);
                appearance.setHoverOpacity(35);
            }
        });
        highlightOpacity = 35;
        // The legacy setting is retained only so old config files load; extra menu variants are never exposed.
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
        editorTarget = TARGET_DEFAULT;
        linkAll = false;
        globalFontEnabled = false;
        globalFontName = FONT_MINECRAFT_DEFAULT;
        applyAllMinecraftText = false;
        fontUiScale = 100;
        fontHudScale = 100;
        fontEditorScale = 100;
        fontTabScale = 100;
        fontChatScale = 100;
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
        fontName = FONT_MINECRAFT_DEFAULT;
        smoothDuration = 120;
        for (Appearance appearance : APPEARANCES) appearance.reset();
        resetEditorPanels();
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
        // Pre-0.10.8.4 stored a chat-only font switch/name. It becomes the global Sky VeLor selection on load.
        customFont = readBool(p, PREFIX + "font.enabled", customFont);
        globalFontEnabled = readBool(p, PREFIX + "globalFont.enabled", customFont);
        globalFontName = normalizeGlobalFontName(readString(p, PREFIX + "globalFont.name",
                readString(p, PREFIX + "font.name", globalFontName, 48), 48));
        applyAllMinecraftText = readBool(p, PREFIX + "globalFont.allMinecraft", false);
        fontUiScale = readInt(p, PREFIX + "globalFont.scale.ui", 100, 50, 200);
        fontHudScale = readInt(p, PREFIX + "globalFont.scale.hud", 100, 50, 200);
        fontEditorScale = readInt(p, PREFIX + "globalFont.scale.editor", 100, 50, 200);
        fontTabScale = readInt(p, PREFIX + "globalFont.scale.tab", 100, 50, 200);
        fontChatScale = readInt(p, PREFIX + "globalFont.scale.chat", 100, 50, 200);
        customFont = globalFontEnabled;

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
        // This retired key is intentionally consumed but not honoured: the menu has exactly two actions now.
        copyWithColours = false;
        fontName = globalFontName;
        smoothDuration = readInt(p, PREFIX + "smooth.duration", smoothDuration, 0, 1000);
        CHAT_1.load(p, PREFIX + "window1.");
        CHAT_2.load(p, PREFIX + "window2.");

        editorTarget = readInt(p, PREFIX + "editor.target", TARGET_DEFAULT, TARGET_DEFAULT, TARGET_CHAT_2);
        linkAll = readBool(p, PREFIX + "editor.linkAll", false);
        boolean hasTargetAppearance = hasPrefix(p, PREFIX + "target.");
        if (hasTargetAppearance) {
            appearance(TARGET_DEFAULT).load(p, PREFIX + "target.default.");
            appearance(TARGET_CHAT_1).load(p, PREFIX + "target.chat1.");
            appearance(TARGET_CHAT_2).load(p, PREFIX + "target.chat2.");
        } else {
            // Compatibility migration: shared 0.10.8.3 appearance becomes both native windows. Default chat stays
            // visually vanilla until deliberately customised.
            migrateLegacyAppearance(backgroundOpacity, backgroundRadius, highlightOpacity, smoothDuration);
        }
        loadEditorPanels(p);
        syncLegacyAppearanceAliases();

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
        // Legacy chat.qol.font/globalFont keys are read for migration only. FontSettings owns persistence now.
        p.setProperty(PREFIX + "tabs.selected", Integer.toString(selectedTab));
        p.setProperty(PREFIX + "tabs.all", Boolean.toString(showAll));
        p.setProperty(PREFIX + "tabs.party", Boolean.toString(showParty));
        p.setProperty(PREFIX + "tabs.reply", Boolean.toString(showReply));
        p.setProperty(PREFIX + "tabs.order", tabOrder);
        p.setProperty(PREFIX + "windows.snap", Boolean.toString(snapWindows));
        p.setProperty(PREFIX + "background.opacity", Integer.toString(backgroundOpacity));
        p.setProperty(PREFIX + "background.radius", Integer.toString(backgroundRadius));
        p.setProperty(PREFIX + "hover.opacity", Integer.toString(highlightOpacity));
        // Do not write the removed colour-copy option back. It cannot re-enable a third menu action.
        p.setProperty(PREFIX + "smooth.duration", Integer.toString(smoothDuration));
        p.setProperty(PREFIX + "editor.target", Integer.toString(editorTarget));
        p.setProperty(PREFIX + "editor.linkAll", Boolean.toString(linkAll));
        appearance(TARGET_DEFAULT).save(p, PREFIX + "target.default.");
        appearance(TARGET_CHAT_1).save(p, PREFIX + "target.chat1.");
        appearance(TARGET_CHAT_2).save(p, PREFIX + "target.chat2.");
        saveEditorPanels(p);
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
        editorTarget = targetIndex(editorTarget);
        globalFontName = normalizeGlobalFontName(globalFontName);
        customFont = globalFontEnabled;
        fontName = globalFontName;
        fontUiScale = clamp(fontUiScale, 50, 200);
        fontHudScale = clamp(fontHudScale, 50, 200);
        fontEditorScale = clamp(fontEditorScale, 50, 200);
        fontTabScale = clamp(fontTabScale, 50, 200);
        fontChatScale = clamp(fontChatScale, 50, 200);
        copyWithColours = false;
        smoothDuration = clamp(smoothDuration, 0, 1000);
        syncLegacyAppearanceAliases();
        CHAT_1.setName(CHAT_1.getName());
        CHAT_2.setName(CHAT_2.getName());
    }

    private static void migrateLegacyAppearance(int oldOpacity, int oldRadius, int oldHoverOpacity, int oldAppearSpeed) {
        for (int target = TARGET_CHAT_1; target <= TARGET_CHAT_2; target++) {
            Appearance appearance = appearance(target);
            appearance.setBackgroundOpacity(oldOpacity);
            appearance.setRadius(oldRadius);
            appearance.setHoverOpacity(oldHoverOpacity);
            appearance.setAppearSpeed(oldAppearSpeed);
            appearance.setFadeSeconds(target == TARGET_CHAT_1 ? CHAT_1.getFadeSeconds() : CHAT_2.getFadeSeconds());
        }
    }

    private static void syncLegacyAppearanceAliases() {
        Appearance primary = appearance(TARGET_CHAT_1);
        backgroundOpacity = primary.backgroundOpacity();
        backgroundRadius = primary.radius();
        highlightOpacity = primary.hoverOpacity();
        smoothDuration = primary.appearSpeed();
        fontName = normalizeGlobalFontName(globalFontName);
        customFont = globalFontEnabled;
    }

    private static void loadEditorPanels(Properties p) {
        for (int panel = 0; panel < EDITOR_PANELS.length; panel++) {
            ChatEditorPanelLayout.State state = EDITOR_PANELS[panel];
            for (int scale = SCALE_MIN; scale <= SCALE_MAX; scale++) {
                String prefix = PREFIX + "editor.panel." + panel + ".s" + scale + ".";
                state.set(scale,
                        readInt(p, prefix + "x", state.x(scale), 0, 10000),
                        readInt(p, prefix + "y", state.y(scale), 0, 10000),
                        readBool(p, prefix + "collapsed", state.collapsed(scale)));
            }
        }
    }

    private static void saveEditorPanels(Properties p) {
        for (int panel = 0; panel < EDITOR_PANELS.length; panel++) {
            ChatEditorPanelLayout.State state = EDITOR_PANELS[panel];
            for (int scale = SCALE_MIN; scale <= SCALE_MAX; scale++) {
                String prefix = PREFIX + "editor.panel." + panel + ".s" + scale + ".";
                p.setProperty(prefix + "x", Integer.toString(state.x(scale)));
                p.setProperty(prefix + "y", Integer.toString(state.y(scale)));
                p.setProperty(prefix + "collapsed", Boolean.toString(state.collapsed(scale)));
            }
        }
    }

    private static int targetIndex(int target) {
        return clamp(target, TARGET_DEFAULT, TARGET_CHAT_2);
    }

    private static int colour(int value, int fallback) {
        return value < 0 || value > 0xFFFFFF ? fallback : value;
    }

    private static int readColour(Properties p, String key, int fallback) {
        String raw = p.getProperty(key);
        if (raw == null) return fallback;
        String value = raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            return colour((int) Long.parseLong(value, 16), fallback);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String colourString(int value) {
        String hex = Integer.toHexString(colour(value, 0));
        StringBuilder out = new StringBuilder("#");
        for (int i = hex.length(); i < 6; i++) out.append('0');
        return out.append(hex.toUpperCase(java.util.Locale.ROOT)).toString();
    }

    private static String normalizeGlobalFontName(String requested) {
        if (requested == null) return FONT_MINECRAFT_DEFAULT;
        String value = requested.trim();
        if (FONT_SMOOTH.equalsIgnoreCase(value) || "Compact".equalsIgnoreCase(value)) return FONT_SMOOTH;
        if (FONT_DOODLE.equalsIgnoreCase(value)) return FONT_DOODLE;
        if (value.startsWith("External:")) {
            String file = value.substring("External:".length());
            String lower = file.toLowerCase(java.util.Locale.ROOT);
            if (file.length() >= 5 && file.length() <= 96 && file.indexOf('/') < 0 && file.indexOf('\\') < 0
                    && file.indexOf("..") < 0 && (lower.endsWith(".ttf") || lower.endsWith(".otf"))) {
                return "External:" + file;
            }
        }
        // Old Minecraft/Unicode chat choices never cause a font-load attempt on migration.
        return FONT_MINECRAFT_DEFAULT;
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
